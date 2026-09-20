package com.crewpocket.fortune;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

public final class VedicAstrologyCalculatorTest {
    private static final Date J2000_NOON =
            new Date(946728000000L); // 2000-01-01T12:00:00Z

    @Test public void matchesSwissEphemerisLahiriReferenceAtJ2000() {
        Map<String, Object> facts = new VedicAstrologyCalculator().calculate(
                "2000-01-01",
                "12:00",
                new BirthPlace("London", 51.5074, -0.1278, "UTC"),
                J2000_NOON);

        assertEquals("Sidereal", facts.get("zodiac"));
        assertEquals("Lahiri / Chitrapaksha", facts.get("ayanamsa"));
        assertEquals("Whole Sign", facts.get("houseSystem"));
        assertEquals("Mean Node", facts.get("nodeType"));

        // External reference fixture: Swiss Ephemeris, Lahiri sidereal,
        // 2000-01-01T12:00Z, London 51.5074 / -0.1278.
        assertEquals(0.1614, number(facts.get("lagnaLongitude")), 0.20);
        assertPlanetLongitude(facts, "Sun", 256.5157, 0.20);
        assertPlanetLongitude(facts, "Moon", 199.4706, 0.20);
        assertPlanetLongitude(facts, "Mercury", 248.0361, 0.25);
        assertPlanetLongitude(facts, "Venus", 217.7126, 0.25);
        assertPlanetLongitude(facts, "Mars", 304.1101, 0.25);
        assertPlanetLongitude(facts, "Jupiter", 1.3998, 0.25);
        assertPlanetLongitude(facts, "Saturn", 16.5424, 0.25);
        assertPlanetLongitude(facts, "Rahu", 101.1874, 0.20);

        assertTrue(String.valueOf(facts.get("lagnaSign")).startsWith("Aries"));
        assertEquals("Ashwini", facts.get("lagnaNakshatra"));
        assertEquals(1, ((Number) facts.get("lagnaPada")).intValue());

        assertTrue(String.valueOf(facts.get("moonSign")).startsWith("Libra"));
        assertEquals("Swati", facts.get("moonNakshatra"));
        assertEquals(4, ((Number) facts.get("moonPada")).intValue());
    }

    @Test public void buildsNinePlanetsTwelveHousesAndVimshottari() {
        Map<String, Object> facts = new VedicAstrologyCalculator().calculate(
                "2000-01-01",
                "12:00",
                new BirthPlace("London", 51.5074, -0.1278, "UTC"),
                J2000_NOON);

        assertEquals(9, ((List<?>) facts.get("planets")).size());
        assertEquals(12, ((List<?>) facts.get("houses")).size());
        assertEquals(12, ((List<?>) facts.get("houseLords")).size());
        assertEquals(9, ((List<?>) facts.get("mahadashaTimeline")).size());

        Map<?, ?> currentMd = (Map<?, ?>) facts.get("currentMahadasha");
        Map<?, ?> currentAd = (Map<?, ?>) facts.get("currentAntardasha");
        assertEquals("Rahu", currentMd.get("lord"));
        assertFalse(String.valueOf(currentMd.get("startDate")).isEmpty());
        assertFalse(String.valueOf(currentMd.get("endDate")).isEmpty());
        assertNotNull(currentAd.get("lord"));

        Map<?, ?> saturn = planet(facts, "Saturn");
        assertEquals(Boolean.TRUE, saturn.get("retrograde"));

        Map<?, ?> ketu = planet(facts, "Ketu");
        double rahu = number(planet(facts, "Rahu").get("siderealLongitude"));
        double ketuLon = number(ketu.get("siderealLongitude"));
        assertEquals(180.0, angularDistance(rahu, ketuLon), 0.000001);
    }

    @Test public void fortuneEngineCarriesVedicFactsWithoutScoreSemantics() {
        FortuneProfile profile = new FortuneProfile(
                "測試",
                "2000-01-01",
                "12:00",
                "",
                new BirthPlace("London", 51.5074, -0.1278, "UTC"));

        FortuneFacts facts = new FortuneEngine().calculateFacts(
                FortuneMode.VEDIC_ASTROLOGY,
                profile,
                J2000_NOON);

        assertEquals(0, facts.score);
        assertTrue(facts.basis.contains("Sidereal · Lahiri · Whole Sign"));
        assertEquals(VedicAstrologyCalculator.METHOD_VERSION,
                facts.detailText("methodVersion"));
        assertTrue(facts.detail("careerProfile") instanceof Map);
        assertTrue(facts.detail("wealthProfile") instanceof Map);
        assertTrue(facts.detail("relationshipProfile") instanceof Map);
    }

    @Test public void rejectsAmbiguousDstBirthTimeInsteadOfGuessingOffset() {
        boolean failed = false;
        try {
            new VedicAstrologyCalculator().calculate(
                    "2021-11-07",
                    "01:30",
                    new BirthPlace("New York", 40.7128, -74.0060, "America/New_York"),
                    new Date(1636263000000L));
        } catch (IllegalArgumentException expected) {
            failed = expected.getMessage().contains("UTC offset");
        }
        assertTrue(failed);
    }

    private static void assertPlanetLongitude(
            Map<String, Object> facts,
            String name,
            double expected,
            double tolerance) {
        assertEquals(expected,
                number(planet(facts, name).get("siderealLongitude")),
                tolerance);
    }

    private static Map<?, ?> planet(Map<String, Object> facts, String name) {
        for (Object raw : (List<?>) facts.get("planets")) {
            Map<?, ?> value = (Map<?, ?>) raw;
            if (name.equals(String.valueOf(value.get("name")))) return value;
        }
        throw new AssertionError("Missing planet " + name);
    }

    private static double number(Object value) {
        return ((Number) value).doubleValue();
    }

    private static double angularDistance(double a, double b) {
        double diff = Math.abs(a - b) % 360.0;
        return diff > 180.0 ? 360.0 - diff : diff;
    }
}
