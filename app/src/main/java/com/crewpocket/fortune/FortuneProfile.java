package com.crewpocket.fortune;

public final class FortuneProfile {
    public final String name;
    public final String birthDate;
    public final String birthTime;
    public final String gender;

    public FortuneProfile(String name, String birthDate, String birthTime, String gender) {
        this.name = clean(name);
        this.birthDate = clean(birthDate);
        this.birthTime = clean(birthTime);
        this.gender = clean(gender).toLowerCase();
    }

    public int genderCode() {
        if ("male".equals(gender) || "男".equals(gender)) return 1;
        if ("female".equals(gender) || "女".equals(gender)) return 0;
        throw new IllegalArgumentException("八字大運需要選擇性別");
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
