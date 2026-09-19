package com.crewpocket.fortune;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class AiFortuneCopyTest {
    @Test public void parsesStructuredAiCopy() {
        AiFortuneCopy copy = AiFortuneCopy.parse(
                "{\"title\":\"財神有來但你也在線\","
                        + "\"analysis\":\"財運結構偏穩定。\","
                        + "\"translation\":\"錢剛進門，購物車就開門迎接。\","
                        + "\"punchline\":\"財神不是外送員。\","
                        + "\"advice\":\"大額購物先睡一晚。\","
                        + "\"shareText\":\"今天財運很忙。\"}");
        assertEquals("財神有來但你也在線", copy.title);
        assertEquals("今天財運很忙。", copy.shareText);
    }

    @Test public void stripsMarkdownFenceIfModelAddsOne() {
        AiFortuneCopy copy = AiFortuneCopy.parse(
                "```json\n{\"title\":\"測試\",\"analysis\":\"A\",\"translation\":\"B\","
                        + "\"punchline\":\"C\",\"advice\":\"D\"}\n```");
        assertEquals("測試", copy.title);
        assertTrue(copy.shareText.isEmpty());
    }
}
