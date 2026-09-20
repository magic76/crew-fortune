package com.crewpocket.fortune;

import java.time.DateTimeException;
import java.time.ZoneId;

public final class BirthPlace {
    public final String displayName;
    public final double latitude;
    public final double longitude;
    public final String timeZoneId;

    public BirthPlace(String displayName, double latitude, double longitude, String timeZoneId) {
        this.displayName = clean(displayName);
        this.latitude = latitude;
        this.longitude = longitude;
        this.timeZoneId = clean(timeZoneId);
        validate();
    }

    public ZoneId zoneId() {
        try {
            return ZoneId.of(timeZoneId);
        } catch (DateTimeException error) {
            throw new IllegalArgumentException("出生地時區無效：" + timeZoneId, error);
        }
    }

    public String summary() {
        String name = displayName.isEmpty() ? "手動座標" : displayName;
        return name + " · " + format(latitude) + ", " + format(longitude)
                + " · " + timeZoneId;
    }

    private void validate() {
        if (!Double.isFinite(latitude) || latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("出生地 latitude 必須介於 -90 到 90");
        }
        if (!Double.isFinite(longitude) || longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("出生地 longitude 必須介於 -180 到 180");
        }
        if (Math.abs(latitude) >= 89.9999) {
            throw new IllegalArgumentException("出生地太接近南北極，Ascendant 無法可靠計算");
        }
        if (timeZoneId.isEmpty()) {
            throw new IllegalArgumentException("印度星盤需要出生地時區，例如 Asia/Taipei 或 +08:00");
        }
        zoneId();
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.4f", value);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
