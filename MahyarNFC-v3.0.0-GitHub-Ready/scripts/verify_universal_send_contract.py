from pathlib import Path
import sys
ROOT=Path(__file__).resolve().parents[1]
checks={
 'app/src/main/java/com/mahweb/mahyarnfc/HceSessionController.java':['setPreferredService','unsetPreferredService','setObserveModeEnabled'],
 'app/src/main/java/com/mahweb/mahyarnfc/NfcShareEvents.java':['MAHYAR_DIRECT','UNIVERSAL_NDEF','notifyReaderDetected','notifyTransferComplete'],
 'app/src/main/java/com/mahweb/mahyarnfc/MainActivity.java':['HceSessionController','NfcShareEvents.Listener','hceSessionController.activate','hceSessionController.deactivate','sendUniversalStatus','btnTestUniversalCard'],
 'app/src/main/res/layout/view_send.xml':['sendUniversalStatus','sendReaderStatus','btnTestUniversalCard','Android','iPhone'],
}
errors=[]
for rel,tokens in checks.items():
 p=ROOT/rel
 if not p.exists(): errors.append('missing '+rel); continue
 s=p.read_text(encoding='utf-8')
 for t in tokens:
  if t not in s: errors.append(f'{rel}: missing {t}')
if errors:
 print('UNIVERSAL SEND CONTRACT FAIL'); print('\n'.join(errors)); sys.exit(1)
print('UNIVERSAL SEND CONTRACT PASS')
