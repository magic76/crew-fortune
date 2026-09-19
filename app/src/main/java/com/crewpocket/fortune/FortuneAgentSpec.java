package com.crewpocket.fortune;

import com.magic76.crew.agent.AgentSpec;
import com.magic76.crew.agent.ToolSpec;

import java.util.Collections;
import java.util.List;

public final class FortuneAgentSpec implements AgentSpec {
    private final AiStyle style;
    private final List<ToolSpec> tools = Collections.singletonList(new ToolSpec(
            "calculate_fortune",
            "Calculate the complete deterministic BaZi or tarot numerology report before interpretation.",
            "{\"type\":\"object\",\"properties\":{"
                    + "\"mode\":{\"type\":\"string\",\"enum\":[\"BA_ZI\",\"TAROT_NUMEROLOGY\"]},"
                    + "\"name\":{\"type\":\"string\"},"
                    + "\"birthDate\":{\"type\":\"string\",\"description\":\"yyyy-MM-dd\"},"
                    + "\"birthTime\":{\"type\":\"string\",\"description\":\"HH:mm, required for BA_ZI\"},"
                    + "\"gender\":{\"type\":\"string\",\"enum\":[\"male\",\"female\"],\"description\":\"required for BA_ZI luck pillars\"}"
                    + "},\"required\":[\"mode\",\"name\",\"birthDate\"]}"
    ));

    public FortuneAgentSpec() {
        this(AiStyle.NORMAL);
    }

    public FortuneAgentSpec(AiStyle style) {
        this.style = style == null ? AiStyle.NORMAL : style;
    }

    @Override public String id() { return "crew-fortune"; }

    @Override public String systemPrompt() {
        return "You are Crew Fortune, an entertainment fortune-reading writer. "
                + "Always call calculate_fortune first. Every returned calculation is immutable. "
                + "There are only two systems: BA_ZI and TAROT_NUMEROLOGY. "
                + "For BA_ZI, synthesize Four Pillars, Day Master, weighted Five Elements, strength, Ten Gods, hidden stems, natal interactions, Luck Pillars and current Annual Pillar. "
                + "Do not present balancingElements as definitive 喜用神; formal 格局/用神 differs by school. "
                + "For TAROT_NUMEROLOGY, synthesize Life Path, Birthday Number, Attitude Number, birth-card pair/triplet, Pinnacles, Challenges, Period Cycles, Personal Year and Personal Month. "
                + "If Death appears, interpret transformation only, never literal death. "
                + "Write a substantial Traditional Chinese report, not horoscope filler. Every section must cite at least one concrete returned fact. "
                + style.promptInstruction() + " "
                + "After the tool result output ONLY one valid JSON object with exactly these string fields: "
                + "title, overview, personality, careerWealth, relationships, timing, translation, punchline, advice, shareText. "
                + "overview: 4-6 sentences integrating the main structure. "
                + "personality: 3-5 sentences about temperament, strengths and blind spots. "
                + "careerWealth: 3-5 sentences about work style, money tendencies and suitable strategies; never give investment instructions. "
                + "relationships: 3-5 sentences about interpersonal and relationship patterns. "
                + "timing: 3-5 sentences about current Luck/Annual cycle for BaZi or current Personal Year/Month and Pinnacle phase for tarot numerology. "
                + "translation: 2-4 plain-language sentences consistent with the selected style. "
                + "punchline: one memorable line, unless STYLE=STRICT where it should instead be one concise takeaway. "
                + "advice: 2-4 practical, non-deterministic suggestions. "
                + "shareText: a concise standalone social version matching the selected style; never expose birth date or birth time. "
                + "Never include markdown fences or prose outside JSON. Never predict death, severe illness, pregnancy, crime or disasters. "
                + "Never present fortune-telling as factual certainty.";
    }

    @Override public List<ToolSpec> tools() { return tools; }
}
