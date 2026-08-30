package com.mahweb.mahyarnfc;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class OnboardingActivity extends Activity {
    private LinearLayout stepWelcome;
    private LinearLayout stepEssential;
    private LinearLayout stepContact;
    private LinearLayout stepPreview;
    private ProgressBar progress;
    private TextView progressText;

    private EditText inputName;
    private EditText inputPhone;
    private EditText inputJob;
    private EditText inputCompany;
    private EditText inputEmail;
    private EditText inputWebsite;
    private EditText inputInstagram;
    private EditText inputTelegram;

    private TextView previewAvatar;
    private TextView previewName;
    private TextView previewRole;
    private TextView previewContact;
    private int step = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(245, 247, 251));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(R.layout.activity_onboarding);
        bindViews();
        loadExistingProfile();
        bindActions();
        showStep(0);
    }

    private void bindViews() {
        stepWelcome = findViewById(R.id.stepWelcome);
        stepEssential = findViewById(R.id.stepEssential);
        stepContact = findViewById(R.id.stepContact);
        stepPreview = findViewById(R.id.stepPreview);
        progress = findViewById(R.id.onboardingProgress);
        progressText = findViewById(R.id.progressText);

        inputName = findViewById(R.id.inputName);
        inputPhone = findViewById(R.id.inputPhone);
        inputJob = findViewById(R.id.inputJob);
        inputCompany = findViewById(R.id.inputCompany);
        inputEmail = findViewById(R.id.inputEmail);
        inputWebsite = findViewById(R.id.inputWebsite);
        inputInstagram = findViewById(R.id.inputInstagram);
        inputTelegram = findViewById(R.id.inputTelegram);

        previewAvatar = findViewById(R.id.previewAvatar);
        previewName = findViewById(R.id.previewName);
        previewRole = findViewById(R.id.previewRole);
        previewContact = findViewById(R.id.previewContact);
    }

    private void bindActions() {
        findViewById(R.id.btnWelcomeNext).setOnClickListener(v -> showStep(1));
        findViewById(R.id.btnEssentialBack).setOnClickListener(v -> showStep(0));
        findViewById(R.id.btnEssentialNext).setOnClickListener(v -> {
            if (validateEssential()) showStep(2);
        });
        findViewById(R.id.btnContactBack).setOnClickListener(v -> showStep(1));
        findViewById(R.id.btnContactNext).setOnClickListener(v -> {
            if (validateContact()) {
                updatePreview();
                showStep(3);
            }
        });
        findViewById(R.id.btnPreviewBack).setOnClickListener(v -> showStep(2));
        findViewById(R.id.btnCreateCard).setOnClickListener(v -> completeOnboarding());
    }

    private void loadExistingProfile() {
        Profile p = ProfileRepository.load(this);
        inputName.setText(p.name);
        inputPhone.setText(p.phone);
        inputJob.setText(p.job);
        inputCompany.setText(p.company);
        inputEmail.setText(p.email);
        inputWebsite.setText(p.website);
        inputInstagram.setText(p.instagram);
        inputTelegram.setText(p.telegram);
    }

    private boolean validateEssential() {
        String nameError = ProfileValidator.validateName(value(inputName));
        String phoneError = ProfileValidator.validatePhone(value(inputPhone));
        inputName.setError(nameError);
        inputPhone.setError(phoneError);
        if (nameError != null) inputName.requestFocus();
        else if (phoneError != null) inputPhone.requestFocus();
        return nameError == null && phoneError == null;
    }

    private boolean validateContact() {
        String emailError = ProfileValidator.validateEmail(value(inputEmail));
        String websiteError = ProfileValidator.validateWebsite(value(inputWebsite));
        inputEmail.setError(emailError);
        inputWebsite.setError(websiteError);
        if (emailError != null) inputEmail.requestFocus();
        else if (websiteError != null) inputWebsite.requestFocus();
        return emailError == null && websiteError == null;
    }

    private Profile collectProfile() {
        Profile p = new Profile();
        p.name = value(inputName);
        p.phone = value(inputPhone);
        p.job = value(inputJob);
        p.company = value(inputCompany);
        p.email = value(inputEmail);
        p.website = ProfileValidator.normalizeWebsite(value(inputWebsite));
        p.instagram = value(inputInstagram);
        p.telegram = value(inputTelegram);
        return p;
    }

    private void updatePreview() {
        Profile p = collectProfile();
        previewName.setText(p.name);
        String role = join(p.job, p.company);
        previewRole.setText(role.isEmpty() ? "کارت تماس دیجیتال" : role);
        StringBuilder contact = new StringBuilder(p.phone);
        if (!TextUtils.isEmpty(p.email)) contact.append("\n").append(p.email);
        if (!TextUtils.isEmpty(p.website)) contact.append("\n").append(p.website);
        previewContact.setText(contact.toString());
        previewAvatar.setText(initialOf(p.name));
    }

    private void completeOnboarding() {
        if (!validateEssential() || !validateContact()) {
            showStep(validateEssential() ? 2 : 1);
            return;
        }
        Profile p = collectProfile();
        ProfileRepository.save(this, p);
        ProfileRepository.setOnboardingCompleted(this, true);
        ProfileRepository.setShareEnabled(this, false);
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showStep(int next) {
        step = Math.max(0, Math.min(3, next));
        stepWelcome.setVisibility(step == 0 ? View.VISIBLE : View.GONE);
        stepEssential.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        stepContact.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        stepPreview.setVisibility(step == 3 ? View.VISIBLE : View.GONE);
        progress.setProgress(step + 1);
        String[] labels = {"خوش آمدید", "مشخصات اصلی", "راه‌های ارتباطی", "پیش‌نمایش کارت"};
        progressText.setText((step + 1) + " از 4  •  " + labels[step]);
    }

    @Override
    public void onBackPressed() {
        if (step > 0) showStep(step - 1);
        else super.onBackPressed();
    }

    private static String value(EditText editText) {
        return ProfileValidator.clean(editText.getText() == null ? "" : editText.getText().toString());
    }

    private static String initialOf(String name) {
        String v = ProfileValidator.clean(name);
        if (v.isEmpty()) return "M";
        return String.valueOf(v.charAt(0)).toUpperCase();
    }

    private static String join(String a, String b) {
        a = ProfileValidator.clean(a);
        b = ProfileValidator.clean(b);
        if (a.isEmpty()) return b;
        if (b.isEmpty()) return a;
        return a + " • " + b;
    }
}
