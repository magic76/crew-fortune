package com.crewpocket.fortune;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class VedicNatalChartView extends View {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint title = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accent = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Map<?, ?>> houses = new ArrayList<Map<?, ?>>();
    private int lagnaHouse = 1;

    public VedicNatalChartView(Context context, FortuneFacts facts) {
        super(context);
        setMinimumHeight(dp(330));
        fill.setColor(Color.rgb(31, 24, 48));
        stroke.setColor(Color.rgb(92, 73, 127));
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(dp(1));
        title.setColor(Color.rgb(255, 214, 128));
        title.setTextSize(sp(11));
        title.setFakeBoldText(true);
        body.setColor(Color.rgb(248, 245, 255));
        body.setTextSize(sp(11));
        accent.setColor(Color.rgb(183, 156, 255));
        accent.setTextSize(sp(10));
        accent.setFakeBoldText(true);

        Object raw = facts == null ? null : facts.detail("houses");
        if (raw instanceof List) {
            for (Object item : (List<?>) raw) {
                if (item instanceof Map) houses.add((Map<?, ?>) item);
            }
        }
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (width <= 0) width = dp(320);
        setMeasuredDimension(width, Math.max(dp(330), (int) (width * 1.02f)));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float pad = dp(6);
        float size = Math.min(getWidth() - pad * 2, getHeight() - pad * 2);
        float left = (getWidth() - size) / 2f;
        float top = (getHeight() - size) / 2f;
        float cell = size / 4f;

        canvas.drawRoundRect(new RectF(left, top, left + size, top + size), dp(14), dp(14), fill);
        for (int i = 0; i <= 4; i++) {
            canvas.drawLine(left + i * cell, top, left + i * cell, top + size, stroke);
            canvas.drawLine(left, top + i * cell, left + size, top + i * cell, stroke);
        }

        fill.setColor(Color.rgb(23, 17, 38));
        canvas.drawRect(left + cell, top + cell, left + cell * 3, top + cell * 3, fill);
        fill.setColor(Color.rgb(31, 24, 48));

        int[][] houseGrid = {
                {12, 1, 2, 3},
                {11, 0, 0, 4},
                {10, 0, 0, 5},
                {9, 8, 7, 6}
        };
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                int house = houseGrid[row][col];
                if (house == 0) continue;
                drawHouse(canvas, house, left + col * cell, top + row * cell, cell);
            }
        }

        String lagnaSign = "";
        for (Map<?, ?> h : houses) {
            if ("1".equals(value(h.get("house")))) {
                lagnaSign = value(h.get("sign"));
                break;
            }
        }
        String center = "South Indian chart";
        float centerX = left + size / 2f;
        float centerY = top + size / 2f - dp(4);
        Paint centerPaint = new Paint(title);
        centerPaint.setTextAlign(Paint.Align.CENTER);
        centerPaint.setTextSize(sp(13));
        canvas.drawText(center, centerX, centerY, centerPaint);
        centerPaint.setTextSize(sp(10));
        centerPaint.setColor(Color.rgb(190, 181, 207));
        canvas.drawText("Whole Sign · Lagna " + lagnaSign, centerX, centerY + dp(18), centerPaint);
    }

    private void drawHouse(Canvas canvas, int houseNumber, float x, float y, float cell) {
        Map<?, ?> data = findHouse(houseNumber);
        String sign = data == null ? "" : value(data.get("sign"));
        String planets = data == null ? "" : compact(data.get("planets"));

        canvas.drawText("H" + houseNumber, x + dp(7), y + dp(16), title);
        if (houseNumber == lagnaHouse) {
            canvas.drawText("LAGNA", x + dp(7), y + dp(32), accent);
        }
        canvas.drawText(sign, x + dp(7), y + (houseNumber == lagnaHouse ? dp(48) : dp(34)), body);

        if (!planets.isEmpty()) {
            float lineY = y + (houseNumber == lagnaHouse ? dp(65) : dp(51));
            for (String line : wrap(planets, 13)) {
                if (lineY > y + cell - dp(5)) break;
                canvas.drawText(line, x + dp(7), lineY, body);
                lineY += dp(14);
            }
        }
    }

    private Map<?, ?> findHouse(int houseNumber) {
        for (Map<?, ?> house : houses) {
            if (String.valueOf(houseNumber).equals(value(house.get("house")))) return house;
        }
        return null;
    }

    private static String compact(Object raw) {
        if (!(raw instanceof List)) return value(raw);
        StringBuilder out = new StringBuilder();
        for (Object item : (List<?>) raw) {
            if (out.length() > 0) out.append(" · ");
            out.append(value(item));
        }
        return out.toString();
    }

    private static List<String> wrap(String value, int maxChars) {
        List<String> out = new ArrayList<String>();
        if (value == null || value.isEmpty()) return out;
        String[] parts = value.split(" · ");
        StringBuilder line = new StringBuilder();
        for (String part : parts) {
            if (line.length() > 0 && line.length() + 3 + part.length() > maxChars) {
                out.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) line.append(" · ");
            line.append(part);
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
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
