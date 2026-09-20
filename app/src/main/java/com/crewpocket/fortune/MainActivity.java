package com.crewpocket.fortune;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.magic76.crew.agent.AgentEvent;
import com.magic76.crew.agent.AgentHarness;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
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
    private Button baZiModeButton;
    private Button tarotModeButton;
    private Button vedicModeButton;
    private LinearLayout vedicLocationSection;
    private EditText birthPlaceNameInput;
    private EditText latitudeInput;
    private EditText longitudeInput;
    private EditText timeZoneInput;
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
    private String pendingTeacherQuestion = "";
    private TextView aiLoadingStageText;
    private int aiLoadingStage = 0;
    private int selectedResultTab = 0;
    private LinearLayout resultTabContent;

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
        outState.putString("state_birth_place_name",
                birthPlaceNameInput == null ? "" : birthPlaceNameInput.getText().toString());
        outState.putString("state_latitude",
                latitudeInput == null ? "" : latitudeInput.getText().toString());
        outState.putString("state_longitude",
                longitudeInput == null ? "" : longitudeInput.getText().toString());
        outState.putString("state_timezone",
                timeZoneInput == null ? "" : timeZoneInput.getText().toString());
        outState.putString("state_mode", selectedMode.name());
        outState.putString("state_ai_style", selectedAiStyle.name());
        outState.putBoolean("state_has_result", currentResult != null && currentFacts != null);
        outState.putInt("state_result_tab", selectedResultTab);
        if (aiCopy != null) outState.putString("state_ai_copy", serializeAiCopy(aiCopy));
        OperationLog.add(this, "STATE_SAVED",
                currentResult == null ? "no_result" : "result_saved");
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = column();
        final int baseLeft = dp(14);
        final int baseTop = dp(16);
        final int baseRight = dp(14);
        final int baseBottom = dp(24);
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
        history.setPadding(dp(8), dp(6), 0, dp(6));
        history.setOnClickListener(v -> showOperationLog());
        top.addView(history);

        aiStatus = text("", 13, ACCENT, true);
        aiStatus.setGravity(Gravity.END);
        aiStatus.setPadding(dp(10), dp(6), 0, dp(6));
        aiStatus.setOnClickListener(v -> {
            OperationLog.add(this, "OPEN_AI_SETTINGS", "");
            showApiKeyDialog();
        });
        top.addView(aiStatus);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_launcher_foreground_art);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        logo.setPadding(dp(2), dp(2), dp(2), dp(2));
        logo.setBackground(round(CARD_2, 12));
        logo.setContentDescription("Crew Fortune");
        logo.setOnClickListener(v -> showAboutDialog());
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(34), dp(34));
        logoLp.leftMargin = dp(8);
        top.addView(logo, logoLp);

        root.addView(top);

        TextView title = text("很認真算，\n別太認真信。", 34, TEXT, true);
        title.setLineSpacing(0, 1.04f);
        root.addView(title, marginTop(4));

        TextView sub = text("底層數字固定，AI 每次換一種方式講你。\n沒有 AI 也能算，有 AI 就比較會嘴。", 15, MUTED, false);
        sub.setLineSpacing(dp(2), 1f);
        root.addView(sub, marginTop(6));

        LinearLayout form = column();
        form.setPadding(dp(12), dp(12), dp(12), dp(12));
        form.setBackground(round(CARD, 18));
        root.addView(form, marginTop(6));

        form.addView(label("選擇算命方式"));

        LinearLayout modeRow = new LinearLayout(this);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        baZiModeButton = modeButton("八字");
        tarotModeButton = modeButton("塔羅生命靈數");
        vedicModeButton = modeButton("印度星盤");
        baZiModeButton.setTextSize(12);
        tarotModeButton.setTextSize(12);
        vedicModeButton.setTextSize(12);
        baZiModeButton.setOnClickListener(v -> selectMode(FortuneMode.BA_ZI));
        tarotModeButton.setOnClickListener(v -> selectMode(FortuneMode.TAROT_NUMEROLOGY));
        vedicModeButton.setOnClickListener(v -> selectMode(FortuneMode.VEDIC_ASTROLOGY));
        LinearLayout.LayoutParams modeLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        modeLp.rightMargin = dp(5);
        modeRow.addView(baZiModeButton, modeLp);
        LinearLayout.LayoutParams tarotLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        tarotLp.rightMargin = dp(5);
        modeRow.addView(tarotModeButton, tarotLp);
        modeRow.addView(vedicModeButton, new LinearLayout.LayoutParams(0, dp(46), 1f));
        form.addView(modeRow, marginTop(6));

        modeLabel = text("", 12, MUTED, false);
        modeLabel.setLineSpacing(dp(2), 1f);
        form.addView(modeLabel, marginTop(5));

        form.addView(label("基本資料"), marginTop(8));
        nameInput = input("你的名字");
        birthInput = input("生日，例如 1985-07-22");
        birthInput.setFocusable(false);
        birthInput.setClickable(true);
        birthInput.setOnClickListener(v -> showDatePicker());

        birthTimeInput = input("出生地當地時間，例如 14:30");
        birthTimeInput.setFocusable(false);
        birthTimeInput.setClickable(true);
        birthTimeInput.setOnClickListener(v -> showTimePicker());
        form.addView(nameInput, marginTop(8));
        form.addView(birthInput, marginTop(6));
        form.addView(birthTimeInput, marginTop(6));

        genderRow = new LinearLayout(this);
        genderRow.setOrientation(LinearLayout.HORIZONTAL);
        maleButton = genderButton("男");
        femaleButton = genderButton("女");
        maleButton.setOnClickListener(v -> selectGender("male"));
        femaleButton.setOnClickListener(v -> selectGender("female"));
        LinearLayout.LayoutParams genderLp = new LinearLayout.LayoutParams(0, dp(44), 1f);
        genderLp.rightMargin = dp(6);
        genderRow.addView(maleButton, genderLp);
        genderRow.addView(femaleButton, new LinearLayout.LayoutParams(0, dp(44), 1f));
        form.addView(genderRow, marginTop(6));

        vedicLocationSection = column();
        TextView vedicRule = text(
                "印度星盤 · Sidereal · Lahiri · Whole Sign\n"
                        + "城市名稱只做顯示；計算直接使用座標與出生地時區，不會交給 AI 猜。",
                11, GOLD, true);
        vedicRule.setLineSpacing(dp(2), 1f);
        vedicLocationSection.addView(vedicRule);

        birthPlaceNameInput = input("出生城市，例如 新北市（只做稱呼）");
        latitudeInput = input("Latitude，例如 25.0120");
        longitudeInput = input("Longitude，例如 121.4657");
        timeZoneInput = input("Timezone，例如 Asia/Taipei 或 +08:00");
        latitudeInput.setInputType(
                InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_FLAG_DECIMAL
                        | InputType.TYPE_NUMBER_FLAG_SIGNED);
        longitudeInput.setInputType(
                InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_FLAG_DECIMAL
                        | InputType.TYPE_NUMBER_FLAG_SIGNED);
        vedicLocationSection.addView(birthPlaceNameInput, marginTop(6));
        vedicLocationSection.addView(latitudeInput, marginTop(6));
        vedicLocationSection.addView(longitudeInput, marginTop(6));
        vedicLocationSection.addView(timeZoneInput, marginTop(6));

        TextView locationHint = text(
                "第一版不綁 geocoding API，避免城市同名、配額與 API key 影響排盤。"
                        + "之後可直接把城市搜尋結果回填這三個 deterministic 欄位。",
                10, MUTED, false);
        locationHint.setLineSpacing(dp(2), 1f);
        vedicLocationSection.addView(locationHint, marginTop(4));
        vedicLocationSection.setVisibility(View.GONE);
        form.addView(vedicLocationSection, marginTop(6));

        LinearLayout presetRow = new LinearLayout(this);
        presetRow.setOrientation(LinearLayout.HORIZONTAL);
        Button choosePreset = secondaryButton("常用資料");
        Button savePreset = secondaryButton("儲存 preset");
        choosePreset.setOnClickListener(v -> showPresetPicker());
        savePreset.setOnClickListener(v -> saveCurrentPreset());
        LinearLayout.LayoutParams presetLp = new LinearLayout.LayoutParams(0, dp(44), 1f);
        presetLp.rightMargin = dp(6);
        presetRow.addView(choosePreset, presetLp);
        presetRow.addView(savePreset, new LinearLayout.LayoutParams(0, dp(44), 1f));
        form.addView(presetRow, marginTop(6));

        TextView styleLabel = text("AI 回應風格", 12, MUTED, true);
        form.addView(styleLabel, marginTop(6));

        LinearLayout styleRow = new LinearLayout(this);
        styleRow.setOrientation(LinearLayout.HORIZONTAL);
        strictStyleButton = styleButton("嚴謹");
        normalStyleButton = styleButton("普通");
        funnyStyleButton = styleButton("風趣");
        strictStyleButton.setOnClickListener(v -> selectAiStyle(AiStyle.STRICT));
        normalStyleButton.setOnClickListener(v -> selectAiStyle(AiStyle.NORMAL));
        funnyStyleButton.setOnClickListener(v -> selectAiStyle(AiStyle.FUNNY));
        LinearLayout.LayoutParams styleLp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        styleLp.rightMargin = dp(5);
        styleRow.addView(strictStyleButton, styleLp);
        LinearLayout.LayoutParams styleLp2 = new LinearLayout.LayoutParams(0, dp(42), 1f);
        styleLp2.rightMargin = dp(5);
        styleRow.addView(normalStyleButton, styleLp2);
        styleRow.addView(funnyStyleButton, new LinearLayout.LayoutParams(0, dp(42), 1f));
        form.addView(styleRow, marginTop(4));

        TextView styleHint = text("嚴謹：專業報告｜普通：白話平衡｜風趣：嘴得準但不傷人\n只改 AI 說話方式，不改命盤計算結果", 11, MUTED, false);
        form.addView(styleHint, marginTop(2));

        Button calculate = new Button(this);
        calculate.setText("開始算命");
        calculate.setTextSize(17);
        calculate.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        calculate.setTextColor(Color.rgb(30, 22, 46));
        calculate.setAllCaps(false);
        calculate.setBackground(round(ACCENT, 20));
        calculate.setOnClickListener(v -> calculate());
        LinearLayout.LayoutParams calcLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        calcLp.topMargin = dp(12);
        root.addView(calculate, calcLp);

        resultCard = column();
        resultCard.setPadding(dp(12), dp(12), dp(12), dp(12));
        resultCard.setBackground(round(CARD, 20));
        resultCard.setVisibility(View.GONE);
        root.addView(resultCard, marginTop(9));

        updateModeSelectionUi();

        TextView foot = text("娛樂用途 · 八字＋塔羅生命靈數＋印度星盤", 12, MUTED, false);
        foot.setGravity(Gravity.CENTER);
        root.addView(foot, marginTop(9));
        return scroll;
    }

    private void selectMode(FortuneMode mode) {
        FortuneMode previous = selectedMode;
        selectedMode = mode;
        OperationLog.add(this, "MODE_SELECTED", mode.name());
        if (previous != mode && currentResult != null) {
            closeTeacher();
            closeAgent();
            currentResult = null;
            currentFacts = null;
            aiCopy = null;
            selectedResultTab = 0;
            if (resultCard != null) resultCard.setVisibility(View.GONE);
        }
        boolean isBaZi = mode == FortuneMode.BA_ZI;
        boolean isVedic = mode == FortuneMode.VEDIC_ASTROLOGY;
        birthTimeInput.setVisibility((isBaZi || isVedic) ? View.VISIBLE : View.GONE);
        genderRow.setVisibility(isBaZi ? View.VISIBLE : View.GONE);
        if (vedicLocationSection != null) {
            vedicLocationSection.setVisibility(isVedic ? View.VISIBLE : View.GONE);
        }
        updateModeSelectionUi();
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
        selectedResultTab = 0;
        OperationLog.add(this, "CALCULATE_START", selectedMode.name());
        closeTeacher();
        closeAgent();
        try {
            FortuneProfile profile = buildCurrentProfile();
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
        updateAiLoadingStage(0);
        OperationLog.add(this, "AI_INTERPRETATION_START",
                selectedMode.name() + " · " + selectedAiStyle.name());
        startAiCopyAttempt(profile, 0, "");
    }

    private void startAiCopyAttempt(
            FortuneProfile profile,
            int attempt,
            String retryReason) {
        synchronized (aiBuffer) {
            aiBuffer.setLength(0);
        }

        try {
            final GeminiTextModelSession session = new GeminiTextModelSession(
                    AppConfig.getGeminiApiKey(this),
                    selectedAiStyle.temperature());

            activeHarness = FortuneAgentRuntime.createInterpretation(session, new AgentHarness.Listener() {
                @Override public void onAgentEvent(AgentEvent event) {
                    if (event == null) return;
                    switch (event.type()) {
                        case MODEL_TEXT:
                            if (aiLoadingStage < 1) {
                                runOnUiThread(() -> updateAiLoadingStage(1));
                            }
                            synchronized (aiBuffer) {
                                aiBuffer.append(event.text());
                            }
                            break;

                        case TURN_COMPLETED:
                            final String completed;
                            synchronized (aiBuffer) {
                                completed = aiBuffer.toString().trim();
                            }

                            final AiFortuneCopy parsed;
                            try {
                                parsed = AiFortuneCopy.parse(completed);
                            } catch (IllegalArgumentException parseError) {
                                String retryCause = "parse_error: "
                                        + safeErrorMessage(parseError)
                                        + " · finishReason=" + session.lastFinishReason();
                                String detail = retryCause
                                        + " · attempt=" + (attempt + 1)
                                        + " · " + safeAiResponseSummary(
                                                completed, profile.name);

                                OperationLog.add(
                                        MainActivity.this,
                                        "AI_INTERPRETATION_PARSE_ERROR",
                                        detail);

                                if (attempt == 0) {
                                    OperationLog.add(
                                            MainActivity.this,
                                            "AI_INTERPRETATION_RETRY",
                                            detail);
                                    runOnUiThread(() -> updateAiLoadingStage(2));
                                    closeAgent();
                                    runOnUiThread(() ->
                                            startAiCopyAttempt(profile, 1, retryCause));
                                    return;
                                }

                                OperationLog.add(
                                        MainActivity.this,
                                        "AI_INTERPRETATION_FAILED",
                                        detail);
                                showAiFallback(
                                        "AI 命理師回覆格式仍不完整，已保留本地完整結果。");
                                closeAgent();
                                return;
                            }

                            String qualityIssues = parsed.qualityIssueSummary(selectedMode);
                            if (!qualityIssues.isEmpty() && attempt == 0) {
                                String retryCause = "quality_short: " + qualityIssues;
                                String detail = retryCause
                                        + " · finishReason=" + session.lastFinishReason()
                                        + " · " + safeAiResponseSummary(
                                                completed, profile.name);
                                OperationLog.add(
                                        MainActivity.this,
                                        "AI_INTERPRETATION_RETRY",
                                        detail);
                                runOnUiThread(() -> updateAiLoadingStage(2));
                                closeAgent();
                                runOnUiThread(() ->
                                        startAiCopyAttempt(profile, 1, retryCause));
                                return;
                            }

                            if (!qualityIssues.isEmpty()) {
                                OperationLog.add(
                                        MainActivity.this,
                                        "AI_INTERPRETATION_QUALITY_WARNING",
                                        qualityIssues);
                            }

                            runOnUiThread(() -> {
                                aiCopy = parsed;
                                OperationLog.add(
                                        MainActivity.this,
                                        "AI_INTERPRETATION_SUCCESS",
                                        selectedAiStyle.name()
                                                + " · attempt=" + (attempt + 1)
                                                + " · chars=" + completed.length());
                                if (currentResult != null) {
                                    renderResult(currentResult, false);
                                }
                            });
                            closeAgent();
                            break;

                        case ERROR:
                            OperationLog.add(
                                    MainActivity.this,
                                    "AI_INTERPRETATION_FAILED",
                                    "model_error · attempt=" + (attempt + 1)
                                            + " · model=" + session.lastModel()
                                            + " · models=" + session.lastModelAttempts()
                                            + " · finishReason="
                                            + session.lastFinishReason()
                                            + " · error="
                                            + safeErrorMessage(event.error()));
                            showAiFallback(
                                    "AI 命理師暫時無法完成解讀，已保留本地完整結果。");
                            closeAgent();
                            break;

                        default:
                            break;
                    }
                }
            }, selectedAiStyle);

            activeHarness.start();
            activeHarness.submitText(
                    buildAiRequest(profile, attempt, retryReason));
        } catch (Exception error) {
            OperationLog.add(
                    this,
                    "AI_INTERPRETATION_FAILED",
                    "startup_error: " + safeErrorMessage(error)
                            + " · attempt=" + (attempt + 1));
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

    private String buildAiRequest(
            FortuneProfile profile,
            int attempt,
            String retryReason) {
        if (currentFacts == null) {
            throw new IllegalStateException("deterministic facts are missing");
        }

        String factsJson = new JSONObject(currentFacts.details).toString();
        StringBuilder value = new StringBuilder();
        value.append("請只解讀以下已由本機完成的 deterministic facts。")
                .append("不要重新計算，不要呼叫工具，不要修正輸入格式，也絕對不要使用預設值。\n");
        value.append("mode=").append(selectedMode.name()).append('\n');
        value.append("displayName=").append(profile.name).append('\n');
        value.append("basis=").append(currentFacts.basis).append('\n');
        value.append("aiStyle=").append(selectedAiStyle.name()).append('\n');
        value.append("deterministicFacts=").append(factsJson).append('\n');

        if (attempt > 0) {
            value.append("RETRY_MODE=JSON_REPAIR_AND_EXPANSION\n");
            value.append("上一次回覆不符合格式或內容過短。原因摘要：")
                    .append(retryReason == null ? "" : retryReason)
                    .append('\n');
            value.append("這次必須重新輸出一個完整、可解析的 JSON object。")
                    .append("不得輸出 markdown code fence、前言、後記或任何 JSON 外文字。")
                    .append("不得省略 title, overview, personality, career, wealth, relationships, ")
                    .append("currentCycle, longTerm, keyYears, topTraits, topTraitEvidence, followUps, translation, punchline, advice, shareText。")
                    .append("不要縮短內容來逃避欄位要求。\n");
        }

        value.append("creativeVariant=").append(System.nanoTime()).append('\n');
        value.append("creativeVariant 只允許改變措辭與笑點。")
                .append("所有數字、干支、十神、大運、流年、塔羅牌、天賦數、行星、宮位、Nakshatra、Dasha")
                .append("都必須逐字遵守 deterministicFacts。");
        return value.toString();
    }

    private String safeAiResponseSummary(String raw, String displayName) {
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
        if (displayName != null && !displayName.trim().isEmpty()) {
            clean = clean.replace(displayName.trim(), "[name]");
        }
        String preview = clean.length() <= 300
                ? clean
                : clean.substring(0, 300) + "…";
        return "len=" + source.length() + " · preview=" + preview;
    }

    private String safeErrorMessage(Throwable error) {
        if (error == null) return "unknown";
        String value = error.getMessage();
        if (value == null || value.trim().isEmpty()) {
            value = error.getClass().getSimpleName();
        }
        value = value.replace('\n', ' ').replace('\r', ' ').trim();
        return value.length() <= 220 ? value : value.substring(0, 220) + "…";
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

        if (result.mode == FortuneMode.BA_ZI
                || result.mode == FortuneMode.TAROT_NUMEROLOGY
                || result.mode == FortuneMode.VEDIC_ASTROLOGY) {
            addUnifiedTabbedResult(result, aiLoading);
            resultCard.setVisibility(View.VISIBLE);
            return;
        }

        if (result.mode == FortuneMode.BA_ZI) {
            addBaZiResultPanel();
        } else if (result.mode == FortuneMode.TAROT_NUMEROLOGY) {
            addTarotResultPanel();
        } else {
            TextView score = text(primaryMetric(result), 30, ACCENT, true);
            resultCard.addView(score, marginTop(8));

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
        resultCard.addView(source, marginTop(6));

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


    private void addUnifiedTabbedResult(FortuneResult result, boolean aiLoading) {
        TextView basis = text("計算依據｜" + result.basis, 12, MUTED, false);
        resultCard.addView(basis, marginTop(6));

        HorizontalScrollView tabScroll = new HorizontalScrollView(this);
        tabScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabScroll.addView(tabs);

        final String[] labels = {"總覽", "本命", "流年", "主題", "解讀"};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            Button tab = new Button(this);
            tab.setText(labels[i]);
            tab.setTextSize(13);
            tab.setAllCaps(false);
            boolean selected = selectedResultTab == i;
            tab.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
            tab.setTextColor(selected ? Color.rgb(30, 22, 46) : TEXT);
            tab.setBackground(round(selected ? ACCENT : CARD_2, 13));
            tab.setPadding(dp(10), 0, dp(10), 0);
            tab.setOnClickListener(v -> {
                selectedResultTab = index;
                OperationLog.add(this, "RESULT_TAB_SELECTED", labels[index]);
                renderResult(currentResult, activeHarness != null && aiCopy == null);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(40));
            lp.rightMargin = dp(4);
            tabs.addView(tab, lp);
        }
        resultCard.addView(tabScroll, marginTop(6));

        resultTabContent = column();
        resultCard.addView(resultTabContent, marginTop(4));
        renderUnifiedTab(result, aiLoading);
        addSharedResultActions(aiLoading, result.mode);
    }

    private void renderUnifiedTab(FortuneResult result, boolean aiLoading) {
        if (resultTabContent == null) return;
        resultTabContent.removeAllViews();
        if (selectedResultTab == 4) {
            addInterpretationTab(result, aiLoading);
            return;
        }

        if (result.mode == FortuneMode.BA_ZI) {
            switch (selectedResultTab) {
                case 1:
                    addBaZiResultPanel();
                    break;
                case 2:
                    addBaZiLuckTimelineTab();
                    break;
                case 3:
                    addBaZiTopicAnalysisTab();
                    break;
                case 0:
                default:
                    addBaZiOverviewTab(result, aiLoading);
                    break;
            }
            return;
        }

        if (result.mode == FortuneMode.VEDIC_ASTROLOGY) {
            switch (selectedResultTab) {
                case 1:
                    addVedicNatalTab();
                    break;
                case 2:
                    addVedicDashaTimelineTab();
                    break;
                case 3:
                    addVedicTopicAnalysisTab();
                    break;
                case 0:
                default:
                    addVedicOverviewTab(result, aiLoading);
                    break;
            }
            return;
        }

        switch (selectedResultTab) {
            case 1:
                addTarotResultPanel();
                break;
            case 2:
                addTarotTimelineTab();
                break;
            case 3:
                addTarotTopicAnalysisTab();
                break;
            case 0:
            default:
                addTarotOverviewTab(result, aiLoading);
                break;
        }
    }

    private void addInterpretationTab(FortuneResult result, boolean aiLoading) {
        LinearLayout panel = resultPanel();

        if (aiLoading) {
            addAiLoadingPlaceholder(panel, result.mode);
            return;
        }

        if (aiCopy != null) {
            addPanelSection(panel, "總覽", aiCopy.overview);
            if (!aiCopy.personality.isEmpty()) {
                addPanelSection(panel, "性格、優勢與盲點", aiCopy.personality);
            }
            if (!aiCopy.career.isEmpty()) {
                addPanelSection(panel, "工作", aiCopy.career);
            }
            if (!aiCopy.wealth.isEmpty()) {
                addPanelSection(panel, "財運與資源", aiCopy.wealth);
            }
            if (!aiCopy.relationships.isEmpty()) {
                addPanelSection(panel, "感情與人際", aiCopy.relationships);
            }
            if (!aiCopy.currentCycle.isEmpty()) {
                addPanelSection(panel, "目前運勢", aiCopy.currentCycle);
            }
            if (!aiCopy.longTerm.isEmpty()) {
                addPanelSection(panel,
                        result.mode == FortuneMode.BA_ZI
                                ? "未來十年"
                                : result.mode == FortuneMode.VEDIC_ASTROLOGY
                                ? "Dasha 長期節奏"
                                : "未來幾年",
                        aiCopy.longTerm);
            }
            if (!aiCopy.keyYears.isEmpty()) {
                addPanelSection(panel,
                        result.mode == FortuneMode.VEDIC_ASTROLOGY
                                ? "重要時期" : "重要年份",
                        aiCopy.keyYears);
            }
            if (!aiCopy.translation.isEmpty()) {
                addPanelSection(panel, "翻譯成人話", aiCopy.translation);
            }
            if (!aiCopy.punchline.isEmpty()) {
                addPanelSection(panel, "命理師補充", aiCopy.punchline);
            }
            if (!aiCopy.advice.isEmpty()) {
                addPanelSection(panel, "建議", aiCopy.advice);
            }
        } else {
            for (Map.Entry<String, String> entry
                    : FortuneLocalReport.sections(currentFacts).entrySet()) {
                addPanelSection(panel, entry.getKey(), entry.getValue());
            }
            addPanelSection(panel, "翻譯成人話", result.translation);
            addPanelSection(panel, "命理師補充", result.punchline);
            addPanelSection(panel, "建議", result.advice);
        }

        TextView hint = text(
                result.mode == FortuneMode.BA_ZI
                        ? "下面的老師可以繼續追問：未來十年、財運、工作、感情、指定年份。"
                        : result.mode == FortuneMode.VEDIC_ASTROLOGY
                        ? "下面的老師可以繼續追問：目前 Mahadasha／Antardasha、工作、財運、感情或指定 Dasha 時期。"
                        : "下面的老師可以繼續追問：未來幾年、工作、感情、指定流年或今年某個月份。",
                12, GOLD, true);
        hint.setLineSpacing(dp(2), 1f);
        panel.addView(hint, marginTop(11));
    }

    private void addVedicOverviewTab(FortuneResult result, boolean aiLoading) {
        LinearLayout panel = resultPanel();

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout lagna = column();
        lagna.addView(text("Lagna", 11, GOLD, true));
        lagna.addView(text(currentFacts.detailText("lagnaSign"), 22, TEXT, true), marginTop(3));
        lagna.addView(text(
                currentFacts.detailText("lagnaNakshatra")
                        + " · Pada " + currentFacts.detailText("lagnaPada"),
                11, MUTED, false), marginTop(2));
        hero.addView(lagna, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout moon = column();
        TextView moonLabel = text("Moon", 11, GOLD, true);
        moonLabel.setGravity(Gravity.END);
        moon.addView(moonLabel);
        TextView moonSign = text(currentFacts.detailText("moonSign"), 22, ACCENT, true);
        moonSign.setGravity(Gravity.END);
        moon.addView(moonSign, marginTop(3));
        TextView moonNakshatra = text(
                currentFacts.detailText("moonNakshatra")
                        + " · Pada " + currentFacts.detailText("moonPada"),
                11, MUTED, false);
        moonNakshatra.setGravity(Gravity.END);
        moon.addView(moonNakshatra, marginTop(2));
        hero.addView(moon, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        panel.addView(hero);

        addPanelSection(panel, "本命核心", VedicFactsFormatter.coreSummary(currentFacts));
        addPanelSection(panel, "目前 Dasha", VedicFactsFormatter.dashaSummary(currentFacts));

        if (!aiLoading && aiCopy != null && !aiCopy.topTraits.isEmpty()) {
            addTopTraits(panel);
        }

        String summary;
        if (aiLoading) {
            summary = localReportSection("核心總覽");
        } else if (aiCopy != null && !aiCopy.overview.isEmpty()) {
            summary = aiCopy.overview;
        } else {
            summary = localReportSection("核心總覽");
        }
        if (!summary.isEmpty()) addPanelSection(panel, "AI 重點", summary);

        TextView rule = text(
                "計算規則｜Sidereal · Lahiri · Whole Sign · Mean Rahu/Ketu",
                11, MUTED, false);
        panel.addView(rule, marginTop(9));
    }

    private void addVedicNatalTab() {
        LinearLayout panel = resultPanel();

        TextView intro = text(
                "本命頁只展示 deterministic chart facts。點任一顆星或宮位可查看完整欄位；AI 不會在這一頁重新排盤。",
                13, MUTED, false);
        intro.setLineSpacing(dp(3), 1f);
        panel.addView(intro);

        addPanelSection(panel, "Lagna / Moon / Sun",
                VedicFactsFormatter.coreSummary(currentFacts));

        TextView planetsTitle = text("九曜", 13, GOLD, true);
        panel.addView(planetsTitle, marginTop(14));
        Object planetsRaw = currentFacts.detail("planets");
        if (planetsRaw instanceof List) {
            for (Object raw : (List<?>) planetsRaw) {
                if (!(raw instanceof Map)) continue;
                final Map<?, ?> planet = (Map<?, ?>) raw;
                String title = mapValue(planet, "name")
                        + " · " + mapValue(planet, "sign")
                        + " · H" + mapValue(planet, "house");
                String summary = String.format(
                        java.util.Locale.US,
                        "%.2f° sidereal",
                        numberValue(planet.get("siderealLongitude")))
                        + " · " + mapValue(planet, "nakshatra")
                        + " P" + mapValue(planet, "pada")
                        + (Boolean.TRUE.equals(planet.get("retrograde")) ? " · R" : "")
                        + " · " + mapValue(planet, "dignity");
                addClickableFactCard(
                        panel,
                        title,
                        summary,
                        () -> showFactDetailDialog(
                                mapValue(planet, "name") + " deterministic facts", planet),
                        "請只用 deterministic facts 解釋 "
                                + mapValue(planet, "name")
                                + " 的 sign、house、Nakshatra、dignity 與它所主宮位對我代表什麼。");
            }
        }

        TextView housesTitle = text("12 Houses · Whole Sign", 13, GOLD, true);
        panel.addView(housesTitle, marginTop(18));
        Object housesRaw = currentFacts.detail("houses");
        if (housesRaw instanceof List) {
            for (Object raw : (List<?>) housesRaw) {
                if (!(raw instanceof Map)) continue;
                final Map<?, ?> house = (Map<?, ?>) raw;
                String title = "H" + mapValue(house, "house")
                        + " · " + mapValue(house, "sign")
                        + " · lord " + mapValue(house, "lord");
                String summary = "宮內：" + compactValue(house.get("planets"));
                addClickableFactCard(
                        panel,
                        title,
                        summary,
                        () -> showFactDetailDialog(
                                "House " + mapValue(house, "house"), house),
                        "請只用 deterministic facts 解釋第 "
                                + mapValue(house, "house")
                                + " 宮、宮主 " + mapValue(house, "lord")
                                + " 與宮內行星的結構。");
            }
        }

        String aspects = VedicFactsFormatter.aspects(currentFacts);
        addPanelSection(panel, "Drishti / Conjunctions",
                aspects.isEmpty() ? "目前沒有符合 v1 規則的合相；Drishti 仍保存在 deterministic facts。" : aspects);

        TextView boundary = text(
                "v1 不計 D9、Yoga、Shadbala、Ashtakavarga；Rahu/Ketu 不套用有爭議的特殊 Drishti 或 dignity。",
                11, MUTED, false);
        boundary.setLineSpacing(dp(2), 1f);
        panel.addView(boundary, marginTop(9));
    }

    private void addVedicDashaTimelineTab() {
        LinearLayout panel = resultPanel();

        TextView intro = text(
                "第一版的「流年」以 Vimshottari Dasha 為主，不混入尚未實作的 Gochar/transit。"
                        + "Mahadasha 是長週期背景，Antardasha 是其中較細的時間段；不是吉凶百分比。",
                13, MUTED, false);
        intro.setLineSpacing(dp(3), 1f);
        panel.addView(intro);

        addPanelSection(panel, "目前 Mahadasha / Antardasha",
                VedicFactsFormatter.dashaSummary(currentFacts));
        addYearHighlights(panel);

        TextView mdTitle = text("Mahadasha timeline", 13, GOLD, true);
        panel.addView(mdTitle, marginTop(18));
        Object raw = currentFacts.detail("mahadashaTimeline");
        String currentLord = mapValue(currentFacts.detail("currentMahadasha"), "lord");
        if (raw instanceof List) {
            for (Object itemRaw : (List<?>) raw) {
                if (!(itemRaw instanceof Map)) continue;
                final Map<?, ?> item = (Map<?, ?>) itemRaw;
                String lord = mapValue(item, "lord");
                String title = (lord.equals(currentLord) ? "● " : "○ ")
                        + lord + " Mahadasha";
                String summary = mapValue(item, "startDate")
                        + " → " + mapValue(item, "endDate")
                        + " · age " + mapValue(item, "startAge")
                        + "–" + mapValue(item, "endAge");
                addClickableFactCard(
                        panel,
                        title,
                        summary,
                        () -> showFactDetailDialog(lord + " Mahadasha", item),
                        "請直接解釋 " + lord
                                + " Mahadasha 對我代表什麼，只引用本命 planets、houses、houseLords 與這段 Dasha facts。");
            }
        }

        TextView adTitle = text("目前 Mahadasha 的 Antardasha", 13, GOLD, true);
        panel.addView(adTitle, marginTop(18));
        String adTimeline = VedicFactsFormatter.antardashaTimeline(currentFacts);
        addPanelSection(panel, "時間軸",
                adTimeline.isEmpty() ? "目前 Mahadasha 不在已產生的時間範圍內。" : adTimeline);

        TextView convention = text(
                currentFacts.detailText("vimshottariConvention"),
                11, MUTED, false);
        convention.setLineSpacing(dp(2), 1f);
        panel.addView(convention, marginTop(9));
    }

    private void addVedicTopicAnalysisTab() {
        LinearLayout panel = resultPanel();

        TextView intro = text(
                "不做吉凶分數。每個主題都拆成「結論範圍 → deterministic evidence → 目前時間 → AI 解讀 → 問老師」。",
                13, MUTED, false);
        intro.setLineSpacing(dp(3), 1f);
        panel.addView(intro);

        addVedicTopicCard(
                panel,
                "個性 / 天賦",
                "Lagna × Moon × Sun × Lagna lord",
                "personalityProfile",
                aiCopy == null ? "" : aiCopy.personality,
                "請直接講我的個性、優勢與盲點。只用 personalityProfile、Lagna、Moon、Sun、house lords 與本命 deterministic facts。");

        addVedicTopicCard(
                panel,
                "工作 / Career",
                "10宮 × 10宮主 × Saturn/Jupiter × Dasha",
                "careerProfile",
                aiCopy == null ? "" : aiCopy.career,
                "請直接講我的工作方向與目前職涯節奏。只用 careerProfile、10宮、10宮主、Saturn/Jupiter 與目前 Dasha。");

        addVedicTopicCard(
                panel,
                "財務 / Wealth",
                "2宮 × 11宮 × Jupiter/Venus × Dasha",
                "wealthProfile",
                aiCopy == null ? "" : aiCopy.wealth,
                "請直接講我的財務與資源節奏。只用 wealthProfile、2宮、11宮及宮主、Jupiter/Venus 與目前 Dasha，不做投資預測。");

        addVedicTopicCard(
                panel,
                "感情 / Relationships",
                "7宮 × 7宮主 × Venus × Dasha",
                "relationshipProfile",
                aiCopy == null ? "" : aiCopy.relationships,
                "請直接講我的感情與關係模式。只用 relationshipProfile、7宮、7宮主、Venus 與目前 Dasha，不把訊號說成必然事件。");

        addVedicTopicCard(
                panel,
                "家庭 / Children",
                "4宮 × 5宮 × Moon/Jupiter",
                "familyChildrenProfile",
                "",
                "請只用 familyChildrenProfile、4宮、5宮及宮主、Moon/Jupiter 說明家庭與子女主題。不要預測懷孕必然結果。");

        TextView boundary = text(
                "娛樂與自我反思用途。工作、財務、感情與家庭可以談結構與節奏；不做死亡、嚴重疾病、懷孕必然、犯罪或災難斷言。",
                11, MUTED, false);
        boundary.setLineSpacing(dp(2), 1f);
        panel.addView(boundary, marginTop(9));
    }

    private void addVedicTopicCard(
            LinearLayout panel,
            String title,
            String subtitle,
            String profileKey,
            String aiText,
            String question) {
        LinearLayout card = topicCard(panel, title, subtitle);
        Object raw = currentFacts.detail(profileKey);
        String rule = mapValue(raw, "rule");
        Object evidence = raw instanceof Map ? ((Map<?, ?>) raw).get("evidence") : null;
        addTopicLine(card, "結論範圍", rule);
        addTopicLine(card, "deterministic evidence", compactValue(evidence));
        addTopicLine(card, "時間", VedicFactsFormatter.dashaSummary(currentFacts));
        addTopicLine(card, "AI 解讀",
                aiText == null || aiText.trim().isEmpty()
                        ? "AI 完成後會只根據上面的 deterministic evidence 解讀；目前先保留可驗證依據。"
                        : aiText);
        addAskTeacherAction(card, "問老師", question);
    }

    private double numberValue(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(String.valueOf(value)); }
        catch (Exception ignored) { return 0.0; }
    }

    private void addTarotOverviewTab(FortuneResult result, boolean aiLoading) {
        LinearLayout panel = resultPanel();

        LinearLayout identities = new LinearLayout(this);
        identities.setOrientation(LinearLayout.HORIZONTAL);
        identities.setGravity(Gravity.CENTER);
        addNumerologyIdentityCard(
                identities,
                "外在人格牌",
                currentFacts.detailText("personalityCardNumber"),
                currentFacts.detailText("personalityCardName"));
        addNumerologyIdentityCard(
                identities,
                "內在靈魂牌",
                currentFacts.detailText("soulCardNumber"),
                currentFacts.detailText("soulCardName"));
        panel.addView(identities);

        addPanelSection(panel, "核心數字",
                "生命道路 " + currentFacts.detailText("lifePathDisplay")
                        + "　·　天賦 " + currentFacts.detailText("talentNumbers")
                        + "\n生日數 " + currentFacts.detailText("birthdayNumber")
                        + "　·　態度數 " + currentFacts.detailText("attitudeNumber"));

        addPanelSection(panel, "目前流年",
                currentFacts.detailText("personalYearCalendarYear")
                        + " 年｜個人流年 " + currentFacts.detailText("personalYear")
                        + "「" + currentFacts.detailText("personalYearCardName") + "」"
                        + "\n個人月 " + currentFacts.detailText("personalMonth")
                        + "｜" + currentTarotYearSummary());

        if (!aiLoading && aiCopy != null && !aiCopy.topTraits.isEmpty()) {
            addTopTraits(panel);
        }

        String summary;
        if (aiLoading) {
            summary = localReportSection("核心總覽");
        } else if (aiCopy != null && !aiCopy.overview.isEmpty()) {
            summary = aiCopy.overview;
        } else {
            summary = localReportSection("核心總覽");
        }
        if (!summary.isEmpty()) addPanelSection(panel, "重點解讀", summary);
    }

    private void addTarotTimelineTab() {
        LinearLayout panel = resultPanel();

        TextView intro = text(
                "個人流年看每一年的主題循環；個人月則把今年拆成 12 個月。數字每 9 年循環一次，所以重點是當年的課題與節奏，不是吉凶分數。",
                13, MUTED, false);
        intro.setLineSpacing(dp(3), 1f);
        panel.addView(intro);

        addPanelSection(panel, "今年",
                currentFacts.detailText("personalYearCalendarYear")
                        + "｜流年 " + currentFacts.detailText("personalYear")
                        + "「" + currentFacts.detailText("personalYearCardName") + "」"
                        + "\n" + currentTarotYearSummary());
        addYearHighlights(panel);

        TextView yearsTitle = text(
                "年度時間軸 · " + currentFacts.detailText("personalYearTimelineStartYear")
                        + "–" + currentFacts.detailText("personalYearTimelineEndYear"),
                13, GOLD, true);
        panel.addView(yearsTitle, marginTop(20));

        Object yearsRaw = currentFacts.detail("personalYearTimeline");
        if (yearsRaw instanceof List) {
            for (Object raw : (List<?>) yearsRaw) {
                if (!(raw instanceof Map)) continue;
                final Map<?, ?> year = (Map<?, ?>) raw;
                String title = mapValue(year, "year")
                        + "　流年 " + mapValue(year, "personalYear")
                        + "「" + mapValue(year, "cardName") + "」";
                String summary = mapValue(year, "plainSummary")
                        + "\n關鍵字：" + mapValue(year, "keywords");
                addClickableFactCard(
                        panel,
                        title,
                        summary,
                        () -> showFactDetailDialog(
                                mapValue(year, "year") + " 個人流年", year),
                        "請直接回答 " + mapValue(year, "year")
                                + " 年的個人流年對我代表什麼？請用 deterministic facts 說明。");
            }
        }

        TextView monthTitle = text("今年 12 個個人月", 13, GOLD, true);
        panel.addView(monthTitle, marginTop(22));

        Object monthsRaw = currentFacts.detail("personalMonthTimeline");
        if (monthsRaw instanceof List) {
            for (Object raw : (List<?>) monthsRaw) {
                if (!(raw instanceof Map)) continue;
                final Map<?, ?> month = (Map<?, ?>) raw;
                String title = mapValue(month, "month") + " 月　"
                        + mapValue(month, "personalMonth")
                        + "「" + mapValue(month, "cardName") + "」";
                String summary = mapValue(month, "plainSummary")
                        + "　·　" + mapValue(month, "keywords");
                addClickableFactCard(
                        panel,
                        title,
                        summary,
                        () -> showFactDetailDialog(
                                mapValue(month, "month") + " 月個人月", month),
                        "請直接回答今年 " + mapValue(month, "month")
                                + " 月的個人月主題對我代表什麼？請用 deterministic facts 說明。");
            }
        }

        addPanelSection(panel, "人生階段",
                "四大巔峰：" + compactValue(currentFacts.detail("pinnacles"))
                        + "\n時程：" + compactValue(currentFacts.detail("pinnacleTiming"))
                        + "\n四大挑戰：" + compactValue(currentFacts.detail("challenges"))
                        + "\n三大週期：" + compactValue(currentFacts.detail("periodCycles")));
    }

    private void addTarotTopicAnalysisTab() {
        LinearLayout panel = resultPanel();

        TextView intro = text(
                "塔羅生命靈數的主題分析會把本命數字、年度循環與 AI 解讀放在一起。先看依據，再看解讀。",
                13, MUTED, false);
        intro.setLineSpacing(dp(3), 1f);
        panel.addView(intro);

        LinearLayout personality = topicCard(panel, "個性與內外", "人格牌 × 靈魂牌 × 生命道路");
        addTopicLine(personality, "本命",
                "外在 " + currentFacts.detailText("personalityCardNumber")
                        + "「" + currentFacts.detailText("personalityCardName") + "」"
                        + "　·　內在 " + currentFacts.detailText("soulCardNumber")
                        + "「" + currentFacts.detailText("soulCardName") + "」"
                        + "\n生命道路 " + currentFacts.detailText("lifePathDisplay")
                        + "　·　天賦 " + currentFacts.detailText("talentNumbers"));
        addTopicLine(personality, "解讀",
                aiCopy != null && !aiCopy.personality.isEmpty()
                        ? aiCopy.personality
                        : localReportSection("內在 vs 外在"));
        addAskTeacherAction(
                personality,
                "問老師個性",
                "請直接回答我的外在人格牌、內在靈魂牌與生命道路之間最明顯的性格落差與優勢。");

        LinearLayout career = topicCard(panel, "工作與資源", "生命道路 × 態度數 × 巔峰 × 當前流年");
        addTopicLine(career, "依據",
                "生命道路 " + currentFacts.detailText("lifePathDisplay")
                        + "　·　態度數 " + currentFacts.detailText("attitudeNumber")
                        + "\n四大巔峰 " + compactValue(currentFacts.detail("pinnacles"))
                        + "\n目前流年 " + currentFacts.detailText("personalYear")
                        + "「" + currentFacts.detailText("personalYearCardName") + "」");
        addTopicLine(career, "時間",
                currentTarotYearSummary());
        addTopicLine(career, "解讀",
                aiCopy != null && !aiCopy.career.isEmpty()
                        ? aiCopy.career
                        : localReportSection("工作與財務"));
        addAskTeacherAction(
                career,
                "問老師工作",
                "請直接回答我的工作優勢與現在的職涯節奏。請用生命道路、態度數、巔峰與個人流年說明。");

        LinearLayout wealth = topicCard(panel, "財運與資源", "生命道路 × 巔峰 × 個人流年");
        addTopicLine(wealth, "依據",
                "生命道路 " + currentFacts.detailText("lifePathDisplay")
                        + "　·　四大巔峰 " + compactValue(currentFacts.detail("pinnacles"))
                        + "\n目前流年 " + currentFacts.detailText("personalYear")
                        + "「" + currentFacts.detailText("personalYearCardName") + "」");
        addTopicLine(wealth, "時間", currentTarotYearSummary());
        addTopicLine(wealth, "解讀",
                aiCopy != null && !aiCopy.wealth.isEmpty()
                        ? aiCopy.wealth
                        : "塔羅生命靈數的財務解讀以資源使用、成果節奏與年度主題為主，不把牌義當成投資預測。");
        addAskTeacherAction(
                wealth,
                "問老師資源",
                "請直接回答我目前的資源與成果節奏。請用生命道路、巔峰與個人流年說明，不做投資預測。");

        LinearLayout relationship = topicCard(panel, "感情與人際", "內外牌 × 挑戰數 × 年度節奏");
        addTopicLine(relationship, "依據",
                "外在人格牌 " + currentFacts.detailText("personalityCardNumber")
                        + "　·　內在靈魂牌 " + currentFacts.detailText("soulCardNumber")
                        + "\n四大挑戰 " + compactValue(currentFacts.detail("challenges")));
        addTopicLine(relationship, "時間",
                "目前流年 " + currentFacts.detailText("personalYear")
                        + "「" + currentFacts.detailText("personalYearCardName") + "」"
                        + "　·　個人月 " + currentFacts.detailText("personalMonth"));
        addTopicLine(relationship, "解讀",
                aiCopy != null && !aiCopy.relationships.isEmpty()
                        ? aiCopy.relationships
                        : localReportSection("感情與人際"));
        addAskTeacherAction(
                relationship,
                "問老師感情",
                "請直接回答我的感情與人際模式。請用外在人格牌、內在靈魂牌、挑戰數與目前流年說明。");

        TextView boundary = text(
                "塔羅生命靈數用於娛樂與自我反思；流年表示主題循環，不代表特定事件一定發生。",
                11, MUTED, false);
        boundary.setLineSpacing(dp(2), 1f);
        panel.addView(boundary, marginTop(9));
    }

    private String currentTarotYearSummary() {
        Object years = currentFacts == null ? null : currentFacts.detail("personalYearTimeline");
        String current = currentFacts == null ? "" : currentFacts.detailText("personalYearCalendarYear");
        if (years instanceof List) {
            for (Object raw : (List<?>) years) {
                if (!(raw instanceof Map)) continue;
                if (current.equals(mapValue(raw, "year"))) {
                    return mapValue(raw, "plainSummary");
                }
            }
        }
        return "";
    }

    private void addSharedResultActions(boolean aiLoading, FortuneMode mode) {
        if (aiLoading) {
            addAiLoadingBanner(mode);
        }

        TextView source = text(
                aiLoading
                        ? "本地計算已完成 · AI 完整解讀整理中"
                        : aiCopy == null
                        ? "本地完整資料 · AI 可選"
                        : "AI 深度解讀 · " + selectedAiStyle.label() + " · 計算資料固定",
                12,
                aiLoading ? ACCENT : MUTED,
                aiLoading);
        resultCard.addView(source, marginTop(aiLoading ? 5 : 6));

        if (!aiLoading && aiCopy != null) {
            List<String> contextualQuestions = buildContextFollowUps();
            if (!contextualQuestions.isEmpty()) {
                addFollowUpQuestions(contextualQuestions);
            }
        }

        Button teacher = new Button(this);
        teacher.setText(aiLoading ? "老師跟我講解 · 整理中" : "老師跟我講解");
        teacher.setTextSize(15);
        teacher.setAllCaps(false);
        teacher.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        teacher.setEnabled(!aiLoading);
        teacher.setAlpha(aiLoading ? 0.48f : 1f);
        teacher.setTextColor(aiLoading ? MUTED : Color.rgb(30, 22, 46));
        teacher.setBackground(round(aiLoading ? CARD_2 : GOLD, 18));
        if (!aiLoading) {
            teacher.setOnClickListener(v -> startTeacherExplanation());
        }
        resultCard.addView(teacher, fixedHeightTop(48, 8));

        Button share = secondaryButton(aiLoading ? "分享圖片 · 整理中" : "分享圖片");
        share.setEnabled(!aiLoading);
        share.setAlpha(aiLoading ? 0.48f : 1f);
        if (!aiLoading) {
            share.setOnClickListener(v -> shareResult());
        }
        resultCard.addView(share, fixedHeightTop(48, 6));
    }

    private void addTopTraits(LinearLayout panel) {
        TextView title = text("最像你的 3 件事", 13, GOLD, true);
        panel.addView(title, marginTop(10));

        int index = 0;
        for (String trait : aiCopy.topTraits) {
            if (index >= 3) break;
            final int traitIndex = index;
            final String traitValue = trait;
            final String evidence = traitIndex < aiCopy.topTraitEvidence.size()
                    ? aiCopy.topTraitEvidence.get(traitIndex)
                    : "這一點來自完整解讀與 deterministic facts；此結果沒有獨立證據摘要。";

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(9), dp(8), dp(9), dp(8));
            row.setBackground(roundBorder(
                    Color.rgb(43, 33, 65),
                    Color.rgb(80, 65, 111),
                    13,
                    1));
            row.setClickable(true);
            row.setOnClickListener(v ->
                    showTraitEvidenceDialog(traitValue, evidence));

            LinearLayout headline = new LinearLayout(this);
            headline.setOrientation(LinearLayout.HORIZONTAL);
            headline.setGravity(Gravity.TOP);

            TextView number = text(
                    index == 0 ? "①" : index == 1 ? "②" : "③",
                    18,
                    ACCENT,
                    true);
            LinearLayout.LayoutParams numberLp = new LinearLayout.LayoutParams(
                    dp(30), LinearLayout.LayoutParams.WRAP_CONTENT);
            numberLp.rightMargin = dp(5);
            headline.addView(number, numberLp);

            TextView body = text(trait, 13, TEXT, true);
            body.setLineSpacing(dp(2), 1f);
            headline.addView(body, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(headline);

            TextView why = text("為什麼這樣說？查看依據 ›", 11, ACCENT, true);
            row.addView(why, marginTop(4));

            panel.addView(row, marginTop(5));
            index++;
        }
    }

    private void addFollowUpQuestions(List<String> questions) {
        TextView title = text(
                selectedResultTab == 0 || selectedResultTab == 4
                        ? "你一定會想問"
                        : "這一頁可以直接問",
                13, GOLD, true);
        resultCard.addView(title, marginTop(9));

        int index = 0;
        LinearLayout row = null;
        for (final String question : questions) {
            if (index >= 4) break;
            if (index % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                resultCard.addView(row, marginTop(5));
            }

            Button button = secondaryButton(question);
            button.setTextSize(12);
            button.setGravity(Gravity.CENTER);
            button.setPadding(dp(7), dp(3), dp(7), dp(3));
            button.setOnClickListener(v -> {
                OperationLog.add(
                        MainActivity.this,
                        "TEACHER_FOLLOWUP_SELECTED",
                        "chars=" + question.length());
                startTeacherExplanation(question);
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, dp(58), 1f);
            if (index % 2 == 0) lp.rightMargin = dp(5);
            row.addView(button, lp);
            index++;
        }
    }

    private List<String> buildContextFollowUps() {
        List<String> out = new ArrayList<String>();
        if (aiCopy == null) return out;

        if (selectedResultTab == 0 || selectedResultTab == 4) {
            out.addAll(aiCopy.followUps);
            return out;
        }

        if (selectedResultTab == 1) {
            if (selectedMode == FortuneMode.BA_ZI) {
                out.add("我的日主和旺衰放到生活裡，最像什麼？");
                out.add("我的十神組合最明顯的優勢是什麼？");
                out.add("我的本命最容易卡在哪裡？");
                out.add("五行結構對我的做事方式有什麼影響？");
            } else {
                out.add("外在人格牌和內在靈魂牌最大的落差是什麼？");
                out.add("生命道路 " + currentFacts.detailText("lifePathDisplay") + " 最像我的地方是什麼？");
                out.add("我的天賦數最適合怎麼用？");
                out.add("四大挑戰裡，哪一個最值得我注意？");
            }
            return out;
        }

        if (selectedResultTab == 2) {
            for (FortuneYearHighlightBuilder.Highlight highlight
                    : FortuneYearHighlightBuilder.build(selectedMode, currentFacts)) {
                out.add(highlight.question);
                if (out.size() >= 2) break;
            }
            if (selectedMode == FortuneMode.BA_ZI) {
                out.add("未來幾年哪一年工作變動訊號最明顯？");
                out.add("未來幾年哪一年財運與資源訊號最值得看？");
            } else {
                out.add("未來幾年哪個流年轉折最大？");
                out.add("今年哪幾個月份最值得我注意？");
            }
            return trimQuestions(out, 4);
        }

        if (selectedResultTab == 3) {
            if (selectedMode == FortuneMode.BA_ZI) {
                out.add("直接講我的工作優勢與盲點。");
                out.add("直接講我的財運重點，不要只講好壞。");
                out.add("直接講我的感情與人際盲點。");
                out.add("這三個主題裡，未來三年哪個變化最大？");
            } else {
                out.add("直接講我的工作優勢與目前節奏。");
                out.add("直接講我的資源與成果節奏。");
                out.add("直接講我的感情與人際模式。");
                out.add("未來三年哪個主題最值得我注意？");
            }
            return out;
        }

        out.addAll(aiCopy.followUps);
        return trimQuestions(out, 4);
    }

    private List<String> trimQuestions(List<String> source, int max) {
        List<String> out = new ArrayList<String>();
        for (String item : source) {
            if (item == null || item.trim().isEmpty()) continue;
            if (out.contains(item.trim())) continue;
            out.add(item.trim());
            if (out.size() >= max) break;
        }
        return out;
    }

    private void addYearHighlights(LinearLayout panel) {
        List<FortuneYearHighlightBuilder.Highlight> highlights =
                FortuneYearHighlightBuilder.build(selectedMode, currentFacts);
        if (highlights.isEmpty()) return;

        TextView title = text("重要年份 · 先看這幾個", 13, GOLD, true);
        panel.addView(title, marginTop(11));

        TextView hint = text(
                selectedMode == FortuneMode.BA_ZI
                        ? "依十神、合沖與工作／財務／感情訊號挑出；不是吉凶排名。"
                        : "依個人流年的週期主題挑出；不是吉凶排名。",
                11, MUTED, false);
        panel.addView(hint, marginTop(2));

        for (FortuneYearHighlightBuilder.Highlight highlight : highlights) {
            LinearLayout card = column();
            card.setPadding(dp(9), dp(9), dp(9), dp(9));
            card.setBackground(roundBorder(
                    Color.rgb(55, 42, 82),
                    Color.rgb(111, 91, 157),
                    14,
                    1));

            LinearLayout head = new LinearLayout(this);
            head.setOrientation(LinearLayout.HORIZONTAL);
            head.setGravity(Gravity.CENTER_VERTICAL);
            TextView year = text(
                    highlight.year + "｜" + highlight.theme,
                    15, TEXT, true);
            head.addView(year, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView badge = text("值得先看", 10, GOLD, true);
            head.addView(badge);
            card.addView(head);

            TextView summary = text(highlight.summary, 12, MUTED, false);
            summary.setLineSpacing(dp(2), 1f);
            card.addView(summary, marginTop(3));

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            Button why = secondaryButton("查看依據");
            Button ask = secondaryButton("問老師");
            why.setTextSize(11);
            ask.setTextSize(11);
            why.setOnClickListener(v ->
                    showHighlightEvidenceDialog(highlight));
            ask.setOnClickListener(v ->
                    startTeacherExplanation(highlight.question));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, dp(38), 1f);
            lp.rightMargin = dp(5);
            actions.addView(why, lp);
            actions.addView(ask, new LinearLayout.LayoutParams(0, dp(38), 1f));
            card.addView(actions, marginTop(6));

            panel.addView(card, marginTop(6));
        }
    }

    private void addAskTeacherAction(
            LinearLayout card,
            String label,
            String question) {
        Button ask = secondaryButton(label);
        ask.setTextSize(12);
        ask.setOnClickListener(v -> startTeacherExplanation(question));
        card.addView(ask, fixedHeightTop(40, 7));
    }

    private void showTraitEvidenceDialog(String trait, String evidence) {
        LinearLayout panel = column();
        panel.setPadding(dp(14), dp(14), dp(14), dp(12));
        panel.setBackground(roundBorder(
                CARD, Color.rgb(92, 73, 127), 20, 1));

        panel.addView(text("為什麼這樣說？", 20, TEXT, true));
        TextView traitView = text(trait, 14, ACCENT, true);
        traitView.setLineSpacing(dp(2), 1f);
        panel.addView(traitView, marginTop(6));

        TextView evidenceView = text(
                "依據\n" + evidence,
                13, TEXT, false);
        evidenceView.setLineSpacing(dp(3), 1f);
        evidenceView.setPadding(dp(9), dp(9), dp(9), dp(9));
        evidenceView.setBackground(roundBorder(
                CARD_2, Color.rgb(80, 65, 111), 14, 1));
        panel.addView(evidenceView, marginTop(8));

        Button ask = secondaryButton("問老師這一點");
        Button close = secondaryButton("關閉");
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44), 1f);
        lp.rightMargin = dp(6);
        actions.addView(ask, lp);
        actions.addView(close, new LinearLayout.LayoutParams(0, dp(44), 1f));
        panel.addView(actions, marginTop(8));

        final AlertDialog dialog = new AlertDialog.Builder(this).setView(panel).create();
        ask.setOnClickListener(v -> {
            dialog.dismiss();
            startTeacherExplanation(
                    "請直接解釋這一點為什麼像我：「" + trait
                            + "」。請用 deterministic facts，尤其是：" + evidence);
        });
        close.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        styleDarkDialog(dialog);
    }

    private void showHighlightEvidenceDialog(
            FortuneYearHighlightBuilder.Highlight highlight) {
        LinearLayout panel = column();
        panel.setPadding(dp(14), dp(14), dp(14), dp(12));
        panel.setBackground(roundBorder(
                CARD, Color.rgb(92, 73, 127), 20, 1));

        panel.addView(text(
                highlight.year + "｜" + highlight.theme,
                20, TEXT, true));
        TextView summary = text(highlight.summary, 13, ACCENT, true);
        summary.setLineSpacing(dp(2), 1f);
        panel.addView(summary, marginTop(6));

        TextView evidence = text(
                "為什麼挑這年\n" + highlight.evidence,
                13, TEXT, false);
        evidence.setLineSpacing(dp(3), 1f);
        evidence.setPadding(dp(9), dp(9), dp(9), dp(9));
        evidence.setBackground(roundBorder(
                CARD_2, Color.rgb(80, 65, 111), 14, 1));
        panel.addView(evidence, marginTop(8));

        Button ask = secondaryButton("問老師這一年");
        Button close = secondaryButton("關閉");
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44), 1f);
        lp.rightMargin = dp(6);
        actions.addView(ask, lp);
        actions.addView(close, new LinearLayout.LayoutParams(0, dp(44), 1f));
        panel.addView(actions, marginTop(8));

        final AlertDialog dialog = new AlertDialog.Builder(this).setView(panel).create();
        ask.setOnClickListener(v -> {
            dialog.dismiss();
            startTeacherExplanation(highlight.question);
        });
        close.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        styleDarkDialog(dialog);
    }

    private void updateAiLoadingStage(int stage) {
        aiLoadingStage = Math.max(0, Math.min(2, stage));
        if (aiLoadingStageText != null && currentResult != null) {
            aiLoadingStageText.setText(aiLoadingStageLabel(currentResult.mode));
        }
    }

    private String aiLoadingStageLabel(FortuneMode mode) {
        if (aiLoadingStage == 2) {
            return "正在補足內容與格式，避免漏掉重要年份或依據";
        }
        if (aiLoadingStage == 1) {
            return mode == FortuneMode.BA_ZI
                    ? "正在撰寫個性、工作、財運、感情與未來十年"
                    : "正在撰寫個性、工作、資源、感情與未來幾年";
        }
        return mode == FortuneMode.BA_ZI
                ? "正在整理本命、大運、流年與主題訊號"
                : "正在整理本命牌、人生階段、個人流年與主題";
    }

    private void addAiLoadingBanner(FortuneMode mode) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(10), dp(10), dp(10), dp(10));
        card.setBackground(roundBorder(
                Color.rgb(45, 35, 67),
                Color.rgb(111, 91, 157),
                15,
                1));

        ProgressBar spinner = new ProgressBar(this);
        spinner.setIndeterminate(true);
        LinearLayout.LayoutParams spinnerLp = new LinearLayout.LayoutParams(
                dp(28), dp(28));
        spinnerLp.rightMargin = dp(10);
        card.addView(spinner, spinnerLp);

        LinearLayout copy = column();

        TextView title = text("AI 命理師正在整理完整解讀", 14, TEXT, true);
        copy.addView(title);

        aiLoadingStageText = text(aiLoadingStageLabel(mode), 12, MUTED, false);
        aiLoadingStageText.setLineSpacing(dp(1), 1f);
        copy.addView(aiLoadingStageText, marginTop(2));

        TextView wait = text("完成後會自動更新，不需要重新按一次。", 11, ACCENT, true);
        copy.addView(wait, marginTop(3));

        card.addView(copy, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        resultCard.addView(card, marginTop(8));
    }

    private void addAiLoadingPlaceholder(LinearLayout panel, FortuneMode mode) {
        TextView title = text("完整解讀正在生成", 16, TEXT, true);
        panel.addView(title);

        TextView subtitle = text(
                mode == FortuneMode.BA_ZI
                        ? "本命、流年等資料已經可以先看；AI 正在把工作、財運、感情與未來十年整理成完整報告。"
                        : "本命與流年資料已經可以先看；AI 正在把個性、工作、資源、感情與未來幾年整理成完整報告。",
                13, MUTED, false);
        subtitle.setLineSpacing(dp(2), 1f);
        panel.addView(subtitle, marginTop(5));

        String[] sections = {
                "總覽",
                "性格、優勢與盲點",
                "工作",
                "財運與資源",
                "感情與人際",
                mode == FortuneMode.BA_ZI ? "未來十年" : "未來幾年",
                "重要年份"
        };
        for (String section : sections) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(9), dp(8), dp(9), dp(8));
            row.setBackground(round(Color.rgb(43, 33, 65), 12));

            TextView label = text(section, 12, MUTED, true);
            row.addView(label, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView state = text("整理中…", 11, ACCENT, true);
            row.addView(state);

            panel.addView(row, marginTop(5));
        }
    }

    private void addBaZiOverviewTab(FortuneResult result, boolean aiLoading) {
        LinearLayout panel = resultPanel();

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout master = column();
        master.addView(text("日主", 11, GOLD, true));
        master.addView(text(
                currentFacts.detailText("dayMaster")
                        + currentFacts.detailText("dayMasterElement"),
                30, TEXT, true), marginTop(3));
        hero.addView(master, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout strength = column();
        TextView strengthLabel = text("旺衰", 11, MUTED, true);
        strengthLabel.setGravity(Gravity.END);
        strength.addView(strengthLabel);
        TextView strengthValue = text(
                currentFacts.detailText("dayMasterStrength")
                        + " · " + currentFacts.detailText("strengthIndex"),
                20, ACCENT, true);
        strengthValue.setGravity(Gravity.END);
        strength.addView(strengthValue, marginTop(3));
        hero.addView(strength, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        panel.addView(hero);

        addPanelSection(panel, "四柱", currentFacts.detailText("fourPillars"));
        addPanelSection(panel, "五行摘要",
                "可見：" + formatMap(currentFacts.detail("visibleFiveElements"))
                        + "\n加權：" + formatMap(currentFacts.detail("weightedFiveElements"))
                        + "\n平衡參考：" + currentFacts.detailText("balancingElements"));

        Object luck = currentFacts.detail("currentLuckPillar");
        addPanelSection(panel, "目前大運", summaryLuck(luck));
        addPanelSection(panel, "今年流年", formatCurrentAnnual());

        if (!aiLoading && aiCopy != null && !aiCopy.topTraits.isEmpty()) {
            addTopTraits(panel);
        }

        String aiSummary;
        if (aiLoading) {
            aiSummary = localReportSection("核心總覽");
        } else if (aiCopy != null && !aiCopy.overview.isEmpty()) {
            aiSummary = aiCopy.overview;
        } else {
            aiSummary = localReportSection("核心總覽");
        }
        if (!aiSummary.isEmpty()) addPanelSection(panel, "重點解讀", aiSummary);
    }

    private void addBaZiLuckTimelineTab() {
        LinearLayout panel = resultPanel();

        TextView intro = text(
                "大運看十年級別的背景，流年看每一年如何落在這個背景上。點任一項可看完整十神、五行、合沖與主題依據。",
                13, MUTED, false);
        intro.setLineSpacing(dp(3), 1f);
        panel.addView(intro);

        addPanelSection(panel, "怎麼看",
                "大運＝約十年的背景；流年＝某一年的放大鏡。\n"
                        + "先看「白話」知道主題，再看十神與合沖了解依據。"
                        + " 有財星不等於一定賺錢、有沖也不等於一定出事。");

        addPanelSection(panel, "目前大運", summaryLuck(currentFacts.detail("currentLuckPillar")));
        addYearHighlights(panel);

        TextView luckTitle = text("大運時間軸", 13, GOLD, true);
        panel.addView(luckTitle, marginTop(11));

        Object luckRaw = currentFacts.detail("luckPillars");
        String currentGanZhi = mapValue(currentFacts.detail("currentLuckPillar"), "ganZhi");
        if (luckRaw instanceof List) {
            for (Object raw : (List<?>) luckRaw) {
                if (!(raw instanceof Map)) continue;
                final Map<?, ?> item = (Map<?, ?>) raw;
                String gz = mapValue(item, "ganZhi");
                String title = (gz.equals(currentGanZhi) ? "● " : "○ ")
                        + gz + "　" + mapValue(item, "startYear")
                        + "–" + mapValue(item, "endYear");
                String summary = mapValue(item, "plainSummary")
                        + "\n十神 " + mapValue(item, "stemTenGod")
                        + "（" + mapValue(item, "stemTenGodMeaning") + "）"
                        + "　·　五行 " + mapValue(item, "element")
                        + "\n主題 " + compactValue(item.get("themes"));
                addClickableFactCard(
                        panel,
                        title,
                        summary,
                        () -> showFactDetailDialog("大運 " + gz, item),
                        "請直接解釋 " + gz
                                + " 大運對我代表什麼？請用這柱大運與本命 deterministic facts 說明。");
            }
        }

        TextView yearTitle = text(
                "逐年流年 · " + currentFacts.detailText("annualTimelineStartYear")
                        + "–" + currentFacts.detailText("annualTimelineEndYear"),
                13, GOLD, true);
        panel.addView(yearTitle, marginTop(22));

        Object yearsRaw = currentFacts.detail("annualTimeline");
        if (yearsRaw instanceof List) {
            for (Object raw : (List<?>) yearsRaw) {
                if (!(raw instanceof Map)) continue;
                final Map<?, ?> year = (Map<?, ?>) raw;
                String title = mapValue(year, "year") + "　"
                        + mapValue(year, "ganZhi")
                        + "　" + mapValue(year, "stemTenGod");
                String summary = mapValue(year, "plainSummary")
                        + "\n大運 " + mapValue(year, "luckPillar")
                        + "　·　十神 " + mapValue(year, "stemTenGod")
                        + "（" + mapValue(year, "stemTenGodMeaning") + "）"
                        + "\n" + firstMeaningfulInteraction(year.get("natalInteractions"));
                addClickableFactCard(
                        panel,
                        title,
                        summary,
                        () -> showFactDetailDialog(
                                mapValue(year, "year") + " 流年", year),
                        "請直接回答 " + mapValue(year, "year")
                                + " 年對我代表什麼？請用這一年的 deterministic facts 與所屬大運說明。");
            }
        }

        TextView convention = text(
                currentFacts.detailText("annualTimelineConvention"),
                11, MUTED, false);
        panel.addView(convention, marginTop(9));
    }

    private void addBaZiTopicAnalysisTab() {
        LinearLayout panel = resultPanel();

        TextView intro = text(
                "這裡不做神祕分數。每個主題都拆成「本命依據 → 現在大運 → 哪些年份訊號明顯 → AI/老師怎麼解讀」。",
                13, MUTED, false);
        intro.setLineSpacing(dp(3), 1f);
        panel.addView(intro);

        addWealthTopic(panel);
        addCareerTopic(panel);
        addRelationshipTopic(panel);
    }

    private void addWealthTopic(LinearLayout panel) {
        Object p = currentFacts.detail("wealthProfile");
        LinearLayout card = topicCard(panel, "財運", "看財星、財星位置、大運與逐年啟動");
        addTopicLine(card, "本命",
                "財星五行 " + mapValue(p, "wealthElement")
                        + "　·　正財 " + mapValue(p, "directWealthCount")
                        + "　·　偏財 " + mapValue(p, "indirectWealthCount"));
        addTopicLine(card, "依據", compactValue(mapObjectValue(p, "natalEvidence")));
        addTopicLine(card, "目前大運", summaryLuck(mapObjectValue(p, "currentLuck")));
        addTopicLine(card, "時間",
                "財星訊號年份：" + compactValue(mapObjectValue(p, "annualSignalYears")));
        addTopicLine(card, "老師解讀",
                aiCopy != null && !aiCopy.wealth.isEmpty()
                        ? aiCopy.wealth
                        : "AI 完成後會把財星、大運與流年證據翻成白話；沒有 AI 時仍可直接看上面的 deterministic evidence。");
        addTopicBoundary(card, mapValue(p, "evidenceRule"));
        addAskTeacherAction(
                card,
                "問老師我的財運",
                "請直接回答我的財運重點。請從 wealthProfile、目前大運與逐年流年挑最重要的依據，不要重新排盤。");
    }

    private void addCareerTopic(LinearLayout panel) {
        Object p = currentFacts.detail("careerProfile");
        LinearLayout card = topicCard(panel, "工作", "看官殺、印、食傷與大運流年");
        addTopicLine(card, "本命",
                "官殺 " + mapValue(p, "officerCount")
                        + "　·　印 " + mapValue(p, "resourceCount")
                        + "　·　食傷 " + mapValue(p, "outputCount"));
        addTopicLine(card, "依據", compactValue(mapObjectValue(p, "natalEvidence")));
        addTopicLine(card, "目前大運", summaryLuck(mapObjectValue(p, "currentLuck")));
        addTopicLine(card, "時間",
                "工作訊號年份：" + compactValue(mapObjectValue(p, "annualSignalYears")));
        addTopicLine(card, "老師解讀",
                aiCopy != null && !aiCopy.career.isEmpty()
                        ? aiCopy.career
                        : "官殺偏責任與規範、印偏資源與學習、食傷偏輸出與表達；要再和大運、流年一起看。");
        addTopicBoundary(card, mapValue(p, "evidenceRule"));
        addAskTeacherAction(
                card,
                "問老師工作",
                "請直接回答我的工作與職涯重點。請從 careerProfile、目前大運與逐年流年挑最重要的依據，不要重新排盤。");
    }

    private void addRelationshipTopic(LinearLayout panel) {
        Object p = currentFacts.detail("relationshipProfile");
        LinearLayout card = topicCard(panel, "感情", "看日支配偶宮、財官約定與合沖");
        addTopicLine(card, "本命",
                "配偶宮 " + mapValue(p, "spousePalace")
                        + "　·　藏干十神 " + compactValue(mapObjectValue(p, "spousePalaceTenGods")));
        addTopicLine(card, "依據",
                "常見財官約定 " + compactValue(mapObjectValue(p, "partnerGodConvention"))
                        + "　·　本命數量 " + mapValue(p, "partnerGodCount"));
        addTopicLine(card, "時間",
                "配偶宮／財官訊號年份：" + compactRelationshipYears(
                        mapObjectValue(p, "annualSignalYears")));
        addTopicLine(card, "老師解讀",
                aiCopy != null && !aiCopy.relationships.isEmpty()
                        ? aiCopy.relationships
                        : "這裡只標出配偶宮與財官訊號被碰到的年份，不直接等同戀愛、結婚或分手。");
        addTopicBoundary(card, mapValue(p, "evidenceRule"));
        addAskTeacherAction(
                card,
                "問老師感情",
                "請直接回答我的感情與人際盲點。請從 relationshipProfile、配偶宮與逐年流年挑最重要的依據，不要重新排盤。");
    }

    private LinearLayout resultPanel() {
        LinearLayout panel = column();
        panel.setPadding(dp(10), dp(11), dp(10), dp(11));
        panel.setBackground(roundBorder(
                CARD_2, Color.rgb(86, 70, 119), 16, 1));
        resultTabContent.addView(panel, marginTop(4));
        return panel;
    }

    private LinearLayout topicCard(LinearLayout parent, String title, String subtitle) {
        LinearLayout card = column();
        card.setPadding(dp(10), dp(10), dp(10), dp(10));
        card.setBackground(roundBorder(
                Color.rgb(31, 24, 49), Color.rgb(80, 65, 111), 16, 1));
        TextView h = text(title, 19, TEXT, true);
        card.addView(h);
        TextView s = text(subtitle, 12, MUTED, false);
        card.addView(s, marginTop(3));
        parent.addView(card, marginTop(6));
        return card;
    }

    private void addTopicLine(LinearLayout card, String label, String value) {
        TextView l = text(label, 11, GOLD, true);
        card.addView(l, marginTop(8));
        TextView v = text(value == null || value.isEmpty() ? "—" : value, 14, TEXT, false);
        v.setLineSpacing(dp(2), 1f);
        card.addView(v, marginTop(2));
    }

    private void addTopicBoundary(LinearLayout card, String value) {
        if (value == null || value.isEmpty()) return;
        TextView v = text(value, 11, MUTED, false);
        v.setLineSpacing(dp(2), 1f);
        card.addView(v, marginTop(8));
    }

    private void addClickableFactCard(
            LinearLayout parent,
            String title,
            String summary,
            final Runnable action) {
        addClickableFactCard(parent, title, summary, action, "");
    }

    private void addClickableFactCard(
            LinearLayout parent,
            String title,
            String summary,
            final Runnable action,
            String askQuestion) {
        LinearLayout card = column();
        card.setPadding(dp(9), dp(9), dp(9), dp(9));
        card.setBackground(roundBorder(
                Color.rgb(31, 24, 49), Color.rgb(80, 65, 111), 14, 1));
        TextView h = text(title, 15, TEXT, true);
        card.addView(h);
        TextView s = text(summary, 12, MUTED, false);
        s.setLineSpacing(dp(2), 1f);
        card.addView(s, marginTop(2));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);

        TextView more = text("查看完整依據 ›", 11, ACCENT, true);
        more.setOnClickListener(v -> action.run());
        actions.addView(more, new LinearLayout.LayoutParams(
                0, dp(36), 1f));

        if (askQuestion != null && !askQuestion.trim().isEmpty()) {
            Button ask = secondaryButton("問老師");
            ask.setTextSize(11);
            ask.setOnClickListener(v ->
                    startTeacherExplanation(askQuestion));
            LinearLayout.LayoutParams askLp = new LinearLayout.LayoutParams(
                    dp(82), dp(36));
            askLp.leftMargin = dp(6);
            actions.addView(ask, askLp);
        }

        card.addView(actions, marginTop(4));
        card.setClickable(true);
        card.setOnClickListener(v -> action.run());
        parent.addView(card, marginTop(8));
    }

    private void showFactDetailDialog(String titleValue, Object value) {
        LinearLayout panel = column();
        panel.setPadding(dp(14), dp(14), dp(14), dp(12));
        panel.setBackground(roundBorder(
                CARD, Color.rgb(92, 73, 127), 20, 1));

        panel.addView(text(titleValue, 21, TEXT, true));
        TextView detail = text(formatStructured(value), 13, TEXT, false);
        detail.setLineSpacing(dp(2), 1f);
        detail.setPadding(dp(9), dp(9), dp(9), dp(9));
        detail.setBackground(roundBorder(
                CARD_2, Color.rgb(80, 65, 111), 14, 1));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(detail);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(420));
        scrollLp.topMargin = dp(8);
        panel.addView(scroll, scrollLp);

        Button close = secondaryButton("關閉");
        panel.addView(close, fixedHeightTop(44, 8));

        final AlertDialog dialog = new AlertDialog.Builder(this).setView(panel).create();
        close.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        styleDarkDialog(dialog);
    }

    private String summaryLuck(Object value) {
        if (!(value instanceof Map)) return "目前沒有對應的大運資料";
        return mapValue(value, "ganZhi")
                + "　" + mapValue(value, "startYear") + "–" + mapValue(value, "endYear")
                + "\n白話｜" + mapValue(value, "plainSummary")
                + "\n十神 " + mapValue(value, "stemTenGod")
                + "（" + mapValue(value, "stemTenGodMeaning") + "）"
                + "　·　五行 " + mapValue(value, "element")
                + "\n主題 " + compactValue(mapObjectValue(value, "themes"))
                + "\n" + firstMeaningfulInteraction(mapObjectValue(value, "interactionsWithNatal"));
    }

    private Object mapObjectValue(Object source, String key) {
        if (!(source instanceof Map)) return null;
        return ((Map<?, ?>) source).get(key);
    }

    private String compactValue(Object value) {
        if (value == null) return "—";
        if (value instanceof List) {
            StringBuilder out = new StringBuilder();
            for (Object item : (List<?>) value) {
                if (out.length() > 0) out.append("、");
                out.append(String.valueOf(item));
            }
            return out.length() == 0 ? "—" : out.toString();
        }
        return String.valueOf(value);
    }

    private String compactRelationshipYears(Object value) {
        if (!(value instanceof List)) return compactValue(value);
        StringBuilder out = new StringBuilder();
        for (Object raw : (List<?>) value) {
            if (!(raw instanceof Map)) continue;
            if (out.length() > 0) out.append("、");
            out.append(mapValue(raw, "year"));
            String relation = mapValue(raw, "spousePalaceInteraction");
            if (!relation.isEmpty()) out.append("(").append(relation).append(")");
        }
        return out.length() == 0 ? "—" : out.toString();
    }

    private String firstMeaningfulInteraction(Object value) {
        if (!(value instanceof List)) return compactValue(value);
        for (Object item : (List<?>) value) {
            String text = String.valueOf(item);
            if (!text.contains("未偵測")) return text;
        }
        return "與本命主要地支互動較少";
    }

    private String formatStructured(Object value) {
        return formatStructured(value, 0);
    }

    private String formatStructured(Object value, int depth) {
        if (value == null) return "—";
        String indent = "";
        for (int i = 0; i < depth; i++) indent += "  ";

        if (value instanceof Map) {
            StringBuilder out = new StringBuilder();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (out.length() > 0) out.append("\n");
                Object child = entry.getValue();
                if (child instanceof Map || child instanceof List) {
                    out.append(indent).append(entry.getKey()).append("：\n")
                            .append(formatStructured(child, depth + 1));
                } else {
                    out.append(indent).append(entry.getKey()).append("：")
                            .append(String.valueOf(child));
                }
            }
            return out.toString();
        }

        if (value instanceof List) {
            StringBuilder out = new StringBuilder();
            for (Object child : (List<?>) value) {
                if (out.length() > 0) out.append("\n");
                if (child instanceof Map || child instanceof List) {
                    out.append(indent).append("•\n")
                            .append(formatStructured(child, depth + 1));
                } else {
                    out.append(indent).append("• ").append(String.valueOf(child));
                }
            }
            return out.toString();
        }
        return indent + String.valueOf(value);
    }

    private String displayFactKey(String key) {
        if ("ganZhi".equals(key)) return "干支";
        if ("startYear".equals(key)) return "起始年";
        if ("endYear".equals(key)) return "結束年";
        if ("startAge".equals(key)) return "起始年齡";
        if ("endAge".equals(key)) return "結束年齡";
        if ("xunKong".equals(key)) return "旬空";
        if ("stemTenGod".equals(key)) return "天干十神";
        if ("stemTenGodMeaning".equals(key)) return "十神白話";
        if ("branchTenGods".equals(key)) return "地支藏干十神";
        if ("hiddenStems".equals(key)) return "藏干";
        if ("element".equals(key)) return "五行";
        if ("interactionsWithNatal".equals(key)) return "與本命互動";
        if ("natalInteractions".equals(key)) return "與本命互動";
        if ("themes".equals(key)) return "主題";
        if ("plainSummary".equals(key)) return "白話重點";
        if ("year".equals(key)) return "年份";
        if ("nominalAge".equals(key)) return "虛歲";
        if ("luckPillar".equals(key)) return "所屬大運";
        if ("luckInteraction".equals(key)) return "大運互動";
        if ("personalYear".equals(key)) return "個人流年";
        if ("personalMonth".equals(key)) return "個人月";
        if ("month".equals(key)) return "月份";
        if ("cardName".equals(key)) return "對應塔羅牌";
        if ("keywords".equals(key)) return "關鍵字";
        if ("spousePalaceInteraction".equals(key)) return "配偶宮互動";
        if ("partnerGodActive".equals(key)) return "財官訊號";
        return key;
    }

    private String localReportSection(String key) {
        if (currentFacts == null) return "";
        Map<String, String> sections = FortuneLocalReport.sections(currentFacts);
        String value = sections.get(key);
        if (value != null) return value;
        for (Map.Entry<String, String> entry : sections.entrySet()) return entry.getValue();
        return "";
    }

    private LinearLayout.LayoutParams fixedHeightTop(int heightDp, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(heightDp));
        lp.topMargin = dp(topDp);
        return lp;
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
        panel.setPadding(dp(10), dp(11), dp(10), dp(11));
        panel.setBackground(round(CARD_2, 16));
        LinearLayout host = resultTabContent == null ? resultCard : resultTabContent;
        host.addView(panel, marginTop(6));

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
        panel.addView(divider, marginTop(11));

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
        panel.addView(elementsTitle, marginTop(11));

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
        panel.addView(tenGodsTitle, marginTop(11));
        TextView tenGods = text(formatTenGods(), 14, TEXT, false);
        tenGods.setLineSpacing(dp(3), 1f);
        panel.addView(tenGods, marginTop(6));

        TextView tenDistTitle = text("十神分布", 12, GOLD, true);
        panel.addView(tenDistTitle, marginTop(11));
        TextView tenDist = text(formatMap(currentFacts.detail("tenGodDistribution")), 13, TEXT, false);
        tenDist.setLineSpacing(dp(2), 1f);
        panel.addView(tenDist, marginTop(6));

        TextView strengthTitle = text("旺衰與平衡", 12, GOLD, true);
        panel.addView(strengthTitle, marginTop(11));
        TextView strength = text(
                currentFacts.detailText("dayMasterStrength")
                        + "｜扶身比 " + currentFacts.detailText("strengthIndex") + "/100"
                        + "\n平衡參考元素：" + currentFacts.detailText("balancingElements")
                        + "\n" + currentFacts.detailText("strengthMethod"),
                13, TEXT, false);
        strength.setLineSpacing(dp(3), 1f);
        panel.addView(strength, marginTop(6));

        TextView interactionTitle = text("命局合沖刑害", 12, GOLD, true);
        panel.addView(interactionTitle, marginTop(11));
        TextView interactions = text(formatList(currentFacts.detail("natalInteractions")), 13, TEXT, false);
        interactions.setLineSpacing(dp(3), 1f);
        panel.addView(interactions, marginTop(6));

        TextView luckTitle = text("大運", 12, GOLD, true);
        panel.addView(luckTitle, marginTop(11));
        TextView luck = text(formatLuckPillars(), 13, TEXT, false);
        luck.setLineSpacing(dp(3), 1f);
        panel.addView(luck, marginTop(6));

        TextView annualTitle = text("今年流年", 12, GOLD, true);
        panel.addView(annualTitle, marginTop(11));
        TextView annual = text(formatCurrentAnnual(), 13, TEXT, false);
        annual.setLineSpacing(dp(3), 1f);
        panel.addView(annual, marginTop(6));

        addPanelSection(panel, "加權五行", formatMap(currentFacts.detail("weightedFiveElements")));
        addPanelSection(panel, "納音", formatList(currentFacts.detail("naYin")));
        addPanelSection(panel, "十二長生", formatList(currentFacts.detail("lifeStages")));
        addPanelSection(panel, "命宮／身宮",
                "命宮 " + currentFacts.detailText("mingGong")
                        + "　·　身宮 " + currentFacts.detailText("shenGong"));

        TextView convention = text(
                "排盤規則｜" + currentFacts.detailText("timeConvention"),
                11, MUTED, false);
        convention.setLineSpacing(dp(2), 1f);
        panel.addView(convention, marginTop(6));
    }

    private void addPillarCard(LinearLayout row,
                               String label,
                               String pillar,
                               String hidden,
                               String tenGod) {
        LinearLayout card = column();
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(4), dp(7), dp(4), dp(7));
        GradientDrawable background = round(Color.rgb(31, 24, 49), 14);
        background.setStroke(dp(1), Color.rgb(78, 63, 108));
        card.setBackground(background);

        TextView labelView = text(label, 11, MUTED, true);
        labelView.setGravity(Gravity.CENTER);
        card.addView(labelView);

        TextView pillarView = text(pillar, 22, TEXT, true);
        pillarView.setGravity(Gravity.CENTER);
        card.addView(pillarView, marginTop(2));

        TextView tenGodView = text(tenGod, 11, GOLD, true);
        tenGodView.setGravity(Gravity.CENTER);
        card.addView(tenGodView, marginTop(3));

        TextView hiddenView = text(shortHidden(hidden), 10, MUTED, false);
        hiddenView.setGravity(Gravity.CENTER);
        card.addView(hiddenView, marginTop(2));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = dp(4);
        row.addView(card, lp);
    }

    private void addElementBar(LinearLayout parent, String element, int count) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView name = text(element, 14, TEXT, true);
        row.addView(name, new LinearLayout.LayoutParams(dp(24), dp(26)));

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
        row.addView(value, new LinearLayout.LayoutParams(dp(34), dp(26)));

        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(26));
        rowLp.topMargin = dp(1);
        parent.addView(row, rowLp);
    }

    private void addTarotResultPanel() {
        if (currentFacts == null) return;

        LinearLayout panel = column();
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setPadding(dp(12), dp(14), dp(12), dp(14));
        GradientDrawable background = round(CARD_2, 18);
        background.setStroke(dp(1), Color.rgb(87, 69, 121));
        panel.setBackground(background);
        LinearLayout host = resultTabContent == null ? resultCard : resultTabContent;
        host.addView(panel, marginTop(6));

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
        panel.addView(coreNumbers, marginTop(5));

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
        panel.addView(innerOuter, marginTop(6));

        TextView contrast = text(
                "外在 " + currentFacts.detailText("personalityCardNumber")
                        + " " + currentFacts.detailText("personalityCardName")
                        + "　↔　內在 " + currentFacts.detailText("soulCardNumber")
                        + " " + currentFacts.detailText("soulCardName"),
                13, ACCENT, true);
        contrast.setGravity(Gravity.CENTER);
        panel.addView(contrast, marginTop(6));

        TextView card = text(currentFacts.detailText("birthCardDisplay"), 27, TEXT, true);
        card.setGravity(Gravity.CENTER);
        panel.addView(card);

        TextView lifePath = text("生命靈數 " + currentFacts.detailText("lifePathDisplay"),
                23, ACCENT, true);
        lifePath.setGravity(Gravity.CENTER);
        panel.addView(lifePath, marginTop(9));

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
        panel.addView(core, marginTop(9));

        addPanelSection(panel, "出生牌組", formatBirthCards());
        addPanelSection(panel, "四大巔峰", formatPairedLists(
                currentFacts.detail("pinnacles"), currentFacts.detail("pinnacleTiming")));
        addPanelSection(panel, "四大挑戰", formatList(currentFacts.detail("challenges")));
        addPanelSection(panel, "人生三大週期", formatList(currentFacts.detail("periodCycles")));
        addPanelSection(panel, "計算規則", formatList(currentFacts.detail("calculationNotes")));

        TextView note = text(currentFacts.detailText("note"), 11, MUTED, false);
        note.setGravity(Gravity.CENTER);
        note.setLineSpacing(dp(2), 1f);
        panel.addView(note, marginTop(6));
    }

    private void addNumerologyIdentityCard(LinearLayout row,
                                                String heading,
                                                String number,
                                                String subtitle) {
        LinearLayout card = column();
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(8), dp(9), dp(8), dp(9));
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
        lp.leftMargin = dp(3);
        lp.rightMargin = dp(3);
        row.addView(card, lp);
    }

    private void addPanelSection(LinearLayout panel, String heading, String body) {
        TextView h = text(heading, 12, GOLD, true);
        panel.addView(h, marginTop(11));
        TextView b = text(body, 13, TEXT, false);
        b.setLineSpacing(dp(2), 1f);
        panel.addView(b, marginTop(3));
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
        resultCard.addView(h, marginTop(11));
        TextView b = text(body, 16, TEXT, false);
        b.setLineSpacing(dp(2), 1f);
        resultCard.addView(b, marginTop(3));
    }

    private void startTeacherExplanation() {
        startTeacherExplanation("");
    }

    private void startTeacherExplanation(String initialQuestion) {
        String question = initialQuestion == null ? "" : initialQuestion.trim();
        OperationLog.add(this, "TEACHER_REQUESTED",
                selectedMode.name() + " · " + selectedAiStyle.name()
                        + (question.isEmpty() ? "" : " · followup"));
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
            pendingTeacherQuestion = question;
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_TEACHER_AUDIO);
            return;
        }
        pendingTeacherQuestion = "";
        openTeacherDialog(question);
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
            String question = pendingTeacherQuestion;
            pendingTeacherQuestion = "";
            openTeacherDialog(question);
        } else {
            OperationLog.add(this, "MIC_PERMISSION_DENIED", "");
            pendingTeacherStart = false;
            pendingTeacherQuestion = "";
            Toast.makeText(this, "需要麥克風權限才能跟老師對話", Toast.LENGTH_SHORT).show();
        }
    }

    private void openTeacherDialog(String initialQuestion) {
        closeTeacher();
        String directQuestion = initialQuestion == null ? "" : initialQuestion.trim();

        LinearLayout body = column();
        body.setPadding(dp(14), dp(14), dp(14), dp(12));
        body.setBackground(roundBorder(
                CARD, Color.rgb(92, 73, 127), 24, 1));

        TextView title = text("命理老師", 22, TEXT, true);
        body.addView(title);

        TextView hint = text(
                directQuestion.isEmpty()
                        ? "老師會先講 60–90 秒重點。想插話時按「我要問」，老師會立刻停下來聽你說。"
                        : "已經把你點的問題帶給老師，會直接回答，不會重新從頭介紹命盤。",
                13, MUTED, false);
        hint.setLineSpacing(dp(3), 1f);
        body.addView(hint, marginTop(6));

        teacherStatusText = text("正在準備…", 13, ACCENT, true);
        body.addView(teacherStatusText, marginTop(9));

        TextView youLabel = text("你剛剛說", 11, GOLD, true);
        body.addView(youLabel, marginTop(6));
        teacherInputText = text(
                directQuestion.isEmpty() ? "—" : directQuestion,
                14, TEXT, false);
        teacherInputText.setLineSpacing(dp(2), 1f);
        body.addView(teacherInputText, marginTop(4));

        TextView teacherLabel = text("老師正在講", 11, GOLD, true);
        body.addView(teacherLabel, marginTop(9));
        teacherOutputText = text("等待老師上線…", 15, TEXT, false);
        teacherOutputText.setLineSpacing(dp(2), 1f);
        body.addView(teacherOutputText, marginTop(4));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button interrupt = secondaryButton("我要問");
        interrupt.setTextColor(Color.rgb(30, 22, 46));
        interrupt.setBackground(round(ACCENT, 14));
        interrupt.setOnClickListener(v -> {
            OperationLog.add(this, "TEACHER_INTERRUPT", "");
            GeminiFortuneLiveSession session = teacherSession;
            if (session != null) session.interrupt();
        });
        LinearLayout.LayoutParams interruptLp =
                new LinearLayout.LayoutParams(0, dp(44), 1f);
        interruptLp.rightMargin = dp(6);
        actions.addView(interrupt, interruptLp);

        Button close = secondaryButton("結束");
        close.setOnClickListener(v -> {
            AlertDialog dialog = teacherDialog;
            if (dialog != null) dialog.dismiss();
        });
        actions.addView(close, new LinearLayout.LayoutParams(0, dp(44), 1f));
        body.addView(actions, marginTop(11));

        teacherDialog = new AlertDialog.Builder(this)
                .setView(body)
                .create();
        teacherDialog.setOnDismissListener(dialog -> closeTeacherSessionOnly());
        teacherDialog.show();
        styleDarkDialog(teacherDialog);

        String teacherPrompt = FortuneTeacherPrompt.systemPrompt(
                selectedMode,
                selectedAiStyle,
                currentFacts,
                nameInput.getText().toString());
        String opening = directQuestion.isEmpty()
                ? FortuneTeacherPrompt.openingPrompt(nameInput.getText().toString())
                : "使用者剛剛點選追問：「" + directQuestion + "」。"
                + "不要做一般 60–90 秒開場，直接回答這個問題。"
                + "先給白話結論，再講 2–4 個 deterministicFacts 裡的具體依據，"
                + "最後補一句可以繼續追問的方向。";

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
        if (currentResult == null || currentFacts == null) return;
        if (activeHarness != null && aiCopy == null) {
            Toast.makeText(this, "AI 還在整理，完成後再產生分享圖片", Toast.LENGTH_SHORT).show();
            return;
        }

        final FortuneMode mode = selectedMode;
        final FortuneFacts facts = currentFacts;
        final FortuneResult result = currentResult;
        final AiFortuneCopy copy = aiCopy;
        final String displayName = nameInput == null
                ? "" : nameInput.getText().toString().trim();

        OperationLog.add(this, "SHARE_IMAGE_START", mode.name());
        Toast.makeText(this, "正在產生分享卡…", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            Bitmap bitmap = null;
            try {
                FortuneShareCardData data = FortuneShareCardData.from(
                        mode, facts, result, copy, displayName);
                bitmap = FortuneShareCardRenderer.render(data);

                File directory = new File(getCacheDir(), "share");
                if (!directory.exists() && !directory.mkdirs()) {
                    throw new IllegalStateException("Cannot create share cache");
                }

                File image = new File(directory, "crew_fortune_share.png");
                FileOutputStream stream = new FileOutputStream(image, false);
                try {
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                        throw new IllegalStateException("PNG encode failed");
                    }
                    stream.flush();
                } finally {
                    try { stream.close(); } catch (Exception ignored) {}
                }

                Uri uri = FileProvider.getUriForFile(
                        MainActivity.this,
                        getPackageName() + ".fileprovider",
                        image);

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("image/png");
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.setClipData(ClipData.newRawUri("Crew Fortune", uri));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                OperationLog.add(
                        MainActivity.this,
                        "SHARE_IMAGE_READY",
                        mode.name() + " · 1080x1350 · privacy_safe");

                runOnUiThread(() -> startActivity(
                        Intent.createChooser(intent, "分享你的命運")));
            } catch (Exception error) {
                OperationLog.add(
                        MainActivity.this,
                        "SHARE_IMAGE_FAILED",
                        safeErrorMessage(error));
                runOnUiThread(() -> Toast.makeText(
                        MainActivity.this,
                        "分享圖片產生失敗，請再試一次",
                        Toast.LENGTH_SHORT).show());
            } finally {
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
        }, "fortune-share-card").start();
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

    private void showAboutDialog() {
        LinearLayout panel = column();
        panel.setPadding(dp(16), dp(16), dp(16), dp(14));
        panel.setBackground(roundBorder(
                CARD, Color.rgb(92, 73, 127), 22, 1));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_launcher_foreground_art);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(
                dp(72), dp(72));
        logoLp.gravity = Gravity.CENTER_HORIZONTAL;
        panel.addView(logo, logoLp);

        TextView title = text("Crew Fortune · 命運研究所", 20, TEXT, true);
        title.setGravity(Gravity.CENTER);
        panel.addView(title, marginTop(8));

        TextView slogan = text("很認真算，別太認真信。", 14, GOLD, true);
        slogan.setGravity(Gravity.CENTER);
        panel.addView(slogan, marginTop(4));

        TextView version = text(
                "v" + appVersionName() + " · deterministic facts + AI interpretation",
                11, MUTED, false);
        version.setGravity(Gravity.CENTER);
        panel.addView(version, marginTop(5));

        Button close = secondaryButton("關閉");
        panel.addView(close, fixedHeightTop(44, 10));

        final AlertDialog dialog = new AlertDialog.Builder(this).setView(panel).create();
        close.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        styleDarkDialog(dialog);
    }

    private String appVersionName() {
        try {
            return getPackageManager()
                    .getPackageInfo(getPackageName(), 0)
                    .versionName;
        } catch (Exception ignored) {
            return "";
        }
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

    private FortuneProfile buildCurrentProfile() {
        BirthPlace birthPlace = null;
        if (selectedMode == FortuneMode.VEDIC_ASTROLOGY) {
            String latitudeText = latitudeInput == null ? "" : latitudeInput.getText().toString().trim();
            String longitudeText = longitudeInput == null ? "" : longitudeInput.getText().toString().trim();
            String zoneText = timeZoneInput == null ? "" : timeZoneInput.getText().toString().trim();
            if (latitudeText.isEmpty() || longitudeText.isEmpty() || zoneText.isEmpty()) {
                throw new IllegalArgumentException(
                        "印度星盤需要出生地 latitude、longitude 與 timezone");
            }
            final double latitude;
            final double longitude;
            try {
                latitude = Double.parseDouble(latitudeText);
                longitude = Double.parseDouble(longitudeText);
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException("出生地座標格式不正確", error);
            }
            birthPlace = new BirthPlace(
                    birthPlaceNameInput == null ? "" : birthPlaceNameInput.getText().toString(),
                    latitude,
                    longitude,
                    zoneText);
        }
        return new FortuneProfile(
                nameInput.getText().toString(),
                birthInput.getText().toString(),
                birthTimeInput.getText().toString(),
                selectedGender,
                birthPlace);
    }

    private FortunePreset currentPreset() {
        return new FortunePreset(
                nameInput.getText().toString(),
                birthInput.getText().toString(),
                birthTimeInput.getText().toString(),
                selectedGender,
                selectedMode,
                birthPlaceNameInput == null ? "" : birthPlaceNameInput.getText().toString(),
                latitudeInput == null ? "" : latitudeInput.getText().toString(),
                longitudeInput == null ? "" : longitudeInput.getText().toString(),
                timeZoneInput == null ? "" : timeZoneInput.getText().toString());
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
        if ((preset.mode == FortuneMode.BA_ZI
                || preset.mode == FortuneMode.VEDIC_ASTROLOGY)
                && preset.birthTime.isEmpty()) {
            Toast.makeText(this,
                    preset.mode == FortuneMode.BA_ZI
                            ? "八字 preset 需要出生時間"
                            : "印度星盤 preset 需要出生時間",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (preset.mode == FortuneMode.VEDIC_ASTROLOGY
                && preset.birthPlaceOrNull() == null) {
            Toast.makeText(this,
                    "印度星盤 preset 需要有效的出生地座標與時區",
                    Toast.LENGTH_SHORT).show();
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
        if (birthPlaceNameInput != null) birthPlaceNameInput.setText(preset.birthPlaceName);
        if (latitudeInput != null) latitudeInput.setText(preset.latitude);
        if (longitudeInput != null) longitudeInput.setText(preset.longitude);
        if (timeZoneInput != null) timeZoneInput.setText(preset.timeZoneId);
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
            if (birthPlaceNameInput != null) {
                birthPlaceNameInput.setText(state.getString("state_birth_place_name", ""));
            }
            if (latitudeInput != null) {
                latitudeInput.setText(state.getString("state_latitude", ""));
            }
            if (longitudeInput != null) {
                longitudeInput.setText(state.getString("state_longitude", ""));
            }
            if (timeZoneInput != null) {
                timeZoneInput.setText(state.getString("state_timezone", ""));
            }
            selectedGender = state.getString("state_gender", "");
            selectedMode = FortuneMode.valueOf(
                    state.getString("state_mode", FortuneMode.BA_ZI.name()));
            selectedAiStyle = AiStyle.valueOf(
                    state.getString("state_ai_style", AiStyle.FUNNY.name()));
            selectedResultTab = state.getInt("state_result_tab", 0);

            selectMode(selectedMode);
            if (!selectedGender.isEmpty()) selectGender(selectedGender);
            updateAiStyleButtons();

            if (state.getBoolean("state_has_result", false)) {
                FortuneProfile profile = buildCurrentProfile();
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
                    .put("career", copy.career)
                    .put("wealth", copy.wealth)
                    .put("relationships", copy.relationships)
                    .put("currentCycle", copy.currentCycle)
                    .put("longTerm", copy.longTerm)
                    .put("keyYears", copy.keyYears)
                    .put("topTraits", new JSONArray(copy.topTraits))
                    .put("topTraitEvidence", new JSONArray(copy.topTraitEvidence))
                    .put("followUps", new JSONArray(copy.followUps))
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

        LinearLayout panel = column();
        panel.setPadding(dp(12), dp(12), dp(12), dp(10));
        panel.setBackground(roundBorder(
                CARD, Color.rgb(92, 73, 127), 24, 1));

        TextView title = text("操作記錄", 22, TEXT, true);
        panel.addView(title);

        TextView hint = text("最近 200 筆 · 點任一筆查看完整內容", 12, MUTED, false);
        panel.addView(hint, marginTop(4));

        LinearLayout body = column();
        body.setPadding(0, dp(2), 0, dp(4));

        if (entries.isEmpty()) {
            body.addView(text("目前還沒有操作記錄", 14, MUTED, false), marginTop(9));
        } else {
            for (final OperationLog.Entry entry : entries) {
                TextView item = text(entry.listLabel(), 13, TEXT, true);
                item.setPadding(dp(10), dp(8), dp(10), dp(8));
                item.setBackground(roundBorder(
                        CARD_2, Color.rgb(80, 65, 111), 14, 1));
                item.setClickable(true);
                item.setOnClickListener(v -> {
                    OperationLog.add(this, "OPERATION_LOG_ITEM_OPENED", entry.action);
                    showOperationLogDetail(entry);
                });
                body.addView(item, marginTop(5));
            }
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(body);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(400));
        scrollLp.topMargin = dp(8);
        panel.addView(scroll, scrollLp);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button clear = secondaryButton("清除記錄");
        Button close = secondaryButton("關閉");
        LinearLayout.LayoutParams actionLp = new LinearLayout.LayoutParams(0, dp(44), 1f);
        actionLp.rightMargin = dp(6);
        actions.addView(clear, actionLp);
        actions.addView(close, new LinearLayout.LayoutParams(0, dp(44), 1f));
        panel.addView(actions, marginTop(8));

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(panel)
                .create();
        clear.setOnClickListener(v -> {
            OperationLog.clear(this);
            Toast.makeText(this, "已清除操作記錄", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        close.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        styleDarkDialog(dialog);
    }

    private void showOperationLogDetail(OperationLog.Entry entry) {
        LinearLayout panel = column();
        panel.setPadding(dp(14), dp(12), dp(14), dp(10));
        panel.setBackground(roundBorder(
                CARD, Color.rgb(92, 73, 127), 24, 1));

        TextView title = text(entry.action.replace('_', ' '), 20, TEXT, true);
        panel.addView(title);

        TextView time = text(entry.timeText(), 12, MUTED, false);
        panel.addView(time, marginTop(4));

        TextView detailLabel = text("內容", 11, GOLD, true);
        panel.addView(detailLabel, marginTop(11));

        String detailValue = entry.detail == null || entry.detail.isEmpty()
                ? "這筆事件沒有額外內容"
                : entry.detail;
        TextView detail = text(detailValue, 14, TEXT, false);
        detail.setLineSpacing(dp(2), 1f);
        detail.setPadding(dp(9), dp(8), dp(9), dp(8));
        detail.setBackground(roundBorder(
                CARD_2, Color.rgb(80, 65, 111), 14, 1));
        panel.addView(detail, marginTop(6));

        Button close = secondaryButton("關閉");
        panel.addView(close, marginTop(6));

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(panel)
                .create();
        close.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        styleDarkDialog(dialog);
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

    private Button modeButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setPadding(dp(8), 0, dp(8), 0);
        return button;
    }

    private void updateModeSelectionUi() {
        boolean bazi = selectedMode == FortuneMode.BA_ZI;
        boolean tarot = selectedMode == FortuneMode.TAROT_NUMEROLOGY;
        boolean vedic = selectedMode == FortuneMode.VEDIC_ASTROLOGY;
        if (baZiModeButton != null) {
            baZiModeButton.setBackground(roundBorder(
                    bazi ? ACCENT : CARD_2,
                    bazi ? ACCENT : Color.rgb(80, 65, 111),
                    16, 1));
            baZiModeButton.setTextColor(bazi ? Color.rgb(30, 22, 46) : TEXT);
        }
        if (tarotModeButton != null) {
            tarotModeButton.setBackground(roundBorder(
                    tarot ? ACCENT : CARD_2,
                    tarot ? ACCENT : Color.rgb(80, 65, 111),
                    16, 1));
            tarotModeButton.setTextColor(tarot ? Color.rgb(30, 22, 46) : TEXT);
        }
        if (vedicModeButton != null) {
            vedicModeButton.setBackground(roundBorder(
                    vedic ? ACCENT : CARD_2,
                    vedic ? ACCENT : Color.rgb(80, 65, 111),
                    16, 1));
            vedicModeButton.setTextColor(vedic ? Color.rgb(30, 22, 46) : TEXT);
        }
        if (modeLabel != null) {
            if (bazi) {
                modeLabel.setText(
                        "四柱、十神、大運、逐年流年 · 需要出生時間與性別\n"
                                + "以出生地當地民用時間排盤，目前不做真太陽時校正");
            } else if (tarot) {
                modeLabel.setText(
                        "人格牌、靈魂牌、生命道路、巔峰／挑戰、個人流年與個人月\n"
                                + "名字只用來稱呼你；計算只使用生日，不使用姓名或出生時間");
            } else {
                modeLabel.setText(
                        "Sidereal · Lahiri · Whole Sign · Mean Rahu/Ketu\n"
                                + "需要精確出生時間、latitude、longitude 與出生地 timezone");
            }
        }
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
        input.setPadding(dp(12), 0, dp(12), 0);
        input.setBackground(round(Color.rgb(29, 23, 45), 14));
        input.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));
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

    private void styleDarkDialog(AlertDialog dialog) {
        if (dialog == null || dialog.getWindow() == null) return;
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setDimAmount(0.68f);
    }

    private GradientDrawable roundBorder(
            int fillColor,
            int strokeColor,
            int radiusDp,
            int strokeDp) {
        GradientDrawable drawable = round(fillColor, radiusDp);
        drawable.setStroke(dp(strokeDp), strokeColor);
        return drawable;
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
