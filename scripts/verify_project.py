from pathlib import Path
import re, sys, xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
errors=[]
def read(rel):
 p=ROOT/rel
 if not p.exists(): errors.append('missing '+rel); return ''
 return p.read_text(encoding='utf-8')
build=read('app/build.gradle.kts')
for n in ['compileSdk = 36','targetSdk = 36','minSdk = 26','versionCode = 40','versionName = "4.0.0"']:
 if n not in build: errors.append('build missing '+n)
manifest=read('app/src/main/AndroidManifest.xml')
for n in ['android.permission.NFC','android.permission.INTERNET','android.permission.ACCESS_NETWORK_STATE','android.permission.BLUETOOTH_SCAN','android:name=".NearbyActivity"','android:name=".NfcCardService"']:
 if n not in manifest: errors.append('manifest missing '+n)
protocol=read('app/src/main/java/com/mahweb/mahyarnfc/NfcProtocol.java'); apdu=read('app/src/main/res/xml/apduservice.xml')
for n in ['F0010203040506','MAGIC = "MNF1"','CHUNK_SIZE = 220']:
 if n not in protocol: errors.append('NFC invariant missing '+n)
for n in ['F0010203040506','D2760000850101']:
 if n not in apdu: errors.append('APDU AID missing '+n)
if 'https://mahyarmolavi.ir/nfc/' not in read('app/src/main/java/com/mahweb/mahyarnfc/UniversalCardPayload.java'): errors.append('bridge URL is not mahyarmolavi.ir')
required=[
 'omnishare/TransferEnvelope.java','omnishare/TransferStateMachine.java','omnishare/crypto/CryptoBox.java','omnishare/identity/AndroidKeystoreIdentityStore.java','omnishare/trust/TrustPolicy.java','omnishare/transfer/TransferLedger.java','omnishare/transport/OmniTransport.java',
 'omnishare/ble/BleProtocol.java','omnishare/ble/BleAdvertiser.java','omnishare/ble/BleScanner.java','omnishare/ble/RssiFilter.java','omnishare/ble/BleChunkCodec.java',
 'omnishare/lan/LanProtocol.java','omnishare/lan/LanDiscoveryManager.java','omnishare/lan/LanHandshake.java','omnishare/lan/LanServer.java','omnishare/lan/LanClient.java','omnishare/lan/MultiRecipientDispatcher.java'
]
base=ROOT/'app/src/main/java/com/mahweb/mahyarnfc'
for rel in required:
 if not (base/rel).exists(): errors.append('missing '+rel)
for p in list((ROOT/'app/src/main/res').rglob('*.xml'))+[ROOT/'app/src/main/AndroidManifest.xml']:
 try: ET.parse(p)
 except Exception as e: errors.append('XML parse failed '+str(p.relative_to(ROOT))+': '+str(e))
if errors:
 print('PROJECT_VERIFY=FAIL'); [print('-',e) for e in errors]; sys.exit(1)
print('PROJECT_VERIFY=PASS')
