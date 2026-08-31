package com.mahweb.mahyarnfc;

import java.util.Arrays;

/** Minimal NFC Forum Type 4 Tag NDEF application for HCE readers such as iPhone. */
public final class Type4NdefProtocol {
    public static final byte[] NDEF_AID = hex("D2760000850101");
    public static final int CC_FILE_ID = 0xE103;
    public static final int NDEF_FILE_ID = 0xE104;
    public static final int NDEF_FILE_SIZE = 4096;

    private static final byte[] SW_OK = hex("9000");
    private static final byte[] SW_FILE_NOT_FOUND = hex("6A82");
    private static final byte[] SW_COMMAND_NOT_ALLOWED = hex("6986");
    private static final byte[] SW_WRONG_OFFSET = hex("6B00");
    private static final byte[] SW_INS_NOT_SUPPORTED = hex("6D00");
    private static final byte[] SW_WRONG_DATA = hex("6A80");

    private static final byte[] CC_FILE = new byte[]{
            0x00, 0x0F,       // CCLEN = 15
            0x20,             // Mapping Version 2.0
            0x00, (byte) 0xFF,// MLe
            0x00, (byte) 0xFF,// MLc
            0x04, 0x06,       // NDEF File Control TLV
            (byte) 0xE1, 0x04,// File ID E104
            0x10, 0x00,       // Max NDEF file size 4096
            0x00,             // Read access: always
            (byte) 0xFF       // Write access: never
    };

    private Type4NdefProtocol() {}

    public static boolean isSelectNdefApplication(byte[] apdu) {
        if (apdu == null || apdu.length < 12) return false;
        if (apdu[0] != 0x00 || apdu[1] != (byte) 0xA4 || apdu[2] != 0x04 || apdu[3] != 0x00) return false;
        int lc = apdu[4] & 0xFF;
        if (lc != NDEF_AID.length || apdu.length < 5 + lc) return false;
        for (int i = 0; i < lc; i++) {
            if (apdu[5 + i] != NDEF_AID[i]) return false;
        }
        return true;
    }

    public static final class Session {
        private static final int FILE_NONE = 0;
        private static final int FILE_CC = 1;
        private static final int FILE_NDEF = 2;

        private final byte[] ndefFile;
        private boolean applicationSelected;
        private int selectedFile = FILE_NONE;

        public Session(byte[] ndefMessage) {
            if (ndefMessage == null || ndefMessage.length == 0 || ndefMessage.length > NDEF_FILE_SIZE - 2) {
                throw new IllegalArgumentException("Invalid NDEF message length");
            }
            ndefFile = new byte[ndefMessage.length + 2];
            ndefFile[0] = (byte) ((ndefMessage.length >>> 8) & 0xFF);
            ndefFile[1] = (byte) (ndefMessage.length & 0xFF);
            System.arraycopy(ndefMessage, 0, ndefFile, 2, ndefMessage.length);
        }

        public byte[] process(byte[] apdu) {
            if (apdu == null || apdu.length < 4) return copy(SW_WRONG_DATA);

            if (isSelectNdefApplication(apdu)) {
                applicationSelected = true;
                selectedFile = FILE_NONE;
                return copy(SW_OK);
            }

            int ins = apdu[1] & 0xFF;
            if (ins == 0xA4) return selectFile(apdu);
            if (ins == 0xB0) return readBinary(apdu);
            return copy(SW_INS_NOT_SUPPORTED);
        }

        private byte[] selectFile(byte[] apdu) {
            if (!applicationSelected) return copy(SW_COMMAND_NOT_ALLOWED);
            if (apdu.length < 7 || apdu[0] != 0x00 || (apdu[2] & 0xFF) != 0x00) return copy(SW_FILE_NOT_FOUND);
            int p2 = apdu[3] & 0xFF;
            if (p2 != 0x00 && p2 != 0x0C) return copy(SW_FILE_NOT_FOUND);
            if ((apdu[4] & 0xFF) != 0x02) return copy(SW_FILE_NOT_FOUND);
            int fileId = ((apdu[5] & 0xFF) << 8) | (apdu[6] & 0xFF);
            if (fileId == CC_FILE_ID) selectedFile = FILE_CC;
            else if (fileId == NDEF_FILE_ID) selectedFile = FILE_NDEF;
            else return copy(SW_FILE_NOT_FOUND);
            return copy(SW_OK);
        }

        private byte[] readBinary(byte[] apdu) {
            if (!applicationSelected || selectedFile == FILE_NONE) return copy(SW_COMMAND_NOT_ALLOWED);
            if (apdu.length < 5 || apdu[0] != 0x00) return copy(SW_WRONG_DATA);
            int offset = ((apdu[2] & 0xFF) << 8) | (apdu[3] & 0xFF);
            int le = apdu[4] & 0xFF;
            if (le == 0) le = 256;
            byte[] file = selectedFile == FILE_CC ? CC_FILE : ndefFile;
            if (offset < 0 || offset >= file.length) return copy(SW_WRONG_OFFSET);
            int end = Math.min(offset + le, file.length);
            byte[] chunk = Arrays.copyOfRange(file, offset, end);
            return withStatus(chunk, SW_OK);
        }
    }

    private static byte[] withStatus(byte[] body, byte[] sw) {
        byte[] out = new byte[body.length + sw.length];
        System.arraycopy(body, 0, out, 0, body.length);
        System.arraycopy(sw, 0, out, body.length, sw.length);
        return out;
    }

    private static byte[] copy(byte[] in) {
        return Arrays.copyOf(in, in.length);
    }

    private static byte[] hex(String s) {
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }
}
