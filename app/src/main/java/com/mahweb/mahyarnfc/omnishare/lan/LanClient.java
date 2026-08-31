package com.mahweb.mahyarnfc.omnishare.lan;

import com.mahweb.mahyarnfc.omnishare.TransferEnvelope;
import com.mahweb.mahyarnfc.omnishare.TransferEnvelopeCodec;
import com.mahweb.mahyarnfc.omnishare.crypto.CryptoBox;
import com.mahweb.mahyarnfc.omnishare.crypto.CryptoEnvelope;
import com.mahweb.mahyarnfc.omnishare.identity.IdentityStore;
import com.mahweb.mahyarnfc.omnishare.transport.DeliveryAck;
import com.mahweb.mahyarnfc.omnishare.transport.DeliveryAckValidator;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class LanClient {
    private static final byte[] ENVELOPE_AAD = "mahyar-lan-envelope-v1".getBytes(StandardCharsets.UTF_8);
    private static final byte[] ACK_AAD = "mahyar-lan-ack-v1".getBytes(StandardCharsets.UTF_8);
    private final IdentityStore identity;
    private final LanPeerVerifier verifier;

    public LanClient(IdentityStore identity) { this(identity, null); }
    public LanClient(IdentityStore identity, LanPeerVerifier verifier) { this.identity = identity; this.verifier = verifier; }

    public Result send(LanPeer peer, TransferEnvelope envelope) throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(peer.host, peer.port), 5_000);
            socket.setSoTimeout(30_000);
            socket.setTcpNoDelay(true);
            InputStream in = new BufferedInputStream(socket.getInputStream());
            OutputStream out = new BufferedOutputStream(socket.getOutputStream());

            LanHandshake.Session session = LanHandshake.client(in, out, identity);
            if (verifier != null && !verifier.approvePeer(session.peerDeviceId, session.peerFingerprint, session.verificationCode)) {
                throw new SecurityException("Peer verification rejected");
            }
            if (!envelope.recipientDeviceId.isEmpty() && !envelope.recipientDeviceId.equals(session.peerDeviceId)) {
                throw new SecurityException("Resolved LAN peer identity does not match recipient");
            }

            TransferEnvelope signed = envelope.signature.length == 0
                    ? envelope.withSignature(identity.sign(envelope.unsignedBytes()))
                    : envelope;
            CryptoEnvelope box = CryptoBox.encryptAesGcm(session.sessionKey, TransferEnvelopeCodec.encode(signed), ENVELOPE_AAD);
            FrameCodec.write(out, LanProtocol.ENVELOPE, SecurePacketCodec.encode(box));

            Frame f = FrameCodec.read(in);
            if (f.type == LanProtocol.ERROR) throw new IOException("LAN delivery rejected: " + new String(f.body, StandardCharsets.UTF_8));
            if (f.type != LanProtocol.ACK) throw new IOException("Expected ACK");
            CryptoEnvelope ackBox = SecurePacketCodec.decode(f.body);
            byte[] ackPlain = CryptoBox.decryptAesGcm(session.sessionKey, ackBox, ACK_AAD);
            DeliveryAck ack = DeliveryAckCodec.decode(ackPlain);
            if (!DeliveryAckValidator.valid(ack, signed.transferId, session.peerDeviceId, session.peerIdentityKey)) {
                throw new SecurityException("Invalid signed LAN acknowledgement");
            }
            return new Result(session.peerDeviceId, session.peerFingerprint, ack);
        }
    }

    public static final class Result {
        public final String peerDeviceId, peerFingerprint;
        public final DeliveryAck ack;
        public Result(String id, String fp, DeliveryAck ack) { this.peerDeviceId=id; this.peerFingerprint=fp; this.ack=ack; }
    }
}
