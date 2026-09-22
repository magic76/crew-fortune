package com.crewpocket.fortune;

import android.app.AlertDialog;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Map;

final class FortuneDialogController {
    private final MainActivity host;

    FortuneDialogController(MainActivity host) {
        this.host = host;
    }

    void showFortuneHistory() {
        final List<FortuneHistoryEntry> entries =
                FortuneHistoryStore.list(host);

        LinearLayout panel = host.column();
        panel.setPadding(
                host.dp(14), host.dp(14),
                host.dp(14), host.dp(12));
        panel.setBackground(host.roundBorder(
                MainActivity.CARD,
                Color.rgb(92, 73, 127),
                22,
                1));

        panel.addView(host.text(
                "算命記錄",
                22,
                MainActivity.TEXT,
                true));

        TextView hint = host.text(
                "點之前的結果直接查看，不會重新排盤，也不會重新呼叫 AI。",
                12,
                MainActivity.MUTED,
                false);
        hint.setLineSpacing(host.dp(2), 1f);
        panel.addView(hint, host.marginTop(4));

        LinearLayout body = host.column();
        final AlertDialog[] dialogHolder = new AlertDialog[1];

        if (entries.isEmpty()) {
            TextView empty = host.text(
                    "還沒有算命記錄。完成一次排盤後會自動保存在這裡。",
                    14,
                    MainActivity.MUTED,
                    false);
            empty.setLineSpacing(host.dp(2), 1f);
            body.addView(empty, host.marginTop(14));
        } else {
            for (FortuneHistoryEntry entry : entries) {
                LinearLayout item = host.column();
                item.setPadding(
                        host.dp(11), host.dp(10),
                        host.dp(11), host.dp(10));
                item.setBackground(host.roundBorder(
                        Color.rgb(39, 29, 60),
                        Color.rgb(168, 137, 230),
                        15,
                        2));
                item.setClickable(true);

                LinearLayout head = new LinearLayout(host);
                head.setOrientation(LinearLayout.HORIZONTAL);
                head.setGravity(Gravity.CENTER_VERTICAL);

                TextView title = host.text(
                        entry.titleLine(),
                        15,
                        MainActivity.TEXT,
                        true);
                head.addView(
                        title,
                        new LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1f));

                TextView time = host.text(
                        entry.savedAtText(),
                        11,
                        MainActivity.MUTED,
                        false);
                head.addView(time);
                item.addView(head);

                TextView profile = host.text(
                        entry.subtitleLine(),
                        12,
                        MainActivity.ACCENT,
                        true);
                profile.setLineSpacing(host.dp(2), 1f);
                item.addView(profile, host.marginTop(4));

                TextView state = host.text(
                        (entry.aiCopy == null
                                ? "本地排盤"
                                : "完整解讀已保存")
                                + " · 點擊查看  →",
                        11,
                        entry.aiCopy == null
                                ? MainActivity.MUTED
                                : MainActivity.GOLD,
                        true);
                item.addView(state, host.marginTop(5));

                final FortuneHistoryEntry selected = entry;
                item.setOnClickListener(v -> {
                    AlertDialog current = dialogHolder[0];
                    if (current != null && current.isShowing()) {
                        current.dismiss();
                    }
                    host.openHistoryEntry(selected);
                });

                body.addView(item, host.marginTop(8));
            }
        }

        ScrollView scroll = new ScrollView(host);
        scroll.setFillViewport(false);
        scroll.addView(body);
        LinearLayout.LayoutParams scrollLp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        entries.isEmpty()
                                ? host.dp(150)
                                : host.dp(430));
        scrollLp.topMargin = host.dp(8);
        panel.addView(scroll, scrollLp);

        LinearLayout actions = new LinearLayout(host);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button clear = host.secondaryButton("清除記錄");
        clear.setEnabled(!entries.isEmpty());
        clear.setAlpha(entries.isEmpty() ? 0.45f : 1f);

        Button close = host.secondaryButton("關閉");
        LinearLayout.LayoutParams actionLp =
                new LinearLayout.LayoutParams(
                        0,
                        host.dp(44),
                        1f);
        actionLp.rightMargin = host.dp(6);
        actions.addView(clear, actionLp);
        actions.addView(
                close,
                new LinearLayout.LayoutParams(
                        0,
                        host.dp(44),
                        1f));
        panel.addView(actions, host.marginTop(10));

        final AlertDialog dialog =
                new AlertDialog.Builder(host)
                        .setView(panel)
                        .create();
        dialogHolder[0] = dialog;

        clear.setOnClickListener(v -> {
            FortuneHistoryStore.clear(host);
            OperationLog.add(host, "FORTUNE_HISTORY_CLEARED", "");
            Toast.makeText(
                    host,
                    "已清除算命記錄",
                    Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        close.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        host.styleDarkDialog(dialog);
    }

    void showOperationLog() {
        OperationLog.add(host, "OPEN_OPERATION_LOG", "");
        List<OperationLog.Entry> entries = OperationLog.list(host);

        LinearLayout panel = host.column();
        panel.setPadding(
                host.dp(12), host.dp(12),
                host.dp(12), host.dp(10));
        panel.setBackground(host.roundBorder(
                MainActivity.CARD,
                Color.rgb(92, 73, 127),
                24,
                1));

        TextView title = host.text(
                "操作記錄", 22, MainActivity.TEXT, true);
        panel.addView(title);

        TextView hint = host.text(
                "最近 200 筆 · 點任一筆查看完整內容",
                12,
                MainActivity.MUTED,
                false);
        panel.addView(hint, host.marginTop(4));

        LinearLayout body = host.column();
        body.setPadding(0, host.dp(2), 0, host.dp(4));

        if (entries.isEmpty()) {
            body.addView(
                    host.text(
                            "目前還沒有操作記錄",
                            14,
                            MainActivity.MUTED,
                            false),
                    host.marginTop(9));
        } else {
            for (final OperationLog.Entry entry : entries) {
                TextView item = host.text(
                        entry.listLabel() + "  →",
                        13,
                        MainActivity.ACCENT,
                        true);
                item.setPadding(
                        host.dp(10), host.dp(8),
                        host.dp(10), host.dp(8));
                item.setBackground(host.roundBorder(
                        Color.rgb(39, 29, 60),
                        Color.rgb(168, 137, 230),
                        14,
                        2));
                item.setClickable(true);
                item.setOnClickListener(v -> {
                    OperationLog.add(
                            host,
                            "OPERATION_LOG_ITEM_OPENED",
                            entry.action);
                    showOperationLogDetail(entry);
                });
                body.addView(item, host.marginTop(5));
            }
        }

        ScrollView scroll = new ScrollView(host);
        scroll.setFillViewport(false);
        scroll.addView(body);
        LinearLayout.LayoutParams scrollLp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        host.dp(400));
        scrollLp.topMargin = host.dp(8);
        panel.addView(scroll, scrollLp);

        LinearLayout actions = new LinearLayout(host);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button clear = host.secondaryButton("清除記錄");
        Button close = host.secondaryButton("關閉");
        LinearLayout.LayoutParams actionLp =
                new LinearLayout.LayoutParams(
                        0, host.dp(44), 1f);
        actionLp.rightMargin = host.dp(6);
        actions.addView(clear, actionLp);
        actions.addView(
                close,
                new LinearLayout.LayoutParams(
                        0, host.dp(44), 1f));
        panel.addView(actions, host.marginTop(8));

        final AlertDialog dialog =
                new AlertDialog.Builder(host)
                        .setView(panel)
                        .create();
        clear.setOnClickListener(v -> {
            OperationLog.clear(host);
            Toast.makeText(
                    host,
                    "已清除操作記錄",
                    Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        close.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        host.styleDarkDialog(dialog);
    }

    void showAbout() {
        LinearLayout panel = host.column();
        panel.setPadding(
                host.dp(16), host.dp(16),
                host.dp(16), host.dp(14));
        panel.setBackground(host.roundBorder(
                MainActivity.CARD,
                Color.rgb(92, 73, 127),
                22,
                1));

        ImageView logo = new ImageView(host);
        logo.setImageResource(
                R.drawable.ic_launcher_foreground_art);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams logoLp =
                new LinearLayout.LayoutParams(
                        host.dp(72),
                        host.dp(72));
        logoLp.gravity = Gravity.CENTER_HORIZONTAL;
        panel.addView(logo, logoLp);

        TextView title = host.text(
                "Crew Fortune · 命運研究所",
                20,
                MainActivity.TEXT,
                true);
        title.setGravity(Gravity.CENTER);
        panel.addView(title, host.marginTop(8));

        TextView slogan = host.text(
                "很認真算，別太認真信。",
                14,
                MainActivity.GOLD,
                true);
        slogan.setGravity(Gravity.CENTER);
        panel.addView(slogan, host.marginTop(4));

        TextView version = host.text(
                "v" + appVersionName()
                        + " · deterministic facts + AI interpretation",
                11,
                MainActivity.MUTED,
                false);
        version.setGravity(Gravity.CENTER);
        panel.addView(version, host.marginTop(5));

        Button close = host.secondaryButton("關閉");
        panel.addView(close, host.fixedHeightTop(44, 10));

        final AlertDialog dialog =
                new AlertDialog.Builder(host)
                        .setView(panel)
                        .create();
        close.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        host.styleDarkDialog(dialog);
    }

    void showApiKey() {
        final EditText keyInput = new EditText(host);
        keyInput.setSingleLine(true);
        keyInput.setHint("Gemini API Key");
        keyInput.setText(AppConfig.getGeminiApiKey(host));
        keyInput.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        keyInput.setPadding(
                host.dp(16), host.dp(8),
                host.dp(16), host.dp(8));

        new AlertDialog.Builder(host)
                .setTitle("AI 模式")
                .setMessage(
                        "Gemini 只負責解讀與文案，底層命盤由本地 deterministic engine 計算。"
                                + "可在主畫面切換嚴謹／普通／風趣三種回應風格。")
                .setView(keyInput)
                .setPositiveButton("儲存", (dialog, which) -> {
                    try {
                        AppConfig.setGeminiApiKey(
                                host,
                                keyInput.getText().toString());
                        OperationLog.add(
                                host,
                                "GEMINI_KEY_SAVED",
                                "encrypted");
                        host.refreshAiStatusFromController();
                        Toast.makeText(
                                host,
                                "Gemini Key 已加密儲存於本機",
                                Toast.LENGTH_SHORT).show();
                    } catch (RuntimeException error) {
                        OperationLog.add(
                                host,
                                "GEMINI_KEY_SAVE_FAILED",
                                host.safeErrorMessage(error));
                        Toast.makeText(
                                host,
                                "無法安全儲存 Gemini Key，請稍後再試",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .setNeutralButton("清除", (dialog, which) -> {
                    try {
                        AppConfig.setGeminiApiKey(host, "");
                        OperationLog.add(
                                host,
                                "GEMINI_KEY_CLEARED",
                                "");
                        host.refreshAiStatusFromController();
                    } catch (RuntimeException error) {
                        OperationLog.add(
                                host,
                                "GEMINI_KEY_CLEAR_FAILED",
                                host.safeErrorMessage(error));
                        Toast.makeText(
                                host,
                                "無法清除本機金鑰",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    void showTraitEvidence(String trait, String evidence) {
        LinearLayout panel = evidencePanel();
        panel.addView(host.text(
                "為什麼這樣說？",
                20,
                MainActivity.TEXT,
                true));

        TextView traitView = host.text(
                trait,
                14,
                MainActivity.ACCENT,
                true);
        traitView.setLineSpacing(host.dp(2), 1f);
        panel.addView(traitView, host.marginTop(6));

        TextView evidenceView = host.text(
                "依據\n" + evidence,
                13,
                MainActivity.TEXT,
                false);
        evidenceView.setLineSpacing(host.dp(3), 1f);
        evidenceView.setPadding(
                host.dp(9), host.dp(9),
                host.dp(9), host.dp(9));
        evidenceView.setBackground(host.roundBorder(
                MainActivity.CARD_2,
                Color.rgb(80, 65, 111),
                14,
                1));
        panel.addView(
                evidenceView,
                host.marginTop(8));

        showTeacherActions(
                panel,
                "問老師這一點",
                "請直接解釋這一點為什麼像我：「"
                        + trait
                        + "」。請用 deterministic facts，尤其是："
                        + evidence);
    }

    void showHighlightEvidence(
            FortuneYearHighlightBuilder.Highlight highlight) {
        LinearLayout panel = evidencePanel();
        panel.addView(host.text(
                highlight.year + "｜" + highlight.theme,
                20,
                MainActivity.TEXT,
                true));

        TextView summary = host.text(
                highlight.summary,
                13,
                MainActivity.ACCENT,
                true);
        summary.setLineSpacing(host.dp(2), 1f);
        panel.addView(summary, host.marginTop(6));

        TextView evidence = host.text(
                "為什麼挑這年\n" + highlight.evidence,
                13,
                MainActivity.TEXT,
                false);
        evidence.setLineSpacing(host.dp(3), 1f);
        evidence.setPadding(
                host.dp(9), host.dp(9),
                host.dp(9), host.dp(9));
        evidence.setBackground(host.roundBorder(
                MainActivity.CARD_2,
                Color.rgb(80, 65, 111),
                14,
                1));
        panel.addView(evidence, host.marginTop(8));

        showTeacherActions(
                panel,
                "問老師這一年",
                highlight.question);
    }

    void showFactDetail(String titleValue, Object value) {
        LinearLayout panel = evidencePanel();
        panel.addView(host.text(
                titleValue,
                21,
                MainActivity.TEXT,
                true));

        TextView detail = host.text(
                formatStructured(value),
                13,
                MainActivity.TEXT,
                false);
        detail.setLineSpacing(host.dp(2), 1f);
        detail.setPadding(
                host.dp(9), host.dp(9),
                host.dp(9), host.dp(9));
        detail.setBackground(host.roundBorder(
                MainActivity.CARD_2,
                Color.rgb(80, 65, 111),
                14,
                1));

        ScrollView scroll = new ScrollView(host);
        scroll.addView(detail);
        LinearLayout.LayoutParams scrollLp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        host.dp(420));
        scrollLp.topMargin = host.dp(8);
        panel.addView(scroll, scrollLp);

        Button close = host.secondaryButton("關閉");
        panel.addView(close, host.fixedHeightTop(44, 8));

        final AlertDialog dialog =
                new AlertDialog.Builder(host)
                        .setView(panel)
                        .create();
        close.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        host.styleDarkDialog(dialog);
    }

    private void showOperationLogDetail(OperationLog.Entry entry) {
        LinearLayout panel = evidencePanel();

        TextView title = host.text(
                entry.action.replace('_', ' '),
                20,
                MainActivity.TEXT,
                true);
        panel.addView(title);

        TextView time = host.text(
                entry.timeText(),
                12,
                MainActivity.MUTED,
                false);
        panel.addView(time, host.marginTop(4));

        TextView detailLabel = host.text(
                "內容",
                11,
                MainActivity.GOLD,
                true);
        panel.addView(detailLabel, host.marginTop(11));

        String detailValue =
                entry.detail == null || entry.detail.isEmpty()
                        ? "這筆事件沒有額外內容"
                        : entry.detail;
        TextView detail = host.text(
                detailValue,
                14,
                MainActivity.TEXT,
                false);
        detail.setLineSpacing(host.dp(2), 1f);
        detail.setPadding(
                host.dp(9), host.dp(8),
                host.dp(9), host.dp(8));
        detail.setBackground(host.roundBorder(
                MainActivity.CARD_2,
                Color.rgb(80, 65, 111),
                14,
                1));
        panel.addView(detail, host.marginTop(6));

        Button close = host.secondaryButton("關閉");
        panel.addView(close, host.marginTop(6));

        final AlertDialog dialog =
                new AlertDialog.Builder(host)
                        .setView(panel)
                        .create();
        close.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        host.styleDarkDialog(dialog);
    }

    private void showTeacherActions(
            LinearLayout panel,
            String buttonLabel,
            String question) {
        Button ask = host.primaryButton(buttonLabel + "  →");
        ask.setBackground(host.round(MainActivity.GOLD, 14));
        Button close = host.secondaryButton("關閉");

        LinearLayout actions = new LinearLayout(host);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        0, host.dp(44), 1f);
        lp.rightMargin = host.dp(6);
        actions.addView(ask, lp);
        actions.addView(
                close,
                new LinearLayout.LayoutParams(
                        0, host.dp(44), 1f));
        panel.addView(actions, host.marginTop(8));

        final AlertDialog dialog =
                new AlertDialog.Builder(host)
                        .setView(panel)
                        .create();
        ask.setOnClickListener(v -> {
            dialog.dismiss();
            host.askTeacherFromDialog(question);
        });
        close.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        host.styleDarkDialog(dialog);
    }

    private LinearLayout evidencePanel() {
        LinearLayout panel = host.column();
        panel.setPadding(
                host.dp(14), host.dp(14),
                host.dp(14), host.dp(12));
        panel.setBackground(host.roundBorder(
                MainActivity.CARD,
                Color.rgb(92, 73, 127),
                20,
                1));
        return panel;
    }

    private String appVersionName() {
        try {
            return host.getPackageManager()
                    .getPackageInfo(
                            host.getPackageName(),
                            0)
                    .versionName;
        } catch (Exception ignored) {
            return "";
        }
    }

    private String formatStructured(Object value) {
        return formatStructured(value, 0);
    }

    private String formatStructured(Object value, int depth) {
        if (value == null) return "—";

        StringBuilder indentBuilder = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            indentBuilder.append("  ");
        }
        String indent = indentBuilder.toString();

        if (value instanceof Map) {
            StringBuilder out = new StringBuilder();
            for (Map.Entry<?, ?> entry :
                    ((Map<?, ?>) value).entrySet()) {
                if (out.length() > 0) out.append("\n");
                Object child = entry.getValue();
                if (child instanceof Map
                        || child instanceof List) {
                    out.append(indent)
                            .append(entry.getKey())
                            .append("：\n")
                            .append(
                                    formatStructured(
                                            child,
                                            depth + 1));
                } else {
                    out.append(indent)
                            .append(entry.getKey())
                            .append("：")
                            .append(String.valueOf(child));
                }
            }
            return out.toString();
        }

        if (value instanceof List) {
            StringBuilder out = new StringBuilder();
            for (Object child : (List<?>) value) {
                if (out.length() > 0) out.append("\n");
                if (child instanceof Map
                        || child instanceof List) {
                    out.append(indent)
                            .append("•\n")
                            .append(
                                    formatStructured(
                                            child,
                                            depth + 1));
                } else {
                    out.append(indent)
                            .append("• ")
                            .append(String.valueOf(child));
                }
            }
            return out.toString();
        }

        return indent + String.valueOf(value);
    }
}
