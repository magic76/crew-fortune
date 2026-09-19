package com.crewpocket.fortune;

import com.nlf.calendar.EightChar;
import com.nlf.calendar.Solar;
import com.nlf.calendar.util.LunarUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BaZiCalculator {
    public static final String METHOD_VERSION = "bazi-four-pillars-lunar-java-1.7.7-sect2";

    public Map<String, Object> calculate(String birthDate, String birthTime) {
        int[] date = parseDate(birthDate);
        int[] time = parseTime(birthTime);

        Solar solar = Solar.fromYmdHms(date[0], date[1], date[2], time[0], time[1], 0);
        EightChar eight = solar.getLunar().getEightChar();

        // sect 2: 23:00-23:59 stays on the current civil day.
        // This is explicit because different BaZi schools use different late-Zi conventions.
        eight.setSect(2);

        Map<String, Integer> elements = new LinkedHashMap<String, Integer>();
        for (String e : Arrays.asList("木", "火", "土", "金", "水")) elements.put(e, 0);

        addElement(elements, LunarUtil.WU_XING_GAN.get(eight.getYearGan()));
        addElement(elements, LunarUtil.WU_XING_ZHI.get(eight.getYearZhi()));
        addElement(elements, LunarUtil.WU_XING_GAN.get(eight.getMonthGan()));
        addElement(elements, LunarUtil.WU_XING_ZHI.get(eight.getMonthZhi()));
        addElement(elements, LunarUtil.WU_XING_GAN.get(eight.getDayGan()));
        addElement(elements, LunarUtil.WU_XING_ZHI.get(eight.getDayZhi()));
        addElement(elements, LunarUtil.WU_XING_GAN.get(eight.getTimeGan()));
        addElement(elements, LunarUtil.WU_XING_ZHI.get(eight.getTimeZhi()));

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("methodVersion", METHOD_VERSION);
        result.put("timeConvention", "出生地當地民用時間；晚子時採 sect=2（23:00 不換日）");
        result.put("yearPillar", eight.getYear());
        result.put("monthPillar", eight.getMonth());
        result.put("dayPillar", eight.getDay());
        result.put("timePillar", eight.getTime());
        result.put("fourPillars", eight.toString());
        result.put("dayMaster", eight.getDayGan());
        result.put("dayMasterElement", LunarUtil.WU_XING_GAN.get(eight.getDayGan()));
        result.put("visibleFiveElements", elements);
        result.put("fiveElementBalance", balanceScore(elements));
        result.put("strongestVisibleElement", extreme(elements, true));
        result.put("weakestVisibleElement", extreme(elements, false));

        Map<String, Object> hidden = new LinkedHashMap<String, Object>();
        hidden.put("year", new ArrayList<String>(eight.getYearHideGan()));
        hidden.put("month", new ArrayList<String>(eight.getMonthHideGan()));
        hidden.put("day", new ArrayList<String>(eight.getDayHideGan()));
        hidden.put("time", new ArrayList<String>(eight.getTimeHideGan()));
        result.put("hiddenStems", hidden);

        Map<String, Object> tenGods = new LinkedHashMap<String, Object>();
        tenGods.put("yearStem", eight.getYearShiShenGan());
        tenGods.put("monthStem", eight.getMonthShiShenGan());
        tenGods.put("dayStem", "日主");
        tenGods.put("timeStem", eight.getTimeShiShenGan());
        tenGods.put("yearBranch", new ArrayList<String>(eight.getYearShiShenZhi()));
        tenGods.put("monthBranch", new ArrayList<String>(eight.getMonthShiShenZhi()));
        tenGods.put("dayBranch", new ArrayList<String>(eight.getDayShiShenZhi()));
        tenGods.put("timeBranch", new ArrayList<String>(eight.getTimeShiShenZhi()));
        result.put("tenGods", tenGods);

        result.put("naYin", Arrays.asList(
                eight.getYearNaYin(), eight.getMonthNaYin(), eight.getDayNaYin(), eight.getTimeNaYin()));
        result.put("lifeStages", Arrays.asList(
                eight.getYearDiShi(), eight.getMonthDiShi(), eight.getDayDiShi(), eight.getTimeDiShi()));
        result.put("mingGong", eight.getMingGong());
        result.put("shenGong", eight.getShenGong());
        return result;
    }

    private static int[] parseDate(String raw) {
        if (raw == null || !raw.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new IllegalArgumentException("生日格式請用 yyyy-MM-dd");
        }
        String[] p = raw.split("-");
        return new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])};
    }

    private static int[] parseTime(String raw) {
        if (raw == null || !raw.matches("\\d{2}:\\d{2}")) {
            throw new IllegalArgumentException("八字命盤需要出生時間，格式請用 HH:mm");
        }
        String[] p = raw.split(":");
        int hour = Integer.parseInt(p[0]);
        int minute = Integer.parseInt(p[1]);
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            throw new IllegalArgumentException("出生時間格式請用 00:00–23:59");
        }
        return new int[]{hour, minute};
    }

    private static void addElement(Map<String, Integer> elements, String element) {
        if (element == null || !elements.containsKey(element)) return;
        elements.put(element, elements.get(element) + 1);
    }

    private static int balanceScore(Map<String, Integer> elements) {
        double deviation = 0;
        for (int count : elements.values()) deviation += Math.abs(count - 1.6d);
        return Math.max(30, Math.min(100, (int) Math.round(100d - deviation * 8d)));
    }

    private static String extreme(Map<String, Integer> elements, boolean max) {
        String winner = "";
        int best = max ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> entry : elements.entrySet()) {
            int value = entry.getValue();
            if ((max && value > best) || (!max && value < best)) {
                winner = entry.getKey();
                best = value;
            }
        }
        return winner;
    }
}
