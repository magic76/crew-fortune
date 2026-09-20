package com.crewpocket.fortune;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FortuneYearHighlightBuilder {
    private FortuneYearHighlightBuilder() {}

    public static List<Highlight> build(FortuneMode mode, FortuneFacts facts) {
        if (mode == null || facts == null) return Collections.emptyList();
        if (mode == FortuneMode.BA_ZI) return buildBaZi(facts);
        if (mode == FortuneMode.VEDIC_ASTROLOGY) return buildVedic(facts);
        return buildTarot(facts);
    }

    private static List<Highlight> buildBaZi(FortuneFacts facts) {
        int currentYear = intValue(mapText(facts.detail("currentAnnual"), "year"));
        Set<Integer> wealthYears = integerYears(
                mapObjectValue(facts.detail("wealthProfile"), "annualSignalYears"));
        Set<Integer> careerYears = integerYears(
                mapObjectValue(facts.detail("careerProfile"), "annualSignalYears"));
        Set<Integer> relationshipYears = relationshipYears(
                mapObjectValue(facts.detail("relationshipProfile"), "annualSignalYears"));

        List<Scored> candidates = new ArrayList<Scored>();
        Object raw = facts.detail("annualTimeline");
        if (!(raw instanceof List)) return Collections.emptyList();

        for (Object itemRaw : (List<?>) raw) {
            if (!(itemRaw instanceof Map)) continue;
            Map<?, ?> item = (Map<?, ?>) itemRaw;
            int year = intValue(mapText(item, "year"));
            if (year < currentYear || year == 0) continue;

            List<String> themes = stringList(item.get("themes"));
            List<String> interactions = stringList(item.get("natalInteractions"));
            int score = 0;

            for (String theme : themes) {
                if (!theme.contains("常態推進")) score += 2;
            }
            for (String interaction : interactions) {
                if (!interaction.contains("未偵測")) score += 2;
            }
            if (wealthYears.contains(year)) score += 3;
            if (careerYears.contains(year)) score += 3;
            if (relationshipYears.contains(year)) score += 3;

            String theme = baZiTheme(
                    year,
                    wealthYears,
                    careerYears,
                    relationshipYears,
                    themes);
            String summary = firstNonEmpty(
                    mapText(item, "plainSummary"),
                    compact(themes),
                    "這一年值得和所屬大運一起看");
            String evidence = baZiEvidence(
                    item,
                    wealthYears.contains(year),
                    careerYears.contains(year),
                    relationshipYears.contains(year));

            candidates.add(new Scored(
                    score,
                    year,
                    new Highlight(
                            year,
                            theme,
                            summary,
                            evidence,
                            "請直接回答 " + year
                                    + " 年為什麼值得注意？請用 deterministic facts 說明。")));
        }

        return top(candidates, 4);
    }

    private static List<Highlight> buildVedic(FortuneFacts facts) {
        List<Highlight> out = new ArrayList<Highlight>();
        Object raw = facts.detail("importantPeriods");
        if (!(raw instanceof List)) return out;

        for (Object itemRaw : (List<?>) raw) {
            if (!(itemRaw instanceof Map)) continue;
            Map<?, ?> item = (Map<?, ?>) itemRaw;
            String startDate = mapText(item, "startDate");
            String endDate = mapText(item, "endDate");
            String md = mapText(item, "mahadashaLord");
            String ad = mapText(item, "lord");
            int year = yearFromDate(startDate);
            String label = (md.isEmpty() ? "" : md + "/") + ad;
            String summary = "Vimshottari " + label
                    + " · " + startDate + " → " + endDate;
            String evidence = "Mahadasha/Antardasha 起訖日期由出生 Moon Nakshatra 與固定 120 年 Vimshottari 規則計算；"
                    + "實際解讀需再對照該 lord 在本命的 house、sign、dignity 與所主 houses。";
            out.add(new Highlight(
                    year,
                    label.isEmpty() ? "Dasha period" : label,
                    summary,
                    evidence,
                    "請直接解釋 " + label + "（" + startDate + " 到 " + endDate
                            + "）對我代表什麼？只用 deterministic Vedic facts。"));
            if (out.size() >= 4) break;
        }
        return out;
    }

    private static List<Highlight> buildTarot(FortuneFacts facts) {
        int currentYear = intValue(facts.detailText("personalYearCalendarYear"));
        List<Scored> candidates = new ArrayList<Scored>();
        Object raw = facts.detail("personalYearTimeline");
        if (!(raw instanceof List)) return Collections.emptyList();

        for (Object itemRaw : (List<?>) raw) {
            if (!(itemRaw instanceof Map)) continue;
            Map<?, ?> item = (Map<?, ?>) itemRaw;
            int year = intValue(mapText(item, "year"));
            if (year < currentYear || year == 0) continue;

            int personalYear = intValue(mapText(item, "personalYear"));
            int score = tarotPriority(personalYear);
            if (year == currentYear) score += 1;

            String card = mapText(item, "cardName");
            String summary = firstNonEmpty(
                    mapText(item, "plainSummary"),
                    mapText(item, "keywords"),
                    "這一年有一個明確的個人流年主題");
            String theme = tarotTheme(personalYear);
            String evidence = "個人流年 " + personalYear
                    + (card.isEmpty() ? "" : "「" + card + "」")
                    + (mapText(item, "keywords").isEmpty()
                    ? ""
                    : " · 關鍵字 " + mapText(item, "keywords"));

            candidates.add(new Scored(
                    score,
                    year,
                    new Highlight(
                            year,
                            theme,
                            summary,
                            evidence,
                            "請直接回答 " + year
                                    + " 年的個人流年對我代表什麼？請用 deterministic facts 說明。")));
        }

        return top(candidates, 4);
    }

    private static String baZiTheme(
            int year,
            Set<Integer> wealth,
            Set<Integer> career,
            Set<Integer> relationship,
            List<String> themes) {
        List<String> labels = new ArrayList<String>();
        if (career.contains(year)) labels.add("工作");
        if (wealth.contains(year)) labels.add("財務");
        if (relationship.contains(year)) labels.add("感情");
        if (labels.isEmpty()) {
            for (String theme : themes) {
                if (theme.contains("變動")) labels.add("變動");
                else if (theme.contains("輸出")) labels.add("輸出");
                else if (theme.contains("學習")) labels.add("學習");
                else if (theme.contains("合作")) labels.add("合作");
                else if (theme.contains("責任")) labels.add("責任");
                if (labels.size() >= 2) break;
            }
        }
        return labels.isEmpty() ? "值得先看" : join(labels, "・");
    }

    private static String baZiEvidence(
            Map<?, ?> item,
            boolean wealth,
            boolean career,
            boolean relationship) {
        List<String> evidence = new ArrayList<String>();
        String god = mapText(item, "stemTenGod");
        String meaning = mapText(item, "stemTenGodMeaning");
        if (!god.isEmpty()) {
            evidence.add("十神 " + god + (meaning.isEmpty() ? "" : "（" + meaning + "）"));
        }
        String luck = mapText(item, "luckPillar");
        if (!luck.isEmpty()) evidence.add("所屬大運 " + luck);
        if (career) evidence.add("工作主題訊號");
        if (wealth) evidence.add("財星／資源訊號");
        if (relationship) evidence.add("感情／配偶宮訊號");

        for (String interaction : stringList(item.get("natalInteractions"))) {
            if (!interaction.contains("未偵測")) evidence.add(interaction);
            if (evidence.size() >= 5) break;
        }
        return join(evidence, "\n");
    }

    private static int tarotPriority(int personalYear) {
        if (personalYear == 1 || personalYear == 5
                || personalYear == 8 || personalYear == 9) return 5;
        if (personalYear == 6 || personalYear == 7) return 4;
        return 3;
    }

    private static String tarotTheme(int personalYear) {
        switch (personalYear) {
            case 1: return "新開始";
            case 2: return "合作・關係";
            case 3: return "表達・創作";
            case 4: return "打基礎";
            case 5: return "變動・選擇";
            case 6: return "承諾・責任";
            case 7: return "內省・研究";
            case 8: return "成果・資源";
            case 9: return "完成・收尾";
            default: return "流年主題";
        }
    }

    private static List<Highlight> top(List<Scored> candidates, int count) {
        Collections.sort(candidates, new Comparator<Scored>() {
            @Override public int compare(Scored a, Scored b) {
                int byScore = Integer.compare(b.score, a.score);
                if (byScore != 0) return byScore;
                return Integer.compare(a.year, b.year);
            }
        });
        List<Highlight> out = new ArrayList<Highlight>();
        for (Scored item : candidates) {
            if (out.size() >= count) break;
            out.add(item.highlight);
        }
        return out;
    }

    private static Set<Integer> integerYears(Object source) {
        Set<Integer> out = new LinkedHashSet<Integer>();
        if (!(source instanceof List)) return out;
        for (Object item : (List<?>) source) {
            int year = intValue(String.valueOf(item));
            if (year > 0) out.add(year);
        }
        return out;
    }

    private static Set<Integer> relationshipYears(Object source) {
        Set<Integer> out = new LinkedHashSet<Integer>();
        if (!(source instanceof List)) return out;
        for (Object item : (List<?>) source) {
            if (item instanceof Map) {
                int year = intValue(mapText(item, "year"));
                if (year > 0) out.add(year);
            }
        }
        return out;
    }

    private static Object mapObjectValue(Object source, String key) {
        if (!(source instanceof Map)) return null;
        return ((Map<?, ?>) source).get(key);
    }

    private static String mapText(Object source, String key) {
        if (!(source instanceof Map)) return "";
        Object value = ((Map<?, ?>) source).get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static List<String> stringList(Object value) {
        List<String> out = new ArrayList<String>();
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                if (item != null) out.add(String.valueOf(item));
            }
        } else if (value != null) {
            out.add(String.valueOf(value));
        }
        return out;
    }

    private static String compact(List<String> values) {
        return join(values, "、");
    }

    private static String join(List<String> values, String separator) {
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) continue;
            if (out.length() > 0) out.append(separator);
            out.append(value.trim());
        }
        return out.toString();
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    private static int yearFromDate(String value) {
        if (value == null || value.length() < 4) return 0;
        try { return Integer.parseInt(value.substring(0, 4)); }
        catch (Exception ignored) { return 0; }
    }

    private static int intValue(String value) {
        try { return Integer.parseInt(value); }
        catch (Exception ignored) { return 0; }
    }

    public static final class Highlight {
        public final int year;
        public final String theme;
        public final String summary;
        public final String evidence;
        public final String question;

        Highlight(
                int year,
                String theme,
                String summary,
                String evidence,
                String question) {
            this.year = year;
            this.theme = theme;
            this.summary = summary;
            this.evidence = evidence;
            this.question = question;
        }
    }

    private static final class Scored {
        final int score;
        final int year;
        final Highlight highlight;

        Scored(int score, int year, Highlight highlight) {
            this.score = score;
            this.year = year;
            this.highlight = highlight;
        }
    }
}
