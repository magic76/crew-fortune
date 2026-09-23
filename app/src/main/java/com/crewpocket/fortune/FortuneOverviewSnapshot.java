package com.crewpocket.fortune;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the short, first-screen reading used by the overview tab.
 * Full technical data stays in Natal/Timing tabs and long-form prose stays in Interpretation.
 */
final class FortuneOverviewSnapshot {
    private FortuneOverviewSnapshot() {}

    static List<String> keyTakeaways(
            FortuneMode mode,
            FortuneFacts facts,
            AiFortuneCopy copy) {
        List<String> out = new ArrayList<String>();

        if (copy != null && copy.topTraits != null) {
            for (String value : copy.topTraits) {
                if (out.size() >= 3) break;
                String compact = compact(value, 105);
                if (!compact.isEmpty()) out.add(compact);
            }
        }
        if (out.size() >= 3) return out;

        Map<String, String> sections =
                facts == null
                        ? new LinkedHashMap<String, String>()
                        : FortuneLocalReport.sections(facts);

        String[] keys;
        if (mode == FortuneMode.BA_ZI) {
            keys = new String[] {
                    "性格與天賦",
                    "工作與財務",
                    "感情與人際"
            };
        } else if (mode == FortuneMode.VEDIC_ASTROLOGY) {
            keys = new String[] {
                    "核心總覽",
                    "工作",
                    "感情"
            };
        } else {
            keys = new String[] {
                    "內在 vs 外在",
                    "性格與天賦",
                    "工作與財務"
            };
        }

        for (String key : keys) {
            if (out.size() >= 3) break;
            String compact = compact(sections.get(key), 105);
            if (!compact.isEmpty() && !out.contains(compact)) {
                out.add(compact);
            }
        }

        while (out.size() < 3) {
            out.add("完整資料已算好；想看專業原始依據可到「命盤資料」。");
        }
        return out;
    }

    static String currentTiming(
            FortuneMode mode,
            FortuneFacts facts,
            AiFortuneCopy copy) {
        if (copy != null && copy.currentCycle != null
                && !copy.currentCycle.trim().isEmpty()) {
            return compact(copy.currentCycle, 170);
        }

        Map<String, String> sections =
                facts == null
                        ? new LinkedHashMap<String, String>()
                        : FortuneLocalReport.sections(facts);

        String value;
        if (mode == FortuneMode.BA_ZI) {
            value = sections.get("目前大運與流年");
        } else if (mode == FortuneMode.VEDIC_ASTROLOGY) {
            value = sections.get("目前 Dasha");
            String gochar = sections.get("目前 Gochar");
            if (value != null && gochar != null && !gochar.trim().isEmpty()) {
                value = value + " " + gochar;
            }
        } else {
            value = sections.get("目前週期");
        }
        return compact(value, 170);
    }

    static String compact(String value, int limit) {
        String clean = value == null ? "" : value
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (clean.length() <= limit) return clean;
        return clean.substring(0, limit) + "…";
    }
}
