package com.crewpocket.fortune;

public enum FortuneMode {
    TODAY("今日運勢", "今天宇宙到底想幹嘛"),
    BA_ZI("八字命盤", "年月日時排四柱，認真算再幽默講"),
    TAROT_NUMEROLOGY("塔羅生命靈數", "生日數字對應你的大阿爾克那"),
    PERSONALITY("隱藏人格", "你沒說出口的那一面"),
    WEALTH("發財命", "錢到底跟你熟不熟"),
    LOVE_BUG("戀愛 Bug", "感情系統錯在哪"),
    COMPATIBILITY("朋友／情侶合盤", "你們到底為什麼湊在一起");

    private final String title;
    private final String subtitle;

    FortuneMode(String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
    }

    public String title() { return title; }
    public String subtitle() { return subtitle; }
}
