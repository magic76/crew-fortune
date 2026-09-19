package com.crewpocket.fortune;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Date;

public final class AdvancedFortuneModeTest {
    private final FortuneEngine engine = new FortuneEngine();

    @Test public void baziFactsReachFortuneEngine() {
        FortuneProfile profile = new FortuneProfile("測試", "2005-12-23", "08:37", "");
        FortuneFacts facts = engine.calculateFacts(FortuneMode.BA_ZI, profile, new Date());
        assertEquals("乙酉 戊子 辛巳 壬辰", facts.detailText("fourPillars"));
        assertTrue(facts.basis.contains("日主 辛金"));
    }

    @Test public void tarotFactsReachFortuneEngine() {
        FortuneProfile profile = new FortuneProfile("測試", "1990-02-14", "", "");
        FortuneFacts facts = engine.calculateFacts(FortuneMode.TAROT_NUMEROLOGY, profile, new Date());
        assertEquals("力量", facts.detailText("birthCardName"));
        assertEquals("8", facts.detailText("lifePathNumber"));
    }
}
