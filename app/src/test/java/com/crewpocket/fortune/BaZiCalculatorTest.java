package com.crewpocket.fortune;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Map;

public final class BaZiCalculatorTest {
    @Test public void matchesLunarJavaReferencePillars() {
        Map<String, Object> facts = new BaZiCalculator().calculate("2005-12-23", "08:37");
        assertEquals("乙酉", facts.get("yearPillar"));
        assertEquals("戊子", facts.get("monthPillar"));
        assertEquals("辛巳", facts.get("dayPillar"));
        assertEquals("壬辰", facts.get("timePillar"));
        assertEquals("辛", facts.get("dayMaster"));
        assertEquals("金", facts.get("dayMasterElement"));
    }

    @Test public void requiresBirthTime() {
        boolean failed = false;
        try {
            new BaZiCalculator().calculate("2005-12-23", "");
        } catch (IllegalArgumentException expected) {
            failed = true;
        }
        assertTrue(failed);
    }
}
