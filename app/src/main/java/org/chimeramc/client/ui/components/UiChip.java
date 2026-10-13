package org.chimeramc.client.ui.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * The shared quick-toggle chip: a 22dp pill with a 6dp status dot and a 10.5sp label. Used by the
 * radial wheel's floating clusters. The layout (width from a measured label) is pure and unit-tested.
 */
public final class UiChip {

    public static final float HEIGHT_DP = 22f;
    public static final float DOT_DP = 6f;
    public static final float LABEL_SP = 10.5f;

    private UiChip() {
    }

    /** The chip's width in px for a label of {@code labelWidthPx} and the given density. */
    public static float width(float labelWidthPx, float density) {
        // padding-left (dot + gaps) + label + padding-right
        return (8f + DOT_DP + 6f + 8f) * density + labelWidthPx;
    }

    /** The status dot's colour: accent when on, disabled grey when off. */
    public static int dotColor(int accent, boolean on) {
        return on ? accent : UiTokens.TEXT_DISABLED;
    }

    /** Draws the chip centred on {@code (cx, cy)}. */
    public static void draw(Canvas c, Paint paint, Paint textPaint, float cx, float cy,
                            float labelWidthPx, String label, int accent, boolean on, float density) {
        float w = width(labelWidthPx, density);
        float h = HEIGHT_DP * density;
        float l = cx - w / 2f;
        float t = cy - h / 2f;
        float r = l + w;
        float b = t + h;
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(UiTokens.CHIP_BG);
        c.drawRoundRect(new RectF(l, t, r, b), h / 2f, h / 2f, paint);
        UiDraw.hairline(c, paint, l, t, r, b, h / 2f, Math.max(1f, density), UiTokens.CHIP_BORDER);

        float dotR = DOT_DP * density / 2f;
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(dotColor(accent, on));
        c.drawCircle(l + 8f * density + dotR, cy, dotR, paint);

        textPaint.setColor(UiTokens.TEXT_PRIMARY);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(LABEL_SP * density);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float ty = cy - (fm.ascent + fm.descent) / 2f;
        c.drawText(label, l + 8f * density + DOT_DP * density + 6f * density, ty, textPaint);
    }
}
