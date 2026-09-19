package com.crewpocket.fortune;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppConfig {
    private static final String PREFS = "crew_fortune_config";
    private static final String KEY_GEMINI_API_KEY = "gemini_api_key";

    private AppConfig() {}

    public static String getGeminiApiKey(Context context) {
        if (context == null) return "";
        return prefs(context).getString(KEY_GEMINI_API_KEY, "").trim();
    }

    public static void setGeminiApiKey(Context context, String value) {
        if (context == null) return;
        prefs(context).edit().putString(KEY_GEMINI_API_KEY, clean(value)).apply();
    }

    public static boolean hasGeminiApiKey(Context context) {
        return !getGeminiApiKey(context).isEmpty();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
