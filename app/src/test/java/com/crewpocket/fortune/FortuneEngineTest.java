package com.crewpocket.fortune;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.util.Date;

public final class FortuneEngineTest {
    private final FortuneEngine engine = new FortuneEngine();

    @Test public void sameTarotInputProducesSameCoreFacts() {
        FortuneProfile profile = new FortuneProfile("小明", "1990-02-14", "", "", "JOHN DOE");
        Date now = new Date(1760000000000L);
        FortuneFacts first = engine.calculateFacts(FortuneMode.TAROT_NUMEROLOGY, profile, now);
        FortuneFacts second = engine.calculateFacts(FortuneMode.TAROT_NUMEROLOGY, profile, now);
        assertEquals(first.detailText("lifePathNumber"), second.detailText("lifePathNumber"));
        assertEquals(first.detailText("birthCardDisplay"), second.detailText("birthCardDisplay"));
        assertEquals(first.detailText("soulUrgeNumber"), second.detailText("soulUrgeNumber"));
        assertEquals(first.detailText("personalityNumber"), second.detailText("personalityNumber"));
    }

    @Test public void sameBaziInputProducesSameFourPillars() {
        FortuneProfile profile = new FortuneProfile("小明", "2005-12-23", "08:37", "male");
        Date now = new Date(1760000000000L);
        FortuneFacts first = engine.calculateFacts(FortuneMode.BA_ZI, profile, now);
        FortuneFacts second = engine.calculateFacts(FortuneMode.BA_ZI, profile, now);
        assertEquals(first.detailText("fourPillars"), second.detailText("fourPillars"));
        assertEquals(first.detailText("dayMasterStrength"), second.detailText("dayMasterStrength"));
    }

    @Test public void baziRequiresGender() {
        FortuneProfile profile = new FortuneProfile("小明", "2005-12-23", "08:37", "");
        assertThrows(IllegalArgumentException.class,
                () -> engine.calculateFacts(FortuneMode.BA_ZI, profile, new Date()));
    }
}
