# Mahyar NFC Universal contact bridge

This directory is a static, backend-free bridge for iPhone background NFC reading.

The Android app emits an HTTPS NDEF URI in this form:

`https://mmahyar115-crypto.github.io/Mahyar-NFC/card/#c=<base64url-profile>`

The profile payload is in the URL fragment (`#...`). Browsers do not include the fragment in the HTTP request, so the static host receives only `/Mahyar-NFC/card/`, not the contact payload. The page does not use analytics, `fetch`, XHR, cookies, accounts, or a database.

## Enable once on GitHub Pages

1. Open the repository `mmahyar115-crypto/Mahyar-NFC`.
2. Go to **Settings → Pages**.
3. Under **Build and deployment**, choose **Deploy from a branch**.
4. Select branch **main** and folder **/(root)**.
5. Save.
6. After Pages is active, open `https://mmahyar115-crypto.github.io/Mahyar-NFC/card/` once to verify the static page is deployed.

Android-to-Android direct NFC and QR do not depend on GitHub Pages. The page is only required for the rich no-app iPhone experience.
