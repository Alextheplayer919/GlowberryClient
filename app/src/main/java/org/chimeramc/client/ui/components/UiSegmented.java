package org.chimeramc.client.ui.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * A segmented control: a 32dp pill with N segments, the selected one filled with the accent. The
 * segment hit-testing is pure so a test can assert which segment a tap lands on.
 */
public final class UiSegmented {

    public static final float HEIGHT_DP = 32f;

    private UiSegmented() {
    }

    /** The segment index for a pointer x within the control. Returns -1 when outside. */
    public static int segmentAt(float x, float left, float width, int count) {
        if (count <= 0 || x < left || x >= left + width) return -1;
        float segW = width / count;
        int idx = (int) ((x - left) / segW);
        return Math.max(0, Math.min(count - 1, idx));
    }

    /** Draws the control with the given labels; the selected index is filled with the accent. */
    public static void draw(Canvas c, Paint paint, Paint textPaint, RectF b, String[] labels,
                            int selected, int accent, float density) {
        float radius = b.height() / 2f;
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(UiTokens.BG_SURFACE);
        c.drawRoundRect(b, radius, radius, paint);
        UiDraw.hairline(c, paint, b.left, b.top, b.right, b.bottom, radius,
                Math.max(1f, density), UiTokens.STROKE_HAIRLINE);

        int count = labels.length;
        float segW = b.width() / count;
        textPaint.setTextSize(12f * density);
        textPaint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < count; i++) {
            float l = b.left + i * segW;
            float r = l + segW;
            if (i == selected) {
                paint.reset();
                paint.setAntiAlias(true);
                paint.setColor(accent);
                c.drawRoundRect(new RectF(l, b.top, r, b.bottom), radius, radius, paint);
            }
            textPaint.setColor(i == selected ? 0xFFFFFFFF : UiTokens.TEXT_SECONDARY);
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float ty = b.centerY() - (fm.ascent + fm.descent) / 2f;
            c.drawText(labels[i], (l + r) / 2f, ty, textPaint);
        }
    }
}
