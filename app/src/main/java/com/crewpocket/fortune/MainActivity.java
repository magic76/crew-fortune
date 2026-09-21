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
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
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
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

public final class MainActivity extends Activity {
    private static final int REQUEST_TEACHER_AUDIO = 4101;
    private static final int BG = Color.rgb(23, 17, 38);
    static final int CARD = Color.rgb(39, 30, 60);
    static final int CARD_2 = Color.rgb(50, 38, 76);
    static final int TEXT = Color.rgb(248, 245, 255);
    static final int MUTED = Color.rgb(190, 181, 207);
    static final int ACCENT = Color.rgb(183, 156, 255);
    static final int GOLD = Color.rgb(255, 214, 128);

    private final FortuneEngine engine = new FortuneEngine();

    private FortuneMode selectedMode = FortuneMode.BA_ZI;
    private TextView modeLabel;
    private Button baZiModeButton;
    private Button tarotModeButton;
    private Button vedicModeButton;
    private TextView aiStatus;
    private Button strictStyleButton;
    private Button normalStyleButton;
    private Button funnyStyleButton;
    private AiStyle selectedAiStyle = AiStyle.FUNNY;
    private LinearLayout resultCard;
    private FortuneResult currentResult;
    private FortuneFacts currentFacts;
    private AiFortuneCopy aiCopy;
    private boolean pendingTeacherStart;
    private String pendingTeacherQuestion = "";
    private TextView aiLoadingStageText;
    private int aiLoadingStage = 0;
    private int selectedResultTab = 0;
    private LocalDate selectedVedicTransitDate;
    private long resultReferenceTimeMillis = -1L;
    private LinearLayout resultTabContent;
    private final VedicResultRenderer vedicResultRenderer = new VedicResultRenderer(this);
    private final BaZiResultRenderer baZiResultRenderer = new BaZiResultRenderer(this);
    private final TarotResultRenderer tarotResultRenderer = new TarotResultRenderer(this);
    private final FortuneAiController aiController = new FortuneAiController(this);
    private final FortuneTeacherController teacherController = new FortuneTeacherController(this);
    private final FortuneShareController shareController = new FortuneShareController(this);
    private final FortuneProfileController profileController = new FortuneProfileController(this);
    private final FortuneDialogController dialogController = new FortuneDialogController(this);

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        selectedAiStyle = AppConfig.getAiStyle(this);
        setContentView(buildScreen());
        refreshAiStatus();
        updateAiStyleButtons();
        if (state != null) {
            restoreInstanceState(state);
        } else {
            profileController.restoreLastProfile();
        }
        OperationLog.add(this, "APP_OPEN", "mode=" + selectedMode.name());
    }

    @Override protected void onDestroy() {
        OperationLog.add(this, "APP_DESTROY", "changingConfig=" + isChangingConfigurations());
        teacherController.close();
        aiController.close();
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
        profileController.saveState(outState);
        outState.putString("state_mode", selectedMode.name());
        outState.putString("state_ai_style", selectedAiStyle.name());
        outState.putBoolean("state_has_result", currentResult != null && currentFacts != null);
        outState.putInt("state_result_tab", selectedResultTab);
        outState.putLong("state_result_reference_time", resultReferenceTimeMillis);
        outState.putString("state_vedic_transit_date",
                FortuneResultState.transitDateText(selectedVedicTransitDate));
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
            int topInset = insets.getSystemWindowInsetTop();
            int bottomInset = insets.getSystemWindowInsetBottom();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                android.graphics.Insets barsAndIme = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                topInset = barsAndIme.top;
                bottomInset = barsAndIme.bottom;
            }
            view.setPadding(
                    baseLeft,
                    baseTop + topInset,
                    baseRight,
                    baseBottom + bottomInset);
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
        history.setOnClickListener(v -> dialogController.showOperationLog());
        top.addView(history);

        aiStatus = text("", 13, ACCENT, true);
        aiStatus.setGravity(Gravity.END);
        aiStatus.setPadding(dp(10), dp(6), 0, dp(6));
        aiStatus.setOnClickListener(v -> {
            OperationLog.add(this, "OPEN_AI_SETTINGS", "");
            dialogController.showApiKey();
        });
        top.addView(aiStatus);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.mipmap.ic_launcher);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        logo.setPadding(0, 0, 0, 0);
        logo.setBackground(round(CARD_2, 12));
        logo.setContentDescription("Crew Fortune");
        logo.setOnClickListener(v -> dialogController.showAbout());
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(34), dp(34));
        logoLp.leftMargin = dp(8);
        top.addView(logo, logoLp);

        root.addView(top);

        TextView title = text("看懂你的命盤，\n也看懂自己的節奏。", 32, TEXT, true);
        title.setLineSpacing(0, 1.04f);
        root.addView(title, marginTop(4));

        TextView sub = text("八字、塔羅生命靈數、印度星盤。\n固定規則排盤，AI 命理老師只負責把結果講成人話。", 15, MUTED, false);
        sub.setLineSpacing(dp(2), 1f);
        root.addView(sub, marginTop(6));

        LinearLayout form = column();
        form.setPadding(dp(12), dp(12), dp(12), dp(12));
        form.setBackground(round(CARD, 18));
        root.addView(form, marginTop(6));

        form.addView(label("選擇排盤方式"));

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

        profileController.addFields(form);

        TextView styleLabel = text("AI 解讀口吻（不影響命盤）", 12, MUTED, true);
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

        TextView styleHint = text("嚴謹＝專業｜普通＝白話｜風趣＝有梗；只改文字口吻。", 11, MUTED, false);
        form.addView(styleHint, marginTop(2));

        Button calculate = new Button(this);
        calculate.setText("開始排盤");
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
            teacherController.close();
            aiController.close();
            currentResult = null;
            currentFacts = null;
            aiCopy = null;
            selectedVedicTransitDate = null;
            resultReferenceTimeMillis = -1L;
            selectedResultTab = 0;
            if (resultCard != null) resultCard.setVisibility(View.GONE);
        }
        profileController.onModeSelected(mode);
        updateModeSelectionUi();
    }

    void selectModeFromProfileController(FortuneMode mode) {
        selectMode(mode);
    }

    void calculateFromProfileController() {
        calculate();
    }

    private void calculate() {
        if (selectedMode == FortuneMode.VEDIC_ASTROLOGY
                && profileController.needsBirthPlaceGeocoding()) {
            profileController.geocodeBirthPlace(true);
            return;
        }
        selectedResultTab = 0;
        selectedVedicTransitDate = null;
        OperationLog.add(this, "CALCULATE_START", selectedMode.name());
        teacherController.close();
        aiController.close();
        try {
            FortuneProfile profile = profileController.buildProfile(selectedMode);
            Date referenceTime = new Date();
            resultReferenceTimeMillis = referenceTime.getTime();
            currentFacts = engine.calculateFacts(selectedMode, profile, referenceTime);
            currentResult = engine.calculate(selectedMode, profile, referenceTime);
            FortunePresetStore.saveLast(
                    this,
                    profileController.currentPreset(selectedMode));
            OperationLog.add(this, "CALCULATE_SUCCESS",
                    selectedMode.name() + " · " + currentFacts.basis);
            aiCopy = null;
            boolean useAi = AppConfig.hasGeminiApiKey(this);
            renderResult(currentResult, useAi);
            if (useAi) aiController.start(profile);
        } catch (IllegalArgumentException error) {
            OperationLog.add(this, "CALCULATE_FAILED",
                    error.getMessage() == null ? "unknown" : error.getMessage());
            Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

     String safeErrorMessage(Throwable error) {
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
            baZiResultRenderer.addBaZiResultPanel();
        } else if (result.mode == FortuneMode.TAROT_NUMEROLOGY) {
            tarotResultRenderer.addTarotResultPanel();
        } else {
            TextView score = text(primaryMetric(result), 30, ACCENT, true);
            resultCard.addView(score, marginTop(8));

            TextView basis = text("計算依據｜" + result.basis, 13, MUTED, false);
            resultCard.addView(basis, marginTop(4));
        }

        if (aiLoading) {
            TextView loading = text(
                    result.mode == FortuneMode.BA_ZI
                            ? "✦ 四柱、五行與十神已排好。AI 命理老師正在整理重點與依據…"
                            : result.mode == FortuneMode.TAROT_NUMEROLOGY
                            ? "✦ 出生牌已確認。AI 命理老師正在把牌義整理成比較好懂的版本…"
                            : "✦ 命盤已排好。AI 命理老師正在整理星盤重點與週期…",
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
            if (!aiCopy.timing.isEmpty()) addSection("目前週期", aiCopy.timing);
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
        share.setOnClickListener(v -> shareController.share());
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
                renderResult(currentResult, aiController.isRunning() && aiCopy == null);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(40));
            lp.rightMargin = dp(4);
            tabs.addView(tab, lp);
        }
        resultCard.addView(tabScroll, marginTop(6));

        TextView tabGuide = text(
                FortuneResultTabCopy.overviewHint(result.mode),
                11, MUTED, true);
        tabGuide.setLineSpacing(dp(2), 1f);
        resultCard.addView(tabGuide, marginTop(5));

        TextView selectedTabGuide = text(
                FortuneResultTabCopy.description(result.mode, selectedResultTab),
                12, ACCENT, false);
        selectedTabGuide.setLineSpacing(dp(2), 1f);
        resultCard.addView(selectedTabGuide, marginTop(3));

        resultTabContent = column();
        resultCard.addView(resultTabContent, marginTop(4));
        renderUnifiedTab(result, aiLoading);
        addSharedResultActions(aiLoading, result.mode);
    }

    void showFactDetailDialog(String titleValue, Object value) {
        dialogController.showFactDetail(titleValue, value);
    }

    void askTeacherFromDialog(String question) {
        startTeacherExplanation(question);
    }

    void refreshAiStatusFromController() {
        refreshAiStatus();
    }

    FortuneResult shareResultForController() { return currentResult; }

    FortuneFacts shareFactsForController() { return currentFacts; }

    AiFortuneCopy shareCopyForController() { return aiCopy; }

    boolean isAiInterpretationRunning() { return aiController.isRunning(); }

    FortuneMode aiModeForController() { return selectedMode; }

    AiStyle aiStyleForController() { return selectedAiStyle; }

    int aiLoadingStageForController() { return aiLoadingStage; }

    void onAiCopyReady(AiFortuneCopy copy) {
        aiCopy = copy;
        if (currentResult != null) renderResult(currentResult, false);
    }

    void onAiFallback(String message) {
        aiCopy = null;
        if (currentResult != null) renderResult(currentResult, false);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    FortuneFacts rendererFacts() { return currentFacts; }

    AiFortuneCopy rendererCopy() { return aiCopy; }

    LocalDate rendererTransitDate() { return selectedVedicTransitDate; }

    LinearLayout resultHostLayout() {
        return resultTabContent == null ? resultCard : resultTabContent;
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
                    baZiResultRenderer.addBaZiResultPanel();
                    break;
                case 2:
                    baZiResultRenderer.addBaZiLuckTimelineTab();
                    break;
                case 3:
                    baZiResultRenderer.addBaZiTopicAnalysisTab();
                    break;
                case 0:
                default:
                    baZiResultRenderer.addBaZiOverviewTab(result, aiLoading);
                    break;
            }
            return;
        }

        if (result.mode == FortuneMode.VEDIC_ASTROLOGY) {
            switch (selectedResultTab) {
                case 1:
                    vedicResultRenderer.addVedicNatalTab();
                    break;
                case 2:
                    vedicResultRenderer.addVedicDashaTimelineTab();
                    break;
                case 3:
                    vedicResultRenderer.addVedicTopicAnalysisTab();
                    break;
                case 0:
                default:
                    vedicResultRenderer.addVedicOverviewTab(result, aiLoading);
                    break;
            }
            return;
        }

        switch (selectedResultTab) {
            case 1:
                tarotResultRenderer.addTarotResultPanel();
                break;
            case 2:
                tarotResultRenderer.addTarotTimelineTab();
                break;
            case 3:
                tarotResultRenderer.addTarotTopicAnalysisTab();
                break;
            case 0:
            default:
                tarotResultRenderer.addTarotOverviewTab(result, aiLoading);
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
            if (result.mode == FortuneMode.VEDIC_ASTROLOGY && !aiCopy.family.isEmpty()) {
                addPanelSection(panel, "家庭與子女", aiCopy.family);
            }
            if (!aiCopy.currentCycle.isEmpty()) {
                addPanelSection(panel, "目前週期", aiCopy.currentCycle);
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
                                ? "值得留意的時期" : "值得留意的年份",
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

     double numberValue(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(String.valueOf(value)); }
        catch (Exception ignored) { return 0.0; }
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

        if (!aiLoading
                && aiCopy != null
                && (selectedResultTab == 0 || selectedResultTab == 4)) {
            List<String> contextualQuestions = buildContextFollowUps();
            if (!contextualQuestions.isEmpty()) {
                addFollowUpQuestions(contextualQuestions);
            }
        }

        Button teacher = secondaryButton(
                aiLoading ? "語音老師 · 整理中" : "語音老師 · 補充／追問");
        teacher.setTextSize(14);
        teacher.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        teacher.setEnabled(!aiLoading);
        teacher.setAlpha(aiLoading ? 0.48f : 1f);
        if (!aiLoading) {
            teacher.setOnClickListener(v -> startTeacherExplanation());
        }
        resultCard.addView(teacher, fixedHeightTop(48, 8));

        Button share = secondaryButton(aiLoading ? "分享圖片 · 整理中" : "分享圖片");
        share.setEnabled(!aiLoading);
        share.setAlpha(aiLoading ? 0.48f : 1f);
        if (!aiLoading) {
            share.setOnClickListener(v -> shareController.share());
        }
        resultCard.addView(share, fixedHeightTop(48, 6));
    }

    void addOverviewTakeaways(LinearLayout panel, FortuneMode mode) {
        List<String> values = FortuneOverviewSnapshot.keyTakeaways(
                mode,
                currentFacts,
                aiCopy);

        TextView title = text("先看這三件事", 14, GOLD, true);
        panel.addView(title, marginTop(10));

        TextView hint = text(
                "先抓結論；原始命盤放在「本命／流年」，完整長文放在「解讀」。",
                11,
                MUTED,
                false);
        hint.setLineSpacing(dp(2), 1f);
        panel.addView(hint, marginTop(2));

        for (int index = 0; index < values.size() && index < 3; index++) {
            final int traitIndex = index;
            final String value = values.get(index);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.TOP);
            row.setPadding(dp(10), dp(9), dp(10), dp(9));
            row.setBackground(roundBorder(
                    Color.rgb(43, 33, 65),
                    Color.rgb(80, 65, 111),
                    13,
                    1));

            TextView number = text(
                    index == 0 ? "①" : index == 1 ? "②" : "③",
                    18,
                    ACCENT,
                    true);
            LinearLayout.LayoutParams numberLp = new LinearLayout.LayoutParams(
                    dp(30),
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            numberLp.rightMargin = dp(6);
            row.addView(number, numberLp);

            LinearLayout copy = column();
            TextView body = text(value, 13, TEXT, true);
            body.setLineSpacing(dp(2), 1f);
            copy.addView(body);

            if (aiCopy != null
                    && traitIndex < aiCopy.topTraitEvidence.size()) {
                final String evidence =
                        aiCopy.topTraitEvidence.get(traitIndex);
                TextView why = text(
                        "查看命盤依據 ›",
                        11,
                        ACCENT,
                        true);
                copy.addView(why, marginTop(4));
                row.setClickable(true);
                row.setOnClickListener(v ->
                        dialogController.showTraitEvidence(value, evidence));
            }

            row.addView(copy, new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f));
            panel.addView(row, marginTop(6));
        }
    }

    void addOverviewTiming(LinearLayout panel, FortuneMode mode) {
        String timing = FortuneOverviewSnapshot.currentTiming(
                mode,
                currentFacts,
                aiCopy);
        if (timing.isEmpty()) return;

        LinearLayout card = column();
        card.setPadding(dp(10), dp(10), dp(10), dp(10));
        card.setBackground(roundBorder(
                Color.rgb(55, 42, 82),
                Color.rgb(111, 91, 157),
                14,
                1));

        String label = mode == FortuneMode.BA_ZI
                ? "現在走到哪裡 · 大運／流年"
                : mode == FortuneMode.VEDIC_ASTROLOGY
                ? "現在走到哪裡 · Dasha／Gochar"
                : "現在走到哪裡 · 個人流年";
        card.addView(text(label, 12, GOLD, true));

        TextView body = text(timing, 13, TEXT, false);
        body.setLineSpacing(dp(2), 1f);
        card.addView(body, marginTop(4));
        panel.addView(card, marginTop(10));
    }

     void addTopTraits(LinearLayout panel) {
        TextView title = text("命盤中的 3 個明顯特徵", 13, GOLD, true);
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
                    dialogController.showTraitEvidence(traitValue, evidence));

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
                        ? "你可能想問"
                        : "這一頁可以直接問",
                13, GOLD, true);
        resultCard.addView(title, marginTop(12));

        int index = 0;
        for (final String question : questions) {
            if (index >= 4) break;

            Button button = secondaryButton(question + "  ›");
            button.setTextSize(13);
            button.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            button.setMinHeight(dp(56));
            button.setMinimumHeight(0);
            button.setPadding(dp(14), dp(10), dp(14), dp(10));
            button.setSingleLine(false);
            button.setMaxLines(3);
            button.setOnClickListener(v -> {
                OperationLog.add(
                        MainActivity.this,
                        "TEACHER_FOLLOWUP_SELECTED",
                        "chars=" + question.length());
                startTeacherExplanation(question);
            });

            LinearLayout.LayoutParams lp = marginTop(index == 0 ? 6 : 7);
            lp.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            resultCard.addView(button, lp);
            index++;
        }
    }

    private List<String> buildContextFollowUps() {
        List<String> out = new ArrayList<String>();
        if (aiCopy == null) return out;

        if (selectedResultTab == 0 || selectedResultTab == 4) {
            out.addAll(aiCopy.followUps);
            return trimQuestions(out, 4);
        }

        if (selectedResultTab == 1) {
            if (selectedMode == FortuneMode.BA_ZI) {
                out.add("我的日主和旺衰放到生活裡，最像什麼？");
                out.add("我的十神組合最明顯的優勢是什麼？");
                out.add("我的本命最容易卡在哪裡？");
                out.add("五行結構對我的做事方式有什麼影響？");
            } else if (selectedMode == FortuneMode.VEDIC_ASTROLOGY) {
                out.add("我的 Lagna 和 Lagna lord 最像我的地方是什麼？");
                out.add("Moon Nakshatra 對我的內在節奏代表什麼？");
                out.add("本命裡最值得注意的 house lord 落點是什麼？");
                out.add("哪些 Drishti 或 conjunction 最影響我的做事方式？");
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
            } else if (selectedMode == FortuneMode.VEDIC_ASTROLOGY) {
                out.add("我現在這個 Mahadasha 對工作代表什麼？");
                out.add("目前 Gochar 哪幾顆星最直接碰到我的本命？");
                out.add("接下來哪個 Antardasha 最值得我先理解？");
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
            } else if (selectedMode == FortuneMode.VEDIC_ASTROLOGY) {
                out.add("我的工作適合什麼方向？請引用 10 宮與目前 Dasha。");
                out.add("財運比較明顯在哪些 Dasha 時期？");
                out.add("感情最大的盲點是什麼？請引用 7 宮與 Venus。");
                out.add("目前 Mahadasha / Antardasha 對哪個生活主題最有關聯？");
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

     void addYearHighlights(LinearLayout panel) {
        List<FortuneYearHighlightBuilder.Highlight> highlights =
                FortuneYearHighlightBuilder.build(selectedMode, currentFacts);
        if (highlights.isEmpty()) return;

        TextView title = text(
                selectedMode == FortuneMode.VEDIC_ASTROLOGY
                        ? "值得留意的時期 · 先看這幾個"
                        : "值得留意的年份 · 先看這幾個",
                13, GOLD, true);
        panel.addView(title, marginTop(11));

        TextView hint = text(
                selectedMode == FortuneMode.BA_ZI
                        ? "依十神、合沖與工作／財務／感情訊號挑出；不是吉凶排名。"
                        : selectedMode == FortuneMode.VEDIC_ASTROLOGY
                        ? "依 Vimshottari Mahadasha / Antardasha 的實際起訖順序顯示；不是吉凶排名。"
                        : "依個人流年的週期主題挑出；不是吉凶排名。",
                11, MUTED, false);
        panel.addView(hint, marginTop(2));

        for (FortuneYearHighlightBuilder.Highlight highlight : highlights) {
            LinearLayout card = column();
            card.setPadding(dp(10), dp(10), dp(10), dp(13));
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
                    dialogController.showHighlightEvidence(highlight));
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

     void addAskTeacherAction(
            LinearLayout card,
            String label,
            String question) {
        Button ask = secondaryButton(label);
        ask.setTextSize(12);
        ask.setMinHeight(dp(44));
        ask.setMinimumHeight(0);
        ask.setPadding(dp(12), dp(8), dp(12), dp(8));
        ask.setOnClickListener(v -> startTeacherExplanation(question));
        LinearLayout.LayoutParams askLp = marginTop(7);
        askLp.height = LinearLayout.LayoutParams.WRAP_CONTENT;
        card.addView(ask, askLp);
    }

     void updateAiLoadingStage(int stage) {
        aiLoadingStage = Math.max(0, Math.min(2, stage));
        if (aiLoadingStageText != null && currentResult != null) {
            aiLoadingStageText.setText(aiLoadingStageLabel(currentResult.mode));
        }
    }

    private String aiLoadingStageLabel(FortuneMode mode) {
        if (aiLoadingStage == 2) {
            return mode == FortuneMode.VEDIC_ASTROLOGY
                    ? "正在補足內容與格式，避免漏掉 Dasha 時期或星盤依據"
                    : "正在補足內容與格式，避免漏掉重要年份或依據";
        }
        if (aiLoadingStage == 1) {
            if (mode == FortuneMode.BA_ZI) {
                return "正在撰寫個性、工作、財運、感情與未來十年";
            }
            if (mode == FortuneMode.VEDIC_ASTROLOGY) {
                return "正在撰寫個性、工作、財務、感情與 Dasha 長期節奏";
            }
            return "正在撰寫個性、工作、資源、感情與未來幾年";
        }
        if (mode == FortuneMode.BA_ZI) {
            return "正在整理本命、大運、流年與主題訊號";
        }
        if (mode == FortuneMode.VEDIC_ASTROLOGY) {
            return "正在整理 Lagna、九曜、12 宮、Nakshatra 與 Vimshottari Dasha";
        }
        return "正在整理本命牌、人生階段、個人流年與主題";
    }

    private void addAiLoadingBanner(FortuneMode mode) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(11), dp(11), dp(11), dp(14));
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

        TextView title = text("AI 命理老師正在整理完整解讀", 14, TEXT, true);
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

        String subtitleValue;
        if (mode == FortuneMode.BA_ZI) {
            subtitleValue = "本命、流年等資料已經可以先看；AI 正在把工作、財運、感情與未來十年整理成完整報告。";
        } else if (mode == FortuneMode.VEDIC_ASTROLOGY) {
            subtitleValue = "Lagna、九曜、12 宮與 Dasha 已經可以先看；AI 正在只依 deterministic Vedic facts 整理完整報告。";
        } else {
            subtitleValue = "本命與流年資料已經可以先看；AI 正在把個性、工作、資源、感情與未來幾年整理成完整報告。";
        }
        TextView subtitle = text(subtitleValue, 13, MUTED, false);
        subtitle.setLineSpacing(dp(2), 1f);
        panel.addView(subtitle, marginTop(5));

        String longTerm = mode == FortuneMode.BA_ZI
                ? "未來十年"
                : mode == FortuneMode.VEDIC_ASTROLOGY
                ? "Dasha 長期節奏"
                : "未來幾年";
        String timingLabel = mode == FortuneMode.VEDIC_ASTROLOGY ? "值得留意的時期" : "值得留意的年份";
        String[] sections = {
                "總覽",
                "性格、優勢與盲點",
                "工作",
                "財運與資源",
                "感情與人際",
                longTerm,
                timingLabel
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

     LinearLayout resultPanel() {
        LinearLayout panel = column();
        panel.setPadding(dp(11), dp(12), dp(11), dp(16));
        panel.setBackground(roundBorder(
                CARD_2, Color.rgb(86, 70, 119), 16, 1));
        resultTabContent.addView(panel, marginTop(4));
        return panel;
    }

     LinearLayout topicCard(LinearLayout parent, String title, String subtitle) {
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

     void addTopicLine(LinearLayout card, String label, String value) {
        TextView l = text(label, 11, GOLD, true);
        card.addView(l, marginTop(8));
        TextView v = text(value == null || value.isEmpty() ? "—" : value, 14, TEXT, false);
        v.setLineSpacing(dp(2), 1f);
        card.addView(v, marginTop(2));
    }

     void addTopicBoundary(LinearLayout card, String value) {
        if (value == null || value.isEmpty()) return;
        TextView v = text(value, 11, MUTED, false);
        v.setLineSpacing(dp(2), 1f);
        card.addView(v, marginTop(8));
    }

     void addClickableFactCard(
            LinearLayout parent,
            String title,
            String summary,
            final Runnable action) {
        addClickableFactCard(parent, title, summary, action, "");
    }

     void addClickableFactCard(
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
        TextView moreView = more;
        moreView.setGravity(Gravity.CENTER_VERTICAL);
        moreView.setPadding(0, dp(5), 0, dp(5));
        LinearLayout.LayoutParams moreLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        moreLp.gravity = Gravity.CENTER_VERTICAL;
        actions.addView(moreView, moreLp);

        if (askQuestion != null && !askQuestion.trim().isEmpty()) {
            Button ask = secondaryButton("問老師");
            ask.setTextSize(11);
            ask.setOnClickListener(v ->
                    startTeacherExplanation(askQuestion));
            ask.setMinHeight(dp(44));
            ask.setMinimumHeight(0);
            ask.setPadding(dp(10), dp(7), dp(10), dp(7));
            LinearLayout.LayoutParams askLp = new LinearLayout.LayoutParams(
                    dp(88), LinearLayout.LayoutParams.WRAP_CONTENT);
            askLp.leftMargin = dp(6);
            askLp.gravity = Gravity.CENTER_VERTICAL;
            actions.addView(ask, askLp);
        }

        card.addView(actions, marginTop(4));
        card.setClickable(true);
        card.setOnClickListener(v -> action.run());
        parent.addView(card, marginTop(8));
    }

     String summaryLuck(Object value) {
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

     Object mapObjectValue(Object source, String key) {
        if (!(source instanceof Map)) return null;
        return ((Map<?, ?>) source).get(key);
    }

     String compactValue(Object value) {
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

     String compactRelationshipYears(Object value) {
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

     String firstMeaningfulInteraction(Object value) {
        if (!(value instanceof List)) return compactValue(value);
        for (Object item : (List<?>) value) {
            String text = String.valueOf(item);
            if (!text.contains("未偵測")) return text;
        }
        return "與本命主要地支互動較少";
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

     String localReportSection(String key) {
        if (currentFacts == null) return "";
        Map<String, String> sections = FortuneLocalReport.sections(currentFacts);
        String value = sections.get(key);
        if (value != null) return value;
        for (Map.Entry<String, String> entry : sections.entrySet()) return entry.getValue();
        return "";
    }

     LinearLayout.LayoutParams fixedHeightTop(int heightDp, int topDp) {
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

     void addPillarCard(LinearLayout row,
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

     void addElementBar(LinearLayout parent, String element, int count) {
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

     void addNumerologyIdentityCard(LinearLayout row,
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

     void addPanelSection(LinearLayout panel, String heading, String body) {
        TextView h = text(heading, 12, GOLD, true);
        panel.addView(h, marginTop(11));
        TextView b = text(body, 13, TEXT, false);
        b.setLineSpacing(dp(2), 1f);
        panel.addView(b, marginTop(3));
    }

     String formatTenGods() {
        Object source = currentFacts == null ? null : currentFacts.detail("tenGods");
        return "年｜" + mapValue(source, "yearStem") + " · " + mapValue(source, "yearBranch")
                + "\n月｜" + mapValue(source, "monthStem") + " · " + mapValue(source, "monthBranch")
                + "\n日｜日主 · " + mapValue(source, "dayBranch")
                + "\n時｜" + mapValue(source, "timeStem") + " · " + mapValue(source, "timeBranch");
    }

     String formatLuckPillars() {
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

     String formatCurrentAnnual() {
        Object annual = currentFacts == null ? null : currentFacts.detail("currentAnnual");
        if (!(annual instanceof Map)) return "";
        return mapValue(annual, "year") + " " + mapValue(annual, "ganZhi")
                + "｜十神 " + mapValue(annual, "tenGod")
                + "｜五行 " + mapValue(annual, "element")
                + "\n" + formatList(((Map<?, ?>) annual).get("interactions"));
    }

     String formatBirthCards() {
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

     String formatPairedLists(Object first, Object second) {
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

     String formatList(Object value) {
        if (!(value instanceof java.util.List)) return value == null ? "" : String.valueOf(value);
        StringBuilder out = new StringBuilder();
        for (Object item : (java.util.List<?>) value) {
            if (out.length() > 0) out.append("\n");
            out.append("• ").append(String.valueOf(item));
        }
        return out.toString();
    }

     String formatMap(Object value) {
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

     String mapValue(Object source, String key) {
        if (!(source instanceof Map)) return "";
        Object value = ((Map<?, ?>) source).get(key);
        return value == null ? "" : String.valueOf(value);
    }

     int intMapValue(Object source, String key) {
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
            dialogController.showApiKey();
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
        teacherController.open(question);
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
            teacherController.open(question);
        } else {
            OperationLog.add(this, "MIC_PERMISSION_DENIED", "");
            pendingTeacherStart = false;
            pendingTeacherQuestion = "";
            Toast.makeText(this, "需要麥克風權限才能跟老師對話", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshAiStatus() {
        if (aiStatus == null) return;
        aiStatus.setText(AppConfig.hasGeminiApiKey(this) ? "AI：ON ⚙" : "AI：OFF ⚙");
    }

    private void restoreInstanceState(Bundle state) {
        try {
            profileController.restoreState(state);
            selectedMode = FortuneMode.valueOf(
                    state.getString(
                            "state_mode",
                            FortuneMode.BA_ZI.name()));
            selectedAiStyle = AiStyle.valueOf(
                    state.getString(
                            "state_ai_style",
                            AiStyle.FUNNY.name()));
            selectedResultTab =
                    state.getInt("state_result_tab", 0);
            resultReferenceTimeMillis =
                    state.getLong(
                            "state_result_reference_time",
                            -1L);
            String savedTransitDate =
                    state.getString(
                            "state_vedic_transit_date",
                            "");
            selectedVedicTransitDate =
                    FortuneResultState.parseTransitDate(
                            savedTransitDate);

            selectMode(selectedMode);
            updateAiStyleButtons();

            if (state.getBoolean("state_has_result", false)) {
                FortuneProfile profile =
                        profileController.buildProfile(selectedMode);
                Date referenceTime =
                        FortuneResultState.referenceDate(
                                resultReferenceTimeMillis);
                resultReferenceTimeMillis =
                        referenceTime.getTime();
                currentFacts = engine.calculateFacts(
                        selectedMode,
                        profile,
                        referenceTime);
                currentResult = engine.calculate(
                        selectedMode,
                        profile,
                        referenceTime);

                String aiRaw =
                        state.getString("state_ai_copy", "");
                if (!aiRaw.isEmpty()) {
                    try {
                        aiCopy = AiFortuneCopy.parse(aiRaw);
                    } catch (Exception ignored) {
                        aiCopy = null;
                    }
                }
                renderResult(currentResult, false);
            }

            OperationLog.add(
                    this,
                    "STATE_RESTORED",
                    state.getBoolean("state_has_result", false)
                            ? "result_restored"
                            : "input_restored");
        } catch (Exception error) {
            profileController.restoreLastProfile();
            OperationLog.add(
                    this,
                    "STATE_RESTORE_FAILED",
                    safeErrorMessage(error));
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
                    .put("family", copy.family)
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

     void showVedicTransitDatePicker() {
        LocalDate base;
        try {
            base = LocalDate.parse(currentFacts == null
                    ? LocalDate.now().toString()
                    : currentFacts.detailText("currentTransitDate"));
        } catch (Exception ignored) {
            base = LocalDate.now();
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, day) ->
                        applyVedicTransitDate(LocalDate.of(year, month + 1, day)),
                base.getYear(),
                base.getMonthValue() - 1,
                base.getDayOfMonth());
        LocalDate today = LocalDate.now();
        dialog.getDatePicker().setMinDate(
                java.util.Date.from(
                        today.minusYears(1)
                                .atStartOfDay(ZoneId.systemDefault())
                                .toInstant()).getTime());
        dialog.getDatePicker().setMaxDate(
                java.util.Date.from(
                        today.plusYears(3)
                                .atTime(23, 59)
                                .atZone(ZoneId.systemDefault())
                                .toInstant()).getTime());
        dialog.show();
    }

     void applyVedicTransitDate(LocalDate targetDate) {
        if (selectedMode != FortuneMode.VEDIC_ASTROLOGY) return;
        teacherController.close();
        aiController.close();
        try {
            FortuneProfile profile =
                    profileController.buildProfile(selectedMode);
            Date target;
            if (targetDate == null) {
                target = new Date();
                selectedVedicTransitDate = null;
            } else {
                ZoneId zone =
                        ZoneId.of(profileController.selectedTimeZoneId());
                target = Date.from(targetDate.atTime(12, 0).atZone(zone).toInstant());
                selectedVedicTransitDate = targetDate;
            }
            resultReferenceTimeMillis = target.getTime();
            currentFacts = engine.calculateFacts(FortuneMode.VEDIC_ASTROLOGY, profile, target);
            currentResult = engine.calculate(FortuneMode.VEDIC_ASTROLOGY, profile, target);
            aiCopy = null;
            selectedResultTab = 2;
            renderResult(currentResult, false);
            OperationLog.add(
                    this,
                    "VEDIC_TRANSIT_DATE_SELECTED",
                    currentFacts.detailText("currentTransitDate"));
        } catch (Exception error) {
            Toast.makeText(
                    this,
                    "Gochar 日期切換失敗：" + safeErrorMessage(error),
                    Toast.LENGTH_SHORT).show();
        }
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
                                + "計算只使用生日，不使用姓名或出生時間");
            } else {
                modeLabel.setText(
                        "Sidereal · Lahiri · Whole Sign · Mean Rahu/Ketu\n"
                                + "需要精確出生時間與出生地；時區可直接選，座標放在進階設定");
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

     Button secondaryButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setTextColor(TEXT);
        button.setMinHeight(dp(44));
        button.setMinimumHeight(0);
        button.setPadding(dp(12), dp(8), dp(12), dp(8));
        button.setBackground(round(CARD_2, 14));
        return button;
    }

     Button genderButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setTextColor(TEXT);
        button.setBackground(round(CARD_2, 14));
        return button;
    }

     EditText input(String hint) {
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

     TextView label(String value) { return text(value, 14, TEXT, true); }

     TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

     LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

     LinearLayout.LayoutParams marginTop(int dp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(dp);
        return lp;
    }

     void styleDarkDialog(AlertDialog dialog) {
        if (dialog == null || dialog.getWindow() == null) return;
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setDimAmount(0.68f);
    }

     GradientDrawable roundBorder(
            int fillColor,
            int strokeColor,
            int radiusDp,
            int strokeDp) {
        GradientDrawable drawable = round(fillColor, radiusDp);
        drawable.setStroke(dp(strokeDp), strokeColor);
        return drawable;
    }

     GradientDrawable round(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

     int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
