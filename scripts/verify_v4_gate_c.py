from pathlib import Path
import sys, xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
errors=[]
def need(rel,*tokens):
 p=ROOT/rel
 if not p.exists(): errors.append('missing '+rel); return
 s=p.read_text(encoding='utf-8')
 for t in tokens:
  if t not in s: errors.append(f'{rel} missing {t}')
need('app/build.gradle.kts','versionCode = 40','versionName = "4.0.0"','compileSdk = 36','targetSdk = 36','minSdk = 26')
need('app/src/main/AndroidManifest.xml','android.permission.INTERNET','android.permission.ACCESS_NETWORK_STATE','android.permission.BLUETOOTH_SCAN','android:name=".NearbyActivity"')
need('app/src/main/java/com/mahweb/mahyarnfc/NfcProtocol.java','F0010203040506','MAGIC = "MNF1"','CHUNK_SIZE = 220')
need('app/src/main/res/xml/apduservice.xml','F0010203040506','D2760000850101')
need('app/src/main/java/com/mahweb/mahyarnfc/UniversalCardPayload.java','https://mahyarmolavi.ir/nfc/')
need('app/src/main/java/com/mahweb/mahyarnfc/omnishare/TransferStateMachine.java','WAITING_ACK','DELIVERED')
need('app/src/main/java/com/mahweb/mahyarnfc/omnishare/identity/AndroidKeystoreIdentityStore.java','AndroidKeyStore','mahyar_omnishare_device_identity_v1','PURPOSE_SIGN')
need('app/src/main/java/com/mahweb/mahyarnfc/omnishare/trust/TrustPolicy.java','TRUSTED','autoReceive','fingerprint')
need('app/src/main/java/com/mahweb/mahyarnfc/omnishare/transfer/TransferLedger.java','REJECT_REPLAY','DUPLICATE_ALREADY_DELIVERED')
need('app/src/main/java/com/mahweb/mahyarnfc/omnishare/ble/BleProtocol.java','8f28a001-5d4c-4c0e-9f31-9cb5a4d10001')
need('app/src/main/java/com/mahweb/mahyarnfc/omnishare/ble/BleScanner.java','setServiceUuid','BleProtocol.SERVICE_UUID')
need('app/src/main/java/com/mahweb/mahyarnfc/omnishare/lan/LanProtocol.java','_mahyar-omnishare._tcp')
need('app/src/main/java/com/mahweb/mahyarnfc/omnishare/lan/LanDiscoveryManager.java','NsdManager','registerService','discoverServices','resolveService')
need('app/src/main/java/com/mahweb/mahyarnfc/omnishare/lan/LanHandshake.java','SERVER-AUTH','CLIENT-AUTH','deriveSharedSecret','hkdfSha256')
need('app/src/main/java/com/mahweb/mahyarnfc/omnishare/lan/LanServer.java','decryptAesGcm','INVALID_ENVELOPE_SIGNATURE','APPROVAL_REQUIRED','ERROR_IDENTITY_CHANGED')
need('app/src/main/java/com/mahweb/mahyarnfc/omnishare/lan/LanClient.java','DeliveryAckValidator','encryptAesGcm','decryptAesGcm')
need('app/src/main/java/com/mahweb/mahyarnfc/omnishare/lan/MultiRecipientDispatcher.java','newFixedThreadPool','CompletableFuture')
need('app/src/main/res/layout/activity_nearby.xml','nearbyList','btnSendSelected','filterSameWifi')
for p in list((ROOT/'app/src/main/res').rglob('*.xml'))+[ROOT/'app/src/main/AndroidManifest.xml']:
 try: ET.parse(p)
 except Exception as e: errors.append(f'XML parse failed {p}: {e}')
if errors:
 print('V4_GATE_C_VERIFY=FAIL')
 for e in errors: print('-',e)
 sys.exit(1)
print('V4_GATE_C_VERIFY=PASS')
