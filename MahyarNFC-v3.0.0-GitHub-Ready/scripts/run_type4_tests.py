from pathlib import Path
import subprocess, tempfile, textwrap, sys
ROOT=Path(__file__).resolve().parents[1]
src=ROOT/'app/src/main/java/com/mahweb/mahyarnfc/Type4NdefProtocol.java'
if not src.exists():
    print('EXPECTED RED: Type4NdefProtocol.java does not exist yet'); sys.exit(1)
with tempfile.TemporaryDirectory() as td:
    td=Path(td); pkg=td/'com/mahweb/mahyarnfc'; pkg.mkdir(parents=True)
    (pkg/'Type4Harness.java').write_text(textwrap.dedent('''
        package com.mahweb.mahyarnfc;
        import java.util.Arrays;
        public class Type4Harness {
            static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
            static byte[] h(String s){int n=s.length()/2;byte[]o=new byte[n];for(int i=0;i<n;i++)o[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16);return o;}
            static String sw(byte[] r){int n=r.length;return String.format("%02X%02X",r[n-2]&255,r[n-1]&255);}
            static byte[] body(byte[] r){return Arrays.copyOf(r,r.length-2);}
            public static void main(String[] args){
                byte[] msg=new byte[600]; for(int i=0;i<msg.length;i++)msg[i]=(byte)(i&255);
                Type4NdefProtocol.Session s=new Type4NdefProtocol.Session(msg);
                check(sw(s.process(h("00A4040007D276000085010100"))).equals("9000"),"select ndef app");
                check(sw(s.process(h("00B000000F"))).equals("6986"),"read without file denied");
                check(sw(s.process(h("00A4000C02E103"))).equals("9000"),"select cc");
                byte[] cc=body(s.process(h("00B000000F")));
                check(cc.length==15,"cc length");
                check((cc[0]&255)==0 && (cc[1]&255)==15,"cc cclen");
                check((cc[2]&255)==0x20,"mapping version");
                check((cc[7]&255)==0x04 && (cc[8]&255)==0x06,"ndef tlv");
                check((cc[9]&255)==0xE1 && (cc[10]&255)==0x04,"ndef file id");
                check((cc[11]&255)==0x10 && (cc[12]&255)==0x00,"ndef file 4096 bytes");
                check((cc[13]&255)==0x00 && (cc[14]&255)==0xFF,"read yes write no");

                check(sw(s.process(h("00A4000C02E104"))).equals("9000"),"select ndef file");
                byte[] nlen=body(s.process(h("00B0000002")));
                check(nlen.length==2 && (nlen[0]&255)==0x02 && (nlen[1]&255)==0x58,"NLEN 600");
                byte[] first=body(s.process(h("00B000020A")));
                check(first.length==10 && first[0]==0 && (first[9]&255)==9,"read first message bytes");
                byte[] le256=body(s.process(h("00B0000200")));
                check(le256.length==256,"Le zero means 256");
                check(sw(s.process(h("00B00FFF10"))).equals("6B00"),"bad offset");
                check(sw(s.process(h("00A4000C02E199"))).equals("6A82"),"unknown file");
                check(sw(s.process(h("00FF000000"))).equals("6D00"),"unknown ins");

                check(sw(s.process(h("00A4040007D2760000850101"))).equals("9000"),"select without Le accepted");
                check(sw(s.process(h("00A4000002E104"))).equals("9000"),"select file p2 zero accepted");
                System.out.println("Type4Harness: PASS");
            }
        }
    '''),encoding='utf-8')
    out=td/'out';out.mkdir()
    subprocess.run(['javac','-encoding','UTF-8','-d',str(out),str(src),str(pkg/'Type4Harness.java')],check=True)
    subprocess.run(['java','-cp',str(out),'com.mahweb.mahyarnfc.Type4Harness'],check=True)
