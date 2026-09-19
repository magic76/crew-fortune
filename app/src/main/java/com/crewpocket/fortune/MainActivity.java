package com.crewpocket.fortune;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.magic76.crew.agent.AgentEvent;
import com.magic76.crew.agent.AgentHarness;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

public final class MainActivity extends Activity {
    private static final int REQUEST_TEACHER_AUDIO = 4101;
    private static final int BG = Color.rgb(23, 17, 38);
    private static final int CARD = Color.rgb(39, 30, 60);
    private static final int CARD_2 = Color.rgb(50, 38, 76);
    private static final int TEXT = Color.rgb(248, 245, 255);
    private static final int MUTED = Color.rgb(190, 181, 207);
    private static final int ACCENT = Color.rgb(183, 156, 255);
    private static final int GOLD = Color.rgb(255, 214, 128);

    private final FortuneEngine engine = new FortuneEngine();
    private final StringBuilder aiBuffer = new StringBuilder();

    private FortuneMode selectedMode = FortuneMode.BA_ZI;
    private EditText nameInput;
    private EditText birthInput;
    private EditText birthTimeInput;
    private LinearLayout genderRow;
    private Button maleButton;
    private Button femaleButton;
    private String selectedGender = "";
    private TextView modeLabel;
    private TextView aiStatus;
    private Button strictStyleButton;
    private Button normalStyleButton;
    private Button funnyStyleButton;
    private AiStyle selectedAiStyle = AiStyle.FUNNY;
    private LinearLayout resultCard;
    private FortuneResult currentResult;
    private FortuneFacts currentFacts;
    private AiFortuneCopy aiCopy;
    private AgentHarness activeHarness;
    private GeminiFortuneLiveSession teacherSession;
    private AlertDialog teacherDialog;
    private TextView teacherStatusText;
    private TextView teacherInputText;
    private TextView teacherOutputText;
    private boolean pendingTeacherStart;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        selectedAiStyle = AppConfig.getAiStyle(this);
        setContentView(buildScreen());
        refreshAiStatus();
        updateAiStyleButtons();
        if (state != null) {
            restoreInstanceState(state);
        } else {
            restoreLastProfile();
        }
        OperationLog.add(this, "APP_OPEN", "mode=" + selectedMode.name());
    }

    @Override protected void onDestroy() {
        OperationLog.add(this, "APP_DESTROY", "changingConfig=" + isChangingConfigurations());
        closeTeacher();
        closeAgent();
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        OperationLog.add(this, "SCREEN_ROTATED",
                newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
                        ? "landscape" : "portrait");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("state_name", nameInput == null ? "" : nameInput.getText().toString());
        outState.putString("state_birth_date", birthInput == null ? "" : birthInput.getText().toString());
        outState.putString("state_birth_time", birthTimeInput == null ? "" : birthTimeInput.getText().toString());
        outState.putString("state_gender", selectedGender);
        outState.putString("state_mode", selectedMode.name());
        outState.putString("state_ai_style", selectedAiStyle.name());
        outState.putBoolean("state_has_result", currentResult != null && currentFacts != null);
        if (aiCopy != null) outState.putString("state_ai_copy", serializeAiCopy(aiCopy));
        OperationLog.add(this, "STATE_SAVED",
                currentResult == null ? "no_result" : "result_saved");
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = column();
        final int baseLeft = dp(20);
        final int baseTop = dp(24);
        final int baseRight = dp(20);
        final int baseBottom = dp(40);
        root.setPadding(baseLeft, baseTop, baseRight, baseBottom);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(
                    baseLeft,
                    baseTop + insets.getSystemWindowInsetTop(),
                    baseRight,
                    baseBottom + insets.getSystemWindowInsetBottom());
            return insets;
        });
        scroll.setClipToPadding(false);
        scroll.addView(root);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView eyebrow = text("CREW FORTUNE · 命運研究所", 13, GOLD, true);
        top.addView(eyebrow, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView history = text("記錄", 13, GOLD, true);
        history.setGravity(Gravity.END);
        history.setPadding(dp(10), dp(8), 0, dp(8));
        history.setOnClickListener(v -> showOperationLog());
        top.addView(history);

        aiStatus = text("", 13, ACCENT, true);
        aiStatus.setGravity(Gravity.END);
        aiStatus.setPadding(dp(12), dp(8), 0, dp(8));
        aiStatus.setOnClickListener(v -> {
            OperationLog.add(this, "OPEN_AI_SETTINGS", "");
            showApiKeyDialog();
        });
        top.addView(aiStatus);
        root.addView(top);

        TextView title = text("很認真算，\n別太認真信。", 34, TEXT, true);
        title.setLineSpacing(0, 1.04f);
        root.addView(title, marginTop(8));

        TextView sub = text("底層數字固定，AI 每次換一種方式講你。\n沒有 AI 也能算，有 AI 就比較會嘴。", 15, MUTED, false);
        sub.setLineSpacing(dp(4), 1f);
        root.addView(sub, marginTop(10));

        LinearLayout form = column();
        form.setPadding(dp(16), dp(16), dp(16), dp(16));
        form.setBackground(round(CARD, 22));
        root.addView(form, marginTop(24));

        form.addView(label("先交代一下你的基本資料"));
        nameInput = input("你的名字");
        birthInput = input("生日，例如 1985-07-22");
        birthInput.setFocusable(false);
        birthInput.setClickable(true);
        birthInput.setOnClickListener(v -> showDatePicker());

        birthTimeInput = input("出生地當地時間，例如 14:30");
        birthTimeInput.setFocusable(false);
        birthTimeInput.setClickable(true);
        birthTimeInput.setOnClickListener(v -> showTimePicker());
        form.addView(nameInput, marginTop(12));
        form.addView(birthInput, marginTop(10));
        form.addView(birthTimeInput, marginTop(10));

        genderRow = new LinearLayout(this);
        genderRow.setOrientation(LinearLayout.HORIZONTAL);
        maleButton = genderButton("男");
        femaleButton = genderButton("女");
        maleButton.setOnClickListener(v -> selectGender("male"));
        femaleButton.setOnClickListener(v -> selectGender("female"));
        LinearLayout.LayoutParams genderLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        genderLp.rightMargin = dp(8);
        genderRow.addView(maleButton, genderLp);
        genderRow.addView(femaleButton, new LinearLayout.LayoutParams(0, dp(46), 1f));
        form.addView(genderRow, marginTop(10));

        LinearLayout presetRow = new LinearLayout(this);
        presetRow.setOrientation(LinearLayout.HORIZONTAL);
        Button choosePreset = secondaryButton("常用資料");
        Button savePreset = secondaryButton("儲存 preset");
        choosePreset.setOnClickListener(v -> showPresetPicker());
        savePreset.setOnClickListener(v -> saveCurrentPreset());
        LinearLayout.LayoutParams presetLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        presetLp.rightMargin = dp(8);
        presetRow.addView(choosePreset, presetLp);
        presetRow.addView(savePreset, new LinearLayout.LayoutParams(0, dp(46), 1f));
        form.addView(presetRow, marginTop(10));

        TextView styleLabel = text("AI 回應風格", 12, MUTED, true);
        form.addView(styleLabel, marginTop(14));

        LinearLayout styleRow = new LinearLayout(this);
        styleRow.setOrientation(LinearLayout.HORIZONTAL);
        strictStyleButton = styleButton("嚴謹");
        normalStyleButton = styleButton("普通");
        funnyStyleButton = styleButton("風趣");
        strictStyleButton.setOnClickListener(v -> selectAiStyle(AiStyle.STRICT));
        normalStyleButton.setOnClickListener(v -> selectAiStyle(AiStyle.NORMAL));
        funnyStyleButton.setOnClickListener(v -> selectAiStyle(AiStyle.FUNNY));
        LinearLayout.LayoutParams styleLp = new LinearLayout.LayoutParams(0, dp(44), 1f);
        styleLp.rightMargin = dp(6);
        styleRow.addView(strictStyleButton, styleLp);
        LinearLayout.LayoutParams styleLp2 = new LinearLayout.LayoutParams(0, dp(44), 1f);
        styleLp2.rightMargin = dp(6);
        styleRow.addView(normalStyleButton, styleLp2);
        styleRow.addView(funnyStyleButton, new LinearLayout.LayoutParams(0, dp(44), 1f));
        form.addView(styleRow, marginTop(6));

        TextView styleHint = text("嚴謹：專業報告｜普通：白話平衡｜風趣：嘴得準但不傷人\n只改 AI 說話方式，不改命盤計算結果", 11, MUTED, false);
        form.addView(styleHint, marginTop(4));

        modeLabel = text("今天想算：八字\n四柱、十神、大運、流年\n以出生地當地民用時間排盤；目前不做真太陽時校正", 16, TEXT, true);
        root.addView(modeLabel, marginTop(24));

        HorizontalScrollView chipsScroll = new HorizontalScrollView(this);
        chipsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chipsScroll.addView(chips);
        for (final FortuneMode mode : FortuneMode.values()) {
            Button chip = new Button(this);
            chip.setText(mode.title());
            chip.setTextSize(14);
            chip.setAllCaps(false);
            chip.setTextColor(TEXT);
            chip.setBackground(round(CARD_2, 18));
            chip.setPadding(dp(14), 0, dp(14), 0);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(48));
            lp.rightMargin = dp(8);
            chips.addView(chip, lp);
            chip.setOnClickListener(v -> selectMode(mode));
        }
        root.addView(chipsScroll, marginTop(10));

        Button calculate = new Button(this);
        calculate.setText("開始算命");
        calculate.setTextSize(17);
        calculate.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        calculate.setTextColor(Color.rgb(30, 22, 46));
        calculate.setAllCaps(false);
        calculate.setBackground(round(ACCENT, 20));
        calculate.setOnClickListener(v -> calculate());
        LinearLayout.LayoutParams calcLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(58));
        calcLp.topMargin = dp(18);
        root.addView(calculate, calcLp);

        resultCard = column();
        resultCard.setPadding(dp(18), dp(18), dp(18), dp(18));
        resultCard.setBackground(round(CARD, 24));
        resultCard.setVisibility(View.GONE);
        root.addView(resultCard, marginTop(22));

        TextView foot = text("娛樂用途 · 八字＋生日型塔羅生命靈數 · v0.6.1", 12, MUTED, false);
        foot.setGravity(Gravity.CENTER);
        root.addView(foot, marginTop(22));
        return scroll;
    }

    private void selectMode(FortuneMode mode) {
        selectedMode = mode;
        OperationLog.add(this, "MODE_SELECTED", mode.name());
        boolean isBaZi = mode == FortuneMode.BA_ZI;
        birthTimeInput.setVisibility(isBaZi ? View.VISIBLE : View.GONE);
        genderRow.setVisibility(isBaZi ? View.VISIBLE : View.GONE);
        modeLabel.setText("今天想算：" + mode.title() + "\n" + mode.subtitle()
                + (isBaZi ? "\n以出生地當地民用時間排盤；目前不做真太陽時校正"
                : "\n只用生日：算生命道路、內靈數、外靈數與塔羅出生牌"));
    }

    private void selectGender(String gender) {
        selectedGender = gender;
        OperationLog.add(this, "GENDER_SELECTED", gender);
        boolean male = "male".equals(gender);
        maleButton.setBackground(round(male ? ACCENT : CARD_2, 14));
        femaleButton.setBackground(round(!male ? ACCENT : CARD_2, 14));
        maleButton.setTextColor(male ? Color.rgb(30, 22, 46) : TEXT);
        femaleButton.setTextColor(!male ? Color.rgb(30, 22, 46) : TEXT);
    }

    private void calculate() {
        OperationLog.add(this, "CALCULATE_START", selectedMode.name());
        closeTeacher();
        closeAgent();
        FortuneProfile profile = new FortuneProfile(
                nameInput.getText().toString(),
                birthInput.getText().toString(),
                birthTimeInput.getText().toString(),
                selectedGender);
        try {
            currentFacts = engine.calculateFacts(selectedMode, profile, new Date());
            currentResult = engine.calculate(selectedMode, profile, new Date());
            FortunePresetStore.saveLast(this, currentPreset());
            OperationLog.add(this, "CALCULATE_SUCCESS",
                    selectedMode.name() + " · " + currentFacts.basis);
            aiCopy = null;
            synchronized (aiBuffer) {
                aiBuffer.setLength(0);
            }
            boolean useAi = AppConfig.hasGeminiApiKey(this);
            renderResult(currentResult, useAi);
            if (useAi) startAiCopy(profile);
        } catch (IllegalArgumentException error) {
            OperationLog.add(this, "CALCULATE_FAILED",
                    error.getMessage() == null ? "unknown" : error.getMessage());
            Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startAiCopy(FortuneProfile profile) {
        OperationLog.add(this, "AI_INTERPRETATION_START",
                selectedMode.name() + " · " + selectedAiStyle.name());
        try {
            GeminiTextModelSession session = new GeminiTextModelSession(
                    AppConfig.getGeminiApiKey(this),
                    selectedAiStyle.temperature());
            activeHarness = FortuneAgentRuntime.createInterpretation(session, new AgentHarness.Listener() {
                @Override public void onAgentEvent(AgentEvent event) {
                    if (event == null) return;
                    switch (event.type()) {
                        case MODEL_TEXT:
                            synchronized (aiBuffer) {
                                aiBuffer.append(event.text());
                            }
                            break;
                        case TURN_COMPLETED:
                            final String completed;
                            synchronized (aiBuffer) {
                                completed = aiBuffer.toString().trim();
                            }
                            try {
                                final AiFortuneCopy parsed = AiFortuneCopy.parse(completed);
                                runOnUiThread(() -> {
                                    aiCopy = parsed;
                                    OperationLog.add(MainActivity.this,
                                            "AI_INTERPRETATION_SUCCESS",
                                            selectedAiStyle.name());
                                    if (currentResult != null) renderResult(currentResult, false);
                                });
                            } catch (IllegalArgumentException parseError) {
                                OperationLog.add(MainActivity.this,
                                        "AI_INTERPRETATION_FAILED", "parse_error");
                                showAiFallback("AI 命理師講得太玄，格式跑掉了。先顯示本地結果。");
                            }
                            closeAgent();
                            break;
                        case ERROR:
                            OperationLog.add(MainActivity.this,
                                    "AI_INTERPRETATION_FAILED", "model_error");
                            showAiFallback("AI 命理師暫時去喝茶，先顯示本地結果。");
                            closeAgent();
                            break;
                        default:
                            break;
                    }
                }
            }, selectedAiStyle);
            activeHarness.start();
            activeHarness.submitText(buildAiRequest(profile));
        } catch (Exception error) {
            OperationLog.add(this, "AI_INTERPRETATION_FAILED", "startup_error");
            showAiFallback("AI 模式啟動失敗，已使用本地結果。");
            closeAgent();
        }
    }

    private void showAiFallback(String message) {
        runOnUiThread(() -> {
            aiCopy = null;
            if (currentResult != null) renderResult(currentResult, false);
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        });
    }

    private String buildAiRequest(FortuneProfile profile) {
        if (currentFacts == null) {
            throw new IllegalStateException("deterministic facts are missing");
        }
        String factsJson = new JSONObject(currentFacts.details).toString();
        StringBuilder value = new StringBuilder();
        value.append("請只解讀以下已由本機完成的 deterministic facts。不要重新計算，不要呼叫工具，不要修正輸入格式，也絕對不要使用預設值。\n");
        value.append("mode=").append(selectedMode.name()).append('\n');
        value.append("displayName=").append(profile.name).append('\n');
        value.append("basis=").append(currentFacts.basis).append('\n');
        value.append("aiStyle=").append(selectedAiStyle.name()).append('\n');
        value.append("deterministicFacts=").append(factsJson).append('\n');
        value.append("creativeVariant=").append(System.nanoTime()).append('\n');
        value.append("creativeVariant 只允許改變措辭與笑點。所有數字、干支、十神、大運、流年、塔羅牌、天賦數都必須逐字遵守 deterministicFacts。");
        return value.toString();
    }

    private void renderResult(FortuneResult result, boolean aiLoading) {
        resultCard.removeAllViews();

        TextView badge = text(result.mode.title().toUpperCase(), 12, GOLD, true);
        resultCard.addView(badge);

        String titleValue;
        if (aiLoading) {
            titleValue = result.mode == FortuneMode.BA_ZI
                    ? "命盤已排好，現在開始講人話"
                    : result.mode == FortuneMode.TAROT_NUMEROLOGY
                    ? "你的出生牌已翻開"
                    : result.title;
        } else {
            titleValue = aiCopy == null ? result.title : aiCopy.title;
        }

        TextView title = text(titleValue, 25, TEXT, true);
        resultCard.addView(title, marginTop(8));

        if (result.mode == FortuneMode.BA_ZI) {
            addBaZiResultPanel();
        } else if (result.mode == FortuneMode.TAROT_NUMEROLOGY) {
            addTarotResultPanel();
        } else {
            TextView score = text(primaryMetric(result), 30, ACCENT, true);
            resultCard.addView(score, marginTop(12));

            TextView basis = text("計算依據｜" + result.basis, 13, MUTED, false);
            resultCard.addView(basis, marginTop(4));
        }

        if (aiLoading) {
            TextView loading = text(
                    result.mode == FortuneMode.BA_ZI
                            ? "✦ 四柱、五行與十神都算完了。AI 命理師正在研究怎麼講得準一點，又不要太像老師訓話…"
                            : result.mode == FortuneMode.TAROT_NUMEROLOGY
                            ? "✦ 出生牌已確認。AI 命理師正在把牌義翻譯成比較像人類會想看的版本…"
                            : "✦ 命盤算完了。AI 命理師正在重新組織措辭，避免拿罐頭話術敷衍你…",
                    15, ACCENT, true);
            loading.setLineSpacing(dp(3), 1f);
            resultCard.addView(loading, marginTop(20));
            resultCard.setVisibility(View.VISIBLE);
            return;
        }

        if (aiCopy == null) {
            for (Map.Entry<String, String> entry : FortuneLocalReport.sections(currentFacts).entrySet()) {
                addSection(entry.getKey(), entry.getValue());
            }
            addSection("翻譯成人話", result.translation);
            addSection("命理師補充", result.punchline);
            addSection("建議", result.advice);
        } else {
            addSection("總覽", aiCopy.overview);
            if (!aiCopy.personality.isEmpty()) addSection("性格與天賦", aiCopy.personality);
            if (!aiCopy.careerWealth.isEmpty()) addSection("工作與財務", aiCopy.careerWealth);
            if (!aiCopy.relationships.isEmpty()) addSection("感情與人際", aiCopy.relationships);
            if (!aiCopy.timing.isEmpty()) addSection("目前運勢／週期", aiCopy.timing);
            addSection("翻譯成人話", aiCopy.translation);
            addSection("命理師補充", aiCopy.punchline);
            addSection("建議", aiCopy.advice);
        }

        TextView source = text(aiCopy == null
                ? "本地完整解讀 · 無需 AI"
                : "AI 深度解讀 · " + selectedAiStyle.label() + " · 計算資料固定",
                12, MUTED, false);
        resultCard.addView(source, marginTop(16));

        Button teacher = new Button(this);
        teacher.setText("老師跟我講解");
        teacher.setTextSize(15);
        teacher.setAllCaps(false);
        teacher.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        teacher.setTextColor(Color.rgb(30, 22, 46));
        teacher.setBackground(round(GOLD, 18));
        teacher.setOnClickListener(v -> startTeacherExplanation());
        LinearLayout.LayoutParams teacherLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        teacherLp.topMargin = dp(16);
        resultCard.addView(teacher, teacherLp);

        Button share = new Button(this);
        share.setText("分享這個荒謬但有點準的結果");
        share.setTextSize(15);
        share.setTextColor(TEXT);
        share.setAllCaps(false);
        share.setBackground(round(CARD_2, 18));
        share.setOnClickListener(v -> shareResult());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        lp.topMargin = dp(18);
        resultCard.addView(share, lp);
        resultCard.setVisibility(View.VISIBLE);
    }

    private String primaryMetric(FortuneResult result) {
        if (currentFacts != null && result.mode == FortuneMode.BA_ZI) {
            return "日主強弱指標 " + currentFacts.detailText("strengthIndex") + " / 100";
        }
        if (currentFacts != null && result.mode == FortuneMode.TAROT_NUMEROLOGY) {
            return "生命靈數 " + currentFacts.detailText("lifePathDisplay");
        }
        return result.score + " / 100";
    }

    private void addBaZiResultPanel() {
        if (currentFacts == null) return;

        LinearLayout panel = column();
        panel.setPadding(dp(14), dp(16), dp(14), dp(16));
        panel.setBackground(round(CARD_2, 20));
        resultCard.addView(panel, marginTop(16));

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout master = column();
        TextView masterLabel = text("日主", 12, GOLD, true);
        TextView masterValue = text(
                currentFacts.detailText("dayMaster") + currentFacts.detailText("dayMasterElement"),
                28, TEXT, true);
        master.addView(masterLabel);
        master.addView(masterValue, marginTop(2));
        hero.addView(master, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout balance = column();
        TextView balanceLabel = text("日主強弱", 12, MUTED, true);
        balanceLabel.setGravity(Gravity.END);
        TextView balanceValue = text(currentFacts.detailText("dayMasterStrength")
                + " · " + currentFacts.detailText("strengthIndex"), 20, ACCENT, true);
        balanceValue.setGravity(Gravity.END);
        balance.addView(balanceLabel);
        balance.addView(balanceValue, marginTop(2));
        hero.addView(balance, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        panel.addView(hero);

        TextView divider = text("四柱命盤", 12, GOLD, true);
        panel.addView(divider, marginTop(18));

        LinearLayout pillars = new LinearLayout(this);
        pillars.setOrientation(LinearLayout.HORIZONTAL);
        pillars.setGravity(Gravity.CENTER);
        addPillarCard(pillars, "年柱", currentFacts.detailText("yearPillar"),
                mapValue(currentFacts.detail("hiddenStems"), "year"),
                mapValue(currentFacts.detail("tenGods"), "yearStem"));
        addPillarCard(pillars, "月柱", currentFacts.detailText("monthPillar"),
                mapValue(currentFacts.detail("hiddenStems"), "month"),
                mapValue(currentFacts.detail("tenGods"), "monthStem"));
        addPillarCard(pillars, "日柱", currentFacts.detailText("dayPillar"),
                mapValue(currentFacts.detail("hiddenStems"), "day"),
                "日主");
        addPillarCard(pillars, "時柱", currentFacts.detailText("timePillar"),
                mapValue(currentFacts.detail("hiddenStems"), "time"),
                mapValue(currentFacts.detail("tenGods"), "timeStem"));
        panel.addView(pillars, marginTop(8));

        TextView elementsTitle = text("可見五行", 12, GOLD, true);
        panel.addView(elementsTitle, marginTop(18));

        Object visible = currentFacts.detail("visibleFiveElements");
        addElementBar(panel, "木", intMapValue(visible, "木"));
        addElementBar(panel, "火", intMapValue(visible, "火"));
        addElementBar(panel, "土", intMapValue(visible, "土"));
        addElementBar(panel, "金", intMapValue(visible, "金"));
        addElementBar(panel, "水", intMapValue(visible, "水"));

        TextView trend = text(
                "較多：" + currentFacts.detailText("strongestVisibleElement")
                        + "　較少：" + currentFacts.detailText("weakestVisibleElement"),
                13, MUTED, false);
        panel.addView(trend, marginTop(8));

        TextView tenGodsTitle = text("十神摘要", 12, GOLD, true);
        panel.addView(tenGodsTitle, marginTop(18));
        TextView tenGods = text(formatTenGods(), 14, TEXT, false);
        tenGods.setLineSpacing(dp(3), 1f);
        panel.addView(tenGods, marginTop(6));

        TextView tenDistTitle = text("十神分布", 12, GOLD, true);
        panel.addView(tenDistTitle, marginTop(18));
        TextView tenDist = text(formatMap(currentFacts.detail("tenGodDistribution")), 13, TEXT, false);
        tenDist.setLineSpacing(dp(2), 1f);
        panel.addView(tenDist, marginTop(6));

        TextView strengthTitle = text("旺衰與平衡", 12, GOLD, true);
        panel.addView(strengthTitle, marginTop(18));
        TextView strength = text(
                currentFacts.detailText("dayMasterStrength")
                        + "｜扶身比 " + currentFacts.detailText("strengthIndex") + "/100"
                        + "\n平衡參考元素：" + currentFacts.detailText("balancingElements")
                        + "\n" + currentFacts.detailText("strengthMethod"),
                13, TEXT, false);
        strength.setLineSpacing(dp(3), 1f);
        panel.addView(strength, marginTop(6));

        TextView interactionTitle = text("命局合沖刑害", 12, GOLD, true);
        panel.addView(interactionTitle, marginTop(18));
        TextView interactions = text(formatList(currentFacts.detail("natalInteractions")), 13, TEXT, false);
        interactions.setLineSpacing(dp(3), 1f);
        panel.addView(interactions, marginTop(6));

        TextView luckTitle = text("大運", 12, GOLD, true);
        panel.addView(luckTitle, marginTop(18));
        TextView luck = text(formatLuckPillars(), 13, TEXT, false);
        luck.setLineSpacing(dp(3), 1f);
        panel.addView(luck, marginTop(6));

        TextView annualTitle = text("今年流年", 12, GOLD, true);
        panel.addView(annualTitle, marginTop(18));
        TextView annual = text(formatCurrentAnnual(), 13, TEXT, false);
        annual.setLineSpacing(dp(3), 1f);
        panel.addView(annual, marginTop(6));

        TextView convention = text(
                "排盤規則｜" + currentFacts.detailText("timeConvention"),
                11, MUTED, false);
        convention.setLineSpacing(dp(2), 1f);
        panel.addView(convention, marginTop(16));
    }

    private void addPillarCard(LinearLayout row,
                               String label,
                               String pillar,
                               String hidden,
                               String tenGod) {
        LinearLayout card = column();
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(5), dp(10), dp(5), dp(10));
        GradientDrawable background = round(Color.rgb(31, 24, 49), 14);
        background.setStroke(dp(1), Color.rgb(78, 63, 108));
        card.setBackground(background);

        TextView labelView = text(label, 11, MUTED, true);
        labelView.setGravity(Gravity.CENTER);
        card.addView(labelView);

        TextView pillarView = text(pillar, 22, TEXT, true);
        pillarView.setGravity(Gravity.CENTER);
        card.addView(pillarView, marginTop(4));

        TextView tenGodView = text(tenGod, 11, GOLD, true);
        tenGodView.setGravity(Gravity.CENTER);
        card.addView(tenGodView, marginTop(5));

        TextView hiddenView = text(shortHidden(hidden), 10, MUTED, false);
        hiddenView.setGravity(Gravity.CENTER);
        card.addView(hiddenView, marginTop(3));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = dp(6);
        row.addView(card, lp);
    }

    private void addElementBar(LinearLayout parent, String element, int count) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView name = text(element, 14, TEXT, true);
        row.addView(name, new LinearLayout.LayoutParams(dp(24), dp(30)));

        LinearLayout track = new LinearLayout(this);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackground(round(Color.rgb(29, 23, 45), 8));

        int safeCount = Math.max(0, Math.min(8, count));
        if (safeCount > 0) {
            View fill = new View(this);
            fill.setBackground(round(ACCENT, 8));
            track.addView(fill, new LinearLayout.LayoutParams(
                    0, dp(10), safeCount));
        }
        if (safeCount < 8) {
            View empty = new View(this);
            track.addView(empty, new LinearLayout.LayoutParams(
                    0, dp(10), 8 - safeCount));
        }

        LinearLayout.LayoutParams trackLp = new LinearLayout.LayoutParams(
                0, dp(10), 1f);
        trackLp.leftMargin = dp(8);
        trackLp.rightMargin = dp(8);
        row.addView(track, trackLp);

        TextView value = text(count + "/8", 12, MUTED, true);
        value.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        row.addView(value, new LinearLayout.LayoutParams(dp(34), dp(30)));

        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(30));
        rowLp.topMargin = dp(2);
        parent.addView(row, rowLp);
    }

    private void addTarotResultPanel() {
        if (currentFacts == null) return;

        LinearLayout panel = column();
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setPadding(dp(18), dp(22), dp(18), dp(20));
        GradientDrawable background = round(CARD_2, 22);
        background.setStroke(dp(1), Color.rgb(87, 69, 121));
        panel.setBackground(background);
        resultCard.addView(panel, marginTop(16));

        TextView coreTitle = text("生日核心數", 12, GOLD, true);
        coreTitle.setGravity(Gravity.CENTER);
        panel.addView(coreTitle);

        TextView coreNumbers = text(
                "生命道路 " + currentFacts.detailText("lifePathDisplay")
                        + "　·　生日數 " + currentFacts.detailText("birthdayNumber")
                        + "\n態度數 " + currentFacts.detailText("attitudeNumber"),
                15, TEXT, true);
        coreNumbers.setGravity(Gravity.CENTER);
        coreNumbers.setLineSpacing(dp(3), 1f);
        panel.addView(coreNumbers, marginTop(8));

        LinearLayout innerOuter = new LinearLayout(this);
        innerOuter.setOrientation(LinearLayout.HORIZONTAL);
        innerOuter.setGravity(Gravity.CENTER);

        addNumerologyIdentityCard(
                innerOuter,
                "內在靈魂牌",
                currentFacts.detailText("soulCardNumber"),
                currentFacts.detailText("soulCardName"));
        addNumerologyIdentityCard(
                innerOuter,
                "外在人格牌",
                currentFacts.detailText("personalityCardNumber"),
                currentFacts.detailText("personalityCardName"));
        panel.addView(innerOuter, marginTop(16));

        TextView contrast = text(
                "外在 " + currentFacts.detailText("personalityCardNumber")
                        + " " + currentFacts.detailText("personalityCardName")
                        + "　↔　內在 " + currentFacts.detailText("soulCardNumber")
                        + " " + currentFacts.detailText("soulCardName"),
                13, ACCENT, true);
        contrast.setGravity(Gravity.CENTER);
        panel.addView(contrast, marginTop(10));

        TextView card = text(currentFacts.detailText("birthCardDisplay"), 27, TEXT, true);
        card.setGravity(Gravity.CENTER);
        panel.addView(card);

        TextView lifePath = text("生命靈數 " + currentFacts.detailText("lifePathDisplay"),
                23, ACCENT, true);
        lifePath.setGravity(Gravity.CENTER);
        panel.addView(lifePath, marginTop(14));

        TextView core = text(
                "天賦數 " + currentFacts.detailText("talentNumbers")
                        + "　·　態度數 " + currentFacts.detailText("attitudeNumber")
                        + "\n" + currentFacts.detailText("personalYearCalendarYear")
                        + " 流年 " + currentFacts.detailText("personalYear")
                        + " " + currentFacts.detailText("personalYearCardName")
                        + "　·　個人月 " + currentFacts.detailText("personalMonth"),
                14, MUTED, false);
        core.setGravity(Gravity.CENTER);
        core.setLineSpacing(dp(3), 1f);
        panel.addView(core, marginTop(14));

        addPanelSection(panel, "出生牌組", formatBirthCards());
        addPanelSection(panel, "四大巔峰", formatPairedLists(
                currentFacts.detail("pinnacles"), currentFacts.detail("pinnacleTiming")));
        addPanelSection(panel, "四大挑戰", formatList(currentFacts.detail("challenges")));
        addPanelSection(panel, "人生三大週期", formatList(currentFacts.detail("periodCycles")));
        addPanelSection(panel, "計算規則", formatList(currentFacts.detail("calculationNotes")));

        TextView note = text(currentFacts.detailText("note"), 11, MUTED, false);
        note.setGravity(Gravity.CENTER);
        note.setLineSpacing(dp(2), 1f);
        panel.addView(note, marginTop(16));
    }

    private void addNumerologyIdentityCard(LinearLayout row,
                                                String heading,
                                                String number,
                                                String subtitle) {
        LinearLayout card = column();
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(10), dp(14), dp(10), dp(14));
        GradientDrawable background = round(Color.rgb(31, 24, 49), 16);
        background.setStroke(dp(1), Color.rgb(86, 70, 119));
        card.setBackground(background);

        TextView h = text(heading, 12, GOLD, true);
        h.setGravity(Gravity.CENTER);
        card.addView(h);

        TextView n = text(number, 30, TEXT, true);
        n.setGravity(Gravity.CENTER);
        card.addView(n, marginTop(4));

        TextView s = text(subtitle, 10, MUTED, false);
        s.setGravity(Gravity.CENTER);
        card.addView(s, marginTop(2));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = dp(4);
        lp.rightMargin = dp(4);
        row.addView(card, lp);
    }

    private void addPanelSection(LinearLayout panel, String heading, String body) {
        TextView h = text(heading, 12, GOLD, true);
        panel.addView(h, marginTop(18));
        TextView b = text(body, 13, TEXT, false);
        b.setLineSpacing(dp(3), 1f);
        panel.addView(b, marginTop(5));
    }

    private String formatTenGods() {
        Object source = currentFacts == null ? null : currentFacts.detail("tenGods");
        return "年｜" + mapValue(source, "yearStem") + " · " + mapValue(source, "yearBranch")
                + "\n月｜" + mapValue(source, "monthStem") + " · " + mapValue(source, "monthBranch")
                + "\n日｜日主 · " + mapValue(source, "dayBranch")
                + "\n時｜" + mapValue(source, "timeStem") + " · " + mapValue(source, "timeBranch");
    }

    private String formatLuckPillars() {
        Object value = currentFacts == null ? null : currentFacts.detail("luckPillars");
        if (!(value instanceof java.util.List)) return "";
        StringBuilder out = new StringBuilder();
        Object current = currentFacts.detail("currentLuckPillar");
        String currentGanZhi = mapValue(current, "ganZhi");
        for (Object item : (java.util.List<?>) value) {
            String ganZhi = mapValue(item, "ganZhi");
            if (out.length() > 0) out.append("\n");
            if (ganZhi.equals(currentGanZhi)) out.append("● ");
            else out.append("○ ");
            out.append(ganZhi)
                    .append("　")
                    .append(mapValue(item, "startAge")).append("–")
                    .append(mapValue(item, "endAge")).append("歲　")
                    .append(mapValue(item, "startYear")).append("–")
                    .append(mapValue(item, "endYear"));
        }
        return "起運：" + currentFacts.detailText("luckStartAge")
                + " · " + currentFacts.detailText("luckDirection")
                + "\n" + out;
    }

    private String formatCurrentAnnual() {
        Object annual = currentFacts == null ? null : currentFacts.detail("currentAnnual");
        if (!(annual instanceof Map)) return "";
        return mapValue(annual, "year") + " " + mapValue(annual, "ganZhi")
                + "｜十神 " + mapValue(annual, "tenGod")
                + "｜五行 " + mapValue(annual, "element")
                + "\n" + formatList(((Map<?, ?>) annual).get("interactions"));
    }

    private String formatBirthCards() {
        Object value = currentFacts == null ? null : currentFacts.detail("birthCards");
        if (!(value instanceof java.util.List)) return "";
        StringBuilder out = new StringBuilder();
        for (Object item : (java.util.List<?>) value) {
            if (out.length() > 0) out.append("\n");
            out.append(mapValue(item, "number"))
                    .append(" ").append(mapValue(item, "name"))
                    .append("｜").append(mapValue(item, "keywords"));
        }
        return out.toString();
    }

    private String formatPairedLists(Object first, Object second) {
        if (!(first instanceof java.util.List) || !(second instanceof java.util.List)) {
            return formatList(first);
        }
        java.util.List<?> a = (java.util.List<?>) first;
        java.util.List<?> b = (java.util.List<?>) second;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < a.size(); i++) {
            if (out.length() > 0) out.append("\n");
            out.append("第").append(i + 1).append("階段：")
                    .append(a.get(i));
            if (i < b.size()) out.append("　(").append(b.get(i)).append("歲)");
        }
        return out.toString();
    }

    private String formatList(Object value) {
        if (!(value instanceof java.util.List)) return value == null ? "" : String.valueOf(value);
        StringBuilder out = new StringBuilder();
        for (Object item : (java.util.List<?>) value) {
            if (out.length() > 0) out.append("\n");
            out.append("• ").append(String.valueOf(item));
        }
        return out.toString();
    }

    private String formatMap(Object value) {
        if (!(value instanceof Map)) return value == null ? "" : String.valueOf(value);
        StringBuilder out = new StringBuilder();
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
            Object raw = entry.getValue();
            if (raw instanceof Number && ((Number) raw).intValue() == 0) continue;
            if (out.length() > 0) out.append("　");
            out.append(entry.getKey()).append(" ").append(raw);
        }
        return out.toString();
    }

    private String mapValue(Object source, String key) {
        if (!(source instanceof Map)) return "";
        Object value = ((Map<?, ?>) source).get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private int intMapValue(Object source, String key) {
        String value = mapValue(source, key);
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String shortHidden(String value) {
        if (value == null) return "";
        return value.replace("[", "").replace("]", "").replace(", ", "·");
    }

    private void addSection(String heading, String body) {
        TextView h = text(heading, 12, GOLD, true);
        resultCard.addView(h, marginTop(18));
        TextView b = text(body, 16, TEXT, false);
        b.setLineSpacing(dp(3), 1f);
        resultCard.addView(b, marginTop(5));
    }

    private void startTeacherExplanation() {
        OperationLog.add(this, "TEACHER_REQUESTED",
                selectedMode.name() + " · " + selectedAiStyle.name());
        if (currentFacts == null || currentResult == null) {
            Toast.makeText(this, "請先完成一次算命", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!AppConfig.hasGeminiApiKey(this)) {
            Toast.makeText(this, "先設定 Gemini Key 才能使用語音老師", Toast.LENGTH_SHORT).show();
            showApiKeyDialog();
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            OperationLog.add(this, "MIC_PERMISSION_REQUESTED", "");
            pendingTeacherStart = true;
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_TEACHER_AUDIO);
            return;
        }
        openTeacherDialog();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_TEACHER_AUDIO) return;
        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (pendingTeacherStart && granted) {
            OperationLog.add(this, "MIC_PERMISSION_GRANTED", "");
            pendingTeacherStart = false;
            openTeacherDialog();
        } else {
            OperationLog.add(this, "MIC_PERMISSION_DENIED", "");
            pendingTeacherStart = false;
            Toast.makeText(this, "需要麥克風權限才能跟老師對話", Toast.LENGTH_SHORT).show();
        }
    }

    private void openTeacherDialog() {
        closeTeacher();

        LinearLayout body = column();
        body.setPadding(dp(20), dp(14), dp(20), dp(10));

        TextView title = text("命理老師", 22, TEXT, true);
        body.addView(title);

        TextView hint = text(
                "老師會先講 60–90 秒重點。講完後直接開口追問，不需要按住麥克風。",
                13, MUTED, false);
        hint.setLineSpacing(dp(3), 1f);
        body.addView(hint, marginTop(6));

        teacherStatusText = text("正在準備…", 13, ACCENT, true);
        body.addView(teacherStatusText, marginTop(14));

        TextView youLabel = text("你剛剛說", 11, GOLD, true);
        body.addView(youLabel, marginTop(16));
        teacherInputText = text("—", 14, TEXT, false);
        teacherInputText.setLineSpacing(dp(3), 1f);
        body.addView(teacherInputText, marginTop(4));

        TextView teacherLabel = text("老師正在講", 11, GOLD, true);
        body.addView(teacherLabel, marginTop(14));
        teacherOutputText = text("等待老師上線…", 15, TEXT, false);
        teacherOutputText.setLineSpacing(dp(4), 1f);
        body.addView(teacherOutputText, marginTop(4));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button interrupt = secondaryButton("我要問");
        interrupt.setOnClickListener(v -> {
            OperationLog.add(this, "TEACHER_INTERRUPT", "");
            GeminiFortuneLiveSession session = teacherSession;
            if (session != null) session.interrupt();
        });
        LinearLayout.LayoutParams interruptLp =
                new LinearLayout.LayoutParams(0, dp(48), 1f);
        interruptLp.rightMargin = dp(8);
        actions.addView(interrupt, interruptLp);

        Button close = secondaryButton("結束");
        close.setOnClickListener(v -> {
            AlertDialog dialog = teacherDialog;
            if (dialog != null) dialog.dismiss();
        });
        actions.addView(close, new LinearLayout.LayoutParams(0, dp(48), 1f));
        body.addView(actions, marginTop(18));

        teacherDialog = new AlertDialog.Builder(this)
                .setView(body)
                .create();
        teacherDialog.setOnDismissListener(dialog -> closeTeacherSessionOnly());
        teacherDialog.show();

        String teacherPrompt = FortuneTeacherPrompt.systemPrompt(
                selectedMode, selectedAiStyle, currentFacts);
        String opening = FortuneTeacherPrompt.openingPrompt(
                nameInput.getText().toString());

        OperationLog.add(this, "TEACHER_START",
                selectedMode.name() + " · media_audio");
        teacherSession = new GeminiFortuneLiveSession(
                this,
                AppConfig.getGeminiApiKey(this),
                FortuneTeacherPrompt.voiceName(selectedAiStyle),
                teacherPrompt,
                opening,
                new GeminiFortuneLiveSession.Listener() {
                    @Override public void onStatus(String status) {
                        runOnUiThread(() -> {
                            if (teacherStatusText != null) teacherStatusText.setText(status);
                        });
                    }

                    @Override public void onReady() {
                        OperationLog.add(MainActivity.this, "TEACHER_READY", "");
                        runOnUiThread(() -> {
                            if (teacherStatusText != null) {
                                teacherStatusText.setText("老師正在看你的命盤…");
                            }
                        });
                    }

                    @Override public void onInputTranscript(String textValue) {
                        OperationLog.add(MainActivity.this,
                                "TEACHER_USER_SPOKE",
                                "chars=" + (textValue == null ? 0 : textValue.length()));
                        runOnUiThread(() -> {
                            if (teacherInputText != null) teacherInputText.setText(textValue);
                        });
                    }

                    @Override public void onOutputTranscript(String textValue) {
                        OperationLog.add(MainActivity.this,
                                "TEACHER_REPLIED",
                                "chars=" + (textValue == null ? 0 : textValue.length()));
                        runOnUiThread(() -> {
                            if (teacherOutputText != null) teacherOutputText.setText(textValue);
                        });
                    }

                    @Override public void onSpeakingChanged(boolean speaking) {
                        runOnUiThread(() -> {
                            if (teacherStatusText != null) {
                                teacherStatusText.setText(
                                        speaking ? "老師正在講…" : "你可以直接追問");
                            }
                        });
                    }

                    @Override public void onError(String message) {
                        OperationLog.add(MainActivity.this,
                                "TEACHER_ERROR",
                                message == null ? "unknown" : message);
                        runOnUiThread(() -> {
                            if (teacherStatusText != null) {
                                teacherStatusText.setText("語音老師暫時無法使用");
                            }
                            if (teacherOutputText != null) teacherOutputText.setText(message);
                        });
                    }
                });
        teacherSession.start();
    }

    private void closeTeacher() {
        AlertDialog dialog = teacherDialog;
        teacherDialog = null;
        if (dialog != null && dialog.isShowing()) {
            dialog.setOnDismissListener(null);
            dialog.dismiss();
        }
        closeTeacherSessionOnly();
    }

    private void closeTeacherSessionOnly() {
        GeminiFortuneLiveSession session = teacherSession;
        if (session != null) OperationLog.add(this, "TEACHER_STOP", "");
        teacherSession = null;
        if (session != null) {
            try { session.close(); } catch (Exception ignored) {}
        }
        teacherStatusText = null;
        teacherInputText = null;
        teacherOutputText = null;
    }

    private void shareResult() {
        if (currentResult == null) return;
        OperationLog.add(this, "SHARE_RESULT", selectedMode.name());
        String payload = aiCopy == null
                ? currentResult.shareText()
                : aiCopy.shareText(currentResult);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, payload);
        startActivity(Intent.createChooser(intent, "分享你的命運"));
    }

    private void showApiKeyDialog() {
        final EditText keyInput = new EditText(this);
        keyInput.setSingleLine(true);
        keyInput.setHint("Gemini API Key");
        keyInput.setText(AppConfig.getGeminiApiKey(this));
        keyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        keyInput.setPadding(dp(16), dp(8), dp(16), dp(8));

        new AlertDialog.Builder(this)
                .setTitle("AI 模式")
                .setMessage("Gemini 只負責解讀與文案，底層命盤由本地 deterministic engine 計算。可在主畫面切換嚴謹／普通／風趣三種回應風格。")
                .setView(keyInput)
                .setPositiveButton("儲存", (dialog, which) -> {
                    AppConfig.setGeminiApiKey(this, keyInput.getText().toString());
                    OperationLog.add(this, "GEMINI_KEY_SAVED", "value_hidden");
                    refreshAiStatus();
                    Toast.makeText(this, "Gemini Key 已儲存於本機", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .setNeutralButton("清除", (dialog, which) -> {
                    AppConfig.setGeminiApiKey(this, "");
                    OperationLog.add(this, "GEMINI_KEY_CLEARED", "");
                    refreshAiStatus();
                })
                .show();
    }

    private void refreshAiStatus() {
        if (aiStatus == null) return;
        aiStatus.setText(AppConfig.hasGeminiApiKey(this) ? "AI：ON ⚙" : "AI：OFF ⚙");
    }

    private void closeAgent() {
        AgentHarness harness = activeHarness;
        activeHarness = null;
        if (harness != null) {
            try { harness.close(); } catch (Exception ignored) {}
        }
    }

    private FortunePreset currentPreset() {
        return new FortunePreset(
                nameInput.getText().toString(),
                birthInput.getText().toString(),
                birthTimeInput.getText().toString(),
                selectedGender,
                selectedMode);
    }

    private void restoreLastProfile() {
        FortunePreset preset = FortunePresetStore.loadLast(this);
        if (preset != null) applyPreset(preset);
    }

    private void saveCurrentPreset() {
        FortunePreset preset = currentPreset();
        if (preset.name.isEmpty()) {
            Toast.makeText(this, "先輸入名字再儲存 preset", Toast.LENGTH_SHORT).show();
            return;
        }
        if (preset.birthDate.isEmpty()) {
            Toast.makeText(this, "先選生日再儲存 preset", Toast.LENGTH_SHORT).show();
            return;
        }
        if (preset.mode == FortuneMode.BA_ZI && preset.birthTime.isEmpty()) {
            Toast.makeText(this, "八字 preset 需要出生時間", Toast.LENGTH_SHORT).show();
            return;
        }
        if (preset.mode == FortuneMode.BA_ZI && preset.gender.isEmpty()) {
            Toast.makeText(this, "八字 preset 需要選擇性別", Toast.LENGTH_SHORT).show();
            return;
        }
        FortunePresetStore.savePreset(this, preset);
        OperationLog.add(this, "PRESET_SAVED", preset.label());
        Toast.makeText(this, "已儲存：" + preset.label(), Toast.LENGTH_SHORT).show();
    }

    private void showPresetPicker() {
        final List<FortunePreset> presets = FortunePresetStore.loadPresets(this);
        if (presets.isEmpty()) {
            Toast.makeText(this, "目前還沒有 preset", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] labels = new String[presets.size()];
        for (int i = 0; i < presets.size(); i++) labels[i] = presets.get(i).label();

        new AlertDialog.Builder(this)
                .setTitle("選擇常用資料")
                .setItems(labels, (dialog, which) -> {
                    OperationLog.add(this, "PRESET_LOADED", presets.get(which).label());
                    applyPreset(presets.get(which));
                })
                .setNeutralButton("清除全部", (dialog, which) -> {
                    FortunePresetStore.clearPresets(this);
                    OperationLog.add(this, "PRESETS_CLEARED", "");
                    Toast.makeText(this, "已清除 presets", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void applyPreset(FortunePreset preset) {
        if (preset == null) return;
        nameInput.setText(preset.name);
        birthInput.setText(preset.birthDate);
        birthTimeInput.setText(preset.birthTime);
        selectMode(preset.mode);
        if (!preset.gender.isEmpty()) selectGender(preset.gender);
        else {
            selectedGender = "";
            maleButton.setBackground(round(CARD_2, 14));
            femaleButton.setBackground(round(CARD_2, 14));
            maleButton.setTextColor(TEXT);
            femaleButton.setTextColor(TEXT);
        }
    }

    private void restoreInstanceState(Bundle state) {
        try {
            nameInput.setText(state.getString("state_name", ""));
            birthInput.setText(state.getString("state_birth_date", ""));
            birthTimeInput.setText(state.getString("state_birth_time", ""));
            selectedGender = state.getString("state_gender", "");
            selectedMode = FortuneMode.valueOf(
                    state.getString("state_mode", FortuneMode.BA_ZI.name()));
            selectedAiStyle = AiStyle.valueOf(
                    state.getString("state_ai_style", AiStyle.FUNNY.name()));

            selectMode(selectedMode);
            if (!selectedGender.isEmpty()) selectGender(selectedGender);
            updateAiStyleButtons();

            if (state.getBoolean("state_has_result", false)) {
                FortuneProfile profile = new FortuneProfile(
                        nameInput.getText().toString(),
                        birthInput.getText().toString(),
                        birthTimeInput.getText().toString(),
                        selectedGender);
                currentFacts = engine.calculateFacts(selectedMode, profile, new Date());
                currentResult = engine.calculate(selectedMode, profile, new Date());

                String aiRaw = state.getString("state_ai_copy", "");
                if (!aiRaw.isEmpty()) {
                    try { aiCopy = AiFortuneCopy.parse(aiRaw); }
                    catch (Exception ignored) { aiCopy = null; }
                }
                renderResult(currentResult, false);
            }
            OperationLog.add(this, "STATE_RESTORED",
                    state.getBoolean("state_has_result", false)
                            ? "result_restored" : "input_restored");
        } catch (Exception error) {
            restoreLastProfile();
            OperationLog.add(this, "STATE_RESTORE_FAILED",
                    error.getMessage() == null ? "unknown" : error.getMessage());
        }
    }

    private String serializeAiCopy(AiFortuneCopy copy) {
        try {
            return new JSONObject()
                    .put("title", copy.title)
                    .put("overview", copy.overview)
                    .put("personality", copy.personality)
                    .put("careerWealth", copy.careerWealth)
                    .put("relationships", copy.relationships)
                    .put("timing", copy.timing)
                    .put("translation", copy.translation)
                    .put("punchline", copy.punchline)
                    .put("advice", copy.advice)
                    .put("shareText", copy.shareText)
                    .toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private void showOperationLog() {
        OperationLog.add(this, "OPEN_OPERATION_LOG", "");
        List<OperationLog.Entry> entries = OperationLog.list(this);
        LinearLayout body = column();
        body.setPadding(dp(18), dp(10), dp(18), dp(10));

        if (entries.isEmpty()) {
            body.addView(text("目前還沒有操作記錄", 14, MUTED, false));
        } else {
            for (OperationLog.Entry entry : entries) {
                TextView item = text(entry.display(), 13, TEXT, false);
                item.setLineSpacing(dp(2), 1f);
                body.addView(item, marginTop(10));
            }
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);

        new AlertDialog.Builder(this)
                .setTitle("操作記錄")
                .setView(scroll)
                .setPositiveButton("關閉", null)
                .setNeutralButton("清除", (dialog, which) -> {
                    OperationLog.clear(this);
                    OperationLog.add(this, "OPERATION_LOG_CLEARED", "");
                    Toast.makeText(this, "已清除操作記錄", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        String raw = birthInput.getText().toString().trim();
        if (raw.matches("\\d{4}-\\d{2}-\\d{2}")) {
            try {
                String[] p = raw.split("-");
                calendar.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]));
            } catch (Exception ignored) {}
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    String value = String.format(
                            java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, day);
                    birthInput.setText(value);
                    OperationLog.add(this, "BIRTH_DATE_SELECTED", value);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        String raw = birthTimeInput.getText().toString().trim();
        if (raw.matches("\\d{2}:\\d{2}")) {
            try {
                String[] p = raw.split(":");
                hour = Integer.parseInt(p[0]);
                minute = Integer.parseInt(p[1]);
            } catch (Exception ignored) {}
        }
        new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {
                    String value = String.format(
                            java.util.Locale.US, "%02d:%02d", selectedHour, selectedMinute);
                    birthTimeInput.setText(value);
                    OperationLog.add(this, "BIRTH_TIME_SELECTED", value);
                },
                hour,
                minute,
                true).show();
    }

    private void selectAiStyle(AiStyle style) {
        selectedAiStyle = style == null ? AiStyle.FUNNY : style;
        OperationLog.add(this, "AI_STYLE_SELECTED", selectedAiStyle.name());
        AppConfig.setAiStyle(this, selectedAiStyle);
        updateAiStyleButtons();
        Toast.makeText(this, "AI 風格：" + selectedAiStyle.label(), Toast.LENGTH_SHORT).show();
    }

    private void updateAiStyleButtons() {
        if (strictStyleButton == null || normalStyleButton == null || funnyStyleButton == null) return;
        applyStyleButton(strictStyleButton, selectedAiStyle == AiStyle.STRICT);
        applyStyleButton(normalStyleButton, selectedAiStyle == AiStyle.NORMAL);
        applyStyleButton(funnyStyleButton, selectedAiStyle == AiStyle.FUNNY);
    }

    private void applyStyleButton(Button button, boolean selected) {
        button.setBackground(round(selected ? ACCENT : CARD_2, 14));
        button.setTextColor(selected ? Color.rgb(30, 22, 46) : TEXT);
        button.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
    }

    private Button styleButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setTextColor(TEXT);
        button.setBackground(round(CARD_2, 14));
        return button;
    }

    private Button secondaryButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setTextColor(TEXT);
        button.setBackground(round(CARD_2, 14));
        return button;
    }

    private Button genderButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setTextColor(TEXT);
        button.setBackground(round(CARD_2, 14));
        return button;
    }

    private EditText input(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setHintTextColor(Color.rgb(142, 131, 163));
        input.setTextColor(TEXT);
        input.setTextSize(15);
        input.setSingleLine(true);
        input.setPadding(dp(14), 0, dp(14), 0);
        input.setBackground(round(Color.rgb(29, 23, 45), 14));
        input.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));
        return input;
    }

    private TextView label(String value) { return text(value, 14, TEXT, true); }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout.LayoutParams marginTop(int dp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(dp);
        return lp;
    }

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
