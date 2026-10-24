package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * The Mod Menu's pill switch, as pure geometry and colour (M5).
 *
 * <p>A 52x32dp track with a 24dp thumb inset 4dp, spring-animated on the thumb only. The state
 * machine is here rather than in the view so a JVM test pins the travel, the colours and the
 * disabled appearance without a device.
 *
 * <p>Deliberately no third "pending" visual state: a module either enables or it does not, and if
 * {@code enable()} fails the switch animates back and a toast names the reason. An intermediate
 * state is how a control starts lying about what it did.
 */
public final class ModMenuSwitch {

    private ModMenuSwitch() {
    }

    /** The thumb's travel in px: the track minus the thumb and both insets. */
    public static float travel(float density) {
        return ModMenuMetrics.SWITCH_WIDTH_DP * density
                - ModMenuMetrics.SWITCH_THUMB_DP * density
                - 2f * ModMenuMetrics.SWITCH_THUMB_INSET_DP * density;
    }

    /**
     * The thumb's centre X inside the track, for animation progress 0 (off) .. 1 (on).
     *
     * <p>The progress is clamped so an over-shooting spring cannot push the thumb outside its track.
     */
    public static float thumbCenterX(float density, float progress) {
        float p = Math.max(0f, Math.min(1f, progress));
        float inset = ModMenuMetrics.SWITCH_THUMB_INSET_DP * density;
        float thumb = ModMenuMetrics.SWITCH_THUMB_DP * density;
        return inset + thumb / 2f + travel(density) * p;
    }

    /** The track fill for a state: the accent when on, the muted accent when off. */
    public static int trackColor(boolean on) {
        return on ? ModMenuPalette.ACCENT : ModMenuPalette.mutedTrack();
    }

    /**
     * The track fill for a row that cannot be driven.
     *
     * <p>A failed module gets {@code state.error} at 30% rather than the muted accent, so the row's
     * failure is visible on the control itself and not only on the badge beside it.
     */
    public static int failedTrackColor() {
        return ModMenuPalette.blend(ModMenuPalette.BG_SURFACE_3,
                ModMenuPalette.STATE_ERROR, 0.30f);
    }

    /** The thumb colour: off-white normally, tertiary grey when the row is not interactive. */
    public static int thumbColor(boolean interactive) {
        return interactive ? ModMenuPalette.TEXT_PRIMARY : ModMenuPalette.TEXT_TERTIARY;
    }

    /**
     * The spring parameters for a state change (M5).
     *
     * <p>Turning <b>on</b> is springier than turning <b>off</b>: an enable is a commitment and the
     * overshoot reads as confirmation, while a disable should feel like a plain retreat.
     */
    public static float dampingFor(boolean turningOn) {
        return turningOn ? 0.72f : 0.85f;
    }

    public static float stiffnessFor(boolean turningOn) {
        return turningOn ? 520f : 480f;
    }

    /**
     * Whether a state change should tick the vibrator (M5).
     *
     * <p>Only the state change haptics — never the press-down, which would make the control feel
     * noisy and fire twice per flip.
     */
    public static boolean hapticsOnStateChange() {
        return true;
    }

    /**
     * Draws the switch.
     *
     * @param interactive false draws the disabled appearance (grey thumb, no accent)
     * @param failed      true draws the error track, so a broken row is visible on the control
     */
    public static void draw(Canvas canvas, Paint paint, float left, float top, float density,
                            boolean on, float progress, boolean interactive, boolean failed) {
        float width = ModMenuMetrics.SWITCH_WIDTH_DP * density;
        float height = ModMenuMetrics.SWITCH_HEIGHT_DP * density;
        float radius = height / 2f;
        RectF track = new RectF(left, top, left + width, top + height);

        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        if (failed) {
            paint.setColor(failedTrackColor());
        } else if (!interactive) {
            paint.setColor(ModMenuPalette.BG_SURFACE_3);
        } else {
            paint.setColor(trackColor(on));
        }
        canvas.drawRoundRect(track, radius, radius, paint);

        float thumb = ModMenuMetrics.SWITCH_THUMB_DP * density;
        float cx = left + thumbCenterX(density, progress);
        float cy = top + height / 2f;
        paint.setColor(thumbColor(interactive));
        canvas.drawCircle(cx, cy, thumb / 2f, paint);

        // The thumb's own shadow, so it reads as a raised part rather than a painted dot.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, density));
        paint.setColor(Color.argb(0x40, 0, 0, 0));
        canvas.drawCircle(cx, cy, thumb / 2f, paint);
    }
}
