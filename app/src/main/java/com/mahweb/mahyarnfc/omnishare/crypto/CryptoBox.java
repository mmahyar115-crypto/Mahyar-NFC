package com.mahweb.mahyarnfc.omnishare.crypto;
import javax.crypto.*; import javax.crypto.spec.*; import java.security.*; import java.security.spec.*; import java.util.*; import java.nio.charset.StandardCharsets;
public final class CryptoBox {
 private static final SecureRandom RNG=new SecureRandom(); private CryptoBox(){}
 public static byte[] randomBytes(int n){byte[] b=new byte[n];RNG.nextBytes(b);return b;}
 public static KeyPair generateEphemeralEcKeyPair(){try{KeyPairGenerator g=KeyPairGenerator.getInstance("EC");g.initialize(new ECGenParameterSpec("secp256r1"),RNG);return g.generateKeyPair();}catch(Exception e){throw new IllegalStateException(e);}}
 public static byte[] sign(PrivateKey k,byte[] m){try{Signature s=Signature.getInstance("SHA256withECDSA");s.initSign(k,RNG);s.update(m);return s.sign();}catch(Exception e){throw new IllegalStateException(e);}}
 public static boolean verify(PublicKey k,byte[] m,byte[] sig)throws GeneralSecurityException{Signature s=Signature.getInstance("SHA256withECDSA");s.initVerify(k);s.update(m);return s.verify(sig);}
 public static boolean verifyQuiet(PublicKey k,byte[] m,byte[] sig){try{return verify(k,m,sig);}catch(Exception e){return false;}}
 public static byte[] deriveSharedSecret(PrivateKey own,PublicKey peer){try{KeyAgreement a=KeyAgreement.getInstance("ECDH");a.init(own);a.doPhase(peer,true);return a.generateSecret();}catch(Exception e){throw new IllegalStateException(e);}}
 public static byte[] hkdfSha256(byte[] ikm,byte[] salt,byte[] info,int len){try{Mac mac=Mac.getInstance("HmacSHA256");byte[] s=(salt==null||salt.length==0)?new byte[32]:salt;mac.init(new SecretKeySpec(s,"HmacSHA256"));byte[] prk=mac.doFinal(ikm);byte[] out=new byte[len],t=new byte[0];int pos=0,c=1;while(pos<len){mac.init(new SecretKeySpec(prk,"HmacSHA256"));mac.update(t);if(info!=null)mac.update(info);mac.update((byte)c++);t=mac.doFinal();int n=Math.min(t.length,len-pos);System.arraycopy(t,0,out,pos,n);pos+=n;}return out;}catch(Exception e){throw new IllegalStateException(e);}}
 public static CryptoEnvelope encryptAesGcm(byte[] key,byte[] plain,byte[] aad){try{byte[] iv=randomBytes(12);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,iv));if(aad!=null)c.updateAAD(aad);return new CryptoEnvelope(iv,c.doFinal(plain));}catch(Exception e){throw new IllegalStateException(e);}}
 public static byte[] decryptAesGcm(byte[] key,CryptoEnvelope e,byte[] aad)throws GeneralSecurityException{Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,e.nonce));if(aad!=null)c.updateAAD(aad);return c.doFinal(e.ciphertext);}
 public static String sha256Hex(byte[] b){try{byte[] h=MessageDigest.getInstance("SHA-256").digest(b==null?new byte[0]:b);StringBuilder s=new StringBuilder(64);for(byte x:h)s.append(String.format(Locale.US,"%02x",x));return s.toString();}catch(Exception e){throw new IllegalStateException(e);}}
 public static String fingerprint(PublicKey k){return sha256Hex(k.getEncoded());}
 public static String publicKeyB64(PublicKey k){return Base64.getUrlEncoder().withoutPadding().encodeToString(k.getEncoded());}
 public static PublicKey decodeEcPublicKey(String b64){try{byte[] b=Base64.getUrlDecoder().decode(b64);return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(b));}catch(Exception e){throw new IllegalArgumentException("Invalid EC public key",e);}}
 public static byte[] concat(byte[]...xs){int n=0;for(byte[]x:xs)n+=x.length;byte[]o=new byte[n];int p=0;for(byte[]x:xs){System.arraycopy(x,0,o,p,x.length);p+=x.length;}return o;}
 public static byte[] utf8(String s){return (s==null?"":s).getBytes(StandardCharsets.UTF_8);}
}
