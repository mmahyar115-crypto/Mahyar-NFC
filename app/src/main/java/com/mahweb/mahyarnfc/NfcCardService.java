package com.mahweb.mahyarnfc;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;

import org.json.JSONException;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Dual-protocol HCE service:
 * - Mahyar private APDU for fast Android-to-Android transfers.
 * - NFC Forum Type 4 NDEF for iPhone and generic standards-based readers.
 */
public class NfcCardService extends HostApduService {
    private static final int MODE_NONE = 0;
    private static final int MODE_PRIVATE = 1;
    private static final int MODE_NDEF = 2;

    private int mode = MODE_NONE;
    private byte[] currentPayload = new byte[0];
    private Type4NdefProtocol.Session type4Session;

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        if (commandApdu == null || commandApdu.length < 4) {
            return NfcProtocol.SW_NOT_FOUND;
        }

        if (!ProfileRepository.isShareEnabled(this)) {
            resetSession();
            return NfcProtocol.SW_CONDITIONS;
        }

        if (Type4NdefProtocol.isSelectNdefApplication(commandApdu)) {
            return beginNdefSession(commandApdu);
        }

        if (isSelectPrivateAid(commandApdu)) {
            return beginPrivateSession();
        }

        if (mode == MODE_NDEF && type4Session != null) {
            return type4Session.process(commandApdu);
        }

        if (mode == MODE_PRIVATE && isReadBinary(commandApdu)) {
            return readPrivatePayload(commandApdu);
        }

        return NfcProtocol.SW_NOT_FOUND;
    }

    private byte[] beginNdefSession(byte[] selectApdu) {
        try {
            Profile p = ProfileRepository.load(this);
            if (!ProfileValidator.isProfileReady(p)) {
                resetSession();
                return NfcProtocol.SW_CONDITIONS;
            }
            byte[] ndef = UniversalCardPayload.buildNdefMessage(p, UniversalCardPayload.DEFAULT_BRIDGE_BASE_URL);
            type4Session = new Type4NdefProtocol.Session(ndef);
            currentPayload = new byte[0];
            mode = MODE_NDEF;
            NfcShareEvents.notifyReaderDetected(NfcShareEvents.Protocol.UNIVERSAL_NDEF);
            return type4Session.process(selectApdu);
        } catch (RuntimeException e) {
            resetSession();
            return NfcProtocol.SW_CONDITIONS;
        }
    }

    private byte[] beginPrivateSession() {
        try {
            Profile p = ProfileRepository.load(this);
            if (!ProfileValidator.isProfileReady(p)) {
                resetSession();
                return NfcProtocol.SW_CONDITIONS;
            }
            currentPayload = p.toJson().toString().getBytes(StandardCharsets.UTF_8);
            if (currentPayload.length == 0 || currentPayload.length > 65535) {
                resetSession();
                return NfcProtocol.SW_NOT_FOUND;
            }
            type4Session = null;
            mode = MODE_PRIVATE;
            NfcShareEvents.notifyReaderDetected(NfcShareEvents.Protocol.MAHYAR_DIRECT);
            return NfcProtocol.selectResponse(currentPayload.length);
        } catch (JSONException e) {
            resetSession();
            return NfcProtocol.SW_NOT_FOUND;
        }
    }

    private byte[] readPrivatePayload(byte[] commandApdu) {
        if (currentPayload.length == 0) return NfcProtocol.SW_CONDITIONS;
        int offset = ((commandApdu[2] & 0xFF) << 8) | (commandApdu[3] & 0xFF);
        if (offset < 0 || offset >= currentPayload.length) return NfcProtocol.SW_WRONG_OFFSET;

        int end = Math.min(offset + NfcProtocol.CHUNK_SIZE, currentPayload.length);
        byte[] chunk = Arrays.copyOfRange(currentPayload, offset, end);
        byte[] out = new byte[chunk.length + NfcProtocol.SW_OK.length];
        System.arraycopy(chunk, 0, out, 0, chunk.length);
        System.arraycopy(NfcProtocol.SW_OK, 0, out, chunk.length, NfcProtocol.SW_OK.length);
        if (end >= currentPayload.length) {
            NfcShareEvents.notifyTransferComplete(NfcShareEvents.Protocol.MAHYAR_DIRECT);
        }
        return out;
    }

    private boolean isSelectPrivateAid(byte[] apdu) {
        if (apdu.length < NfcProtocol.SELECT_APDU.length) return false;
        for (int i = 0; i < NfcProtocol.SELECT_APDU.length; i++) {
            if (apdu[i] != NfcProtocol.SELECT_APDU[i]) return false;
        }
        return true;
    }

    private boolean isReadBinary(byte[] apdu) {
        return apdu.length >= 4 && apdu[0] == 0x00 && apdu[1] == (byte) 0xB0;
    }

    private void resetSession() {
        mode = MODE_NONE;
        currentPayload = new byte[0];
        type4Session = null;
    }

    @Override
    public void onDeactivated(int reason) {
        resetSession();
        NfcShareEvents.notifyFieldLost();
    }
}
