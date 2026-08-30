package com.mahweb.mahyarnfc;

import android.content.Context;
import android.content.SharedPreferences;

public final class ProfileRepository {
    private static final String PREFS = "mahyar_nfc_prefs";
    private static final String K_NAME = "name";
    private static final String K_JOB = "job";
    private static final String K_COMPANY = "company";
    private static final String K_PHONE = "phone";
    private static final String K_EMAIL = "email";
    private static final String K_WEBSITE = "website";
    private static final String K_INSTAGRAM = "instagram";
    private static final String K_TELEGRAM = "telegram";
    private static final String K_SHARE = "share_enabled";

    private ProfileRepository() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static Profile load(Context c) {
        SharedPreferences p = prefs(c);
        Profile r = new Profile();
        r.name = p.getString(K_NAME, "");
        r.job = p.getString(K_JOB, "");
        r.company = p.getString(K_COMPANY, "");
        r.phone = p.getString(K_PHONE, "");
        r.email = p.getString(K_EMAIL, "");
        r.website = p.getString(K_WEBSITE, "");
        r.instagram = p.getString(K_INSTAGRAM, "");
        r.telegram = p.getString(K_TELEGRAM, "");
        return r;
    }

    public static void save(Context c, Profile r) {
        prefs(c).edit()
                .putString(K_NAME, r.name)
                .putString(K_JOB, r.job)
                .putString(K_COMPANY, r.company)
                .putString(K_PHONE, r.phone)
                .putString(K_EMAIL, r.email)
                .putString(K_WEBSITE, r.website)
                .putString(K_INSTAGRAM, r.instagram)
                .putString(K_TELEGRAM, r.telegram)
                .apply();
    }

    public static boolean isShareEnabled(Context c) {
        return prefs(c).getBoolean(K_SHARE, false);
    }

    public static void setShareEnabled(Context c, boolean enabled) {
        prefs(c).edit().putBoolean(K_SHARE, enabled).apply();
    }
}
