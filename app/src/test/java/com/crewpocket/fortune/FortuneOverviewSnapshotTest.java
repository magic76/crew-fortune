package com.crewpocket.fortune;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Date;
import java.util.List;

public final class FortuneOverviewSnapshotTest {
    private final FortuneEngine engine = new FortuneEngine();

    @Test public void everyModeHasThreeDeterministicTakeaways() {
        for (FortuneMode mode : FortuneMode.values()) {
            FortuneProfile profile = profile(mode);
            FortuneFacts facts = engine.calculateFacts(
                    mode,
                    profile,
                    new Date(1789766400000L));
            List<String> values =
                    FortuneOverviewSnapshot.keyTakeaways(mode, facts, null);
            assertEquals(3, values.size());
            for (String value : values) {
                assertFalse(value.trim().isEmpty());
                assertTrue(value.length() <= 106);
            }
            assertFalse(
                    FortuneOverviewSnapshot.currentTiming(mode, facts, null)
                            .trim().isEmpty());
        }
    }

    @Test public void aiTraitsOverrideFallbackWhenAvailable() {
        AiFortuneCopy copy = AiFortuneCopy.parse(
                "{"
                        + "\"title\":\"T\","
                        + "\"overview\":\"O\","
                        + "\"topTraits\":[\"A\",\"B\",\"C\"],"
                        + "\"currentCycle\":\"目前週期文字\""
                        + "}");
        List<String> values =
                FortuneOverviewSnapshot.keyTakeaways(
                        FortuneMode.TAROT_NUMEROLOGY,
                        null,
                        copy);
        assertEquals("A", values.get(0));
        assertEquals("B", values.get(1));
        assertEquals("C", values.get(2));
        assertEquals(
                "目前週期文字",
                FortuneOverviewSnapshot.currentTiming(
                        FortuneMode.TAROT_NUMEROLOGY,
                        null,
                        copy));
    }

    @Test public void compactNormalizesWhitespaceAndCapsLength() {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < 200; i++) value.append("甲 ");
        String compact = FortuneOverviewSnapshot.compact(
                value.toString(),
                60);
        assertTrue(compact.length() <= 61);
        assertTrue(compact.endsWith("…"));
    }

    private static FortuneProfile profile(FortuneMode mode) {
        if (mode == FortuneMode.BA_ZI) {
            return new FortuneProfile(
                    "",
                    "1985-07-06",
                    "04:10",
                    "male");
        }
        if (mode == FortuneMode.VEDIC_ASTROLOGY) {
            return new FortuneProfile(
                    "",
                    "1985-07-06",
                    "04:10",
                    "",
                    new BirthPlace(
                            "New Taipei",
                            25.0120,
                            121.4657,
                            "Asia/Taipei"));
        }
        return new FortuneProfile(
                "",
                "1985-07-06",
                "",
                "");
    }
}
