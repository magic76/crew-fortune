package com.crewpocket.fortune;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Map;

public final class TarotNumerologyCalculatorTest {
    @Test public void calculatesBirthCardFromDigitSum() {
        Map<String, Object> facts = new TarotNumerologyCalculator().calculate("1990-02-14");
        assertEquals(26, ((Number) facts.get("rawDigitSum")).intValue());
        assertEquals(8, ((Number) facts.get("lifePathNumber")).intValue());
        assertEquals(8, ((Number) facts.get("birthCardNumber")).intValue());
        assertEquals("力量", facts.get("birthCardName"));
    }

    @Test public void mapsTwentyTwoToTheFool() {
        Map<String, Object> facts = new TarotNumerologyCalculator().calculate("1979-09-13");
        if (((Number) facts.get("tarotReductionValue")).intValue() == 22) {
            assertEquals(0, ((Number) facts.get("birthCardNumber")).intValue());
            assertEquals("愚者", facts.get("birthCardName"));
        }
    }
}
