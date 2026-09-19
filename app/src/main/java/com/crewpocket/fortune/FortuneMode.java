package com.crewpocket.fortune;

public enum FortuneMode {
    BA_ZI("八字", "四柱、十神、大運、流年"),
    TAROT_NUMEROLOGY("塔羅生命靈數", "生命靈數、出生牌、年度循環");

    private final String title;
    private final String subtitle;

    FortuneMode(String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
    }

    public String title() { return title; }
    public String subtitle() { return subtitle; }
}
