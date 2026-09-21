package com.crewpocket.fortune;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class FortunePresetTest {
    @Test public void roundTripsPresetJson() {
        FortunePreset source = new FortunePreset(
                "小明", "1990-02-14", "08:30", "male", FortuneMode.BA_ZI);
        FortunePreset restored = FortunePreset.fromJson(source.toJson());
        assertEquals("", source.name);
        assertEquals("", restored.name);
        assertEquals(source.birthDate, restored.birthDate);
        assertEquals(source.birthTime, restored.birthTime);
        assertEquals(source.gender, restored.gender);
        assertEquals(source.mode, restored.mode);
    }
}
