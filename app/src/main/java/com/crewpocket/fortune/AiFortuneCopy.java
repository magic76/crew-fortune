package com.crewpocket.fortune;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AiFortuneCopy {
    public final String title;
    public final String overview;
    public final String personality;
    public final String career;
    public final String wealth;
    public final String careerWealth;
    public final String relationships;
    public final String family;
    public final String currentCycle;
    public final String longTerm;
    public final String keyYears;
    public final String timing;
    public final List<String> topTraits;
    public final List<String> topTraitEvidence;
    public final List<String> followUps;
    public final String translation;
    public final String punchline;
    public final String advice;
    public final String shareText;

    private AiFortuneCopy(
            String title,
            String overview,
            String personality,
            String career,
            String wealth,
            String relationships,
            String family,
            String currentCycle,
            String longTerm,
            String keyYears,
            List<String> topTraits,
            List<String> topTraitEvidence,
            List<String> followUps,
            String translation,
            String punchline,
            String advice,
            String shareText) {
        this.title = title;
        this.overview = overview;
        this.personality = personality;
        this.career = career;
        this.wealth = wealth;
        this.careerWealth = combineSections("工作", career, "財運", wealth);
        this.relationships = relationships;
        this.family = family;
        this.currentCycle = currentCycle;
        this.longTerm = longTerm;
        this.keyYears = keyYears;
        this.timing = combineTiming(currentCycle, longTerm, keyYears);
        this.topTraits = immutable(topTraits);
        this.topTraitEvidence = immutable(topTraitEvidence);
        this.followUps = immutable(followUps);
        this.translation = translation;
        this.punchline = punchline;
        this.advice = advice;
        this.shareText = shareText;
    }

    public static AiFortuneCopy parse(String raw) {
        final JSONObject object;
        try {
            object = new JSONObject(cleanJson(raw));
        } catch (JSONException error) {
            throw new IllegalArgumentException(
                    "Invalid JSON: " + safeMessage(error.getMessage()), error);
        }

        String title = firstNonEmpty(
                optional(object, "title"),
                optional(object, "headline"));
        if (title.isEmpty()) {
            throw new IllegalArgumentException("Missing core field: title");
        }

        String overview = firstNonEmpty(
                optional(object, "overview"),
                optional(object, "analysis"),
                optional(object, "summary"));
        if (overview.isEmpty()) {
            throw new IllegalArgumentException("Missing core field: overview/analysis");
        }

        String personality = firstNonEmpty(
                optional(object, "personality"),
                optional(object, "personalityTalents"),
                optional(object, "traits"));

        String legacyCareerWealth = optional(object, "careerWealth");
        String career = firstNonEmpty(
                optional(object, "career"),
                optional(object, "work"),
                legacyCareerWealth);
        String wealth = firstNonEmpty(
                optional(object, "wealth"),
                optional(object, "finance"),
                legacyCareerWealth);

        String relationships = firstNonEmpty(
                optional(object, "relationships"),
                optional(object, "relationship"),
                optional(object, "love"));

        String family = firstNonEmpty(
                optional(object, "family"),
                optional(object, "familyChildren"),
                optional(object, "children"));

        String legacyTiming = optional(object, "timing");
        String currentCycle = firstNonEmpty(
                optional(object, "currentCycle"),
                optional(object, "currentTiming"),
                legacyTiming);
        String longTerm = firstNonEmpty(
                optional(object, "longTerm"),
                optional(object, "decade"),
                optional(object, "futureTrend"));
        String keyYears = firstNonEmpty(
                optional(object, "keyYears"),
                optional(object, "importantYears"),
                optional(object, "yearHighlights"));

        List<String> topTraits = firstNonEmptyList(
                optionalList(object, "topTraits"),
                optionalList(object, "topInsights"),
                optionalList(object, "mostLikeYou"));
        List<String> topTraitEvidence = firstNonEmptyList(
                optionalList(object, "topTraitEvidence"),
                optionalList(object, "traitEvidence"),
                optionalList(object, "topInsightsEvidence"));
        List<String> followUps = firstNonEmptyList(
                optionalList(object, "followUps"),
                optionalList(object, "suggestedQuestions"),
                optionalList(object, "questions"));

        String translation = firstNonEmpty(
                optional(object, "translation"),
                optional(object, "plainLanguage"),
                overview);
        String punchline = firstNonEmpty(
                optional(object, "punchline"),
                optional(object, "oneLiner"),
                "重點不是被一句命理定義，而是看哪些具體訊號真的對得上你的生活。");
        String advice = firstNonEmpty(
                optional(object, "advice"),
                optional(object, "suggestions"),
                "把這份解讀當成觀察方向；重要決策仍以實際資訊、風險與自己的判斷為準。");
        String shareText = firstNonEmpty(
                optional(object, "shareText"),
                optional(object, "share"));

        return new AiFortuneCopy(
                title,
                overview,
                personality,
                career,
                wealth,
                relationships,
                family,
                currentCycle,
                longTerm,
                keyYears,
                topTraits,
                topTraitEvidence,
                followUps,
                translation,
                punchline,
                advice,
                shareText);
    }

    public String qualityIssueSummary(FortuneMode mode) {
        List<String> issues = new ArrayList<String>();
        requireLength(issues, "overview", overview, 180);
        requireLength(issues, "personality", personality, 200);
        requireLength(issues, "career", career, 180);
        requireLength(issues, "wealth", wealth, 180);
        requireLength(issues, "relationships", relationships, 180);
        if (mode == FortuneMode.VEDIC_ASTROLOGY) {
            requireLength(issues, "family", family, 180);
        }
        requireLength(issues, "currentCycle", currentCycle, 180);
        requireLength(issues, "longTerm", longTerm,
                mode == FortuneMode.BA_ZI ? 320
                        : mode == FortuneMode.VEDIC_ASTROLOGY ? 300 : 260);
        requireLength(issues, "keyYears", keyYears, 220);
        requireListSize(issues, "topTraits", topTraits, 3);
        requireListSize(issues, "topTraitEvidence", topTraitEvidence, 3);
        requireListSize(issues, "followUps", followUps, 4);
        requireLength(issues, "advice", advice, 100);
        return join(issues, ", ");
    }

    public String shareText(FortuneResult base) {
        if (!shareText.isEmpty()) return shareText;
        return title + "\n\n"
                + translation + "\n\n"
                + "命理師補充：" + punchline + "\n"
                + "建議：" + advice + "\n\n"
                + "很認真算，別太認真信。 #CrewFortune";
    }

    static String cleanJson(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.startsWith("\uFEFF")) value = value.substring(1).trim();

        int start = value.indexOf('{');
        if (start < 0) {
            throw new IllegalArgumentException("No JSON object found");
        }

        boolean inString = false;
        boolean escaped = false;
        int depth = 0;
        for (int i = start; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }

            if (ch == '"') {
                inString = true;
            } else if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return value.substring(start, i + 1).trim();
                }
            }
        }

        throw new IllegalArgumentException("Truncated JSON object");
    }

    private static String optional(JSONObject object, String key) {
        Object raw = object.opt(key);
        if (raw == null || raw == JSONObject.NULL) return "";
        return String.valueOf(raw).trim();
    }

    private static List<String> optionalList(JSONObject object, String key) {
        Object raw = object.opt(key);
        List<String> out = new ArrayList<String>();
        if (raw == null || raw == JSONObject.NULL) return out;

        if (raw instanceof JSONArray) {
            JSONArray array = (JSONArray) raw;
            for (int i = 0; i < array.length(); i++) {
                String value = String.valueOf(array.opt(i)).trim();
                if (!value.isEmpty() && !"null".equalsIgnoreCase(value)) out.add(value);
            }
            return out;
        }

        String text = String.valueOf(raw).trim();
        if (text.isEmpty()) return out;
        String[] parts = text.split("\\r?\\n|\\s*\\|\\s*");
        for (String part : parts) {
            String value = part.replaceFirst("^[•·\\-*\\d.、)）(（\\s]+", "").trim();
            if (!value.isEmpty()) out.add(value);
        }
        return out;
    }

    @SafeVarargs
    private static List<String> firstNonEmptyList(List<String>... values) {
        if (values == null) return new ArrayList<String>();
        for (List<String> value : values) {
            if (value != null && !value.isEmpty()) return value;
        }
        return new ArrayList<String>();
    }

    private static List<String> immutable(List<String> values) {
        if (values == null || values.isEmpty()) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<String>(values));
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    private static String combineSections(
            String firstLabel, String first,
            String secondLabel, String second) {
        if (first.isEmpty()) return second;
        if (second.isEmpty()) return first;
        if (first.equals(second)) return first;
        return firstLabel + "｜" + first + "\n\n"
                + secondLabel + "｜" + second;
    }

    private static String combineTiming(String currentCycle, String longTerm, String keyYears) {
        StringBuilder out = new StringBuilder();
        appendSection(out, "目前週期", currentCycle);
        appendSection(out, "長期走勢", longTerm);
        appendSection(out, "重要年份", keyYears);
        return out.toString();
    }

    private static void appendSection(StringBuilder out, String label, String value) {
        if (value == null || value.trim().isEmpty()) return;
        if (out.length() > 0) out.append("\n\n");
        out.append(label).append("｜").append(value.trim());
    }

    private static void requireLength(
            List<String> issues, String field, String value, int minimum) {
        int length = value == null ? 0 : value.trim().length();
        if (length < minimum) {
            issues.add(field + "=" + length + "<" + minimum);
        }
    }

    private static void requireListSize(
            List<String> issues, String field, List<String> value, int minimum) {
        int size = value == null ? 0 : value.size();
        if (size < minimum) {
            issues.add(field + "=" + size + "<" + minimum);
        }
    }

    private static String join(List<String> values, String separator) {
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            if (out.length() > 0) out.append(separator);
            out.append(value);
        }
        return out.toString();
    }

    private static String safeMessage(String value) {
        if (value == null || value.trim().isEmpty()) return "unknown JSON error";
        String clean = value.replace('\n', ' ').replace('\r', ' ').trim();
        return clean.length() <= 180 ? clean : clean.substring(0, 180) + "…";
    }
}
