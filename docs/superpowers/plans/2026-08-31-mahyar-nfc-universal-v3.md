# Mahyar NFC Universal v3 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add dual-protocol HCE, standards-based Type 4 NDEF for iPhone, foreground HCE routing, a static privacy-preserving contact bridge, and improved universal-send UX.

**Architecture:** Preserve the existing Mahyar private APDU path for Android app-to-app. Add pure-Java `UniversalCardPayload` and `Type4NdefProtocol` helpers so NDEF encoding and Type 4 APDU behavior can be unit-tested without Android SDK. `NfcCardService` multiplexes Mahyar and NDEF applications. `MainActivity` owns foreground HCE preference and user-facing state.

**Tech Stack:** Android Java 17, HCE/ISO-DEP, NFC Forum NDEF Type 4, SharedPreferences, static HTML/JS bridge, ZXing QR.

**Spec:** `docs/superpowers/specs/2026-08-31-mahyar-nfc-universal-v3-design.md`

## Global Constraints

- Keep private AID `F0010203040506` backward compatible.
- Add NDEF Tag Application AID `D2760000850101`.
- First NDEF record must be an HTTPS URI for iPhone background reading.
- Profile data in the bridge URL must be in the URL fragment, not query/path.
- No backend/database/analytics.
- Android direct + QR must continue working even if the bridge is unavailable.
- `compileSdk=36`, `targetSdk=36`, `minSdk=26`, Java 17.
- Release target `3.0.0`, versionCode 30.

---

### Task 1: Universal profile payload and NDEF encoding

**Files:**
- Create `app/src/main/java/com/mahweb/mahyarnfc/UniversalCardPayload.java`
- Create `scripts/run_universal_payload_tests.py`

**Interfaces:**
- `UniversalCardPayload.bridgeUrl(Profile, String): String`
- `UniversalCardPayload.buildNdefMessage(Profile, String): byte[]`
- `UniversalCardPayload.isUniversalPayloadSupported(Profile, String): boolean`

- [ ] Write failing pure JVM harness for base64url fragment, URI-first NDEF, vCard MIME second record, Unicode round-trip expectations, max-capacity behavior.
- [ ] Run harness and confirm RED.
- [ ] Implement minimal encoder.
- [ ] Run harness and confirm GREEN.

### Task 2: NFC Forum Type 4 APDU state machine

**Files:**
- Create `app/src/main/java/com/mahweb/mahyarnfc/Type4NdefProtocol.java`
- Create `scripts/run_type4_tests.py`

**Interfaces:**
- `Type4NdefProtocol.Session(byte[] ndefMessage)`
- `Session.process(byte[] apdu): byte[]`
- Constants for NDEF AID, CC file ID, NDEF file ID.

- [ ] Write failing APDU tests for NDEF AID selection, CC selection/read, NDEF file selection/read, Le=0, invalid file/read state.
- [ ] Confirm RED.
- [ ] Implement state machine with standard status words.
- [ ] Confirm GREEN.

### Task 3: Dual-protocol HCE service

**Files:**
- Modify `app/src/main/java/com/mahweb/mahyarnfc/NfcCardService.java`
- Modify `app/src/main/res/xml/apduservice.xml`
- Modify `app/src/main/res/values/strings.xml`
- Create `scripts/verify_universal_hce_contract.py`

**Interfaces:**
- Existing Mahyar APDU behavior unchanged.
- NDEF AID routes to `Type4NdefProtocol.Session`.

- [ ] Write contract assertions first and confirm RED.
- [ ] Register both AIDs.
- [ ] Multiplex private and Type 4 sessions based on SELECT AID.
- [ ] Reject all sharing while `share_enabled=false`.
- [ ] Confirm private protocol framing still matches `NfcProtocol`.

### Task 4: Foreground HCE preference and robust send lifecycle

**Files:**
- Create `app/src/main/java/com/mahweb/mahyarnfc/HceSessionController.java`
- Modify `app/src/main/java/com/mahweb/mahyarnfc/MainActivity.java`
- Modify `app/src/main/res/layout/view_send.xml`
- Create `scripts/verify_universal_send_contract.py`

**Interfaces:**
- `HceSessionController.activate(Activity): boolean`
- `HceSessionController.deactivate(Activity): void`

- [ ] Add contract test for preferred-service lifecycle and Universal UI copy; confirm RED.
- [ ] Implement `setPreferredService` / `unsetPreferredService` and safe observe-mode disable on supported versions.
- [ ] Activate only while send screen share is active.
- [ ] Show Android + iPhone compatibility and bridge/QR fallbacks.
- [ ] Confirm share is disabled on pause/exit.

### Task 5: Static iPhone contact bridge

**Files:**
- Create `card/index.html`
- Create `card/README.md`
- Create `.nojekyll`
- Create `scripts/verify_card_bridge.py`

**Interfaces:**
- Fragment format `#c=<base64url UTF-8 compact JSON>`.
- Browser renders card and generates `.vcf` locally.

- [ ] Write static contract test first and confirm RED.
- [ ] Implement RTL-friendly responsive card page with Save Contact, Call, Email, Website and fallback copy actions.
- [ ] Ensure no analytics/network APIs other than initial static page request.
- [ ] Document one-time GitHub Pages enablement.

### Task 6: Release integration and verification

**Files:**
- Modify `app/build.gradle.kts`
- Modify `.github/workflows/main.yml`
- Modify `README.md`
- Modify `scripts/verify_project.py`

- [ ] Set version 3.0.0 / 30 and artifact name.
- [ ] Run all pure JVM and contract tests.
- [ ] Parse all XML.
- [ ] Verify private AID/protocol constants unchanged.
- [ ] Package clean GitHub-ready and current-repo upload ZIPs.
- [ ] Use GitHub Actions to compile APK after user uploads because connector write permission is currently denied.
