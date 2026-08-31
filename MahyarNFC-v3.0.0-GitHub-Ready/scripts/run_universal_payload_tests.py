from pathlib import Path
import subprocess, tempfile, textwrap, sys

ROOT = Path(__file__).resolve().parents[1]
src = ROOT / 'app/src/main/java/com/mahweb/mahyarnfc/UniversalCardPayload.java'
if not src.exists():
    print('EXPECTED RED: UniversalCardPayload.java does not exist yet')
    sys.exit(1)

with tempfile.TemporaryDirectory() as td:
    td = Path(td)
    pkg = td / 'com/mahweb/mahyarnfc'; pkg.mkdir(parents=True)
    (pkg/'Profile.java').write_text(textwrap.dedent('''
        package com.mahweb.mahyarnfc;
        public class Profile {
            public String name="", job="", company="", phone="", email="", website="", instagram="", telegram="";
            public String toVCard() {
                return "BEGIN:VCARD\\r\\nVERSION:3.0\\r\\nFN:"+name+"\\r\\nTEL;TYPE=CELL:"+phone+"\\r\\nEND:VCARD";
            }
        }
    '''), encoding='utf-8')
    (pkg/'UniversalPayloadHarness.java').write_text(textwrap.dedent('''
        package com.mahweb.mahyarnfc;
        import java.nio.charset.StandardCharsets;
        import java.util.Arrays;
        import java.util.Base64;

        public class UniversalPayloadHarness {
            static void check(boolean v, String m) { if (!v) throw new AssertionError(m); }
            static int u8(byte b) { return b & 0xff; }
            static int readPayloadLength(byte[] n, int off) {
                boolean sr = (n[off] & 0x10) != 0;
                if (sr) return u8(n[off+2]);
                return (u8(n[off+2])<<24)|(u8(n[off+3])<<16)|(u8(n[off+4])<<8)|u8(n[off+5]);
            }
            static int headerSize(byte[] n, int off) { return ((n[off] & 0x10) != 0) ? 3 : 6; }
            static int nextRecord(byte[] n, int off) {
                int h=headerSize(n,off), tl=u8(n[off+1]), pl=readPayloadLength(n,off);
                return off+h+tl+pl;
            }
            public static void main(String[] args) throws Exception {
                Profile p=new Profile();
                p.name="مهیار رضایی"; p.phone="+989121234567"; p.job="مدیر"; p.company="Mahyar";
                p.email="me@example.com"; p.website="https://example.com"; p.instagram="mahyar"; p.telegram="mahyar";
                String base="https://mmahyar115-crypto.github.io/Mahyar-NFC/card/";
                String url=UniversalCardPayload.bridgeUrl(p,base);
                check(url.startsWith(base+"#c="), "fragment url");
                check(!url.contains("?"), "no query profile data");
                String token=url.substring((base+"#c=").length());
                byte[] decoded=Base64.getUrlDecoder().decode(token);
                String json=new String(decoded, StandardCharsets.UTF_8);
                check(json.contains("مهیار رضایی"), "unicode profile encoded");
                check(json.contains("+989121234567"), "phone encoded");

                byte[] ndef=UniversalCardPayload.buildNdefMessage(p,base);
                check(ndef.length > 20, "ndef not empty");
                check((ndef[0] & 0x80) != 0, "first record MB");
                check((ndef[0] & 0x07) == 0x01, "first record well known TNF");
                int h1=headerSize(ndef,0), t1=u8(ndef[1]), pl1=readPayloadLength(ndef,0);
                check(t1==1 && ndef[h1]=='U', "first record URI type");
                int p1=h1+t1;
                check(ndef[p1]==0x04, "https URI prefix code");
                String uriRemainder=new String(Arrays.copyOfRange(ndef,p1+1,p1+pl1),StandardCharsets.UTF_8);
                check(("https://"+uriRemainder).equals(url), "URI payload exact");

                int r2=nextRecord(ndef,0);
                check(r2 < ndef.length, "second record exists");
                check((ndef[r2] & 0x40) != 0, "second record ME");
                check((ndef[r2] & 0x07) == 0x02, "second record MIME TNF");
                int h2=headerSize(ndef,r2), t2=u8(ndef[r2+1]), pl2=readPayloadLength(ndef,r2);
                String mime=new String(Arrays.copyOfRange(ndef,r2+h2,r2+h2+t2),StandardCharsets.US_ASCII);
                check(mime.equals("text/vcard"), "vcard mime");
                String vcard=new String(Arrays.copyOfRange(ndef,r2+h2+t2,r2+h2+t2+pl2),StandardCharsets.UTF_8);
                check(vcard.contains("BEGIN:VCARD") && vcard.contains("مهیار رضایی"), "vcard payload");
                check(UniversalCardPayload.isUniversalPayloadSupported(p,base), "normal payload supported");

                p.website="x".repeat(5000);
                check(!UniversalCardPayload.isUniversalPayloadSupported(p,base), "oversized payload rejected");
                System.out.println("UniversalPayloadHarness: PASS");
            }
        }
    '''), encoding='utf-8')
    out=td/'out'; out.mkdir()
    subprocess.run(['javac','-encoding','UTF-8','-d',str(out),str(src),str(pkg/'Profile.java'),str(pkg/'UniversalPayloadHarness.java')],check=True)
    subprocess.run(['java','-cp',str(out),'com.mahweb.mahyarnfc.UniversalPayloadHarness'],check=True)
