package com.crewpocket.fortune;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TarotNumerologyCalculator {
    public static final String METHOD_VERSION = "tarot-school-birth-cards-plus-birthdate-numerology-v2";

    private static final String[] CARD_NAMES = {
            "愚者", "魔術師", "女祭司", "皇后", "皇帝", "教皇", "戀人", "戰車",
            "力量", "隱者", "命運之輪", "正義", "倒吊人", "死神", "節制", "惡魔",
            "高塔", "星星", "月亮", "太陽", "審判", "世界"
    };

    private static final String[] CARD_KEYWORDS = {
            "自由、開始、未知", "行動、意志、創造", "直覺、內在、觀察", "滋養、豐盛、感受",
            "結構、責任、掌控", "傳統、學習、價值", "選擇、關係、結盟", "推進、意志、方向",
            "勇氣、耐性、內在力量", "獨處、研究、尋找答案", "循環、轉折、時機", "平衡、判斷、責任",
            "換角度、暫停、放下控制", "轉化、結束舊階段、更新", "調和、節奏、整合", "慾望、執著、束縛",
            "突破、重建、醒悟", "希望、修復、願景", "想像、不確定、潛意識", "活力、清晰、展現",
            "覺醒、回顧、重新選擇", "完成、整合、下一階段"
    };

    public Map<String, Object> calculate(String birthDate, Date now) {
        int[] d = parseDate(birthDate);
        int year = d[0];
        int month = d[1];
        int day = d[2];

        int monthCore = reduceSingle(month);
        int dayCore = reduceSingle(day);
        int yearCore = reduceSingle(digitSum(year));

        int rawDigitSum = digitSum(year) + digitSum(month) + digitSum(day);
        int lifePath = reduceMaster(rawDigitSum);
        int attitude = reduceSingle(month + day);
        int birthdayCore = reduceSingle(day);

        Calendar c = Calendar.getInstance();
        c.setTime(now == null ? new Date() : now);
        int currentYear = c.get(Calendar.YEAR);
        int currentMonth = c.get(Calendar.MONTH) + 1;
        int personalYear = reduceSingle(monthCore + dayCore + reduceSingle(digitSum(currentYear)));
        int personalMonth = reduceSingle(personalYear + currentMonth);

        List<Integer> birthCards = tarotSchoolBirthCards(month, day, year);
        List<Map<String, Object>> cardDetails = new ArrayList<Map<String, Object>>();
        List<String> cardNames = new ArrayList<String>();
        for (int number : birthCards) {
            Map<String, Object> card = new LinkedHashMap<String, Object>();
            card.put("number", number);
            card.put("name", CARD_NAMES[number]);
            card.put("keywords", CARD_KEYWORDS[number]);
            cardDetails.add(card);
            cardNames.add(number + " " + CARD_NAMES[number]);
        }

        int[] pinnacles = pinnacles(month, day, year);
        int[] challenges = challenges(month, day, year);
        int lifePathSingle = reduceSingle(lifePath);
        int firstEnd = 36 - lifePathSingle;

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("methodVersion", METHOD_VERSION);
        result.put("lifePathNumber", lifePath);
        result.put("lifePathDisplay", masterDisplay(lifePath));
        result.put("birthdayNumber", day);
        result.put("birthdayCore", birthdayCore);
        result.put("attitudeNumber", attitude);
        result.put("personalYear", personalYear);
        result.put("personalMonth", personalMonth);
        result.put("birthCards", cardDetails);
        result.put("birthCardDisplay", join(cardNames, " × "));
        result.put("pinnacles", Arrays.asList(pinnacles[0], pinnacles[1], pinnacles[2], pinnacles[3]));
        result.put("pinnacleTiming", Arrays.asList(
                "0–" + firstEnd,
                (firstEnd + 1) + "–" + (firstEnd + 9),
                (firstEnd + 10) + "–" + (firstEnd + 18),
                (firstEnd + 19) + "+"));
        result.put("challenges", Arrays.asList(challenges[0], challenges[1], challenges[2], challenges[3]));
        result.put("periodCycles", Arrays.asList(
                reduceMaster(month), reduceMaster(day), reduceMaster(digitSum(year))));
        result.put("calculationNotes", Arrays.asList(
                "生命靈數：完整生日數字加總，保留 11/22/33 主數",
                "態度數：出生月 + 出生日，化為 1–9",
                "個人年：出生月 + 出生日 + 當年度，化為 1–9",
                "出生牌：採 Tarot School 的 MM + DD + century + YY 公式與牌組配對",
                "Rider-Waite-Smith 編號：8=力量、11=正義；出生牌組不把 0 愚者列為配對牌"));
        result.put("note", containsCard(birthCards, 13)
                ? "死神牌在此代表轉化與階段更替，不是死亡預測。"
                : "塔羅生命靈數作為娛樂與自我反思用途，不代表必然命運。");
        return result;
    }

    private static List<Integer> tarotSchoolBirthCards(int month, int day, int year) {
        int century = year / 100;
        int yy = year % 100;
        int sum = month + day + century + yy;

        int primary;
        if (sum >= 100) {
            primary = (sum / 10) + (sum % 10);
        } else {
            primary = digitSum(sum);
        }
        while (primary > 21) primary = digitSum(primary);

        List<Integer> cards = new ArrayList<Integer>();
        if (primary == 19) {
            cards.add(19);
            cards.add(10);
            cards.add(1);
            return cards;
        }

        if (primary >= 10 && primary <= 21) {
            cards.add(primary);
            cards.add(digitSum(primary));
            return cards;
        }

        int[] implied = {0,10,11,12,13,14,15,16,17,18};
        int pair = implied[Math.max(1, Math.min(9, primary))];
        cards.add(pair);
        cards.add(primary);
        return cards;
    }

    private static int[] pinnacles(int month, int day, int year) {
        int m = reduceMaster(month);
        int d = reduceMaster(day);
        int y = reduceMaster(digitSum(year));
        int p1 = reducePinnacle(m + d);
        int p2 = reducePinnacle(d + y);
        int p3 = reducePinnacle(p1 + p2);
        int p4 = reducePinnacle(m + y);
        return new int[]{p1,p2,p3,p4};
    }

    private static int[] challenges(int month, int day, int year) {
        int m = reduceSingle(month);
        int d = reduceSingle(day);
        int y = reduceSingle(digitSum(year));
        int c1 = Math.abs(m - d);
        int c2 = Math.abs(d - y);
        int c3 = Math.abs(c1 - c2);
        int c4 = Math.abs(m - y);
        return new int[]{c1,c2,c3,c4};
    }

    private static int reducePinnacle(int value) {
        while (value > 9 && value != 11 && value != 22) value = digitSum(value);
        return value;
    }

    private static int reduceMaster(int value) {
        while (value > 9 && value != 11 && value != 22 && value != 33) value = digitSum(value);
        return value;
    }

    private static int reduceSingle(int value) {
        int v = Math.abs(value);
        while (v > 9) v = digitSum(v);
        return v;
    }

    private static int digitSum(int value) {
        int v = Math.abs(value);
        int sum = 0;
        do {
            sum += v % 10;
            v /= 10;
        } while (v > 0);
        return sum;
    }

    private static boolean containsCard(List<Integer> cards, int number) {
        for (int card : cards) if (card == number) return true;
        return false;
    }

    private static String masterDisplay(int value) {
        if (value == 11 || value == 22 || value == 33) return value + "/" + reduceSingle(value);
        return String.valueOf(value);
    }

    private static String join(List<String> values, String separator) {
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            if (out.length() > 0) out.append(separator);
            out.append(value);
        }
        return out.toString();
    }

    private static int[] parseDate(String raw) {
        if (raw == null || !raw.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new IllegalArgumentException("生日格式請用 yyyy-MM-dd");
        }
        String[] p = raw.split("-");
        int y = Integer.parseInt(p[0]);
        int m = Integer.parseInt(p[1]);
        int d = Integer.parseInt(p[2]);
        if (m < 1 || m > 12 || d < 1 || d > 31) throw new IllegalArgumentException("生日格式不正確");
        return new int[]{y,m,d};
    }
}
