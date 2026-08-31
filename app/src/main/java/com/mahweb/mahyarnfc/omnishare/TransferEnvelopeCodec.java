package com.mahweb.mahyarnfc.omnishare;
import java.io.*;
import java.nio.charset.StandardCharsets;
public final class TransferEnvelopeCodec {
    private static final int MAX_STRING=8192, MAX_BYTES=65536;
    private TransferEnvelopeCodec(){}
    public static byte[] encode(TransferEnvelope e){
        try { ByteArrayOutputStream b=new ByteArrayOutputStream(); DataOutputStream d=new DataOutputStream(b);
            d.writeInt(e.protocolVersion); ws(d,e.transferId); ws(d,e.senderDeviceId); ws(d,e.recipientDeviceId); d.writeLong(e.createdAtEpochMs); d.writeLong(e.expiresAtEpochMs);
            d.writeInt(e.payloadType.ordinal()); ws(d,e.payloadHashHex); wb(d,e.nonce); wb(d,e.payload); wb(d,e.signature); d.writeInt(e.transport.ordinal()); d.writeInt(e.retryCount); d.flush(); return b.toByteArray();
        } catch(IOException ex){throw new IllegalStateException(ex);} }
    public static TransferEnvelope decode(byte[] data)throws IOException{
        DataInputStream d=new DataInputStream(new ByteArrayInputStream(data)); int pv=d.readInt(); String tid=rs(d),sid=rs(d),rid=rs(d); long c=d.readLong(),x=d.readLong();
        int pt=d.readInt(); String hash=rs(d); byte[] nonce=rb(d),payload=rb(d),sig=rb(d); int tk=d.readInt(),retry=d.readInt();
        if(d.available()!=0) throw new IOException("Trailing envelope bytes");
        if(pt<0||pt>=PayloadType.values().length||tk<0||tk>=TransportKind.values().length) throw new IOException("Invalid enum");
        return new TransferEnvelope(pv,tid,sid,rid,c,x,PayloadType.values()[pt],hash,nonce,payload,sig,TransportKind.values()[tk],retry);
    }
    private static void ws(DataOutputStream d,String s)throws IOException{byte[] b=(s==null?"":s).getBytes(StandardCharsets.UTF_8);d.writeInt(b.length);d.write(b);} 
    private static String rs(DataInputStream d)throws IOException{int n=d.readInt();if(n<0||n>MAX_STRING)throw new IOException("String too large");byte[] b=new byte[n];d.readFully(b);return new String(b,StandardCharsets.UTF_8);} 
    private static void wb(DataOutputStream d,byte[] b)throws IOException{byte[] x=b==null?new byte[0]:b;d.writeInt(x.length);d.write(x);} 
    private static byte[] rb(DataInputStream d)throws IOException{int n=d.readInt();if(n<0||n>MAX_BYTES)throw new IOException("Bytes too large");byte[] b=new byte[n];d.readFully(b);return b;}
}
