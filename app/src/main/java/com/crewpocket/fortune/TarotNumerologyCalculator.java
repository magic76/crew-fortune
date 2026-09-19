package com.crewpocket.fortune;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TarotNumerologyCalculator {
    public static final String METHOD_VERSION = "tarot-personality-soul-birthday-v5";

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

        int rawDigitSum = digitSum(year) + digitSum(month) + digitSum(day);
        int lifePath = reduceMaster(rawDigitSum);
        int personalityNumber = reduceTo22(rawDigitSum);
        int soulNumber = reduceSingle(personalityNumber);
        int innerNumber = soulNumber;
        int outerNumber = personalityNumber;
        int attitude = reduceSingle(month + day);
        int birthdayCore = reduceSingle(day);
        List<Integer> talentNumbers = digitsOf(rawDigitSum);

        Calendar c = Calendar.getInstance();
        c.setTime(now == null ? new Date() : now);
        int currentYear = c.get(Calendar.YEAR);
        int currentMonth = c.get(Calendar.MONTH) + 1;
        int personalYear = reduceSingle(monthCore + dayCore + reduceSingle(digitSum(currentYear)));
        int personalMonth = reduceSingle(personalYear + currentMonth);
        List<Map<String, Object>> personalYearTimeline =
                personalYearTimeline(monthCore, dayCore, currentYear);
        List<Map<String, Object>> personalMonthTimeline =
                personalMonthTimeline(personalYear, currentYear);

        List<Integer> birthCards = tarotPersonalitySoulCards(personalityNumber, soulNumber);
        List<Map<String, Object>> cardDetails = new ArrayList<Map<String, Object>>();
        List<String> cardNames = new ArrayList<String>();
        for (int number : birthCards) {
            int index = cardIndex(number);
            Map<String, Object> card = new LinkedHashMap<String, Object>();
            card.put("number", number);
            card.put("name", CARD_NAMES[index]);
            card.put("keywords", CARD_KEYWORDS[index]);
            cardDetails.add(card);
            cardNames.add(number + " " + CARD_NAMES[index]);
        }

        int[] pinnacles = pinnacles(month, day, year);
        int[] challenges = challenges(month, day, year);
        int lifePathSingle = reduceSingle(lifePath);
        int firstEnd = 36 - lifePathSingle;

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("methodVersion", METHOD_VERSION);
        result.put("lifePathNumber", lifePath);
        result.put("lifePathDisplay", masterDisplay(lifePath));
        result.put("innerNumber", innerNumber);
        result.put("outerNumber", outerNumber);
        result.put("personalityCardNumber", personalityNumber);
        result.put("personalityCardName", CARD_NAMES[cardIndex(personalityNumber)]);
        result.put("soulCardNumber", soulNumber);
        result.put("soulCardName", CARD_NAMES[cardIndex(soulNumber)]);
        result.put("talentNumbers", talentNumbers);
        result.put("talentSource", rawDigitSum);
        result.put("birthdayNumber", day);
        result.put("birthdayCore", birthdayCore);
        result.put("attitudeNumber", attitude);
        result.put("personalYearCalendarYear", currentYear);
        result.put("personalYear", personalYear);
        result.put("personalYearCardName", CARD_NAMES[cardIndex(personalYear)]);
        result.put("personalMonth", personalMonth);
        result.put("personalYearTimelineStartYear", currentYear - 1);
        result.put("personalYearTimelineEndYear", currentYear + 9);
        result.put("personalYearTimeline", personalYearTimeline);
        result.put("personalMonthTimeline", personalMonthTimeline);
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
                "外在人格牌：西元出生年月日所有數字加總；若大於 22，持續將各位數相加，直到 1–22",
                "內在靈魂牌：將人格牌再化簡至 1–9",
                "天賦數：保留生日原始數字總和的各位數作為延伸觀察",
                "生命道路數：西元出生年月日全部數字加總，保留 11/22/33 主數",
                "態度數：出生月 + 出生日，化為 1–9",
                "個人流年：當年西元年各位數 + 出生月 + 出生日，化為 1–9",
                "流年時間軸：固定顯示上一年到未來 9 年；每年依同一公式計算，對應 1–9 號大牌",
                "個人月：當年個人流年 + 月份，化為 1–9",
                "Rider-Waite-Smith 編號：5=教皇、8=力量、9=隱者、11=正義",
                "人格牌／靈魂牌流派有差異，本 App 固定採生日加總 → ≤22 → 1–9 的規則，不使用姓名"));
        result.put("note", containsCard(birthCards, 13)
                ? "死神牌在此代表轉化與階段更替，不是死亡預測。"
                : "塔羅生命靈數作為娛樂與自我反思用途，不代表必然命運。");
        return result;
    }

    private static List<Map<String, Object>> personalYearTimeline(
            int monthCore, int dayCore, int currentYear) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (int year = currentYear - 1; year <= currentYear + 9; year++) {
            int number = reduceSingle(
                    monthCore + dayCore + reduceSingle(digitSum(year)));
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("year", year);
            item.put("personalYear", number);
            item.put("cardName", CARD_NAMES[cardIndex(number)]);
            item.put("keywords", CARD_KEYWORDS[cardIndex(number)]);
            item.put("plainSummary", personalYearSummary(number));
            out.add(item);
        }
        return out;
    }

    private static List<Map<String, Object>> personalMonthTimeline(
            int personalYear, int currentYear) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (int month = 1; month <= 12; month++) {
            int number = reduceSingle(personalYear + month);
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("year", currentYear);
            item.put("month", month);
            item.put("personalMonth", number);
            item.put("cardName", CARD_NAMES[cardIndex(number)]);
            item.put("keywords", CARD_KEYWORDS[cardIndex(number)]);
            item.put("plainSummary", personalMonthSummary(number));
            out.add(item);
        }
        return out;
    }

    private static String personalYearSummary(int number) {
        switch (number) {
            case 1: return "新週期啟動，適合定方向、開始新計畫與建立自主節奏";
            case 2: return "合作與關係調整更重要，適合觀察、協調與耐心累積";
            case 3: return "表達、創作與社交能見度提高，適合把想法說出來做出來";
            case 4: return "重點在打基礎、制度化與穩定推進，成果靠持續累積";
            case 5: return "變化、移動與新選項增加，適合保持彈性但避免亂衝";
            case 6: return "責任、家庭、承諾與照顧議題變重，需要平衡自己與他人";
            case 7: return "研究、內省與重新理解方向的一年，適合減少雜訊、深挖能力";
            case 8: return "成果、資源與現實目標被放大，適合談效率、權責與資源配置";
            case 9: return "整理、完成與收尾的年份，適合清掉舊包袱並準備下一輪";
            default: return "以穩定觀察與自我整理為主";
        }
    }

    private static String personalMonthSummary(int number) {
        switch (number) {
            case 1: return "適合啟動";
            case 2: return "適合協調";
            case 3: return "適合表達";
            case 4: return "適合整理";
            case 5: return "適合變動";
            case 6: return "適合承擔";
            case 7: return "適合研究";
            case 8: return "適合推進成果";
            case 9: return "適合收尾";
            default: return "保持觀察";
        }
    }

    private static List<Integer> tarotPersonalitySoulCards(int personality, int soul) {
        List<Integer> cards = new ArrayList<Integer>();
        cards.add(personality);
        if (soul != personality) cards.add(soul);
        return cards;
    }

    private static int cardIndex(int number) {
        if (number == 22) return 0;
        return Math.max(0, Math.min(21, number));
    }

    private static int reduceTo22(int value) {
        int v = Math.abs(value);
        while (v > 22) v = digitSum(v);
        return v == 0 ? 22 : v;
    }

    private static List<Integer> digitsOf(int value) {
        List<Integer> digits = new ArrayList<Integer>();
        String raw = String.valueOf(Math.abs(value));
        for (int i = 0; i < raw.length(); i++) {
            digits.add(raw.charAt(i) - '0');
        }
        return digits;
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
