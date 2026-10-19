from pathlib import Path
import json, re, subprocess, tempfile, textwrap

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/com/mahweb/mahyarnfc"
JS = ROOT / "scripts/phase3_bridge_fixture.js"
def check(cond, msg):
    if not cond: raise AssertionError(msg)

with tempfile.TemporaryDirectory() as tmp:
    d = Path(tmp); pkg = d / "com/mahweb/mahyarnfc"; pkg.mkdir(parents=True)
    (pkg / "Profile.java").write_text(textwrap.dedent("""
        package com.mahweb.mahyarnfc;
        public class Profile {
          public String name="",job="",company="",phone="",email="",website="",instagram="",telegram="";
          public String toVCard() {return "BEGIN:VCARD\\r\\nVERSION:3.0\\r\\nFN:"+name+"\\r\\nEND:VCARD";}
        }
    """),encoding="utf-8")
    (pkg / "Fixture.java").write_text(textwrap.dedent("""
        package com.mahweb.mahyarnfc;
        public class Fixture {
          public static void main(String[] args) {
            Profile p = new Profile();
            p.name="Test Example";p.phone="+12025550183";p.job="Engineer";p.company="Example";
            p.email="test@example.invalid";p.website="https://example.invalid";
            System.out.println(UniversalCardPayload.bridgeUrl(p,UniversalCardPayload.DEFAULT_BRIDGE_BASE_URL));
          }
        }
    """),encoding="utf-8")
    subprocess.run(["javac","-encoding","UTF-8","-d",str(d),str(SRC/"UniversalCardPayload.java"),str(pkg/"Profile.java"),str(pkg/"Fixture.java")],check=True)
    url=subprocess.check_output(["java","-cp",str(d),"com.mahweb.mahyarnfc.Fixture"],text=True).strip()
    check(url.startswith("https://mahyarmolavi.ir/nfc/#c="),"bridge URL unchanged")
    output=subprocess.check_output(["node",str(JS),url],text=True).strip()
    profile=json.loads(output)
    check(profile["n"]=="Test Example" and profile["p"]=="+12025550183","Android payload decodes as browser card")
    check(profile["v"]==1,"contract version")
    check("?" not in url.split("#",1)[0],"no PII in query string")

for path in [SRC/"NfcProtocol.java",SRC/"Type4NdefProtocol.java",SRC/"NfcCardService.java"]:
    check(path.exists(),f"missing {path}")
raw=(SRC/"NfcProtocol.java").read_text(encoding="utf-8")
check('MAGIC = "MNF1"' in raw,"direct APDU protocol unchanged")
check("CHUNK_SIZE = 220" in raw,"direct chunk size unchanged")
check('D2760000850101' in (SRC/"Type4NdefProtocol.java").read_text(encoding="utf-8"),"Type4 AID unchanged")
check("NfcPreparedPayloads.getOrPrepare" not in (SRC/"NfcCardService.java").read_text(encoding="utf-8") or (SRC/"NfcPreparedPayloads.java").exists(),"prewarm dependency present")
print("PHASE3_COMPATIBILITY=PASS")
