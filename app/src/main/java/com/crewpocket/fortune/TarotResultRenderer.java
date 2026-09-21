package com.crewpocket.fortune;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Map;


/** Result rendering for Tarot mode. */
final class TarotResultRenderer {
    private final MainActivity host;

    TarotResultRenderer(MainActivity host) {
        this.host = host;
    }

    void addTarotOverviewTab(FortuneResult result, boolean aiLoading) {
        LinearLayout panel = host.resultPanel();

        LinearLayout identities = new LinearLayout(host);
        identities.setOrientation(LinearLayout.HORIZONTAL);
        identities.setGravity(Gravity.CENTER);
        host.addNumerologyIdentityCard(
                identities,
                "外在人格牌",
                host.rendererFacts().detailText("personalityCardNumber"),
                host.rendererFacts().detailText("personalityCardName"));
        host.addNumerologyIdentityCard(
                identities,
                "內在靈魂牌",
                host.rendererFacts().detailText("soulCardNumber"),
                host.rendererFacts().detailText("soulCardName"));
        panel.addView(identities);

        host.addPanelSection(panel, "核心數字",
                "生命道路 " + host.rendererFacts().detailText("lifePathDisplay")
                        + "　·　天賦 " + host.rendererFacts().detailText("talentNumbers")
                        + "\n生日數 " + host.rendererFacts().detailText("birthdayNumber")
                        + "　·　態度數 " + host.rendererFacts().detailText("attitudeNumber"));

        host.addPanelSection(panel, "目前流年",
                host.rendererFacts().detailText("personalYearCalendarYear")
                        + " 年｜個人流年 " + host.rendererFacts().detailText("personalYear")
                        + "「" + host.rendererFacts().detailText("personalYearCardName") + "」"
                        + "\n個人月 " + host.rendererFacts().detailText("personalMonth")
                        + "｜" + currentTarotYearSummary());

        if (!aiLoading && host.rendererCopy() != null && !host.rendererCopy().topTraits.isEmpty()) {
            host.addTopTraits(panel);
        }

        String summary;
        if (aiLoading) {
            summary = host.localReportSection("核心總覽");
        } else if (host.rendererCopy() != null && !host.rendererCopy().overview.isEmpty()) {
            summary = host.rendererCopy().overview;
        } else {
            summary = host.localReportSection("核心總覽");
        }
        if (!summary.isEmpty()) host.addPanelSection(panel, "重點解讀", summary);
    }
    void addTarotTimelineTab() {
        LinearLayout panel = host.resultPanel();

        TextView intro = host.text(
                "個人流年看每一年的主題循環；個人月則把今年拆成 12 個月。數字每 9 年循環一次，所以重點是當年的課題與節奏，不是吉凶分數。",
                13, MainActivity.MUTED, false);
        intro.setLineSpacing(host.dp(3), 1f);
        panel.addView(intro);

        host.addPanelSection(panel, "今年",
                host.rendererFacts().detailText("personalYearCalendarYear")
                        + "｜流年 " + host.rendererFacts().detailText("personalYear")
                        + "「" + host.rendererFacts().detailText("personalYearCardName") + "」"
                        + "\n" + currentTarotYearSummary());
        host.addYearHighlights(panel);

        TextView yearsTitle = host.text(
                "年度時間軸 · " + host.rendererFacts().detailText("personalYearTimelineStartYear")
                        + "–" + host.rendererFacts().detailText("personalYearTimelineEndYear"),
                13, MainActivity.GOLD, true);
        panel.addView(yearsTitle, host.marginTop(20));

        Object yearsRaw = host.rendererFacts().detail("personalYearTimeline");
        if (yearsRaw instanceof List) {
            for (Object raw : (List<?>) yearsRaw) {
                if (!(raw instanceof Map)) continue;
                final Map<?, ?> year = (Map<?, ?>) raw;
                String title = host.mapValue(year, "year")
                        + "　流年 " + host.mapValue(year, "personalYear")
                        + "「" + host.mapValue(year, "cardName") + "」";
                String summary = host.mapValue(year, "plainSummary")
                        + "\n關鍵字：" + host.mapValue(year, "keywords");
                host.addClickableFactCard(
                        panel,
                        title,
                        summary,
                        () -> host.showFactDetailDialog(
                                host.mapValue(year, "year") + " 個人流年", year),
                        "請直接回答 " + host.mapValue(year, "year")
                                + " 年的個人流年對我代表什麼？請用 deterministic facts 說明。");
            }
        }

        TextView monthTitle = host.text("今年 12 個個人月", 13, MainActivity.GOLD, true);
        panel.addView(monthTitle, host.marginTop(22));

        Object monthsRaw = host.rendererFacts().detail("personalMonthTimeline");
        if (monthsRaw instanceof List) {
            for (Object raw : (List<?>) monthsRaw) {
                if (!(raw instanceof Map)) continue;
                final Map<?, ?> month = (Map<?, ?>) raw;
                String title = host.mapValue(month, "month") + " 月　"
                        + host.mapValue(month, "personalMonth")
                        + "「" + host.mapValue(month, "cardName") + "」";
                String summary = host.mapValue(month, "plainSummary")
                        + "　·　" + host.mapValue(month, "keywords");
                host.addClickableFactCard(
                        panel,
                        title,
                        summary,
                        () -> host.showFactDetailDialog(
                                host.mapValue(month, "month") + " 月個人月", month),
                        "請直接回答今年 " + host.mapValue(month, "month")
                                + " 月的個人月主題對我代表什麼？請用 deterministic facts 說明。");
            }
        }

        host.addPanelSection(panel, "人生階段",
                "四大巔峰：" + host.compactValue(host.rendererFacts().detail("pinnacles"))
                        + "\n時程：" + host.compactValue(host.rendererFacts().detail("pinnacleTiming"))
                        + "\n四大挑戰：" + host.compactValue(host.rendererFacts().detail("challenges"))
                        + "\n三大週期：" + host.compactValue(host.rendererFacts().detail("periodCycles")));
    }
    void addTarotTopicAnalysisTab() {
        LinearLayout panel = host.resultPanel();

        TextView intro = host.text(
                "塔羅生命靈數的主題分析會把本命數字、年度循環與完整文字解讀放在一起。先看結論與依據，語音只用來補充。",
                13, MainActivity.MUTED, false);
        intro.setLineSpacing(host.dp(3), 1f);
        panel.addView(intro);

        LinearLayout personality = host.topicCard(panel, "個性與內外", "人格牌 × 靈魂牌 × 生命道路");
        host.addTopicLine(personality, "本命",
                "外在 " + host.rendererFacts().detailText("personalityCardNumber")
                        + "「" + host.rendererFacts().detailText("personalityCardName") + "」"
                        + "　·　內在 " + host.rendererFacts().detailText("soulCardNumber")
                        + "「" + host.rendererFacts().detailText("soulCardName") + "」"
                        + "\n生命道路 " + host.rendererFacts().detailText("lifePathDisplay")
                        + "　·　天賦 " + host.rendererFacts().detailText("talentNumbers"));
        host.addTopicLine(personality, "解讀",
                host.rendererCopy() != null && !host.rendererCopy().personality.isEmpty()
                        ? FortuneResultTabCopy.compactInterpretation(host.rendererCopy().personality, "")
                        : host.localReportSection("內在 vs 外在"));
        host.addAskTeacherAction(
                personality,
                "問老師個性",
                "請直接回答我的外在人格牌、內在靈魂牌與生命道路之間最明顯的性格落差與優勢。");

        LinearLayout career = host.topicCard(panel, "工作與資源", "生命道路 × 態度數 × 巔峰 × 當前流年");
        host.addTopicLine(career, "依據",
                "生命道路 " + host.rendererFacts().detailText("lifePathDisplay")
                        + "　·　態度數 " + host.rendererFacts().detailText("attitudeNumber")
                        + "\n四大巔峰 " + host.compactValue(host.rendererFacts().detail("pinnacles"))
                        + "\n目前流年 " + host.rendererFacts().detailText("personalYear")
                        + "「" + host.rendererFacts().detailText("personalYearCardName") + "」");
        host.addTopicLine(career, "時間",
                currentTarotYearSummary());
        host.addTopicLine(career, "解讀",
                host.rendererCopy() != null && !host.rendererCopy().career.isEmpty()
                        ? FortuneResultTabCopy.compactInterpretation(host.rendererCopy().career, "")
                        : FortuneResultTabCopy.compactInterpretation("", host.localReportSection("工作與財務")));
        host.addAskTeacherAction(
                career,
                "用語音追問工作",
                "請直接回答我的工作優勢與現在的職涯節奏。請用生命道路、態度數、巔峰與個人流年說明。");

        LinearLayout wealth = host.topicCard(panel, "財運與資源", "生命道路 × 巔峰 × 個人流年");
        host.addTopicLine(wealth, "依據",
                "生命道路 " + host.rendererFacts().detailText("lifePathDisplay")
                        + "　·　四大巔峰 " + host.compactValue(host.rendererFacts().detail("pinnacles"))
                        + "\n目前流年 " + host.rendererFacts().detailText("personalYear")
                        + "「" + host.rendererFacts().detailText("personalYearCardName") + "」");
        host.addTopicLine(wealth, "時間", currentTarotYearSummary());
        host.addTopicLine(wealth, "解讀",
                host.rendererCopy() != null && !host.rendererCopy().wealth.isEmpty()
                        ? FortuneResultTabCopy.compactInterpretation(host.rendererCopy().wealth, "")
                        : "塔羅生命靈數的財務解讀以資源使用、成果節奏與年度主題為主，不把牌義當成投資預測。");
        host.addAskTeacherAction(
                wealth,
                "問老師資源",
                "請直接回答我目前的資源與成果節奏。請用生命道路、巔峰與個人流年說明，不做投資預測。");

        LinearLayout relationship = host.topicCard(panel, "感情與人際", "內外牌 × 挑戰數 × 年度節奏");
        host.addTopicLine(relationship, "依據",
                "外在人格牌 " + host.rendererFacts().detailText("personalityCardNumber")
                        + "　·　內在靈魂牌 " + host.rendererFacts().detailText("soulCardNumber")
                        + "\n四大挑戰 " + host.compactValue(host.rendererFacts().detail("challenges")));
        host.addTopicLine(relationship, "時間",
                "目前流年 " + host.rendererFacts().detailText("personalYear")
                        + "「" + host.rendererFacts().detailText("personalYearCardName") + "」"
                        + "　·　個人月 " + host.rendererFacts().detailText("personalMonth"));
        host.addTopicLine(relationship, "解讀",
                host.rendererCopy() != null && !host.rendererCopy().relationships.isEmpty()
                        ? FortuneResultTabCopy.compactInterpretation(host.rendererCopy().relationships, "")
                        : FortuneResultTabCopy.compactInterpretation("", host.localReportSection("感情與人際")));
        host.addAskTeacherAction(
                relationship,
                "用語音追問感情",
                "請直接回答我的感情與人際模式。請用外在人格牌、內在靈魂牌、挑戰數與目前流年說明。");

        TextView boundary = host.text(
                "塔羅生命靈數用於娛樂與自我反思；流年表示主題循環，不代表特定事件一定發生。",
                11, MainActivity.MUTED, false);
        boundary.setLineSpacing(host.dp(2), 1f);
        panel.addView(boundary, host.marginTop(9));
    }
    String currentTarotYearSummary() {
        Object years = host.rendererFacts() == null ? null : host.rendererFacts().detail("personalYearTimeline");
        String current = host.rendererFacts() == null ? "" : host.rendererFacts().detailText("personalYearCalendarYear");
        if (years instanceof List) {
            for (Object raw : (List<?>) years) {
                if (!(raw instanceof Map)) continue;
                if (current.equals(host.mapValue(raw, "year"))) {
                    return host.mapValue(raw, "plainSummary");
                }
            }
        }
        return "";
    }
    void addTarotResultPanel() {
        if (host.rendererFacts() == null) return;

        LinearLayout panel = host.column();
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setPadding(host.dp(12), host.dp(14), host.dp(12), host.dp(14));
        GradientDrawable background = host.round(MainActivity.CARD_2, 18);
        background.setStroke(host.dp(1), Color.rgb(87, 69, 121));
        panel.setBackground(background);
        LinearLayout container = host.resultHostLayout();
        container.addView(panel, host.marginTop(6));

        TextView coreTitle = host.text("生日核心數", 12, MainActivity.GOLD, true);
        coreTitle.setGravity(Gravity.CENTER);
        panel.addView(coreTitle);

        TextView coreNumbers = host.text(
                "生命道路 " + host.rendererFacts().detailText("lifePathDisplay")
                        + "　·　生日數 " + host.rendererFacts().detailText("birthdayNumber")
                        + "\n態度數 " + host.rendererFacts().detailText("attitudeNumber"),
                15, MainActivity.TEXT, true);
        coreNumbers.setGravity(Gravity.CENTER);
        coreNumbers.setLineSpacing(host.dp(3), 1f);
        panel.addView(coreNumbers, host.marginTop(5));

        LinearLayout innerOuter = new LinearLayout(host);
        innerOuter.setOrientation(LinearLayout.HORIZONTAL);
        innerOuter.setGravity(Gravity.CENTER);

        host.addNumerologyIdentityCard(
                innerOuter,
                "內在靈魂牌",
                host.rendererFacts().detailText("soulCardNumber"),
                host.rendererFacts().detailText("soulCardName"));
        host.addNumerologyIdentityCard(
                innerOuter,
                "外在人格牌",
                host.rendererFacts().detailText("personalityCardNumber"),
                host.rendererFacts().detailText("personalityCardName"));
        panel.addView(innerOuter, host.marginTop(6));

        TextView contrast = host.text(
                "外在 " + host.rendererFacts().detailText("personalityCardNumber")
                        + " " + host.rendererFacts().detailText("personalityCardName")
                        + "　↔　內在 " + host.rendererFacts().detailText("soulCardNumber")
                        + " " + host.rendererFacts().detailText("soulCardName"),
                13, MainActivity.ACCENT, true);
        contrast.setGravity(Gravity.CENTER);
        panel.addView(contrast, host.marginTop(6));

        TextView card = host.text(host.rendererFacts().detailText("birthCardDisplay"), 27, MainActivity.TEXT, true);
        card.setGravity(Gravity.CENTER);
        panel.addView(card);

        TextView lifePath = host.text("生命靈數 " + host.rendererFacts().detailText("lifePathDisplay"),
                23, MainActivity.ACCENT, true);
        lifePath.setGravity(Gravity.CENTER);
        panel.addView(lifePath, host.marginTop(9));

        TextView core = host.text(
                "天賦數 " + host.rendererFacts().detailText("talentNumbers")
                        + "　·　態度數 " + host.rendererFacts().detailText("attitudeNumber")
                        + "\n" + host.rendererFacts().detailText("personalYearCalendarYear")
                        + " 流年 " + host.rendererFacts().detailText("personalYear")
                        + " " + host.rendererFacts().detailText("personalYearCardName")
                        + "　·　個人月 " + host.rendererFacts().detailText("personalMonth"),
                14, MainActivity.MUTED, false);
        core.setGravity(Gravity.CENTER);
        core.setLineSpacing(host.dp(3), 1f);
        panel.addView(core, host.marginTop(9));

        TextView contentFirstHint = host.text(
                "先看這些數字在生活裡代表什麼，再往下看完整計算資料；不需要先找老師翻譯。",
                12, MainActivity.MUTED, false);
        contentFirstHint.setGravity(Gravity.CENTER_HORIZONTAL);
        contentFirstHint.setLineSpacing(host.dp(2), 1f);
        panel.addView(contentFirstHint, host.marginTop(9));
        host.addPanelSection(panel, "內在 vs 外在", host.localReportSection("內在 vs 外在"));
        host.addPanelSection(panel, "性格與天賦", host.localReportSection("性格與天賦"));
        host.addPanelSection(panel, "工作與資源傾向", host.localReportSection("工作與財務"));

        host.addPanelSection(panel, "出生牌組 · 以下是依據", host.formatBirthCards());
        host.addPanelSection(panel, "四大巔峰", host.formatPairedLists(
                host.rendererFacts().detail("pinnacles"), host.rendererFacts().detail("pinnacleTiming")));
        host.addPanelSection(panel, "四大挑戰", host.formatList(host.rendererFacts().detail("challenges")));
        host.addPanelSection(panel, "人生三大週期", host.formatList(host.rendererFacts().detail("periodCycles")));
        host.addPanelSection(panel, "計算規則", host.formatList(host.rendererFacts().detail("calculationNotes")));

        TextView note = host.text(host.rendererFacts().detailText("note"), 11, MainActivity.MUTED, false);
        note.setGravity(Gravity.CENTER);
        note.setLineSpacing(host.dp(2), 1f);
        panel.addView(note, host.marginTop(6));
    }
}
