package com.crewpocket.fortune;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public final class AiFortuneCopyTest {
    private static final String FULL_JSON =
            "{"
                    + "\"title\":\"日主有話要說\","
                    + "\"overview\":\"總覽內容\","
                    + "\"personality\":\"性格內容\","
                    + "\"career\":\"工作內容\","
                    + "\"wealth\":\"財運內容\","
                    + "\"relationships\":\"感情內容\","
                    + "\"currentCycle\":\"目前週期\","
                    + "\"longTerm\":\"長期走勢\","
                    + "\"keyYears\":\"重要年份\","
                    + "\"topTraits\":[\"很像你的第一點\",\"很像你的第二點\",\"很像你的第三點\"],"
                    + "\"topTraitEvidence\":[\"日主壬水、身強\",\"食傷、目前大運\",\"財星、流年互動\"],"
                    + "\"followUps\":[\"工作哪幾年最值得衝？\",\"財運哪幾年訊號最強？\",\"感情上最大的盲點是什麼？\",\"未來十年哪一年變化最大？\"],"
                    + "\"translation\":\"白話翻譯\","
                    + "\"punchline\":\"補刀\","
                    + "\"advice\":\"建議\","
                    + "\"shareText\":\"分享文案\""
                    + "}";

    @Test public void parsesValidJson() {
        AiFortuneCopy copy = AiFortuneCopy.parse(FULL_JSON);
        assertEquals("日主有話要說", copy.title);
        assertEquals("工作內容", copy.career);
        assertEquals("財運內容", copy.wealth);
        assertEquals("長期走勢", copy.longTerm);
        assertEquals("重要年份", copy.keyYears);
        assertEquals(3, copy.topTraits.size());
        assertEquals("很像你的第一點", copy.topTraits.get(0));
        assertEquals(3, copy.topTraitEvidence.size());
        assertEquals("日主壬水、身強", copy.topTraitEvidence.get(0));
        assertEquals(4, copy.followUps.size());
        assertEquals("工作哪幾年最值得衝？", copy.followUps.get(0));
        assertEquals("分享文案", copy.shareText);
    }

    @Test public void parsesMarkdownJsonCodeFence() {
        AiFortuneCopy copy = AiFortuneCopy.parse(
                "```json\n" + FULL_JSON + "\n```");
        assertEquals("日主有話要說", copy.title);
        assertEquals("總覽內容", copy.overview);
    }

    @Test public void parsesJsonWithExtraTextBeforeAndAfter() {
        AiFortuneCopy copy = AiFortuneCopy.parse(
                "以下是結果，請參考：\n" + FULL_JSON + "\n以上內容僅供娛樂。");
        assertEquals("日主有話要說", copy.title);
        assertEquals("白話翻譯", copy.translation);
    }

    @Test public void supportsAnalysisWhenOverviewIsMissing() {
        AiFortuneCopy copy = AiFortuneCopy.parse(
                "{\"title\":\"舊格式\",\"analysis\":\"舊分析\","
                        + "\"translation\":\"B\",\"punchline\":\"C\",\"advice\":\"D\"}");
        assertEquals("舊分析", copy.overview);
    }

    @Test public void missingAdviceUsesSafeFallbackInsteadOfFailingWholeReport() {
        AiFortuneCopy copy = AiFortuneCopy.parse(
                "{\"title\":\"可解析\",\"overview\":\"有核心總覽\"}");
        assertFalse(copy.advice.isEmpty());
        assertFalse(copy.translation.isEmpty());
        assertFalse(copy.punchline.isEmpty());
    }

    @Test public void legacyCareerWealthAndTimingRemainCompatible() {
        AiFortuneCopy copy = AiFortuneCopy.parse(
                "{\"title\":\"舊格式\",\"overview\":\"總覽\","
                        + "\"careerWealth\":\"工作財務舊欄位\","
                        + "\"timing\":\"舊週期\"}");
        assertEquals("工作財務舊欄位", copy.career);
        assertEquals("工作財務舊欄位", copy.wealth);
        assertEquals("舊週期", copy.currentCycle);
    }

    @Test public void missingCoreOverviewHasIdentifiableError() {
        try {
            AiFortuneCopy.parse("{\"title\":\"只有標題\",\"advice\":\"建議\"}");
            fail("Expected parser failure");
        } catch (IllegalArgumentException error) {
            assertTrue(error.getMessage().contains("Missing core field: overview/analysis"));
        }
    }

    @Test public void missingCoreTitleHasIdentifiableError() {
        try {
            AiFortuneCopy.parse("{\"overview\":\"只有總覽\"}");
            fail("Expected parser failure");
        } catch (IllegalArgumentException error) {
            assertTrue(error.getMessage().contains("Missing core field: title"));
        }
    }

    @Test public void truncatedJsonHasIdentifiableError() {
        try {
            AiFortuneCopy.parse(
                    "{\"title\":\"截斷\",\"overview\":\"還沒結束");
            fail("Expected parser failure");
        } catch (IllegalArgumentException error) {
            assertTrue(error.getMessage().contains("Truncated JSON object"));
        }
    }

    @Test public void malformedJsonHasIdentifiableError() {
        try {
            AiFortuneCopy.parse(
                    "{\"title\":\"壞格式\",\"overview\":[}");
            fail("Expected parser failure");
        } catch (IllegalArgumentException error) {
            assertTrue(error.getMessage().contains("Invalid JSON"));
        }
    }

    @Test public void shortReportIsFlaggedForOneExpansionRetry() {
        AiFortuneCopy copy = AiFortuneCopy.parse(FULL_JSON);
        String issues = copy.qualityIssueSummary(FortuneMode.BA_ZI);
        assertTrue(issues.contains("overview="));
        assertTrue(issues.contains("longTerm="));
        assertTrue(issues.contains("keyYears="));
        assertFalse(issues.contains("topTraits="));
        assertFalse(issues.contains("topTraitEvidence="));
        assertFalse(issues.contains("followUps="));
    }

    @Test public void arrayFieldsAlsoAcceptLineSeparatedFallback() {
        AiFortuneCopy copy = AiFortuneCopy.parse(
                "{\"title\":\"相容格式\",\"overview\":\"總覽\","
                        + "\"topTraits\":\"第一點\\n第二點\\n第三點\","
                        + "\"topTraitEvidence\":\"依據一\\n依據二\\n依據三\","
                        + "\"followUps\":\"問題一？|問題二？|問題三？|問題四？\"}");
        assertEquals(3, copy.topTraits.size());
        assertEquals(3, copy.topTraitEvidence.size());
        assertEquals(4, copy.followUps.size());
    }

    @Test public void cleanJsonIgnoresBracesInsideStrings() {
        String raw = "prefix {\"title\":\"A { B }\",\"overview\":\"C\"} suffix";
        assertEquals(
                "{\"title\":\"A { B }\",\"overview\":\"C\"}",
                AiFortuneCopy.cleanJson(raw));
    }
}
