package com.mahweb.mahyarnfc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.WriterException;

public class MainActivity extends Activity implements NfcReader.Listener {
    private static final int SCREEN_HOME = 0;
    private static final int SCREEN_RECEIVE = 1;
    private static final int SCREEN_PROFILE = 2;
    private static final int SCREEN_SEND = 3;

    private FrameLayout contentContainer;
    private TextView navHome;
    private TextView navReceive;
    private TextView navProfile;
    private TextView headerBadge;
    private TextView headerSubtitle;
    private NfcReader nfcReader;
    private int currentScreen = SCREEN_HOME;
    private TextView receiveStatusTitle;
    private TextView receiveStatusDescription;
    private View receiveResultCard;
    private TextView receivedName;
    private TextView receivedRole;
    private TextView receivedPhone;
    private TextView receivedEmail;
    private TextView receivedWebsite;
    private TextView receivedSocial;
    private Profile lastReceived;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!ensureProfileReady()) return;

        getWindow().setStatusBarColor(Color.rgb(245, 247, 251));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(R.layout.activity_main);

        contentContainer = findViewById(R.id.contentContainer);
        navHome = findViewById(R.id.navHome);
        navReceive = findViewById(R.id.navReceive);
        navProfile = findViewById(R.id.navProfile);
        headerBadge = findViewById(R.id.headerBadge);
        headerSubtitle = findViewById(R.id.headerSubtitle);
        nfcReader = new NfcReader(this, this);

        navHome.setOnClickListener(v -> showDashboard());
        navReceive.setOnClickListener(v -> showReceiveScreen());
        navProfile.setOnClickListener(v -> showProfile());
        showDashboard();
    }

    private boolean ensureProfileReady() {
        if (!ProfileRepository.isOnboardingCompleted(this)) {
            if (ProfileRepository.hasUsableProfile(this)) {
                ProfileRepository.setOnboardingCompleted(this, true);
            } else {
                startActivity(new Intent(this, OnboardingActivity.class));
                finish();
                return false;
            }
        }
        if (!ProfileRepository.hasUsableProfile(this)) {
            ProfileRepository.setOnboardingCompleted(this, false);
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return false;
        }
        return true;
    }

    private void showDashboard() {
        leaveActiveNfcModes();
        currentScreen = SCREEN_HOME;
        setNavActive(SCREEN_HOME);
        headerSubtitle.setText("کارت تماس دیجیتال");
        Profile p = ProfileRepository.load(this);
        headerBadge.setText(initialOf(p.name));

        View view = LayoutInflater.from(this).inflate(R.layout.view_dashboard, contentContainer, false);
        ((TextView) view.findViewById(R.id.dashboardAvatar)).setText(initialOf(p.name));
        ((TextView) view.findViewById(R.id.dashboardName)).setText(p.name);
        String role = join(p.job, p.company);
        ((TextView) view.findViewById(R.id.dashboardRole)).setText(role.isEmpty() ? "کارت تماس دیجیتال" : role);
        ((TextView) view.findViewById(R.id.dashboardPhone)).setText(p.phone);
        TextView email = view.findViewById(R.id.dashboardEmail);
        if (TextUtils.isEmpty(p.email)) email.setVisibility(View.GONE);
        else { email.setText(p.email); email.setVisibility(View.VISIBLE); }

        renderDashboardNfcState(view);
        view.findViewById(R.id.btnEditProfile).setOnClickListener(v -> showProfile());
        view.findViewById(R.id.btnShowQr).setOnClickListener(v -> showQr(ProfileRepository.load(this)));
        view.findViewById(R.id.btnSendNfc).setOnClickListener(v -> showSendScreen());
        view.findViewById(R.id.btnReceiveNfc).setOnClickListener(v -> showReceiveScreen());
        replaceContent(view);
    }

    private void renderDashboardNfcState(View view) {
        TextView status = view.findViewById(R.id.dashboardNfcStatus);
        TextView help = view.findViewById(R.id.dashboardNfcHelp);
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        boolean hasHce = getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION);
        if (adapter == null) {
            status.setText("پشتیبانی نمی‌شود");
            status.setTextColor(getColor(R.color.danger));
            help.setText("این گوشی NFC ندارد؛ برای اشتراک‌گذاری از QR استفاده کنید.");
        } else if (!adapter.isEnabled()) {
            status.setText("خاموش");
            status.setTextColor(getColor(R.color.danger));
            help.setText("NFC خاموش است. برای ارسال یا دریافت مستقیم آن را روشن کنید.");
        } else if (!hasHce) {
            status.setText("فقط دریافت");
            status.setTextColor(getColor(R.color.text_secondary));
            help.setText("NFC روشن است اما این گوشی ارسال HCE را پشتیبانی نمی‌کند؛ QR در دسترس است.");
        } else {
            status.setText("آماده");
            status.setTextColor(getColor(R.color.success));
            status.setBackgroundResource(R.drawable.bg_chip_success);
            help.setText("NFC روشن است و گوشی برای ارسال و دریافت آماده است.");
        }
    }

    private void showProfile() {
        leaveActiveNfcModes();
        currentScreen = SCREEN_PROFILE;
        setNavActive(SCREEN_PROFILE);
        headerSubtitle.setText("پروفایل من");
        Profile p = ProfileRepository.load(this);
        headerBadge.setText(initialOf(p.name));

        View view = LayoutInflater.from(this).inflate(R.layout.view_profile, contentContainer, false);
        EditText name = view.findViewById(R.id.profileName);
        EditText phone = view.findViewById(R.id.profilePhone);
        EditText job = view.findViewById(R.id.profileJob);
        EditText company = view.findViewById(R.id.profileCompany);
        EditText email = view.findViewById(R.id.profileEmail);
        EditText website = view.findViewById(R.id.profileWebsite);
        EditText instagram = view.findViewById(R.id.profileInstagram);
        EditText telegram = view.findViewById(R.id.profileTelegram);
        name.setText(p.name); phone.setText(p.phone); job.setText(p.job); company.setText(p.company);
        email.setText(p.email); website.setText(p.website); instagram.setText(p.instagram); telegram.setText(p.telegram);

        Button save = view.findViewById(R.id.btnSaveProfile);
        Button preview = view.findViewById(R.id.btnPreviewProfile);
        save.setOnClickListener(v -> {
            Profile edited = collectProfile(name, phone, job, company, email, website, instagram, telegram);
            if (!validateProfileInputs(edited, name, phone, email, website)) return;
            ProfileRepository.save(this, edited);
            ProfileRepository.setOnboardingCompleted(this, true);
            headerBadge.setText(initialOf(edited.name));
            Toast.makeText(this, "تغییرات پروفایل ذخیره شد", Toast.LENGTH_SHORT).show();
        });
        preview.setOnClickListener(v -> {
            Profile edited = collectProfile(name, phone, job, company, email, website, instagram, telegram);
            if (!validateProfileInputs(edited, name, phone, email, website)) return;
            showCardPreview(edited);
        });
        replaceContent(view);
    }

    private Profile collectProfile(EditText name, EditText phone, EditText job, EditText company,
                                   EditText email, EditText website, EditText instagram, EditText telegram) {
        Profile p = new Profile();
        p.name = value(name); p.phone = value(phone); p.job = value(job); p.company = value(company);
        p.email = value(email); p.website = ProfileValidator.normalizeWebsite(value(website));
        p.instagram = value(instagram); p.telegram = value(telegram);
        return p;
    }

    private boolean validateProfileInputs(Profile p, EditText name, EditText phone, EditText email, EditText website) {
        String eName = ProfileValidator.validateName(p.name);
        String ePhone = ProfileValidator.validatePhone(p.phone);
        String eEmail = ProfileValidator.validateEmail(p.email);
        String eWebsite = ProfileValidator.validateWebsite(p.website);
        name.setError(eName); phone.setError(ePhone); email.setError(eEmail); website.setError(eWebsite);
        if (eName != null) name.requestFocus();
        else if (ePhone != null) phone.requestFocus();
        else if (eEmail != null) email.requestFocus();
        else if (eWebsite != null) website.requestFocus();
        return eName == null && ePhone == null && eEmail == null && eWebsite == null;
    }

    private void showCardPreview(Profile p) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(22);
        wrap.setPadding(pad, pad, pad, pad);
        TextView name = dialogText(p.name, 22, true);
        wrap.addView(name);
        String role = join(p.job, p.company);
        if (!role.isEmpty()) wrap.addView(dialogText(role, 14, false));
        wrap.addView(dialogText(p.phone, 15, false));
        if (!TextUtils.isEmpty(p.email)) wrap.addView(dialogText(p.email, 14, false));
        if (!TextUtils.isEmpty(p.website)) wrap.addView(dialogText(p.website, 14, false));
        new AlertDialog.Builder(this)
                .setTitle("پیش‌نمایش کارت")
                .setView(wrap)
                .setPositiveButton("بستن", null)
                .show();
    }

    private void showQr(Profile p) {
        if (!ProfileValidator.isProfileReady(p)) {
            Toast.makeText(this, "ابتدا نام و شماره موبایل را کامل کنید", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            ImageView image = new ImageView(this);
            int size = Math.min(getResources().getDisplayMetrics().widthPixels - dp(64), dp(700));
            image.setImageBitmap(QrUtil.create(p.toVCard(), size));
            image.setPadding(dp(12), dp(12), dp(12), dp(12));
            LinearLayout wrap = new LinearLayout(this);
            wrap.setOrientation(LinearLayout.VERTICAL);
            wrap.setPadding(dp(16), dp(8), dp(16), dp(16));
            TextView hint = dialogText("دوربین گوشی مقابل را روی QR بگیرید", 14, false);
            hint.setTextColor(getColor(R.color.text_secondary));
            wrap.addView(hint);
            wrap.addView(image, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            new AlertDialog.Builder(this).setTitle(p.name).setView(wrap).setPositiveButton("بستن", null).show();
        } catch (WriterException e) {
            Toast.makeText(this, "ساخت QR ناموفق بود", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSendScreen() {
        leaveActiveNfcModes();
        currentScreen = SCREEN_SEND;
        setNavActive(SCREEN_HOME);
        headerSubtitle.setText("ارسال کارت");

        Profile profile = ProfileRepository.load(this);
        View view = LayoutInflater.from(this).inflate(R.layout.view_send, contentContainer, false);
        TextView sendName = view.findViewById(R.id.sendProfileName);
        TextView sendRole = view.findViewById(R.id.sendProfileRole);
        TextView statusTitle = view.findViewById(R.id.sendStatusTitle);
        TextView statusDescription = view.findViewById(R.id.sendStatusDescription);
        TextView instruction = view.findViewById(R.id.sendReadyInstruction);
        Button btnToggleShare = view.findViewById(R.id.btnToggleShare);
        Button settingsButton = view.findViewById(R.id.btnOpenNfcSettings);
        Button qrButton = view.findViewById(R.id.btnSendQr);

        sendName.setText(profile.name);
        String role = join(profile.job, profile.company);
        sendRole.setText(role.isEmpty() ? "کارت تماس دیجیتال" : role);

        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        boolean nfcAvailable = adapter != null;
        boolean nfcEnabled = adapter != null && adapter.isEnabled();
        boolean hceAvailable = getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION);
        NfcState.Status status = NfcState.evaluate(ProfileValidator.isProfileReady(profile), nfcAvailable, hceAvailable, nfcEnabled);

        statusTitle.setText(NfcState.title(status));
        statusDescription.setText(NfcState.description(status));
        boolean ready = NfcState.canShare(status);
        btnToggleShare.setEnabled(ready);
        btnToggleShare.setAlpha(ready ? 1f : 0.45f);
        settingsButton.setVisibility(status == NfcState.Status.NFC_OFF ? View.VISIBLE : View.GONE);
        instruction.setVisibility(View.GONE);
        ProfileRepository.setShareEnabled(this, false);

        if (status == NfcState.Status.READY) {
            statusTitle.setTextColor(getColor(R.color.success));
        } else if (status == NfcState.Status.NFC_OFF || status == NfcState.Status.NFC_UNAVAILABLE) {
            statusTitle.setTextColor(getColor(R.color.danger));
        } else {
            statusTitle.setTextColor(getColor(R.color.text_primary));
        }

        btnToggleShare.setOnClickListener(v -> {
            boolean active = !ProfileRepository.isShareEnabled(this);
            ProfileRepository.setShareEnabled(this, active);
            if (active) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                statusTitle.setText("آماده ارسال");
                statusTitle.setTextColor(getColor(R.color.success));
                statusDescription.setText("حالت ارسال فعال است. در گوشی مقابل صفحه «دریافت» را باز کنید و پشت دو گوشی را نزدیک نگه دارید.");
                instruction.setVisibility(View.VISIBLE);
                btnToggleShare.setText("توقف ارسال NFC");
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                statusTitle.setText(NfcState.title(status));
                statusDescription.setText(NfcState.description(status));
                instruction.setVisibility(View.GONE);
                btnToggleShare.setText("فعال کردن ارسال NFC");
            }
        });

        settingsButton.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
            }
        });
        qrButton.setOnClickListener(v -> showQr(ProfileRepository.load(this)));
        replaceContent(view);
    }

    private void showReceiveScreen() {
        leaveActiveNfcModes();
        currentScreen = SCREEN_RECEIVE;
        setNavActive(SCREEN_RECEIVE);
        headerSubtitle.setText("دریافت کارت");
        lastReceived = null;

        View view = LayoutInflater.from(this).inflate(R.layout.view_receive, contentContainer, false);
        receiveStatusTitle = view.findViewById(R.id.receiveStatusTitle);
        receiveStatusDescription = view.findViewById(R.id.receiveStatusDescription);
        receiveResultCard = view.findViewById(R.id.receiveResultCard);
        receivedName = view.findViewById(R.id.receivedName);
        receivedRole = view.findViewById(R.id.receivedRole);
        receivedPhone = view.findViewById(R.id.receivedPhone);
        receivedEmail = view.findViewById(R.id.receivedEmail);
        receivedWebsite = view.findViewById(R.id.receivedWebsite);
        receivedSocial = view.findViewById(R.id.receivedSocial);
        Button retry = view.findViewById(R.id.btnReceiveRetry);
        Button settings = view.findViewById(R.id.btnOpenReceiveNfcSettings);
        Button save = view.findViewById(R.id.btnSaveContact);
        Button another = view.findViewById(R.id.btnReceiveAnother);

        retry.setOnClickListener(v -> startReceiveMode());
        settings.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
            }
        });
        save.setOnClickListener(v -> {
            if (lastReceived != null) openContactInsert(lastReceived);
        });
        another.setOnClickListener(v -> {
            lastReceived = null;
            receiveResultCard.setVisibility(View.GONE);
            startReceiveMode();
        });

        replaceContent(view);
        startReceiveMode();
    }

    private void startReceiveMode() {
        if (currentScreen != SCREEN_RECEIVE || receiveStatusTitle == null) return;
        View root = receiveStatusTitle.getRootView();
        Button settings = root.findViewById(R.id.btnOpenReceiveNfcSettings);
        if (!nfcReader.isAvailable()) {
            receiveStatusTitle.setText("این گوشی NFC ندارد");
            receiveStatusTitle.setTextColor(getColor(R.color.danger));
            receiveStatusDescription.setText("دریافت مستقیم کارت با NFC روی این دستگاه ممکن نیست.");
            settings.setVisibility(View.GONE);
            return;
        }
        if (!nfcReader.isEnabled()) {
            receiveStatusTitle.setText("NFC خاموش است");
            receiveStatusTitle.setTextColor(getColor(R.color.danger));
            receiveStatusDescription.setText("NFC را روشن کنید و سپس «شروع مجدد دریافت» را بزنید.");
            settings.setVisibility(View.VISIBLE);
            return;
        }
        settings.setVisibility(View.GONE);
        receiveStatusTitle.setText("آماده دریافت");
        receiveStatusTitle.setTextColor(getColor(R.color.success));
        receiveStatusDescription.setText("پشت گوشی فرستنده را نزدیک این گوشی نگه دارید. در گوشی فرستنده باید ارسال NFC فعال باشد.");
        nfcReader.stop();
        nfcReader.start();
    }

    private void renderReceivedProfile(Profile p) {
        if (receiveResultCard == null) return;
        receiveResultCard.setVisibility(View.VISIBLE);
        receivedName.setText(TextUtils.isEmpty(p.name) ? "مخاطب جدید" : p.name);
        String role = join(p.job, p.company);
        setOptionalText(receivedRole, role);
        receivedPhone.setText(TextUtils.isEmpty(p.phone) ? "شماره موبایل ثبت نشده" : p.phone);
        setOptionalText(receivedEmail, p.email);
        setOptionalText(receivedWebsite, p.website);
        String social = joinLtr(
                TextUtils.isEmpty(p.instagram) ? "" : "Instagram: " + p.instagram,
                TextUtils.isEmpty(p.telegram) ? "" : "Telegram: " + p.telegram);
        setOptionalText(receivedSocial, social);
    }

    private void openContactInsert(Profile p) {
        Intent i = new Intent(Intent.ACTION_INSERT);
        i.setType(ContactsContract.Contacts.CONTENT_TYPE);
        i.putExtra(ContactsContract.Intents.Insert.NAME, p.name);
        i.putExtra(ContactsContract.Intents.Insert.PHONE, p.phone);
        i.putExtra(ContactsContract.Intents.Insert.EMAIL, p.email);
        i.putExtra(ContactsContract.Intents.Insert.COMPANY, p.company);
        i.putExtra(ContactsContract.Intents.Insert.JOB_TITLE, p.job);
        String notes = joinLines(
                TextUtils.isEmpty(p.website) ? "" : "Website: " + p.website,
                TextUtils.isEmpty(p.instagram) ? "" : "Instagram: " + p.instagram,
                TextUtils.isEmpty(p.telegram) ? "" : "Telegram: " + p.telegram);
        if (!notes.isEmpty()) i.putExtra(ContactsContract.Intents.Insert.NOTES, notes);
        startActivity(i);
    }

    private void replaceContent(View view) {
        contentContainer.removeAllViews();
        contentContainer.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void setNavActive(int screen) {
        styleNav(navHome, screen == SCREEN_HOME || screen == SCREEN_SEND);
        styleNav(navReceive, screen == SCREEN_RECEIVE);
        styleNav(navProfile, screen == SCREEN_PROFILE);
    }

    private void styleNav(TextView nav, boolean active) {
        nav.setTextColor(getColor(active ? R.color.primary : R.color.text_secondary));
        nav.setTypeface(android.graphics.Typeface.DEFAULT, active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void leaveActiveNfcModes() {
        if (nfcReader != null) nfcReader.stop();
        ProfileRepository.setShareEnabled(this, false);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override protected void onPause() {
        super.onPause();
        if (nfcReader != null) nfcReader.stop();
        if (currentScreen == SCREEN_SEND) {
            ProfileRepository.setShareEnabled(this, false);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (contentContainer == null) return;
        if (currentScreen == SCREEN_HOME) showDashboard();
        else if (currentScreen == SCREEN_SEND) showSendScreen();
        else if (currentScreen == SCREEN_RECEIVE) startReceiveMode();
    }

    @Override public void onReading() {
        if (currentScreen != SCREEN_RECEIVE || receiveStatusTitle == null) return;
        receiveStatusTitle.setText("گوشی شناسایی شد");
        receiveStatusTitle.setTextColor(getColor(R.color.primary));
        receiveStatusDescription.setText("در حال دریافت اطلاعات کارت… گوشی‌ها را چند لحظه ثابت نگه دارید.");
    }

    @Override public void onProfile(Profile profile) {
        if (currentScreen != SCREEN_RECEIVE) return;
        lastReceived = profile;
        if (receiveStatusTitle != null) {
            receiveStatusTitle.setText("دریافت با موفقیت انجام شد");
            receiveStatusTitle.setTextColor(getColor(R.color.success));
            receiveStatusDescription.setText("اطلاعات را بررسی کنید و در صورت تمایل در مخاطبین ذخیره کنید.");
        }
        renderReceivedProfile(profile);
        if (nfcReader != null) nfcReader.stop();
    }

    @Override public void onError(String message) {
        if (currentScreen != SCREEN_RECEIVE || receiveStatusTitle == null) return;
        receiveStatusTitle.setText("دریافت انجام نشد");
        receiveStatusTitle.setTextColor(getColor(R.color.danger));
        receiveStatusDescription.setText(TextUtils.isEmpty(message) ? "خطایی در خواندن NFC رخ داد. دوباره تلاش کنید." : message);
    }

    private static void setOptionalText(TextView view, String value) {
        if (TextUtils.isEmpty(value)) {
            view.setVisibility(View.GONE);
        } else {
            view.setText(value);
            view.setVisibility(View.VISIBLE);
        }
    }

    private static String joinLtr(String a, String b) {
        if (TextUtils.isEmpty(a)) return b == null ? "" : b;
        if (TextUtils.isEmpty(b)) return a;
        return a + "  •  " + b;
    }

    private static String joinLines(String... values) {
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            if (TextUtils.isEmpty(value)) continue;
            if (out.length() > 0) out.append("\n");
            out.append(value);
        }
        return out.toString();
    }

    private TextView dialogText(String text, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(getColor(R.color.text_primary));
        t.setPadding(0, dp(5), 0, dp(5));
        t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return t;
    }

    private static String value(EditText editText) {
        return ProfileValidator.clean(editText.getText() == null ? "" : editText.getText().toString());
    }

    private static String initialOf(String name) {
        String v = ProfileValidator.clean(name);
        return v.isEmpty() ? "M" : String.valueOf(v.charAt(0)).toUpperCase();
    }

    private static String join(String a, String b) {
        a = ProfileValidator.clean(a); b = ProfileValidator.clean(b);
        if (a.isEmpty()) return b;
        if (b.isEmpty()) return a;
        return a + " • " + b;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
