package com.mahweb.mahyarnfc.omnishare.lan;

import com.mahweb.mahyarnfc.omnishare.TransferEnvelope;

public interface LanReceivePolicy {
    boolean approve(String deviceId, String fingerprint, String verificationCode, TransferEnvelope envelope);
    void onReceived(String deviceId, String fingerprint, TransferEnvelope envelope);
    void onRejected(String deviceId, String fingerprint, String reason);
}
