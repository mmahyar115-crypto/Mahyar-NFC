package com.mahweb.mahyarnfc.omnishare.lan;

import com.mahweb.mahyarnfc.omnishare.TransferEnvelope;
import com.mahweb.mahyarnfc.omnishare.transport.*;
import java.util.*;
import java.util.concurrent.*;

public final class MultiRecipientDispatcher implements AutoCloseable {
    private final ExecutorService pool;
    public MultiRecipientDispatcher(int maxConcurrency) {
        if (maxConcurrency < 1 || maxConcurrency > 16) throw new IllegalArgumentException("maxConcurrency");
        pool = Executors.newFixedThreadPool(maxConcurrency);
    }
    public Map<String, CompletableFuture<DeliveryAck>> sendAll(OmniTransport transport, List<Recipient> recipients, TransferEnvelope envelope) {
        LinkedHashMap<String, CompletableFuture<DeliveryAck>> result = new LinkedHashMap<>();
        int i=0;
        for (Recipient r : recipients) {
            String key = (r.deviceId == null || r.deviceId.isEmpty()) ? "recipient-"+(i++) : r.deviceId;
            CompletableFuture<DeliveryAck> f = CompletableFuture.supplyAsync(() -> {
                try { return transport.send(r, envelope); }
                catch (Exception e) { throw new CompletionException(e); }
            }, pool);
            result.put(key, f);
        }
        return result;
    }
    public void close() { pool.shutdownNow(); }
}
