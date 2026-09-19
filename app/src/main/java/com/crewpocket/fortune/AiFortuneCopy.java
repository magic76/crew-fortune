package com.crewpocket.fortune;

import org.json.JSONObject;

public final class AiFortuneCopy {
    public final String title;
    public final String overview;
    public final String personality;
    public final String careerWealth;
    public final String relationships;
    public final String timing;
    public final String translation;
    public final String punchline;
    public final String advice;
    public final String shareText;

    private AiFortuneCopy(String title,
                          String overview,
                          String personality,
                          String careerWealth,
                          String relationships,
                          String timing,
                          String translation,
                          String punchline,
                          String advice,
                          String shareText) {
        this.title = title;
        this.overview = overview;
        this.personality = personality;
        this.careerWealth = careerWealth;
        this.relationships = relationships;
        this.timing = timing;
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
            String overview = optional(object, "overview");
            if (overview.isEmpty()) overview = required(object, "analysis");
            String personality = optional(object, "personality");
            String careerWealth = optional(object, "careerWealth");
            String relationships = optional(object, "relationships");
            String timing = optional(object, "timing");
            String translation = required(object, "translation");
            String punchline = required(object, "punchline");
            String advice = required(object, "advice");
            String shareText = optional(object, "shareText");
            return new AiFortuneCopy(
                    title, overview, personality, careerWealth, relationships,
                    timing, translation, punchline, advice, shareText);
        } catch (Exception error) {
            throw new IllegalArgumentException("AI 回覆格式不完整", error);
        }
    }

    public String shareText(FortuneResult base) {
        if (!shareText.isEmpty()) return shareText;
        return title + "\n\n"
                + translation + "\n\n"
                + "命理師補充：" + punchline + "\n"
                + "建議：" + advice + "\n\n"
                + "很認真算，別太認真信。 #CrewFortune";
    }

    private static String required(JSONObject object, String key) {
        String value = optional(object, key);
        if (value.isEmpty()) throw new IllegalArgumentException("Missing " + key);
        return value;
    }

    private static String optional(JSONObject object, String key) {
        return object.optString(key, "").trim();
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
