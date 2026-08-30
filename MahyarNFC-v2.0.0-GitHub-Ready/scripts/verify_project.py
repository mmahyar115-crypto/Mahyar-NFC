from pathlib import Path
import re, sys, xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
errors=[]

def read(rel):
    p=ROOT/rel
    if not p.exists():
        errors.append(f'missing {rel}')
        return ''
    return p.read_text(encoding='utf-8')

build=read('app/build.gradle.kts')
for needle in ['compileSdk = 36','targetSdk = 36','minSdk = 26','versionCode = 20','versionName = "2.0.0"']:
    if needle not in build: errors.append(f'build.gradle.kts missing: {needle}')

manifest=read('app/src/main/AndroidManifest.xml')
for needle in ['android:allowBackup="false"','android:name=".OnboardingActivity"','android:name=".MainActivity"','android:name=".NfcCardService"','android.permission.BIND_NFC_SERVICE']:
    if needle not in manifest: errors.append(f'manifest missing: {needle}')
if 'android.permission.INTERNET' in manifest:
    errors.append('manifest must not request INTERNET')

protocol=read('app/src/main/java/com/mahweb/mahyarnfc/NfcProtocol.java')
apdu=read('app/src/main/res/xml/apduservice.xml')
aid_match=re.search(r'AID_HEX\s*=\s*"([0-9A-F]+)"',protocol)
aid_xml=re.search(r'aid-filter android:name="([0-9A-F]+)"',apdu)
if not aid_match or not aid_xml or aid_match.group(1)!=aid_xml.group(1):
    errors.append('NFC AID mismatch')
if 'F0010203040506' not in protocol or 'F0010203040506' not in apdu:
    errors.append('expected AID changed')
for needle in ['MAGIC = "MNF1"','CHUNK_SIZE = 220']:
    if needle not in protocol: errors.append(f'protocol invariant missing: {needle}')

repo=read('app/src/main/java/com/mahweb/mahyarnfc/ProfileRepository.java')
for needle in ['onboarding_completed','profile_schema_version','hasUsableProfile']:
    if needle not in repo: errors.append(f'ProfileRepository missing: {needle}')

validator=read('app/src/main/java/com/mahweb/mahyarnfc/ProfileValidator.java')
for needle in ['validateName','validatePhone','validateEmail','validateWebsite','normalizeWebsite','isProfileReady']:
    if needle not in validator: errors.append(f'ProfileValidator missing: {needle}')

main=read('app/src/main/java/com/mahweb/mahyarnfc/MainActivity.java')
for needle in ['showDashboard','showProfile','showSendScreen','showReceiveScreen','openContactInsert','showQr']:
    if needle not in main: errors.append(f'MainActivity missing: {needle}')

onboarding=read('app/src/main/java/com/mahweb/mahyarnfc/OnboardingActivity.java')
for needle in ['completeOnboarding','setOnboardingCompleted','btnCreateCard']:
    if needle not in onboarding: errors.append(f'OnboardingActivity missing: {needle}')

required_layouts=[
 'activity_onboarding.xml','activity_main.xml','view_dashboard.xml','view_profile.xml','view_send.xml','view_receive.xml'
]
for name in required_layouts:
    p=ROOT/'app/src/main/res/layout'/name
    if not p.exists(): errors.append(f'missing layout: {name}')

workflow=read('.github/workflows/main.yml')
for needle in ['platforms;android-36','build-tools;36.0.0','MahyarNFC-v2.0.0-debug-apk','gradle :app:assembleDebug']:
    if needle not in workflow: errors.append(f'workflow missing: {needle}')

# Parse all XML resources and manifest.
for p in list((ROOT/'app/src/main/res').rglob('*.xml'))+[ROOT/'app/src/main/AndroidManifest.xml']:
    try: ET.parse(p)
    except Exception as e: errors.append(f'XML parse failed {p.relative_to(ROOT)}: {e}')

if errors:
    print('PROJECT_VERIFY=FAIL')
    for e in errors: print('-',e)
    sys.exit(1)
print('PROJECT_VERIFY=PASS')
