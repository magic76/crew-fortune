package com.crewpocket.fortune;

import org.json.JSONObject;

public final class AiFortuneCopy {
    public final String title;
    public final String analysis;
    public final String translation;
    public final String punchline;
    public final String advice;
    public final String shareText;

    private AiFortuneCopy(String title,
                          String analysis,
                          String translation,
                          String punchline,
                          String advice,
                          String shareText) {
        this.title = title;
        this.analysis = analysis;
        this.translation = translation;
        this.punchline = punchline;
        this.advice = advice;
        this.shareText = shareText;
    }

    public static AiFortuneCopy parse(String raw) {
        String json = cleanJson(raw);
        try {
            JSONObject object = new JSONObject(json);
            String title = required(object, "title");
            String analysis = required(object, "analysis");
            String translation = required(object, "translation");
            String punchline = required(object, "punchline");
            String advice = required(object, "advice");
            String shareText = object.optString("shareText", "").trim();
            return new AiFortuneCopy(title, analysis, translation, punchline, advice, shareText);
        } catch (Exception error) {
            throw new IllegalArgumentException("AI 回覆格式不完整", error);
        }
    }

    public String shareText(FortuneResult base) {
        if (!shareText.isEmpty()) return shareText;
        return title + "\n\n"
                + "命運指數 " + base.score + "/100\n"
                + translation + "\n\n"
                + "命理師補充：" + punchline + "\n"
                + "今日忠告：" + advice + "\n\n"
                + "很認真算，別太認真信。 #CrewFortune";
    }

    private static String required(JSONObject object, String key) {
        String value = object.optString(key, "").trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Missing " + key);
        return value;
    }

    private static String cleanJson(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.startsWith("```json")) value = value.substring(7).trim();
        else if (value.startsWith("```")) value = value.substring(3).trim();
        if (value.endsWith("```")) value = value.substring(0, value.length() - 3).trim();
        int first = value.indexOf('{');
        int last = value.lastIndexOf('}');
        if (first >= 0 && last > first) value = value.substring(first, last + 1);
        return value;
    }
}
