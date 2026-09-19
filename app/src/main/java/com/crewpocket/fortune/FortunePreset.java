package com.crewpocket.fortune;

import org.json.JSONObject;

public final class FortunePreset {
    public final String name;
    public final String birthDate;
    public final String birthTime;
    public final String gender;
    public final FortuneMode mode;

    public FortunePreset(String name, String birthDate, String birthTime, String gender, FortuneMode mode) {
        this.name = clean(name);
        this.birthDate = clean(birthDate);
        this.birthTime = clean(birthTime);
        this.gender = clean(gender);
        this.mode = mode == null ? FortuneMode.BA_ZI : mode;
    }

    public String label() {
        StringBuilder out = new StringBuilder(name.isEmpty() ? "未命名" : name);
        if (!birthDate.isEmpty()) out.append(" · ").append(birthDate);
        if (mode == FortuneMode.BA_ZI && !birthTime.isEmpty()) out.append(" ").append(birthTime);
        return out.toString();
    }

    public String key() {
        return name + "|" + birthDate + "|" + birthTime + "|" + gender;
    }

    public JSONObject toJson() {
        JSONObject object = new JSONObject();
        try {
            object.put("name", name);
            object.put("birthDate", birthDate);
            object.put("birthTime", birthTime);
            object.put("gender", gender);
            object.put("mode", mode.name());
        } catch (Exception ignored) {}
        return object;
    }

    public static FortunePreset fromJson(JSONObject object) {
        if (object == null) return null;
        try {
            FortuneMode mode = FortuneMode.valueOf(object.optString("mode", "BA_ZI"));
            return new FortunePreset(
                    object.optString("name", ""),
                    object.optString("birthDate", ""),
                    object.optString("birthTime", ""),
                    object.optString("gender", ""),
                    mode);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
