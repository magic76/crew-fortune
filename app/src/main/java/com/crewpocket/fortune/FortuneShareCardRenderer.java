package com.crewpocket.fortune;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

public final class FortuneShareCardRenderer {
    public static final int WIDTH = 1080;
    public static final int HEIGHT = 1350;

    private static final int BG_TOP = Color.rgb(23, 17, 38);
    private static final int BG_BOTTOM = Color.rgb(36, 24, 57);
    private static final int CARD = Color.rgb(46, 35, 70);
    private static final int CARD_ALT = Color.rgb(39, 30, 60);
    private static final int BORDER = Color.rgb(92, 73, 127);
    private static final int TEXT = Color.rgb(248, 245, 255);
    private static final int MUTED = Color.rgb(190, 181, 207);
    private static final int ACCENT = Color.rgb(183, 156, 255);
    private static final int GOLD = Color.rgb(255, 214, 128);

    private FortuneShareCardRenderer() {}

    public static Bitmap render(FortuneShareCardData data) {
        if (data == null) throw new IllegalArgumentException("share data is required");

        Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint background = new Paint(Paint.ANTI_ALIAS_FLAG);
        background.setShader(new LinearGradient(
                0, 0, 0, HEIGHT,
                BG_TOP, BG_BOTTOM,
                Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, WIDTH, HEIGHT, background);

        final float margin = 70f;
        drawText(canvas, "CREW FORTUNE · 命運研究所",
                margin, 72f, 30f, GOLD, true, WIDTH - margin * 2, 1.0f);
        drawBadge(canvas, data.modeLabel, WIDTH - margin - 230f, 54f, 230f, 58f);

        drawText(canvas, "很認真算，別太認真信。",
                margin, 125f, 52f, TEXT, true, WIDTH - margin * 2, 1.0f);

        drawSection(
                canvas,
                margin, 205f,
                WIDTH - margin * 2, 220f,
                CARD,
                data.coreTitle,
                data.coreValue,
                38f);

        drawSection(
                canvas,
                margin, 445f,
                WIDTH - margin * 2, 210f,
                CARD_ALT,
                data.cycleTitle,
                data.cycleValue,
                34f);

        drawYears(
                canvas,
                margin, 675f,
                WIDTH - margin * 2, 330f,
                data.yearsTitle,
                data.years);

        drawQuote(
                canvas,
                margin, 1025f,
                WIDTH - margin * 2, 220f,
                data.quote);

        drawText(canvas, "娛樂用途 · 計算資料固定 · #CrewFortune",
                margin, 1280f, 24f, MUTED, false, WIDTH - margin * 2, 1.0f);

        return bitmap;
    }

    private static void drawSection(
            Canvas canvas,
            float left,
            float top,
            float width,
            float height,
            int fill,
            String label,
            String body,
            float bodySize) {
        drawCard(canvas, left, top, width, height, fill);
        drawText(canvas, label,
                left + 30f, top + 28f, 25f, GOLD, true, width - 60f, 1.0f);
        drawText(canvas, body,
                left + 30f, top + 72f, bodySize, TEXT, true, width - 60f, 1.12f);
    }

    private static void drawYears(
            Canvas canvas,
            float left,
            float top,
            float width,
            float height,
            String label,
            java.util.List<String> years) {
        drawCard(canvas, left, top, width, height, CARD);

        drawText(canvas, label,
                left + 30f, top + 28f, 25f, GOLD, true, width - 60f, 1.0f);

        float y = top + 78f;
        int index = 0;
        for (String year : years) {
            if (index >= 3) break;

            Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
            dot.setColor(ACCENT);
            canvas.drawCircle(left + 38f, y + 18f, 7f, dot);

            drawText(canvas, year,
                    left + 64f, y,
                    31f, TEXT, index == 0,
                    width - 94f, 1.08f);
            y += 78f;
            index++;
        }
    }

    private static void drawQuote(
            Canvas canvas,
            float left,
            float top,
            float width,
            float height,
            String quote) {
        drawCard(canvas, left, top, width, height, Color.rgb(55, 42, 82));

        drawText(canvas, "AI 命理師一句話",
                left + 30f, top + 26f,
                24f, GOLD, true, width - 60f, 1.0f);

        String value = quote == null || quote.trim().isEmpty()
                ? "把命理當成觀察自己的另一個角度。"
                : "「" + quote.trim() + "」";
        drawText(canvas, value,
                left + 30f, top + 70f,
                34f, TEXT, true, width - 60f, 1.12f);
    }

    private static void drawCard(
            Canvas canvas,
            float left,
            float top,
            float width,
            float height,
            int fill) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(fill);
        canvas.drawRoundRect(
                new RectF(left, top, left + width, top + height),
                30f, 30f, paint);

        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2f);
        stroke.setColor(BORDER);
        canvas.drawRoundRect(
                new RectF(left, top, left + width, top + height),
                30f, 30f, stroke);
    }

    private static void drawBadge(
            Canvas canvas,
            String label,
            float left,
            float top,
            float width,
            float height) {
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(Color.rgb(62, 47, 92));
        canvas.drawRoundRect(
                new RectF(left, top, left + width, top + height),
                height / 2f, height / 2f, fill);

        drawCenteredText(canvas, label, left, top, width, height, 25f, ACCENT, true);
    }

    private static void drawCenteredText(
            Canvas canvas,
            String value,
            float left,
            float top,
            float width,
            float height,
            float size,
            int color,
            boolean bold) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        float textWidth = paint.measureText(value == null ? "" : value);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float baseline = top + (height - (metrics.bottom - metrics.top)) / 2f - metrics.top;
        canvas.drawText(
                value == null ? "" : value,
                left + (width - textWidth) / 2f,
                baseline,
                paint);
    }

    private static void drawText(
            Canvas canvas,
            String value,
            float left,
            float top,
            float size,
            int color,
            boolean bold,
            float width,
            float lineSpacingMultiplier) {
        TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);

        StaticLayout layout = StaticLayout.Builder.obtain(
                        value == null ? "" : value,
                        0,
                        value == null ? 0 : value.length(),
                        paint,
                        Math.max(1, Math.round(width)))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .setLineSpacing(0f, lineSpacingMultiplier)
                .build();

        canvas.save();
        canvas.translate(left, top);
        layout.draw(canvas);
        canvas.restore();
    }
}
