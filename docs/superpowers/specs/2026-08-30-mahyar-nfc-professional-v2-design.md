# Mahyar NFC — Professional UX/UI Redesign Specification

Date: 2026-08-30
Target: Android app, Java, Android API 36
Repository: mmahyar115-crypto/Mahyar-NFC

## 1. Goal

Transform the current functional NFC profile-sharing app into a polished, user-friendly product without changing the core NFC protocol unless a real defect is found.

Primary goals:
- First-run onboarding that collects and validates the user's profile.
- A modern, clear dashboard.
- Professional send / receive NFC flows.
- QR/vCard fallback.
- Local-only profile storage.
- Safe handling for phones without NFC/HCE or with NFC disabled.
- Keep the app lightweight and reliable.

## 2. Technical Direction

Use the existing Java Android project and migrate the UI from a large programmatic MainActivity toward structured XML layouts and focused Java controllers/activities where practical.

Do not rewrite the NFC protocol just for styling.

Core components to preserve:
- NfcCardService
- NfcReader
- NfcProtocol
- Profile JSON payload
- QR/vCard export
- SharedPreferences-based local storage

Target:
- compileSdk 36
- targetSdk 36
- minSdk 26
- Java 17

## 3. First-Run Onboarding

On first launch, if onboarding is not completed:

### Screen 1 — Welcome
- Mahyar NFC branding
- Simple explanation: digital contact card shared by NFC or QR
- Primary CTA: «شروع کنیم»

### Screen 2 — Essential profile
Required:
- Full name
- Mobile number

Optional:
- Job title
- Company / brand

Validation:
- Name cannot be blank.
- Phone cannot be blank.
- Trim whitespace.
- Show inline Persian error messages.

### Screen 3 — Contact and social
Optional:
- Email
- Website
- Instagram
- Telegram

Validation:
- Email syntax if entered.
- Website normalization/validation if entered.

### Screen 4 — Card preview
- Digital-card preview
- User can go back and edit
- Primary CTA: «ساخت کارت من»

On completion:
- Save profile locally.
- Set onboardingCompleted = true.
- Open main dashboard.

Existing users with a valid saved profile should not be forced through onboarding again.

## 4. Main Dashboard

Modern RTL dashboard.

Primary card:
- Avatar initials
- Full name
- Job title
- Company
- Essential contact summary
- Edit shortcut

Primary actions:
1. «ارسال با NFC»
2. «دریافت با NFC»
3. «نمایش QR»

Secondary status area:
- NFC available / unavailable
- NFC enabled / disabled
- HCE available / unavailable

Bottom navigation:
- خانه
- دریافت
- پروفایل

## 5. Send Flow

The Send screen must prevent confusing states.

Before enabling share:
- Profile must pass required-field validation.
- Device must support NFC.
- Device must support HCE for direct phone-to-phone NFC.
- NFC must be enabled.

States:
- Ready: green visual state
- NFC off: button opens NFC settings
- HCE unsupported: explain direct NFC is unavailable, offer QR
- Invalid/incomplete profile: link to profile editor

When NFC Share is enabled:
- Keep screen awake.
- Clearly show «آماده ارسال»
- Show instruction to bring the back of the other phone close.
- Include QR fallback on the same screen.

Leaving the share flow should not accidentally leave an unclear active state.

## 6. Receive Flow

Reader Mode activates only on the Receive screen.

States:
- Preparing
- Ready to receive
- Device detected / reading
- Success
- Error

Success card:
- Name
- Job/company
- Phone
- Email
- Website/socials where available

Actions:
- «ذخیره در مخاطبین»
- «دریافت کارت دیگر»
- optional copy/share actions if they do not add unnecessary complexity

Error handling:
- NFC unavailable
- NFC disabled
- sender share disabled
- unsupported NFC protocol
- interrupted transfer
- invalid payload

Errors must be understandable Persian UI messages, not technical exceptions.

## 7. Profile Screen

Editable profile with grouped sections:
- Personal
- Work
- Contact
- Social

Required markers for:
- Full name
- Mobile number

Actions:
- Save changes
- Preview card

Profile updates must immediately affect NFC and QR payloads.

## 8. Data Storage

Continue local-only SharedPreferences storage.

Add:
- onboarding_completed
- optional profile schema/version key for future migration

Do not add:
- Server
- Account system
- Cloud sync
- Analytics SDK

## 9. Visual Design System

Style:
- Clean modern fintech/contact-card feel
- RTL-first Persian UI
- Large touch targets
- Clear hierarchy
- Rounded cards
- Soft surfaces and restrained shadows
- One primary blue accent
- Green for ready/success
- Red only for actual errors
- Neutral gray backgrounds

Typography:
- System Android Persian-compatible font stack to avoid bundling external font files.
- Strong differentiation between title, section heading, body, helper text.

Accessibility:
- Minimum practical touch target ~48dp.
- Sufficient contrast.
- Do not rely on color alone for NFC status.
- Clear labels for every input.

## 10. Code Structure

Target decomposition:
- MainActivity: app shell/navigation only
- OnboardingActivity or dedicated onboarding flow
- Dashboard/Profile/Send/Receive UI controllers or focused screens
- ProfileRepository: persistence + onboarding state
- ProfileValidator: required fields and input validation
- NfcState helper: NFC/HCE availability state
- Existing NfcReader / NfcCardService / NfcProtocol retained

Avoid turning the new MainActivity into another monolithic file.

## 11. Safety / Privacy

- Profile data stays on the local device.
- NFC sharing happens only when user intentionally enables sharing.
- No silent contact write permission; saving received profiles uses Android contact insertion UI.
- No background server transmission.
- No hidden tracking.

## 12. Compatibility

Must work from Android API 26 through target API 36 where hardware capabilities allow.

Direct phone-to-phone NFC:
- Sender requires NFC + HCE.
- Receiver requires NFC and this app's reader implementation.
- QR fallback remains available when direct NFC cannot be used.

## 13. Verification

Before release:
- GitHub Actions debug build must pass.
- Verify APK artifact exists.
- Validate first-run onboarding.
- Validate saved-user relaunch bypasses onboarding.
- Validate form errors.
- Validate QR generation.
- Validate NFC-off and no-NFC UI states.
- Validate HCE-disabled/unsupported state.
- Validate received profile rendering.
- Validate Android Contacts insert intent.

Physical NFC behavior still requires a real two-phone test after APK build.

## 14. Release Target

Professional redesign release target:
- v2.0.0
- Debug APK first for device testing.
- After real-device NFC validation, prepare signed release build if requested.
