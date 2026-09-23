package com.crewpocket.fortune;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class FortuneResultTabCopyTest {
    @Test public void plainLanguageTabsSeparateTopicsReportAndRawData() {
        for (FortuneMode mode : FortuneMode.values()) {
            String topics = FortuneResultTabCopy.description(mode, 2);
            String interpretation = FortuneResultTabCopy.description(mode, 3);
            String data = FortuneResultTabCopy.description(mode, 4);

            assertTrue(topics.contains("生活主題")
                    || topics.contains("個性"));
            assertTrue(interpretation.contains("完整個人文字報告"));
            assertTrue(data.contains("專業命盤資料"));
        }
    }

    @Test public void compactInterpretationKeepsShortTextAndTruncatesLongText() {
        String shortText = "這是一段簡短解讀";
        assertTrue(FortuneResultTabCopy.compactInterpretation(shortText, "").equals(shortText));

        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 220; i++) longText.append('甲');
        String compact = FortuneResultTabCopy.compactInterpretation(longText.toString(), "");
        assertTrue(compact.contains("完整內容請看「完整解讀」"));
        assertFalse(compact.equals(longText.toString()));
    }

    @Test public void overviewDescriptionsPromiseFastFirstRead() {
        for (FortuneMode mode : FortuneMode.values()) {
            String overview = FortuneResultTabCopy.description(mode, 0);
            assertTrue(overview.contains("10 秒"));
            assertTrue(overview.contains("3 個"));
        }
    }

    @Test public void overviewHintsPushTechnicalDataToTheEnd() {
        for (FortuneMode mode : FortuneMode.values()) {
            String hint = FortuneResultTabCopy.overviewHint(mode);
            assertTrue(hint.contains("先看懂自己"));
            assertTrue(hint.contains("命盤資料"));
            assertTrue(hint.contains("專業細節"));
        }
    }
}
