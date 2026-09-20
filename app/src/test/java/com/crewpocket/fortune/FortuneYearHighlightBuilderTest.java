package com.crewpocket.fortune;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FortuneYearHighlightBuilderTest {
    @Test public void baziHighlightsPreferYearsWithMoreGroundedSignals() {
        Map<String, Object> details = new LinkedHashMap<String, Object>();

        Map<String, Object> current = new LinkedHashMap<String, Object>();
        current.put("year", 2026);
        details.put("currentAnnual", current);

        List<Map<String, Object>> timeline = new ArrayList<Map<String, Object>>();
        timeline.add(baziYear(2026, "丙午", "責任／規範", "與本命未偵測到主要六合、六沖、六害或相刑"));
        timeline.add(baziYear(2027, "丁未", "財星／資源", "日支申 ↔ 未 六害"));
        timeline.add(baziYear(2028, "戊申", "常態推進", "與本命未偵測到主要六合、六沖、六害或相刑"));
        timeline.add(baziYear(2029, "己酉", "輸出／表達", "月支卯 ↔ 酉 六沖"));
        details.put("annualTimeline", timeline);

        Map<String, Object> wealth = new LinkedHashMap<String, Object>();
        wealth.put("annualSignalYears", Arrays.asList(2027));
        details.put("wealthProfile", wealth);

        Map<String, Object> career = new LinkedHashMap<String, Object>();
        career.put("annualSignalYears", Arrays.asList(2029));
        details.put("careerProfile", career);

        Map<String, Object> relationship = new LinkedHashMap<String, Object>();
        List<Map<String, Object>> relationshipYears = new ArrayList<Map<String, Object>>();
        Map<String, Object> relationshipYear = new LinkedHashMap<String, Object>();
        relationshipYear.put("year", 2027);
        relationshipYears.add(relationshipYear);
        relationship.put("annualSignalYears", relationshipYears);
        details.put("relationshipProfile", relationship);

        List<FortuneYearHighlightBuilder.Highlight> highlights =
                FortuneYearHighlightBuilder.build(
                        FortuneMode.BA_ZI,
                        facts(FortuneMode.BA_ZI, details));

        assertTrue(highlights.size() >= 2);
        assertEquals(2027, highlights.get(0).year);
        assertTrue(highlights.get(0).theme.contains("財務"));
        assertTrue(highlights.get(0).theme.contains("感情"));
        assertTrue(highlights.get(0).evidence.contains("財星"));
        assertTrue(highlights.get(0).question.contains("2027"));
    }

    @Test public void tarotHighlightsExposeCycleThemeAndEvidence() {
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("personalYearCalendarYear", 2026);

        List<Map<String, Object>> timeline = new ArrayList<Map<String, Object>>();
        timeline.add(tarotYear(2026, 4, "皇帝", "打基礎", "秩序、穩定"));
        timeline.add(tarotYear(2027, 5, "教皇", "變化與新選項", "變動、學習"));
        timeline.add(tarotYear(2028, 6, "戀人", "責任與關係", "合作、承諾"));
        timeline.add(tarotYear(2029, 8, "力量", "成果與資源", "成果、資源"));
        details.put("personalYearTimeline", timeline);

        List<FortuneYearHighlightBuilder.Highlight> highlights =
                FortuneYearHighlightBuilder.build(
                        FortuneMode.TAROT_NUMEROLOGY,
                        facts(FortuneMode.TAROT_NUMEROLOGY, details));

        assertTrue(highlights.size() >= 3);
        assertEquals(2027, highlights.get(0).year);
        assertEquals("變動・選擇", highlights.get(0).theme);
        assertTrue(highlights.get(0).evidence.contains("個人流年 5"));
        assertTrue(highlights.get(0).question.contains("2027"));
    }

    private static Map<String, Object> baziYear(
            int year, String ganZhi, String theme, String interaction) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("year", year);
        item.put("ganZhi", ganZhi);
        item.put("stemTenGod", "正財");
        item.put("stemTenGodMeaning", "穩定收入、資源管理");
        item.put("luckPillar", "甲子");
        item.put("plainSummary", "測試摘要 " + year);
        item.put("themes", Arrays.asList(theme));
        item.put("natalInteractions", Arrays.asList(interaction));
        return item;
    }

    private static Map<String, Object> tarotYear(
            int year, int personalYear, String card, String summary, String keywords) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("year", year);
        item.put("personalYear", personalYear);
        item.put("cardName", card);
        item.put("plainSummary", summary);
        item.put("keywords", keywords);
        return item;
    }

    private static FortuneFacts facts(
            FortuneMode mode,
            Map<String, Object> details) {
        return new FortuneFacts(
                mode, 0, "basis", 0, "", 0, 0,
                0, 0, 0, 0, "", details);
    }
}
