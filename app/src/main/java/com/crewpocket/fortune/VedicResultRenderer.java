package com.crewpocket.fortune;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Owns Vedic result-tab rendering while MainActivity remains the UI host.
 * Calculation and state ownership stay outside this renderer.
 */
final class VedicResultRenderer {
    private final MainActivity host;

    VedicResultRenderer(MainActivity host) {
        this.host = host;
    }
    void addVedicOverviewTab(FortuneResult result, boolean aiLoading) {
        LinearLayout panel = host.resultPanel();

        LinearLayout hero = new LinearLayout(host);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout lagna = host.column();
        lagna.addView(host.text("Lagna", 11, MainActivity.GOLD, true));
        lagna.addView(host.text(
                host.rendererFacts().detailText("lagnaSign"),
                22,
                MainActivity.TEXT,
                true), host.marginTop(3));
        lagna.addView(host.text(
                host.rendererFacts().detailText("lagnaNakshatra")
                        + " · Pada " + host.rendererFacts().detailText("lagnaPada"),
                11,
                MainActivity.MUTED,
                false), host.marginTop(2));
        hero.addView(lagna, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout moon = host.column();
        TextView moonLabel = host.text("Moon", 11, MainActivity.GOLD, true);
        moonLabel.setGravity(Gravity.END);
        moon.addView(moonLabel);
        TextView moonSign = host.text(
                host.rendererFacts().detailText("moonSign"),
                22,
                MainActivity.ACCENT,
                true);
        moonSign.setGravity(Gravity.END);
        moon.addView(moonSign, host.marginTop(3));
        TextView moonNakshatra = host.text(
                host.rendererFacts().detailText("moonNakshatra")
                        + " · Pada " + host.rendererFacts().detailText("moonPada"),
                11,
                MainActivity.MUTED,
                false);
        moonNakshatra.setGravity(Gravity.END);
        moon.addView(moonNakshatra, host.marginTop(2));
        hero.addView(moon, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        panel.addView(hero);

        host.addOverviewTakeaways(panel, FortuneMode.VEDIC_ASTROLOGY);
        host.addOverviewTiming(panel, FortuneMode.VEDIC_ASTROLOGY);

        String summary;
        if (aiLoading) {
            summary = host.localReportSection("核心總覽");
        } else if (host.rendererCopy() != null
                && !host.rendererCopy().overview.isEmpty()) {
            summary = host.rendererCopy().overview;
        } else {
            summary = host.localReportSection("核心總覽");
        }
        summary = FortuneResultTabCopy.compactInterpretation(summary, "");
        if (!summary.isEmpty()) {
            host.addPanelSection(panel, "一句總結", summary);
        }

        TextView next = host.text(
                "想看行星、宮位與 Nakshatra →「本命」；想看 Dasha／Gochar →「流年」。",
                11,
                MainActivity.MUTED,
                false);
        next.setLineSpacing(host.dp(2), 1f);
        panel.addView(next, host.marginTop(9));

        TextView rule = host.text(
                "Sidereal · Lahiri · Whole Sign · Mean Rahu/Ketu",
                10,
                MainActivity.MUTED,
                false);
        panel.addView(rule, host.marginTop(5));
    }
    void addVedicNatalTab() {
        LinearLayout panel = host.resultPanel();

        TextView intro = host.text(
                "這一頁是你的「出生底盤」：看你天生比較容易把力氣放在哪裡、怎麼思考、怎麼做事，以及不同人生主題的基本結構。"
                        + "下面的星座、宮位、Nakshatra 與宮主是原始命盤資料；不熟印度占星時，不需要自己逐條翻譯。",
                13, MainActivity.MUTED, false);
        intro.setLineSpacing(host.dp(3), 1f);
        panel.addView(intro);

        host.addPanelSection(panel, "Lagna / Moon / Sun",
                VedicFactsFormatter.coreSummary(host.rendererFacts()));

        String natalPlain = host.rendererCopy() != null && !host.rendererCopy().personality.isEmpty()
                ? host.rendererCopy().personality
                : VedicFactsFormatter.profileEvidence(host.rendererFacts(), "personalityProfile");
        if (!natalPlain.isEmpty()) {
            host.addPanelSection(panel, "本命白話重點", natalPlain);
        }
        if (host.rendererCopy() != null && !host.rendererCopy().topTraits.isEmpty()) {
            host.addTopTraits(panel);
        }

        addVedicPageTeacherGuide(
                panel,
                "想用語音聽白話版？",
                "上面的本命內容已經可以直接讀。語音老師會再挑 3–5 個最重要的結構，用比較口語的方式把個性、工作方式與關係模式串起來。",
                "聽老師講本命",
                "請把我的本命頁講成人話。不要逐條念資料，也不要先講流年。"
                        + "請從 Lagna、Moon、Sun、Lagna lord、最重要的 house lord placements、行星落宮、Nakshatra、dignity、retrograde、Drishti/Conjunctions 中挑 3 到 5 個最關鍵的結構。"
                        + "先說結論，再說每個結論的 deterministic evidence，最後告訴我這些結構在個性、工作方式與關係模式上怎麼彼此連動。");

        TextView chartTitle = host.text("本命盤 · Whole Sign", 13, MainActivity.GOLD, true);
        panel.addView(chartTitle, host.marginTop(14));
        TextView chartHint = host.text(
                "這張圖是本命盤位置圖。H1 是你的上升起點；其他宮位代表工作、財務、關係、家庭等不同人生領域。",
                11, MainActivity.MUTED, false);
        chartHint.setLineSpacing(host.dp(2), 1f);
        panel.addView(chartHint, host.marginTop(3));
        VedicNatalChartView natalChart = new VedicNatalChartView(host, host.rendererFacts());
        panel.addView(natalChart, host.marginTop(6));

        TextView planetsTitle = host.text("九曜 · 行星落點", 13, MainActivity.GOLD, true);
        panel.addView(planetsTitle, host.marginTop(14));
        Object planetsRaw = host.rendererFacts().detail("planets");
        if (planetsRaw instanceof List) {
            for (Object raw : (List<?>) planetsRaw) {
                if (!(raw instanceof Map)) continue;
                final Map<?, ?> planet = (Map<?, ?>) raw;
                String title = host.mapValue(planet, "name")
                        + " · " + host.mapValue(planet, "sign")
                        + " · H" + host.mapValue(planet, "house");
                String summary = String.format(
                        java.util.Locale.US,
                        "%.2f° sidereal",
                        host.numberValue(planet.get("siderealLongitude")))
                        + " · " + host.mapValue(planet, "nakshatra")
                        + " P" + host.mapValue(planet, "pada")
                        + (Boolean.TRUE.equals(planet.get("retrograde")) ? " · R" : "")
                        + " · " + host.mapValue(planet, "dignity");
                host.addClickableFactCard(
                        panel,
                        title,
                        summary,
                        () -> host.showFactDetailDialog(
                                host.mapValue(planet, "name") + " deterministic facts", planet),
                        "請只用 deterministic facts 解釋 "
                                + host.mapValue(planet, "name")
                                + " 的 sign、house、Nakshatra、dignity 與它所主宮位對我代表什麼。");
            }
        }

        TextView housesTitle = host.text("12 宮 · 人生領域", 13, MainActivity.GOLD, true);
        panel.addView(housesTitle, host.marginTop(18));
        Object housesRaw = host.rendererFacts().detail("houses");
        if (housesRaw instanceof List) {
            for (Object raw : (List<?>) housesRaw) {
                if (!(raw instanceof Map)) continue;
                final Map<?, ?> house = (Map<?, ?>) raw;
                String title = "H" + host.mapValue(house, "house")
                        + " · " + host.mapValue(house, "sign")
                        + " · lord " + host.mapValue(house, "lord");
                String summary = "宮內：" + host.compactValue(house.get("planets"));
                host.addClickableFactCard(
                        panel,
                        title,
                        summary,
                        () -> host.showFactDetailDialog(
                                "House " + host.mapValue(house, "house"), house),
                        "請只用 deterministic facts 解釋第 "
                                + host.mapValue(house, "house")
                                + " 宮、宮主 " + host.mapValue(house, "lord")
                                + " 與宮內行星的結構。");
            }
        }

        host.addPanelSection(
                panel,
                "12 宮主跑到哪裡",
                VedicFactsFormatter.houseLordPlacements(host.rendererFacts()));

        String aspects = VedicFactsFormatter.aspects(host.rendererFacts());
        host.addPanelSection(panel, "行星之間的影響 · Drishti / Conjunctions",
                aspects.isEmpty() ? "目前沒有符合 v1 規則的合相；Drishti 仍保存在 deterministic facts。" : aspects);

        TextView boundary = host.text(
                "v1 不計 D9、Yoga、Shadbala、Ashtakavarga；Rahu/Ketu 不套用有爭議的特殊 Drishti 或 dignity。",
                11, MainActivity.MUTED, false);
        boundary.setLineSpacing(host.dp(2), 1f);
        panel.addView(boundary, host.marginTop(9));
    }
    void addVedicDashaTimelineTab() {
        LinearLayout panel = host.resultPanel();

        TextView intro = host.text(
                "這一頁在看「現在走到人生哪一段，以及最近哪些主題比較容易被碰到」。"
                        + "Dasha 像人生目前的大章節，Antardasha 是章節裡的小段落；Gochar 則是現在天空中的行星正在碰你本命的哪些位置。",
                13, MainActivity.MUTED, false);
        intro.setLineSpacing(host.dp(3), 1f);
        panel.addView(intro);

        host.addPanelSection(panel, "目前人生週期 · Mahadasha / Antardasha",
                VedicFactsFormatter.dashaSummary(host.rendererFacts()));

        if (host.rendererCopy() != null && !host.rendererCopy().currentCycle.isEmpty()) {
            host.addPanelSection(panel, "現在的白話解讀", host.rendererCopy().currentCycle);
        }

        host.addPanelSection(
                panel,
                (host.rendererTransitDate() == null ? "目前 Gochar · " : "查看 Gochar · ")
                        + host.rendererFacts().detailText("currentTransitDate"),
                VedicFactsFormatter.currentGochar(host.rendererFacts()));
        String gocharHighlights = VedicFactsFormatter.gocharHighlights(host.rendererFacts());
        if (!gocharHighlights.isEmpty()) {
            host.addPanelSection(panel, "Gochar × 本命重點", gocharHighlights);
        }

        LinearLayout gocharActions = new LinearLayout(host);
        gocharActions.setOrientation(LinearLayout.HORIZONTAL);
        Button pickTransitDate = host.secondaryButton("選擇 Gochar 日期");
        pickTransitDate.setOnClickListener(v -> host.showVedicTransitDatePicker());
        LinearLayout.LayoutParams pickLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        gocharActions.addView(pickTransitDate, pickLp);
        if (host.rendererTransitDate() != null) {
            Button today = host.secondaryButton("回到今天");
            today.setOnClickListener(v -> host.applyVedicTransitDate(null));
            LinearLayout.LayoutParams todayLp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            todayLp.leftMargin = host.dp(6);
            gocharActions.addView(today, todayLp);
        }
        panel.addView(gocharActions, host.marginTop(8));

        TextView gocharRule = host.text(
                "Gochar 使用所選日期的 Lahiri sidereal 行星位置，宮位以本命 Lagna 的 Whole Sign Houses 計算。",
                11, MainActivity.MUTED, false);
        gocharRule.setLineSpacing(host.dp(2), 1f);
        panel.addView(gocharRule, host.marginTop(7));

        host.addPanelSection(
                panel,
                "目前週期碰上現在行運 · Dasha × Gochar",
                VedicFactsFormatter.dashaSummary(host.rendererFacts())
                        + (gocharHighlights.isEmpty()
                        ? ""
                        : "\n\n" + gocharHighlights));

        String majorTimeline = VedicFactsFormatter.majorTransitTimeline(host.rendererFacts());
        if (!majorTimeline.isEmpty()) {
            host.addPanelSection(
                    panel,
                    "未來 3 年主要行運變化",
                    majorTimeline);
            TextView timelineHint = host.text(
                    "只列 Jupiter、Saturn、Rahu、Ketu 的換星座／換本命宮事件，減少快行星造成的雜訊。",
                    11, MainActivity.MUTED, false);
            timelineHint.setLineSpacing(host.dp(2), 1f);
            panel.addView(timelineHint, host.marginTop(5));
        }

        if (host.rendererCopy() != null && !host.rendererCopy().keyYears.isEmpty()) {
            host.addPanelSection(panel, "接下來值得先看的時期", host.rendererCopy().keyYears);
        }

        addVedicPageTeacherGuide(
                panel,
                "想用語音整理這段時間？",
                "上面的週期、行運與未來變化都已經寫在頁面裡。語音老師只會把它們濃縮成 3–5 個最有感的時間重點，方便你直接問下去。",
                "聽老師講目前週期",
                "請把我的流年頁講成人話。請以目前選擇的 currentTransitDate 為基準，"
                        + "先說 currentMahadasha/currentAntardasha 代表的人生背景，再疊加 currentTransits、transitAspectsToNatal、transitConjunctionsToNatal。"
                        + "只挑 3 到 5 個目前最值得注意的時間訊號，分清楚哪些來自 Dasha、哪些來自 Gochar。"
                        + "如果談未來，只能引用 majorTransitTimeline 與既有 Dasha 日期；不要自行補沒有計算的 transit，也不要把任何訊號說成必然事件。");

        TextView visualTitle = host.text("人生大週期時間軸 · Dasha", 13, MainActivity.GOLD, true);
        panel.addView(visualTitle, host.marginTop(14));
        TextView visualHint = host.text(
                "長條代表完整 Mahadasha 序列；紫色高亮目前週期，金線代表今天。",
                11, MainActivity.MUTED, false);
        panel.addView(visualHint, host.marginTop(3));
        VedicDashaTimelineView dashaTimeline =
                new VedicDashaTimelineView(host, host.rendererFacts());
        panel.addView(dashaTimeline, host.marginTop(5));

        host.addYearHighlights(panel);

        TextView mdTitle = host.text("Mahadasha timeline", 13, MainActivity.GOLD, true);
        panel.addView(mdTitle, host.marginTop(18));
        Object raw = host.rendererFacts().detail("mahadashaTimeline");
        String currentLord = host.mapValue(host.rendererFacts().detail("currentMahadasha"), "lord");
        if (raw instanceof List) {
            for (Object itemRaw : (List<?>) raw) {
                if (!(itemRaw instanceof Map)) continue;
                final Map<?, ?> item = (Map<?, ?>) itemRaw;
                String lord = host.mapValue(item, "lord");
                String title = (lord.equals(currentLord) ? "● " : "○ ")
                        + lord + " Mahadasha";
                String summary = host.mapValue(item, "startDate")
                        + " → " + host.mapValue(item, "endDate")
                        + " · age " + host.mapValue(item, "startAge")
                        + "–" + host.mapValue(item, "endAge");
                host.addClickableFactCard(
                        panel,
                        title,
                        summary,
                        () -> host.showFactDetailDialog(lord + " Mahadasha", item),
                        "請直接解釋 " + lord
                                + " Mahadasha 對我代表什麼，只引用本命 planets、houses、houseLords 與這段 Dasha facts。");
            }
        }

        TextView adTitle = host.text("目前 Mahadasha 的 Antardasha", 13, MainActivity.GOLD, true);
        panel.addView(adTitle, host.marginTop(18));
        String adTimeline = VedicFactsFormatter.antardashaTimeline(host.rendererFacts());
        host.addPanelSection(panel, "時間軸",
                adTimeline.isEmpty() ? "目前 Mahadasha 不在已產生的時間範圍內。" : adTimeline);

        TextView convention = host.text(
                host.rendererFacts().detailText("vimshottariConvention"),
                11, MainActivity.MUTED, false);
        convention.setLineSpacing(host.dp(2), 1f);
        panel.addView(convention, host.marginTop(9));
    }
    void addVedicPageTeacherGuide(
            LinearLayout panel,
            String title,
            String body,
            String buttonLabel,
            String question) {
        LinearLayout guide = host.column();
        guide.setPadding(host.dp(11), host.dp(11), host.dp(11), host.dp(12));
        guide.setBackground(host.roundBorder(
                Color.rgb(47, 36, 72),
                Color.rgb(105, 84, 146),
                16,
                1));

        TextView titleView = host.text(title, 14, MainActivity.TEXT, true);
        guide.addView(titleView);

        TextView bodyView = host.text(body, 12, MainActivity.MUTED, false);
        bodyView.setLineSpacing(host.dp(2), 1f);
        guide.addView(bodyView, host.marginTop(4));

        host.addAskTeacherAction(guide, buttonLabel, question);
        panel.addView(guide, host.marginTop(9));
    }
    void addVedicTopicAnalysisTab() {
        LinearLayout panel = host.resultPanel();

        TextView intro = host.text(
                "不做吉凶分數。每個主題都先給「怎麼看 → 關鍵資料 → 目前時間 → 白話解讀」，語音老師只負責補充與追問。",
                13, MainActivity.MUTED, false);
        intro.setLineSpacing(host.dp(3), 1f);
        panel.addView(intro);

        addVedicTopicCard(
                panel,
                "個性 / 天賦",
                "Lagna × Moon × Sun × Lagna lord",
                "personalityProfile",
                host.rendererCopy() == null ? "" : host.rendererCopy().personality,
                "請直接講我的個性、優勢與盲點。只用 personalityProfile、Lagna、Moon、Sun、house lords 與本命 deterministic facts。");

        addVedicTopicCard(
                panel,
                "工作 / Career",
                "10宮 × 10宮主 × Saturn/Jupiter × Dasha",
                "careerProfile",
                host.rendererCopy() == null ? "" : host.rendererCopy().career,
                "請直接講我的工作方向與目前職涯節奏。只用 careerProfile、10宮、10宮主、Saturn/Jupiter、目前 Dasha 與 currentTransits。");

        addVedicTopicCard(
                panel,
                "財務 / Wealth",
                "2宮 × 11宮 × Jupiter/Venus × Dasha",
                "wealthProfile",
                host.rendererCopy() == null ? "" : host.rendererCopy().wealth,
                "請直接講我的財務與資源節奏。只用 wealthProfile、2宮、11宮及宮主、Jupiter/Venus、目前 Dasha 與 currentTransits，不做投資預測。");

        addVedicTopicCard(
                panel,
                "感情 / Relationships",
                "7宮 × 7宮主 × Venus × Dasha",
                "relationshipProfile",
                host.rendererCopy() == null ? "" : host.rendererCopy().relationships,
                "請直接講我的感情與關係模式。只用 relationshipProfile、7宮、7宮主、Venus、目前 Dasha 與 currentTransits，不把訊號說成必然事件。");

        addVedicTopicCard(
                panel,
                "家庭 / Children",
                "4宮 × 5宮 × Moon/Jupiter",
                "familyChildrenProfile",
                host.rendererCopy() == null ? "" : host.rendererCopy().family,
                "請只用 familyChildrenProfile、4宮、5宮及宮主、Moon/Jupiter 說明家庭與子女主題。不要預測懷孕必然結果。");

        TextView boundary = host.text(
                "娛樂與自我反思用途。工作、財務、感情與家庭可以談結構與節奏；不做死亡、嚴重疾病、懷孕必然、犯罪或災難斷言。",
                11, MainActivity.MUTED, false);
        boundary.setLineSpacing(host.dp(2), 1f);
        panel.addView(boundary, host.marginTop(9));
    }
    void addVedicTopicCard(
            LinearLayout panel,
            String title,
            String subtitle,
            String profileKey,
            String aiText,
            String question) {
        LinearLayout card = host.topicCard(panel, title, subtitle);
        Object raw = host.rendererFacts().detail(profileKey);
        String rule = host.mapValue(raw, "rule");
        Object evidence = raw instanceof Map ? ((Map<?, ?>) raw).get("evidence") : null;

        host.addTopicLine(card, "怎麼看", rule);

        String timingEvidence =
                VedicFactsFormatter.topicTimingEvidence(host.rendererFacts(), profileKey);
        addVedicEvidenceCardGrid(card, evidence, timingEvidence);

        host.addTopicLine(card, "白話解讀",
                FortuneResultTabCopy.compactInterpretation(
                        aiText,
                        "上面的本命與時間資料已經可以直接看；完整文字解讀會把這些訊號彼此之間的關係講清楚。"));
        host.addAskTeacherAction(card, "用語音追問這一題", question);
    }
    void addVedicEvidenceCardGrid(
            LinearLayout card,
            Object natalEvidence,
            String timingEvidence) {
        List<String> items = new ArrayList<String>();

        if (natalEvidence instanceof List) {
            for (Object value : (List<?>) natalEvidence) {
                String item = String.valueOf(value == null ? "" : value).trim();
                if (!item.isEmpty()) items.add(item);
                if (items.size() >= 4) break;
            }
        }

        if (timingEvidence != null && !timingEvidence.trim().isEmpty()) {
            String[] timingLines = timingEvidence.split("\\n");
            for (String line : timingLines) {
                String item = line == null ? "" : line.trim();
                if (item.isEmpty()) continue;
                items.add(item);
                if (items.size() >= 6) break;
            }
        }

        if (items.isEmpty()) return;

        TextView sectionTitle = host.text("關鍵資料", 11, MainActivity.GOLD, true);
        card.addView(sectionTitle, host.marginTop(10));

        for (int i = 0; i < items.size(); i += 2) {
            LinearLayout row = new LinearLayout(host);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.TOP);

            addVedicEvidenceMiniCard(row, items.get(i), false);
            if (i + 1 < items.size()) {
                addVedicEvidenceMiniCard(row, items.get(i + 1), true);
            } else {
                View spacer = new View(host);
                LinearLayout.LayoutParams spacerLp =
                        new LinearLayout.LayoutParams(0, 1, 1f);
                spacerLp.leftMargin = host.dp(6);
                row.addView(spacer, spacerLp);
            }
            card.addView(row, host.marginTop(6));
        }

        if (natalEvidence instanceof List && ((List<?>) natalEvidence).size() > 4) {
            host.addTopicLine(card, "完整本命依據", host.compactValue(natalEvidence));
        }
        if (timingEvidence != null && timingEvidence.split("\\n").length > 2) {
            host.addTopicLine(card, "完整目前時間", timingEvidence);
        }
    }
    void addVedicEvidenceMiniCard(
            LinearLayout row,
            String rawValue,
            boolean withLeftMargin) {
        LinearLayout mini = host.column();
        mini.setPadding(host.dp(9), host.dp(8), host.dp(9), host.dp(9));
        mini.setBackground(host.roundBorder(
                Color.rgb(45, 34, 69),
                Color.rgb(83, 67, 112),
                13,
                1));

        String label = vedicEvidenceLabel(rawValue);
        String value = vedicEvidenceValue(rawValue, label);

        TextView labelView = host.text(label, 10, MainActivity.GOLD, true);
        mini.addView(labelView);

        TextView valueView = host.text(value, 12, MainActivity.TEXT, true);
        valueView.setLineSpacing(host.dp(2), 1f);
        valueView.setMaxLines(4);
        mini.addView(valueView, host.marginTop(3));

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        if (withLeftMargin) lp.leftMargin = host.dp(6);
        row.addView(mini, lp);
    }
    String vedicEvidenceLabel(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.startsWith("Mahadasha｜")) return "Mahadasha";
        if (value.startsWith("Antardasha｜")) return "Antardasha";
        if (value.startsWith("Gochar ")) {
            int arrow = value.indexOf(" →");
            return arrow > 7 ? value.substring(7, arrow).trim() + " Gochar" : "Gochar";
        }
        if (value.startsWith("Transit ")) return "Transit";
        if (value.startsWith("目前 Mahadasha")) return "Dasha";
        int space = value.indexOf(' ');
        if (space > 0 && space <= 12) return value.substring(0, space).trim();
        return "命盤";
    }
    String vedicEvidenceValue(String raw, String label) {
        String value = raw == null ? "" : raw.trim();
        if ("Mahadasha".equals(label) && value.startsWith("Mahadasha｜")) {
            return value.substring("Mahadasha｜".length()).trim();
        }
        if ("Antardasha".equals(label) && value.startsWith("Antardasha｜")) {
            return value.substring("Antardasha｜".length()).trim();
        }
        if (label.endsWith(" Gochar") && value.startsWith("Gochar ")) {
            int arrow = value.indexOf(" →");
            return arrow >= 0 ? value.substring(arrow + 1).trim() : value;
        }
        if ("Transit".equals(label) && value.startsWith("Transit ")) {
            return value.substring("Transit ".length()).trim();
        }
        if (value.startsWith(label + " ")) {
            return value.substring(label.length() + 1).trim();
        }
        return value;
    }
}
