package com.crewpocket.fortune;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FortuneLocalReport {
    private FortuneLocalReport() {}

    public static LinkedHashMap<String, String> sections(FortuneFacts facts) {
        return facts != null && facts.mode == FortuneMode.BA_ZI
                ? bazi(facts)
                : tarot(facts);
    }

    private static LinkedHashMap<String, String> bazi(FortuneFacts f) {
        LinkedHashMap<String, String> out = new LinkedHashMap<String, String>();
        String master = f.detailText("dayMaster") + f.detailText("dayMasterElement");
        String strength = f.detailText("dayMasterStrength");
        String interactions = compactList(f.detail("natalInteractions"), 3);
        String gods = topGods(f.detail("tenGodDistribution"));

        out.put("命局總覽",
                "四柱為「" + f.detailText("fourPillars") + "」，日主是 " + master
                        + "，簡化旺衰模型落在「" + strength + "」。"
                        + " 加權五行為 " + f.detailText("weightedFiveElements") + "。"
                        + " 十神較突出的部分是 " + gods + "。"
                        + " 命局主要合沖訊號：" + interactions + "。");

        out.put("性格與天賦",
                elementPersonality(f.detailText("dayMasterElement"))
                        + " 日主目前屬「" + strength + "」，因此同一種特質可能表現成優勢，也可能在壓力下放大成盲點。"
                        + godPersonality(f.detail("tenGodDistribution")));

        int output = godCount(f.detail("tenGodDistribution"), "食神", "伤官");
        int wealth = godCount(f.detail("tenGodDistribution"), "正财", "偏财");
        int officer = godCount(f.detail("tenGodDistribution"), "正官", "七杀");
        int resource = godCount(f.detail("tenGodDistribution"), "正印", "偏印");
        int peer = godCount(f.detail("tenGodDistribution"), "比肩", "劫财");
        out.put("工作與財務",
                "十神群組中：食傷 " + output + "、財星 " + wealth + "、官殺 " + officer
                        + "、印星 " + resource + "、比劫 " + peer + "。"
                        + workInterpretation(output, wealth, officer, resource, peer)
                        + " 這裡描述的是偏好與結構，不代表收入高低或投資結果。");

        out.put("感情與人際",
                "日支所見十神為 " + mapValue(f.detail("tenGods"), "dayBranch")
                        + "，可當作親密關係與日常互動的一個觀察角度。"
                        + " 命局內的合沖刑害為：" + interactions + "。"
                        + " 有合不等於一定和諧、有沖也不等於一定衝突，實際更像是互動張力與議題容易被觸發的位置。");

        out.put("目前大運與流年",
                currentLuckText(f) + " "
                        + currentAnnualText(f)
                        + " 流年只是一層時間訊號，應和本命、大運及現實環境一起看。");

        out.put("解讀邊界",
                "目前的「旺衰」是以天干、藏干與月令加權的簡化模型；"
                        + "「平衡參考元素」為 " + f.detailText("balancingElements")
                        + "，不直接宣稱是唯一喜用神。正式格局、調候與用神在不同八字流派會有不同取法。");
        return out;
    }

    private static LinkedHashMap<String, String> tarot(FortuneFacts f) {
        LinkedHashMap<String, String> out = new LinkedHashMap<String, String>();
        int life = intValue(f.detail("lifePathNumber"));
        int soul = intValue(f.detail("soulUrgeNumber"));
        int personality = intValue(f.detail("personalityNumber"));
        int expression = intValue(f.detail("expressionNumber"));
        String[] meanings = numberMeaning(life);
        String[] soulMeanings = numberMeaning(soul);
        String[] personalityMeanings = numberMeaning(personality);
        String[] expressionMeanings = numberMeaning(expression);
        String cards = f.detailText("birthCardDisplay");

        out.put("核心總覽",
                "生命道路數是 " + f.detailText("lifePathDisplay")
                        + "，表達／命運數是 " + f.detailText("expressionDisplay")
                        + "，出生牌組是「" + cards + "」。"
                        + " 生日數 " + f.detailText("birthdayNumber")
                        + "、態度數 " + f.detailText("attitudeNumber")
                        + "，提供核心人生主題之外的日常表現角度。"
                        + " " + meanings[0]
                        + " 表達數則補充你比較自然會拿什麼能力去處理人生：" + expressionMeanings[0]);

        out.put("內在 vs 外在",
                "靈魂數（內在）是 " + f.detailText("soulUrgeDisplay")
                        + "，人格數（外在）是 " + f.detailText("personalityDisplay") + "。"
                        + " 內在層：" + soulMeanings[1]
                        + " 外在層：" + personalityMeanings[1]
                        + " 當兩個數字差異明顯時，常見情況是「自己真正需要的」和「別人第一眼認識的你」不是同一套介面。");

        out.put("性格與天賦",
                meanings[1]
                        + " 表達／命運數 " + f.detailText("expressionDisplay")
                        + " 的能力方向：" + expressionMeanings[1]
                        + " 出生牌組的關鍵字為：" + birthCardKeywords(f.detail("birthCards"))
                        + "。生命道路描述長期主題，姓名數字補充內在動機、外在印象與自然表達方式。");

        out.put("工作與財務",
                meanings[2]
                        + " 表達／命運數 " + f.detailText("expressionDisplay")
                        + " 的工作傾向可補充為：" + expressionMeanings[2]
                        + " 四大巔峰數為 " + f.detailText("pinnacles")
                        + "，代表不同人生階段會反覆換重點。"
                        + " 這些數字適合拿來看工作方式與資源運用偏好，不應當成職業或投資決策的單一依據。");

        out.put("感情與人際",
                meanings[3]
                        + " 內在靈魂數 " + f.detailText("soulUrgeDisplay")
                        + " 與外在人格數 " + f.detailText("personalityDisplay")
                        + " 的落差，是關係中很值得觀察的一層。"
                        + " 態度數 " + f.detailText("attitudeNumber")
                        + " 比較接近別人第一時間感受到的你；四大挑戰數 "
                        + f.detailText("challenges")
                        + " 則可視為反覆出現的人際與自我調整題目。");

        out.put("目前週期",
                "目前個人年為 " + f.detailText("personalYear")
                        + "，個人月為 " + f.detailText("personalMonth") + "。"
                        + personalYearMeaning(intValue(f.detail("personalYear")))
                        + " 四大巔峰時程為 " + f.detailText("pinnacleTiming")
                        + "，用來看長週期；個人年與個人月則偏向短期節奏。");

        out.put("解讀邊界",
                "塔羅生命靈數與姓名靈數都有多種流派。本 App 的姓名數採 Pythagorean 1–9："
                        + "全部字母算表達數、母音算靈魂數、子音算人格數；Y 在目前版本固定視為子音。"
                        + " 它適合當作自我反思與娛樂工具，不是事件預言。");
        return out;
    }

    private static String elementPersonality(String e) {
        if ("木".equals(e)) return "木日主常被拿來描述成重視成長、方向與延伸，優點是有推進力，盲點是容易一路長到忘了修枝。";
        if ("火".equals(e)) return "火日主常被拿來描述成重視表達、熱度與可見度，優點是能帶動氣氛，盲點是能量上來時容易過熱。";
        if ("土".equals(e)) return "土日主常被拿來描述成重視穩定、承接與實際，優點是可靠，盲點是容易把太多責任都扛在自己身上。";
        if ("金".equals(e)) return "金日主常被拿來描述成重視規則、品質與取捨，優點是判斷明確，盲點是標準高時也容易對自己太嚴。";
        if ("水".equals(e)) return "水日主常被拿來描述成重視流動、資訊與適應，優點是反應快，盲點是選項太多時容易一直繞路。";
        return "";
    }

    private static String godPersonality(Object source) {
        int output = godCount(source, "食神", "伤官");
        int resource = godCount(source, "正印", "偏印");
        int peer = godCount(source, "比肩", "劫财");
        if (output >= resource && output >= peer) {
            return " 食傷訊號較明顯時，常見主題是表達、產出、創意與把想法做成看得見的東西。";
        }
        if (resource >= peer) {
            return " 印星訊號較明顯時，常見主題是學習、理解、吸收與建立自己的知識系統。";
        }
        return " 比劫訊號較明顯時，常見主題是自主、競爭、同儕互動與自己掌握節奏。";
    }

    private static String workInterpretation(int output, int wealth, int officer, int resource, int peer) {
        int max = Math.max(Math.max(output, wealth), Math.max(officer, Math.max(resource, peer)));
        if (max == output) return " 表達與產出訊號較強，適合把能力轉成作品、方案或可交付成果。";
        if (max == wealth) return " 財星訊號較強，容易把注意力放在結果、資源配置與實際回報。";
        if (max == officer) return " 官殺訊號較強，對責任、標準、壓力與位置感通常比較敏感。";
        if (max == resource) return " 印星訊號較強，學習、研究、證據與專業累積通常是重要優勢。";
        return " 比劫訊號較強，自主性、競爭感與夥伴關係通常會直接影響工作狀態。";
    }

    private static String currentLuckText(FortuneFacts f) {
        Object current = f.detail("currentLuckPillar");
        if (!(current instanceof Map)) return "目前尚未落入第一步正式大運。";
        return "目前大運為 " + mapValue(current, "ganZhi")
                + "（" + mapValue(current, "startAge") + "–" + mapValue(current, "endAge")
                + "歲，" + mapValue(current, "startYear") + "–" + mapValue(current, "endYear") + "）。";
    }

    private static String currentAnnualText(FortuneFacts f) {
        Object annual = f.detail("currentAnnual");
        if (!(annual instanceof Map)) return "";
        return "今年流年 " + mapValue(annual, "ganZhi")
                + "，相對日主的十神為 " + mapValue(annual, "tenGod")
                + "，流年五行為 " + mapValue(annual, "element")
                + "；與本命互動：" + compactList(((Map<?, ?>) annual).get("interactions"), 3) + "。";
    }

    private static String topGods(Object source) {
        if (!(source instanceof Map)) return "資料不足";
        String first = "";
        String second = "";
        int a = -1, b = -1;
        for (Map.Entry<?, ?> e : ((Map<?, ?>) source).entrySet()) {
            int value = intValue(e.getValue());
            if (value > a) {
                second = first; b = a;
                first = String.valueOf(e.getKey()) + " " + value; a = value;
            } else if (value > b) {
                second = String.valueOf(e.getKey()) + " " + value; b = value;
            }
        }
        return first + (second.isEmpty() ? "" : "、" + second);
    }

    private static int godCount(Object source, String... names) {
        if (!(source instanceof Map)) return 0;
        int total = 0;
        Map<?, ?> map = (Map<?, ?>) source;
        for (String name : names) total += intValue(map.get(name));
        return total;
    }

    private static String[] numberMeaning(int n) {
        int core = n;
        if (core == 11) return new String[]{"主題偏向直覺、靈感與把感受到的東西傳出去。","你通常對氣氛與細節比較敏感，優勢是感受深，盲點是容易過度接收。","工作上適合把洞察轉成溝通、創意或協助別人的能力。","關係裡需要被理解，也需要練習把感受說清楚，而不是期待別人自動接收到。"};
        if (core == 22) return new String[]{"主題偏向把大型想法落成現實。","你容易同時看到願景與結構，優勢是能建系統，盲點是壓力也容易跟著放大。","工作上適合長期建設、整合資源與把複雜事情做成可持續的系統。","關係裡常把責任放很前面，需要保留單純相處而不是一直管理事情的空間。"};
        if (core == 33) return new String[]{"主題偏向照顧、教導與影響他人。","你容易對別人的需要有感，優勢是能給支持，盲點是容易過度承擔。","工作上適合教育、創作、服務或需要人際影響力的場景。","關係裡要注意照顧和拯救不是同一件事，界線本身也是善意。"};
        switch (core) {
            case 1: return new String[]{"主題是獨立、開創與自我定義。","你傾向自己先動手，優勢是主動，盲點是容易什麼都自己扛。","工作上適合有自主權、能開新局或直接負責成果的環境。","關係裡需要空間，也要記得獨立不等於不說需求。"};
            case 2: return new String[]{"主題是合作、敏感度與關係。","你擅長感受他人與調整節奏，盲點是容易為了和諧壓掉自己的意見。","工作上適合協作、協調、顧問與需要細膩互動的角色。","關係裡重視互相回應，界線與直接表達會比猜測更重要。"};
            case 3: return new String[]{"主題是表達、創意與社交能量。","你需要把想法說出來或做出來，盲點是興趣很多時容易分散。","工作上適合內容、創意、溝通與需要把氣氛帶起來的場景。","關係裡幽默是優勢，但重要情緒不要只用玩笑包裝。"};
            case 4: return new String[]{"主題是結構、穩定與建立基礎。","你重視秩序與可靠，盲點是變動太快時容易先抗拒。","工作上適合流程、工程、管理與長期累積型任務。","關係裡承諾感強，但也要留一點彈性給彼此。"};
            case 5: return new String[]{"主題是自由、變化與體驗。","你對新鮮事反應快，優勢是適應，盲點是容易對重複感到不耐。","工作上適合變化高、跨域、溝通或需要快速反應的環境。","關係裡需要自由感，穩定最好建立在可談的規則上。"};
            case 6: return new String[]{"主題是責任、關係與照顧。","你對品質和他人感受有要求，盲點是容易把責任攬過頭。","工作上適合需要信任、審美、服務或照顧品質的角色。","關係裡願意付出，但不要把愛變成全包式客服。"};
            case 7: return new String[]{"主題是研究、深度與理解。","你習慣先觀察再相信，優勢是分析，盲點是容易退太遠。","工作上適合研究、技術、策略與需要深入思考的領域。","關係裡需要精神交流，也要讓對方知道你的沉默不是離線。"};
            case 8: return new String[]{"主題是成果、資源與影響力。","你對效率與結果敏感，優勢是執行，盲點是容易把自己也當 KPI。","工作上適合管理資源、商業、領導與能看見成果的任務。","關係裡不要只解決問題，也要留下單純陪伴的空間。"};
            case 9: return new String[]{"主題是整合、理想與更大的視角。","你容易看到整體與人的故事，優勢是同理，盲點是容易替太多人操心。","工作上適合創意、教育、公益或能連結不同觀點的角色。","關係裡需要接受不是所有人都要被你理解到底。"};
            default: return new String[]{"核心主題偏向平衡與自我探索。","你的特質需要放回實際經驗中觀察。","工作上以真實能力與環境匹配為主。","關係裡以直接溝通比任何數字更重要。"};
        }
    }

    private static String personalYearMeaning(int n) {
        switch (n) {
            case 1: return "這通常被視為開新局、重新定義方向的一年。";
            case 2: return "這通常被視為合作、等待與調整關係節奏的一年。";
            case 3: return "這通常被視為表達、社交與創作能量較明顯的一年。";
            case 4: return "這通常被視為打基礎、整理制度與穩定推進的一年。";
            case 5: return "這通常被視為變化、移動與嘗試新選項的一年。";
            case 6: return "這通常被視為責任、家庭與關係承諾較突出的年份。";
            case 7: return "這通常被視為內省、研究與重新理解自己的年份。";
            case 8: return "這通常被視為成果、資源與現實目標較受關注的一年。";
            case 9: return "這通常被視為整理、完成與準備下一輪週期的一年。";
            default: return "";
        }
    }

    private static String birthCardKeywords(Object value) {
        if (!(value instanceof List)) return "";
        StringBuilder out = new StringBuilder();
        for (Object item : (List<?>) value) {
            if (!(item instanceof Map)) continue;
            if (out.length() > 0) out.append("；");
            out.append(mapValue(item, "name")).append("：").append(mapValue(item, "keywords"));
        }
        return out.toString();
    }

    private static String compactList(Object value, int limit) {
        if (!(value instanceof List)) return value == null ? "" : String.valueOf(value);
        StringBuilder out = new StringBuilder();
        int count = 0;
        for (Object item : (List<?>) value) {
            if (count++ >= limit) break;
            if (out.length() > 0) out.append("；");
            out.append(String.valueOf(item));
        }
        return out.toString();
    }

    private static String mapValue(Object source, String key) {
        if (!(source instanceof Map)) return "";
        Object value = ((Map<?, ?>) source).get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static int intValue(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(String.valueOf(value)); }
        catch (Exception ignored) { return 0; }
    }
}
