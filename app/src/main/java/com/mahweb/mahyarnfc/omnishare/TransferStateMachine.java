package com.mahweb.mahyarnfc.omnishare;
import java.util.EnumMap;
import java.util.EnumSet;
public final class TransferStateMachine {
    private static final EnumMap<TransferState, EnumSet<TransferState>> ALLOWED = new EnumMap<>(TransferState.class);
    static {
        ALLOWED.put(TransferState.CREATED, EnumSet.of(TransferState.DISCOVERING, TransferState.CANCELLED, TransferState.EXPIRED));
        ALLOWED.put(TransferState.DISCOVERING, EnumSet.of(TransferState.NEGOTIATING, TransferState.FAILED_RETRYABLE, TransferState.FAILED_FINAL, TransferState.CANCELLED, TransferState.EXPIRED));
        ALLOWED.put(TransferState.NEGOTIATING, EnumSet.of(TransferState.AUTHENTICATING, TransferState.FAILED_RETRYABLE, TransferState.FAILED_FINAL, TransferState.CANCELLED, TransferState.EXPIRED));
        ALLOWED.put(TransferState.AUTHENTICATING, EnumSet.of(TransferState.READY, TransferState.FAILED_RETRYABLE, TransferState.FAILED_FINAL, TransferState.CANCELLED, TransferState.EXPIRED));
        ALLOWED.put(TransferState.READY, EnumSet.of(TransferState.SENDING, TransferState.CANCELLED, TransferState.EXPIRED));
        ALLOWED.put(TransferState.SENDING, EnumSet.of(TransferState.WAITING_ACK, TransferState.FAILED_RETRYABLE, TransferState.FAILED_FINAL, TransferState.CANCELLED, TransferState.EXPIRED));
        ALLOWED.put(TransferState.WAITING_ACK, EnumSet.of(TransferState.DELIVERED, TransferState.FAILED_RETRYABLE, TransferState.FAILED_FINAL, TransferState.CANCELLED, TransferState.EXPIRED));
        ALLOWED.put(TransferState.FAILED_RETRYABLE, EnumSet.of(TransferState.DISCOVERING, TransferState.CANCELLED, TransferState.EXPIRED));
    }
    private TransferState state;
    public TransferStateMachine() { this(TransferState.CREATED); }
    private TransferStateMachine(TransferState state) { this.state = state; }
    public static TransferStateMachine at(TransferState state) { return new TransferStateMachine(state); }
    public synchronized TransferState current() { return state; }
    public synchronized void moveTo(TransferState next) {
        EnumSet<TransferState> allowed = ALLOWED.get(state);
        if (allowed == null || !allowed.contains(next)) throw new IllegalStateException("Invalid OmniShare transition " + state + " -> " + next);
        state = next;
    }
    public synchronized boolean isTerminal() { return !ALLOWED.containsKey(state); }
}
