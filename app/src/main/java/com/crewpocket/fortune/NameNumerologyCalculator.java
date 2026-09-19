package com.crewpocket.fortune;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class NameNumerologyCalculator {
    public static final String METHOD_VERSION = "pythagorean-name-numerology-v1-y-consonant";

    public Map<String, Object> calculate(String birthNameLatin) {
        String normalized = normalize(birthNameLatin);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("姓名靈數需要英文／羅馬拼音出生姓名");
        }

        int all = 0;
        int vowels = 0;
        int consonants = 0;
        int letterCount = 0;
        int vowelCount = 0;
        int consonantCount = 0;

        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (ch < 'A' || ch > 'Z') continue;
            int value = valueOf(ch);
            all += value;
            letterCount++;
            if (isVowel(ch)) {
                vowels += value;
                vowelCount++;
            } else {
                consonants += value;
                consonantCount++;
            }
        }

        if (letterCount == 0) {
            throw new IllegalArgumentException("姓名靈數只接受英文字母／羅馬拼音");
        }
        if (vowelCount == 0) {
            throw new IllegalArgumentException("出生姓名至少需要一個母音才能計算內在靈魂數");
        }
        if (consonantCount == 0) {
            throw new IllegalArgumentException("出生姓名至少需要一個子音才能計算外在人格數");
        }

        int expression = reduceMaster(all);
        int soul = reduceMaster(vowels);
        int personality = reduceMaster(consonants);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("methodVersion", METHOD_VERSION);
        result.put("birthNameLatin", normalized);
        result.put("expressionNumber", expression);
        result.put("expressionDisplay", masterDisplay(expression));
        result.put("soulUrgeNumber", soul);
        result.put("soulUrgeDisplay", masterDisplay(soul));
        result.put("personalityNumber", personality);
        result.put("personalityDisplay", masterDisplay(personality));
        result.put("rawExpressionSum", all);
        result.put("rawSoulUrgeSum", vowels);
        result.put("rawPersonalitySum", consonants);
        result.put("vowelConvention", "A/E/I/O/U 為母音；Y 在 v1 固定視為子音");
        result.put("calculationRule",
                "Expression=全部英文字母；Soul Urge=母音；Personality=子音；Pythagorean 1–9；保留 11/22/33");
        return result;
    }

    public static String normalize(String raw) {
        if (raw == null) return "";
        String upper = raw.trim().toUpperCase(Locale.US);
        StringBuilder out = new StringBuilder();
        boolean previousSpace = false;
        for (int i = 0; i < upper.length(); i++) {
            char ch = upper.charAt(i);
            if (ch >= 'A' && ch <= 'Z') {
                out.append(ch);
                previousSpace = false;
            } else if ((ch == ' ' || ch == '-' || ch == '\'') && out.length() > 0 && !previousSpace) {
                out.append(' ');
                previousSpace = true;
            }
        }
        while (out.length() > 0 && out.charAt(out.length() - 1) == ' ') {
            out.deleteCharAt(out.length() - 1);
        }
        return out.toString();
    }

    private static int valueOf(char ch) {
        return ((ch - 'A') % 9) + 1;
    }

    private static boolean isVowel(char ch) {
        return ch == 'A' || ch == 'E' || ch == 'I' || ch == 'O' || ch == 'U';
    }

    private static int reduceMaster(int value) {
        int v = Math.abs(value);
        while (v > 9 && v != 11 && v != 22 && v != 33) {
            v = digitSum(v);
        }
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

    private static String masterDisplay(int value) {
        if (value == 11 || value == 22 || value == 33) {
            return value + "/" + digitSum(value);
        }
        return String.valueOf(value);
    }
}
