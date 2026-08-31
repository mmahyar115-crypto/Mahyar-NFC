package com.mahweb.mahyarnfc;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Builds the standards-based NDEF payload served to iPhone and generic NFC readers. */
public final class UniversalCardPayload {
    public static final String DEFAULT_BRIDGE_BASE_URL = "https://mahyarmolavi.ir/nfc/";
    public static final int MAX_BRIDGE_URL_CHARS = 1800;
    public static final int MAX_NDEF_MESSAGE_BYTES = 4094;
    private static final byte TNF_WELL_KNOWN = 0x01;
    private static final byte TNF_MIME_MEDIA = 0x02;
    private static final byte[] TYPE_URI = new byte[]{0x55}; // "U"
    private static final byte HTTPS_PREFIX_CODE = 0x04;

    private UniversalCardPayload() {}

    public static String bridgeUrl(Profile p, String baseUrl) {
        if (p == null) throw new IllegalArgumentException("profile");
        if (baseUrl == null || !baseUrl.startsWith("https://")) {
            throw new IllegalArgumentException("Bridge URL must use HTTPS");
        }
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        byte[] json = compactJson(p).getBytes(StandardCharsets.UTF_8);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        return normalizedBase + "#c=" + token;
    }

    public static boolean isUniversalPayloadSupported(Profile p, String baseUrl) {
        try {
            String url = bridgeUrl(p, baseUrl);
            if (url.length() > MAX_BRIDGE_URL_CHARS) return false;
            return buildNdefMessageUnchecked(p, url).length <= MAX_NDEF_MESSAGE_BYTES;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static byte[] buildNdefMessage(Profile p, String baseUrl) {
        String url = bridgeUrl(p, baseUrl);
        if (url.length() > MAX_BRIDGE_URL_CHARS) {
            throw new IllegalArgumentException("Universal card URL is too large");
        }
        byte[] out = buildNdefMessageUnchecked(p, url);
        if (out.length > MAX_NDEF_MESSAGE_BYTES) {
            throw new IllegalArgumentException("Universal NDEF message is too large");
        }
        return out;
    }

    private static byte[] buildNdefMessageUnchecked(Profile p, String url) {
        byte[] uriRemainder = url.substring("https://".length()).getBytes(StandardCharsets.UTF_8);
        byte[] uriPayload = new byte[uriRemainder.length + 1];
        uriPayload[0] = HTTPS_PREFIX_CODE;
        System.arraycopy(uriRemainder, 0, uriPayload, 1, uriRemainder.length);

        byte[] first = record(true, false, TNF_WELL_KNOWN, TYPE_URI, uriPayload);
        byte[] second = record(false, true, TNF_MIME_MEDIA,
                "text/vcard".getBytes(StandardCharsets.US_ASCII),
                p.toVCard().getBytes(StandardCharsets.UTF_8));

        byte[] out = new byte[first.length + second.length];
        System.arraycopy(first, 0, out, 0, first.length);
        System.arraycopy(second, 0, out, first.length, second.length);
        return out;
    }

    private static byte[] record(boolean mb, boolean me, byte tnf, byte[] type, byte[] payload) {
        boolean shortRecord = payload.length <= 255;
        int flags = tnf & 0x07;
        if (mb) flags |= 0x80;
        if (me) flags |= 0x40;
        if (shortRecord) flags |= 0x10;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(flags);
        out.write(type.length & 0xFF);
        if (shortRecord) {
            out.write(payload.length & 0xFF);
        } else {
            out.write((payload.length >>> 24) & 0xFF);
            out.write((payload.length >>> 16) & 0xFF);
            out.write((payload.length >>> 8) & 0xFF);
            out.write(payload.length & 0xFF);
        }
        out.write(type, 0, type.length);
        out.write(payload, 0, payload.length);
        return out.toByteArray();
    }

    private static String compactJson(Profile p) {
        return "{" +
                "\"v\":1," +
                "\"n\":\"" + esc(p.name) + "\"," +
                "\"p\":\"" + esc(p.phone) + "\"," +
                "\"j\":\"" + esc(p.job) + "\"," +
                "\"c\":\"" + esc(p.company) + "\"," +
                "\"e\":\"" + esc(p.email) + "\"," +
                "\"w\":\"" + esc(p.website) + "\"," +
                "\"i\":\"" + esc(p.instagram) + "\"," +
                "\"t\":\"" + esc(p.telegram) + "\"" +
                "}";
    }

    private static String esc(String value) {
        if (value == null) return "";
        StringBuilder b = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\': b.append("\\\\"); break;
                case '"': b.append("\\\""); break;
                case '\b': b.append("\\b"); break;
                case '\f': b.append("\\f"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (ch < 0x20) b.append(String.format("\\u%04x", (int) ch));
                    else b.append(ch);
            }
        }
        return b.toString();
    }
}
