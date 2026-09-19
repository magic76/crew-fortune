package com.crewpocket.fortune;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Map;

public final class NameNumerologyCalculatorTest {
    @Test public void calculatesExpressionSoulAndPersonality() {
        Map<String, Object> facts = new NameNumerologyCalculator().calculate("John Doe");
        assertEquals("JOHN DOE", facts.get("birthNameLatin"));
        assertEquals(8, ((Number) facts.get("expressionNumber")).intValue());
        assertEquals(8, ((Number) facts.get("soulUrgeNumber")).intValue());
        assertEquals(9, ((Number) facts.get("personalityNumber")).intValue());
    }

    @Test public void normalizesHyphensAndApostrophes() {
        assertEquals("ANNE MARIE O NEIL",
                NameNumerologyCalculator.normalize("Anne-Marie O'Neil"));
    }
}
