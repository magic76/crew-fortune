package com.crewpocket.fortune;

public final class FortuneProfile {
    public final String name;
    public final String birthDate;
    public final String birthTime;
    public final String secondaryName;

    public FortuneProfile(String name, String birthDate, String secondaryName) {
        this(name, birthDate, "", secondaryName);
    }

    public FortuneProfile(String name, String birthDate, String birthTime, String secondaryName) {
        this.name = clean(name);
        this.birthDate = clean(birthDate);
        this.birthTime = clean(birthTime);
        this.secondaryName = clean(secondaryName);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
