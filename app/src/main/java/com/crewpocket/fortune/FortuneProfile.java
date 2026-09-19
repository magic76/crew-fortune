package com.crewpocket.fortune;

public final class FortuneProfile {
    public final String name;
    public final String birthDate;
    public final String birthTime;
    public final String gender;
    public final String birthNameLatin;

    public FortuneProfile(String name, String birthDate, String birthTime, String gender) {
        this(name, birthDate, birthTime, gender, "");
    }

    public FortuneProfile(String name,
                          String birthDate,
                          String birthTime,
                          String gender,
                          String birthNameLatin) {
        this.name = clean(name);
        this.birthDate = clean(birthDate);
        this.birthTime = clean(birthTime);
        this.gender = clean(gender).toLowerCase();
        this.birthNameLatin = clean(birthNameLatin);
    }

    public int genderCode() {
        if ("male".equals(gender) || "男".equals(gender)) return 1;
        if ("female".equals(gender) || "女".equals(gender)) return 0;
        throw new IllegalArgumentException("八字大運需要選擇性別");
    }

    public String numerologyBirthName() {
        if (!birthNameLatin.isEmpty()) return birthNameLatin;
        if (name.matches("[A-Za-z][A-Za-z .'-]*")) return name;
        return "";
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
