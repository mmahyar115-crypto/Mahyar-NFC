package com.mahweb.mahyarnfc;

import org.json.JSONException;
import org.json.JSONObject;

public class Profile {
    public String name = "";
    public String job = "";
    public String company = "";
    public String phone = "";
    public String email = "";
    public String website = "";
    public String instagram = "";
    public String telegram = "";

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("name", name);
        o.put("job", job);
        o.put("company", company);
        o.put("phone", phone);
        o.put("email", email);
        o.put("website", website);
        o.put("instagram", instagram);
        o.put("telegram", telegram);
        return o;
    }

    public static Profile fromJson(String json) throws JSONException {
        JSONObject o = new JSONObject(json);
        Profile p = new Profile();
        p.name = o.optString("name", "");
        p.job = o.optString("job", "");
        p.company = o.optString("company", "");
        p.phone = o.optString("phone", "");
        p.email = o.optString("email", "");
        p.website = o.optString("website", "");
        p.instagram = o.optString("instagram", "");
        p.telegram = o.optString("telegram", "");
        return p;
    }

    public String toVCard() {
        StringBuilder b = new StringBuilder();
        b.append("BEGIN:VCARD\r\n");
        b.append("VERSION:3.0\r\n");
        if (hasText(name)) b.append("FN:").append(escape(name)).append("\r\n");
        if (hasText(company)) b.append("ORG:").append(escape(company)).append("\r\n");
        if (hasText(job)) b.append("TITLE:").append(escape(job)).append("\r\n");
        if (hasText(phone)) b.append("TEL;TYPE=CELL:").append(escape(phone)).append("\r\n");
        if (hasText(email)) b.append("EMAIL:").append(escape(email)).append("\r\n");
        if (hasText(website)) b.append("URL:").append(escape(website)).append("\r\n");
        StringBuilder note = new StringBuilder();
        if (hasText(instagram)) note.append("Instagram: ").append(instagram);
        if (hasText(telegram)) {
            if (note.length() > 0) note.append(" | ");
            note.append("Telegram: ").append(telegram);
        }
        if (note.length() > 0) b.append("NOTE:").append(escape(note.toString())).append("\r\n");
        b.append("END:VCARD");
        return b.toString();
    }

    private static boolean hasText(String v) {
        return v != null && !v.trim().isEmpty();
    }

    private static String escape(String v) {
        return v.replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace(";", "\\;")
                .replace(",", "\\,");
    }
}
