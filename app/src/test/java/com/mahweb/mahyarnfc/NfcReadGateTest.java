package com.mahweb.mahyarnfc;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class NfcReadGateTest {
    @Test public void duplicateCallbacksOnlyAdmitOneReader() {
        NfcReadGate gate = new NfcReadGate();
        gate.start();
        int session = gate.tryBegin();
        assertTrue(session >= 0);
        assertEquals(NfcReadGate.REJECTED, gate.tryBegin());
        assertTrue(gate.markSuccess(session));
        gate.finish(session);
        assertEquals(NfcReadGate.REJECTED, gate.tryBegin());
    }

    @Test public void failedReadCanBeRetried() {
        NfcReadGate gate = new NfcReadGate();
        gate.start();
        int first = gate.tryBegin();
        gate.finish(first);
        int second = gate.tryBegin();
        assertEquals(first, second);
        assertTrue(gate.markSuccess(second));
        assertFalse(gate.markSuccess(second));
    }

    @Test public void staleReadCannotCompleteNewSession() {
        NfcReadGate gate = new NfcReadGate();
        gate.start();
        int previous = gate.tryBegin();
        gate.stop();
        gate.start();
        int fresh = gate.tryBegin();
        assertNotEquals(previous, fresh);
        gate.finish(previous); // Old callback must not release the new reader.
        assertEquals(NfcReadGate.REJECTED, gate.tryBegin());
        assertFalse(gate.markSuccess(previous));
        assertTrue(gate.markSuccess(fresh));
    }

    @Test public void stoppedGateRejectsCallbacks() {
        NfcReadGate gate = new NfcReadGate();
        gate.start();
        int session = gate.tryBegin();
        gate.stop();
        assertFalse(gate.isCurrent(session));
        assertFalse(gate.markSuccess(session));
        assertEquals(NfcReadGate.REJECTED, gate.tryBegin());
    }

    @Test public void concurrentTagCallbacksAdmitAtMostOne() throws Exception {
        final NfcReadGate gate = new NfcReadGate();
        gate.start();
        final int threads = 12;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger accepted = new AtomicInteger();
        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                ready.countDown();
                try {
                    if (go.await(2, TimeUnit.SECONDS)
                            && gate.tryBegin() != NfcReadGate.REJECTED) accepted.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
            t.start();
        }
        assertTrue(ready.await(2, TimeUnit.SECONDS));
        go.countDown();
        assertTrue(done.await(3, TimeUnit.SECONDS));
        assertEquals(1, accepted.get());
    }
}
