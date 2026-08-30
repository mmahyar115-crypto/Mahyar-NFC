from pathlib import Path
import sys
ROOT=Path(__file__).resolve().parents[1]
checks={
 'app/src/main/res/layout/view_send.xml':['sendStatusTitle','sendStatusDescription','btnToggleShare','btnOpenNfcSettings','btnSendQr','sendProfileName'],
 'app/src/main/java/com/mahweb/mahyarnfc/MainActivity.java':['showSendScreen','NfcState.evaluate','btnToggleShare'],
}
errors=[]
for rel,tokens in checks.items():
 p=ROOT/rel
 if not p.exists(): errors.append(f'missing {rel}'); continue
 s=p.read_text(encoding='utf-8')
 for t in tokens:
  if t not in s: errors.append(f'{rel}: missing {t}')
if errors:
 print('SEND CONTRACT FAIL'); print('\n'.join(errors)); sys.exit(1)
print('SEND CONTRACT PASS')
