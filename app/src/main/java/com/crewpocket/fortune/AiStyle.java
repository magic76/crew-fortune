package com.crewpocket.fortune;

public enum AiStyle {
    STRICT("嚴謹", 0.55),
    NORMAL("普通", 0.82),
    FUNNY("風趣", 1.05);

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
                return "STYLE=FUNNY. Keep every factual claim anchored to the tool output, but make the entire report entertaining. "
                        + "Use sharp observational humor, unexpected but understandable metaphors, relatable work/family/social-life comparisons, and occasional concise tech-style analogies. "
                        + "Each major section should contain at least one memorable funny line or image, not only translation/punchline. "
                        + "Do not become random, cruel, insulting, vulgar, mystical-for-the-sake-of-it, or turn the report into stand-up comedy. "
                        + "The reader should think: 'this is annoyingly specific and funny.'";
            case NORMAL:
            default:
                return "STYLE=NORMAL. Balance professional explanation with friendly plain language. "
                        + "Use light humor occasionally, especially when translating technical concepts, but keep most sections grounded and easy to read.";
        }
    }
}
