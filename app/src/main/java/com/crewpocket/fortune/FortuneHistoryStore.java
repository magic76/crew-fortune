package com.crewpocket.fortune;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

final class FortuneHistoryStore {
    private static final String PREFS = "crew_fortune_history";
    private static final String KEY_HISTORY = "history_v1";
    private static final int MAX_ITEMS = 30;

    private FortuneHistoryStore() {}

    static FortuneHistoryEntry add(
            Context context,
            FortunePreset preset,
            long referenceTimeMillis,
            String transitDate,
            FortuneResult result,
            FortuneFacts facts,
            AiFortuneCopy aiCopy) {
        FortuneHistoryEntry entry = FortuneHistoryEntry.create(
                preset,
                referenceTimeMillis,
                transitDate,
                result,
                facts,
                aiCopy);
        List<FortuneHistoryEntry> items = list(context);
        items.add(0, entry);
        while (items.size() > MAX_ITEMS) {
            items.remove(items.size() - 1);
        }
        write(context, items);
        return entry;
    }

    static List<FortuneHistoryEntry> list(Context context) {
        List<FortuneHistoryEntry> out =
                new ArrayList<FortuneHistoryEntry>();
        if (context == null) return out;
        String raw = prefs(context).getString(KEY_HISTORY, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                FortuneHistoryEntry entry =
                        FortuneHistoryEntry.fromJson(
                                array.optJSONObject(i));
                if (entry != null) out.add(entry);
            }
        } catch (Exception ignored) {}
        return out;
    }

    static void updateAiCopy(
            Context context,
            String id,
            AiFortuneCopy copy) {
        if (context == null || id == null || id.trim().isEmpty()) return;
        List<FortuneHistoryEntry> items = list(context);
        boolean changed = false;
        for (int i = 0; i < items.size(); i++) {
            FortuneHistoryEntry item = items.get(i);
            if (id.equals(item.id)) {
                items.set(i, item.withAiCopy(copy));
                changed = true;
                break;
            }
        }
        if (changed) write(context, items);
    }

    static void clear(Context context) {
        if (context == null) return;
        prefs(context).edit().remove(KEY_HISTORY).apply();
    }

    private static void write(
            Context context,
            List<FortuneHistoryEntry> items) {
        if (context == null) return;
        JSONArray array = new JSONArray();
        if (items != null) {
            for (FortuneHistoryEntry item : items) {
                if (item != null) array.put(item.toJson());
            }
        }
        prefs(context).edit()
                .putString(KEY_HISTORY, array.toString())
                .apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE);
    }
}
