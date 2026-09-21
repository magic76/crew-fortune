package com.crewpocket.fortune;

import org.json.JSONObject;

public final class FortunePreset {
    public final String name;
    public final String birthDate;
    public final String birthTime;
    public final String gender;
    public final FortuneMode mode;
    public final String birthPlaceName;
    public final String latitude;
    public final String longitude;
    public final String timeZoneId;

    public FortunePreset(
            String name,
            String birthDate,
            String birthTime,
            String gender,
            FortuneMode mode) {
        this(name, birthDate, birthTime, gender, mode, "", "", "", "");
    }

    public FortunePreset(
            String name,
            String birthDate,
            String birthTime,
            String gender,
            FortuneMode mode,
            String birthPlaceName,
            String latitude,
            String longitude,
            String timeZoneId) {
        // Names are intentionally ignored; fortune calculations do not use identity data.
        this.name = "";
        this.birthDate = clean(birthDate);
        this.birthTime = clean(birthTime);
        this.gender = clean(gender);
        this.mode = mode == null ? FortuneMode.BA_ZI : mode;
        this.birthPlaceName = clean(birthPlaceName);
        this.latitude = clean(latitude);
        this.longitude = clean(longitude);
        this.timeZoneId = clean(timeZoneId);
    }

    public String label() {
        StringBuilder out = new StringBuilder(mode.title());
        if (!birthDate.isEmpty()) out.append(" · ").append(birthDate);
        if ((mode == FortuneMode.BA_ZI || mode == FortuneMode.VEDIC_ASTROLOGY)
                && !birthTime.isEmpty()) {
            out.append(" ").append(birthTime);
        }
        if (mode == FortuneMode.VEDIC_ASTROLOGY && !birthPlaceName.isEmpty()) {
            out.append(" · ").append(birthPlaceName);
        }
        return out.toString();
    }

    public String key() {
        return birthDate + "|" + birthTime + "|" + gender
                + "|" + mode.name() + "|" + latitude + "|" + longitude + "|" + timeZoneId;
    }

    public BirthPlace birthPlaceOrNull() {
        if (mode != FortuneMode.VEDIC_ASTROLOGY) return null;
        if (latitude.isEmpty() || longitude.isEmpty() || timeZoneId.isEmpty()) return null;
        try {
            return new BirthPlace(
                    birthPlaceName,
                    Double.parseDouble(latitude),
                    Double.parseDouble(longitude),
                    timeZoneId);
        } catch (Exception ignored) {
            return null;
        }
    }

    public JSONObject toJson() {
        JSONObject object = new JSONObject();
        try {
            object.put("birthDate", birthDate);
            object.put("birthTime", birthTime);
            object.put("gender", gender);
            object.put("mode", mode.name());
            object.put("birthPlaceName", birthPlaceName);
            object.put("latitude", latitude);
            object.put("longitude", longitude);
            object.put("timeZoneId", timeZoneId);
        } catch (Exception ignored) {}
        return object;
    }

    public static FortunePreset fromJson(JSONObject object) {
        if (object == null) return null;
        try {
            FortuneMode mode = FortuneMode.valueOf(object.optString("mode", "BA_ZI"));
            return new FortunePreset(
                    "",
                    object.optString("birthDate", ""),
                    object.optString("birthTime", ""),
                    object.optString("gender", ""),
                    mode,
                    object.optString("birthPlaceName", ""),
                    object.optString("latitude", ""),
                    object.optString("longitude", ""),
                    object.optString("timeZoneId", ""));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
