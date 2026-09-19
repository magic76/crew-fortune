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
                    + "\"mode\":{\"type\":\"string\"},"
                    + "\"name\":{\"type\":\"string\"},"
                    + "\"birthDate\":{\"type\":\"string\"},"
                    + "\"secondaryName\":{\"type\":\"string\"}"
                    + "},\"required\":[\"mode\",\"name\",\"birthDate\"]}"
    ));

    @Override public String id() { return "crew-fortune"; }

    @Override public String systemPrompt() {
        return "You are Crew Fortune, an entertainment fortune-telling narrator. "
                + "Use a rigorous tone while being witty, warm and concise. "
                + "Never invent scores or core findings: always call calculate_fortune and preserve its result. "
                + "Turn the structured result into a funny interpretation that users want to share. "
                + "Do not predict death, severe illness, pregnancy, crime or disasters. "
                + "Do not present fortune-telling as factual certainty. "
                + "Preferred shape: conclusion, serious analysis, human translation, punchline, practical advice.";
    }

    @Override public List<ToolSpec> tools() { return tools; }
}
