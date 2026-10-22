package com.rehome.launcher;

import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * The look of the launcher as one JSON file, so it can be exported, edited (by hand, by a designer or by an AI)
 * and imported again. See THEMING.md in the repository for every key.
 */
final class ThemeFile {
    private ThemeFile() {
    }

    static final String DEFAULT = "{"
            + "\"name\":\"Vezel Radio\",\"version\":2,"
            + "\"dark\":{\"bgTop\":\"#1B2733\",\"bgBottom\":\"#0A0F14\",\"accent\":\"#FF8A1E\","
            + "\"text\":\"#F2F5F8\",\"sub\":\"#9BA1A8\",\"glyph\":\"#F2F5F8\","
            + "\"tileFill\":\"#59000000\",\"tileStroke\":\"#66FFFFFF\",\"vignetteColor\":\"#000000\","
            + "\"map\":{\"ground\":\"#0A1220\",\"water\":\"#0E2238\",\"road\":\"#4F6A86\",\"major\":\"#9FB8D4\","
            + "\"building\":\"#26384D\",\"text\":\"#DCE8F5\",\"halo\":\"#CC0A1220\"}},"
            + "\"light\":{\"bgTop\":\"#EEF2F6\",\"bgBottom\":\"#CBD3DA\",\"accent\":\"#E8590C\","
            + "\"text\":\"#14171A\",\"sub\":\"#4A5057\",\"glyph\":\"#14171A\","
            + "\"tileFill\":\"#4DFFFFFF\",\"tileStroke\":\"#66000000\",\"vignetteColor\":\"#3A4856\","
            + "\"map\":{\"ground\":\"#E9EEF3\",\"water\":\"#C6D8E8\",\"road\":\"#7B8EA3\",\"major\":\"#33495F\","
            + "\"building\":\"#B7C3CF\",\"text\":\"#1B2733\",\"halo\":\"#CCE9EEF3\"}},"
            + "\"menu\":{\"bgTop\":\"#212B35\",\"bgBottom\":\"#0A0E12\",\"stroke\":\"#5A6068\","
            + "\"accent\":\"#FF8A1E\",\"text\":\"#F2F5F8\",\"sub\":\"#9BA1A8\"},"
            + "\"vignette\":50,\"tileRadius\":30,\"tileGap\":0,\"iconGlow\":45,"
            + "\"layout\":{\"pipW\":0.398,\"pipH\":0.515,\"barH\":0.15,\"dockH\":0.065},"
            + "\"frame\":{\"margin\":8,\"corner\":60},"
            + "\"iconColors\":{},"
            + "\"icons\":{}}";

    static JSONObject defaults() {
        try {
            return new JSONObject(DEFAULT);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    /** Deep merge: values of over replace values of base. */
    static void overlay(JSONObject base, JSONObject over) {
        Iterator<String> it = over.keys();
        while (it.hasNext()) {
            String k = it.next();
            try {
                Object v = over.get(k);
                Object cur = base.opt(k);
                if (v instanceof JSONObject && cur instanceof JSONObject) overlay((JSONObject) cur, (JSONObject) v);
                else base.put(k, v);
            } catch (JSONException ignored) {
            }
        }
    }

    /** The default theme with the imported theme (if any) on top. */
    static JSONObject merged(SharedPreferences cfg) {
        JSONObject d = defaults();
        String s = cfg.getString("themeJson", "");
        if (s.length() > 0) {
            try {
                overlay(d, new JSONObject(s));
            } catch (JSONException ignored) {
            }
        }
        return d;
    }

    static int col(JSONObject o, String key, int def) {
        if (o == null) return def;
        String s = o.optString(key, "");
        if (s.length() == 0) return def;
        try {
            if (s.startsWith("#")) s = s.substring(1);
            long v = Long.parseLong(s, 16);
            if (s.length() <= 6) v |= 0xFF000000L;
            return (int) v;
        } catch (Exception e) {
            return def;
        }
    }
}
