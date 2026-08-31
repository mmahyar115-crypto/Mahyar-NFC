package com.mahweb.mahyarnfc.omnishare.lan;
import java.net.InetAddress;
public final class LanPeer { public final String serviceName,tokenHex; public final InetAddress host; public final int port,capabilities; public final long lastSeen; public LanPeer(String n,String t,InetAddress h,int p,int c,long s){serviceName=n;tokenHex=t;host=h;port=p;capabilities=c;lastSeen=s;} public String endpoint(){return host.getHostAddress()+":"+port;} }
