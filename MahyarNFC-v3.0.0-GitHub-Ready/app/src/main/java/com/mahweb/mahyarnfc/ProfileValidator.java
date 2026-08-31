package com.mahweb.mahyarnfc;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

public final class ProfileValidator {
    private static final Pattern EMAIL = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private ProfileValidator() {}

    public static String validateName(String value) {
        String v = clean(value);
        if (v.isEmpty()) return "نام و نام خانوادگی را وارد کنید";
        if (v.length() < 2) return "نام وارد شده خیلی کوتاه است";
        return null;
    }

    public static String validatePhone(String value) {
        String v = clean(value);
        if (v.isEmpty()) return "شماره موبایل را وارد کنید";
        String digits = v.replaceAll("[^0-9]", "");
        if (digits.length() < 7 || digits.length() > 15) return "شماره موبایل معتبر نیست";
        return null;
    }

    public static String validateEmail(String value) {
        String v = clean(value);
        if (v.isEmpty()) return null;
        return EMAIL.matcher(v).matches() ? null : "ایمیل معتبر نیست";
    }

    public static String validateWebsite(String value) {
        String v = normalizeWebsite(value);
        if (v.isEmpty()) return null;
        try {
            URI uri = URI.create(v);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!("http".equals(scheme) || "https".equals(scheme))) return "آدرس وب‌سایت معتبر نیست";
            String host = uri.getHost();
            if (host == null || host.trim().isEmpty() || !host.contains(".")) return "آدرس وب‌سایت معتبر نیست";
            return null;
        } catch (Exception e) {
            return "آدرس وب‌سایت معتبر نیست";
        }
    }

    public static String normalizeWebsite(String value) {
        String v = clean(value);
        if (v.isEmpty()) return "";
        String lower = v.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            v = "https://" + v;
        }
        return v;
    }

    public static boolean isProfileReady(Profile profile) {
        return profile != null
                && validateName(profile.name) == null
                && validatePhone(profile.phone) == null;
    }

    public static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
