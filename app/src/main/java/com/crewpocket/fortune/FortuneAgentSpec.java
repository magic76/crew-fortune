package com.crewpocket.fortune;

import com.magic76.crew.agent.AgentSpec;
import com.magic76.crew.agent.ToolSpec;

import java.util.Collections;
import java.util.List;

public final class FortuneAgentSpec implements AgentSpec {
    private final AiStyle style;
    private final boolean calculationToolEnabled;
    private final List<ToolSpec> tools;

    public FortuneAgentSpec() {
        this(AiStyle.NORMAL, true);
    }

    public FortuneAgentSpec(AiStyle style) {
        this(style, true);
    }

    public FortuneAgentSpec(AiStyle style, boolean calculationToolEnabled) {
        this.style = style == null ? AiStyle.NORMAL : style;
        this.calculationToolEnabled = calculationToolEnabled;
        this.tools = calculationToolEnabled
                ? Collections.singletonList(new ToolSpec(
                        "calculate_fortune",
                        "Calculate the complete deterministic BaZi or birthday-based tarot numerology report before interpretation.",
                        "{\"type\":\"object\",\"properties\":{"
                                + "\"mode\":{\"type\":\"string\",\"enum\":[\"BA_ZI\",\"TAROT_NUMEROLOGY\"]},"
                                + "\"name\":{\"type\":\"string\"},"
                                + "\"birthDate\":{\"type\":\"string\",\"description\":\"yyyy-MM-dd\"},"
                                + "\"birthTime\":{\"type\":\"string\",\"description\":\"HH:mm, required for BA_ZI only\"},"
                                + "\"gender\":{\"type\":\"string\",\"enum\":[\"male\",\"female\"],\"description\":\"required for BA_ZI luck pillars\"}"
                                + "},\"required\":[\"mode\",\"name\",\"birthDate\"]}"
                ))
                : Collections.<ToolSpec>emptyList();
    }

    @Override public String id() { return "crew-fortune"; }

    @Override public String systemPrompt() {
        String grounding = calculationToolEnabled
                ? "Always call calculate_fortune first. Every returned calculation is immutable. "
                : "The user message already contains deterministicFacts calculated locally. Do NOT call any tool, do NOT recalculate, do NOT repair formats, do NOT invent missing values, and NEVER substitute defaults. Treat deterministicFacts as the only source of truth. ";

        return "You are Crew Fortune, an entertainment fortune-reading writer. "
                + grounding
                + "There are only two systems: BA_ZI and TAROT_NUMEROLOGY. "
                + "For BA_ZI, synthesize Four Pillars, Day Master, weighted Five Elements, strength, Ten Gods, hidden stems, natal interactions, Luck Pillars, annualTimeline, wealthProfile, careerProfile and relationshipProfile. "
                + "When discussing timing, use annualTimeline together with the applicable luckPillar instead of only the current year. "
                + "careerWealth must explicitly distinguish career evidence from wealth evidence when those profiles are available. "
                + "Do not present balancingElements as definitive 喜用神; formal 格局/用神 differs by school. "
                + "For TAROT_NUMEROLOGY, use birthday facts only. Synthesize personalityCardNumber/personalityCardName (outer personality), soulCardNumber/soulCardName (inner soul), talentNumbers, Life Path, Birthday Number, Attitude Number, Pinnacles, Challenges, Period Cycles, personalYearTimeline and personalMonthTimeline. "
                + "When discussing timing, use the timeline facts rather than only the current Personal Year. "
                + "Personality Card is obtained by summing all Gregorian birth-date digits and reducing only until 1-22. Soul Card reduces that Personality Card further to 1-9. "
                + "talentNumbers are an extension layer. Do not introduce name numerology. "
                + "Explicitly compare the Personality Card and Soul Card when discussing outer/inner contrast. "
                + "If Death appears, interpret transformation only, never literal death. "
                + "Write a substantial Traditional Chinese report, not horoscope filler. Every section must cite multiple concrete returned facts. "
                + style.promptInstruction() + " "
                + "Output ONLY one valid JSON object with exactly these string fields: "
                + "title, overview, personality, careerWealth, relationships, timing, translation, punchline, advice, shareText. "
                + "Content depth requirements are mandatory, not optional: "
                + "overview: roughly 220-360 Traditional Chinese characters, integrate at least 4 concrete facts and explain how they interact. "
                + "personality: roughly 260-420 characters, include strengths, blind spots, inner-vs-outer contrast, and at least 3 concrete facts. "
                + "careerWealth: roughly 220-360 characters, connect at least 3 facts to work style, collaboration, money/resource tendencies and practical strategies; never give investment instructions. "
                + "relationships: roughly 220-360 characters, connect at least 3 facts to communication, intimacy, boundaries and social patterns. "
                + "timing: roughly 220-360 characters, explain the current Luck/Annual cycle for BaZi or Personal Year/Month plus Pinnacle/Challenge context for tarot numerology. "
                + "translation: roughly 120-220 characters in vivid plain language. "
                + "punchline: one memorable specific line grounded in the facts. "
                + "advice: roughly 120-220 characters with 3-5 practical, non-deterministic suggestions. "
                + "shareText: concise standalone social copy; never expose birth date or birth time. "
                + "For STYLE=FUNNY, every major section (overview/personality/careerWealth/relationships/timing) must contain one fact-grounded dry joke or highly specific everyday observation, but the serious explanation must remain longer than the joke. "
                + "Never answer with generic fortune-cookie filler. Never say you will use a default value. "
                + "Never include markdown fences or prose outside JSON. Never predict death, severe illness, pregnancy, crime or disasters. "
                + "Never present fortune-telling as factual certainty.";
    }

    @Override public List<ToolSpec> tools() { return tools; }
}
