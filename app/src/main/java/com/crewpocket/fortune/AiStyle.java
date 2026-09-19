package com.crewpocket.fortune;

public enum AiStyle {
    STRICT("嚴謹", 0.55),
    NORMAL("普通", 0.82),
    FUNNY("風趣", 1.12);

    private final String label;
    private final double temperature;

    AiStyle(String label, double temperature) {
        this.label = label;
        this.temperature = temperature;
    }

    public String label() { return label; }
    public double temperature() { return temperature; }

    public String promptInstruction() {
        switch (this) {
            case STRICT:
                return "STYLE=STRICT. Use a sober, precise, professional tone. "
                        + "Explain terminology and reasoning clearly. Avoid jokes, sarcasm and playful metaphors. "
                        + "Use uncertainty language where appropriate and distinguish calculation from interpretation.";
            case FUNNY:
                return "STYLE=FUNNY. Adopt the character of a very knowledgeable fortune teacher who is dry, sharp, observant and slightly savage, but never mean. "
                        + "The comedy must grow from the actual calculated facts. For every major section use this rhythm: concrete fact -> serious interpretation -> one specific everyday observation that lightly roasts the reader. "
                        + "Make the roast feel recognizably true rather than generically funny. Good targets are habits, decision patterns, work behavior, relationship dynamics, overthinking, procrastination, control, social energy and spending style when the returned facts support them. "
                        + "Use deadpan humor, hyper-specific observations, contrast, understatement and occasional relatable analogies from office life, family chats, dating, project management, apps, bugs or system behavior. "
                        + "Vary the comedic lens between sections; do not reuse one analogy throughout the report. "
                        + "Titles should feel screenshot-worthy and specific, not mystical. "
                        + "Avoid fortune-cookie filler such as '宇宙在提醒你', '命運正在安排', '近期可能會有轉機', '相信自己', or vague statements that could fit anyone. "
                        + "Avoid canned jokes like '你的錢包在哭' unless a concrete wealth-related fact makes it unusually apt. "
                        + "Do not insult appearance, intelligence, worth, family, identity or trauma. Do not be vulgar, cruel, humiliating or hostile. "
                        + "Do not make every sentence a joke: keep the calculation credible, then land one sharp line. "
                        + "The reader should feel '被看穿了，但很好笑', not '被罵了'. ";
            case NORMAL:
            default:
                return "STYLE=NORMAL. Balance professional explanation with friendly plain language. "
                        + "Use light humor occasionally, especially when translating technical concepts, but keep most sections grounded and easy to read.";
        }
    }
}
