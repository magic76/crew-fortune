package com.crewpocket.fortune;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class FortuneCommercialFlowTest {
    @Test public void readingIdIsStableAndDoesNotExposeBirthData() {
        FortunePreset preset = new FortunePreset(
                "",
                "1985-07-06",
                "04:10",
                "male",
                FortuneMode.BA_ZI);

        String first = FortuneReadingId.from(preset);
        String second = FortuneReadingId.from(preset);

        assertEquals(first, second);
        assertEquals(64, first.length());
        assertFalse(first.contains("1985"));
        assertFalse(first.contains("07-06"));
        assertFalse(first.contains("04:10"));
    }

    @Test public void differentProfileOrModeGetsDifferentReadingId() {
        FortunePreset bazi = new FortunePreset(
                "",
                "1985-07-06",
                "04:10",
                "male",
                FortuneMode.BA_ZI);
        FortunePreset tarot = new FortunePreset(
                "",
                "1985-07-06",
                "",
                "",
                FortuneMode.TAROT_NUMEROLOGY);

        assertFalse(
                FortuneReadingId.from(bazi)
                        .equals(FortuneReadingId.from(tarot)));
    }

    @Test public void irrelevantSharedFieldsDoNotChangeTarotReadingId() {
        FortunePreset first = new FortunePreset(
                "",
                "1985-07-06",
                "04:10",
                "male",
                FortuneMode.TAROT_NUMEROLOGY,
                "Bangkok",
                "13.7563",
                "100.5018",
                "Asia/Bangkok");
        FortunePreset second = new FortunePreset(
                "",
                "1985-07-06",
                "22:45",
                "female",
                FortuneMode.TAROT_NUMEROLOGY,
                "Taipei",
                "25.0330",
                "121.5654",
                "Asia/Taipei");

        assertEquals(
                FortuneReadingId.from(first),
                FortuneReadingId.from(second));
    }

    @Test public void irrelevantSharedFieldsDoNotChangeBaziOrVedicReadingId() {
        FortunePreset baziA = new FortunePreset(
                "", "1985-07-06", "04:10", "male",
                FortuneMode.BA_ZI,
                "Bangkok", "13.7", "100.5", "Asia/Bangkok");
        FortunePreset baziB = new FortunePreset(
                "", "1985-07-06", "04:10", "male",
                FortuneMode.BA_ZI,
                "Taipei", "25.0", "121.5", "Asia/Taipei");
        assertEquals(
                FortuneReadingId.from(baziA),
                FortuneReadingId.from(baziB));

        FortunePreset vedicA = new FortunePreset(
                "", "1985-07-06", "04:10", "male",
                FortuneMode.VEDIC_ASTROLOGY,
                "Bangkok", "13.7", "100.5", "Asia/Bangkok");
        FortunePreset vedicB = new FortunePreset(
                "", "1985-07-06", "04:10", "female",
                FortuneMode.VEDIC_ASTROLOGY,
                "Bangkok", "13.7", "100.5", "Asia/Bangkok");
        assertEquals(
                FortuneReadingId.from(vedicA),
                FortuneReadingId.from(vedicB));
    }

    @Test public void aiCopyRoundTripsForPaidLocalRestore() {
        AiFortuneCopy copy = AiFortuneCopy.parse(
                "{"
                        + "\"title\":\"完整報告\","
                        + "\"overview\":\"核心總覽\","
                        + "\"personality\":\"性格\","
                        + "\"career\":\"工作\","
                        + "\"wealth\":\"財運\","
                        + "\"relationships\":\"關係\","
                        + "\"family\":\"家庭\","
                        + "\"currentCycle\":\"現在\","
                        + "\"longTerm\":\"長期\","
                        + "\"keyYears\":\"年份\","
                        + "\"topTraits\":[\"A\",\"B\",\"C\"],"
                        + "\"topTraitEvidence\":[\"EA\",\"EB\",\"EC\"],"
                        + "\"followUps\":[\"Q1\",\"Q2\",\"Q3\",\"Q4\"],"
                        + "\"translation\":\"白話\","
                        + "\"punchline\":\"一句\","
                        + "\"advice\":\"建議\","
                        + "\"shareText\":\"分享\""
                        + "}");

        AiFortuneCopy restored =
                AiFortuneCopy.parse(copy.toJson());

        assertEquals(copy.title, restored.title);
        assertEquals(copy.career, restored.career);
        assertEquals(copy.wealth, restored.wealth);
        assertEquals(copy.topTraits, restored.topTraits);
        assertEquals(copy.followUps, restored.followUps);
        assertTrue(restored.toJson().contains("\"完整報告\""));
    }
}
