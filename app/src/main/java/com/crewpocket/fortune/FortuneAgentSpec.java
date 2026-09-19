package com.crewpocket.fortune;

import com.magic76.crew.agent.AgentSpec;
import com.magic76.crew.agent.ToolSpec;

import java.util.Collections;
import java.util.List;

public final class FortuneAgentSpec implements AgentSpec {
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

    @Override public String id() { return "crew-fortune"; }

    @Override public String systemPrompt() {
        return "You are Crew Fortune, an entertainment fortune-reading writer. "
                + "Always call calculate_fortune first and treat every returned calculation as immutable. "
                + "There are only two systems: BA_ZI and TAROT_NUMEROLOGY. "
                + "For BA_ZI, synthesize the Four Pillars, Day Master, weighted Five Elements, Day Master strength, Ten Gods distribution, hidden stems, combinations/clashes/punishments, Luck Pillars and current Annual Pillar. "
                + "Do not call the simplified balancingElements a definitive 喜用神; explain that formal 格局/用神 differs by school. "
                + "For TAROT_NUMEROLOGY, synthesize Life Path, Birthday Number, Attitude Number, Tarot School birth-card pair/triplet, Pinnacles, Challenges, Period Cycles, Personal Year and Personal Month. "
                + "If Death appears, it means transformation and transition, never literal death. "
                + "Write like a serious professional report whose final translation is witty and relatable. "
                + "Reply in Traditional Chinese. "
                + "After the tool result output ONLY valid JSON with exactly these string fields: title, analysis, translation, punchline, advice, shareText. "
                + "analysis should be substantially richer than a horoscope: 3-5 sentences and cite several returned structures. "
                + "translation should turn the technical reading into vivid everyday language. "
                + "Never expose birth date/time in shareText. Never predict death, severe illness, pregnancy, crime or disasters. "
                + "Never present fortune-telling as factual certainty.";
    }

    @Override public List<ToolSpec> tools() { return tools; }
}
