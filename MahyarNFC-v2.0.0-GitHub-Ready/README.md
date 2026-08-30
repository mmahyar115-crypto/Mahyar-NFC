# Mahyar NFC v2.0.0

Mahyar NFC is a lightweight Android digital contact-card app. Your profile is stored locally on the phone and can be shared directly between two compatible Android phones with NFC, or through a standard vCard QR code.

## What changed in v2

- Guided first-run onboarding with a four-step setup flow.
- Full name and mobile number are required before the card can be used.
- Email and website validation with automatic `https://` normalization.
- Modern RTL-first Persian dashboard and editable digital card.
- Clear NFC send states: ready, NFC off, NFC unavailable, HCE unavailable, or incomplete profile.
- NFC sharing is enabled only by an explicit user action and is disabled when leaving the Send screen.
- Professional receive flow with ready / reading / success / error states.
- Received profiles can be opened in Android's standard Contacts insert screen; no Contacts write permission is requested.
- QR/vCard fallback for phones where direct NFC sharing is unavailable.
- No server, account, analytics SDK, Internet permission, or cloud sync.
- Android backup is disabled to keep profile data from being copied by app backup mechanisms.

## Supported Android versions

- `minSdk`: 26
- `targetSdk`: 36
- `compileSdk`: 36
- Java: 17

## Direct phone-to-phone NFC requirements

### Sending phone

- NFC hardware
- NFC enabled
- Host Card Emulation (HCE)
- Mahyar NFC installed
- A completed profile
- Send screen open with NFC Share explicitly active

### Receiving phone

- NFC hardware
- NFC enabled
- Mahyar NFC installed
- Receive screen open

Direct transfer uses the app's existing ISO-DEP/APDU protocol and AID `F0010203040506`.

## QR fallback

The QR code contains a vCard 3.0 payload. A second phone can normally scan it with its camera or QR scanner even if Mahyar NFC is not installed.

## Privacy

Profile fields are stored in local Android `SharedPreferences`. The app requests NFC permission only. It does not request Internet access and does not upload profile data to a backend.

## Building on GitHub Actions

The repository contains `.github/workflows/main.yml`. A push to `main` starts a debug build. A successful run uploads an artifact named:

`MahyarNFC-v2.0.0-debug-apk`

Inside the artifact ZIP is:

`app-debug.apk`

## Verification

Local source checks:

```bash
python scripts/run_logic_tests.py
python scripts/run_nfc_state_tests.py
python scripts/verify_ui_contract.py
python scripts/verify_main_contract.py
python scripts/verify_send_contract.py
python scripts/verify_receive_contract.py
python scripts/verify_project.py
```

The Android APK build itself must be verified by GitHub Actions (or an Android SDK environment). Real phone-to-phone NFC behavior should then be tested on two physical NFC-capable Android devices.
