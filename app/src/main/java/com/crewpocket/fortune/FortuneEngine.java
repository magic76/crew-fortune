package com.crewpocket.fortune;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FortuneEngine {
    public FortuneFacts calculateFacts(FortuneMode mode, FortuneProfile profile, Date now) {
        if (mode == null) throw new IllegalArgumentException("mode is required");
        if (profile == null || profile.name.isEmpty()) throw new IllegalArgumentException("請輸入名字");

        Map<String, Object> details = new LinkedHashMap<String, Object>();
        String basis;
        int score;

        if (mode == FortuneMode.BA_ZI) {
            details.putAll(new BaZiCalculator().calculate(
                    profile.birthDate, profile.birthTime, profile.genderCode(), now));
            score = intValue(details.get("strengthIndex"), 50);
            basis = "四柱 " + details.get("fourPillars")
                    + " · 日主 " + details.get("dayMaster") + details.get("dayMasterElement")
                    + " · " + details.get("dayMasterStrength");
        } else {
            details.putAll(new TarotNumerologyCalculator().calculate(profile.birthDate, now));
            score = intValue(details.get("lifePathNumber"), 1);
            basis = "外在人格牌 " + details.get("personalityCardNumber") + " " + details.get("personalityCardName")
                    + " · 內在靈魂牌 " + details.get("soulCardNumber") + " " + details.get("soulCardName")
                    + " · 天賦 " + details.get("talentNumbers")
                    + " · 流年 " + details.get("personalYear") + " " + details.get("personalYearCardName");
        }

        return new FortuneFacts(
                mode, score, basis, 0, "", 0, 0,
                0, 0, 0, 0, "stable", details);
    }

    public FortuneResult calculate(FortuneMode mode, FortuneProfile profile, Date now) {
        FortuneFacts facts = calculateFacts(mode, profile, now);
        if (mode == FortuneMode.BA_ZI) {
            return new FortuneResult(
                    mode,
                    facts.score,
                    "日主 " + facts.detailText("dayMaster") + facts.detailText("dayMasterElement")
                            + " · " + facts.detailText("dayMasterStrength"),
                    facts.basis,
                    "四柱、藏干、十神、大運與流年已完成排盤。AI 模式會根據完整結構生成解讀。",
                    "你的設定檔比履歷還完整，現在連十年一換的大運都被列出來了。",
                    "命盤是結構，不是判決書。",
                    "把命理當成觀察與提問，不要拿它取代現實決策。");
        }

        return new FortuneResult(
                mode,
                facts.score,
                facts.detailText("birthCardDisplay"),
                facts.basis,
                "外在人格牌、內在靈魂牌、天賦數、生命道路、態度數、出生牌組、巔峰、挑戰與今年流年已完成計算。",
                "這次只看生日，不再叫你的名字去兼差當數學題。",
                "同一個生日會得到同一組核心數字與出生牌。",
                "把數字和牌義當成反思提示，不要當成宇宙合約。");
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(String.valueOf(value)); }
        catch (Exception ignored) { return fallback; }
    }
}
