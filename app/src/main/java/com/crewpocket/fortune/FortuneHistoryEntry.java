package com.crewpocket.fortune;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class FortuneHistoryEntry {
    final String id;
    final long createdAtMillis;
    final FortunePreset preset;
    final long referenceTimeMillis;
    final String transitDate;
    final FortuneResult result;
    final FortuneFacts facts;
    final AiFortuneCopy aiCopy;

    FortuneHistoryEntry(
            String id,
            long createdAtMillis,
            FortunePreset preset,
            long referenceTimeMillis,
            String transitDate,
            FortuneResult result,
            FortuneFacts facts,
            AiFortuneCopy aiCopy) {
        this.id = clean(id);
        this.createdAtMillis = createdAtMillis;
        this.preset = preset;
        this.referenceTimeMillis = referenceTimeMillis;
        this.transitDate = clean(transitDate);
        this.result = result;
        this.facts = facts;
        this.aiCopy = aiCopy;
    }

    static FortuneHistoryEntry create(
            FortunePreset preset,
            long referenceTimeMillis,
            String transitDate,
            FortuneResult result,
            FortuneFacts facts,
            AiFortuneCopy aiCopy) {
        long now = System.currentTimeMillis();
        String reading = FortuneReadingId.from(preset);
        String id = reading.substring(0, Math.min(16, reading.length()))
                + "-" + now;
        return new FortuneHistoryEntry(
                id,
                now,
                preset,
                referenceTimeMillis,
                transitDate,
                result,
                facts,
                aiCopy);
    }

    FortuneHistoryEntry withAiCopy(AiFortuneCopy copy) {
        return new FortuneHistoryEntry(
                id,
                createdAtMillis,
                preset,
                referenceTimeMillis,
                transitDate,
                result,
                facts,
                copy);
    }

    String titleLine() {
        return (preset == null || preset.mode == null)
                ? "命理結果"
                : preset.mode.title();
    }

    String subtitleLine() {
        if (preset == null) return "";
        StringBuilder out = new StringBuilder();
        if (!preset.birthDate.isEmpty()) {
            out.append(preset.birthDate);
        }
        if (!preset.birthTime.isEmpty()) {
            if (out.length() > 0) out.append(" ");
            out.append(preset.birthTime);
        }
        if (preset.mode == FortuneMode.VEDIC_ASTROLOGY
                && !preset.birthPlaceName.isEmpty()) {
            if (out.length() > 0) out.append(" · ");
            out.append(preset.birthPlaceName);
        }
        return out.toString();
    }

    String savedAtText() {
        return new SimpleDateFormat(
                "MM/dd HH:mm",
                Locale.TAIWAN).format(new Date(createdAtMillis));
    }

    JSONObject toJson() {
        JSONObject object = new JSONObject();
        try {
            object.put("id", id);
            object.put("createdAtMillis", createdAtMillis);
            object.put("preset", preset == null
                    ? JSONObject.NULL
                    : preset.toJson());
            object.put("referenceTimeMillis", referenceTimeMillis);
            object.put("transitDate", transitDate);
            object.put("result", resultToJson(result));
            object.put("facts", factsToJson(facts));
            object.put("aiCopy", aiCopy == null
                    ? ""
                    : aiCopy.toJson());
        } catch (Exception error) {
            throw new IllegalStateException(
                    "Unable to serialize fortune history",
                    error);
        }
        return object;
    }

    static FortuneHistoryEntry fromJson(JSONObject object) {
        if (object == null) return null;
        try {
            FortunePreset preset =
                    FortunePreset.fromJson(object.optJSONObject("preset"));
            FortuneResult result =
                    resultFromJson(object.optJSONObject("result"));
            FortuneFacts facts =
                    factsFromJson(object.optJSONObject("facts"));
            if (preset == null || result == null || facts == null) return null;

            String aiRaw = object.optString("aiCopy", "");
            AiFortuneCopy copy = null;
            if (!aiRaw.trim().isEmpty()) {
                try {
                    copy = AiFortuneCopy.parse(aiRaw);
                } catch (RuntimeException ignored) {}
            }

            return new FortuneHistoryEntry(
                    object.optString("id", ""),
                    object.optLong("createdAtMillis", 0L),
                    preset,
                    object.optLong("referenceTimeMillis", -1L),
                    object.optString("transitDate", ""),
                    result,
                    facts,
                    copy);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JSONObject resultToJson(FortuneResult value) {
        if (value == null) return null;
        JSONObject object = new JSONObject();
        try {
            object.put("mode", value.mode.name());
            object.put("score", value.score);
            object.put("title", value.title);
            object.put("basis", value.basis);
            object.put("analysis", value.analysis);
            object.put("translation", value.translation);
            object.put("punchline", value.punchline);
            object.put("advice", value.advice);
        } catch (Exception ignored) {}
        return object;
    }

    private static FortuneResult resultFromJson(JSONObject object) {
        if (object == null) return null;
        try {
            return new FortuneResult(
                    FortuneMode.valueOf(object.optString("mode", "BA_ZI")),
                    object.optInt("score", 0),
                    object.optString("title", ""),
                    object.optString("basis", ""),
                    object.optString("analysis", ""),
                    object.optString("translation", ""),
                    object.optString("punchline", ""),
                    object.optString("advice", ""));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JSONObject factsToJson(FortuneFacts value) {
        if (value == null) return null;
        JSONObject object = new JSONObject();
        try {
            object.put("mode", value.mode.name());
            object.put("score", value.score);
            object.put("basis", value.basis);
            object.put("lifeNumber", value.lifeNumber);
            object.put("zodiac", value.zodiac);
            object.put("nameNumber", value.nameNumber);
            object.put("secondaryNameNumber", value.secondaryNameNumber);
            object.put("momentum", value.momentum);
            object.put("stability", value.stability);
            object.put("social", value.social);
            object.put("impulse", value.impulse);
            object.put("dayKey", value.dayKey);
            object.put("details", toJsonValue(value.details));
        } catch (Exception ignored) {}
        return object;
    }

    private static FortuneFacts factsFromJson(JSONObject object) {
        if (object == null) return null;
        try {
            Object detailsRaw = object.opt("details");
            Map<String, Object> details =
                    detailsRaw instanceof JSONObject
                            ? objectToMap((JSONObject) detailsRaw)
                            : new LinkedHashMap<String, Object>();
            return new FortuneFacts(
                    FortuneMode.valueOf(object.optString("mode", "BA_ZI")),
                    object.optInt("score", 0),
                    object.optString("basis", ""),
                    object.optInt("lifeNumber", 0),
                    object.optString("zodiac", ""),
                    object.optInt("nameNumber", 0),
                    object.optInt("secondaryNameNumber", 0),
                    object.optInt("momentum", 0),
                    object.optInt("stability", 0),
                    object.optInt("social", 0),
                    object.optInt("impulse", 0),
                    object.optString("dayKey", ""),
                    details);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Object toJsonValue(Object value) {
        if (value == null) return JSONObject.NULL;
        if (value instanceof Map) {
            JSONObject object = new JSONObject();
            for (Map.Entry<?, ?> entry
                    : ((Map<?, ?>) value).entrySet()) {
                try {
                    object.put(
                            String.valueOf(entry.getKey()),
                            toJsonValue(entry.getValue()));
                } catch (Exception ignored) {}
            }
            return object;
        }
        if (value instanceof Iterable) {
            JSONArray array = new JSONArray();
            for (Object item : (Iterable<?>) value) {
                array.put(toJsonValue(item));
            }
            return array;
        }
        if (value instanceof Number
                || value instanceof Boolean
                || value instanceof String) {
            return value;
        }
        return String.valueOf(value);
    }

    private static Map<String, Object> objectToMap(JSONObject object) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        if (object == null) return out;
        java.util.Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            out.put(key, fromJsonValue(object.opt(key)));
        }
        return out;
    }

    private static Object fromJsonValue(Object value) {
        if (value == null || value == JSONObject.NULL) return null;
        if (value instanceof JSONObject) {
            return objectToMap((JSONObject) value);
        }
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            List<Object> out = new ArrayList<Object>();
            for (int i = 0; i < array.length(); i++) {
                out.add(fromJsonValue(array.opt(i)));
            }
            return out;
        }
        return value;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
