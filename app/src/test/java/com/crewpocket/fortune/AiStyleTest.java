package com.crewpocket.fortune;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class AiStyleTest {
    @Test public void stylesHaveDistinctCreativityLevels() {
        assertTrue(AiStyle.STRICT.temperature() < AiStyle.NORMAL.temperature());
        assertTrue(AiStyle.NORMAL.temperature() < AiStyle.FUNNY.temperature());
    }

    @Test public void funnyPromptChangesWholeReportStyle() {
        String prompt = new FortuneAgentSpec(AiStyle.FUNNY).systemPrompt();
        assertTrue(prompt.contains("STYLE=FUNNY"));
        assertTrue(prompt.contains("concrete fact -> serious interpretation"));
        assertTrue(prompt.contains("被看穿了，但很好笑"));
        assertTrue(prompt.contains("Avoid fortune-cookie filler"));
    }

    @Test public void strictPromptExplicitlyAvoidsJokes() {
        String prompt = new FortuneAgentSpec(AiStyle.STRICT).systemPrompt();
        assertTrue(prompt.contains("STYLE=STRICT"));
        assertTrue(prompt.contains("Avoid jokes"));
    }

    @Test public void labelsMatchUi() {
        assertEquals("嚴謹", AiStyle.STRICT.label());
        assertEquals("普通", AiStyle.NORMAL.label());
        assertEquals("風趣", AiStyle.FUNNY.label());
    }
}
