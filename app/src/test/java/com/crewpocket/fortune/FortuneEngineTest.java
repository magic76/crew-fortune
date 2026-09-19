package com.crewpocket.fortune;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import org.junit.Test;
import java.util.Date;

public final class FortuneEngineTest {
    private final FortuneEngine engine = new FortuneEngine();

    @Test public void sameInputProducesSameResult() {
        FortuneProfile profile = new FortuneProfile("小明", "2015-06-18", "");
        Date now = new Date(1760000000000L);
        FortuneResult first = engine.calculate(FortuneMode.TODAY, profile, now);
        FortuneResult second = engine.calculate(FortuneMode.TODAY, profile, now);
        assertEquals(first.score, second.score);
        assertEquals(first.title, second.title);
        assertEquals(first.translation, second.translation);
    }

    @Test public void nonDailyModesStayStableAcrossDates() {
        FortuneProfile profile = new FortuneProfile("Alice", "1990-03-12", "");
        FortuneResult first = engine.calculate(FortuneMode.PERSONALITY, profile, new Date(0L));
        FortuneResult second = engine.calculate(FortuneMode.PERSONALITY, profile, new Date(1893456000000L));
        assertEquals(first.score, second.score);
        assertEquals(first.title, second.title);
    }

    @Test public void compatibilityRequiresSecondName() {
        FortuneProfile profile = new FortuneProfile("小明", "2015-06-18", "");
        assertThrows(IllegalArgumentException.class,
                () -> engine.calculate(FortuneMode.COMPATIBILITY, profile, new Date()));
    }

    @Test public void invalidBirthDateIsRejected() {
        FortuneProfile profile = new FortuneProfile("小明", "2015-99-99", "");
        assertThrows(IllegalArgumentException.class,
                () -> engine.calculate(FortuneMode.WEALTH, profile, new Date()));
    }
}
