package com.mahweb.mahyarnfc;

import java.lang.ref.WeakReference;

/** Lightweight in-process events from the HCE service to the visible send screen. */
public final class NfcShareEvents {
    public enum Protocol { MAHYAR_DIRECT, UNIVERSAL_NDEF }

    public interface Listener {
        void onReaderDetected(Protocol protocol);
        void onTransferComplete(Protocol protocol);
        void onFieldLost();
    }

    private static volatile WeakReference<Listener> listener = new WeakReference<>(null);

    private NfcShareEvents() {}

    public static void setListener(Listener value) {
        listener = new WeakReference<>(value);
    }

    public static void clearListener(Listener value) {
        Listener current = listener.get();
        if (current == value) listener = new WeakReference<>(null);
    }

    public static void notifyReaderDetected(Protocol protocol) {
        Listener l = listener.get();
        if (l != null) l.onReaderDetected(protocol);
    }

    public static void notifyTransferComplete(Protocol protocol) {
        Listener l = listener.get();
        if (l != null) l.onTransferComplete(protocol);
    }

    public static void notifyFieldLost() {
        Listener l = listener.get();
        if (l != null) l.onFieldLost();
    }
}
