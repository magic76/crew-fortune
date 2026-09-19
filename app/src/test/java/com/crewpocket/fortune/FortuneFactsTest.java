package com.crewpocket.fortune;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import java.util.Date;

public final class FortuneFactsTest {
    private final FortuneEngine engine = new FortuneEngine();

    @Test public void factsStayDeterministic() {
        FortuneProfile profile = new FortuneProfile("小明", "2015-06-18", "");
        Date now = new Date(1760000000000L);
        FortuneFacts a = engine.calculateFacts(FortuneMode.WEALTH, profile, now);
        FortuneFacts b = engine.calculateFacts(FortuneMode.WEALTH, profile, now);
        assertEquals(a.score, b.score);
        assertEquals(a.momentum, b.momentum);
        assertEquals(a.stability, b.stability);
        assertEquals(a.impulse, b.impulse);
    }

    @Test public void differentModesProduceDifferentFactShape() {
        FortuneProfile profile = new FortuneProfile("小明", "2015-06-18", "");
        FortuneFacts wealth = engine.calculateFacts(FortuneMode.WEALTH, profile, new Date());
        FortuneFacts love = engine.calculateFacts(FortuneMode.LOVE_BUG, profile, new Date());
        assertNotEquals(wealth.score, love.score);
    }
}
