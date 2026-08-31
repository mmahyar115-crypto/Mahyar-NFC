package com.mahweb.mahyarnfc.omnishare.ble;
import java.util.*;
public final class BleProtocol {
 public static final UUID SERVICE_UUID=UUID.fromString("8f28a001-5d4c-4c0e-9f31-9cb5a4d10001");
 public static final UUID CONTROL_UUID=UUID.fromString("8f28a001-5d4c-4c0e-9f31-9cb5a4d10002");
 public static final UUID DATA_UUID=UUID.fromString("8f28a001-5d4c-4c0e-9f31-9cb5a4d10003");
 public static final int VERSION=1, CAP_GATT=1, CAP_LAN=2, CAP_REMOTE=4;
 private BleProtocol(){}
 public static byte[] serviceData(int caps,byte[] token){if(token==null||token.length!=8)throw new IllegalArgumentException("token must be 8 bytes");byte[]o=new byte[10];o[0]=(byte)VERSION;o[1]=(byte)caps;System.arraycopy(token,0,o,2,8);return o;}
 public static Parsed parse(byte[] d){if(d==null||d.length!=10||(d[0]&255)!=VERSION)return null;return new Parsed(d[1]&255,Arrays.copyOfRange(d,2,10));}
 public static final class Parsed{public final int capabilities;public final byte[]token;public Parsed(int c,byte[]t){capabilities=c;token=t;}}
}
