package com.mahweb.mahyarnfc;

public final class NfcState {
    public enum Status {
        READY,
        NFC_OFF,
        NFC_UNAVAILABLE,
        HCE_UNAVAILABLE,
        PROFILE_INCOMPLETE
    }

    private NfcState() {}

    public static Status evaluate(boolean profileReady, boolean nfcAvailable, boolean hceAvailable, boolean nfcEnabled) {
        if (!profileReady) return Status.PROFILE_INCOMPLETE;
        if (!nfcAvailable) return Status.NFC_UNAVAILABLE;
        if (!hceAvailable) return Status.HCE_UNAVAILABLE;
        if (!nfcEnabled) return Status.NFC_OFF;
        return Status.READY;
    }

    public static boolean canShare(Status status) {
        return status == Status.READY;
    }

    public static String title(Status status) {
        switch (status) {
            case READY: return "آماده برای ارسال";
            case NFC_OFF: return "NFC خاموش است";
            case NFC_UNAVAILABLE: return "این گوشی NFC ندارد";
            case HCE_UNAVAILABLE: return "ارسال مستقیم پشتیبانی نمی‌شود";
            case PROFILE_INCOMPLETE: return "پروفایل کامل نیست";
            default: return "وضعیت نامشخص";
        }
    }

    public static String description(Status status) {
        switch (status) {
            case READY:
                return "ارسال را فعال کنید و پشت گوشی گیرنده را به پشت این گوشی نزدیک نگه دارید.";
            case NFC_OFF:
                return "برای ارسال مستقیم، NFC را از تنظیمات گوشی روشن کنید. QR همچنان قابل استفاده است.";
            case NFC_UNAVAILABLE:
                return "این دستگاه سخت‌افزار NFC ندارد؛ برای اشتراک‌گذاری از QR استفاده کنید.";
            case HCE_UNAVAILABLE:
                return "گوشی NFC دارد اما Host Card Emulation برای ارسال کارت در دسترس نیست. QR را استفاده کنید.";
            case PROFILE_INCOMPLETE:
                return "برای ارسال NFC باید نام و شماره موبایل معتبر در پروفایل ذخیره شده باشد.";
            default:
                return "";
        }
    }
}
