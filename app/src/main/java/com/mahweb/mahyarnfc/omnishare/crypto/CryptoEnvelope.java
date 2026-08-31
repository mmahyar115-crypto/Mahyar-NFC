package com.mahweb.mahyarnfc.omnishare.crypto;
import java.util.Arrays;
public final class CryptoEnvelope { public final byte[] nonce,ciphertext; public CryptoEnvelope(byte[] n,byte[] c){nonce=Arrays.copyOf(n,n.length);ciphertext=Arrays.copyOf(c,c.length);} }
