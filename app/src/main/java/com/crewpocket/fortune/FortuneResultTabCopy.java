package com.crewpocket.fortune;

/** Copy-only policy for the unified five-tab result information architecture. */
final class FortuneResultTabCopy {
    private FortuneResultTabCopy() {}

    static String overviewHint(FortuneMode mode) {
        return "重點＝先看懂自己　｜　人生節奏＝看現在與未來　｜　命盤資料＝給想看專業細節的人";
    }

    static String compactInterpretation(String value, String fallback) {
        String source = value == null || value.trim().isEmpty() ? fallback : value;
        String clean = source == null ? "" : source.replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (clean.length() <= 170) return clean;
        return clean.substring(0, 170) + "…\n完整內容請看「完整解讀」。";
    }

    static String description(FortuneMode mode, int tab) {
        switch (tab) {
            case 1:
                return mode == FortuneMode.BA_ZI
                        ? "只看現在走到哪裡、未來幾年的節奏與值得留意年份；專業十神與合沖放到「命盤資料」。"
                        : mode == FortuneMode.VEDIC_ASTROLOGY
                        ? "只看目前 Dasha／Gochar、接下來的週期與值得留意時期；星盤細節放到「命盤資料」。"
                        : "只看目前流年、未來幾年與今年月份節奏；完整數字結構放到「命盤資料」。";
            case 2:
                return "直接看個性、工作、財運與感情等生活主題；先給白話結論，需要時再展開依據。";
            case 3:
                return "完整個人文字報告：把命盤、時間與生活主題串起來，不需要先學命理術語。";
            case 4:
                return mode == FortuneMode.BA_ZI
                        ? "專業命盤資料：四柱、五行、十神、旺衰與合沖。看不懂也沒關係，前面幾頁已經翻成白話。"
                        : mode == FortuneMode.VEDIC_ASTROLOGY
                        ? "專業命盤資料：Lagna、九曜、12 宮、宮主、Nakshatra 與行星互動。一般閱讀可跳過。"
                        : "專業命盤資料：出生牌、核心數字、巔峰、挑戰與週期。一般閱讀可跳過。";
            default:
                return "先用 10 秒看懂：一句話看你、3 個最像你的特徵、目前階段與接下來值得注意的事。";
        }
    }

}
