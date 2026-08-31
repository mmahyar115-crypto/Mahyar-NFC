from pathlib import Path
import sys
ROOT=Path(__file__).resolve().parents[1]
checks={
 'app/src/main/res/xml/apduservice.xml':['F0010203040506','D2760000850101'],
 'app/src/main/java/com/mahweb/mahyarnfc/NfcCardService.java':['Type4NdefProtocol.isSelectNdefApplication','UniversalCardPayload.buildNdefMessage','NfcProtocol.SELECT_APDU','MODE_NDEF','MODE_PRIVATE'],
 'app/src/main/java/com/mahweb/mahyarnfc/UniversalCardPayload.java':['https://mmahyar115-crypto.github.io/Mahyar-NFC/card/'],
}
errors=[]
for rel,tokens in checks.items():
 p=ROOT/rel
 if not p.exists(): errors.append('missing '+rel); continue
 s=p.read_text(encoding='utf-8')
 for t in tokens:
  if t not in s: errors.append(f'{rel}: missing {t}')
if errors:
 print('UNIVERSAL HCE CONTRACT FAIL'); print('\n'.join(errors)); sys.exit(1)
print('UNIVERSAL HCE CONTRACT PASS')
