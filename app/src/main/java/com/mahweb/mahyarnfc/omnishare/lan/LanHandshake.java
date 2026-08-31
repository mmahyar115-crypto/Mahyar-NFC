package com.mahweb.mahyarnfc.omnishare.lan;

import com.mahweb.mahyarnfc.omnishare.crypto.CryptoBox;
import com.mahweb.mahyarnfc.omnishare.identity.DeviceIdentity;
import com.mahweb.mahyarnfc.omnishare.identity.IdentityStore;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;

public final class LanHandshake {
    private static final byte[] INFO = "mahyar-omnishare-lan-v1".getBytes(StandardCharsets.UTF_8);
    private LanHandshake() {}

    public static final class Session {
        public final String peerDeviceId;
        public final PublicKey peerIdentityKey;
        public final String peerFingerprint;
        public final String verificationCode;
        public final byte[] sessionKey;
        Session(String id, PublicKey key, byte[] sk) {
            peerDeviceId = id;
            peerIdentityKey = key;
            peerFingerprint = CryptoBox.fingerprint(key);
            verificationCode = pairingCode(sk);
            sessionKey = Arrays.copyOf(sk, sk.length);
        }
    }

    private static final class Hello {
        String deviceId, identityKeyB64, ephemeralKeyB64;
        byte[] nonce;
    }
    private static final class Challenge {
        String deviceId, identityKeyB64, ephemeralKeyB64;
        byte[] nonce, signature;
    }

    public static Session client(InputStream in, OutputStream out, IdentityStore identity) throws Exception {
        DeviceIdentity own = identity.getOrCreate();
        KeyPair eph = CryptoBox.generateEphemeralEcKeyPair();
        Hello h = new Hello();
        h.deviceId = own.deviceId;
        h.identityKeyB64 = own.publicKeyB64;
        h.ephemeralKeyB64 = CryptoBox.publicKeyB64(eph.getPublic());
        h.nonce = CryptoBox.randomBytes(16);
        FrameCodec.write(out, LanProtocol.HELLO, encodeHello(h));

        Frame f = FrameCodec.read(in);
        if (f.type == LanProtocol.ERROR) throw new IOException("LAN server error: " + new String(f.body, StandardCharsets.UTF_8));
        if (f.type != LanProtocol.CHALLENGE) throw new IOException("Expected CHALLENGE");
        Challenge c = decodeChallenge(f.body);
        byte[] transcript = transcript(h, c);
        PublicKey peerIdentity = CryptoBox.decodeEcPublicKey(c.identityKeyB64);
        if (!CryptoBox.verify(peerIdentity, append(transcript, "SERVER-AUTH"), c.signature)) throw new GeneralSecurityException("Invalid server identity signature");

        byte[] clientSig = identity.sign(append(transcript, "CLIENT-AUTH"));
        FrameCodec.write(out, LanProtocol.AUTH, encodeBytes(clientSig));
        Frame ready = FrameCodec.read(in);
        if (ready.type == LanProtocol.ERROR) throw new IOException("LAN auth rejected: " + new String(ready.body, StandardCharsets.UTF_8));
        if (ready.type != LanProtocol.READY) throw new IOException("Expected READY");

        PublicKey peerEph = CryptoBox.decodeEcPublicKey(c.ephemeralKeyB64);
        byte[] secret = CryptoBox.deriveSharedSecret(eph.getPrivate(), peerEph);
        byte[] salt = CryptoBox.concat(h.nonce, c.nonce);
        byte[] key = CryptoBox.hkdfSha256(secret, salt, INFO, 32);
        return new Session(c.deviceId, peerIdentity, key);
    }

    public static Session server(InputStream in, OutputStream out, IdentityStore identity) throws Exception {
        Frame f = FrameCodec.read(in);
        if (f.type != LanProtocol.HELLO) throw new IOException("Expected HELLO");
        Hello h = decodeHello(f.body);
        PublicKey peerIdentity = CryptoBox.decodeEcPublicKey(h.identityKeyB64);
        PublicKey peerEph = CryptoBox.decodeEcPublicKey(h.ephemeralKeyB64);

        DeviceIdentity own = identity.getOrCreate();
        KeyPair eph = CryptoBox.generateEphemeralEcKeyPair();
        Challenge c = new Challenge();
        c.deviceId = own.deviceId;
        c.identityKeyB64 = own.publicKeyB64;
        c.ephemeralKeyB64 = CryptoBox.publicKeyB64(eph.getPublic());
        c.nonce = CryptoBox.randomBytes(16);
        byte[] transcript = transcript(h, c);
        c.signature = identity.sign(append(transcript, "SERVER-AUTH"));
        FrameCodec.write(out, LanProtocol.CHALLENGE, encodeChallenge(c));

        Frame auth = FrameCodec.read(in);
        if (auth.type != LanProtocol.AUTH) throw new IOException("Expected AUTH");
        byte[] clientSig = decodeBytes(auth.body, 4096);
        if (!CryptoBox.verify(peerIdentity, append(transcript, "CLIENT-AUTH"), clientSig)) {
            FrameCodec.write(out, LanProtocol.ERROR, "INVALID_SIGNATURE".getBytes(StandardCharsets.UTF_8));
            throw new GeneralSecurityException("Invalid client identity signature");
        }

        byte[] secret = CryptoBox.deriveSharedSecret(eph.getPrivate(), peerEph);
        byte[] salt = CryptoBox.concat(h.nonce, c.nonce);
        byte[] key = CryptoBox.hkdfSha256(secret, salt, INFO, 32);
        FrameCodec.write(out, LanProtocol.READY, new byte[]{1});
        return new Session(h.deviceId, peerIdentity, key);
    }

    private static String pairingCode(byte[] sessionKey) {
        String hex = CryptoBox.sha256Hex(CryptoBox.concat(sessionKey, "pairing-code-v1".getBytes(StandardCharsets.UTF_8)));
        long value = Long.parseUnsignedLong(hex.substring(0, 8), 16) % 1_000_000L;
        return String.format(java.util.Locale.US, "%06d", value);
    }

    private static byte[] transcript(Hello h, Challenge c) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream d = new DataOutputStream(b);
            d.writeInt(LanProtocol.VERSION);
            ws(d, h.deviceId); ws(d, h.identityKeyB64); ws(d, h.ephemeralKeyB64); wb(d, h.nonce);
            ws(d, c.deviceId); ws(d, c.identityKeyB64); ws(d, c.ephemeralKeyB64); wb(d, c.nonce);
            d.flush(); return b.toByteArray();
        } catch (IOException e) { throw new IllegalStateException(e); }
    }
    private static byte[] append(byte[] a, String suffix) { return CryptoBox.concat(a, suffix.getBytes(StandardCharsets.UTF_8)); }
    private static byte[] encodeHello(Hello h) throws IOException { ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(b);d.writeInt(LanProtocol.VERSION);ws(d,h.deviceId);ws(d,h.identityKeyB64);ws(d,h.ephemeralKeyB64);wb(d,h.nonce);d.flush();return b.toByteArray(); }
    private static Hello decodeHello(byte[] x) throws IOException { DataInputStream d=new DataInputStream(new ByteArrayInputStream(x));if(d.readInt()!=LanProtocol.VERSION)throw new IOException("protocol mismatch");Hello h=new Hello();h.deviceId=rs(d);h.identityKeyB64=rs(d);h.ephemeralKeyB64=rs(d);h.nonce=rb(d,64);if(d.available()!=0)throw new IOException("hello trailing bytes");return h; }
    private static byte[] encodeChallenge(Challenge c) throws IOException { ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(b);d.writeInt(LanProtocol.VERSION);ws(d,c.deviceId);ws(d,c.identityKeyB64);ws(d,c.ephemeralKeyB64);wb(d,c.nonce);wb(d,c.signature);d.flush();return b.toByteArray(); }
    private static Challenge decodeChallenge(byte[] x) throws IOException { DataInputStream d=new DataInputStream(new ByteArrayInputStream(x));if(d.readInt()!=LanProtocol.VERSION)throw new IOException("protocol mismatch");Challenge c=new Challenge();c.deviceId=rs(d);c.identityKeyB64=rs(d);c.ephemeralKeyB64=rs(d);c.nonce=rb(d,64);c.signature=rb(d,4096);if(d.available()!=0)throw new IOException("challenge trailing bytes");return c; }
    private static byte[] encodeBytes(byte[] x)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(b);wb(d,x);d.flush();return b.toByteArray();}
    private static byte[] decodeBytes(byte[] x,int max)throws IOException{DataInputStream d=new DataInputStream(new ByteArrayInputStream(x));byte[]b=rb(d,max);if(d.available()!=0)throw new IOException("trailing bytes");return b;}
    private static void ws(DataOutputStream d,String s)throws IOException{byte[]b=(s==null?"":s).getBytes(StandardCharsets.UTF_8);if(b.length>8192)throw new IOException("string too long");d.writeShort(b.length);d.write(b);} 
    private static String rs(DataInputStream d)throws IOException{int n=d.readUnsignedShort();if(n>8192)throw new IOException("string too long");byte[]b=new byte[n];d.readFully(b);return new String(b,StandardCharsets.UTF_8);} 
    private static void wb(DataOutputStream d,byte[]b)throws IOException{byte[]x=b==null?new byte[0]:b;d.writeShort(x.length);d.write(x);} 
    private static byte[] rb(DataInputStream d,int max)throws IOException{int n=d.readUnsignedShort();if(n>max)throw new IOException("byte field too long");byte[]b=new byte[n];d.readFully(b);return b;}
}
