package com.crewpocket.fortune;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

public final class TarotNumerologyCalculatorTest {
    @Test public void matchesTarotSchoolJusticePriestessExample() {
        Map<String, Object> facts = new TarotNumerologyCalculator().calculate(
                "1950-02-21", new Date(1760000000000L));
        assertEquals("11 正義 × 2 女祭司", facts.get("birthCardDisplay"));
        assertEquals(2, ((Number) facts.get("lifePathNumber")).intValue());
        assertEquals(2, ((Number) facts.get("innerNumber")).intValue());
        assertEquals(3, ((Number) facts.get("outerNumber")).intValue());
    }

    @Test public void birthday19850706UsesBirthdayInnerOuterMethod() {
        Map<String, Object> facts = new TarotNumerologyCalculator().calculate(
                "1985-07-06", new Date(1760000000000L));
        assertEquals(9, ((Number) facts.get("lifePathNumber")).intValue());
        assertEquals(9, ((Number) facts.get("innerNumber")).intValue());
        assertEquals(6, ((Number) facts.get("outerNumber")).intValue());
    }

    @Test public void matchesTarotSchoolStarStrengthExample() {
        Map<String, Object> facts = new TarotNumerologyCalculator().calculate(
                "1969-08-20", new Date(1760000000000L));
        assertEquals("17 星星 × 8 力量", facts.get("birthCardDisplay"));
        assertTrue(facts.get("pinnacles") instanceof List);
        assertTrue(facts.get("challenges") instanceof List);
        assertTrue(facts.get("personalYear") instanceof Number);
    }
}
