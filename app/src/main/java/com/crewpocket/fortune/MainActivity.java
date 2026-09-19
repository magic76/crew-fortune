package com.crewpocket.fortune;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Date;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(23, 17, 38);
    private static final int CARD = Color.rgb(39, 30, 60);
    private static final int CARD_2 = Color.rgb(50, 38, 76);
    private static final int TEXT = Color.rgb(248, 245, 255);
    private static final int MUTED = Color.rgb(190, 181, 207);
    private static final int ACCENT = Color.rgb(183, 156, 255);
    private static final int GOLD = Color.rgb(255, 214, 128);

    private final FortuneEngine engine = new FortuneEngine();
    private FortuneMode selectedMode = FortuneMode.TODAY;
    private EditText nameInput;
    private EditText birthInput;
    private EditText secondInput;
    private TextView modeLabel;
    private LinearLayout resultCard;
    private FortuneResult currentResult;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        setContentView(buildScreen());
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = column();
        root.setPadding(dp(20), dp(24), dp(20), dp(40));
        scroll.addView(root);

        TextView eyebrow = text("CREW FORTUNE · 命運研究所", 13, GOLD, true);
        root.addView(eyebrow);

        TextView title = text("很認真算，\n別太認真信。", 34, TEXT, true);
        title.setLineSpacing(0, 1.04f);
        root.addView(title, marginTop(8));

        TextView sub = text("把命理算得有條理，把人生講得有笑點。\n結果不好也沒關係，至少文案要好笑。", 15, MUTED, false);
        sub.setLineSpacing(dp(4), 1f);
        root.addView(sub, marginTop(10));

        LinearLayout form = column();
        form.setPadding(dp(16), dp(16), dp(16), dp(16));
        form.setBackground(round(CARD, 22));
        root.addView(form, marginTop(24));

        form.addView(label("先交代一下你的基本資料"));
        nameInput = input("你的名字");
        birthInput = input("生日，例如 1985-07-22");
        secondInput = input("朋友 / 對象名字（合盤時填）");
        secondInput.setVisibility(View.GONE);
        form.addView(nameInput, marginTop(12));
        form.addView(birthInput, marginTop(10));
        form.addView(secondInput, marginTop(10));

        modeLabel = text("今天想算：今日運勢", 16, TEXT, true);
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

        TextView foot = text("娛樂用途 · 不預測死亡、重大疾病、懷孕、犯罪或災難", 12, MUTED, false);
        foot.setGravity(Gravity.CENTER);
        root.addView(foot, marginTop(22));
        return scroll;
    }

    private void selectMode(FortuneMode mode) {
        selectedMode = mode;
        modeLabel.setText("今天想算：" + mode.title() + "\n" + mode.subtitle());
        secondInput.setVisibility(mode == FortuneMode.COMPATIBILITY ? View.VISIBLE : View.GONE);
    }

    private void calculate() {
        FortuneProfile profile = new FortuneProfile(
                nameInput.getText().toString(),
                birthInput.getText().toString(),
                secondInput.getText().toString());
        try {
            currentResult = engine.calculate(selectedMode, profile, new Date());
            renderResult(currentResult);
        } catch (IllegalArgumentException error) {
            Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void renderResult(FortuneResult result) {
        resultCard.removeAllViews();

        TextView badge = text(result.mode.title().toUpperCase(), 12, GOLD, true);
        resultCard.addView(badge);

        TextView title = text(result.title, 25, TEXT, true);
        resultCard.addView(title, marginTop(8));

        TextView score = text(result.score + " / 100", 30, ACCENT, true);
        resultCard.addView(score, marginTop(12));

        TextView basis = text("計算依據｜" + result.basis, 13, MUTED, false);
        resultCard.addView(basis, marginTop(4));

        addSection("認真分析", result.analysis);
        addSection("翻譯成人話", result.translation);
        addSection("命理師補充", result.punchline);
        addSection("今日忠告", result.advice);

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

    private void addSection(String heading, String body) {
        TextView h = text(heading, 12, GOLD, true);
        resultCard.addView(h, marginTop(18));
        TextView b = text(body, 16, TEXT, false);
        b.setLineSpacing(dp(3), 1f);
        resultCard.addView(b, marginTop(5));
    }

    private void shareResult() {
        if (currentResult == null) return;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, currentResult.shareText());
        startActivity(Intent.createChooser(intent, "分享你的命運"));
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
