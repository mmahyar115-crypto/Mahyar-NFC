# Mahyar NFC v1.3.0

GitHub-ready Android project. Upload the CONTENTS of this folder directly to a GitHub repository root.
A push to `main` or `master`, or a manual workflow dispatch, builds the debug APK automatically.

Build matrix: AGP 9.3.0 / Gradle 9.5.0 / JDK 17 / compileSdk 37 / Build Tools 36.0.0.

Artifact: `MahyarNFC-v1.3.0-debug-apk` containing `app-debug.apk` and its SHA-256 checksum.

# Mahyar NFC — Android MVP

نسخه 1.2 اپ موبایلی کارت هویت دیجیتال با NFC، بدون سرور و بدون پنل وب.

## قابلیت‌ها

- ذخیره پروفایل روی خود گوشی با SharedPreferences
- نام، عنوان شغلی، شرکت، موبایل، ایمیل، وب‌سایت، Instagram و Telegram
- حالت **ارسال NFC** با Android Host Card Emulation (HCE)
- حالت **دریافت NFC** با Android Reader Mode + IsoDep
- انتقال اطلاعات به‌صورت chunked APDU برای پایداری بهتر
- QR آفلاین بر پایه vCard برای گوشی‌هایی که اپ را ندارند
- باز کردن صفحه استاندارد Android برای افزودن مخاطب، بدون گرفتن مجوز مستقیم Contacts
- رابط فارسی و RTL

## روش تست NFC با دو گوشی

1. پروژه را در Android Studio باز و روی هر دو گوشی نصب کنید.
2. NFC هر دو گوشی روشن باشد.
3. در گوشی اول وارد **پروفایل** شوید و اطلاعات را ذخیره کنید.
4. در گوشی اول وارد **ارسال** شوید و `NFC Share` را روشن کنید.
5. در گوشی دوم وارد **دریافت** شوید.
6. پشت دو گوشی را نزدیک محل آنتن NFC نگه دارید.
7. پروفایل روی گوشی دوم نمایش داده می‌شود و می‌توان آن را به مخاطبین اضافه کرد.

> هر دو گوشی برای انتقال مستقیم گوشی‌به‌گوشی باید NFC داشته باشند و گوشی فرستنده از HCE پشتیبانی کند.

## گوشی مقابل اپ را ندارد

در تب **ارسال** روی «نمایش QR مشخصات» بزنید. QR یک vCard واقعی است و به سرور وابسته نیست.

## محدودیت مهم Android

Android اجازه نمی‌دهد یک اپ معمولی، گوشی فرستنده را به یک NFC Tag عمومیِ URL تبدیل کند که هر گوشی ناشناسی بدون داشتن اپ مخصوص بتواند آن را مثل تگ NFC معمولی بخواند. HCE از ISO-DEP/APDU استفاده می‌کند و Reader باید پروتکل اپ را بداند. به همین دلیل این MVP برای NFC مستقیم، اپ را روی دو گوشی می‌خواهد و برای حالت بدون اپ از QR استفاده می‌کند.

## ساخت APK در Android Studio

- Android Studio جدید را نصب کنید.
- پوشه `MahyarNFC` را با **Open** باز کنید.
- اگر SDK 37 نصب نیست، Android Studio پیشنهاد نصب آن را می‌دهد.
- صبر کنید Gradle Sync کامل شود.
- از منوی `Build > Build APK(s)` خروجی APK بگیرید.

پروژه از Android Gradle Plugin 9.3.0 و Gradle 9.5.0 استفاده می‌کند.

## ساخت از خط فرمان در ویندوز

```bat
gradlew.bat assembleDebug
```

APK معمولاً در این مسیر ساخته می‌شود:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## نکته Gradle Wrapper

برای کوچک ماندن ZIP، فایل باینری `gradle-wrapper.jar` داخل پروژه قرار داده نشده است. اسکریپت‌های `gradlew` و `gradlew.bat` در اولین اجرا نسخه رسمی wrapper مربوط به Gradle 9.5.0 را از `services.gradle.org` دریافت می‌کنند.

## پروتکل NFC این MVP

- AID: `F0010203040506`
- SELECT AID → پاسخ `MNF1 + length + 9000`
- READ BINARY (`00 B0 P1 P2 00`) → chunkهای حداکثر 220 بایت
- payload → JSON UTF-8

این پروتکل اختصاصی است و در نسخه بعد می‌توانیم رمزنگاری، امضای payload، چند پروفایل و Exchange Contact دوطرفه را به آن اضافه کنیم.

## ساخت خودکار APK با GitHub Actions

این پروژه فایل `.github/workflows/build-apk.yml` دارد. پس از قرار گرفتن پروژه روی GitHub:

1. وارد تب **Actions** مخزن شوید.
2. Workflow با نام **Build Android APK** را باز کنید.
3. روی **Run workflow** بزنید.
4. پس از اتمام Build، از بخش **Artifacts** فایل `MahyarNFC-v1.3.0-debug-apk` را دریافت کنید.
5. داخل Artifact فایل `app-debug.apk` قرار دارد و روی Android قابل نصب است.

این workflow طبق ماتریس سازگاری رسمی AGP 9.3 از JDK 17، Android SDK 37، Build Tools 36.0.0 و Gradle 9.5.0 استفاده می‌کند و سپس APK دیباگ را Build می‌کند.
