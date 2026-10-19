# Mahyar-NFC Phase 3: real-device verification

**Automated CI != measured RF compatibility.** Log observations locally, never upload real contact details or full `#c=` links. Use test-only fictional profiles.

## Matrix (record phone model, OS, app SHA, NFC enabled, foreground/unlocked status)
| Scenario | Expected | Measured result | Elapsed ms |
| --- | --- | --- | --- |
| Android sender HCE -> Android receiver (MNF1) | Exactly one card, no duplicate contact | Pending device test | |
| Android sender HCE -> standard NDEF reader | Valid Type 4 URI / vCard | Pending device test | |
| Android HCE -> supported iPhone NFC | Website link if supported by phone/OS | Pending device test | |
| Android -> iPhone camera QR online | Same contact card at HTTPS `/nfc/#c=` | Pending device test | |
| Android -> iPhone QR offline | vCard scanner compatibility (varies by camera app) | Pending device test | |
| Tag leaves field during APDU | Useful error, safe retry | Pending device test | |
| Rapid repeated NFC taps | At most one successful callback/session | Pending device test | |
| NFC disabled/no NFC hardware | QR alternative remains available | Pending device test | |
| Mobile website FA/EN/DE, dark/light | Correct text, theme, direction | Pending browser test | |
| iOS and Android VCF import | Correct contact fields | Pending device test | |

## Measurements
Use 20 successful and 20 unsuccessful/edge-case attempts per device pairing. Record start at NFC discovery and end at parsed profile render (not just successful APDU). Report median, p95, success percentage; compare against the same phones running the baseline APK. Collect only durations, failure categories, OS/model and app SHA. Do not log names, phone numbers, NFC payloads, vCards, URLs with fragments or secrets. Record any OEM restrictions. Test QR and physical NDEF tag separately from Android HCE. A green JVM build does not claim iPhone-to-HCE compatibility.

## Release gate
1. PR #1 phase 1 must independently pass Android CI.
2. PR #2 phase 2 must pass Android CI; merge ordering must preserve both changes.
3. Phase 3 CI compiles APK and verifies browser bridge fixture; then complete device matrix.
4. Never deploy Django from Android Actions or claim a ParsPack deploy from GitHub CI.
