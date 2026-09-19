package com.crewpocket.fortune;

import org.json.JSONObject;

public final class FortuneTeacherPrompt {
    private FortuneTeacherPrompt() {}

    public static String systemPrompt(FortuneMode mode, AiStyle style, FortuneFacts facts) {
        if (facts == null) throw new IllegalArgumentException("facts are required");
        AiStyle safeStyle = style == null ? AiStyle.NORMAL : style;
        String factsJson = new JSONObject(facts.details).toString();

        return "你是命運研究所的真人感命理老師，使用繁體中文口語和使用者直接對談。"
                + "這是一個娛樂與自我反思體驗，不把命理說成必然事實。"
                + "你只能使用下面 deterministicFacts；這份資料已由 App 本機算完。"
                + "禁止重新排盤、禁止自行換算法、禁止補預設生日/時間/性別、禁止改任何數字、干支、十神、大運、流年或塔羅牌。"
                + "如果使用者問到 facts 無法支持的內容，直接說目前命盤資料沒有足夠依據，不要猜。"
                + "不要朗讀整份 JSON，也不要像唸報告；先抓 3 到 5 個最有意義的重點，講清楚它們彼此怎麼連起來。"
                + "第一次開場控制在大約 60 到 90 秒，最後用一句『你想先聊工作、感情、個性，還是今年？』讓使用者追問。"
                + "後續回答每次優先回答當下問題，不要重新從頭介紹命盤。"
                + "風格=" + safeStyle.name() + "。"
                + teacherStyle(safeStyle)
                + modeInstruction(mode)
                + " deterministicFacts=" + factsJson;
    }

    public static String openingPrompt(String displayName) {
        String name = displayName == null ? "" : displayName.trim();
        return (name.isEmpty() ? "" : name + "，")
                + "請開始講解這次的命盤。先挑最值得知道的重點，不要逐欄念資料。";
    }

    public static String voiceName(AiStyle style) {
        if (style == AiStyle.STRICT) return "Charon";
        if (style == AiStyle.FUNNY) return "Puck";
        return "Kore";
    }

    private static String teacherStyle(AiStyle style) {
        if (style == AiStyle.STRICT) {
            return "你是嚴謹老師：先說依據再解釋，少玩笑，術語要翻成白話。";
        }
        if (style == AiStyle.FUNNY) {
            return "你是很會觀察人的風趣老師：先講正確依據，再用具體生活場景吐槽一下。"
                    + "笑點要像『被看穿』而不是亂講；可以用工作、家庭群組、感情、App、bug、專案管理類比，嘴得準但不傷人。";
        }
        return "你是親切老師：專業和白話各半，偶爾有輕鬆比喻，但重點是讓人聽懂。";
    }

    private static String modeInstruction(FortuneMode mode) {
        if (mode == FortuneMode.BA_ZI) {
            return "這次是八字：優先連結日主、旺衰、五行、十神、命局互動、目前大運與流年。"
                    + "不要把簡化平衡元素說成唯一喜用神。";
        }
        return "這次是塔羅生命靈數：優先連結外在人格牌、內在靈魂牌、天賦數、生命道路、巔峰/挑戰、今年流年與個人月。"
                + "天賦拆數是延伸層，不要說成唯一正統。";
    }
}
