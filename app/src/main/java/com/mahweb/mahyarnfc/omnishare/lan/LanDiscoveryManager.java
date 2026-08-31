package com.mahweb.mahyarnfc.omnishare.lan;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

import com.mahweb.mahyarnfc.omnishare.ble.EphemeralTokenProvider;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LanDiscoveryManager implements AutoCloseable {
    public interface Listener {
        void onPeer(LanPeer peer);
        void onPeerLost(String serviceName);
        void onError(String code, String message);
    }

    private final NsdManager nsd;
    private final EphemeralTokenProvider tokens;
    private final int capabilities;
    private final Listener listener;
    private final Map<String, LanPeer> peers = new ConcurrentHashMap<>();
    private NsdManager.RegistrationListener registrationListener;
    private NsdManager.DiscoveryListener discoveryListener;
    private String ownTokenHex = "";

    public LanDiscoveryManager(Context context, EphemeralTokenProvider tokens, int capabilities, Listener listener) {
        this.nsd = (NsdManager) context.getApplicationContext().getSystemService(Context.NSD_SERVICE);
        this.tokens = tokens;
        this.capabilities = capabilities;
        this.listener = listener;
    }

    public void advertise(int port) {
        if (nsd == null) { error("NSD_UNAVAILABLE", "Local discovery is unavailable"); return; }
        ownTokenHex = hex(tokens.currentToken(System.currentTimeMillis()));
        NsdServiceInfo service = new NsdServiceInfo();
        service.setServiceType(LanProtocol.SERVICE_TYPE);
        service.setServiceName("Mahyar-" + ownTokenHex.substring(Math.max(0, ownTokenHex.length() - 6)).toUpperCase(Locale.US));
        service.setPort(port);
        service.setAttribute("v", Integer.toString(LanProtocol.VERSION));
        service.setAttribute("t", ownTokenHex);
        service.setAttribute("c", Integer.toHexString(capabilities));
        registrationListener = new NsdManager.RegistrationListener() {
            public void onServiceRegistered(NsdServiceInfo info) { }
            public void onRegistrationFailed(NsdServiceInfo info, int code) { error("NSD_REGISTER_FAILED", Integer.toString(code)); }
            public void onServiceUnregistered(NsdServiceInfo info) { }
            public void onUnregistrationFailed(NsdServiceInfo info, int code) { error("NSD_UNREGISTER_FAILED", Integer.toString(code)); }
        };
        nsd.registerService(service, NsdManager.PROTOCOL_DNS_SD, registrationListener);
    }

    public void discover() {
        if (nsd == null || discoveryListener != null) return;
        discoveryListener = new NsdManager.DiscoveryListener() {
            public void onDiscoveryStarted(String type) { }
            public void onStartDiscoveryFailed(String type, int code) { error("NSD_START_FAILED", Integer.toString(code)); safeStopDiscovery(); }
            public void onStopDiscoveryFailed(String type, int code) { error("NSD_STOP_FAILED", Integer.toString(code)); }
            public void onDiscoveryStopped(String type) { }
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                if (!LanProtocol.SERVICE_TYPE.equals(serviceInfo.getServiceType())) return;
                resolve(serviceInfo);
            }
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                peers.remove(serviceInfo.getServiceName());
                if (listener != null) listener.onPeerLost(serviceInfo.getServiceName());
            }
        };
        nsd.discoverServices(LanProtocol.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    @SuppressWarnings("deprecation")
    private void resolve(NsdServiceInfo serviceInfo) {
        try {
            nsd.resolveService(serviceInfo, new NsdManager.ResolveListener() {
                public void onResolveFailed(NsdServiceInfo info, int code) { error("NSD_RESOLVE_FAILED", Integer.toString(code)); }
                public void onServiceResolved(NsdServiceInfo info) {
                    String v = attr(info, "v"), token = attr(info, "t"), caps = attr(info, "c");
                    if (!Integer.toString(LanProtocol.VERSION).equals(v) || token.isEmpty() || token.equals(ownTokenHex)) return;
                    InetAddress host = info.getHost();
                    if (host == null || info.getPort() <= 0) return;
                    int capabilityFlags;
                    try { capabilityFlags = Integer.parseInt(caps.isEmpty() ? "0" : caps, 16); } catch (NumberFormatException e) { capabilityFlags = 0; }
                    LanPeer peer = new LanPeer(info.getServiceName(), token, host, info.getPort(), capabilityFlags, System.currentTimeMillis());
                    peers.put(info.getServiceName(), peer);
                    if (listener != null) listener.onPeer(peer);
                }
            });
        } catch (IllegalArgumentException | IllegalStateException e) {
            error("NSD_RESOLVE_EXCEPTION", e.getMessage());
        }
    }

    private static String attr(NsdServiceInfo info, String key) {
        try {
            byte[] v = info.getAttributes().get(key);
            return v == null ? "" : new String(v, StandardCharsets.UTF_8);
        } catch (Exception e) { return ""; }
    }

    private void error(String code, String message) { if (listener != null) listener.onError(code, message == null ? "" : message); }
    private void safeStopDiscovery() {
        if (nsd != null && discoveryListener != null) {
            try { nsd.stopServiceDiscovery(discoveryListener); } catch (Exception ignored) { }
            discoveryListener = null;
        }
    }
    public Map<String, LanPeer> snapshot() { return new ConcurrentHashMap<>(peers); }
    public void close() {
        safeStopDiscovery();
        if (nsd != null && registrationListener != null) {
            try { nsd.unregisterService(registrationListener); } catch (Exception ignored) { }
            registrationListener = null;
        }
        peers.clear();
    }
    private static String hex(byte[] b) { StringBuilder s=new StringBuilder(); for(byte x:b)s.append(String.format(Locale.US,"%02x",x)); return s.toString(); }
}
