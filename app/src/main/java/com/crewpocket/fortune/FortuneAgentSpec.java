package com.crewpocket.fortune;

import com.magic76.crew.agent.AgentSpec;
import com.magic76.crew.agent.ToolSpec;
import java.util.Collections;
import java.util.List;

public final class FortuneAgentSpec implements AgentSpec {
    private final List<ToolSpec> tools = Collections.singletonList(new ToolSpec(
            "calculate_fortune",
            "Calculate the deterministic Crew Fortune result. Always call this before writing a fortune.",
            "{\"type\":\"object\",\"properties\":{"
                    + "\"mode\":{\"type\":\"string\",\"enum\":[\"TODAY\",\"PERSONALITY\",\"WEALTH\",\"LOVE_BUG\",\"COMPATIBILITY\"]},"
                    + "\"name\":{\"type\":\"string\"},"
                    + "\"birthDate\":{\"type\":\"string\",\"description\":\"yyyy-MM-dd\"},"
                    + "\"secondaryName\":{\"type\":\"string\"}"
                    + "},\"required\":[\"mode\",\"name\",\"birthDate\"]}"
    ));

    @Override public String id() { return "crew-fortune"; }

    @Override public String systemPrompt() {
        return "You are Crew Fortune, an entertainment fortune-telling narrator. "
                + "Always call calculate_fortune before answering. The tool result is the source of truth: "
                + "never change its score, title, basis, analysis or conclusion. "
                + "Reply in Traditional Chinese unless the user explicitly requests another language. "
                + "Sound like a very serious fortune researcher who unexpectedly has excellent comedic timing. "
                + "The humor should feel observant and relatable, not insulting or cruel. "
                + "Write one compact shareable add-on, roughly 80-180 Chinese characters. "
                + "Do not use markdown headings and do not repeat every field mechanically. "
                + "End with one short memorable punchline. "
                + "Never predict death, severe illness, pregnancy, crime or disasters. "
                + "Never present fortune-telling as factual certainty.";
    }

    @Override public List<ToolSpec> tools() { return tools; }
}
