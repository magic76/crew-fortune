package com.crewpocket.fortune;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FortuneShareCardDataTest {
    @Test public void baziShareCardUsesSafeSummaryOnly() {
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("dayMaster", "壬");
        details.put("dayMasterElement", "水");
        details.put("dayMasterStrength", "身強");
        details.put("birthDate", "1985-07-06");
        details.put("birthTime", "14:30");

        Map<String, Object> luck = new LinkedHashMap<String, Object>();
        luck.put("ganZhi", "甲子");
        luck.put("startYear", 2024);
        luck.put("endYear", 2033);
        luck.put("plainSummary", "資源與工作責任一起被放大");
        details.put("currentLuckPillar", luck);

        Map<String, Object> annual = new LinkedHashMap<String, Object>();
        annual.put("year", 2026);
        details.put("currentAnnual", annual);

        List<Map<String, Object>> years = new ArrayList<Map<String, Object>>();
        years.add(baziYear(2026, "丙午", "今年"));
        years.add(baziYear(2027, "丁未", "工作責任變明顯"));
        years.add(baziYear(2028, "戊申", "資源整合感增加"));
        years.add(baziYear(2029, "己酉", "輸出與成果議題增加"));
        details.put("annualTimeline", years);

        FortuneFacts facts = facts(FortuneMode.BA_ZI, details);
        FortuneResult result = result(FortuneMode.BA_ZI);
        AiFortuneCopy copy = AiFortuneCopy.parse(
                "{\"title\":\"測試\",\"overview\":\"總覽\","
                        + "\"punchline\":\"小楊 1985-07-06 14:30 很會把錢變成下一個計畫\"}");

        FortuneShareCardData data = FortuneShareCardData.from(
                FortuneMode.BA_ZI, facts, result, copy, "小楊");

        assertEquals("八字", data.modeLabel);
        assertEquals("壬水 · 身強", data.coreValue);
        assertEquals(3, data.years.size());
        assertTrue(data.years.get(0).startsWith("2027"));
        String all = data.allTextForTest();
        assertFalse(all.contains("小楊"));
        assertFalse(all.contains("1985-07-06"));
        assertFalse(all.contains("14:30"));
        assertTrue(all.contains("[日期]"));
        assertTrue(all.contains("[時間]"));
    }

    @Test public void tarotShareCardUsesCardsAndFutureYears() {
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("personalityCardNumber", 9);
        details.put("personalityCardName", "隱者");
        details.put("soulCardNumber", 9);
        details.put("soulCardName", "隱者");
        details.put("lifePathDisplay", "9");
        details.put("personalYearCalendarYear", 2026);
        details.put("personalYear", 5);
        details.put("personalYearCardName", "教皇");
        details.put("birthDate", "1985-07-06");

        List<Map<String, Object>> years = new ArrayList<Map<String, Object>>();
        years.add(tarotYear(2026, 5, "教皇", "今年主題"));
        years.add(tarotYear(2027, 6, "戀人", "合作與承諾"));
        years.add(tarotYear(2028, 7, "戰車", "方向與推進"));
        years.add(tarotYear(2029, 8, "力量", "成果與資源"));
        details.put("personalYearTimeline", years);

        FortuneShareCardData data = FortuneShareCardData.from(
                FortuneMode.TAROT_NUMEROLOGY,
                facts(FortuneMode.TAROT_NUMEROLOGY, details),
                result(FortuneMode.TAROT_NUMEROLOGY),
                null,
                "某某");

        assertEquals("塔羅生命靈數", data.modeLabel);
        assertTrue(data.coreValue.contains("外在人格｜9 隱者"));
        assertTrue(data.cycleValue.contains("2026｜流年 5「教皇」"));
        assertEquals(3, data.years.size());
        assertTrue(data.years.get(0).startsWith("2027"));
        assertFalse(data.allTextForTest().contains("1985-07-06"));
    }

    private static Map<String, Object> baziYear(
            int year, String ganZhi, String summary) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("year", year);
        item.put("ganZhi", ganZhi);
        item.put("plainSummary", summary);
        item.put("themes", Arrays.asList("責任／規範"));
        return item;
    }

    private static Map<String, Object> tarotYear(
            int year, int personalYear, String card, String summary) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("year", year);
        item.put("personalYear", personalYear);
        item.put("cardName", card);
        item.put("plainSummary", summary);
        return item;
    }

    private static FortuneFacts facts(
            FortuneMode mode, Map<String, Object> details) {
        return new FortuneFacts(
                mode, 0, "basis", 0, "", 0, 0,
                0, 0, 0, 0, "", details);
    }

    private static FortuneResult result(FortuneMode mode) {
        return new FortuneResult(
                mode, 0, "標題", "basis",
                "analysis", "translation",
                "本地一句話", "advice");
    }
}
