package com.crewpocket.fortune;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class AiFortuneCopyTest {
    @Test public void parsesFullReportSchema() {
        AiFortuneCopy copy = AiFortuneCopy.parse(
                "{\"title\":\"日主有話要說\","
                        + "\"overview\":\"總覽\","
                        + "\"personality\":\"性格\","
                        + "\"careerWealth\":\"工作財務\","
                        + "\"relationships\":\"感情人際\","
                        + "\"timing\":\"目前週期\","
                        + "\"translation\":\"白話翻譯\","
                        + "\"punchline\":\"補刀\","
                        + "\"advice\":\"建議\","
                        + "\"shareText\":\"分享文案\"}");
        assertEquals("日主有話要說", copy.title);
        assertEquals("工作財務", copy.careerWealth);
        assertEquals("目前週期", copy.timing);
        assertEquals("分享文案", copy.shareText);
    }

    @Test public void remainsBackwardCompatibleWithAnalysisField() {
        AiFortuneCopy copy = AiFortuneCopy.parse(
                "{\"title\":\"舊格式\",\"analysis\":\"舊分析\","
                        + "\"translation\":\"B\",\"punchline\":\"C\",\"advice\":\"D\"}");
        assertEquals("舊分析", copy.overview);
        assertTrue(copy.personality.isEmpty());
    }
}
