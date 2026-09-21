package com.crewpocket.fortune;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class FortuneResultTabCopyTest {
    @Test public void topicTabsPointToShortSummaryAndInterpretationOwnsFullReport() {
        for (FortuneMode mode : FortuneMode.values()) {
            String topic = FortuneResultTabCopy.description(mode, 3);
            String interpretation = FortuneResultTabCopy.description(mode, 4);
            assertTrue(topic.contains("短摘要"));
            assertTrue(topic.contains("解讀"));
            assertTrue(interpretation.contains("完整文字報告"));
        }
    }

    @Test public void compactInterpretationKeepsShortTextAndTruncatesLongText() {
        String shortText = "這是一段簡短解讀";
        assertTrue(FortuneResultTabCopy.compactInterpretation(shortText, "").equals(shortText));

        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 220; i++) longText.append('甲');
        String compact = FortuneResultTabCopy.compactInterpretation(longText.toString(), "");
        assertTrue(compact.contains("完整內容請看「解讀」"));
        assertFalse(compact.equals(longText.toString()));
    }

    @Test public void overviewHintsKeepInterpretationAsPrimaryLongFormDestination() {
        for (FortuneMode mode : FortuneMode.values()) {
            assertTrue(FortuneResultTabCopy.overviewHint(mode).contains("解讀＝完整文字報告"));
        }
    }
}
