from pathlib import Path
import sys
ROOT=Path(__file__).resolve().parents[1]
checks={
 'app/src/main/res/layout/activity_main.xml': ['contentContainer','navHome','navReceive','navProfile'],
 'app/src/main/res/layout/view_dashboard.xml': ['dashboardName','btnSendNfc','btnReceiveNfc','btnShowQr','btnEditProfile','dashboardNfcStatus'],
 'app/src/main/res/layout/view_profile.xml': ['profileName','profilePhone','profileEmail','btnSaveProfile','btnPreviewProfile'],
 'app/src/main/java/com/mahweb/mahyarnfc/MainActivity.java': ['OnboardingActivity','showDashboard','showProfile'],
}
errors=[]
for rel,tokens in checks.items():
 p=ROOT/rel
 if not p.exists(): errors.append(f'missing {rel}'); continue
 s=p.read_text(encoding='utf-8')
 for t in tokens:
  if t not in s: errors.append(f'{rel}: missing {t}')
if errors:
 print('MAIN CONTRACT FAIL'); print('\n'.join(errors)); sys.exit(1)
print('MAIN CONTRACT PASS')
