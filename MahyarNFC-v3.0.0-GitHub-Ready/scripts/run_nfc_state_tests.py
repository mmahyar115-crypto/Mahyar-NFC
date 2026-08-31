from pathlib import Path
import subprocess, tempfile, textwrap, sys
ROOT=Path(__file__).resolve().parents[1]
src=ROOT/'app/src/main/java/com/mahweb/mahyarnfc/NfcState.java'
if not src.exists():
    print('FAIL: NfcState.java does not exist yet')
    sys.exit(1)
with tempfile.TemporaryDirectory() as td:
    td=Path(td); out=td/'out'; out.mkdir()
    h=td/'NfcStateHarness.java'
    h.write_text(textwrap.dedent('''
        import com.mahweb.mahyarnfc.NfcState;
        public class NfcStateHarness {
          static void c(boolean v,String m){if(!v) throw new AssertionError(m);}
          public static void main(String[] a){
            c(NfcState.evaluate(false,true,true,true)==NfcState.Status.PROFILE_INCOMPLETE,"profile");
            c(NfcState.evaluate(true,false,false,false)==NfcState.Status.NFC_UNAVAILABLE,"nfc missing");
            c(NfcState.evaluate(true,true,false,true)==NfcState.Status.HCE_UNAVAILABLE,"hce");
            c(NfcState.evaluate(true,true,true,false)==NfcState.Status.NFC_OFF,"off");
            c(NfcState.evaluate(true,true,true,true)==NfcState.Status.READY,"ready");
            c(NfcState.canShare(NfcState.Status.READY),"can share");
            c(!NfcState.canShare(NfcState.Status.NFC_OFF),"cannot share");
            System.out.println("NfcStateHarness: PASS");
          }
        }
    '''), encoding='utf-8')
    subprocess.run(['javac','-encoding','UTF-8','-d',str(out),str(src),str(h)],check=True)
    subprocess.run(['java','-cp',str(out),'NfcStateHarness'],check=True)
