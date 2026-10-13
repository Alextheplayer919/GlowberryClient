package org.chimeramc.client.ui.components;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

/**
 * The shared toggle: a 36x20dp track with a 16dp white thumb, ON track a blue gradient and OFF a flat
 * grey, thumb sliding 140ms. The <b>state machine</b> ({@link #thumbFraction}, {@link #trackColor})
 * is pure so a JVM test pins the geometry and the colours without a device.
 */
public final class UiToggle {

    /** The thumb's centre X within the track, given the animation progress 0 (off) .. 1 (on). */
    public static float thumbFraction(boolean on, float progress) {
        float p = on ? progress : 1f - progress;
        return Math.max(0f, Math.min(1f, p));
    }

    /**
     * The track colour for a state: a left-to-right accent gradient when on, the flat off track when
     * off. Returns the single representative colour for the off state so a test can compare directly.
     */
    public static int trackColor(int accent, boolean on) {
        return on ? accent : UiTokens.TOGGLE_OFF_TRACK;
    }

    /** The thumb centre, in px, inside a track of the given bounds. */
    public static float thumbCenterX(float trackLeft, float trackWidth, float trackHeight,
                                     float thumbDiameter, float inset, boolean on, float progress) {
        float f = thumbFraction(on, progress);
        float travel = trackWidth - thumbDiameter - 2f * inset;
        return trackLeft + inset + thumbDiameter / 2f + travel * f;
    }

    /** Draws the toggle. {@code progress} is 0..1 (0 = fully off, 1 = fully on). */
    public static void draw(Canvas c, Paint paint, float left, float top, float width, float height,
                            int accent, boolean on, float progress, float density) {
        float radius = height / 2f;
        RectF track = new RectF(left, top, left + width, top + height);
        paint.reset();
        paint.setAntiAlias(true);
        if (on) {
            paint.setShader(new LinearGradient(left, top, left + width, top,
                    accent, UiTokens.accentHover(accent), Shader.TileMode.CLAMP));
        } else {
            paint.setColor(UiTokens.TOGGLE_OFF_TRACK);
        }
        c.drawRoundRect(track, radius, radius, paint);
        paint.setShader(null);

        float thumb = 16f * density;
        float inset = 2f * density;
        float cx = thumbCenterX(left, width, height, thumb, inset, on, progress);
        float cy = top + height / 2f;
        paint.setColor(Color.WHITE);
        c.drawCircle(cx, cy, thumb / 2f, paint);
    }
}
