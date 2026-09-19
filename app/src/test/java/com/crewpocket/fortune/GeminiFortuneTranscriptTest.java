package com.crewpocket.fortune;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class GeminiFortuneTranscriptTest {
    @Test public void normalizesLineBreaksIntoOneParagraph() {
        assertEquals(
                "你今天其實很適合先觀察，再決定。",
                GeminiFortuneLiveSession.normalizeTranscriptChunk(
                        "你今天其實\n很適合先觀察，\r\n再決定。"));
    }

    @Test public void preservesSpaceBetweenAsciiWordsOnly() {
        assertEquals(
                "AI teacher",
                GeminiFortuneLiveSession.normalizeTranscriptChunk("AI   teacher"));
    }
}
