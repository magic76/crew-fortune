package com.crewpocket.fortune;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class FortuneAgentSpecTest {
    @Test public void interpretationPromptUsesConclusionEvidenceSynthesisContract() {
        String prompt = new FortuneAgentSpec(
                AiStyle.NORMAL,
                false).systemPrompt();

        assertTrue(prompt.contains(
                "conclusion first → 2-4 concrete evidence points"));
        assertTrue(prompt.contains("Avoid repetition"));
        assertTrue(prompt.contains("Prefer causal synthesis"));
        assertTrue(prompt.contains(
                "overview: roughly 180-280"));
        assertTrue(prompt.contains(
                "overview gives the synthesis only"));
    }

    @Test public void majorSectionsHaveDistinctResponsibilities() {
        String prompt = new FortuneAgentSpec(
                AiStyle.FUNNY,
                false).systemPrompt();

        assertTrue(prompt.contains(
                "personality owns stable behavior patterns"));
        assertTrue(prompt.contains(
                "career owns work style"));
        assertTrue(prompt.contains(
                "wealth owns money/resource patterns"));
        assertTrue(prompt.contains(
                "currentCycle owns the present timing"));
        assertTrue(prompt.contains(
                "longTerm/keyYears own future timing"));
    }
}
