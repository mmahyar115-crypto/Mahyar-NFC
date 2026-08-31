package com.mahweb.mahyarnfc.omnishare.lan;

import com.mahweb.mahyarnfc.omnishare.TransferEnvelope;
import com.mahweb.mahyarnfc.omnishare.TransferEnvelopeCodec;
import com.mahweb.mahyarnfc.omnishare.crypto.CryptoBox;
import com.mahweb.mahyarnfc.omnishare.crypto.CryptoEnvelope;
import com.mahweb.mahyarnfc.omnishare.identity.IdentityStore;
import com.mahweb.mahyarnfc.omnishare.transfer.TransferLedger;
import com.mahweb.mahyarnfc.omnishare.transport.DeliveryAck;
import com.mahweb.mahyarnfc.omnishare.trust.TrustPolicy;
import com.mahweb.mahyarnfc.omnishare.trust.TrustLookup;
import com.mahweb.mahyarnfc.omnishare.trust.TrustedDevice;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LanServer implements AutoCloseable {
    private static final byte[] ENVELOPE_AAD = "mahyar-lan-envelope-v1".getBytes(StandardCharsets.UTF_8);
    private static final byte[] ACK_AAD = "mahyar-lan-ack-v1".getBytes(StandardCharsets.UTF_8);

    private final IdentityStore identity;
    private final TrustLookup trust;
    private final TransferLedger ledger;
    private final LanReceivePolicy policy;
    private final ExecutorService clients = Executors.newFixedThreadPool(4);
    private volatile boolean running;
    private ServerSocket server;
    private Thread acceptThread;

    public LanServer(IdentityStore identity, TrustLookup trust, TransferLedger ledger, LanReceivePolicy policy) {
        this.identity = identity; this.trust = trust; this.ledger = ledger; this.policy = policy;
    }

    public synchronized int start() throws IOException {
        if (running) return server.getLocalPort();
        server = new ServerSocket(0);
        server.setReuseAddress(true);
        running = true;
        acceptThread = new Thread(this::acceptLoop, "mahyar-lan-accept");
        acceptThread.start();
        return server.getLocalPort();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = server.accept();
                clients.execute(() -> handle(socket));
            } catch (IOException e) {
                if (running) { /* next loop or close */ }
            }
        }
    }

    private void handle(Socket socket) {
        try (Socket s = socket) {
            s.setSoTimeout(30_000);
            s.setTcpNoDelay(true);
            InputStream in = new BufferedInputStream(s.getInputStream());
            OutputStream out = new BufferedOutputStream(s.getOutputStream());
            LanHandshake.Session session = LanHandshake.server(in, out, identity);

            TrustedDevice known = trust.get(session.peerDeviceId);
            if (TrustPolicy.isBlocked(known)) {
                error(out, "TRUST_BLOCKED");
                if (policy != null) policy.onRejected(session.peerDeviceId, session.peerFingerprint, "TRUST_BLOCKED");
                return;
            }
            if (known != null && !known.fingerprint.equals(session.peerFingerprint)) {
                error(out, "ERROR_IDENTITY_CHANGED");
                if (policy != null) policy.onRejected(session.peerDeviceId, session.peerFingerprint, "ERROR_IDENTITY_CHANGED");
                return;
            }

            Frame frame = FrameCodec.read(in);
            if (frame.type != LanProtocol.ENVELOPE) { error(out, "EXPECTED_ENVELOPE"); return; }
            CryptoEnvelope secure = SecurePacketCodec.decode(frame.body);
            byte[] plain = CryptoBox.decryptAesGcm(session.sessionKey, secure, ENVELOPE_AAD);
            TransferEnvelope envelope = TransferEnvelopeCodec.decode(plain);

            if (!envelope.senderDeviceId.equals(session.peerDeviceId)) { error(out, "SENDER_ID_MISMATCH"); return; }
            String myId = identity.getOrCreate().deviceId;
            if (!envelope.recipientDeviceId.isEmpty() && !envelope.recipientDeviceId.equals(myId)) { error(out, "RECIPIENT_MISMATCH"); return; }
            if (!envelope.verifySignature(session.peerIdentityKey)) { error(out, "INVALID_ENVELOPE_SIGNATURE"); return; }

            TransferLedger.AcceptDecision decision = ledger.evaluate(envelope, System.currentTimeMillis());
            if (decision == TransferLedger.AcceptDecision.REJECT_EXPIRED) { error(out, "ENVELOPE_EXPIRED"); return; }
            if (decision == TransferLedger.AcceptDecision.REJECT_REPLAY) { error(out, "REPLAY_REJECTED"); return; }
            if (decision == TransferLedger.AcceptDecision.DUPLICATE_ALREADY_DELIVERED) {
                sendAck(out, session, envelope.transferId, myId);
                return;
            }

            boolean auto = TrustPolicy.canAutoReceive(known, session.peerDeviceId, session.peerFingerprint);
            boolean approved = auto || (policy != null && policy.approve(session.peerDeviceId, session.peerFingerprint, session.verificationCode, envelope));
            if (!approved) {
                error(out, "APPROVAL_REQUIRED");
                if (policy != null) policy.onRejected(session.peerDeviceId, session.peerFingerprint, "APPROVAL_REQUIRED");
                return;
            }

            if (policy != null) policy.onReceived(session.peerDeviceId, session.peerFingerprint, envelope);
            ledger.markDelivered(envelope.transferId, System.currentTimeMillis());
            sendAck(out, session, envelope.transferId, myId);
        } catch (Exception ignored) { }
    }

    private void sendAck(OutputStream out, LanHandshake.Session session, String transferId, String myId) throws Exception {
        DeliveryAck ack = new DeliveryAck(transferId, myId, System.currentTimeMillis(), new byte[0]);
        ack = ack.withSignature(identity.sign(ack.unsignedBytes()));
        CryptoEnvelope box = CryptoBox.encryptAesGcm(session.sessionKey, DeliveryAckCodec.encode(ack), ACK_AAD);
        FrameCodec.write(out, LanProtocol.ACK, SecurePacketCodec.encode(box));
    }

    private static void error(OutputStream out, String code) {
        try { FrameCodec.write(out, LanProtocol.ERROR, code.getBytes(StandardCharsets.UTF_8)); } catch (IOException ignored) { }
    }

    public synchronized void close() {
        running = false;
        try { if (server != null) server.close(); } catch (IOException ignored) { }
        clients.shutdownNow();
        if (acceptThread != null) acceptThread.interrupt();
    }
}
