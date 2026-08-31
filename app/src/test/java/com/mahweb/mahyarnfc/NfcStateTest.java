package com.mahweb.mahyarnfc;

import org.junit.Test;
import static org.junit.Assert.*;

public class NfcStateTest {
    @Test public void incompleteProfileBlocksSharingFirst() {
        assertEquals(NfcState.Status.PROFILE_INCOMPLETE, NfcState.evaluate(false, true, true, true));
    }
    @Test public void missingNfcIsReported() {
        assertEquals(NfcState.Status.NFC_UNAVAILABLE, NfcState.evaluate(true, false, false, false));
    }
    @Test public void missingHceIsReported() {
        assertEquals(NfcState.Status.HCE_UNAVAILABLE, NfcState.evaluate(true, true, false, true));
    }
    @Test public void disabledNfcIsReported() {
        assertEquals(NfcState.Status.NFC_OFF, NfcState.evaluate(true, true, true, false));
    }
    @Test public void readyRequiresAllCapabilities() {
        assertEquals(NfcState.Status.READY, NfcState.evaluate(true, true, true, true));
        assertTrue(NfcState.canShare(NfcState.Status.READY));
        assertFalse(NfcState.canShare(NfcState.Status.NFC_OFF));
    }
}
