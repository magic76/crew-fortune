package com.crewpocket.fortune;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Owns the Gemini Live teacher dialog and session lifecycle.
 * Permission handling stays in MainActivity.
 */
final class FortuneTeacherController {
    private final MainActivity host;

    private GeminiFortuneLiveSession session;
    private AlertDialog dialog;
    private TextView statusText;
    private TextView inputText;
    private TextView outputText;

    FortuneTeacherController(MainActivity host) {
        this.host = host;
    }

    void open(String initialQuestion) {
        close();
        String directQuestion =
                initialQuestion == null ? "" : initialQuestion.trim();

        LinearLayout body = host.column();
        body.setPadding(
                host.dp(14), host.dp(14), host.dp(14), host.dp(12));
        body.setBackground(host.roundBorder(
                MainActivity.CARD,
                Color.rgb(92, 73, 127),
                24,
                1));

        TextView title = host.text(
                "命理老師", 22, MainActivity.TEXT, true);
        body.addView(title);

        TextView hint = host.text(
                directQuestion.isEmpty()
                        ? "老師會用 35–60 秒補充最重要的重點。想插話時按「我要問」，老師會立刻停下來聽你說。"
                        : "已經把你點的問題帶給老師，會直接回答，不會重新從頭介紹命盤。",
                13,
                MainActivity.MUTED,
                false);
        hint.setLineSpacing(host.dp(3), 1f);
        body.addView(hint, host.marginTop(6));

        statusText = host.text(
                "正在準備…", 13, MainActivity.ACCENT, true);
        body.addView(statusText, host.marginTop(9));

        TextView youLabel = host.text(
                "你剛剛說", 11, MainActivity.GOLD, true);
        body.addView(youLabel, host.marginTop(6));
        inputText = host.text(
                directQuestion.isEmpty() ? "—" : directQuestion,
                14,
                MainActivity.TEXT,
                false);
        inputText.setLineSpacing(host.dp(2), 1f);
        body.addView(inputText, host.marginTop(4));

        TextView teacherLabel = host.text(
                "老師正在講", 11, MainActivity.GOLD, true);
        body.addView(teacherLabel, host.marginTop(9));
        outputText = host.text(
                "等待老師上線…", 15, MainActivity.TEXT, false);
        outputText.setLineSpacing(host.dp(2), 1f);
        body.addView(outputText, host.marginTop(4));

        LinearLayout actions = new LinearLayout(host);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button interrupt = host.secondaryButton("我要問");
        interrupt.setTextColor(Color.rgb(30, 22, 46));
        interrupt.setBackground(host.round(MainActivity.ACCENT, 14));
        interrupt.setOnClickListener(v -> {
            OperationLog.add(host, "TEACHER_INTERRUPT", "");
            GeminiFortuneLiveSession current = session;
            if (current != null) current.interrupt();
        });
        LinearLayout.LayoutParams interruptLp =
                new LinearLayout.LayoutParams(0, host.dp(44), 1f);
        interruptLp.rightMargin = host.dp(6);
        actions.addView(interrupt, interruptLp);

        Button close = host.secondaryButton("結束");
        close.setOnClickListener(v -> {
            AlertDialog current = dialog;
            if (current != null) current.dismiss();
        });
        actions.addView(
                close,
                new LinearLayout.LayoutParams(0, host.dp(44), 1f));
        body.addView(actions, host.marginTop(11));

        dialog = new AlertDialog.Builder(host)
                .setView(body)
                .create();
        dialog.setOnDismissListener(value -> closeSessionOnly());
        dialog.show();
        host.styleDarkDialog(dialog);

        FortuneMode mode = host.aiModeForController();
        AiStyle style = host.aiStyleForController();
        FortuneFacts facts = host.rendererFacts();

        String teacherPrompt = FortuneTeacherPrompt.systemPrompt(
                mode,
                style,
                facts,
                "");
        String opening = directQuestion.isEmpty()
                ? FortuneTeacherPrompt.openingPrompt("")
                : "使用者剛剛點選追問：「" + directQuestion + "」。"
                + "不要做一般開場，也不要重複書面報告，直接回答這個問題。"
                + "先給白話結論，再講 2–4 個 deterministicFacts 裡的具體依據，"
                + "最後補一句可以繼續追問的方向。";

        OperationLog.add(
                host,
                "TEACHER_START",
                mode.name() + " · media_audio");

        session = new GeminiFortuneLiveSession(
                host,
                AppConfig.getGeminiApiKey(host),
                FortuneTeacherPrompt.voiceName(style),
                teacherPrompt,
                opening,
                new GeminiFortuneLiveSession.Listener() {
                    @Override public void onStatus(String status) {
                        host.runOnUiThread(() -> {
                            if (statusText != null) statusText.setText(status);
                        });
                    }

                    @Override public void onReady() {
                        OperationLog.add(host, "TEACHER_READY", "");
                        host.runOnUiThread(() -> {
                            if (statusText != null) {
                                statusText.setText("老師正在看你的命盤…");
                            }
                        });
                    }

                    @Override public void onInputTranscript(String textValue) {
                        OperationLog.add(
                                host,
                                "TEACHER_USER_SPOKE",
                                "chars=" + (textValue == null ? 0 : textValue.length()));
                        host.runOnUiThread(() -> {
                            if (inputText != null) inputText.setText(textValue);
                        });
                    }

                    @Override public void onOutputTranscript(String textValue) {
                        OperationLog.add(
                                host,
                                "TEACHER_REPLIED",
                                "chars=" + (textValue == null ? 0 : textValue.length()));
                        host.runOnUiThread(() -> {
                            if (outputText != null) outputText.setText(textValue);
                        });
                    }

                    @Override public void onSpeakingChanged(boolean speaking) {
                        host.runOnUiThread(() -> {
                            if (statusText != null) {
                                statusText.setText(
                                        speaking
                                                ? "老師正在講…"
                                                : "你可以直接追問");
                            }
                        });
                    }

                    @Override public void onError(String message) {
                        OperationLog.add(
                                host,
                                "TEACHER_ERROR",
                                message == null ? "unknown" : message);
                        host.runOnUiThread(() -> {
                            if (statusText != null) {
                                statusText.setText("語音老師暫時無法使用");
                            }
                            if (outputText != null) outputText.setText(message);
                        });
                    }
                });
        session.start();
    }

    void close() {
        AlertDialog currentDialog = dialog;
        dialog = null;
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.setOnDismissListener(null);
            currentDialog.dismiss();
        }
        closeSessionOnly();
    }

    private void closeSessionOnly() {
        GeminiFortuneLiveSession current = session;
        if (current != null) {
            OperationLog.add(host, "TEACHER_STOP", "");
        }
        session = null;
        if (current != null) {
            try { current.close(); } catch (Exception ignored) {}
        }
        statusText = null;
        inputText = null;
        outputText = null;
    }
}
