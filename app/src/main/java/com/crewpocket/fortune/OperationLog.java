package com.crewpocket.fortune;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class OperationLog {
    private static final String PREFS = "crew_fortune_operation_log";
    private static final String KEY_ITEMS = "items_v1";
    private static final int MAX_ITEMS = 200;

    private OperationLog() {}

    public static synchronized void add(Context context, String action, String detail) {
        if (context == null || action == null || action.trim().isEmpty()) return;
        try {
            JSONArray current = loadArray(context);
            JSONArray next = new JSONArray();

            JSONObject item = new JSONObject();
            item.put("time", System.currentTimeMillis());
            item.put("action", action.trim());
            item.put("detail", sanitizeDetail(action, detail));
            next.put(item);

            for (int i = 0; i < current.length() && next.length() < MAX_ITEMS; i++) {
                JSONObject old = current.optJSONObject(i);
                if (old != null) {
                    old.put("detail", sanitizeDetail(
                            old.optString("action", ""),
                            old.optString("detail", "")));
                    next.put(old);
                }
            }

            prefs(context).edit().putString(KEY_ITEMS, next.toString()).apply();
        } catch (Exception ignored) {}
    }

    static String sanitizeDetail(String action, String detail) {
        String safeAction = action == null ? "" : action.trim();
        if (safeAction.startsWith("BIRTH_PLACE")
                || safeAction.startsWith("GEOCODE")
                || safeAction.startsWith("VEDIC_CITY_")
                || safeAction.startsWith("VEDIC_TIMEZONE_")) {
            return "[location]";
        }
        if (safeAction.startsWith("PRESET_")) {
            return "[preset]";
        }

        String clean = detail == null ? "" : detail.trim();
        return clean
                .replaceAll("\\d{4}-\\d{2}-\\d{2}", "[date]")
                .replaceAll("(?<!\\d)\\d{1,2}:\\d{2}(?!\\d)", "[time]")
                .replaceAll("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}", "[email]")
                .replaceAll("(?<![\\d.])-?\\d{1,3}\\.\\d{3,}(?![\\d.])", "[decimal]")
                .replaceAll("(?<!\\d)\\d{8,15}(?!\\d)", "[number]")
                .replaceAll("[A-Za-z0-9_\\-]{24,}", "[token]");
    }

    public static synchronized List<Entry> list(Context context) {
        List<Entry> result = new ArrayList<Entry>();
        if (context == null) return result;
        JSONArray array = loadArray(context);
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) continue;
            result.add(new Entry(
                    item.optLong("time", 0L),
                    item.optString("action", ""),
                    item.optString("detail", "")));
        }
        return result;
    }

    public static synchronized void clear(Context context) {
        if (context == null) return;
        prefs(context).edit().remove(KEY_ITEMS).apply();
    }

    private static JSONArray loadArray(Context context) {
        try {
            return new JSONArray(prefs(context).getString(KEY_ITEMS, "[]"));
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static final class Entry {
        public final long time;
        public final String action;
        public final String detail;

        Entry(long time, String action, String detail) {
            this.time = time;
            this.action = action;
            this.detail = detail;
        }

        public String timeText() {
            return new SimpleDateFormat(
                    "MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(time));
        }

        public String listLabel() {
            return timeText() + "  ·  " + action.replace('_', ' ');
        }

        public String display() {
            return listLabel()
                    + (detail == null || detail.isEmpty() ? "" : "\n" + detail);
        }
    }
}
