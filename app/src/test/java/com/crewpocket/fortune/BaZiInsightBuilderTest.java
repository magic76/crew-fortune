package com.crewpocket.fortune;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

public final class BaZiInsightBuilderTest {
    private final FortuneEngine engine = new FortuneEngine();

    @Test public void baziIncludesSeventeenYearTimelineAndTopicProfiles() {
        Date now = new GregorianCalendar(2026, Calendar.SEPTEMBER, 19).getTime();
        FortuneFacts facts = engine.calculateFacts(
                FortuneMode.BA_ZI,
                new FortuneProfile("測試", "1985-07-06", "14:30", "male"),
                now);

        assertEquals("2025", facts.detailText("annualTimelineStartYear"));
        assertEquals("2041", facts.detailText("annualTimelineEndYear"));

        Object timelineRaw = facts.detail("annualTimeline");
        assertTrue(timelineRaw instanceof List);
        List<?> timeline = (List<?>) timelineRaw;
        assertEquals(17, timeline.size());

        Object first = timeline.get(0);
        assertTrue(first instanceof Map);
        assertEquals("2025", String.valueOf(((Map<?, ?>) first).get("year")));
        assertTrue(((Map<?, ?>) first).containsKey("stemTenGod"));
        assertTrue(((Map<?, ?>) first).containsKey("themes"));

        assertTrue(facts.detail("wealthProfile") instanceof Map);
        assertTrue(facts.detail("careerProfile") instanceof Map);
        assertTrue(facts.detail("relationshipProfile") instanceof Map);

        Map<?, ?> wealth = (Map<?, ?>) facts.detail("wealthProfile");
        assertTrue(wealth.containsKey("directWealthCount"));
        assertTrue(wealth.containsKey("annualSignalYears"));

        Map<?, ?> career = (Map<?, ?>) facts.detail("careerProfile");
        assertTrue(career.containsKey("officerCount"));
        assertTrue(career.containsKey("annualSignalYears"));

        Map<?, ?> relationship = (Map<?, ?>) facts.detail("relationshipProfile");
        assertNotNull(relationship.get("spousePalace"));
        assertTrue(relationship.containsKey("annualSignalYears"));
    }

    @Test public void luckPillarsContainInterpretationEvidence() {
        Date now = new GregorianCalendar(2026, Calendar.SEPTEMBER, 19).getTime();
        FortuneFacts facts = engine.calculateFacts(
                FortuneMode.BA_ZI,
                new FortuneProfile("測試", "1985-07-06", "14:30", "male"),
                now);

        Object luckRaw = facts.detail("luckPillars");
        assertTrue(luckRaw instanceof List);
        List<?> luck = (List<?>) luckRaw;
        assertTrue(!luck.isEmpty());

        Map<?, ?> item = (Map<?, ?>) luck.get(0);
        assertTrue(item.containsKey("stemTenGod"));
        assertTrue(item.containsKey("branchTenGods"));
        assertTrue(item.containsKey("element"));
        assertTrue(item.containsKey("interactionsWithNatal"));
        assertTrue(item.containsKey("themes"));
    }
}
