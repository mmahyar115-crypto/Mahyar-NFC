# Mahyar NFC Universal v3 Design

## Goal

Deliver the strongest practical phone-to-phone NFC experience that Android permits while making Android-to-iPhone work through standards-based NFC Forum Type 4 / NDEF rather than an app-specific APDU only.

## Product behavior

- Android -> Android with Mahyar NFC installed: keep the fast private Mahyar APDU protocol.
- Android -> iPhone XS or later: expose an NFC Forum Type 4 NDEF tag whose first record is an HTTPS URI. iOS background tag reading can surface this URI without installing Mahyar NFC on the iPhone.
- NDEF also contains a `text/vcard` MIME record for capable NFC readers/apps.
- QR remains the universal offline fallback.
- iPhone is treated as a reader in the no-iOS-app flow; do not promise unrestricted iPhone HCE/card emulation because Apple gates HCE behind managed entitlements and specific contactless use cases.

## Android HCE

One `HostApduService` registers both:
- Mahyar private AID `F0010203040506`.
- NFC Forum NDEF Tag Application AID `D2760000850101`.

The service switches protocol state based on selected AID. For NDEF Type 4 it supports selecting the Capability Container file, selecting the NDEF file, and READ BINARY.

The send screen calls `CardEmulation.setPreferredService()` while the Activity is resumed and sharing is active, and unsets it on pause/exit. This follows Android guidance for foreground HCE transactions.

## Universal contact bridge

NDEF URI points to:
`https://mahyarmolavi.ir/nfc/#c=<base64url compact profile>`

The fragment is intentionally client-side so profile data is not sent to GitHub Pages or another backend as part of the HTTP request. The static page decodes the fragment in the browser, renders the contact card, and creates a vCard locally for Save Contact. No database, account, or analytics is required.

## Compatibility and graceful fallback

- If NFC or HCE is unavailable, QR is always offered.
- If the bridge page is not deployed/enabled, Android-to-Android still works and QR still works; iPhone notification may open a missing page until GitHub Pages is enabled.
- If the profile makes the Universal URL too large for the configured NDEF capacity, the UI should report Universal iPhone sharing unavailable for that profile and keep Android direct + QR available.

## Version

- versionName `3.0.0`
- versionCode `30`
- compileSdk/targetSdk 36, minSdk 26
