package com.mahweb.mahyarnfc.omnishare.identity;
import java.security.PublicKey;
public interface IdentityStore { DeviceIdentity getOrCreate(); PublicKey publicKey(); byte[] sign(byte[] message); }
