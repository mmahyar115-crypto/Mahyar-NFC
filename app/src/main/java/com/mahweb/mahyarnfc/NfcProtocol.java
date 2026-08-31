package com.mahweb.mahyarnfc;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class NfcProtocol {
    public static final String AID_HEX = "F0010203040506";
    public static final byte[] SELECT_APDU = Hex.fromHex("00A4040007" + AID_HEX);
    public static final byte[] SW_OK = Hex.fromHex("9000");
    public static final byte[] SW_NOT_FOUND = Hex.fromHex("6A82");
    public static final byte[] SW_CONDITIONS = Hex.fromHex("6985");
    public static final byte[] SW_WRONG_OFFSET = Hex.fromHex("6B00");
    public static final byte[] MAGIC = "MNF1".getBytes(StandardCharsets.US_ASCII);
    public static final int CHUNK_SIZE = 220;

    private NfcProtocol() {}

    public static byte[] selectResponse(int length) {
        ByteBuffer b = ByteBuffer.allocate(MAGIC.length + 4 + 2);
        b.put(MAGIC);
        b.putInt(length);
        b.put(SW_OK);
        return b.array();
    }

    public static boolean endsWithOk(byte[] response) {
        int n = response == null ? 0 : response.length;
        return n >= 2 && response[n - 2] == (byte) 0x90 && response[n - 1] == 0x00;
    }

    public static byte[] body(byte[] response) {
        return Arrays.copyOf(response, response.length - 2);
    }

    public static byte[] readBinaryApdu(int offset) {
        return new byte[] {
                0x00,
                (byte) 0xB0,
                (byte) ((offset >> 8) & 0xFF),
                (byte) (offset & 0xFF),
                0x00
        };
    }
}
