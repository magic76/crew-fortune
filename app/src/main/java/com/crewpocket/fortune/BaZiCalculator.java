package com.crewpocket.fortune;

import com.nlf.calendar.EightChar;
import com.nlf.calendar.Solar;
import com.nlf.calendar.eightchar.DaYun;
import com.nlf.calendar.eightchar.LiuNian;
import com.nlf.calendar.eightchar.Yun;
import com.nlf.calendar.util.LunarUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BaZiCalculator {
    public static final String METHOD_VERSION = "bazi-mainstream-report-v2-lunar-java-1.7.7-sect2";

    private static final String[] ELEMENTS = {"木", "火", "土", "金", "水"};
    private static final String[] GENERATES = {"火", "土", "金", "水", "木"};
    private static final String[] CONTROLS = {"土", "金", "水", "木", "火"};

    public Map<String, Object> calculate(String birthDate, String birthTime, int gender, Date now) {
        int[] date = parseDate(birthDate);
        int[] time = parseTime(birthTime);

        Solar solar = Solar.fromYmdHms(date[0], date[1], date[2], time[0], time[1], 0);
        EightChar eight = solar.getLunar().getEightChar();
        eight.setSect(2);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("methodVersion", METHOD_VERSION);
        result.put("timeConvention", "出生地當地民用時間；晚子時採 sect=2（23:00 不換日）；目前未做真太陽時校正");
        result.put("gender", gender == 1 ? "男" : "女");
        result.put("yearPillar", eight.getYear());
        result.put("monthPillar", eight.getMonth());
        result.put("dayPillar", eight.getDay());
        result.put("timePillar", eight.getTime());
        result.put("fourPillars", eight.toString());
        result.put("dayMaster", eight.getDayGan());
        result.put("dayMasterElement", LunarUtil.WU_XING_GAN.get(eight.getDayGan()));

        Map<String, Integer> visible = visibleElements(eight);
        result.put("visibleFiveElements", visible);
        result.put("strongestVisibleElement", extremeInt(visible, true));
        result.put("weakestVisibleElement", extremeInt(visible, false));

        Map<String, Object> hidden = hiddenStems(eight);
        result.put("hiddenStems", hidden);

        Map<String, Object> tenGods = tenGods(eight);
        result.put("tenGods", tenGods);
        result.put("tenGodDistribution", tenGodDistribution(eight));

        Map<String, Double> weighted = weightedElements(eight);
        result.put("weightedFiveElements", roundMap(weighted));

        String dayElement = LunarUtil.WU_XING_GAN.get(eight.getDayGan());
        double supportRatio = supportRatio(weighted, dayElement);
        int strengthIndex = (int) Math.round(supportRatio * 100d);
        result.put("strengthIndex", strengthIndex);
        result.put("dayMasterStrength", strengthLabel(strengthIndex));
        result.put("strengthMethod",
                "簡化旺衰模型：以天干、藏干與月令加權估算扶身比例；用於娛樂型解讀，不等同門派格局或正式喜用神判定");
        result.put("balancingElements", balancingElements(dayElement, strengthIndex));

        result.put("natalInteractions", natalInteractions(eight));
        result.put("naYin", Arrays.asList(
                eight.getYearNaYin(), eight.getMonthNaYin(), eight.getDayNaYin(), eight.getTimeNaYin()));
        result.put("lifeStages", Arrays.asList(
                eight.getYearDiShi(), eight.getMonthDiShi(), eight.getDayDiShi(), eight.getTimeDiShi()));
        result.put("mingGong", eight.getMingGong());
        result.put("shenGong", eight.getShenGong());

        addLuckCycles(result, eight, gender, date[0], now == null ? new Date() : now);
        return result;
    }

    private static Map<String, Integer> visibleElements(EightChar e) {
        Map<String, Integer> map = intElementMap();
        addInt(map, LunarUtil.WU_XING_GAN.get(e.getYearGan()));
        addInt(map, LunarUtil.WU_XING_ZHI.get(e.getYearZhi()));
        addInt(map, LunarUtil.WU_XING_GAN.get(e.getMonthGan()));
        addInt(map, LunarUtil.WU_XING_ZHI.get(e.getMonthZhi()));
        addInt(map, LunarUtil.WU_XING_GAN.get(e.getDayGan()));
        addInt(map, LunarUtil.WU_XING_ZHI.get(e.getDayZhi()));
        addInt(map, LunarUtil.WU_XING_GAN.get(e.getTimeGan()));
        addInt(map, LunarUtil.WU_XING_ZHI.get(e.getTimeZhi()));
        return map;
    }

    private static Map<String, Object> hiddenStems(EightChar e) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("year", new ArrayList<String>(e.getYearHideGan()));
        map.put("month", new ArrayList<String>(e.getMonthHideGan()));
        map.put("day", new ArrayList<String>(e.getDayHideGan()));
        map.put("time", new ArrayList<String>(e.getTimeHideGan()));
        return map;
    }

    private static Map<String, Object> tenGods(EightChar e) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("yearStem", e.getYearShiShenGan());
        map.put("monthStem", e.getMonthShiShenGan());
        map.put("dayStem", "日主");
        map.put("timeStem", e.getTimeShiShenGan());
        map.put("yearBranch", new ArrayList<String>(e.getYearShiShenZhi()));
        map.put("monthBranch", new ArrayList<String>(e.getMonthShiShenZhi()));
        map.put("dayBranch", new ArrayList<String>(e.getDayShiShenZhi()));
        map.put("timeBranch", new ArrayList<String>(e.getTimeShiShenZhi()));
        return map;
    }

    private static Map<String, Integer> tenGodDistribution(EightChar e) {
        Map<String, Integer> map = new LinkedHashMap<String, Integer>();
        for (String key : Arrays.asList("比肩","劫财","食神","伤官","正财","偏财","正官","七杀","正印","偏印")) {
            map.put(key, 0);
        }
        addGod(map, e.getYearShiShenGan());
        addGod(map, e.getMonthShiShenGan());
        addGod(map, e.getTimeShiShenGan());
        for (String g : e.getYearShiShenZhi()) addGod(map, g);
        for (String g : e.getMonthShiShenZhi()) addGod(map, g);
        for (String g : e.getDayShiShenZhi()) addGod(map, g);
        for (String g : e.getTimeShiShenZhi()) addGod(map, g);
        return map;
    }

    private static Map<String, Double> weightedElements(EightChar e) {
        Map<String, Double> map = doubleElementMap();
        addDouble(map, LunarUtil.WU_XING_GAN.get(e.getYearGan()), 1.0);
        addDouble(map, LunarUtil.WU_XING_GAN.get(e.getMonthGan()), 1.2);
        addDouble(map, LunarUtil.WU_XING_GAN.get(e.getDayGan()), 1.0);
        addDouble(map, LunarUtil.WU_XING_GAN.get(e.getTimeGan()), 1.0);
        addHidden(map, e.getYearHideGan(), 1.0);
        addHidden(map, e.getMonthHideGan(), 2.0);
        addHidden(map, e.getDayHideGan(), 1.0);
        addHidden(map, e.getTimeHideGan(), 1.0);
        return map;
    }

    private static void addHidden(Map<String, Double> map, List<String> stems, double weight) {
        if (stems == null || stems.isEmpty()) return;
        double[] ratios = stems.size() == 1
                ? new double[]{1.0}
                : stems.size() == 2 ? new double[]{0.7, 0.3} : new double[]{0.6, 0.3, 0.1};
        for (int i = 0; i < stems.size(); i++) {
            addDouble(map, LunarUtil.WU_XING_GAN.get(stems.get(i)), weight * ratios[Math.min(i, ratios.length - 1)]);
        }
    }

    private static double supportRatio(Map<String, Double> energies, String dayElement) {
        int index = elementIndex(dayElement);
        if (index < 0) return 0.5;
        String resource = ELEMENTS[(index + 4) % 5];
        double total = 0d;
        double support = 0d;
        for (Map.Entry<String, Double> entry : energies.entrySet()) {
            total += entry.getValue();
            if (dayElement.equals(entry.getKey()) || resource.equals(entry.getKey())) support += entry.getValue();
        }
        return total <= 0 ? 0.5 : support / total;
    }

    private static String strengthLabel(int index) {
        if (index >= 70) return "偏旺";
        if (index >= 58) return "身強";
        if (index >= 43) return "中和";
        if (index >= 30) return "身弱";
        return "偏弱";
    }

    private static List<String> balancingElements(String dayElement, int strength) {
        int i = elementIndex(dayElement);
        if (i < 0) return new ArrayList<String>();
        String resource = ELEMENTS[(i + 4) % 5];
        String output = GENERATES[i];
        String wealth = CONTROLS[i];
        String officer = ELEMENTS[(i + 2) % 5];
        if (strength < 43) return Arrays.asList(resource, dayElement);
        if (strength > 57) return Arrays.asList(output, wealth, officer);
        return Arrays.asList(output, resource);
    }

    private static List<String> natalInteractions(EightChar e) {
        String[] stems = {e.getYearGan(), e.getMonthGan(), e.getDayGan(), e.getTimeGan()};
        String[] branches = {e.getYearZhi(), e.getMonthZhi(), e.getDayZhi(), e.getTimeZhi()};
        List<String> out = new ArrayList<String>();

        addPairRelations(out, stems, new String[][]{
                {"甲","己","天干五合"},{"乙","庚","天干五合"},{"丙","辛","天干五合"},{"丁","壬","天干五合"},{"戊","癸","天干五合"}
        });
        addPairRelations(out, branches, new String[][]{
                {"子","丑","六合"},{"寅","亥","六合"},{"卯","戌","六合"},{"辰","酉","六合"},{"巳","申","六合"},{"午","未","六合"},
                {"子","午","六沖"},{"丑","未","六沖"},{"寅","申","六沖"},{"卯","酉","六沖"},{"辰","戌","六沖"},{"巳","亥","六沖"},
                {"子","未","六害"},{"丑","午","六害"},{"寅","巳","六害"},{"卯","辰","六害"},{"申","亥","六害"},{"酉","戌","六害"},
                {"子","卯","相刑"}
        });

        addTriple(out, branches, new String[]{"申","子","辰"}, "申子辰三合水局");
        addTriple(out, branches, new String[]{"亥","卯","未"}, "亥卯未三合木局");
        addTriple(out, branches, new String[]{"寅","午","戌"}, "寅午戌三合火局");
        addTriple(out, branches, new String[]{"巳","酉","丑"}, "巳酉丑三合金局");
        addTriple(out, branches, new String[]{"寅","巳","申"}, "寅巳申三刑");
        addTriple(out, branches, new String[]{"丑","未","戌"}, "丑未戌三刑");

        for (String z : new String[]{"辰","午","酉","亥"}) {
            int count = 0;
            for (String b : branches) if (z.equals(b)) count++;
            if (count >= 2) out.add(z + z + "自刑");
        }
        if (out.isEmpty()) out.add("命局內未偵測到主要六合、六沖、六害或三合／三刑組合");
        return out;
    }

    private static void addPairRelations(List<String> out, String[] values, String[][] relations) {
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                for (String[] r : relations) {
                    if ((r[0].equals(values[i]) && r[1].equals(values[j]))
                            || (r[1].equals(values[i]) && r[0].equals(values[j]))) {
                        out.add(values[i] + values[j] + " " + r[2]);
                    }
                }
            }
        }
    }

    private static void addTriple(List<String> out, String[] values, String[] need, String label) {
        List<String> list = Arrays.asList(values);
        if (list.contains(need[0]) && list.contains(need[1]) && list.contains(need[2])) out.add(label);
    }

    private static void addLuckCycles(Map<String, Object> result,
                                      EightChar eight,
                                      int gender,
                                      int birthYear,
                                      Date now) {
        Yun yun = eight.getYun(gender, 2);
        result.put("luckDirection", yun.isForward() ? "順行" : "逆行");
        result.put("luckStart", yun.getStartSolar().toYmd());
        result.put("luckStartAge",
                yun.getStartYear() + "年" + yun.getStartMonth() + "月" + yun.getStartDay() + "日");

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH) + 1;
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

        List<Map<String, Object>> periods = new ArrayList<Map<String, Object>>();
        Map<String, Object> current = null;
        for (DaYun d : yun.getDaYun(9)) {
            if (d.getIndex() == 0) continue;
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("ganZhi", d.getGanZhi());
            item.put("startYear", d.getStartYear());
            item.put("endYear", d.getEndYear());
            item.put("startAge", d.getStartAge());
            item.put("endAge", d.getEndAge());
            item.put("xunKong", d.getXunKong());
            periods.add(item);
            if (currentYear >= d.getStartYear() && currentYear <= d.getEndYear()) current = item;
        }
        result.put("luckPillars", periods);
        if (current != null) result.put("currentLuckPillar", current);

        String annual = Solar.fromYmd(currentYear, currentMonth, currentDay).getLunar().getYearInGanZhiExact();
        String annualGan = annual.substring(0, 1);
        String annualZhi = annual.substring(1);
        Map<String, Object> annualMap = new LinkedHashMap<String, Object>();
        annualMap.put("year", currentYear);
        annualMap.put("ganZhi", annual);
        annualMap.put("nominalAge", currentYear - birthYear + 1);
        annualMap.put("tenGod", LunarUtil.SHI_SHEN.get(eight.getDayGan() + annualGan));
        annualMap.put("element", LunarUtil.WU_XING_GAN.get(annualGan) + LunarUtil.WU_XING_ZHI.get(annualZhi));
        annualMap.put("interactions", annualInteractions(eight, annualZhi));
        result.put("currentAnnual", annualMap);
    }

    private static List<String> annualInteractions(EightChar e, String annualZhi) {
        String[] natal = {e.getYearZhi(), e.getMonthZhi(), e.getDayZhi(), e.getTimeZhi()};
        List<String> out = new ArrayList<String>();
        String[][] relations = {
                {"子","丑","六合"},{"寅","亥","六合"},{"卯","戌","六合"},{"辰","酉","六合"},{"巳","申","六合"},{"午","未","六合"},
                {"子","午","六沖"},{"丑","未","六沖"},{"寅","申","六沖"},{"卯","酉","六沖"},{"辰","戌","六沖"},{"巳","亥","六沖"},
                {"子","未","六害"},{"丑","午","六害"},{"寅","巳","六害"},{"卯","辰","六害"},{"申","亥","六害"},{"酉","戌","六害"}
        };
        for (String n : natal) {
            for (String[] r : relations) {
                if ((r[0].equals(n) && r[1].equals(annualZhi))
                        || (r[1].equals(n) && r[0].equals(annualZhi))) {
                    out.add(n + " ↔ " + annualZhi + " " + r[2]);
                }
            }
        }
        if (out.isEmpty()) out.add("流年地支與本命四支未偵測到主要六合、六沖或六害");
        return out;
    }

    private static Map<String, Integer> intElementMap() {
        Map<String, Integer> map = new LinkedHashMap<String, Integer>();
        for (String e : ELEMENTS) map.put(e, 0);
        return map;
    }

    private static Map<String, Double> doubleElementMap() {
        Map<String, Double> map = new LinkedHashMap<String, Double>();
        for (String e : ELEMENTS) map.put(e, 0d);
        return map;
    }

    private static Map<String, Double> roundMap(Map<String, Double> source) {
        Map<String, Double> out = new LinkedHashMap<String, Double>();
        for (Map.Entry<String, Double> e : source.entrySet()) {
            out.put(e.getKey(), Math.round(e.getValue() * 100d) / 100d);
        }
        return out;
    }

    private static void addInt(Map<String, Integer> map, String key) {
        if (key != null && map.containsKey(key)) map.put(key, map.get(key) + 1);
    }

    private static void addDouble(Map<String, Double> map, String key, double value) {
        if (key != null && map.containsKey(key)) map.put(key, map.get(key) + value);
    }

    private static void addGod(Map<String, Integer> map, String god) {
        if (god != null && map.containsKey(god)) map.put(god, map.get(god) + 1);
    }

    private static int elementIndex(String element) {
        for (int i = 0; i < ELEMENTS.length; i++) if (ELEMENTS[i].equals(element)) return i;
        return -1;
    }

    private static String extremeInt(Map<String, Integer> map, boolean max) {
        String winner = "";
        int best = max ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if ((max && e.getValue() > best) || (!max && e.getValue() < best)) {
                best = e.getValue();
                winner = e.getKey();
            }
        }
        return winner;
    }

    private static int[] parseDate(String raw) {
        if (raw == null || !raw.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new IllegalArgumentException("生日格式請用 yyyy-MM-dd");
        }
        String[] p = raw.split("-");
        int year = Integer.parseInt(p[0]);
        int month = Integer.parseInt(p[1]);
        int day = Integer.parseInt(p[2]);
        Solar.fromYmd(year, month, day);
        return new int[]{year, month, day};
    }

    private static int[] parseTime(String raw) {
        if (raw == null || !raw.matches("\\d{2}:\\d{2}")) {
            throw new IllegalArgumentException("八字需要出生時間，格式請用 HH:mm");
        }
        String[] p = raw.split(":");
        int hour = Integer.parseInt(p[0]);
        int minute = Integer.parseInt(p[1]);
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            throw new IllegalArgumentException("出生時間格式請用 00:00–23:59");
        }
        return new int[]{hour, minute};
    }
}
