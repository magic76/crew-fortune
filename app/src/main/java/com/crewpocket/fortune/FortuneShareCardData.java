package com.crewpocket.fortune;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class FortuneShareCardData {
    public final FortuneMode mode;
    public final String modeLabel;
    public final String coreTitle;
    public final String coreValue;
    public final String cycleTitle;
    public final String cycleValue;
    public final String yearsTitle;
    public final List<String> years;
    public final String quote;

    private FortuneShareCardData(
            FortuneMode mode,
            String modeLabel,
            String coreTitle,
            String coreValue,
            String cycleTitle,
            String cycleValue,
            String yearsTitle,
            List<String> years,
            String quote) {
        this.mode = mode;
        this.modeLabel = modeLabel;
        this.coreTitle = coreTitle;
        this.coreValue = coreValue;
        this.cycleTitle = cycleTitle;
        this.cycleValue = cycleValue;
        this.yearsTitle = yearsTitle;
        this.years = Collections.unmodifiableList(new ArrayList<String>(years));
        this.quote = quote;
    }

    public static FortuneShareCardData from(
            FortuneMode mode,
            FortuneFacts facts,
            FortuneResult result,
            AiFortuneCopy aiCopy,
            String displayName) {
        if (mode == null || facts == null || result == null) {
            throw new IllegalArgumentException("share card requires result facts");
        }
        return mode == FortuneMode.BA_ZI
                ? fromBaZi(facts, result, aiCopy, displayName)
                : fromTarot(facts, result, aiCopy, displayName);
    }

    private static FortuneShareCardData fromBaZi(
            FortuneFacts facts,
            FortuneResult result,
            AiFortuneCopy aiCopy,
            String displayName) {
        String dayMaster = facts.detailText("dayMaster");
        String dayElement = facts.detailText("dayMasterElement");
        String strength = facts.detailText("dayMasterStrength");
        String core = joinNonEmpty(" · ",
                dayMaster + dayElement,
                strength);

        Object luck = facts.detail("currentLuckPillar");
        String luckTitle = mapText(luck, "ganZhi");
        String startYear = mapText(luck, "startYear");
        String endYear = mapText(luck, "endYear");
        if (!startYear.isEmpty() && !endYear.isEmpty()) {
            luckTitle += "大運 · " + startYear + "–" + endYear;
        } else if (!luckTitle.isEmpty()) {
            luckTitle += "大運";
        }
        String luckSummary = firstNonEmpty(
                mapText(luck, "plainSummary"),
                facts.detailText("balancingElements"),
                "目前以大運與逐年流年一起觀察");

        int currentYear = intValue(mapText(facts.detail("currentAnnual"), "year"));
        List<String> years = baZiUpcomingYears(
                facts.detail("annualTimeline"), currentYear, 3);

        return new FortuneShareCardData(
                FortuneMode.BA_ZI,
                "八字",
                "本命核心",
                core,
                "目前大運",
                joinLines(luckTitle, luckSummary),
                "接下來三年",
                years,
                safeQuote(aiCopy, result, displayName));
    }

    private static FortuneShareCardData fromTarot(
            FortuneFacts facts,
            FortuneResult result,
            AiFortuneCopy aiCopy,
            String displayName) {
        String personality = facts.detailText("personalityCardNumber")
                + " " + facts.detailText("personalityCardName");
        String soul = facts.detailText("soulCardNumber")
                + " " + facts.detailText("soulCardName");
        String lifePath = facts.detailText("lifePathDisplay");

        String core = "外在人格｜" + personality.trim()
                + "\n內在靈魂｜" + soul.trim()
                + "\n生命道路｜" + lifePath;

        int currentYear = intValue(facts.detailText("personalYearCalendarYear"));
        String currentSummary = "";
        Object timeline = facts.detail("personalYearTimeline");
        if (timeline instanceof List) {
            for (Object raw : (List<?>) timeline) {
                if (!(raw instanceof Map)) continue;
                Map<?, ?> item = (Map<?, ?>) raw;
                if (intValue(mapText(item, "year")) == currentYear) {
                    currentSummary = mapText(item, "plainSummary");
                    break;
                }
            }
        }

        String cycle = currentYear
                + "｜流年 " + facts.detailText("personalYear")
                + "「" + facts.detailText("personalYearCardName") + "」";

        List<String> years = tarotUpcomingYears(timeline, currentYear, 3);

        return new FortuneShareCardData(
                FortuneMode.TAROT_NUMEROLOGY,
                "塔羅生命靈數",
                "你的本命",
                core,
                "目前流年",
                joinLines(cycle, currentSummary),
                "接下來三年",
                years,
                safeQuote(aiCopy, result, displayName));
    }

    private static List<String> baZiUpcomingYears(
            Object source, int currentYear, int count) {
        List<YearItem> candidates = new ArrayList<YearItem>();
        if (source instanceof List) {
            for (Object raw : (List<?>) source) {
                if (!(raw instanceof Map)) continue;
                Map<?, ?> item = (Map<?, ?>) raw;
                int year = intValue(mapText(item, "year"));
                if (year <= currentYear) continue;
                String ganZhi = mapText(item, "ganZhi");
                String summary = mapText(item, "plainSummary");
                if (summary.isEmpty()) summary = compactList(item.get("themes"));
                candidates.add(new YearItem(
                        year,
                        year + " " + ganZhi + "｜" + truncate(summary, 46)));
            }
        }
        return firstYears(candidates, count);
    }

    private static List<String> tarotUpcomingYears(
            Object source, int currentYear, int count) {
        List<YearItem> candidates = new ArrayList<YearItem>();
        if (source instanceof List) {
            for (Object raw : (List<?>) source) {
                if (!(raw instanceof Map)) continue;
                Map<?, ?> item = (Map<?, ?>) raw;
                int year = intValue(mapText(item, "year"));
                if (year <= currentYear) continue;
                String summary = mapText(item, "plainSummary");
                String value = year
                        + "｜" + mapText(item, "personalYear")
                        + "「" + mapText(item, "cardName") + "」"
                        + " · " + truncate(summary, 38);
                candidates.add(new YearItem(year, value));
            }
        }
        return firstYears(candidates, count);
    }

    private static List<String> firstYears(List<YearItem> source, int count) {
        Collections.sort(source, new Comparator<YearItem>() {
            @Override public int compare(YearItem a, YearItem b) {
                return Integer.compare(a.year, b.year);
            }
        });
        List<String> out = new ArrayList<String>();
        for (YearItem item : source) {
            if (out.size() >= count) break;
            out.add(item.text);
        }
        if (out.isEmpty()) out.add("目前沒有可分享的後續年度資料");
        return out;
    }

    private static String safeQuote(
            AiFortuneCopy aiCopy,
            FortuneResult result,
            String displayName) {
        String value = aiCopy != null
                ? firstNonEmpty(aiCopy.punchline, aiCopy.translation)
                : firstNonEmpty(result.punchline, result.translation);
        if (value.isEmpty()) {
            value = "把命理當成觀察自己的另一個角度，不要當成人生遙控器。";
        }
        value = value.replace('\n', ' ').replace('\r', ' ')
                .replaceAll("\\d{4}-\\d{2}-\\d{2}", "[日期]")
                .replaceAll("(?<!\\d)\\d{1,2}:\\d{2}(?!\\d)", "[時間]")
                .replaceAll("\\s+", " ")
                .trim();
        if (displayName != null && !displayName.trim().isEmpty()) {
            value = value.replace(displayName.trim(), "你");
        }
        return truncate(value, 92);
    }

    private static String mapText(Object source, String key) {
        if (!(source instanceof Map)) return "";
        Object value = ((Map<?, ?>) source).get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static String compactList(Object value) {
        if (!(value instanceof List)) return value == null ? "" : String.valueOf(value);
        StringBuilder out = new StringBuilder();
        for (Object item : (List<?>) value) {
            if (out.length() > 0) out.append("、");
            out.append(String.valueOf(item));
        }
        return out.toString();
    }

    private static int intValue(String value) {
        try { return Integer.parseInt(value); }
        catch (Exception ignored) { return 0; }
    }

    private static String truncate(String value, int max) {
        String clean = value == null ? "" : value.trim();
        return clean.length() <= max ? clean : clean.substring(0, max) + "…";
    }

    private static String joinLines(String first, String second) {
        if (first == null || first.trim().isEmpty()) return second == null ? "" : second.trim();
        if (second == null || second.trim().isEmpty()) return first.trim();
        return first.trim() + "\n" + second.trim();
    }

    private static String joinNonEmpty(String separator, String... values) {
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

    String allTextForTest() {
        return modeLabel + "\n"
                + coreTitle + "\n" + coreValue + "\n"
                + cycleTitle + "\n" + cycleValue + "\n"
                + yearsTitle + "\n" + years + "\n"
                + quote;
    }

    private static final class YearItem {
        final int year;
        final String text;

        YearItem(int year, String text) {
            this.year = year;
            this.text = text;
        }
    }
}
