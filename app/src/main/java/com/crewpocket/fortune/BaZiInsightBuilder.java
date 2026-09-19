package com.crewpocket.fortune;

import com.nlf.calendar.EightChar;
import com.nlf.calendar.Solar;
import com.nlf.calendar.util.LunarUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BaZiInsightBuilder {
    private BaZiInsightBuilder() {}

    public static void enrich(Map<String, Object> result, EightChar eight, int gender,
                              int birthYear, Date now) {
        Calendar c = Calendar.getInstance();
        c.setTime(now == null ? new Date() : now);
        int currentYear = c.get(Calendar.YEAR);

        enrichLuckPillars(result, eight);
        List<Map<String, Object>> years = annualTimeline(result, eight, birthYear, currentYear);
        result.put("annualTimelineStartYear", currentYear - 1);
        result.put("annualTimelineEndYear", currentYear + 15);
        result.put("annualTimeline", years);
        result.put("annualTimelineConvention",
                "逐年干支以該年年中日期取得八字流年干支；實際交界以立春節氣為準");

        result.put("wealthProfile", wealthProfile(result, years));
        result.put("careerProfile", careerProfile(result, years));
        result.put("relationshipProfile", relationshipProfile(result, eight, gender, years));
    }

    @SuppressWarnings("unchecked")
    private static void enrichLuckPillars(Map<String, Object> result, EightChar eight) {
        Object raw = result.get("luckPillars");
        if (!(raw instanceof List)) return;
        for (Object value : (List<?>) raw) {
            if (!(value instanceof Map)) continue;
            Map<String, Object> item = (Map<String, Object>) value;
            String gz = text(item.get("ganZhi"));
            if (gz.length() < 2) continue;
            String gan = gz.substring(0, 1);
            String zhi = gz.substring(1, 2);
            List<String> branchGods = branchGods(eight.getDayGan(), zhi);
            List<String> interactions = interactions(eight, zhi);
            item.put("stemTenGod", god(eight.getDayGan(), gan));
            item.put("branchTenGods", branchGods);
            item.put("hiddenStems", hidden(zhi));
            item.put("element", text(LunarUtil.WU_XING_GAN.get(gan))
                    + text(LunarUtil.WU_XING_ZHI.get(zhi)));
            item.put("interactionsWithNatal", interactions);
            List<String> itemThemes = themes(god(eight.getDayGan(), gan), branchGods, interactions);
            item.put("themes", itemThemes);
            item.put("plainSummary", plainSummary(itemThemes));
            item.put("stemTenGodMeaning", godMeaning(god(eight.getDayGan(), gan)));
        }
    }

    private static List<Map<String, Object>> annualTimeline(
            Map<String, Object> result, EightChar eight, int birthYear, int currentYear) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (int year = currentYear - 1; year <= currentYear + 15; year++) {
            String gz = Solar.fromYmd(year, 7, 1).getLunar().getYearInGanZhiExact();
            String gan = gz.substring(0, 1);
            String zhi = gz.substring(1, 2);
            String stemGod = god(eight.getDayGan(), gan);
            List<String> branchGods = branchGods(eight.getDayGan(), zhi);
            List<String> interactions = interactions(eight, zhi);

            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("year", year);
            item.put("nominalAge", year - birthYear + 1);
            item.put("ganZhi", gz);
            item.put("stemTenGod", stemGod);
            item.put("branchTenGods", branchGods);
            item.put("element", text(LunarUtil.WU_XING_GAN.get(gan))
                    + text(LunarUtil.WU_XING_ZHI.get(zhi)));
            item.put("natalInteractions", interactions);
            Map<String, Object> luck = luckForYear(result.get("luckPillars"), year);
            if (luck != null) item.put("luckPillar", luck.get("ganZhi"));
            List<String> itemThemes = themes(stemGod, branchGods, interactions);
            item.put("themes", itemThemes);
            item.put("plainSummary", plainSummary(itemThemes));
            item.put("stemTenGodMeaning", godMeaning(stemGod));
            out.add(item);
        }
        return out;
    }

    private static Map<String, Object> wealthProfile(
            Map<String, Object> result, List<Map<String, Object>> years) {
        Map<String, Object> p = new LinkedHashMap<String, Object>();
        p.put("wealthElement", controlledElement(text(result.get("dayMasterElement"))));
        p.put("directWealthCount", countGod(result.get("tenGodDistribution"), "正财", "正財"));
        p.put("indirectWealthCount", countGod(result.get("tenGodDistribution"), "偏财", "偏財"));
        p.put("natalEvidence", positions(result.get("tenGods"),
                Arrays.asList("正財", "偏財")));
        p.put("currentLuck", result.get("currentLuckPillar"));
        p.put("annualSignalYears", signalYears(years, Arrays.asList("正財", "偏財")));
        p.put("evidenceRule",
                "財運只整理財星、大運與流年啟動及合沖訊號，不等於收入、投資報酬或必然事件");
        return p;
    }

    private static Map<String, Object> careerProfile(
            Map<String, Object> result, List<Map<String, Object>> years) {
        Map<String, Object> p = new LinkedHashMap<String, Object>();
        p.put("officerCount", countGod(result.get("tenGodDistribution"), "正官")
                + countGod(result.get("tenGodDistribution"), "七杀", "七殺"));
        p.put("resourceCount", countGod(result.get("tenGodDistribution"), "正印")
                + countGod(result.get("tenGodDistribution"), "偏印"));
        p.put("outputCount", countGod(result.get("tenGodDistribution"), "食神")
                + countGod(result.get("tenGodDistribution"), "伤官", "傷官"));
        p.put("natalEvidence", positions(result.get("tenGods"),
                Arrays.asList("正官", "七殺", "正印", "偏印", "食神", "傷官")));
        p.put("currentLuck", result.get("currentLuckPillar"));
        p.put("annualSignalYears", signalYears(years,
                Arrays.asList("正官", "七殺", "正印", "偏印", "食神", "傷官")));
        p.put("evidenceRule",
                "工作主題以官殺、印、食傷及大運流年互動整理，不直接等同升職、轉職或失業預測");
        return p;
    }

    private static Map<String, Object> relationshipProfile(
            Map<String, Object> result, EightChar eight, int gender,
            List<Map<String, Object>> years) {
        Map<String, Object> p = new LinkedHashMap<String, Object>();
        String dayBranch = eight.getDayZhi();
        List<String> partnerGods = gender == 1
                ? Arrays.asList("正財", "偏財")
                : Arrays.asList("正官", "七殺");
        p.put("spousePalace", dayBranch);
        p.put("spousePalaceHiddenStems", hidden(dayBranch));
        p.put("spousePalaceTenGods", branchGods(eight.getDayGan(), dayBranch));
        p.put("partnerGodConvention", partnerGods);
        p.put("partnerGodCount", gender == 1
                ? countGod(result.get("tenGodDistribution"), "正财", "正財")
                    + countGod(result.get("tenGodDistribution"), "偏财", "偏財")
                : countGod(result.get("tenGodDistribution"), "正官")
                    + countGod(result.get("tenGodDistribution"), "七杀", "七殺"));
        p.put("annualSignalYears", relationshipYears(years, dayBranch, partnerGods));
        p.put("evidenceRule",
                "感情以日支配偶宮、常見男女財官約定及大運流年合沖整理，只作娛樂解讀");
        return p;
    }

    private static List<Integer> signalYears(
            List<Map<String, Object>> years, List<String> targetGods) {
        List<Integer> out = new ArrayList<Integer>();
        for (Map<String, Object> year : years) {
            if (hasGod(year, targetGods)) out.add(intValue(year.get("year")));
        }
        return out;
    }

    private static List<Map<String, Object>> relationshipYears(
            List<Map<String, Object>> years, String dayBranch, List<String> partnerGods) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> year : years) {
            String gz = text(year.get("ganZhi"));
            if (gz.length() < 2) continue;
            String relation = relation(dayBranch, gz.substring(1, 2));
            boolean active = hasGod(year, partnerGods);
            if (relation.isEmpty() && !active) continue;
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("year", year.get("year"));
            item.put("ganZhi", gz);
            item.put("spousePalaceInteraction", relation);
            item.put("partnerGodActive", active);
            out.add(item);
        }
        return out;
    }

    private static List<String> themes(
            String stemGod, List<String> branchGods, List<String> interactions) {
        List<String> gods = new ArrayList<String>();
        gods.add(stemGod);
        gods.addAll(branchGods);
        List<String> out = new ArrayList<String>();
        if (contains(gods, Arrays.asList("正財","偏財"))) out.add("財星／資源");
        if (contains(gods, Arrays.asList("正官","七殺"))) out.add("責任／規範");
        if (contains(gods, Arrays.asList("正印","偏印"))) out.add("學習／支援");
        if (contains(gods, Arrays.asList("食神","傷官"))) out.add("輸出／表達");
        if (contains(gods, Arrays.asList("比肩","劫財"))) out.add("自我／同儕");
        String joined = String.valueOf(interactions);
        if (joined.contains("沖")) out.add("變動");
        if (joined.contains("合")) out.add("合作／連結");
        if (joined.contains("刑") || joined.contains("害")) out.add("摩擦／調整");
        if (out.isEmpty()) out.add("常態推進");
        return out;
    }

    private static String plainSummary(List<String> themes) {
        List<String> parts = new ArrayList<String>();
        if (themes.contains("財星／資源")) parts.add("金錢、資源與現實成果議題較容易被放大");
        if (themes.contains("責任／規範")) parts.add("工作責任、制度要求或角色壓力較明顯");
        if (themes.contains("學習／支援")) parts.add("學習、資格、資源支援與整理能力較重要");
        if (themes.contains("輸出／表達")) parts.add("輸出、表達、作品與把想法做出來的需求增加");
        if (themes.contains("自我／同儕")) parts.add("自主性、競爭、合作分工與同儕關係更值得注意");
        if (themes.contains("變動")) parts.add("合沖訊號帶來較強的調整與變動感");
        if (themes.contains("合作／連結")) parts.add("合作、關係連結或資源整合機會增加");
        if (themes.contains("摩擦／調整")) parts.add("容易出現卡點，適合提早調整節奏與界線");
        if (parts.isEmpty()) return "這段以穩定推進為主，沒有特別突出的主題訊號";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.size() && i < 3; i++) {
            if (out.length() > 0) out.append("；");
            out.append(parts.get(i));
        }
        return out.toString();
    }

    private static String godMeaning(String god) {
        String g = trad(god);
        if ("正財".equals(g)) return "穩定收入、資源管理、現實責任";
        if ("偏財".equals(g)) return "機會型資源、人脈、彈性收入";
        if ("正官".equals(g)) return "責任、規範、職位與制度";
        if ("七殺".equals(g)) return "壓力、競爭、決斷與高要求";
        if ("正印".equals(g)) return "學習、支援、資格與保護";
        if ("偏印".equals(g)) return "研究、洞察、非典型學習";
        if ("食神".equals(g)) return "穩定輸出、創造、享受與表達";
        if ("傷官".equals(g)) return "強表達、突破、質疑與創新";
        if ("比肩".equals(g)) return "自主、同儕、競爭與自我主張";
        if ("劫財".equals(g)) return "合作競爭、資源分配與人際拉扯";
        return "";
    }

    private static List<String> branchGods(String dayGan, String branch) {
        List<String> out = new ArrayList<String>();
        for (String gan : hidden(branch)) out.add(god(dayGan, gan));
        return out;
    }

    private static List<String> hidden(String branch) {
        List<String> source = LunarUtil.ZHI_HIDE_GAN.get(branch);
        return source == null ? new ArrayList<String>() : new ArrayList<String>(source);
    }

    private static String god(String dayGan, String targetGan) {
        return trad(text(LunarUtil.SHI_SHEN.get(dayGan + targetGan)));
    }

    private static List<String> interactions(EightChar e, String target) {
        String[] labels = {"年支","月支","日支","時支"};
        String[] natal = {e.getYearZhi(),e.getMonthZhi(),e.getDayZhi(),e.getTimeZhi()};
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < natal.length; i++) {
            String r = relation(natal[i], target);
            if (!r.isEmpty()) out.add(labels[i] + natal[i] + " ↔ " + target + " " + r);
        }
        if (out.isEmpty()) out.add("與本命四支未偵測到主要六合、六沖、六害或相刑");
        return out;
    }

    private static String relation(String a, String b) {
        String[][] pairs = {
                {"子","丑","六合"},{"寅","亥","六合"},{"卯","戌","六合"},{"辰","酉","六合"},{"巳","申","六合"},{"午","未","六合"},
                {"子","午","六沖"},{"丑","未","六沖"},{"寅","申","六沖"},{"卯","酉","六沖"},{"辰","戌","六沖"},{"巳","亥","六沖"},
                {"子","未","六害"},{"丑","午","六害"},{"寅","巳","六害"},{"卯","辰","六害"},{"申","亥","六害"},{"酉","戌","六害"},
                {"子","卯","相刑"}
        };
        for (String[] p : pairs) {
            if ((p[0].equals(a) && p[1].equals(b)) || (p[1].equals(a) && p[0].equals(b))) return p[2];
        }
        if (a.equals(b) && Arrays.asList("辰","午","酉","亥").contains(a)) return "自刑";
        return "";
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> luckForYear(Object source, int year) {
        if (!(source instanceof List)) return null;
        for (Object raw : (List<?>) source) {
            if (!(raw instanceof Map)) continue;
            Map<String, Object> item = (Map<String, Object>) raw;
            if (year >= intValue(item.get("startYear")) && year <= intValue(item.get("endYear"))) return item;
        }
        return null;
    }

    private static List<String> positions(Object source, List<String> targets) {
        List<String> out = new ArrayList<String>();
        if (!(source instanceof Map)) return out;
        Map<?, ?> map = (Map<?, ?>) source;
        for (String key : Arrays.asList("yearStem","monthStem","timeStem","yearBranch","monthBranch","dayBranch","timeBranch")) {
            Object value = map.get(key);
            if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    if (contains(Arrays.asList(trad(text(item))), targets)) out.add(key + "：" + trad(text(item)));
                }
            } else if (contains(Arrays.asList(trad(text(value))), targets)) {
                out.add(key + "：" + trad(text(value)));
            }
        }
        return out;
    }

    private static boolean hasGod(Map<String, Object> year, List<String> targets) {
        List<String> gods = new ArrayList<String>();
        gods.add(trad(text(year.get("stemTenGod"))));
        Object branch = year.get("branchTenGods");
        if (branch instanceof List) for (Object g : (List<?>) branch) gods.add(trad(text(g)));
        return contains(gods, targets);
    }

    private static boolean contains(List<String> values, List<String> targets) {
        for (String value : values) for (String target : targets) if (trad(value).equals(trad(target))) return true;
        return false;
    }

    private static int countGod(Object source, String... names) {
        if (!(source instanceof Map)) return 0;
        int total = 0;
        Map<?, ?> map = (Map<?, ?>) source;
        for (String name : names) {
            Object value = map.get(name);
            if (value instanceof Number) total += ((Number) value).intValue();
        }
        return total;
    }

    private static String controlledElement(String e) {
        if ("木".equals(e)) return "土";
        if ("火".equals(e)) return "金";
        if ("土".equals(e)) return "水";
        if ("金".equals(e)) return "木";
        if ("水".equals(e)) return "火";
        return "";
    }

    private static String trad(String value) {
        return value.replace("劫财","劫財").replace("伤官","傷官")
                .replace("正财","正財").replace("偏财","偏財").replace("七杀","七殺");
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int intValue(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(text(value)); } catch (Exception ignored) { return 0; }
    }
}
