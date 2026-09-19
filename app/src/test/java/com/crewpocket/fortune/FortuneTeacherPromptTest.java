package com.crewpocket.fortune;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Date;

public final class FortuneTeacherPromptTest {
    private final FortuneEngine engine = new FortuneEngine();

    @Test public void teacherUsesDeterministicFactsWithoutDefaults() {
        FortuneFacts facts = engine.calculateFacts(
                FortuneMode.TAROT_NUMEROLOGY,
                new FortuneProfile("測試", "1985-07-06", "", ""),
                new Date(1789766400000L));

        String prompt = FortuneTeacherPrompt.systemPrompt(
                FortuneMode.TAROT_NUMEROLOGY,
                AiStyle.FUNNY,
                facts);

        assertTrue(prompt.contains("deterministicFacts"));
        assertTrue(prompt.contains("禁止補預設"));
        assertTrue(prompt.contains("\"personalityCardNumber\":9"));
        assertTrue(prompt.contains("\"soulCardNumber\":9"));
        assertTrue(prompt.contains("\"personalYear\":5"));
    }

    @Test public void funnyTeacherHasDistinctVoicePersona() {
        FortuneFacts facts = engine.calculateFacts(
                FortuneMode.TAROT_NUMEROLOGY,
                new FortuneProfile("測試", "1985-07-06", "", ""),
                new Date(1789766400000L));

        String prompt = FortuneTeacherPrompt.systemPrompt(
                FortuneMode.TAROT_NUMEROLOGY,
                AiStyle.FUNNY,
                facts);

        assertTrue(prompt.contains("嘴得準但不傷人"));
        assertTrue(prompt.contains("工作、家庭群組、感情、App、bug、專案管理"));
        assertEquals("Puck", FortuneTeacherPrompt.voiceName(AiStyle.FUNNY));
        assertEquals("Charon", FortuneTeacherPrompt.voiceName(AiStyle.STRICT));
    }

    @Test public void openingInvitesFocusedExplanation() {
        String prompt = FortuneTeacherPrompt.openingPrompt("小明");
        assertTrue(prompt.startsWith("小明，"));
        assertTrue(prompt.contains("最值得知道的重點"));
    }
}
