package com.crewpocket.fortune;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Date;

public final class VedicProductIntegrationTest {
    private static final Date J2000_NOON = new Date(946728000000L);

    @Test public void liveTeacherIsGroundedAndDeclaresMissingVedicLayers() {
        FortuneFacts facts = new FortuneEngine().calculateFacts(
                FortuneMode.VEDIC_ASTROLOGY,
                new FortuneProfile(
                        "測試",
                        "2000-01-01",
                        "12:00",
                        "",
                        new BirthPlace("London", 51.5074, -0.1278, "UTC")),
                J2000_NOON);

        String prompt = FortuneTeacherPrompt.systemPrompt(
                FortuneMode.VEDIC_ASTROLOGY,
                AiStyle.FUNNY,
                facts);

        assertTrue(prompt.contains("Sidereal Zodiac"));
        assertTrue(prompt.contains("Lahiri ayanamsa"));
        assertTrue(prompt.contains("Whole Sign Houses"));
        assertTrue(prompt.contains("currentMahadasha"));
        assertTrue(prompt.contains("currentAntardasha"));
        assertTrue(prompt.contains("Navamsa D9"));
        assertTrue(prompt.contains("目前 facts 不足"));
        assertTrue(prompt.contains("不得預測死亡"));
        assertTrue(prompt.contains("currentTransits"));
        assertTrue(prompt.contains("majorTransitTimeline"));
        assertTrue(prompt.contains("三年內 Jupiter、Saturn、Rahu、Ketu"));
    }

    @Test public void vedicShareCardDoesNotExposeBirthPlaceOrCoordinates() {
        FortuneProfile profile = new FortuneProfile(
                "Private Name",
                "2000-01-01",
                "12:00",
                "",
                new BirthPlace("London", 51.5074, -0.1278, "UTC"));
        FortuneEngine engine = new FortuneEngine();
        FortuneFacts facts = engine.calculateFacts(
                FortuneMode.VEDIC_ASTROLOGY, profile, J2000_NOON);
        FortuneResult result = engine.calculate(
                FortuneMode.VEDIC_ASTROLOGY, profile, J2000_NOON);

        FortuneShareCardData data = FortuneShareCardData.from(
                FortuneMode.VEDIC_ASTROLOGY,
                facts,
                result,
                null,
                profile.name);

        String all = data.allTextForTest();
        assertTrue(all.contains("印度星盤"));
        assertTrue(all.contains("Lagna｜"));
        assertTrue(all.contains("Mahadasha"));
        assertFalse(all.contains("Private Name"));
        assertFalse(all.contains("London"));
        assertFalse(all.contains("51.5074"));
        assertFalse(all.contains("-0.1278"));
        assertFalse(all.contains("12:00"));
    }

    @Test public void localReportKeepsOmittedFeaturesExplicit() {
        FortuneFacts facts = new FortuneEngine().calculateFacts(
                FortuneMode.VEDIC_ASTROLOGY,
                new FortuneProfile(
                        "測試",
                        "2000-01-01",
                        "12:00",
                        "",
                        new BirthPlace("London", 51.5074, -0.1278, "UTC")),
                J2000_NOON);

        String boundary = FortuneLocalReport.sections(facts).get("解讀邊界");
        assertTrue(boundary.contains("Navamsa D9"));
        assertTrue(boundary.contains("Shadbala"));
        assertTrue(boundary.contains("Gochar 可切換查看日期"));
        assertTrue(boundary.contains("Jupiter、Saturn、Rahu、Ketu"));
    }

    @Test public void vedicEvidenceFormattersExposeFactsWithoutTeacher() {
        FortuneFacts facts = new FortuneEngine().calculateFacts(
                FortuneMode.VEDIC_ASTROLOGY,
                new FortuneProfile(
                        "測試",
                        "2000-01-01",
                        "12:00",
                        "",
                        new BirthPlace("London", 51.5074, -0.1278, "UTC")),
                J2000_NOON);

        String lords = VedicFactsFormatter.houseLordPlacements(facts);
        assertTrue(lords.contains("H1 宮主"));
        assertTrue(lords.contains("→ H"));

        String careerTiming =
                VedicFactsFormatter.topicTimingEvidence(facts, "careerProfile");
        assertTrue(careerTiming.contains("Mahadasha"));
        assertTrue(careerTiming.contains("Gochar"));
    }

}
