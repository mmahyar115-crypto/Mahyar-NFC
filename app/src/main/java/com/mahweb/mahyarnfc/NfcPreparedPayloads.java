package com.mahweb.mahyarnfc;

import android.content.Context;

import org.json.JSONException;

import java.nio.charset.StandardCharsets;

/**
 * Prepare APDU response bytes before placing the HCE service in foreground.
 * Contents stay in process memory only, and are cleared on share stop/edit.
 * A cold-started HCE service can rebuild the snapshot as a fallback.
 */
public final class NfcPreparedPayloads {
    public static final class Snapshot {
        final byte[] privatePayload;
        final byte[] ndefMessage; // null if profile exceeds Universal limits

        private Snapshot(byte[] direct, byte[] universal) {
            privatePayload = direct;
            ndefMessage = universal;
        }
    }

    private static volatile Snapshot cached;

    private NfcPreparedPayloads() {}

    public static synchronized Snapshot prepare(Context context) throws JSONException {
        Profile profile = ProfileRepository.load(context);
        if (!ProfileValidator.isProfileReady(profile)) {
            throw new IllegalStateException("Contact profile is incomplete");
        }

        byte[] direct = profile.toJson().toString().getBytes(StandardCharsets.UTF_8);
        if (direct.length == 0 || direct.length > 65535) {
            throw new IllegalArgumentException("Direct NFC profile is too large");
        }

        byte[] universal;
        try {
            universal = UniversalCardPayload.buildNdefMessage(
                    profile, UniversalCardPayload.DEFAULT_BRIDGE_BASE_URL);
        } catch (IllegalArgumentException oversizedUniversal) {
            // Direct Android-to-Android transfer remains available.
            universal = null;
        }

        Snapshot next = new Snapshot(direct, universal);
        cached = next;
        return next;
    }

    public static Snapshot getOrPrepare(Context context) throws JSONException {
        Snapshot snapshot = cached;
        if (snapshot != null) return snapshot;
        synchronized (NfcPreparedPayloads.class) {
            snapshot = cached;
            return snapshot != null ? snapshot : prepare(context);
        }
    }

    public static synchronized void clear() {
        cached = null;
    }
}
