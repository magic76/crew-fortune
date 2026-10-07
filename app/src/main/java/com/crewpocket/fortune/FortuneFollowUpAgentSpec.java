package com.crewpocket.fortune;

import com.magic76.crew.agent.AgentSpec;
import com.magic76.crew.agent.ToolSpec;

import java.util.Collections;
import java.util.List;

/** Grounded plain-text follow-up interpreter for an already calculated reading. */
final class FortuneFollowUpAgentSpec implements AgentSpec {
    private final AiStyle style;

    FortuneFollowUpAgentSpec(AiStyle style) {
        this.style = style == null ? AiStyle.NORMAL : style;
    }

    @Override public String id() {
        return "crew-fortune-follow-up";
    }

    @Override public String systemPrompt() {
        String tone;
        switch (style) {
            case STRICT:
                tone = "語氣精準、克制，先結論，再列證據與實際含義。";
                break;
            case FUNNY:
                tone = "可以有一句具體但不傷人的乾式幽默，但主要內容仍要清楚、認真。";
                break;
            case NORMAL:
            default:
                tone = "使用自然白話，像熟悉命盤的顧問直接回答，不要故作玄妙。";
                break;
        }

        return "你是 Crew Fortune 的文字命理老師。"
                + "使用者訊息會包含已由本機完成的 deterministicFacts 與一個問題。"
                + "只能使用這些 facts 回答，不得重新排盤、不得修正輸入、不得補預設值、不得發明不存在的年份、行星、宮位、十神、流年或事件。"
                + "若資料不足，要直接說目前命盤資料不足以支持那個結論。"
                + "回答使用繁體中文，先用 1 到 2 句直接回答，再用 2 到 4 個具體命盤依據說明，最後補一段實際生活上的意思。"
                + "一般使用者不需要懂術語：八字十神、合沖、印度占星 Lagna、Dasha、Gochar、Nakshatra 等術語第一次出現時要立刻翻成白話。"
                + "不要輸出 JSON、markdown code fence 或原始 facts dump。"
                + "不要把命理說成確定事實，也不要預測死亡、嚴重疾病、懷孕必然、犯罪或災難。"
                + tone;
    }

    @Override public List<ToolSpec> tools() {
        return Collections.emptyList();
    }
}
