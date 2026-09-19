package com.crewpocket.fortune;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Date;

public final class FortuneFactsTest {
    private final FortuneEngine engine = new FortuneEngine();

    @Test public void twoSystemsExposeDifferentStructuredReports() {
        FortuneFacts bazi = engine.calculateFacts(
                FortuneMode.BA_ZI,
                new FortuneProfile("測試", "2005-12-23", "08:37", "female"),
                new Date(1760000000000L));
        FortuneFacts tarot = engine.calculateFacts(
                FortuneMode.TAROT_NUMEROLOGY,
                new FortuneProfile("測試", "2005-12-23", "", "", "JOHN DOE"),
                new Date(1760000000000L));

        assertTrue(bazi.details.containsKey("fourPillars"));
        assertTrue(bazi.details.containsKey("luckPillars"));
        assertTrue(tarot.details.containsKey("birthCards"));
        assertTrue(tarot.details.containsKey("pinnacles"));
        assertTrue(tarot.details.containsKey("coreFive"));
        assertTrue(tarot.details.containsKey("soulUrgeNumber"));
        assertTrue(tarot.details.containsKey("personalityNumber"));
        assertNotEquals(bazi.basis, tarot.basis);
    }
}
