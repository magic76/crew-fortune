package com.crewpocket.fortune;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class VedicDashaTimelineView extends View {
    private final Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint segment = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint current = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint muted = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Map<?, ?>> periods = new ArrayList<Map<?, ?>>();
    private String currentLord = "";

    public VedicDashaTimelineView(Context context, FortuneFacts facts) {
        super(context);
        setMinimumHeight(dp(130));
        base.setColor(Color.rgb(50, 38, 76));
        segment.setColor(Color.rgb(83, 67, 113));
        current.setColor(Color.rgb(183, 156, 255));
        text.setColor(Color.rgb(248, 245, 255));
        text.setTextSize(sp(10));
        text.setFakeBoldText(true);
        muted.setColor(Color.rgb(190, 181, 207));
        muted.setTextSize(sp(9));

        Object raw = facts == null ? null : facts.detail("mahadashaTimeline");
        if (raw instanceof List) {
            for (Object item : (List<?>) raw) {
                if (item instanceof Map) periods.add((Map<?, ?>) item);
            }
        }
        Object currentRaw = facts == null ? null : facts.detail("currentMahadasha");
        if (currentRaw instanceof Map) {
            currentLord = value(((Map<?, ?>) currentRaw).get("lord"));
        }
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (width <= 0) width = dp(320);
        setMeasuredDimension(width, dp(138));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (periods.isEmpty()) return;

        float left = dp(6);
        float right = getWidth() - dp(6);
        float top = dp(32);
        float height = dp(34);
        float width = right - left;

        LocalDate first = parse(value(periods.get(0).get("startDate")));
        LocalDate last = parse(value(periods.get(periods.size() - 1).get("endDate")));
        long totalDays = Math.max(1, ChronoUnit.DAYS.between(first, last));

        canvas.drawRoundRect(new RectF(left, top, right, top + height), dp(10), dp(10), base);

        for (Map<?, ?> period : periods) {
            LocalDate start = parse(value(period.get("startDate")));
            LocalDate end = parse(value(period.get("endDate")));
            float x1 = left + width * ChronoUnit.DAYS.between(first, start) / (float) totalDays;
            float x2 = left + width * ChronoUnit.DAYS.between(first, end) / (float) totalDays;
            String lord = value(period.get("lord"));
            Paint p = lord.equals(currentLord) ? current : segment;
            canvas.drawRoundRect(new RectF(x1 + dp(1), top + dp(1), x2 - dp(1), top + height - dp(1)), dp(8), dp(8), p);

            if (x2 - x1 >= dp(30)) {
                Paint tp = new Paint(text);
                tp.setTextAlign(Paint.Align.CENTER);
                if (lord.equals(currentLord)) tp.setColor(Color.rgb(30, 22, 46));
                canvas.drawText(lord, (x1 + x2) / 2f, top + dp(21), tp);
            }
        }

        Paint head = new Paint(text);
        head.setTextSize(sp(12));
        head.setColor(Color.rgb(255, 214, 128));
        canvas.drawText("Vimshottari Mahadasha", left, dp(18), head);

        String currentLabel = currentLord.isEmpty() ? "" : "現在：" + currentLord;
        Paint currentLabelPaint = new Paint(muted);
        currentLabelPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(currentLabel, right, dp(18), currentLabelPaint);

        canvas.drawText(first.toString(), left, top + height + dp(20), muted);
        Paint endPaint = new Paint(muted);
        endPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(last.toString(), right, top + height + dp(20), endPaint);

        drawNowMarker(canvas, first, last, left, right, top, height);
    }

    private void drawNowMarker(
            Canvas canvas,
            LocalDate first,
            LocalDate last,
            float left,
            float right,
            float top,
            float height) {
        LocalDate now = LocalDate.now();
        if (now.isBefore(first) || !now.isBefore(last)) return;
        long totalDays = Math.max(1, ChronoUnit.DAYS.between(first, last));
        long elapsed = ChronoUnit.DAYS.between(first, now);
        float x = left + (right - left) * elapsed / (float) totalDays;
        Paint marker = new Paint(Paint.ANTI_ALIAS_FLAG);
        marker.setColor(Color.rgb(255, 214, 128));
        marker.setStrokeWidth(dp(2));
        canvas.drawLine(x, top - dp(7), x, top + height + dp(7), marker);
    }

    private static LocalDate parse(String value) {
        try { return LocalDate.parse(value); }
        catch (Exception ignored) { return LocalDate.of(1970, 1, 1); }
    }

    private static String value(Object raw) {
        return raw == null ? "" : String.valueOf(raw);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float sp(int value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
