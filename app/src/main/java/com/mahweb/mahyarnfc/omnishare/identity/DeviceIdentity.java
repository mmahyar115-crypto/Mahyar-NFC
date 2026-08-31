package com.mahweb.mahyarnfc.omnishare.identity;
public final class DeviceIdentity { public final String deviceId,publicKeyB64,publicKeyFingerprint,displayAlias; public final long createdAtEpochMs; public DeviceIdentity(String id,String pk,String fp,String alias,long created){deviceId=id;publicKeyB64=pk;publicKeyFingerprint=fp;displayAlias=alias;createdAtEpochMs=created;} }
