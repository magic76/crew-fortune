package com.crewpocket.fortune;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VedicInsightBuilder {
    private VedicInsightBuilder() {}

    public static void enrich(Map<String, Object> facts) {
        if (facts == null) return;
        facts.put("personalityProfile", profile(
                "Lagna、Lagna lord、Moon 與 Sun 描述本命性格與外在／內在節奏。",
                evidence(
                        "Lagna " + text(facts.get("lagnaSign")) + " · " + text(facts.get("lagnaNakshatra")),
                        lordEvidence(facts, 1),
                        planetEvidence(facts, "Moon"),
                        planetEvidence(facts, "Sun"))));

        facts.put("careerProfile", profile(
                "工作主題以 10 宮、10 宮主、Saturn/Jupiter 與目前 Dasha 為 deterministic evidence。",
                evidence(
                        houseEvidence(facts, 10),
                        lordEvidence(facts, 10),
                        planetEvidence(facts, "Saturn"),
                        planetEvidence(facts, "Jupiter"),
                        dashaEvidence(facts))));

        facts.put("wealthProfile", profile(
                "資源與財務主題以 2 宮、11 宮及其宮主，再配合 Jupiter/Venus 與目前 Dasha 觀察；不是投資預測。",
                evidence(
                        houseEvidence(facts, 2),
                        lordEvidence(facts, 2),
                        houseEvidence(facts, 11),
                        lordEvidence(facts, 11),
                        planetEvidence(facts, "Jupiter"),
                        planetEvidence(facts, "Venus"),
                        dashaEvidence(facts))));

        facts.put("relationshipProfile", profile(
                "感情與關係以 7 宮、7 宮主、Venus 與目前 Dasha 為主要 evidence；不把訊號說成事件必然。",
                evidence(
                        houseEvidence(facts, 7),
                        lordEvidence(facts, 7),
                        planetEvidence(facts, "Venus"),
                        dashaEvidence(facts))));

        facts.put("familyChildrenProfile", profile(
                "家庭與子女只提供 4 宮／5 宮及宮主、Moon/Jupiter 的結構 evidence，不做懷孕或必然結果預測。",
                evidence(
                        houseEvidence(facts, 4),
                        lordEvidence(facts, 4),
                        houseEvidence(facts, 5),
                        lordEvidence(facts, 5),
                        planetEvidence(facts, "Moon"),
                        planetEvidence(facts, "Jupiter"))));
    }

    private static Map<String, Object> profile(String rule, List<String> evidence) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("rule", rule);
        out.put("evidence", evidence);
        return out;
    }

    private static List<String> evidence(String... values) {
        List<String> out = new ArrayList<String>();
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) continue;
            if (!out.contains(value.trim())) out.add(value.trim());
        }
        return out;
    }

    private static String houseEvidence(Map<String, Object> facts, int house) {
        Object raw = facts.get("houses");
        if (!(raw instanceof List)) return "";
        for (Object item : (List<?>) raw) {
            if (!(item instanceof Map)) continue;
            Map<?, ?> map = (Map<?, ?>) item;
            if (intValue(map.get("house")) != house) continue;
            return house + "宮 " + text(map.get("sign"))
                    + " · 宮主 " + text(map.get("lord"))
                    + " · 宮內 " + compact(map.get("planets"));
        }
        return "";
    }

    private static String lordEvidence(Map<String, Object> facts, int house) {
        Object raw = facts.get("houseLords");
        if (!(raw instanceof List)) return "";
        String lord = "";
        for (Object item : (List<?>) raw) {
            if (!(item instanceof Map)) continue;
            Map<?, ?> map = (Map<?, ?>) item;
            if (intValue(map.get("house")) == house) {
                lord = text(map.get("lord"));
                break;
            }
        }
        if (lord.isEmpty()) return "";
        Object planets = facts.get("planets");
        if (!(planets instanceof List)) return house + "宮主 " + lord;
        for (Object item : (List<?>) planets) {
            if (!(item instanceof Map)) continue;
            Map<?, ?> map = (Map<?, ?>) item;
            if (!lord.equals(text(map.get("name")))) continue;
            return house + "宮主 " + lord
                    + " 落 " + text(map.get("sign"))
                    + " 第" + text(map.get("house")) + "宮"
                    + " · " + text(map.get("nakshatra"))
                    + " Pada " + text(map.get("pada"))
                    + " · " + text(map.get("dignity"));
        }
        return house + "宮主 " + lord;
    }

    private static String planetEvidence(Map<String, Object> facts, String planet) {
        Object raw = facts.get("planets");
        if (!(raw instanceof List)) return "";
        for (Object item : (List<?>) raw) {
            if (!(item instanceof Map)) continue;
            Map<?, ?> map = (Map<?, ?>) item;
            if (!planet.equals(text(map.get("name")))) continue;
            return planet + " " + text(map.get("sign"))
                    + " 第" + text(map.get("house")) + "宮"
                    + " · " + text(map.get("nakshatra"))
                    + " Pada " + text(map.get("pada"))
                    + " · " + text(map.get("dignity"))
                    + (Boolean.TRUE.equals(map.get("retrograde")) ? " · retrograde" : "");
        }
        return "";
    }

    private static String dashaEvidence(Map<String, Object> facts) {
        Object md = facts.get("currentMahadasha");
        Object ad = facts.get("currentAntardasha");
        if (!(md instanceof Map)) return "";
        String value = "目前 Mahadasha " + text(((Map<?, ?>) md).get("lord"))
                + " " + text(((Map<?, ?>) md).get("startDate"))
                + "–" + text(((Map<?, ?>) md).get("endDate"));
        if (ad instanceof Map) {
            value += " · Antardasha " + text(((Map<?, ?>) ad).get("lord"))
                    + " " + text(((Map<?, ?>) ad).get("startDate"))
                    + "–" + text(((Map<?, ?>) ad).get("endDate"));
        }
        return value;
    }

    private static String compact(Object raw) {
        if (!(raw instanceof List)) return raw == null ? "無" : String.valueOf(raw);
        StringBuilder out = new StringBuilder();
        for (Object value : (List<?>) raw) {
            if (out.length() > 0) out.append("、");
            out.append(String.valueOf(value));
        }
        return out.length() == 0 ? "無" : out.toString();
    }

    private static int intValue(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(text(value)); }
        catch (Exception ignored) { return 0; }
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
