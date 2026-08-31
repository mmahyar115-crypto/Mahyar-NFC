# Mahyar NFC v3.0.0 Universal

Mahyar NFC is an Android digital contact-card app focused on the strongest practical phone-to-phone NFC experience across Android and iPhone while keeping profile storage local to the Android device.

## Universal architecture

### Android → Android with Mahyar NFC

The receiving Android phone opens **Receive** in Mahyar NFC. The sender opens **Universal Send** and activates sharing. The app uses the private ISO-DEP/APDU protocol and AID `F0010203040506` for a fast structured profile transfer.

### Android → iPhone without installing Mahyar NFC

The same Android HCE service also exposes the standard NFC Forum NDEF Tag Application AID `D2760000850101` and behaves as a read-only Type 4 NDEF tag.

The first NDEF record is an HTTPS URI so iPhone background NFC reading can surface a notification. The second record is `text/vcard` for standards-capable NFC readers.

The URI opens the static contact bridge:

`https://mmahyar115-crypto.github.io/Mahyar-NFC/card/#c=<profile>`

Profile data is encoded after `#` (the URL fragment). Browsers do not include the fragment in the HTTP request. The static page decodes the profile locally and generates a `.vcf` file in the browser. There is no account, backend database, analytics SDK, XHR, or fetch call.

> The rich iPhone path requires GitHub Pages to be enabled once for this repository. See `card/README.md`.

### QR fallback

QR remains the guaranteed offline fallback and carries a standard vCard. It does not depend on GitHub Pages.

## Reliability improvements over v2

- Foreground HCE preference using `CardEmulation.setPreferredService()` while sharing is active.
- Preferred service is released on pause/exit.
- Safe observe-mode disable where supported by Android.
- Dual-protocol HCE: Mahyar direct + NFC Forum Type 4 NDEF.
- Explicit detection UI for Mahyar direct vs standard/iPhone readers.
- Type 4 Capability Container + NDEF file state machine with bounds/status-word handling.
- Universal profile-size validation before attempting iPhone sharing.
- Existing direct Android AID and `MNF1` framing preserved.
- All profile data remains local in Android SharedPreferences.
- Android app still requests no Internet permission; opening the iPhone preview is delegated to the browser.

## iPhone behavior and limitations

For no-app iPhone receiving, use an iPhone with background NFC tag reading support (iPhone XS or later). Keep the screen active, bring the top of the iPhone near the Android NFC antenna, and tap the NFC notification.

iPhone is treated as the reader for this universal flow. Generic iPhone card emulation is not assumed because Apple's HCE/CardSession access is entitlement-managed and intended for approved contactless transaction use cases.

## Android requirements

- minSdk 26
- targetSdk 36
- compileSdk 36
- Java 17
- Sender requires NFC + HCE for phone-as-card transmission.

## Privacy

- Local Android profile storage only.
- Android backup disabled.
- No Android Internet permission.
- Universal bridge has no backend database and no analytics.
- Contact payload lives in the URL fragment and is decoded client-side.

## GitHub Pages setup for iPhone rich card

1. Repository → **Settings → Pages**.
2. **Build and deployment** → **Deploy from a branch**.
3. Branch `main`, folder `/(root)`.
4. Save.
5. Verify `https://mmahyar115-crypto.github.io/Mahyar-NFC/card/` opens.

## Verification

Local/pure tests:

```bash
python scripts/run_logic_tests.py
python scripts/run_nfc_state_tests.py
python scripts/run_universal_payload_tests.py
python scripts/run_type4_tests.py
python scripts/verify_ui_contract.py
python scripts/verify_main_contract.py
python scripts/verify_send_contract.py
python scripts/verify_receive_contract.py
python scripts/verify_universal_hce_contract.py
python scripts/verify_universal_send_contract.py
python scripts/verify_card_bridge.py
python scripts/verify_project.py
```

GitHub Actions then compiles the Android app and uploads `MahyarNFC-v3.0.0-universal-debug-apk`.

Real NFC interoperability must still be verified on physical devices because antenna placement, OEM NFC stacks, and iPhone background-read state are hardware/runtime dependent.
