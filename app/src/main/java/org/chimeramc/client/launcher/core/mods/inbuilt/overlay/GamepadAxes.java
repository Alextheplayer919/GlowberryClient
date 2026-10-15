package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.view.InputDevice;
import android.view.MotionEvent;

/**
 * Reads the right stick from a {@link MotionEvent} as a normalised pair.
 *
 * <p>The radial wheel's cursor did not move at all, and the cause was the axis source. The wheel
 * read {@code AXIS_Z}/{@code AXIS_RZ} only. Those are the older Android right-stick axes, but a pad
 * may also report its sticks as {@code AXIS_RX}/{@code AXIS_RY} (the generic HID axes). Some devices
 * populate both and some only one, so reading a single pair produced an all-zero vector — and the
 * cursor never moved.
 *
 * <p>This resolves the axis by taking the convention with the actual deflection, and falls back to
 * the left-stick axes so a pad with only one stick reported still drives the cursor. Pure arithmetic
 * over the two axis values it is handed, so the resolution rule is unit-tested without a device;
 * only {@link #read} touches Android.
 */
public final class GamepadAxes {

    /** A right-stick reading: x/y in -1..1, with the source that produced it. */
    public static final class Stick {
        public final float x;
        public final float y;
        public final String source;

        public Stick(float x, float y, String source) {
            this.x = x;
            this.y = y;
            this.source = source == null ? "" : source;
        }
    }

    private GamepadAxes() {
    }

    /**
     * The axis conventions tried, as {@code {xAxis, yAxis}} pairs.
     *
     * <p>Android documents two right-stick pairs: the older {@code AXIS_Z}/{@code AXIS_RZ} (what an
     * Xbox pad reports) and {@code AXIS_RX}/{@code AXIS_RY} (what a generic HID pad reports). Some
     * pads populate both and some only one, so both are tried. Reading only the first (the old code)
     * gave an all-zero vector on the pads that use the second, which is why the cursor never moved.
     *
     * <p>A third "PlayStation" convention, {@code AXIS_RZ}/{@code AXIS_RX}, is deliberately
     * <b>not</b> in this list: {@code RZ} is the <em>y</em> axis under the Xbox convention and the
     * <em>x</em> axis under that one, so including it would make an Xbox stick pushed straight up
     * read as pushed right. {@link #pick} also prefers the pair with the larger magnitude, so a pad
     * that reports both conventions resolves to the one actually carrying the deflection.
     */
    public static final int[][] RIGHT_STICK_AXES = {
            {MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ},
            {MotionEvent.AXIS_RX, MotionEvent.AXIS_RY},
    };

    /**
     * Picks the right-stick pair.
     *
     * <p>Among the candidate conventions, the one with the largest deflection magnitude wins, so a
     * pad that reports zero on one convention and a real value on the other resolves correctly. When
     * every convention is inside {@code threshold} the stick is centred and the first convention's
     * (near-zero) values are returned.
     *
     * @param candidates  axis pairs, each {@code {xAxis, yAxis}}
     * @param values      a lookup from axis to value, supplied by the caller so this is testable
     * @param threshold   the magnitude above which an axis counts as deflected
     * @param sourceName  a label prefix for the chosen pair, e.g. "right"
     */
    public static Stick pick(int[][] candidates, AxisLookup values, float threshold,
                             String sourceName) {
        if (candidates == null || candidates.length == 0) {
            return new Stick(0f, 0f, sourceName + ":none");
        }
        int bestIndex = -1;
        float bestMagnitude = threshold;
        float bestX = 0f;
        float bestY = 0f;
        for (int i = 0; i < candidates.length; i++) {
            int[] pair = candidates[i];
            if (pair == null || pair.length < 2) continue;
            float x = values.value(pair[0]);
            float y = values.value(pair[1]);
            float magnitude = (float) Math.hypot(x, y);
            if (magnitude > bestMagnitude) {
                bestMagnitude = magnitude;
                bestIndex = i;
                bestX = x;
                bestY = y;
            }
        }
        if (bestIndex >= 0) {
            return new Stick(bestX, bestY, sourceName + ":pair" + bestIndex);
        }
        // Nothing deflected: a genuinely centred stick. Return the first convention's real values.
        int[] first = candidates[0];
        return new Stick(values.value(first[0]), values.value(first[1]), sourceName + ":pair0");
    }

    /** Supplies an axis value; the real one reads a MotionEvent, the test one a table. */
    public interface AxisLookup {
        float value(int axis);
    }

    /**
     * Reads the right stick from a live event, falling back to the left stick when the pad only
     * reports one.
     */
    public static Stick read(MotionEvent event) {
        if (event == null) return new Stick(0f, 0f, "none");
        AxisLookup lookup = event::getAxisValue;
        Stick right = pick(RIGHT_STICK_AXES, lookup, 0.01f, "right");
        if (Math.abs(right.x) > 0.01f || Math.abs(right.y) > 0.01f) {
            return right;
        }
        // Some handhelds and single-stick pads only report AXIS_X/AXIS_Y. Fall back so the cursor is
        // still drivable rather than dead.
        int[][] leftAxes = {{MotionEvent.AXIS_X, MotionEvent.AXIS_Y}};
        Stick left = pick(leftAxes, lookup, 0.01f, "left");
        if (Math.abs(left.x) > 0.01f || Math.abs(left.y) > 0.01f) {
            return left;
        }
        return right;
    }

    /** True when the event came from a joystick/gamepad source (not a mouse or touch). */
    public static boolean isGamepadSource(MotionEvent event) {
        if (event == null) return false;
        int sources = event.getSource();
        return (sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
                || (sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK;
    }
}
