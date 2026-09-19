package com.crewpocket.fortune;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class FortunePresetStore {
    private static final String PREFS = "crew_fortune_profiles";
    private static final String KEY_LAST = "last_profile_v1";
    private static final String KEY_PRESETS = "presets_v1";

    private FortunePresetStore() {}

    public static void saveLast(Context context, FortunePreset preset) {
        if (context == null || preset == null) return;
        prefs(context).edit().putString(KEY_LAST, preset.toJson().toString()).apply();
    }

    public static FortunePreset loadLast(Context context) {
        if (context == null) return null;
        String raw = prefs(context).getString(KEY_LAST, "");
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            return FortunePreset.fromJson(new JSONObject(raw));
        } catch (Exception ignored) {
            return null;
        }
    }

    public static List<FortunePreset> loadPresets(Context context) {
        List<FortunePreset> result = new ArrayList<FortunePreset>();
        if (context == null) return result;
        String raw = prefs(context).getString(KEY_PRESETS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                FortunePreset preset = FortunePreset.fromJson(array.optJSONObject(i));
                if (preset != null) result.add(preset);
            }
        } catch (Exception ignored) {}
        return result;
    }

    public static void savePreset(Context context, FortunePreset preset) {
        if (context == null || preset == null) return;
        List<FortunePreset> items = loadPresets(context);
        boolean replaced = false;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).key().equals(preset.key())) {
                items.set(i, preset);
                replaced = true;
                break;
            }
        }
        if (!replaced) items.add(0, preset);
        while (items.size() > 12) items.remove(items.size() - 1);

        JSONArray array = new JSONArray();
        for (FortunePreset item : items) array.put(item.toJson());
        prefs(context).edit().putString(KEY_PRESETS, array.toString()).apply();
        saveLast(context, preset);
    }

    public static void clearPresets(Context context) {
        if (context == null) return;
        prefs(context).edit().remove(KEY_PRESETS).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
