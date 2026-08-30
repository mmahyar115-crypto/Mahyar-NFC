package com.mahweb.mahyarnfc;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;

import org.json.JSONException;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class NfcCardService extends HostApduService {
    private byte[] currentPayload = new byte[0];

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        if (commandApdu == null || commandApdu.length < 4) {
            return NfcProtocol.SW_NOT_FOUND;
        }

        if (isSelectAid(commandApdu)) {
            if (!ProfileRepository.isShareEnabled(this)) {
                currentPayload = new byte[0];
                return NfcProtocol.SW_CONDITIONS;
            }

            try {
                Profile p = ProfileRepository.load(this);
                currentPayload = p.toJson().toString().getBytes(StandardCharsets.UTF_8);
                if (currentPayload.length > 65535) {
                    currentPayload = new byte[0];
                    return NfcProtocol.SW_NOT_FOUND;
                }
                return NfcProtocol.selectResponse(currentPayload.length);
            } catch (JSONException e) {
                currentPayload = new byte[0];
                return NfcProtocol.SW_NOT_FOUND;
            }
        }

        if (isReadBinary(commandApdu)) {
            if (!ProfileRepository.isShareEnabled(this) || currentPayload.length == 0) {
                return NfcProtocol.SW_CONDITIONS;
            }

            int offset = ((commandApdu[2] & 0xFF) << 8) | (commandApdu[3] & 0xFF);
            if (offset < 0 || offset >= currentPayload.length) {
                return NfcProtocol.SW_WRONG_OFFSET;
            }

            int end = Math.min(offset + NfcProtocol.CHUNK_SIZE, currentPayload.length);
            byte[] chunk = Arrays.copyOfRange(currentPayload, offset, end);
            byte[] out = new byte[chunk.length + NfcProtocol.SW_OK.length];
            System.arraycopy(chunk, 0, out, 0, chunk.length);
            System.arraycopy(NfcProtocol.SW_OK, 0, out, chunk.length, NfcProtocol.SW_OK.length);
            return out;
        }

        return NfcProtocol.SW_NOT_FOUND;
    }

    private boolean isSelectAid(byte[] apdu) {
        if (apdu.length < NfcProtocol.SELECT_APDU.length) return false;
        for (int i = 0; i < NfcProtocol.SELECT_APDU.length; i++) {
            if (apdu[i] != NfcProtocol.SELECT_APDU[i]) return false;
        }
        return true;
    }

    private boolean isReadBinary(byte[] apdu) {
        return apdu.length >= 4 && apdu[0] == 0x00 && apdu[1] == (byte) 0xB0;
    }

    @Override
    public void onDeactivated(int reason) {
        currentPayload = new byte[0];
    }
}
