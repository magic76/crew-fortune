package com.crewpocket.fortune;

import com.magic76.crew.agent.AgentEvent;
import com.magic76.crew.agent.AgentHarness;

import org.json.JSONObject;

final class FortuneAiController {
    private final MainActivity host;
    private final StringBuilder buffer = new StringBuilder();
    private AgentHarness activeHarness;

    FortuneAiController(MainActivity host) {
        this.host = host;
    }

    void start(FortuneProfile profile) {
        host.updateAiLoadingStage(0);
        OperationLog.add(
                host,
                "AI_INTERPRETATION_START",
                host.aiModeForController().name()
                        + " · " + host.aiStyleForController().name());
        startAttempt(profile, 0, "");
    }

    boolean isRunning() {
        return activeHarness != null;
    }

    void close() {
        AgentHarness harness = activeHarness;
        activeHarness = null;
        if (harness != null) {
            try { harness.close(); } catch (Exception ignored) {}
        }
    }

    private void startAttempt(
            FortuneProfile profile,
            int attempt,
            String retryReason) {
        synchronized (buffer) {
            buffer.setLength(0);
        }

        try {
            final AiStyle style = host.aiStyleForController();
            final FortuneTextModelSession session =
                    new FortuneTextModelSession(
                            host,
                            style.temperature());

            activeHarness = FortuneAgentRuntime.createInterpretation(
                    session,
                    new AgentHarness.Listener() {
                        @Override public void onAgentEvent(AgentEvent event) {
                            if (event == null) return;
                            switch (event.type()) {
                                case MODEL_TEXT:
                                    if (host.aiLoadingStageForController() < 1) {
                                        host.runOnUiThread(() ->
                                                host.updateAiLoadingStage(1));
                                    }
                                    synchronized (buffer) {
                                        buffer.append(event.text());
                                    }
                                    break;
                                case TURN_COMPLETED:
                                    handleCompleted(profile, attempt, session, style);
                                    break;
                                case ERROR:
                                    OperationLog.add(
                                            host,
                                            "AI_INTERPRETATION_FAILED",
                                            "model_error · attempt=" + (attempt + 1)
                                                    + " · model=" + session.lastModel()
                                                    + " · models=" + session.lastModelAttempts()
                                                    + " · finishReason=" + session.lastFinishReason()
                                                    + " · error="
                                                    + host.safeErrorMessage(event.error()));
                                    fallback(
                                            "AI 命理老師暫時無法完成解讀，已保留本地完整結果。");
                                    close();
                                    break;
                                default:
                                    break;
                            }
                        }
                    },
                    style);

            activeHarness.start();
            activeHarness.submitText(
                    buildRequest(attempt, retryReason));
        } catch (Exception error) {
            OperationLog.add(
                    host,
                    "AI_INTERPRETATION_FAILED",
                    "startup_error: " + host.safeErrorMessage(error)
                            + " · attempt=" + (attempt + 1));
            fallback("AI 模式啟動失敗，已使用本地結果。");
            close();
        }
    }

    private void handleCompleted(
            FortuneProfile profile,
            int attempt,
            FortuneTextModelSession session,
            AiStyle style) {
        final String completed;
        synchronized (buffer) {
            completed = buffer.toString().trim();
        }

        final AiFortuneCopy parsed;
        try {
            parsed = AiFortuneCopy.parse(completed);
        } catch (IllegalArgumentException parseError) {
            String retryCause = "parse_error: "
                    + host.safeErrorMessage(parseError)
                    + " · finishReason=" + session.lastFinishReason();
            String detail = retryCause
                    + " · attempt=" + (attempt + 1)
                    + " · " + safeResponseSummary(completed);
            OperationLog.add(host, "AI_INTERPRETATION_PARSE_ERROR", detail);

            if (attempt == 0) {
                retry(profile, retryCause, detail);
                return;
            }

            OperationLog.add(host, "AI_INTERPRETATION_FAILED", detail);
            fallback("AI 命理老師回覆格式仍不完整，已保留本地完整結果。");
            close();
            return;
        }

        String qualityIssues =
                parsed.qualityIssueSummary(host.aiModeForController());
        if (!qualityIssues.isEmpty() && attempt == 0) {
            String retryCause = "quality_short: " + qualityIssues;
            String detail = retryCause
                    + " · finishReason=" + session.lastFinishReason()
                    + " · " + safeResponseSummary(completed);
            retry(profile, retryCause, detail);
            return;
        }

        if (!qualityIssues.isEmpty()) {
            OperationLog.add(
                    host,
                    "AI_INTERPRETATION_QUALITY_WARNING",
                    qualityIssues);
        }

        host.runOnUiThread(() -> {
            OperationLog.add(
                    host,
                    "AI_INTERPRETATION_SUCCESS",
                    style.name()
                            + " · attempt=" + (attempt + 1)
                            + " · chars=" + completed.length());
            host.onAiCopyReady(parsed);
        });
        close();
    }

    private void retry(
            FortuneProfile profile,
            String retryCause,
            String detail) {
        OperationLog.add(host, "AI_INTERPRETATION_RETRY", detail);
        host.runOnUiThread(() -> host.updateAiLoadingStage(2));
        close();
        host.runOnUiThread(() ->
                startAttempt(profile, 1, retryCause));
    }

    private void fallback(String message) {
        host.runOnUiThread(() -> host.onAiFallback(message));
    }

    private String buildRequest(
            int attempt,
            String retryReason) {
        FortuneFacts facts = host.rendererFacts();
        if (facts == null) {
            throw new IllegalStateException("deterministic facts are missing");
        }

        String factsJson = new JSONObject(facts.details).toString();
        StringBuilder value = new StringBuilder();
        value.append("請只解讀以下已由本機完成的 deterministic facts。")
                .append("不要重新計算，不要呼叫工具，不要修正輸入格式，也絕對不要使用預設值。\n");
        value.append("mode=")
                .append(host.aiModeForController().name()).append('\n');
        value.append("basis=").append(facts.basis).append('\n');
        value.append("aiStyle=")
                .append(host.aiStyleForController().name()).append('\n');
        value.append("deterministicFacts=")
                .append(factsJson).append('\n');

        if (attempt > 0) {
            value.append("RETRY_MODE=JSON_REPAIR_AND_EXPANSION\n");
            value.append("上一次回覆不符合格式或內容過短。原因摘要：")
                    .append(retryReason == null ? "" : retryReason)
                    .append('\n');
            value.append("這次必須重新輸出一個完整、可解析的 JSON object。")
                    .append("不得輸出 markdown code fence、前言、後記或任何 JSON 外文字。")
                    .append("不得省略 title, overview, personality, career, wealth, relationships, family, ")
                    .append("currentCycle, longTerm, keyYears, topTraits, topTraitEvidence, followUps, translation, punchline, advice, shareText。")
                    .append("不要縮短內容來逃避欄位要求。\n");
        }

        value.append("creativeVariant=").append(System.nanoTime()).append('\n');
        value.append("creativeVariant 只允許改變措辭與笑點。")
                .append("所有數字、干支、十神、大運、流年、塔羅牌、天賦數、行星、宮位、Nakshatra、Dasha")
                .append("都必須逐字遵守 deterministicFacts。");
        return value.toString();
    }

    private String safeResponseSummary(String raw) {
        String source = raw == null ? "" : raw;
        String clean = source
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\d{4}-\\d{2}-\\d{2}", "[date]")
                .replaceAll("(?<!\\d)\\d{1,2}:\\d{2}(?!\\d)", "[time]")
                .replaceAll("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}", "[email]")
                .replaceAll("(?<!\\d)\\d{8,15}(?!\\d)", "[number]")
                .replaceAll("[A-Za-z0-9_\\-]{24,}", "[token]")
                .replaceAll("\\s+", " ")
                .trim();
        String preview = clean.length() <= 300
                ? clean
                : clean.substring(0, 300) + "…";
        return "len=" + source.length() + " · preview=" + preview;
    }
}
