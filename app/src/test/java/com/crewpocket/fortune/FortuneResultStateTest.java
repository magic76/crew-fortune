package com.crewpocket.fortune;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Date;

public final class FortuneResultStateTest {
    @Test public void roundTripsSelectedTransitDate() {
        LocalDate source = LocalDate.of(2027, 3, 14);
        assertEquals(source, FortuneResultState.parseTransitDate(
                FortuneResultState.transitDateText(source)));
    }

    @Test public void invalidTransitDateDoesNotBreakRestore() {
        assertNull(FortuneResultState.parseTransitDate("not-a-date"));
        assertNull(FortuneResultState.parseTransitDate(""));
    }

    @Test public void restoresExactCalculationReferenceTime() {
        long millis = 1789955385000L;
        Date restored = FortuneResultState.referenceDate(millis);
        assertEquals(millis, restored.getTime());
        assertTrue(FortuneResultState.referenceDate(-1L).getTime() > 0L);
    }
}
