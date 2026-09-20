package com.crewpocket.fortune;

import java.util.List;
import java.util.Map;

public final class VedicFactsFormatter {
    private VedicFactsFormatter() {}

    public static String coreSummary(FortuneFacts facts) {
        return "Lagna｜" + facts.detailText("lagnaSign")
                + " · " + facts.detailText("lagnaNakshatra")
                + " Pada " + facts.detailText("lagnaPada")
                + "\nMoon｜" + facts.detailText("moonSign")
                + " · " + facts.detailText("moonNakshatra")
                + " Pada " + facts.detailText("moonPada")
                + "\nSun｜" + facts.detailText("sunSign");
    }

    public static String dashaSummary(FortuneFacts facts) {
        return "Mahadasha｜" + period(facts.detail("currentMahadasha"))
                + "\nAntardasha｜" + period(facts.detail("currentAntardasha"));
    }

    public static String planets(FortuneFacts facts) {
        StringBuilder out = new StringBuilder();
        Object raw = facts.detail("planets");
        if (!(raw instanceof List)) return "";
        for (Object item : (List<?>) raw) {
            if (!(item instanceof Map)) continue;
            Map<?, ?> p = (Map<?, ?>) item;
            if (out.length() > 0) out.append("\n");
            out.append(text(p.get("name")))
                    .append("｜").append(text(p.get("sign")))
                    .append(" ").append(formatDegree(p.get("degreeInSign")))
                    .append(" · H").append(text(p.get("house")))
                    .append(" · ").append(text(p.get("nakshatra")))
                    .append(" P").append(text(p.get("pada")));
            if (Boolean.TRUE.equals(p.get("retrograde"))) out.append(" · R");
            String dignity = text(p.get("dignity"));
            if (!dignity.isEmpty() && !"neutral".equals(dignity)) {
                out.append(" · ").append(dignity);
            }
        }
        return out.toString();
    }

    public static String houses(FortuneFacts facts) {
        StringBuilder out = new StringBuilder();
        Object raw = facts.detail("houses");
        if (!(raw instanceof List)) return "";
        for (Object item : (List<?>) raw) {
            if (!(item instanceof Map)) continue;
            Map<?, ?> h = (Map<?, ?>) item;
            if (out.length() > 0) out.append("\n");
            out.append("H").append(text(h.get("house")))
                    .append("｜").append(text(h.get("sign")))
                    .append(" · lord ").append(text(h.get("lord")))
                    .append(" · ").append(compact(h.get("planets")));
        }
        return out.toString();
    }

    public static String aspects(FortuneFacts facts) {
        StringBuilder out = new StringBuilder();
        Object raw = facts.detail("aspects");
        if (raw instanceof List) {
            for (Object item : (List<?>) raw) {
                if (!(item instanceof Map)) continue;
                Map<?, ?> a = (Map<?, ?>) item;
                if (out.length() > 0) out.append("\n");
                out.append(text(a.get("from")))
                        .append(" → H").append(text(a.get("toHouse")))
                        .append(" (").append(text(a.get("distance"))).append("th)")
                        .append(" · ").append(compact(a.get("targetPlanets")));
            }
        }
        Object conjunctionRaw = facts.detail("conjunctions");
        if (conjunctionRaw instanceof List && !((List<?>) conjunctionRaw).isEmpty()) {
            if (out.length() > 0) out.append("\n\n");
            out.append("Conjunctions\n");
            for (Object item : (List<?>) conjunctionRaw) {
                if (!(item instanceof Map)) continue;
                Map<?, ?> c = (Map<?, ?>) item;
                out.append(text(c.get("a"))).append(" + ").append(text(c.get("b")))
                        .append("｜").append(text(c.get("sign")))
                        .append(" · H").append(text(c.get("house")))
                        .append(" · ").append(text(c.get("separationDegrees"))).append("°")
                        .append("\n");
            }
        }
        return out.toString().trim();
    }

    public static String mahadashaTimeline(FortuneFacts facts) {
        StringBuilder out = new StringBuilder();
        Object raw = facts.detail("mahadashaTimeline");
        if (!(raw instanceof List)) return "";
        for (Object item : (List<?>) raw) {
            if (!(item instanceof Map)) continue;
            Map<?, ?> d = (Map<?, ?>) item;
            if (out.length() > 0) out.append("\n");
            out.append(text(d.get("lord")))
                    .append("｜").append(text(d.get("startDate")))
                    .append(" → ").append(text(d.get("endDate")))
                    .append(" · age ").append(text(d.get("startAge")))
                    .append("–").append(text(d.get("endAge")));
        }
        return out.toString();
    }

    public static String antardashaTimeline(FortuneFacts facts) {
        Object current = facts.detail("currentMahadasha");
        String currentLord = mapText(current, "lord");
        Object raw = facts.detail("mahadashaTimeline");
        if (!(raw instanceof List)) return "";
        for (Object item : (List<?>) raw) {
            if (!(item instanceof Map)) continue;
            Map<?, ?> d = (Map<?, ?>) item;
            if (!currentLord.equals(text(d.get("lord")))) continue;
            Object antars = d.get("antardashas");
            if (!(antars instanceof List)) return "";
            StringBuilder out = new StringBuilder();
            for (Object adRaw : (List<?>) antars) {
                if (!(adRaw instanceof Map)) continue;
                Map<?, ?> ad = (Map<?, ?>) adRaw;
                if (out.length() > 0) out.append("\n");
                out.append(currentLord).append("/").append(text(ad.get("lord")))
                        .append("｜").append(text(ad.get("startDate")))
                        .append(" → ").append(text(ad.get("endDate")));
            }
            return out.toString();
        }
        return "";
    }

    public static String profileEvidence(FortuneFacts facts, String key) {
        Object raw = facts.detail(key);
        if (!(raw instanceof Map)) return "";
        Map<?, ?> profile = (Map<?, ?>) raw;
        StringBuilder out = new StringBuilder(text(profile.get("rule")));
        Object evidence = profile.get("evidence");
        if (evidence instanceof List) {
            for (Object value : (List<?>) evidence) {
                if (out.length() > 0) out.append("\n");
                out.append("• ").append(text(value));
            }
        }
        return out.toString();
    }

    public static String houseLordPlacements(FortuneFacts facts) {
        StringBuilder out = new StringBuilder();
        Object raw = facts.detail("houseLords");
        Object planets = facts.detail("planets");
        if (!(raw instanceof List)) return "";
        for (Object item : (List<?>) raw) {
            if (!(item instanceof Map)) continue;
            Map<?, ?> lord = (Map<?, ?>) item;
            String name = text(lord.get("lord"));
            if (out.length() > 0) out.append("\n");
            out.append("H").append(text(lord.get("house")))
                    .append(" 宮主 ").append(name);
            Map<?, ?> planet = findPlanet(planets, name);
            if (planet != null) {
                out.append(" → H").append(text(planet.get("house")))
                        .append(" · ").append(text(planet.get("sign")))
                        .append(" · ").append(text(planet.get("nakshatra")))
                        .append(" P").append(text(planet.get("pada")));
                if (Boolean.TRUE.equals(planet.get("retrograde"))) out.append(" · R");
                String dignity = text(planet.get("dignity"));
                if (!dignity.isEmpty() && !"neutral".equals(dignity)) {
                    out.append(" · ").append(dignity);
                }
            }
        }
        return out.toString();
    }

    public static String topicTimingEvidence(FortuneFacts facts, String profileKey) {
        StringBuilder out = new StringBuilder();
        String dasha = dashaSummary(facts);
        if (!dasha.isEmpty()) out.append(dasha);

        List<String> focus = new java.util.ArrayList<String>();
        if ("careerProfile".equals(profileKey)) {
            focus.add("Saturn"); focus.add("Jupiter");
        } else if ("wealthProfile".equals(profileKey)) {
            focus.add("Jupiter"); focus.add("Venus");
        } else if ("relationshipProfile".equals(profileKey)) {
            focus.add("Venus");
        } else if ("familyChildrenProfile".equals(profileKey)) {
            focus.add("Moon"); focus.add("Jupiter");
        } else if ("personalityProfile".equals(profileKey)) {
            focus.add("Moon"); focus.add("Sun");
        }

        Object transits = facts.detail("currentTransits");
        if (transits instanceof List && !focus.isEmpty()) {
            for (Object item : (List<?>) transits) {
                if (!(item instanceof Map)) continue;
                Map<?, ?> p = (Map<?, ?>) item;
                if (!focus.contains(text(p.get("name")))) continue;
                if (out.length() > 0) out.append("\n");
                out.append("Gochar ")
                        .append(text(p.get("name")))
                        .append(" → H").append(text(p.get("natalHouse")))
                        .append(" · ").append(text(p.get("sign")));
                if (Boolean.TRUE.equals(p.get("retrograde"))) out.append(" · R");
            }
        }

        String hits = topicTransitHits(facts, focus);
        if (!hits.isEmpty()) {
            if (out.length() > 0) out.append("\n");
            out.append(hits);
        }
        return out.toString();
    }

    private static String topicTransitHits(FortuneFacts facts, List<String> focus) {
        StringBuilder out = new StringBuilder();
        Object aspects = facts.detail("transitAspectsToNatal");
        if (aspects instanceof List) {
            for (Object item : (List<?>) aspects) {
                if (!(item instanceof Map)) continue;
                Map<?, ?> a = (Map<?, ?>) item;
                if (!focus.isEmpty() && !focus.contains(text(a.get("transitPlanet")))) continue;
                Object targets = a.get("natalPlanets");
                if (!(targets instanceof List) || ((List<?>) targets).isEmpty()) continue;
                if (out.length() > 0) out.append("\n");
                out.append("Transit ")
                        .append(text(a.get("transitPlanet")))
                        .append(" aspect → H")
                        .append(text(a.get("toNatalHouse")))
                        .append(" · ").append(compact(targets));
            }
        }
        Object conjunctions = facts.detail("transitConjunctionsToNatal");
        if (conjunctions instanceof List) {
            for (Object item : (List<?>) conjunctions) {
                if (!(item instanceof Map)) continue;
                Map<?, ?> c = (Map<?, ?>) item;
                if (!focus.isEmpty() && !focus.contains(text(c.get("transitPlanet")))) continue;
                if (out.length() > 0) out.append("\n");
                out.append("Transit ")
                        .append(text(c.get("transitPlanet")))
                        .append(" 合本命 ").append(text(c.get("natalPlanet")))
                        .append(" · H").append(text(c.get("natalHouse")))
                        .append(" · ").append(text(c.get("separationDegrees"))).append("°");
            }
        }
        return out.toString();
    }

    private static Map<?, ?> findPlanet(Object raw, String name) {
        if (!(raw instanceof List)) return null;
        for (Object item : (List<?>) raw) {
            if (!(item instanceof Map)) continue;
            Map<?, ?> p = (Map<?, ?>) item;
            if (name.equals(text(p.get("name")))) return p;
        }
        return null;
    }

    public static String currentGochar(FortuneFacts facts) {
        StringBuilder out = new StringBuilder();
        Object raw = facts.detail("currentTransits");
        if (!(raw instanceof List)) return "";
        for (Object item : (List<?>) raw) {
            if (!(item instanceof Map)) continue;
            Map<?, ?> p = (Map<?, ?>) item;
            if (out.length() > 0) out.append("\n");
            out.append(text(p.get("name")))
                    .append("｜").append(text(p.get("sign")))
                    .append(" ").append(formatDegree(p.get("degreeInSign")))
                    .append(" · H").append(text(p.get("natalHouse")));
            if (Boolean.TRUE.equals(p.get("retrograde"))) out.append(" · R");
        }
        return out.toString();
    }

    public static String gocharHighlights(FortuneFacts facts) {
        StringBuilder out = new StringBuilder();
        Object conjunctions = facts.detail("transitConjunctionsToNatal");
        if (conjunctions instanceof List) {
            for (Object item : (List<?>) conjunctions) {
                if (!(item instanceof Map)) continue;
                Map<?, ?> c = (Map<?, ?>) item;
                if (out.length() > 0) out.append("\n");
                out.append("合相｜")
                        .append(text(c.get("transitPlanet")))
                        .append(" → 本命 ")
                        .append(text(c.get("natalPlanet")))
                        .append(" · H").append(text(c.get("natalHouse")))
                        .append(" · ").append(text(c.get("separationDegrees"))).append("°");
            }
        }
        Object aspects = facts.detail("transitAspectsToNatal");
        if (aspects instanceof List) {
            int added = 0;
            for (Object item : (List<?>) aspects) {
                if (!(item instanceof Map)) continue;
                Map<?, ?> a = (Map<?, ?>) item;
                Object targets = a.get("natalPlanets");
                if (!(targets instanceof List) || ((List<?>) targets).isEmpty()) continue;
                if (out.length() > 0) out.append("\n");
                out.append(text(a.get("transitPlanet")))
                        .append(" → H").append(text(a.get("toNatalHouse")))
                        .append(" · ").append(compact(targets));
                if (++added >= 8) break;
            }
        }
        return out.toString();
    }

    public static String majorTransitTimeline(FortuneFacts facts) {
        StringBuilder out = new StringBuilder();
        Object raw = facts.detail("majorTransitTimeline");
        if (!(raw instanceof List)) return "";
        int count = 0;
        for (Object item : (List<?>) raw) {
            if (!(item instanceof Map)) continue;
            Map<?, ?> e = (Map<?, ?>) item;
            if (out.length() > 0) out.append("\n");
            out.append(text(e.get("date")))
                    .append("｜")
                    .append(text(e.get("planet")))
                    .append(" ")
                    .append(text(e.get("fromSign")))
                    .append(" → ")
                    .append(text(e.get("toSign")))
                    .append(" · H")
                    .append(text(e.get("fromHouse")))
                    .append(" → H")
                    .append(text(e.get("toHouse")));
            if (Boolean.TRUE.equals(e.get("retrograde"))) out.append(" · R");
            if (++count >= 16) break;
        }
        return out.toString();
    }

    public static String importantPeriods(FortuneFacts facts) {
        StringBuilder out = new StringBuilder();
        Object raw = facts.detail("importantPeriods");
        if (!(raw instanceof List)) return "";
        for (Object item : (List<?>) raw) {
            if (!(item instanceof Map)) continue;
            Map<?, ?> p = (Map<?, ?>) item;
            if (out.length() > 0) out.append("\n");
            out.append(mapText(p, "mahadashaLord"))
                    .append("/").append(mapText(p, "lord"))
                    .append("｜").append(mapText(p, "startDate"))
                    .append(" → ").append(mapText(p, "endDate"));
        }
        return out.toString();
    }

    private static String period(Object raw) {
        if (!(raw instanceof Map)) return "目前資料範圍外";
        Map<?, ?> p = (Map<?, ?>) raw;
        String lord = text(p.get("lord"));
        if (lord.isEmpty()) return "目前資料範圍外";
        return lord + " · " + text(p.get("startDate")) + " → " + text(p.get("endDate"));
    }

    private static String mapText(Object raw, String key) {
        if (!(raw instanceof Map)) return "";
        return text(((Map<?, ?>) raw).get(key));
    }

    private static String compact(Object raw) {
        if (!(raw instanceof List)) {
            String value = text(raw);
            return value.isEmpty() ? "—" : value;
        }
        StringBuilder out = new StringBuilder();
        for (Object value : (List<?>) raw) {
            if (out.length() > 0) out.append("、");
            out.append(text(value));
        }
        return out.length() == 0 ? "—" : out.toString();
    }

    private static String formatDegree(Object raw) {
        try {
            return String.format(java.util.Locale.US, "%.2f°",
                    raw instanceof Number
                            ? ((Number) raw).doubleValue()
                            : Double.parseDouble(text(raw)));
        } catch (Exception ignored) {
            return text(raw);
        }
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
