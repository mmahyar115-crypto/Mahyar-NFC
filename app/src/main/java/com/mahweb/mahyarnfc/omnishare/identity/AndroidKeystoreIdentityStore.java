package com.mahweb.mahyarnfc.omnishare.identity;
import android.content.Context; import android.content.SharedPreferences; import android.security.keystore.*; import java.security.*; import java.security.spec.ECGenParameterSpec; import java.util.UUID; import com.mahweb.mahyarnfc.omnishare.crypto.CryptoBox;
public final class AndroidKeystoreIdentityStore implements IdentityStore {
 public static final String KEY_ALIAS="mahyar_omnishare_device_identity_v1"; private final Context ctx;
 public AndroidKeystoreIdentityStore(Context c){ctx=c.getApplicationContext();}
 private SharedPreferences prefs(){return ctx.getSharedPreferences("mahyar_omnishare_identity_v1",Context.MODE_PRIVATE);} 
 private KeyStore ks(){try{KeyStore k=KeyStore.getInstance("AndroidKeyStore");k.load(null);return k;}catch(Exception e){throw new IllegalStateException(e);}}
 private void ensure(){try{KeyStore k=ks();if(k.containsAlias(KEY_ALIAS))return;KeyPairGenerator g=KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC,"AndroidKeyStore");g.initialize(new KeyGenParameterSpec.Builder(KEY_ALIAS,KeyProperties.PURPOSE_SIGN).setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1")).setDigests(KeyProperties.DIGEST_SHA256).build());g.generateKeyPair();}catch(Exception e){throw new IllegalStateException(e);}}
 public DeviceIdentity getOrCreate(){ensure();SharedPreferences p=prefs();String id=p.getString("device_id",null);long created=p.getLong("created_at",0);if(id==null){id=UUID.randomUUID().toString();created=System.currentTimeMillis();p.edit().putString("device_id",id).putLong("created_at",created).apply();}PublicKey pub=publicKey();String alias=p.getString("alias","Mahyar "+id.substring(0,4));return new DeviceIdentity(id,CryptoBox.publicKeyB64(pub),CryptoBox.fingerprint(pub),alias,created);}
 public PublicKey publicKey(){ensure();try{return ks().getCertificate(KEY_ALIAS).getPublicKey();}catch(Exception e){throw new IllegalStateException(e);}}
 public byte[] sign(byte[] m){ensure();try{PrivateKey k=(PrivateKey)ks().getKey(KEY_ALIAS,null);return CryptoBox.sign(k,m);}catch(Exception e){throw new IllegalStateException(e);}}
}
