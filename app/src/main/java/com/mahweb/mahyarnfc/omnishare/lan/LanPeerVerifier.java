package com.mahweb.mahyarnfc.omnishare.lan;
public interface LanPeerVerifier {
    boolean approvePeer(String deviceId, String fingerprint, String verificationCode);
}
