package com.mahweb.mahyarnfc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.WriterException;

public class MainActivity extends Activity implements NfcReader.Listener {
    private static final int BG = Color.rgb(246, 247, 250);
    private static final int CARD = Color.WHITE;
    private static final int TEXT = Color.rgb(24, 28, 36);
    private static final int MUTED = Color.rgb(99, 106, 120);
    private static final int PRIMARY = Color.rgb(16, 105, 255);
    private static final int SUCCESS = Color.rgb(19, 149, 92);
    private static final int BORDER = Color.rgb(226, 229, 236);

    private FrameLayout content;
    private Button navProfile;
    private Button navShare;
    private Button navReceive;
    private int currentTab = 0;
    private NfcReader nfcReader;
    private TextView receiverStatus;
    private LinearLayout receiverResult;
    private Profile lastReceived;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        nfcReader = new NfcReader(this, this);
        setContentView(buildRoot());
        showTab(0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcReader != null) nfcReader.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentTab == 2) startReaderMode();
    }

    private View buildRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(18), dp(18), dp(18), dp(12));

        TextView brand = text("Mahyar NFC", 24, TEXT, true);
        brand.setGravity(Gravity.CENTER_HORIZONTAL);
        brand.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        root.addView(brand, lpMatchWrap());

        TextView subtitle = text("کارت هویت دیجیتال، مستقیم از موبایل", 13, MUTED, false);
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(subtitle, lpMatchWrap(dp(4), dp(16)));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(4), dp(4), dp(4), dp(4));
        nav.setBackground(round(BORDER, dp(14), BORDER, 0));

        navProfile = navButton("پروفایل");
        navShare = navButton("ارسال");
        navReceive = navButton("دریافت");
        nav.addView(navProfile, weighted());
        nav.addView(navShare, weighted());
        nav.addView(navReceive, weighted());
        root.addView(nav, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        navProfile.setOnClickListener(v -> showTab(0));
        navShare.setOnClickListener(v -> showTab(1));
        navReceive.setOnClickListener(v -> showTab(2));

        content = new FrameLayout(this);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        cp.topMargin = dp(14);
        root.addView(content, cp);
        return root;
    }

    private void showTab(int tab) {
        currentTab = tab;
        if (nfcReader != null) nfcReader.stop();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        updateNav(navProfile, tab == 0);
        updateNav(navShare, tab == 1);
        updateNav(navReceive, tab == 2);

        content.removeAllViews();
        if (tab == 0) {
            content.addView(buildProfileScreen());
        } else if (tab == 1) {
            content.addView(buildShareScreen());
        } else {
            content.addView(buildReceiveScreen());
            startReaderMode();
        }
    }

    private View buildProfileScreen() {
        Profile p = ProfileRepository.load(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout box = vertical();
        box.setPadding(dp(2), dp(2), dp(2), dp(24));
        scroll.addView(box);

        box.addView(sectionTitle("مشخصات من", "این اطلاعات فقط روی همین گوشی ذخیره می‌شود."));

        EditText name = field("نام و نام خانوادگی", p.name, InputType.TYPE_CLASS_TEXT);
        EditText job = field("عنوان شغلی", p.job, InputType.TYPE_CLASS_TEXT);
        EditText company = field("شرکت / برند", p.company, InputType.TYPE_CLASS_TEXT);
        EditText phone = field("شماره موبایل", p.phone, InputType.TYPE_CLASS_PHONE);
        EditText email = field("ایمیل", p.email, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText website = field("وب‌سایت", p.website, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        EditText instagram = field("Instagram", p.instagram, InputType.TYPE_CLASS_TEXT);
        EditText telegram = field("Telegram", p.telegram, InputType.TYPE_CLASS_TEXT);

        box.addView(name);
        box.addView(job);
        box.addView(company);
        box.addView(phone);
        box.addView(email);
        box.addView(website);
        box.addView(instagram);
        box.addView(telegram);

        Button save = primaryButton("ذخیره مشخصات");
        LinearLayout.LayoutParams sp = lpMatchWrap(dp(8), dp(0));
        sp.height = dp(54);
        box.addView(save, sp);
        save.setOnClickListener(v -> {
            Profile edited = new Profile();
            edited.name = clean(name);
            edited.job = clean(job);
            edited.company = clean(company);
            edited.phone = clean(phone);
            edited.email = clean(email);
            edited.website = clean(website);
            edited.instagram = clean(instagram);
            edited.telegram = clean(telegram);
            ProfileRepository.save(this, edited);
            Toast.makeText(this, "مشخصات ذخیره شد", Toast.LENGTH_SHORT).show();
        });

        return scroll;
    }

    private View buildShareScreen() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = vertical();
        box.setPadding(dp(2), dp(2), dp(2), dp(24));
        scroll.addView(box);

        Profile p = ProfileRepository.load(this);
        LinearLayout card = cardBox();
        TextView avatar = text(initialOf(p.name), 30, Color.WHITE, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(round(PRIMARY, dp(38), PRIMARY, 0));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(dp(76), dp(76));
        ap.gravity = Gravity.CENTER_HORIZONTAL;
        card.addView(avatar, ap);

        TextView name = text(TextUtils.isEmpty(p.name) ? "پروفایل شما" : p.name, 22, TEXT, true);
        name.setGravity(Gravity.CENTER_HORIZONTAL);
        card.addView(name, lpMatchWrap(dp(12), 0));

        String line = joinNonEmpty(p.job, p.company, " • ");
        if (line.isEmpty()) line = "مشخصات خود را در تب پروفایل تکمیل کنید";
        TextView role = text(line, 14, MUTED, false);
        role.setGravity(Gravity.CENTER_HORIZONTAL);
        card.addView(role, lpMatchWrap(dp(4), dp(4)));
        box.addView(card, lpMatchWrap(0, dp(12)));

        boolean nfcExists = NfcAdapter.getDefaultAdapter(this) != null;
        boolean hce = getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION);
        boolean nfcOn = nfcExists && NfcAdapter.getDefaultAdapter(this).isEnabled();

        LinearLayout control = cardBox();
        TextView title = text("اشتراک‌گذاری NFC", 17, TEXT, true);
        control.addView(title);
        TextView desc = text(hce
                ? "با فعال کردن این گزینه، گوشی دوم که همین اپ در حالت دریافت دارد می‌تواند پروفایل را بخواند."
                : "این گوشی از Host Card Emulation پشتیبانی نمی‌کند؛ QR همچنان قابل استفاده است.", 13, MUTED, false);
        desc.setLineSpacing(dp(2), 1f);
        control.addView(desc, lpMatchWrap(dp(6), dp(8)));

        Switch shareSwitch = new Switch(this);
        shareSwitch.setText("NFC Share");
        shareSwitch.setTextSize(15);
        shareSwitch.setTextColor(TEXT);
        shareSwitch.setGravity(Gravity.CENTER_VERTICAL);
        shareSwitch.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        shareSwitch.setChecked(ProfileRepository.isShareEnabled(this) && hce);
        shareSwitch.setEnabled(hce && nfcOn);
        control.addView(shareSwitch, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        TextView status = statusPill(shareSwitch.isChecked() ? "آماده ارسال با NFC" : "NFC Share خاموش است", shareSwitch.isChecked());
        control.addView(status, lpMatchWrap(dp(4), dp(4)));

        shareSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ProfileRepository.setShareEnabled(this, isChecked);
            status.setText(isChecked ? "آماده ارسال با NFC" : "NFC Share خاموش است");
            status.setTextColor(isChecked ? SUCCESS : MUTED);
            status.setBackground(round(isChecked ? Color.rgb(231, 248, 240) : Color.rgb(242, 243, 246), dp(12), Color.TRANSPARENT, 0));
            if (isChecked) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });

        if (!nfcOn && nfcExists) {
            Button settings = secondaryButton("روشن کردن NFC");
            control.addView(settings, lpMatchWrap(dp(8), 0));
            settings.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                } catch (Exception e) {
                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                }
            });
        }
        box.addView(control, lpMatchWrap(0, dp(12)));

        LinearLayout qrCard = cardBox();
        qrCard.addView(text("ارسال بدون اپ روی گوشی مقابل", 17, TEXT, true));
        TextView qd = text("QR شامل vCard است؛ گوشی مقابل می‌تواند با دوربین آن را اسکن و اطلاعات تماس را دریافت کند.", 13, MUTED, false);
        qd.setLineSpacing(dp(2), 1f);
        qrCard.addView(qd, lpMatchWrap(dp(6), dp(10)));
        Button qr = primaryButton("نمایش QR مشخصات");
        qrCard.addView(qr, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));
        qr.setOnClickListener(v -> showQr(ProfileRepository.load(this)));
        box.addView(qrCard);

        if (shareSwitch.isChecked()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        return scroll;
    }

    private View buildReceiveScreen() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = vertical();
        box.setPadding(dp(2), dp(2), dp(2), dp(24));
        scroll.addView(box);

        LinearLayout scanner = cardBox();
        TextView icon = text("NFC", 28, PRIMARY, true);
        icon.setGravity(Gravity.CENTER);
        icon.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        scanner.addView(icon, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));

        TextView title = text("گوشی فرستنده را نزدیک کنید", 19, TEXT, true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        scanner.addView(title, lpMatchWrap(dp(4), 0));

        TextView help = text("در گوشی مقابل تب «ارسال» باز باشد و NFC Share فعال شده باشد. پشت دو گوشی را نزدیک آنتن NFC یکدیگر نگه دارید.", 13, MUTED, false);
        help.setGravity(Gravity.CENTER_HORIZONTAL);
        help.setLineSpacing(dp(3), 1f);
        scanner.addView(help, lpMatchWrap(dp(8), dp(12)));

        receiverStatus = statusPill("در حال آماده‌سازی NFC…", false);
        receiverStatus.setGravity(Gravity.CENTER);
        scanner.addView(receiverStatus, lpMatchWrap(0, dp(8)));

        Button retry = secondaryButton("شروع مجدد دریافت");
        scanner.addView(retry, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));
        retry.setOnClickListener(v -> startReaderMode());
        box.addView(scanner, lpMatchWrap(0, dp(12)));

        receiverResult = cardBox();
        receiverResult.setVisibility(View.GONE);
        box.addView(receiverResult);
        return scroll;
    }

    private void startReaderMode() {
        if (currentTab != 2 || receiverStatus == null) return;
        if (!nfcReader.isAvailable()) {
            receiverStatus.setText("این گوشی NFC ندارد");
            receiverStatus.setTextColor(Color.rgb(190, 70, 55));
            return;
        }
        if (!nfcReader.isEnabled()) {
            receiverStatus.setText("NFC خاموش است؛ آن را روشن کنید");
            receiverStatus.setTextColor(Color.rgb(190, 70, 55));
            return;
        }
        receiverStatus.setText("آماده دریافت — گوشی‌ها را نزدیک کنید");
        receiverStatus.setTextColor(SUCCESS);
        receiverStatus.setBackground(round(Color.rgb(231, 248, 240), dp(12), Color.TRANSPARENT, 0));
        nfcReader.start();
    }

    @Override
    public void onReading() {
        if (receiverStatus != null) {
            receiverStatus.setText("گوشی شناسایی شد؛ در حال دریافت…");
            receiverStatus.setTextColor(PRIMARY);
        }
    }

    @Override
    public void onProfile(Profile profile) {
        lastReceived = profile;
        if (receiverStatus != null) {
            receiverStatus.setText("پروفایل با موفقیت دریافت شد");
            receiverStatus.setTextColor(SUCCESS);
        }
        renderReceivedProfile(profile);
    }

    @Override
    public void onError(String message) {
        if (receiverStatus != null) {
            receiverStatus.setText(message);
            receiverStatus.setTextColor(Color.rgb(190, 70, 55));
            receiverStatus.setBackground(round(Color.rgb(253, 239, 237), dp(12), Color.TRANSPARENT, 0));
        }
    }

    private void renderReceivedProfile(Profile p) {
        if (receiverResult == null) return;
        receiverResult.removeAllViews();
        receiverResult.setVisibility(View.VISIBLE);
        TextView t = text(TextUtils.isEmpty(p.name) ? "پروفایل دریافت‌شده" : p.name, 21, TEXT, true);
        receiverResult.addView(t);
        addValue(receiverResult, "عنوان", p.job);
        addValue(receiverResult, "شرکت", p.company);
        addValue(receiverResult, "موبایل", p.phone);
        addValue(receiverResult, "ایمیل", p.email);
        addValue(receiverResult, "وب‌سایت", p.website);
        addValue(receiverResult, "Instagram", p.instagram);
        addValue(receiverResult, "Telegram", p.telegram);

        Button add = primaryButton("افزودن به مخاطبین");
        receiverResult.addView(add, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));
        add.setOnClickListener(v -> openContactInsert(p));
    }

    private void openContactInsert(Profile p) {
        Intent i = new Intent(Intent.ACTION_INSERT);
        i.setType(ContactsContract.Contacts.CONTENT_TYPE);
        i.putExtra(ContactsContract.Intents.Insert.NAME, p.name);
        i.putExtra(ContactsContract.Intents.Insert.PHONE, p.phone);
        i.putExtra(ContactsContract.Intents.Insert.EMAIL, p.email);
        i.putExtra(ContactsContract.Intents.Insert.COMPANY, p.company);
        i.putExtra(ContactsContract.Intents.Insert.JOB_TITLE, p.job);
        String notes = joinNonEmpty(
                !hasText(p.website) ? "" : "Website: " + p.website,
                joinNonEmpty(
                        !hasText(p.instagram) ? "" : "Instagram: " + p.instagram,
                        !hasText(p.telegram) ? "" : "Telegram: " + p.telegram,
                        "\n"),
                "\n");
        i.putExtra(ContactsContract.Intents.Insert.NOTES, notes);
        startActivity(i);
    }

    private void showQr(Profile p) {
        if (TextUtils.isEmpty(p.name) && TextUtils.isEmpty(p.phone) && TextUtils.isEmpty(p.email)) {
            Toast.makeText(this, "ابتدا مشخصات خود را وارد و ذخیره کنید", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            ImageView image = new ImageView(this);
            int size = Math.min(getResources().getDisplayMetrics().widthPixels - dp(64), dp(700));
            image.setImageBitmap(QrUtil.create(p.toVCard(), size));
            image.setPadding(dp(12), dp(12), dp(12), dp(12));

            LinearLayout wrap = vertical();
            wrap.setPadding(dp(16), dp(8), dp(16), dp(16));
            TextView hint = text("دوربین گوشی مقابل را روی QR بگیرید", 14, MUTED, false);
            hint.setGravity(Gravity.CENTER_HORIZONTAL);
            wrap.addView(hint, lpMatchWrap(dp(4), dp(8)));
            wrap.addView(image, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            new AlertDialog.Builder(this)
                    .setTitle(TextUtils.isEmpty(p.name) ? "QR مشخصات" : p.name)
                    .setView(wrap)
                    .setPositiveButton("بستن", null)
                    .show();
        } catch (WriterException e) {
            Toast.makeText(this, "ساخت QR ناموفق بود", Toast.LENGTH_SHORT).show();
        }
    }

    private void addValue(LinearLayout parent, String label, String value) {
        if (!hasText(value)) return;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        row.addView(text(label, 12, MUTED, false));
        TextView v = text(value, 15, TEXT, false);
        if (label.equals("موبایل") || label.equals("ایمیل") || label.equals("وب‌سایت") || label.equals("Instagram") || label.equals("Telegram")) {
            v.setTextDirection(View.TEXT_DIRECTION_LTR);
            v.setGravity(Gravity.START);
        }
        row.addView(v, lpMatchWrap(dp(2), 0));
        parent.addView(row);
    }

    private LinearLayout sectionTitle(String title, String sub) {
        LinearLayout b = vertical();
        b.addView(text(title, 21, TEXT, true));
        TextView s = text(sub, 13, MUTED, false);
        s.setLineSpacing(dp(2), 1f);
        b.addView(s, lpMatchWrap(dp(5), dp(12)));
        return b;
    }

    private EditText field(String hint, String value, int inputType) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setTextSize(15);
        e.setTextColor(TEXT);
        e.setHintTextColor(Color.rgb(145, 150, 160));
        e.setSingleLine(true);
        e.setInputType(inputType);
        e.setPadding(dp(14), 0, dp(14), 0);
        e.setBackground(round(CARD, dp(14), BORDER, dp(1)));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        p.bottomMargin = dp(10);
        e.setLayoutParams(p);
        if ((inputType & InputType.TYPE_CLASS_PHONE) == InputType.TYPE_CLASS_PHONE
                || (inputType & InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                || (inputType & InputType.TYPE_TEXT_VARIATION_URI) == InputType.TYPE_TEXT_VARIATION_URI
                || hint.equals("Instagram") || hint.equals("Telegram")) {
            e.setTextDirection(View.TEXT_DIRECTION_LTR);
            e.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        } else {
            e.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        }
        return e;
    }

    private LinearLayout cardBox() {
        LinearLayout l = vertical();
        l.setPadding(dp(18), dp(18), dp(18), dp(18));
        l.setBackground(round(CARD, dp(18), BORDER, dp(1)));
        return l;
    }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return l;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.START);
        t.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return t;
    }

    private TextView statusPill(String value, boolean ok) {
        TextView t = text(value, 13, ok ? SUCCESS : MUTED, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(12), dp(10), dp(12), dp(10));
        t.setBackground(round(ok ? Color.rgb(231, 248, 240) : Color.rgb(242, 243, 246), dp(12), Color.TRANSPARENT, 0));
        return t;
    }

    private Button navButton(String title) {
        Button b = new Button(this);
        b.setText(title);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setTextColor(MUTED);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setPadding(dp(4), 0, dp(4), 0);
        return b;
    }

    private void updateNav(Button b, boolean active) {
        b.setTextColor(active ? PRIMARY : MUTED);
        b.setTypeface(Typeface.create("sans", active ? Typeface.BOLD : Typeface.NORMAL));
        b.setBackground(active ? round(CARD, dp(11), Color.TRANSPARENT, 0) : round(Color.TRANSPARENT, 0, Color.TRANSPARENT, 0));
    }

    private Button primaryButton(String title) {
        Button b = new Button(this);
        b.setText(title);
        b.setTextSize(15);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackground(round(PRIMARY, dp(14), PRIMARY, 0));
        return b;
    }

    private Button secondaryButton(String title) {
        Button b = new Button(this);
        b.setText(title);
        b.setTextSize(14);
        b.setTextColor(PRIMARY);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackground(round(Color.rgb(239, 245, 255), dp(13), Color.rgb(213, 227, 255), dp(1)));
        return b;
    }

    private GradientDrawable round(int fill, int radius, int stroke, int strokeWidth) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(radius);
        if (strokeWidth > 0) d.setStroke(strokeWidth, stroke);
        return d;
    }

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
    }

    private LinearLayout.LayoutParams lpMatchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams lpMatchWrap(int top, int bottom) {
        LinearLayout.LayoutParams p = lpMatchWrap();
        p.topMargin = top;
        p.bottomMargin = bottom;
        return p;
    }

    private String clean(EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private static boolean hasText(String v) {
        return v != null && !v.trim().isEmpty();
    }

    private static String initialOf(String name) {
        if (name == null || name.trim().isEmpty()) return "M";
        return name.trim().substring(0, 1).toUpperCase();
    }

    private static String joinNonEmpty(String a, String b, String separator) {
        boolean aa = hasText(a);
        boolean bb = hasText(b);
        if (aa && bb) return a + separator + b;
        if (aa) return a;
        return bb ? b : "";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
