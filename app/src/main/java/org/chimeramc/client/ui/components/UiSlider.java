package org.chimeramc.client.ui.components;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * A slider: a 4dp track, an accent-filled portion and a 16dp thumb. The value-to-x mapping is pure
 * so a test can pin it.
 */
public final class UiSlider {

    private UiSlider() {
    }

    /** The thumb centre x for a value in {@code [min, max]}. */
    public static float thumbX(float value, float min, float max, float left, float right) {
        float t = (max <= min) ? 0f : (value - min) / (max - min);
        t = Math.max(0f, Math.min(1f, t));
        return left + (right - left) * t;
    }

    /** Draws the slider. */
    public static void draw(Canvas c, Paint paint, float left, float right, float cy, float value,
                            float min, float max, int accent, float density) {
        float trackH = 4f * density;
        float thumbR = 8f * density;
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(UiTokens.TOGGLE_OFF_TRACK);
        c.drawRoundRect(left, cy - trackH / 2f, right, cy + trackH / 2f, trackH / 2f, trackH / 2f,
                paint);
        float tx = thumbX(value, min, max, left, right);
        paint.setColor(accent);
        c.drawRoundRect(left, cy - trackH / 2f, tx, cy + trackH / 2f, trackH / 2f, trackH / 2f,
                paint);
        paint.setColor(0xFFFFFFFF);
        c.drawCircle(tx, cy, thumbR, paint);
    }
}
