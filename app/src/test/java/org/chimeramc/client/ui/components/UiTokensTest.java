package org.chimeramc.client.ui.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the shared UI tokens: the exact palette values, the contrast ratios the spec requires
 * (text.primary on bg.surface &gt;= 7:1, text.secondary &gt;= 4.5:1) and the toggle state machine.
 * Pure JVM — no device, and no Android graphics calls.
 */
public class UiTokensTest {

    @Test
    public void thePaletteMatchesTheSpec() {
        assertEquals(0xFF17191D, UiTokens.BG_SURFACE);
        assertEquals(0xFF1E2126, UiTokens.BG_SURFACE_HOVER);
        assertEquals(0xFF0F5BE4, UiTokens.ACCENT_500);
        assertEquals(0xFF3682E2, UiTokens.ACCENT_400);
        assertEquals(0xFF71ACEF, UiTokens.ACCENT_300);
        assertEquals(0xFF10254F, UiTokens.ACCENT_900);
        assertEquals(0xFFE8EAED, UiTokens.TEXT_PRIMARY);
        assertEquals(0xFF9AA0A6, UiTokens.TEXT_SECONDARY);
        assertEquals(0xFF5F6368, UiTokens.TEXT_DISABLED);
        assertEquals(0xFF2A2D33, UiTokens.TOGGLE_OFF_TRACK);
    }

    @Test
    public void theWindowIsNearlyOpaque() {
        int alpha = (UiTokens.BG_WINDOW >>> 24) & 0xFF;
        assertTrue("window should be ~0.96 alpha", alpha >= 240 && alpha <= 250);
    }

    @Test
    public void textPrimaryOnSurfaceMeetsWcagAaa() {
        double ratio = UiTokens.contrastRatio(UiTokens.TEXT_PRIMARY, UiTokens.BG_SURFACE);
        assertTrue("text.primary contrast was " + ratio, ratio >= 7.0);
    }

    @Test
    public void textSecondaryOnSurfaceMeetsWcagAa() {
        double ratio = UiTokens.contrastRatio(UiTokens.TEXT_SECONDARY, UiTokens.BG_SURFACE);
        assertTrue("text.secondary contrast was " + ratio, ratio >= 4.5);
    }

    @Test
    public void textPrimaryOnWindowMeetsWcagAa() {
        double ratio = UiTokens.contrastRatio(UiTokens.TEXT_PRIMARY, UiTokens.BG_WINDOW);
        assertTrue("text.primary on window contrast was " + ratio, ratio >= 4.5);
    }

    @Test
    public void blendAndAlphaArePureIntegerMaths() {
        assertEquals(0xFF000000, UiTokens.blend(0xFF000000, 0xFFFFFFFF, 0f));
        assertEquals(0xFFFFFFFF, UiTokens.blend(0xFF000000, 0xFFFFFFFF, 1f));
        // Half blend of black and white is ~0x808080.
        int mid = UiTokens.blend(0xFF000000, 0xFFFFFFFF, 0.5f);
        assertEquals(0x7F, (mid >>> 16) & 0xFF);
        assertEquals(128, UiTokens.withAlpha(0xFF0F5BE4, 128) >>> 24);
    }

    @Test
    public void theDefaultAccentIsBlue() {
        assertEquals(UiTokens.ACCENT_500, UiTokens.resolveAccent(null));
    }

    @Test
    public void toggleTrackColourReflectsState() {
        assertEquals(UiTokens.ACCENT_500, UiToggle.trackColor(UiTokens.ACCENT_500, true));
        assertEquals(UiTokens.TOGGLE_OFF_TRACK, UiToggle.trackColor(UiTokens.ACCENT_500, false));
    }

    @Test
    public void toggleThumbTravelsTheTrack() {
        float left = 100f, width = 36f, height = 20f, thumb = 16f, inset = 2f;
        float off = UiToggle.thumbCenterX(left, width, height, thumb, inset, false, 1f);
        float on = UiToggle.thumbCenterX(left, width, height, thumb, inset, true, 1f);
        assertEquals(left + inset + thumb / 2f, off, 0.01f);
        assertEquals(left + width - inset - thumb / 2f, on, 0.01f);
        assertTrue(on > off);
    }

    @Test
    public void toggleFractionClamps() {
        assertEquals(0f, UiToggle.thumbFraction(false, 1f), 0.001f);
        assertEquals(1f, UiToggle.thumbFraction(true, 1f), 0.001f);
        assertEquals(0.5f, UiToggle.thumbFraction(true, 0.5f), 0.001f);
    }

    @Test
    public void theTouchTargetsMeetTheMinimum() {
        assertTrue("row height must be a comfortable touch target",
                UiTokens.ROW_HEIGHT >= 44);
        assertTrue("the toggle must be at least 48dp wide with its row padding",
                UiTokens.TOGGLE_WIDTH + 24 >= 48);
    }

    @Test
    public void motionDurationsStayWithinTheCap() {
        assertTrue(UiTokens.MOTION_DEFAULT_MS <= UiTokens.MOTION_MAX_MS);
        assertTrue(UiTokens.MOTION_PRESS_MS <= UiTokens.MOTION_MAX_MS);
        assertTrue(UiTokens.TOGGLE_SLIDE_MS <= UiTokens.MOTION_MAX_MS);
    }

    @Test
    public void chipDotColourReflectsState() {
        assertEquals(UiTokens.ACCENT_500, UiChip.dotColor(UiTokens.ACCENT_500, true));
        assertEquals(UiTokens.TEXT_DISABLED, UiChip.dotColor(UiTokens.ACCENT_500, false));
    }

    @Test
    public void chipWidthGrowsWithTheLabel() {
        float a = UiChip.width(20f, 1f);
        float b = UiChip.width(60f, 1f);
        assertTrue(b > a);
    }

    @Test
    public void segmentedHitTestingPicksTheRightSegment() {
        // 3 segments across [0, 300): segment width 100.
        assertEquals(0, UiSegmented.segmentAt(10f, 0f, 300f, 3));
        assertEquals(1, UiSegmented.segmentAt(150f, 0f, 300f, 3));
        assertEquals(2, UiSegmented.segmentAt(290f, 0f, 300f, 3));
        assertEquals(-1, UiSegmented.segmentAt(-5f, 0f, 300f, 3));
        assertEquals(-1, UiSegmented.segmentAt(305f, 0f, 300f, 3));
    }

    @Test
    public void sliderMapsValueToThumb() {
        assertEquals(0f, UiSlider.thumbX(0f, 0f, 100f, 0f, 200f), 0.001f);
        assertEquals(200f, UiSlider.thumbX(100f, 0f, 100f, 0f, 200f), 0.001f);
        assertEquals(100f, UiSlider.thumbX(50f, 0f, 100f, 0f, 200f), 0.001f);
    }

    @Test
    public void windowBoundsAreCentredAndCapped() {
        // On a very wide screen the 720dp cap applies.
        float[] wide = UiWindow.boundsArray(4000, 2000, 1f);
        assertEquals(720f, wide[2] - wide[0], 0.5f);
        assertEquals((4000 - 720) / 2f, wide[0], 0.5f);
        // On a narrow screen the fraction applies.
        float[] narrow = UiWindow.boundsArray(400, 800, 1f);
        assertEquals(400 * 0.66f, narrow[2] - narrow[0], 0.5f);
        assertEquals(800 * 0.78f, narrow[3] - narrow[1], 0.5f);
    }
}
