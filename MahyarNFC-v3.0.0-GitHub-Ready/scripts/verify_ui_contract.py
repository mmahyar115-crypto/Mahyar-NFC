from pathlib import Path
import sys
ROOT=Path(__file__).resolve().parents[1]
checks={
 'app/src/main/res/layout/activity_onboarding.xml': ['stepWelcome','stepEssential','stepContact','stepPreview','inputName','inputPhone','inputEmail','inputWebsite','btnCreateCard'],
 'app/src/main/AndroidManifest.xml': ['.OnboardingActivity'],
}
errors=[]
for rel, tokens in checks.items():
    p=ROOT/rel
    if not p.exists():
        errors.append(f'missing {rel}')
        continue
    s=p.read_text(encoding='utf-8')
    for t in tokens:
        if t not in s: errors.append(f'{rel}: missing {t}')
if errors:
    print('UI CONTRACT FAIL')
    print('\n'.join(errors))
    sys.exit(1)
print('UI CONTRACT PASS')
