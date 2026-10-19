package com.mahweb.mahyarnfc;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe reader admission gate. A completed read is delivered at most once
 * per listening session; callbacks from prior sessions are ignored.
 */
public final class NfcReadGate {
    public static final int REJECTED = -1;
    private final AtomicInteger generation = new AtomicInteger();
    private final AtomicInteger readingGeneration = new AtomicInteger(REJECTED);
    private final AtomicBoolean delivered = new AtomicBoolean();
    private volatile boolean active;

    public synchronized void start() {
        active = false;
        generation.incrementAndGet();
        readingGeneration.set(REJECTED);
        delivered.set(false);
        active = true;
    }

    public synchronized void stop() {
        active = false;
        generation.incrementAndGet();
        readingGeneration.set(REJECTED);
    }

    public int tryBegin() {
        int candidate = generation.get();
        if (!active || delivered.get() ||
                !readingGeneration.compareAndSet(REJECTED, candidate)) return REJECTED;
        if (!isCurrent(candidate) || delivered.get()) {
            readingGeneration.compareAndSet(candidate, REJECTED);
            return REJECTED;
        }
        return candidate;
    }

    public boolean markSuccess(int session) {
        return isCurrent(session) && delivered.compareAndSet(false, true);
    }

    public boolean isCurrent(int session) {
        return active && generation.get() == session;
    }

    public void finish(int session) {
        readingGeneration.compareAndSet(session, REJECTED);
    }
}
