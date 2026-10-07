package com.crewpocket.fortune;

import android.app.AlertDialog;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.magic76.crew.agent.AgentEvent;
import com.magic76.crew.agent.AgentHarness;

import org.json.JSONObject;

final class FortuneFollowUpController {
    private final MainActivity host;
    private final StringBuilder buffer = new StringBuilder();
    private AgentHarness activeHarness;
    private AlertDialog loadingDialog;

    FortuneFollowUpController(MainActivity host) {
        this.host = host;
    }

    boolean isRunning() {
        return activeHarness != null;
    }

    void ask(String question) {
        String value = question == null ? "" : question.trim();
        if (value.isEmpty()) return;
        if (host.rendererFacts() == null) {
            Toast.makeText(host, "目前沒有可追問的命盤資料", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!FortuneTextModelSession.canGenerate(host)) {
            Toast.makeText(
                    host,
                    "文字老師目前尚未連線，命盤結果仍可正常閱讀。",
                    Toast.LENGTH_LONG).show();
            return;
        }

        close();
        synchronized (buffer) {
            buffer.setLength(0);
        }
        showLoading(value);
        OperationLog.add(host, "TEXT_FOLLOWUP_START", "chars=" + value.length());

        try {
            FortuneTextModelSession session = new FortuneTextModelSession(
                    host,
                    host.aiStyleForController().temperature(),
                    false);
            activeHarness = FortuneAgentRuntime.createFollowUp(
                    session,
                    new AgentHarness.Listener() {
                        @Override public void onAgentEvent(AgentEvent event) {
                            if (event == null) return;
                            switch (event.type()) {
                                case MODEL_TEXT:
                                    synchronized (buffer) {
                                        buffer.append(event.text());
                                    }
                                    break;
                                case TURN_COMPLETED:
                                    finish(value);
                                    break;
                                case ERROR:
                                    fail(event.error());
                                    break;
                                default:
                                    break;
                            }
                        }
                    },
                    host.aiStyleForController());
            activeHarness.start();
            activeHarness.submitText(buildRequest(value));
        } catch (Exception error) {
            fail(error);
        }
    }

    void close() {
        AgentHarness harness = activeHarness;
        activeHarness = null;
        if (harness != null) {
            try { harness.close(); } catch (Exception ignored) {}
        }
        AlertDialog dialog = loadingDialog;
        loadingDialog = null;
        if (dialog != null && dialog.isShowing()) {
            try { dialog.dismiss(); } catch (Exception ignored) {}
        }
    }

    private String buildRequest(String question) {
        FortuneFacts facts = host.rendererFacts();
        String factsJson = new JSONObject(facts.details).toString();
        StringBuilder out = new StringBuilder();
        out.append("mode=").append(host.aiModeForController().name()).append('\n');
        out.append("basis=").append(facts.basis).append('\n');
        out.append("deterministicFacts=").append(factsJson).append('\n');
        out.append("question=").append(question);
        return out.toString();
    }

    private void finish(String question) {
        final String answer;
        synchronized (buffer) {
            answer = buffer.toString().trim();
        }
        host.runOnUiThread(() -> {
            dismissLoading();
            if (answer.isEmpty()) {
                Toast.makeText(host, "老師沒有回傳內容，請再試一次", Toast.LENGTH_SHORT).show();
            } else {
                OperationLog.add(
                        host,
                        "TEXT_FOLLOWUP_SUCCESS",
                        "chars=" + answer.length());
                showAnswer(question, answer);
            }
            closeHarnessOnly();
        });
    }

    private void fail(Throwable error) {
        host.runOnUiThread(() -> {
            OperationLog.add(
                    host,
                    "TEXT_FOLLOWUP_FAILED",
                    host.safeErrorMessage(error));
            dismissLoading();
            Toast.makeText(
                    host,
                    "文字老師暫時無法回答，請稍後再試。",
                    Toast.LENGTH_LONG).show();
            closeHarnessOnly();
        });
    }

    private void showLoading(String question) {
        LinearLayout panel = host.column();
        panel.setPadding(host.dp(16), host.dp(16), host.dp(16), host.dp(14));
        panel.setBackground(host.roundBorder(
                MainActivity.CARD,
                FortuneTheme.LINE,
                20,
                1));

        panel.addView(host.text(
                "正在看這個問題",
                18,
                MainActivity.TEXT,
                true));

        TextView questionView = host.text(
                question,
                13,
                MainActivity.ACCENT,
                true);
        questionView.setLineSpacing(host.dp(2), 1f);
        panel.addView(questionView, host.marginTop(6));

        ProgressBar progress = new ProgressBar(host);
        LinearLayout.LayoutParams progressLp =
                new LinearLayout.LayoutParams(host.dp(34), host.dp(34));
        progressLp.gravity = Gravity.CENTER_HORIZONTAL;
        progressLp.topMargin = host.dp(18);
        panel.addView(progress, progressLp);

        TextView hint = host.text(
                "正在用這份命盤的既有資料整理白話答案…",
                11,
                MainActivity.MUTED,
                false);
        hint.setGravity(Gravity.CENTER);
        panel.addView(hint, host.marginTop(8));

        loadingDialog = new AlertDialog.Builder(host)
                .setView(panel)
                .create();
        loadingDialog.setOnCancelListener(dialog -> closeHarnessOnly());
        loadingDialog.show();
        host.styleDarkDialog(loadingDialog);
    }

    private void showAnswer(String question, String answer) {
        LinearLayout panel = host.column();
        panel.setPadding(host.dp(16), host.dp(16), host.dp(16), host.dp(14));
        panel.setBackground(host.roundBorder(
                MainActivity.CARD,
                FortuneTheme.LINE,
                20,
                1));

        panel.addView(host.text(
                "老師的白話回答",
                20,
                MainActivity.TEXT,
                true));

        TextView questionView = host.text(
                question,
                13,
                MainActivity.ACCENT,
                true);
        questionView.setLineSpacing(host.dp(2), 1f);
        panel.addView(questionView, host.marginTop(6));

        TextView answerView = host.text(
                answer,
                14,
                MainActivity.TEXT,
                false);
        answerView.setLineSpacing(host.dp(4), 1f);
        answerView.setPadding(
                host.dp(12), host.dp(11),
                host.dp(12), host.dp(11));
        answerView.setBackground(host.roundBorder(
                FortuneTheme.SURFACE_ALT,
                FortuneTheme.LINE,
                15,
                1));

        ScrollView scroll = new ScrollView(host);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(answerView);
        LinearLayout.LayoutParams scrollLp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        host.dp(420));
        scrollLp.topMargin = host.dp(10);
        panel.addView(scroll, scrollLp);

        Button close = host.secondaryButton("關閉");
        panel.addView(close, host.fixedHeightTop(44, 10));

        final AlertDialog dialog = new AlertDialog.Builder(host)
                .setView(panel)
                .create();
        close.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        host.styleDarkDialog(dialog);
    }

    private void dismissLoading() {
        AlertDialog dialog = loadingDialog;
        loadingDialog = null;
        if (dialog != null && dialog.isShowing()) {
            try { dialog.dismiss(); } catch (Exception ignored) {}
        }
    }

    private void closeHarnessOnly() {
        AgentHarness harness = activeHarness;
        activeHarness = null;
        if (harness != null) {
            try { harness.close(); } catch (Exception ignored) {}
        }
    }
}
