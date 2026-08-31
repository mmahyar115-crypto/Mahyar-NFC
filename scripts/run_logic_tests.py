from pathlib import Path
import subprocess, tempfile, textwrap, sys

ROOT = Path(__file__).resolve().parents[1]
src = ROOT / 'app/src/main/java/com/mahweb/mahyarnfc/ProfileValidator.java'
if not src.exists():
    print('FAIL: ProfileValidator.java does not exist yet')
    sys.exit(1)

with tempfile.TemporaryDirectory() as td:
    td = Path(td)
    pkg = td / 'com/mahweb/mahyarnfc'
    pkg.mkdir(parents=True)
    (pkg / 'Profile.java').write_text(textwrap.dedent('''
        package com.mahweb.mahyarnfc;
        public class Profile {
            public String name = "";
            public String phone = "";
        }
    '''), encoding='utf-8')
    (pkg / 'ProfileValidatorHarness.java').write_text(textwrap.dedent('''
        package com.mahweb.mahyarnfc;
        public class ProfileValidatorHarness {
            private static void check(boolean v, String msg) {
                if (!v) throw new AssertionError(msg);
            }
            public static void main(String[] args) {
                check(ProfileValidator.validateName("   ") != null, "blank name");
                check(ProfileValidator.validateName("مهیار رضایی") == null, "valid name");
                check(ProfileValidator.validatePhone("") != null, "blank phone");
                check(ProfileValidator.validatePhone("+98 912 123 4567") == null, "valid phone");
                check(ProfileValidator.validatePhone("12") != null, "short phone");
                check(ProfileValidator.validateEmail("") == null, "blank email optional");
                check(ProfileValidator.validateEmail("name@example.com") == null, "valid email");
                check(ProfileValidator.validateEmail("bad@email") != null, "bad email");
                check("https://example.com".equals(ProfileValidator.normalizeWebsite("example.com")), "website normalize");
                check("https://example.com/a".equals(ProfileValidator.normalizeWebsite("https://example.com/a")), "website keep scheme");
                Profile p = new Profile();
                p.name = "مهیار"; p.phone = "+989121234567";
                check(ProfileValidator.isProfileReady(p), "ready profile");
                p.phone = "";
                check(!ProfileValidator.isProfileReady(p), "incomplete profile");
                System.out.println("ProfileValidatorHarness: PASS");
            }
        }
    '''), encoding='utf-8')
    out = td / 'out'; out.mkdir()
    cmd = ['javac', '-encoding', 'UTF-8', '-d', str(out), str(src), str(pkg/'Profile.java'), str(pkg/'ProfileValidatorHarness.java')]
    subprocess.run(cmd, check=True)
    subprocess.run(['java', '-cp', str(out), 'com.mahweb.mahyarnfc.ProfileValidatorHarness'], check=True)
