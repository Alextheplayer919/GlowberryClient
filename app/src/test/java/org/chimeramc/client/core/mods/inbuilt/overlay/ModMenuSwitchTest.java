package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the pill switch's geometry, colour and state rules (M2, M5).
 *
 * <p>The switch is the control the whole phase exists for, so its travel, its off-track colour and
 * its disabled/failed appearances are asserted arithmetically rather than eyeballed on a device.
 */
public class ModMenuSwitchTest {

    private static final float DENSITY = 3f;

    @Test
    public void theThumbTravelsExactlyTheTrackMinusTheThumbAndBothInsets() {
        float expected = ModMenuMetrics.SWITCH_WIDTH_DP * DENSITY
                - ModMenuMetrics.SWITCH_THUMB_DP * DENSITY
                - 2f * ModMenuMetrics.SWITCH_THUMB_INSET_DP * DENSITY;
        assertEquals(expected, ModMenuSwitch.travel(DENSITY), 0.0001f);
    }

    @Test
    public void theThumbStartsAndEndsInsideItsTrack() {
        float off = ModMenuSwitch.thumbCenterX(DENSITY, 0f);
        float on = ModMenuSwitch.thumbCenterX(DENSITY, 1f);
        float trackLeft = 0f;
        float trackRight = ModMenuMetrics.SWITCH_WIDTH_DP * DENSITY;
        float half = ModMenuMetrics.SWITCH_THUMB_DP * DENSITY / 2f;
        assertTrue("off thumb clears the left edge", off - half >= trackLeft);
        assertTrue("on thumb clears the right edge", on + half <= trackRight + 0.0001f);
        assertTrue("the thumb actually moves", on > off);
    }

    @Test
    public void anOvershootingSpringCannotPushTheThumbOutOfItsTrack() {
        float half = ModMenuMetrics.SWITCH_THUMB_DP * DENSITY / 2f;
        float low = ModMenuSwitch.thumbCenterX(DENSITY, -0.6f);
        float high = ModMenuSwitch.thumbCenterX(DENSITY, 1.6f);
        assertTrue(low - half >= -0.0001f);
        assertTrue(high + half <= ModMenuMetrics.SWITCH_WIDTH_DP * DENSITY + 0.0001f);
    }

    @Test
    public void theTrackIsTheAccentWhenOnAndTheMutedAccentWhenOff() {
        assertEquals(ModMenuPalette.ACCENT, ModMenuSwitch.trackColor(true));
        assertEquals(ModMenuPalette.mutedTrack(), ModMenuSwitch.trackColor(false));
        assertNotEquals(ModMenuSwitch.trackColor(true), ModMenuSwitch.trackColor(false));
    }

    @Test
    public void theOffTrackIsTranslucentAndTheOnTrackIsOpaque() {
        assertTrue(((ModMenuSwitch.trackColor(false) >>> 24) & 0xFF) < 0xFF);
        assertEquals(0xFF, (ModMenuSwitch.trackColor(true) >>> 24) & 0xFF);
    }

    @Test
    public void aFailedRowGetsTheErrorTrackNotTheAccent() {
        int failed = ModMenuSwitch.failedTrackColor();
        assertNotEquals(ModMenuSwitch.trackColor(true), failed);
        assertNotEquals(ModMenuSwitch.trackColor(false), failed);
        // It is the error colour blended into the surface, so the red channel dominates.
        assertTrue(((failed >> 16) & 0xFF) > ((failed >> 8) & 0xFF));
    }

    @Test
    public void aDisabledRowGetsTheTertiaryThumb() {
        assertEquals(ModMenuPalette.TEXT_PRIMARY, ModMenuSwitch.thumbColor(true));
        assertEquals(ModMenuPalette.TEXT_TERTIARY, ModMenuSwitch.thumbColor(false));
    }

    @Test
    public void turningOnIsSpringierThanTurningOff() {
        // An enable is a commitment and the overshoot reads as confirmation; a disable retreats.
        assertTrue(ModMenuSwitch.dampingFor(true) < ModMenuSwitch.dampingFor(false));
        assertTrue(ModMenuSwitch.stiffnessFor(true) > ModMenuSwitch.stiffnessFor(false));
    }

    @Test
    public void theHapticFiresOnTheStateChangeAndNotOnThePress() {
        assertTrue(ModMenuSwitch.hapticsOnStateChange());
    }

    @Test
    public void theTrackMatchesTheSpecifiedSize() {
        assertEquals(52, ModMenuMetrics.SWITCH_WIDTH_DP);
        assertEquals(32, ModMenuMetrics.SWITCH_HEIGHT_DP);
        assertEquals(24, ModMenuMetrics.SWITCH_THUMB_DP);
        assertEquals(4, ModMenuMetrics.SWITCH_THUMB_INSET_DP);
    }

    @Test
    public void theSwitchNeedsInvisiblePaddingToReachTheMinimumTouchTarget() {
        // The visual is 32dp tall, below the 48dp minimum, so the view must reserve more.
        assertTrue(ModMenuMetrics.needsInvisiblePadding(ModMenuMetrics.SWITCH_HEIGHT_DP));
        assertFalse(ModMenuMetrics.needsInvisiblePadding(ModMenuMetrics.MIN_TOUCH_TARGET_DP));
    }
}
