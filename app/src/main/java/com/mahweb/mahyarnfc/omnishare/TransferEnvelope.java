package com.mahweb.mahyarnfc.omnishare;
import com.mahweb.mahyarnfc.omnishare.crypto.CryptoBox;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Arrays;

public final class TransferEnvelope {
    public static final int PROTOCOL_VERSION = 1;
    public final int protocolVersion;
    public final String transferId, senderDeviceId, recipientDeviceId, payloadHashHex;
    public final long createdAtEpochMs, expiresAtEpochMs;
    public final PayloadType payloadType;
    public final byte[] nonce, payload, signature;
    public final TransportKind transport;
    public final int retryCount;

    public TransferEnvelope(int protocolVersion, String transferId, String senderDeviceId, String recipientDeviceId,
            long createdAtEpochMs, long expiresAtEpochMs, PayloadType payloadType, String payloadHashHex,
            byte[] nonce, byte[] payload, byte[] signature, TransportKind transport, int retryCount) {
        this.protocolVersion=protocolVersion; this.transferId=nz(transferId); this.senderDeviceId=nz(senderDeviceId);
        this.recipientDeviceId=nz(recipientDeviceId); this.createdAtEpochMs=createdAtEpochMs; this.expiresAtEpochMs=expiresAtEpochMs;
        this.payloadType=payloadType; this.payloadHashHex=nz(payloadHashHex); this.nonce=copy(nonce); this.payload=copy(payload);
        this.signature=copy(signature); this.transport=transport; this.retryCount=retryCount;
    }
    public static TransferEnvelope unsigned(String transferId, String senderDeviceId, String recipientDeviceId,
            long createdAt, long expiresAt, PayloadType type, byte[] nonce, byte[] payload, TransportKind transport) {
        return new TransferEnvelope(PROTOCOL_VERSION, transferId, senderDeviceId, recipientDeviceId, createdAt, expiresAt,
                type, CryptoBox.sha256Hex(payload), nonce, payload, new byte[0], transport, 0);
    }
    public TransferEnvelope withSignature(byte[] sig) {
        return new TransferEnvelope(protocolVersion,transferId,senderDeviceId,recipientDeviceId,createdAtEpochMs,expiresAtEpochMs,
                payloadType,payloadHashHex,nonce,payload,sig,transport,retryCount);
    }
    public boolean isValidAt(long now) {
        return protocolVersion==PROTOCOL_VERSION && !transferId.isEmpty() && !senderDeviceId.isEmpty() && payloadType!=null && transport!=null
            && createdAtEpochMs<=now+300000L && expiresAtEpochMs>=now && expiresAtEpochMs>createdAtEpochMs
            && nonce.length>=12 && nonce.length<=64 && payload.length<=65536 && payloadHashHex.equals(CryptoBox.sha256Hex(payload));
    }
    public boolean verifySignature(PublicKey key) { return signature.length>0 && CryptoBox.verifyQuiet(key,unsignedBytes(),signature); }
    public byte[] unsignedBytes() {
        try { ByteArrayOutputStream b=new ByteArrayOutputStream(); DataOutputStream d=new DataOutputStream(b);
            d.writeInt(protocolVersion); ws(d,transferId); ws(d,senderDeviceId); ws(d,recipientDeviceId);
            d.writeLong(createdAtEpochMs); d.writeLong(expiresAtEpochMs); d.writeInt(payloadType.ordinal()); ws(d,payloadHashHex);
            wb(d,nonce); wb(d,payload); d.writeInt(transport.ordinal()); d.writeInt(retryCount); d.flush(); return b.toByteArray();
        } catch(IOException e){ throw new IllegalStateException(e); }
    }
    private static void ws(DataOutputStream d,String s)throws IOException{ byte[] x=nz(s).getBytes(StandardCharsets.UTF_8); d.writeInt(x.length); d.write(x); }
    private static void wb(DataOutputStream d,byte[] x)throws IOException{ byte[] y=copy(x); d.writeInt(y.length); d.write(y); }
    private static String nz(String s){ return s==null?"":s; }
    private static byte[] copy(byte[] b){ return b==null?new byte[0]:Arrays.copyOf(b,b.length); }
}
