package com.crewpocket.fortune;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Date;

public final class AdvancedFortuneModeTest {
    private final FortuneEngine engine = new FortuneEngine();

    @Test public void baziFactsReachFortuneEngine() {
        FortuneProfile profile = new FortuneProfile("測試", "2005-12-23", "08:37", "male");
        FortuneFacts facts = engine.calculateFacts(FortuneMode.BA_ZI, profile, new Date(1760000000000L));
        assertEquals("乙酉 戊子 辛巳 壬辰", facts.detailText("fourPillars"));
        assertEquals("辛", facts.detailText("dayMaster"));
        assertTrue(facts.detail("luckPillars") instanceof java.util.List);
        assertTrue(facts.detail("tenGodDistribution") instanceof java.util.Map);
    }

    @Test public void tarotFactsReachFortuneEngine() {
        FortuneProfile profile = new FortuneProfile("測試", "1950-02-21", "", "");
        FortuneFacts facts = engine.calculateFacts(FortuneMode.TAROT_NUMEROLOGY, profile, new Date(1760000000000L));
        assertEquals("11 正義 × 2 女祭司", facts.detailText("birthCardDisplay"));
        assertEquals("2", facts.detailText("lifePathNumber"));
        assertTrue(facts.detail("pinnacles") instanceof java.util.List);
    }
}
