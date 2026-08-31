import com.mahweb.mahyarnfc.omnishare.*;
import com.mahweb.mahyarnfc.omnishare.crypto.*;
import com.mahweb.mahyarnfc.omnishare.identity.*;
import com.mahweb.mahyarnfc.omnishare.lan.*;
import com.mahweb.mahyarnfc.omnishare.transfer.*;
import com.mahweb.mahyarnfc.omnishare.transport.*;
import com.mahweb.mahyarnfc.omnishare.trust.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

public class LanHarness {
    static final class FakeIdentity implements IdentityStore {
        final String id; final KeyPair kp; final DeviceIdentity identity;
        FakeIdentity(String id) {
            this.id=id; kp=CryptoBox.generateEphemeralEcKeyPair();
            identity=new DeviceIdentity(id,CryptoBox.publicKeyB64(kp.getPublic()),CryptoBox.fingerprint(kp.getPublic()),id,1);
        }
        public DeviceIdentity getOrCreate(){return identity;}
        public PublicKey publicKey(){return kp.getPublic();}
        public byte[] sign(byte[] m){return CryptoBox.sign(kp.getPrivate(),m);}
    }
    static void check(boolean v,String m){if(!v)throw new AssertionError(m);}

    public static void main(String[] args) throws Exception {
        frameCodec();
        secureLoopback();
        multiRecipientIsolation();
        tenRecipientLoopback();
        System.out.println("LAN_GATE_C_HARNESS=PASS");
    }

    static void frameCodec() throws Exception {
        ByteArrayOutputStream b=new ByteArrayOutputStream();
        FrameCodec.write(b,LanProtocol.HELLO,"a".getBytes(StandardCharsets.UTF_8));
        FrameCodec.write(b,LanProtocol.ENVELOPE,"payload".getBytes(StandardCharsets.UTF_8));
        ByteArrayInputStream in=new ByteArrayInputStream(b.toByteArray());
        check(FrameCodec.read(in).type==LanProtocol.HELLO,"frame1");
        check(FrameCodec.read(in).type==LanProtocol.ENVELOPE,"frame2");
        ByteArrayOutputStream bad=new ByteArrayOutputStream(); new DataOutputStream(bad).writeInt(LanProtocol.MAX_FRAME_BYTES+1);
        try { FrameCodec.read(new ByteArrayInputStream(bad.toByteArray())); throw new AssertionError("oversize accepted"); }
        catch(IOException expected){}
    }

    static void secureLoopback() throws Exception {
        FakeIdentity serverId=new FakeIdentity("server-device");
        FakeIdentity clientId=new FakeIdentity("client-device");
        TrustedDevice td=new TrustedDevice(clientId.id,clientId.identity.publicKeyFingerprint,"client",RelationshipState.TRUSTED,true,1,1);
        TrustLookup lookup=id->id.equals(clientId.id)?td:null;
        TransferLedger ledger=new TransferLedger();
        List<TransferEnvelope> received=Collections.synchronizedList(new ArrayList<>());
        LanReceivePolicy policy=new LanReceivePolicy(){
            public boolean approve(String id,String fp,String code,TransferEnvelope e){return false;}
            public void onReceived(String id,String fp,TransferEnvelope e){received.add(e);}
            public void onRejected(String id,String fp,String reason){}
        };
        LanServer server=new LanServer(serverId,lookup,ledger,policy);
        int port=server.start();
        LanPeer peer=new LanPeer("loopback","deadbeef",InetAddress.getLoopbackAddress(),port,0,System.currentTimeMillis());
        byte[] payload="hello-secure-lan".getBytes(StandardCharsets.UTF_8);
        TransferEnvelope e=TransferEnvelope.unsigned(UUID.randomUUID().toString(),clientId.id,serverId.id,System.currentTimeMillis(),System.currentTimeMillis()+60_000,PayloadType.PROFILE_JSON,CryptoBox.randomBytes(16),payload,TransportKind.LAN);
        e=e.withSignature(clientId.sign(e.unsignedBytes()));
        LanClient.Result result=new LanClient(clientId).send(peer,e);
        check(result.peerDeviceId.equals(serverId.id),"server identity mismatch");
        check(Arrays.equals(received.get(0).payload,payload),"payload mismatch");
        check(result.ack.transferId.equals(e.transferId),"ack tx mismatch");

        // Same transfer is idempotent: returns ACK but does not import twice.
        new LanClient(clientId).send(peer,e);
        check(received.size()==1,"duplicate imported twice");

        // Envelope signed by another identity must be rejected after handshake authenticates clientId.
        FakeIdentity attacker=new FakeIdentity("attacker");
        TransferEnvelope bad=TransferEnvelope.unsigned(UUID.randomUUID().toString(),clientId.id,serverId.id,System.currentTimeMillis(),System.currentTimeMillis()+60_000,PayloadType.PROFILE_JSON,CryptoBox.randomBytes(16),payload,TransportKind.LAN);
        bad=bad.withSignature(attacker.sign(bad.unsignedBytes()));
        boolean rejected=false;
        try { new LanClient(clientId).send(peer,bad); } catch(Exception ex) { rejected=true; }
        check(rejected,"forged envelope signature accepted");
        server.close();
    }

    static void multiRecipientIsolation() throws Exception {
        OmniTransport fake=new OmniTransport(){
            public TransportKind kind(){return TransportKind.LAN;}
            public boolean isAvailable(){return true;}
            public List<Recipient> discoverRecipients(long t){return Collections.emptyList();}
            public TransportCandidate evaluate(Recipient r){return new TransportCandidate(TransportKind.LAN,100,"test");}
            public DeliveryAck send(Recipient r,TransferEnvelope e)throws Exception{if("B".equals(r.alias))throw new IOException("B down");return new DeliveryAck(e.transferId,r.deviceId,System.currentTimeMillis(),new byte[]{1});}
            public void cancel(String id){} public void close(){}
        };
        List<Recipient> rs=new ArrayList<>();
        for(String x:new String[]{"A","B","C"})rs.add(new Recipient(x,"",x,RelationshipState.KNOWN,EnumSet.of(TransportKind.LAN),Collections.emptyMap()));
        TransferEnvelope e=TransferEnvelope.unsigned("tx-multi","sender","",1,9999999999999L,PayloadType.PROFILE_JSON,new byte[16],new byte[]{1},TransportKind.LAN);
        try(MultiRecipientDispatcher d=new MultiRecipientDispatcher(2)){
            Map<String,CompletableFuture<DeliveryAck>> m=d.sendAll(fake,rs,e);
            check(m.get("A").get().recipientDeviceId.equals("A"),"A failed");
            boolean bFailed=false;try{m.get("B").get();}catch(ExecutionException ex){bFailed=true;}check(bFailed,"B should fail");
            check(m.get("C").get().recipientDeviceId.equals("C"),"C cancelled by B");
        }
    }
    static void tenRecipientLoopback() throws Exception {
        FakeIdentity client=new FakeIdentity("broadcast-client");
        List<LanServer> servers=new ArrayList<>();
        List<LanPeer> peers=new ArrayList<>();
        List<List<TransferEnvelope>> inboxes=new ArrayList<>();
        try {
            for(int i=0;i<10;i++) {
                FakeIdentity sid=new FakeIdentity("server-"+i);
                TrustedDevice td=new TrustedDevice(client.id,client.identity.publicKeyFingerprint,"client",RelationshipState.TRUSTED,true,1,1);
                List<TransferEnvelope> inbox=Collections.synchronizedList(new ArrayList<>()); inboxes.add(inbox);
                LanReceivePolicy pol=new LanReceivePolicy(){public boolean approve(String id,String fp,String code,TransferEnvelope e){return false;}public void onReceived(String id,String fp,TransferEnvelope e){inbox.add(e);}public void onRejected(String id,String fp,String reason){}};
                LanServer srv=new LanServer(sid,id->id.equals(client.id)?td:null,new TransferLedger(),pol); int port=srv.start(); servers.add(srv);
                peers.add(new LanPeer("s"+i,"t"+i,InetAddress.getLoopbackAddress(),port,0,System.currentTimeMillis()));
            }
            byte[] payload="broadcast-10".getBytes(StandardCharsets.UTF_8); long now=System.currentTimeMillis();
            TransferEnvelope env=TransferEnvelope.unsigned(UUID.randomUUID().toString(),client.id,"",now,now+60000,PayloadType.PROFILE_JSON,CryptoBox.randomBytes(16),payload,TransportKind.LAN);
            env=env.withSignature(client.sign(env.unsignedBytes()));
            ExecutorService pool=Executors.newFixedThreadPool(4); List<Future<DeliveryAck>> fs=new ArrayList<>(); final TransferEnvelope signed=env;
            for(LanPeer peer:peers) fs.add(pool.submit(()->new LanClient(client).send(peer,signed).ack));
            for(Future<DeliveryAck> f:fs) check(f.get(20,TimeUnit.SECONDS)!=null,"10-recipient ACK missing");
            pool.shutdownNow();
            for(List<TransferEnvelope> inbox:inboxes) check(inbox.size()==1,"10-recipient inbox mismatch");
        } finally { for(LanServer s:servers)s.close(); }
    }

}
