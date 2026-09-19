package com.crewpocket.fortune;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TarotNumerologyCalculator {
    public static final String METHOD_VERSION = "tarot-birth-card-digit-sum-v1";

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
            "突破、重建、突發醒悟", "希望、修復、願景", "想像、不確定、潛意識", "活力、清晰、展現",
            "覺醒、回顧、重新選擇", "完成、整合、進入下一階段"
    };

    public Map<String, Object> calculate(String birthDate) {
        int rawDigitSum = digitSumFromDate(birthDate);
        int tarotValue = rawDigitSum;
        while (tarotValue > 22) tarotValue = digitSum(tarotValue);

        int cardNumber = tarotValue == 22 ? 0 : tarotValue;
        if (cardNumber < 0 || cardNumber > 21) {
            throw new IllegalArgumentException("無法計算塔羅出生牌");
        }

        int lifePath = rawDigitSum;
        while (lifePath > 9 && lifePath != 11 && lifePath != 22 && lifePath != 33) {
            lifePath = digitSum(lifePath);
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("methodVersion", METHOD_VERSION);
        result.put("rawDigitSum", rawDigitSum);
        result.put("lifePathNumber", lifePath);
        result.put("tarotReductionValue", tarotValue);
        result.put("birthCardNumber", cardNumber);
        result.put("birthCardName", CARD_NAMES[cardNumber]);
        result.put("birthCardKeywords", CARD_KEYWORDS[cardNumber]);
        result.put("deckConvention", "Rider-Waite-Smith order: 8=力量, 11=正義; 22→0 愚者");

        if (tarotValue >= 10 && tarotValue <= 21) {
            int reduced = digitSum(tarotValue);
            result.put("reducedSoulNumber", reduced);
            result.put("reducedSoulCardName", CARD_NAMES[reduced]);
            result.put("reducedSoulCardKeywords", CARD_KEYWORDS[reduced]);
        }

        result.put("note", cardNumber == 13
                ? "死神牌在此代表轉化與階段更替，不是死亡預測。"
                : "出生牌作為娛樂與自我反思用途，不代表必然命運。");
        return result;
    }

    public String cardName(int number) {
        if (number < 0 || number > 21) return "";
        return CARD_NAMES[number];
    }

    private static int digitSumFromDate(String raw) {
        if (raw == null || !raw.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new IllegalArgumentException("生日格式請用 yyyy-MM-dd");
        }
        int sum = 0;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isDigit(c)) sum += c - '0';
        }
        return sum;
    }

    private static int digitSum(int value) {
        int sum = 0;
        int v = Math.abs(value);
        while (v > 0) {
            sum += v % 10;
            v /= 10;
        }
        return sum;
    }
}
