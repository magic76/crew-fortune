package com.crewpocket.fortune;

public final class FortuneResult {
    public final FortuneMode mode;
    public final int score;
    public final String title;
    public final String basis;
    public final String analysis;
    public final String translation;
    public final String punchline;
    public final String advice;

    public FortuneResult(FortuneMode mode, int score, String title, String basis,
                         String analysis, String translation, String punchline, String advice) {
        this.mode = mode;
        this.score = score;
        this.title = title;
        this.basis = basis;
        this.analysis = analysis;
        this.translation = translation;
        this.punchline = punchline;
        this.advice = advice;
    }

    public String shareText() {
        return title + "\n\n"
                + "命運指數 " + score + "/100\n"
                + translation + "\n\n"
                + "命理師補充：" + punchline + "\n"
                + "今日忠告：" + advice + "\n\n"
                + "很認真算，別太認真信。 #CrewFortune";
    }
}
