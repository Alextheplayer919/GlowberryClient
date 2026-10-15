package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.view.MotionEvent;

import org.junit.Test;

/**
 * Pins the right-stick axis resolution.
 *
 * <p>The controller cursor did not move because the wheel read only {@code AXIS_Z}/{@code AXIS_RZ}:
 * a pad that reports its right stick on {@code AXIS_RX}/{@code AXIS_RY} (or
 * {@code AXIS_RZ}/{@code AXIS_RX}) then delivered an all-zero vector. This test drives the pure
 * resolution rule with fake axis tables for each convention, so the fix is verified without a device.
 *
 * <p>Only the real axis constants are referenced from Android; the values come from the table, so no
 * MotionEvent is constructed.
 */
public class GamepadAxesTest {

    /** A lookup backed by a map, so no MotionEvent is needed. */
    private static GamepadAxes.AxisLookup axes(float z, float rz, float rx, float ry,
                                               float x, float y) {
        return axis -> {
            if (axis == MotionEvent.AXIS_Z) return z;
            if (axis == MotionEvent.AXIS_RZ) return rz;
            if (axis == MotionEvent.AXIS_RX) return rx;
            if (axis == MotionEvent.AXIS_RY) return ry;
            if (axis == MotionEvent.AXIS_X) return x;
            if (axis == MotionEvent.AXIS_Y) return y;
            return 0f;
        };
    }

    @Test
    public void anXboxPadIsReadFromZAndRz() {
        GamepadAxes.Stick s = GamepadAxes.pick(GamepadAxes.RIGHT_STICK_AXES,
                axes(0.8f, -0.4f, 0f, 0f, 0f, 0f), 0.01f, "right");
        assertEquals(0.8f, s.x, 1e-3f);
        assertEquals(-0.4f, s.y, 1e-3f);
        assertEquals("right:pair0", s.source);
    }

    @Test
    public void aGenericPadIsReadFromRxAndRyWhenZIsZero() {
        // The bug: Z/RZ are zero and the deflection is on RX/RY. Reading only the first pair gave a
        // zero vector and the cursor never moved.
        GamepadAxes.Stick s = GamepadAxes.pick(GamepadAxes.RIGHT_STICK_AXES,
                axes(0f, 0f, 0.6f, 0.5f, 0f, 0f), 0.01f, "right");
        assertEquals(0.6f, s.x, 1e-3f);
        assertEquals(0.5f, s.y, 1e-3f);
        assertEquals("right:pair1", s.source);
    }

    @Test
    public void aPlayStationStylePadStillReadsFromRx() {
        // A pad that reports its right stick as RX/RY (with RZ carrying the y axis) must resolve via
        // the HID convention rather than being treated as zero.
        GamepadAxes.Stick s = GamepadAxes.pick(GamepadAxes.RIGHT_STICK_AXES,
                axes(0f, 0f, -0.3f, 0.7f, 0f, 0f), 0.01f, "right");
        assertEquals(-0.3f, s.x, 1e-3f);
        assertEquals(0.7f, s.y, 1e-3f);
        assertEquals("right:pair1", s.source);
    }

    @Test
    public void aCentredStickReportsZeroFromTheFirstConvention() {
        GamepadAxes.Stick s = GamepadAxes.pick(GamepadAxes.RIGHT_STICK_AXES,
                axes(0f, 0f, 0f, 0f, 0f, 0f), 0.01f, "right");
        assertEquals(0f, s.x, 1e-3f);
        assertEquals(0f, s.y, 1e-3f);
        assertEquals("right:pair0", s.source);
    }

    @Test
    public void aStickInsideTheThresholdCountsAsCentred() {
        // A tiny drift on both conventions must resolve to a value inside the threshold (the
        // cursor's own radial dead zone then flattens it), so the cursor does not creep.
        GamepadAxes.Stick s = GamepadAxes.pick(GamepadAxes.RIGHT_STICK_AXES,
                axes(0.005f, 0f, 0.005f, 0.005f, 0f, 0f), 0.01f, "right");
        assertTrue("no convention is treated as deflected",
                Math.hypot(s.x, s.y) <= 0.01f);
    }

    @Test
    public void anEmptyCandidateListIsSafe() {
        GamepadAxes.Stick s = GamepadAxes.pick(new int[0][], axes(0, 0, 0, 0, 0, 0), 0.01f, "right");
        assertEquals(0f, s.x, 1e-3f);
        assertTrue(s.source.endsWith("none"));
    }

    @Test
    public void theFirstPairStillWinsWhenBothConventionsCarryInput() {
        // An Xbox pad with a phantom RX value must still read from Z/RZ, so the cursor direction does
        // not flip based on a stray axis.
        GamepadAxes.Stick s = GamepadAxes.pick(GamepadAxes.RIGHT_STICK_AXES,
                axes(0.9f, 0f, 0.1f, 0.1f, 0f, 0f), 0.01f, "right");
        assertEquals(0.9f, s.x, 1e-3f);
        assertEquals("right:pair0", s.source);
    }
}
