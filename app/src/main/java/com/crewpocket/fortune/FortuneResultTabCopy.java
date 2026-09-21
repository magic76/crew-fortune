package com.crewpocket.fortune;

/** Copy-only policy for the unified five-tab result information architecture. */
final class FortuneResultTabCopy {
    private FortuneResultTabCopy() {}

    static String overviewHint(FortuneMode mode) {
        if (mode == FortuneMode.BA_ZI) {
            return "本命＝看出生底盤　｜　流年＝看大運與逐年節奏　｜　解讀＝完整文字報告";
        }
        if (mode == FortuneMode.TAROT_NUMEROLOGY) {
            return "本命＝看核心數字與出生牌　｜　流年＝看年度／月份循環　｜　解讀＝完整文字報告";
        }
        return "本命＝看出生底盤　｜　流年＝看現在與未來節奏　｜　解讀＝完整文字報告";
    }

    static String compactInterpretation(String value, String fallback) {
        String source = value == null || value.trim().isEmpty() ? fallback : value;
        String clean = source == null ? "" : source.replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (clean.length() <= 170) return clean;
        return clean.substring(0, 170) + "…\n完整內容請看「解讀」。";
    }

    static String description(FortuneMode mode, int tab) {
        if (mode == FortuneMode.BA_ZI) {
            switch (tab) {
                case 1:
                    return "這頁先用白話說明性格、工作資源與關係模式，再往下看四柱、五行、十神、旺衰與合沖依據。";
                case 2:
                    return "這頁可看：目前大運、逐年流年、值得留意年份，以及每段時間的白話主題。";
                case 3:
                    return "這頁只抓財運、工作、感情的關鍵依據與短摘要；完整長文集中在「解讀」。";
                case 4:
                    return "這頁就是完整文字報告；不開語音也能讀完主要解讀。";
                default:
                    return "這頁先抓日主、旺衰、目前大運與今年流年，再看最重要的白話重點。";
            }
        }
        if (mode == FortuneMode.TAROT_NUMEROLOGY) {
            switch (tab) {
                case 1:
                    return "這頁先解釋內在／外在人格、生命道路與工作資源傾向，再往下看出生牌、巔峰、挑戰與週期資料。";
                case 2:
                    return "這頁可看：未來幾年個人流年、今年 12 個個人月，以及人生階段節奏。";
                case 3:
                    return "這頁只看個性、工作、資源、感情的依據、時間與短摘要；完整長文集中在「解讀」。";
                case 4:
                    return "這頁就是完整文字報告；語音老師只負責補充與追問。";
                default:
                    return "這頁先看外在人格牌、內在靈魂牌、生命道路與目前流年。";
            }
        }
        switch (tab) {
            case 1:
                return "這頁先給本命白話重點，再往下看 Lagna、Moon、Sun、九曜、12 宮、宮主、Nakshatra 與行星互動。";
            case 2:
                return "這頁先給目前週期的文字解讀，再往下看 Mahadasha / Antardasha、Gochar、Dasha × Gochar 與未來 3 年行運。";
            case 3:
                return "這頁只看個性、工作、財務、感情、家庭的命盤依據、時間證據與短摘要；完整長文集中在「解讀」。";
            case 4:
                return "這頁就是完整文字報告；語音老師只負責把內容講得更口語、或回答追問。";
            default:
                return "這頁先看核心命盤身份、目前人生週期，以及最值得先理解的幾個重點。";
        }
    }
}
