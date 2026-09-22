package com.crewpocket.fortune;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FortuneHistoryEntryTest {
    @Test public void roundTripKeepsResultFactsAndProfileWithoutRecalculation() {
        Map<String, Object> nested = new LinkedHashMap<String, Object>();
        nested.put("ganZhi", "甲子");
        nested.put("year", 2027);

        List<Object> years = new ArrayList<Object>();
        years.add(nested);

        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("currentAnnual", nested);
        details.put("annualTimeline", years);

        FortunePreset preset = new FortunePreset(
                "",
                "1985-07-22",
                "14:30",
                "male",
                FortuneMode.BA_ZI);

        FortuneFacts facts = new FortuneFacts(
                FortuneMode.BA_ZI,
                72,
                "test basis",
                0,
                "",
                0,
                0,
                0,
                0,
                0,
                0,
                "day",
                details);

        FortuneResult result = new FortuneResult(
                FortuneMode.BA_ZI,
                72,
                "測試結果",
                "test basis",
                "analysis",
                "translation",
                "punchline",
                "advice");

        FortuneHistoryEntry original = FortuneHistoryEntry.create(
                preset,
                123456789L,
                "",
                result,
                facts,
                null);

        FortuneHistoryEntry restored =
                FortuneHistoryEntry.fromJson(original.toJson());

        assertNotNull(restored);
        assertEquals(FortuneMode.BA_ZI, restored.preset.mode);
        assertEquals("1985-07-22", restored.preset.birthDate);
        assertEquals("測試結果", restored.result.title);
        assertEquals("test basis", restored.facts.basis);

        Object annual = restored.facts.detail("currentAnnual");
        assertTrue(annual instanceof Map);
        assertEquals("甲子", ((Map<?, ?>) annual).get("ganZhi"));

        Object timeline = restored.facts.detail("annualTimeline");
        assertTrue(timeline instanceof List);
        assertEquals(1, ((List<?>) timeline).size());
    }

    @Test public void vedicHistoryKeepsResolvedBirthplaceInternals() {
        FortunePreset preset = new FortunePreset(
                "",
                "1985-07-22",
                "14:30",
                "male",
                FortuneMode.VEDIC_ASTROLOGY,
                "Bangkok, Thailand",
                "13.756300",
                "100.501800",
                "Asia/Bangkok");

        FortuneFacts facts = new FortuneFacts(
                FortuneMode.VEDIC_ASTROLOGY,
                0,
                "vedic",
                0,
                "",
                0,
                0,
                0,
                0,
                0,
                0,
                "",
                new LinkedHashMap<String, Object>());

        FortuneResult result = new FortuneResult(
                FortuneMode.VEDIC_ASTROLOGY,
                0,
                "Vedic",
                "vedic",
                "",
                "",
                "",
                "");

        FortuneHistoryEntry restored =
                FortuneHistoryEntry.fromJson(
                        FortuneHistoryEntry.create(
                                preset,
                                42L,
                                "2026-09-22",
                                result,
                                facts,
                                null).toJson());

        assertNotNull(restored);
        assertEquals("Bangkok, Thailand", restored.preset.birthPlaceName);
        assertEquals("Asia/Bangkok", restored.preset.timeZoneId);
        assertEquals("13.756300", restored.preset.latitude);
        assertEquals("100.501800", restored.preset.longitude);
    }
}
