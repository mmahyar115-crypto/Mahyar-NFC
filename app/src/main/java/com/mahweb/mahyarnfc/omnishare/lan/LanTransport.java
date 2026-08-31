package com.mahweb.mahyarnfc.omnishare.lan;

import com.mahweb.mahyarnfc.omnishare.TransferEnvelope;
import com.mahweb.mahyarnfc.omnishare.TransportKind;
import com.mahweb.mahyarnfc.omnishare.identity.IdentityStore;
import com.mahweb.mahyarnfc.omnishare.transport.*;

import java.net.InetAddress;
import java.io.IOException;
import java.util.*;

public final class LanTransport implements OmniTransport {
    private final IdentityStore identity;
    private final LanDiscoveryManager discovery;
    private final LanPeerVerifier verifier;
    private final Map<String, LanPeer> peers = new HashMap<>();

    public LanTransport(IdentityStore identity, LanDiscoveryManager discovery) { this(identity, discovery, null); }
    public LanTransport(IdentityStore identity, LanDiscoveryManager discovery, LanPeerVerifier verifier) {
        this.identity = identity; this.discovery = discovery; this.verifier = verifier;
    }
    public TransportKind kind() { return TransportKind.LAN; }
    public boolean isAvailable() { return discovery != null; }

    public List<Recipient> discoverRecipients(long timeoutMs) throws Exception {
        if (discovery == null) return Collections.emptyList();
        discovery.discover();
        if (timeoutMs > 0) Thread.sleep(Math.min(timeoutMs, 2500L));
        List<Recipient> out = new ArrayList<>();
        for (LanPeer p : discovery.snapshot().values()) {
            peers.put(p.serviceName, p);
            Map<String,String> m = new HashMap<>();
            m.put("serviceName", p.serviceName);
            m.put("host", p.host.getHostAddress());
            m.put("port", Integer.toString(p.port));
            m.put("token", p.tokenHex);
            out.add(new Recipient("", "", "Mahyar LAN • " + p.tokenHex.substring(Math.max(0,p.tokenHex.length()-4)),
                    com.mahweb.mahyarnfc.omnishare.trust.RelationshipState.UNKNOWN,
                    EnumSet.of(TransportKind.LAN), m));
        }
        return out;
    }

    public TransportCandidate evaluate(Recipient r) {
        boolean has = r != null && r.transports.contains(TransportKind.LAN);
        return new TransportCandidate(TransportKind.LAN, has ? 100 : 0, has ? "same LAN" : "not available");
    }

    public DeliveryAck send(Recipient r, TransferEnvelope e) throws Exception {
        if (r == null) throw new IllegalArgumentException("recipient");
        String host = r.metadata.get("host"), port = r.metadata.get("port"), service = r.metadata.get("serviceName");
        LanPeer peer = service == null ? null : peers.get(service);
        if (peer == null && host != null && port != null) {
            peer = new LanPeer(service==null?"manual":service, r.metadata.getOrDefault("token",""), InetAddress.getByName(host), Integer.parseInt(port), 0, System.currentTimeMillis());
        }
        if (peer == null) throw new IOException("LAN endpoint unavailable");
        return new LanClient(identity, verifier).send(peer, e).ack;
    }
    public void cancel(String transferId) { }
    public void close() { if (discovery != null) discovery.close(); }
}
