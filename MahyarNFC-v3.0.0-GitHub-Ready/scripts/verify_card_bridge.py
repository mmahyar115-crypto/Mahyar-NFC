from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
REPO_ROOT = ROOT.parent

p = ROOT / 'card/index.html'
errors = []

if not p.exists():
    errors.append('missing card/index.html')
else:
    s = p.read_text(encoding='utf-8')
    required = [
        'location.hash',
        'TextDecoder',
        'atob',
        'new Blob',
        'text/vcard',
        'BEGIN:VCARD',
        'ذخیره مخاطب',
        'تماس',
        'کپی شماره',
    ]
    for t in required:
        if t not in s:
            errors.append('missing ' + t)

    forbidden = [
        'fetch(',
        'XMLHttpRequest',
        'google-analytics',
        'gtag(',
        'segment.com',
        'mixpanel',
    ]
    low = s.lower()
    for t in forbidden:
        if t.lower() in low:
            errors.append('forbidden network/analytics token ' + t)

if not (ROOT / 'card/README.md').exists():
    errors.append('missing card/README.md')

# .nojekyll belongs at repository root for GitHub Pages.
# Also accept a project-local copy so the test remains portable.
if not ((ROOT / '.nojekyll').exists() or (REPO_ROOT / '.nojekyll').exists()):
    errors.append('missing .nojekyll (expected at repository root or project root)')

if errors:
    print('CARD BRIDGE CONTRACT FAIL')
    print('\n'.join(errors))
    sys.exit(1)

print('CARD BRIDGE CONTRACT PASS')
