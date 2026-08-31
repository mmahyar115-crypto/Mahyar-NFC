# Mahyar NFC Professional v2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver Mahyar NFC v2.0.0 with first-run onboarding, modern RTL dashboard, robust profile validation, polished NFC send/receive flows, and QR fallback while preserving the working NFC protocol.

**Architecture:** Keep the existing NFC transport layer (`NfcProtocol`, `NfcReader`, `NfcCardService`) stable. Add pure validation/state helpers and split presentation into focused Activities with XML layouts: `OnboardingActivity` for first-run setup and `MainActivity` for dashboard/navigation. Persist onboarding state in `ProfileRepository` and route launch based on profile completion.

**Tech Stack:** Android Java 17, XML layouts, Android SDK 36, SharedPreferences, Host Card Emulation, NFC Reader Mode, ZXing core 3.5.3, JUnit 4 for pure JVM tests.

**Spec:** `docs/superpowers/specs/2026-08-30-mahyar-nfc-professional-v2-design.md`

## Global Constraints

- `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`.
- Keep data local-only; no server, account, analytics, or cloud sync.
- Keep `NfcProtocol` wire format/AID unchanged unless a verified bug requires otherwise.
- Sender direct NFC requires NFC + HCE; receiver requires NFC + this app.
- QR/vCard fallback remains available.
- Full name and mobile are required before onboarding completion or NFC Share activation.
- UI is RTL-first Persian with system fonts and >=48dp touch targets.
- Version target is `2.0.0` / `versionCode = 20`.

---

### Task 1: Profile validation and onboarding persistence

**Files:**
- Create: `app/src/main/java/com/mahweb/mahyarnfc/ProfileValidator.java`
- Modify: `app/src/main/java/com/mahweb/mahyarnfc/ProfileRepository.java`
- Create: `app/src/test/java/com/mahweb/mahyarnfc/ProfileValidatorTest.java`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- Produces: `ProfileValidator.validateName(String)`, `validatePhone(String)`, `validateEmail(String)`, `normalizeWebsite(String)`, `isProfileReady(Profile)`.
- Produces: `ProfileRepository.isOnboardingCompleted(Context)`, `setOnboardingCompleted(Context, boolean)`, `hasUsableProfile(Context)`.

- [ ] Write failing JVM tests covering blank name, blank phone, valid/invalid email, website normalization, and required-profile readiness.
- [ ] Run tests and verify RED because `ProfileValidator` does not exist.
- [ ] Implement minimal `ProfileValidator` and repository onboarding keys.
- [ ] Run tests and verify GREEN.
- [ ] Commit task.

### Task 2: First-run onboarding experience

**Files:**
- Create: `app/src/main/java/com/mahweb/mahyarnfc/OnboardingActivity.java`
- Create: `app/src/main/res/layout/activity_onboarding.xml`
- Create: `app/src/main/res/drawable/bg_primary_button.xml`
- Create: `app/src/main/res/drawable/bg_secondary_button.xml`
- Create: `app/src/main/res/drawable/bg_card.xml`
- Create: `app/src/main/res/drawable/bg_input.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `ProfileValidator` and `ProfileRepository` from Task 1.
- Produces: first-run 4-step flow ending with saved `Profile` and `onboardingCompleted=true`.

- [ ] Add a static/source test that asserts onboarding layout IDs and manifest Activity registration; verify it fails first.
- [ ] Implement XML and `OnboardingActivity` with welcome, essential info, contact/social info, and card preview steps.
- [ ] Validate required inputs inline and normalize website before save.
- [ ] On completion persist profile and onboarding flag, then open `MainActivity`.
- [ ] Re-run source tests.
- [ ] Commit task.

### Task 3: Modern dashboard and profile editor

**Files:**
- Replace: `app/src/main/java/com/mahweb/mahyarnfc/MainActivity.java`
- Create: `app/src/main/res/layout/activity_main.xml`
- Create: `app/src/main/res/layout/view_dashboard.xml`
- Create: `app/src/main/res/layout/view_profile.xml`
- Create: `app/src/main/res/drawable/bg_chip_success.xml`
- Create: `app/src/main/res/drawable/bg_chip_neutral.xml`
- Modify: `app/src/main/res/values/styles.xml`

**Interfaces:**
- Consumes: `ProfileRepository`, `ProfileValidator`.
- Produces: dashboard navigation and editable profile screen.

- [ ] Add source assertions for dashboard action IDs and profile required-field IDs; verify RED.
- [ ] Implement a clean RTL dashboard with digital card, NFC/QR actions, state summary, and bottom navigation.
- [ ] Implement grouped profile editor with Save + Preview, preserving all current profile fields.
- [ ] On app launch, redirect to onboarding when no completed usable profile exists.
- [ ] Re-run source tests.
- [ ] Commit task.

### Task 4: Professional NFC send flow

**Files:**
- Create: `app/src/main/res/layout/view_send.xml`
- Create: `app/src/main/java/com/mahweb/mahyarnfc/NfcState.java`
- Modify: `app/src/main/java/com/mahweb/mahyarnfc/MainActivity.java`
- Create: `app/src/test/java/com/mahweb/mahyarnfc/NfcStateTest.java` for pure state-label mapping where possible.

**Interfaces:**
- Produces: `NfcState.Status` values `READY`, `NFC_OFF`, `NFC_UNAVAILABLE`, `HCE_UNAVAILABLE`, `PROFILE_INCOMPLETE`.
- MainActivity renders status and only enables share in `READY` state.

- [ ] Write failing tests for state priority/messaging helper.
- [ ] Implement `NfcState` and pass tests.
- [ ] Implement send screen UI states, NFC settings CTA, HCE fallback messaging, QR fallback, keep-screen-awake only while sharing.
- [ ] Ensure leaving send screen disables the UI session state consistently and does not leave misleading status text.
- [ ] Commit task.

### Task 5: Professional receive and contact-save flow

**Files:**
- Create: `app/src/main/res/layout/view_receive.xml`
- Modify: `app/src/main/java/com/mahweb/mahyarnfc/MainActivity.java`
- Modify only if needed: `app/src/main/java/com/mahweb/mahyarnfc/NfcReader.java`

**Interfaces:**
- Consumes existing `NfcReader.Listener` callbacks.
- Produces clear receive states and a received-profile card with Android Contacts insertion.

- [ ] Add source assertions for receive state/result/action IDs and verify RED.
- [ ] Implement ready/reading/success/error visual states.
- [ ] Render all non-empty received profile fields safely.
- [ ] Add `ContactsContract.Intents.Insert` action without direct contacts permission.
- [ ] Add “receive another card” reset action.
- [ ] Keep `NfcReader` protocol behavior unchanged unless source-level review exposes a concrete issue.
- [ ] Commit task.

### Task 6: QR polish, versioning, regression checks, release package

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `README.md`
- Modify: `.github/workflows/main.yml`
- Create/update: `app/src/test/...` and `scripts/verify_project.py`

**Interfaces:**
- Produces v2.0.0 source package and CI artifact named `MahyarNFC-v2.0.0-debug-apk`.

- [ ] Add verification script checking AID consistency, manifest services/activities, SDK/version values, required resources, required navigation IDs, and workflow artifact name.
- [ ] Run JVM tests and verification script.
- [ ] Update version to `versionCode=20`, `versionName="2.0.0"` and CI artifact name.
- [ ] Update README with onboarding, NFC requirements, QR fallback, privacy, and two-phone test steps.
- [ ] Run final local verification and inspect git diff for accidental protocol changes.
- [ ] Package clean ZIP for GitHub upload.
- [ ] Commit task.

## Final verification

- Pure JVM validation/state tests pass.
- `scripts/verify_project.py` passes with zero failures.
- No changes to AID or APDU framing.
- Android API 36 configuration is consistent.
- GitHub Actions must produce `app-debug.apk` before declaring the Android build verified.
- Physical two-phone NFC test remains required before declaring real-device NFC compatibility complete.
