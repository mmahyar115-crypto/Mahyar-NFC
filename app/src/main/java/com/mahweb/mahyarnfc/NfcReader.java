package com.mahweb.mahyarnfc;

import android.app.Activity;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class NfcReader implements NfcAdapter.ReaderCallback {
    public interface Listener {
        void onReading();
        void onProfile(Profile profile);
        void onError(String message);
    }

    private final Activity activity;
    private final NfcAdapter adapter;
    private final Listener listener;
    private final NfcReadGate readGate = new NfcReadGate();

    public NfcReader(Activity activity, Listener listener) {
        this.activity = activity;
        this.adapter = NfcAdapter.getDefaultAdapter(activity);
        this.listener = listener;
    }

    public boolean isAvailable() {
        return adapter != null;
    }

    public boolean isEnabled() {
        return adapter != null && adapter.isEnabled();
    }

    public void start() {
        if (adapter == null) {
            listener.onError("این گوشی NFC ندارد.");
            return;
        }
        if (!adapter.isEnabled()) {
            listener.onError("NFC خاموش است. ابتدا NFC را روشن کنید.");
            return;
        }

        Bundle options = new Bundle();
        options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);
        int flags = NfcAdapter.FLAG_READER_NFC_A
                | NfcAdapter.FLAG_READER_NFC_B
                | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK;
        readGate.start();
        try {
            adapter.enableReaderMode(activity, this, flags, options);
        } catch (RuntimeException e) {
            readGate.stop();
            listener.onError("حالت دریافت NFC فعال نشد. دوباره تلاش کنید.");
        }
    }

    public void stop() {
        readGate.stop();
        if (adapter != null) {
            try {
                adapter.disableReaderMode(activity);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        final int session = readGate.tryBegin();
        if (session == NfcReadGate.REJECTED) return;
        final long startedNs = SystemClock.elapsedRealtimeNanos();
        runUi(session, listener::onReading);
        IsoDep isoDep = IsoDep.get(tag);
        if (isoDep == null) {
            runUi(session, () -> listener.onError("دستگاه مقابل از پروتکل NFC موردنیاز پشتیبانی نکرد."));
            readGate.finish(session);
            return;
        }

        try {
            isoDep.connect();
            isoDep.setTimeout(4000);

            byte[] selectResponse = isoDep.transceive(NfcProtocol.SELECT_APDU);
            if (!NfcProtocol.endsWithOk(selectResponse)) {
                String status = statusWord(selectResponse);
                if ("6985".equals(status)) {
                    throw new IllegalStateException("حالت اشتراک‌گذاری در گوشی مقابل فعال نیست.");
                }
                throw new IllegalStateException("پروفایل NFC شناسایی نشد. کد: " + status);
            }

            byte[] selectBody = NfcProtocol.body(selectResponse);
            if (selectBody.length < 8 || !Arrays.equals(Arrays.copyOfRange(selectBody, 0, 4), NfcProtocol.MAGIC)) {
                throw new IllegalStateException("فرمت اطلاعات NFC معتبر نیست.");
            }

            int totalLength = ByteBuffer.wrap(selectBody, 4, 4).getInt();
            if (totalLength <= 0 || totalLength > 16384) {
                throw new IllegalStateException("حجم اطلاعات NFC نامعتبر است.");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream(totalLength);
            int offset = 0;
            while (offset < totalLength) {
                byte[] response = isoDep.transceive(NfcProtocol.readBinaryApdu(offset));
                if (!NfcProtocol.endsWithOk(response)) {
                    throw new IllegalStateException("خواندن NFC در میانه انتقال متوقف شد.");
                }
                byte[] chunk = NfcProtocol.body(response);
                if (chunk.length == 0) {
                    throw new IllegalStateException("بخشی از اطلاعات NFC دریافت نشد.");
                }
                int take = Math.min(chunk.length, totalLength - offset);
                out.write(chunk, 0, take);
                offset += take;
            }

            Profile profile = Profile.fromJson(new String(out.toByteArray(), StandardCharsets.UTF_8));
            if (readGate.markSuccess(session)) {
                if ((activity.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                    long elapsedMs = (SystemClock.elapsedRealtimeNanos() - startedNs) / 1_000_000L;
                    Log.d("MahyarNFC", "Direct NFC read completed: elapsedMs=" + elapsedMs
                            + ", bytes=" + totalLength);
                }
                runUi(session, () -> listener.onProfile(profile));
            }
        } catch (Exception e) {
            runUi(session, () -> listener.onError(
                    e.getMessage() == null ? "خطا در خواندن NFC" : e.getMessage()));
        } finally {
            try {
                isoDep.close();
            } catch (Exception ignored) {
            }
            readGate.finish(session);
        }
    }

    private void runUi(int session, Runnable callback) {
        activity.runOnUiThread(() -> {
            if (readGate.isCurrent(session)) callback.run();
        });
    }

    private static String statusWord(byte[] response) {
        if (response == null || response.length < 2) return "----";
        int n = response.length;
        return String.format("%02X%02X", response[n - 2] & 0xFF, response[n - 1] & 0xFF);
    }
}
