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


/** Result rendering for BaZi mode. */
final class BaZiResultRenderer {
    private final MainActivity host;

    BaZiResultRenderer(MainActivity host) {
        this.host = host;
    }
    void addBaZiOverviewTab(FortuneResult result, boolean aiLoading) {
        LinearLayout panel = host.resultPanel();

        host.addOverviewIdentity(panel, FortuneMode.BA_ZI);
        host.addOverviewTakeaways(panel, FortuneMode.BA_ZI);
        host.addOverviewTiming(panel, FortuneMode.BA_ZI);
        host.addOverviewNextFocus(panel);
        host.addOverviewPrimaryAction(panel, aiLoading);
    }

    void addBaZiLuckTimelineTab() {
        LinearLayout panel = host.resultPanel();

        TextView intro = host.text(
                "大運看十年級別的背景，流年看每一年如何落在這個背景上。點任一項可看完整十神、五行、合沖與主題依據。",
                13, MainActivity.MUTED, false);
        intro.setLineSpacing(host.dp(3), 1f);
        panel.addView(intro);

        host.addPanelSection(panel, "怎麼看",
                "大運＝約十年的背景；流年＝某一年的放大鏡。\n"
                        + "先看「白話」知道主題，再看十神與合沖了解依據。"
                        + " 有財星不等於一定賺錢、有沖也不等於一定出事。");

        host.addPanelSection(panel, "目前大運", host.summaryLuck(host.rendererFacts().detail("currentLuckPillar")));
        host.addYearHighlights(panel);

        TextView luckTitle = host.text("大運時間軸", 13, MainActivity.GOLD, true);
        panel.addView(luckTitle, host.marginTop(11));

        Object luckRaw = host.rendererFacts().detail("luckPillars");
        String currentGanZhi = host.mapValue(host.rendererFacts().detail("currentLuckPillar"), "ganZhi");
        if (luckRaw instanceof List) {
            for (Object raw : (List<?>) luckRaw) {
                if (!(raw instanceof Map)) continue;
                final Map<?, ?> item = (Map<?, ?>) raw;
                String gz = host.mapValue(item, "ganZhi");
                String title = (gz.equals(currentGanZhi) ? "● " : "○ ")
                        + gz + "　" + host.mapValue(item, "startYear")
                        + "–" + host.mapValue(item, "endYear");
                String summary = host.mapValue(item, "plainSummary")
                        + "\n十神 " + host.mapValue(item, "stemTenGod")
                        + "（" + host.mapValue(item, "stemTenGodMeaning") + "）"
                        + "　·　五行 " + host.mapValue(item, "element")
                        + "\n主題 " + host.compactValue(item.get("themes"));
                host.addClickableFactCard(
                        panel,
                        title,
                        summary,
                        () -> host.showFactDetailDialog("大運 " + gz, item),
                        "請直接解釋 " + gz
                                + " 大運對我代表什麼？請用這柱大運與本命 deterministic facts 說明。");
            }
        }

        TextView yearTitle = host.text(
                "逐年流年 · " + host.rendererFacts().detailText("annualTimelineStartYear")
                        + "–" + host.rendererFacts().detailText("annualTimelineEndYear"),
                13, MainActivity.GOLD, true);
        panel.addView(yearTitle, host.marginTop(22));

        Object yearsRaw = host.rendererFacts().detail("annualTimeline");
        if (yearsRaw instanceof List) {
            for (Object raw : (List<?>) yearsRaw) {
                if (!(raw instanceof Map)) continue;
                final Map<?, ?> year = (Map<?, ?>) raw;
                String title = host.mapValue(year, "year") + "　"
                        + host.mapValue(year, "ganZhi")
                        + "　" + host.mapValue(year, "stemTenGod");
                String summary = host.mapValue(year, "plainSummary")
                        + "\n大運 " + host.mapValue(year, "luckPillar")
                        + "　·　十神 " + host.mapValue(year, "stemTenGod")
                        + "（" + host.mapValue(year, "stemTenGodMeaning") + "）"
                        + "\n" + host.firstMeaningfulInteraction(year.get("natalInteractions"));
                host.addClickableFactCard(
                        panel,
                        title,
                        summary,
                        () -> host.showFactDetailDialog(
                                host.mapValue(year, "year") + " 流年", year),
                        "請直接回答 " + host.mapValue(year, "year")
                                + " 年對我代表什麼？請用這一年的 deterministic facts 與所屬大運說明。");
            }
        }

        TextView convention = host.text(
                host.rendererFacts().detailText("annualTimelineConvention"),
                11, MainActivity.MUTED, false);
        panel.addView(convention, host.marginTop(9));
    }
    void addBaZiTopicAnalysisTab() {
        LinearLayout panel = host.resultPanel();

        TextView intro = host.text(
                "先看工作、財運、感情對你代表什麼；命盤術語只放在後面的「為什麼這樣說」。",
                13, MainActivity.MUTED, false);
        intro.setLineSpacing(host.dp(3), 1f);
        panel.addView(intro);

        addWealthTopic(panel);
        addCareerTopic(panel);
        addRelationshipTopic(panel);
    }
    void addWealthTopic(LinearLayout panel) {
        Object p = host.rendererFacts().detail("wealthProfile");
        LinearLayout card = host.topicCard(panel, "財運", "先看白話重點，需要時再看命盤依據");
        host.addTopicLine(card, "先說重點",
                host.rendererCopy() != null && !host.rendererCopy().wealth.isEmpty()
                        ? FortuneResultTabCopy.compactInterpretation(host.rendererCopy().wealth, "")
                        : FortuneResultTabCopy.compactInterpretation("", host.localReportSection("工作與財務")));
        host.addTopicLine(card, "為什麼這樣說",
                "財星五行 " + host.mapValue(p, "wealthElement")
                        + "　·　正財 " + host.mapValue(p, "directWealthCount")
                        + "　·　偏財 " + host.mapValue(p, "indirectWealthCount"));
        host.addTopicLine(card, "命盤依據", host.compactValue(host.mapObjectValue(p, "natalEvidence")));
        host.addTopicLine(card, "目前階段", host.summaryLuck(host.mapObjectValue(p, "currentLuck")));
        host.addTopicLine(card, "值得留意",
                "財星訊號年份：" + host.compactValue(host.mapObjectValue(p, "annualSignalYears")));
        host.addTopicBoundary(card, host.mapValue(p, "evidenceRule"));
        host.addAskTeacherAction(
                card,
                "用語音追問財運",
                "請直接回答我的財運重點。請從 wealthProfile、目前大運與逐年流年挑最重要的依據，不要重新排盤。");
    }
    void addCareerTopic(LinearLayout panel) {
        Object p = host.rendererFacts().detail("careerProfile");
        LinearLayout card = host.topicCard(panel, "工作", "先看白話重點，需要時再看命盤依據");
        host.addTopicLine(card, "先說重點",
                host.rendererCopy() != null && !host.rendererCopy().career.isEmpty()
                        ? FortuneResultTabCopy.compactInterpretation(host.rendererCopy().career, "")
                        : host.localReportSection("工作與財務"));
        host.addTopicLine(card, "為什麼這樣說",
                "官殺 " + host.mapValue(p, "officerCount")
                        + "　·　印 " + host.mapValue(p, "resourceCount")
                        + "　·　食傷 " + host.mapValue(p, "outputCount"));
        host.addTopicLine(card, "命盤依據", host.compactValue(host.mapObjectValue(p, "natalEvidence")));
        host.addTopicLine(card, "目前階段", host.summaryLuck(host.mapObjectValue(p, "currentLuck")));
        host.addTopicLine(card, "值得留意",
                "工作訊號年份：" + host.compactValue(host.mapObjectValue(p, "annualSignalYears")));
        host.addTopicBoundary(card, host.mapValue(p, "evidenceRule"));
        host.addAskTeacherAction(
                card,
                "用語音追問工作",
                "請直接回答我的工作與職涯重點。請從 careerProfile、目前大運與逐年流年挑最重要的依據，不要重新排盤。");
    }
    void addRelationshipTopic(LinearLayout panel) {
        Object p = host.rendererFacts().detail("relationshipProfile");
        LinearLayout card = host.topicCard(panel, "感情", "先看白話重點，需要時再看命盤依據");
        host.addTopicLine(card, "先說重點",
                host.rendererCopy() != null && !host.rendererCopy().relationships.isEmpty()
                        ? FortuneResultTabCopy.compactInterpretation(host.rendererCopy().relationships, "")
                        : host.localReportSection("感情與人際"));
        host.addTopicLine(card, "為什麼這樣說",
                "配偶宮 " + host.mapValue(p, "spousePalace")
                        + "　·　藏干十神 " + host.compactValue(host.mapObjectValue(p, "spousePalaceTenGods")));
        host.addTopicLine(card, "命盤依據",
                "常見財官約定 " + host.compactValue(host.mapObjectValue(p, "partnerGodConvention"))
                        + "　·　本命數量 " + host.mapValue(p, "partnerGodCount"));
        host.addTopicLine(card, "值得留意",
                "配偶宮／財官訊號年份：" + host.compactRelationshipYears(
                        host.mapObjectValue(p, "annualSignalYears")));
        host.addTopicBoundary(card, host.mapValue(p, "evidenceRule"));
        host.addAskTeacherAction(
                card,
                "用語音追問感情",
                "請直接回答我的感情與人際盲點。請從 relationshipProfile、配偶宮與逐年流年挑最重要的依據，不要重新排盤。");
    }
    void addBaZiResultPanel() {
        if (host.rendererFacts() == null) return;

        LinearLayout panel = host.column();
        panel.setPadding(host.dp(10), host.dp(11), host.dp(10), host.dp(11));
        panel.setBackground(host.round(MainActivity.CARD_2, 16));
        LinearLayout container = host.resultHostLayout();
        container.addView(panel, host.marginTop(6));

        LinearLayout hero = new LinearLayout(host);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout master = host.column();
        TextView masterLabel = host.text("日主", 12, MainActivity.GOLD, true);
        TextView masterValue = host.text(
                host.rendererFacts().detailText("dayMaster") + host.rendererFacts().detailText("dayMasterElement"),
                28, MainActivity.TEXT, true);
        master.addView(masterLabel);
        master.addView(masterValue, host.marginTop(2));
        hero.addView(master, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout balance = host.column();
        TextView balanceLabel = host.text("日主強弱", 12, MainActivity.MUTED, true);
        balanceLabel.setGravity(Gravity.END);
        TextView balanceValue = host.text(host.rendererFacts().detailText("dayMasterStrength")
                + " · " + host.rendererFacts().detailText("strengthIndex"), 20, MainActivity.ACCENT, true);
        balanceValue.setGravity(Gravity.END);
        balance.addView(balanceLabel);
        balance.addView(balanceValue, host.marginTop(2));
        hero.addView(balance, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        panel.addView(hero);

        TextView contentFirstHint = host.text(
                "先看白話，再看命盤依據。下面三段不需要開語音，也能先理解這張八字跟日常生活最有關的部分。",
                12, MainActivity.MUTED, false);
        contentFirstHint.setLineSpacing(host.dp(2), 1f);
        panel.addView(contentFirstHint, host.marginTop(9));
        host.addPanelSection(panel, "性格與做事方式", host.localReportSection("性格與天賦"));
        host.addPanelSection(panel, "工作與資源傾向", host.localReportSection("工作與財務"));
        host.addPanelSection(panel, "關係模式", host.localReportSection("感情與人際"));

        TextView divider = host.text("四柱命盤 · 以下是依據", 12, MainActivity.GOLD, true);
        panel.addView(divider, host.marginTop(11));

        LinearLayout pillars = new LinearLayout(host);
        pillars.setOrientation(LinearLayout.HORIZONTAL);
        pillars.setGravity(Gravity.CENTER);
        host.addPillarCard(pillars, "年柱", host.rendererFacts().detailText("yearPillar"),
                host.mapValue(host.rendererFacts().detail("hiddenStems"), "year"),
                host.mapValue(host.rendererFacts().detail("tenGods"), "yearStem"));
        host.addPillarCard(pillars, "月柱", host.rendererFacts().detailText("monthPillar"),
                host.mapValue(host.rendererFacts().detail("hiddenStems"), "month"),
                host.mapValue(host.rendererFacts().detail("tenGods"), "monthStem"));
        host.addPillarCard(pillars, "日柱", host.rendererFacts().detailText("dayPillar"),
                host.mapValue(host.rendererFacts().detail("hiddenStems"), "day"),
                "日主");
        host.addPillarCard(pillars, "時柱", host.rendererFacts().detailText("timePillar"),
                host.mapValue(host.rendererFacts().detail("hiddenStems"), "time"),
                host.mapValue(host.rendererFacts().detail("tenGods"), "timeStem"));
        panel.addView(pillars, host.marginTop(8));

        TextView elementsTitle = host.text("可見五行", 12, MainActivity.GOLD, true);
        panel.addView(elementsTitle, host.marginTop(11));

        Object visible = host.rendererFacts().detail("visibleFiveElements");
        host.addElementBar(panel, "木", host.intMapValue(visible, "木"));
        host.addElementBar(panel, "火", host.intMapValue(visible, "火"));
        host.addElementBar(panel, "土", host.intMapValue(visible, "土"));
        host.addElementBar(panel, "金", host.intMapValue(visible, "金"));
        host.addElementBar(panel, "水", host.intMapValue(visible, "水"));

        TextView trend = host.text(
                "較多：" + host.rendererFacts().detailText("strongestVisibleElement")
                        + "　較少：" + host.rendererFacts().detailText("weakestVisibleElement"),
                13, MainActivity.MUTED, false);
        panel.addView(trend, host.marginTop(8));

        TextView tenGodsTitle = host.text("十神摘要", 12, MainActivity.GOLD, true);
        panel.addView(tenGodsTitle, host.marginTop(11));
        TextView tenGods = host.text(host.formatTenGods(), 14, MainActivity.TEXT, false);
        tenGods.setLineSpacing(host.dp(3), 1f);
        panel.addView(tenGods, host.marginTop(6));

        TextView tenDistTitle = host.text("十神分布", 12, MainActivity.GOLD, true);
        panel.addView(tenDistTitle, host.marginTop(11));
        TextView tenDist = host.text(host.formatMap(host.rendererFacts().detail("tenGodDistribution")), 13, MainActivity.TEXT, false);
        tenDist.setLineSpacing(host.dp(2), 1f);
        panel.addView(tenDist, host.marginTop(6));

        TextView strengthTitle = host.text("旺衰與平衡", 12, MainActivity.GOLD, true);
        panel.addView(strengthTitle, host.marginTop(11));
        TextView strength = host.text(
                host.rendererFacts().detailText("dayMasterStrength")
                        + "｜扶身比 " + host.rendererFacts().detailText("strengthIndex") + "/100"
                        + "\n平衡參考元素：" + host.rendererFacts().detailText("balancingElements")
                        + "\n" + host.rendererFacts().detailText("strengthMethod"),
                13, MainActivity.TEXT, false);
        strength.setLineSpacing(host.dp(3), 1f);
        panel.addView(strength, host.marginTop(6));

        TextView interactionTitle = host.text("命局合沖刑害", 12, MainActivity.GOLD, true);
        panel.addView(interactionTitle, host.marginTop(11));
        TextView interactions = host.text(host.formatList(host.rendererFacts().detail("natalInteractions")), 13, MainActivity.TEXT, false);
        interactions.setLineSpacing(host.dp(3), 1f);
        panel.addView(interactions, host.marginTop(6));

        TextView luckTitle = host.text("大運", 12, MainActivity.GOLD, true);
        panel.addView(luckTitle, host.marginTop(11));
        TextView luck = host.text(host.formatLuckPillars(), 13, MainActivity.TEXT, false);
        luck.setLineSpacing(host.dp(3), 1f);
        panel.addView(luck, host.marginTop(6));

        TextView annualTitle = host.text("今年流年", 12, MainActivity.GOLD, true);
        panel.addView(annualTitle, host.marginTop(11));
        TextView annual = host.text(host.formatCurrentAnnual(), 13, MainActivity.TEXT, false);
        annual.setLineSpacing(host.dp(3), 1f);
        panel.addView(annual, host.marginTop(6));

        host.addPanelSection(panel, "加權五行", host.formatMap(host.rendererFacts().detail("weightedFiveElements")));
        host.addPanelSection(panel, "納音", host.formatList(host.rendererFacts().detail("naYin")));
        host.addPanelSection(panel, "十二長生", host.formatList(host.rendererFacts().detail("lifeStages")));
        host.addPanelSection(panel, "命宮／身宮",
                "命宮 " + host.rendererFacts().detailText("mingGong")
                        + "　·　身宮 " + host.rendererFacts().detailText("shenGong"));

        TextView convention = host.text(
                "排盤規則｜" + host.rendererFacts().detailText("timeConvention"),
                11, MainActivity.MUTED, false);
        convention.setLineSpacing(host.dp(2), 1f);
        panel.addView(convention, host.marginTop(6));
    }
}
