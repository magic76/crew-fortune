package com.crewpocket.fortune;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

public final class TarotNumerologyCalculatorTest {
    @Test public void personalityAndSoulUseBirthdayReduction() {
        Map<String, Object> facts = new TarotNumerologyCalculator().calculate(
                "1950-02-21", new Date(1760000000000L));
        assertEquals("20 審判 × 2 女祭司", facts.get("birthCardDisplay"));
        assertEquals(20, ((Number) facts.get("personalityCardNumber")).intValue());
        assertEquals(2, ((Number) facts.get("soulCardNumber")).intValue());
        assertEquals(2, ((Number) facts.get("lifePathNumber")).intValue());
    }

    @Test public void birthday19850706MatchesExpectedPersonalitySoulTalentAnd2026Year() {
        Date in2026 = new GregorianCalendar(2026, Calendar.JANUARY, 15).getTime();
        Map<String, Object> facts = new TarotNumerologyCalculator().calculate(
                "1985-07-06", in2026);

        assertEquals(9, ((Number) facts.get("personalityCardNumber")).intValue());
        assertEquals("隱者", facts.get("personalityCardName"));
        assertEquals(9, ((Number) facts.get("soulCardNumber")).intValue());
        assertEquals("隱者", facts.get("soulCardName"));
        assertEquals(Arrays.asList(3, 6), facts.get("talentNumbers"));
        assertEquals(5, ((Number) facts.get("personalYear")).intValue());
        assertEquals("教皇", facts.get("personalYearCardName"));
    }

    @Test public void personalYearAndMonthTimelinesAreAvailable() {
        Date in2026 = new GregorianCalendar(2026, Calendar.SEPTEMBER, 19).getTime();
        Map<String, Object> facts = new TarotNumerologyCalculator().calculate(
                "1985-07-06", in2026);

        assertEquals(2025, ((Number) facts.get("personalYearTimelineStartYear")).intValue());
        assertEquals(2035, ((Number) facts.get("personalYearTimelineEndYear")).intValue());

        Object yearsRaw = facts.get("personalYearTimeline");
        assertTrue(yearsRaw instanceof List);
        List<?> years = (List<?>) yearsRaw;
        assertEquals(11, years.size());

        Map<?, ?> current = null;
        for (Object item : years) {
            Map<?, ?> year = (Map<?, ?>) item;
            if (((Number) year.get("year")).intValue() == 2026) {
                current = year;
                break;
            }
        }
        assertTrue(current != null);
        assertEquals(5, ((Number) current.get("personalYear")).intValue());
        assertEquals("教皇", current.get("cardName"));
        assertTrue(String.valueOf(current.get("plainSummary")).length() > 8);

        Object monthsRaw = facts.get("personalMonthTimeline");
        assertTrue(monthsRaw instanceof List);
        assertEquals(12, ((List<?>) monthsRaw).size());
    }

    @Test public void birthday19690820ProducesSingleStrengthCard() {
        Map<String, Object> facts = new TarotNumerologyCalculator().calculate(
                "1969-08-20", new Date(1760000000000L));
        assertEquals("8 力量", facts.get("birthCardDisplay"));
        assertTrue(facts.get("pinnacles") instanceof List);
        assertTrue(facts.get("challenges") instanceof List);
        assertTrue(facts.get("personalYear") instanceof Number);
    }
}
