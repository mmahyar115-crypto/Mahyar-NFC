# Mahyar NFC Universal contact bridge

This directory is the static, backend-free contact bridge used by the Android app for iPhone and generic NFC readers.

The Android app emits an HTTPS NDEF URI in this form:

`https://mahyarmolavi.ir/nfc/#c=<base64url-profile>`

The profile payload is stored only in the URL fragment (`#...`). Browsers do not include the fragment in the HTTP request, so the web server receives only `/nfc/`; the contact payload is decoded locally in the browser. The page uses no analytics, `fetch`, XHR, cookies, accounts, or database.

## Deploy once on mahyarmolavi.ir

1. Open the hosting file manager or FTP/SFTP for `mahyarmolavi.ir`.
2. Inside the web root (usually `public_html`) create a directory named `nfc`.
3. Upload this directory's `index.html` as `public_html/nfc/index.html`.
4. Ensure the domain has a valid HTTPS/SSL certificate.
5. Open `https://mahyarmolavi.ir/nfc/` once to confirm the page is reachable.

Android-to-Android direct NFC and QR do not depend on this web page. The bridge is required only for the rich no-app iPhone/browser flow.
