package com.mahweb.mahyarnfc.omnishare.lan;
public final class LanProtocol { public static final int VERSION=1,MAX_FRAME_BYTES=65536; public static final String SERVICE_TYPE="_mahyar-omnishare._tcp."; public static final byte HELLO=1,CHALLENGE=2,AUTH=3,READY=4,ENVELOPE=5,ACK=6,ERROR=7,BYE=8; private LanProtocol(){} }
