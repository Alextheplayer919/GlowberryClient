package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.chimeramc.client.core.mods.inbuilt.overlay.ModMenuSettings.Density;

/**
 * Pins the Mod Menu's fixed geometry (M4, M5).
 *
 * <p>These are spec numbers, not preferences. A layout edit that quietly changes the row height or
 * the panel radius fails here rather than on a device.
 */
public class ModMenuMetricsTest {

    @Test
    public void theRowHeightsMatchTheSpec() {
        assertEquals(64, ModMenuMetrics.ROW_COMFORTABLE_DP);
        assertEquals(56, ModMenuMetrics.ROW_COMPACT_DP);
        assertEquals(ModMenuMetrics.ROW_COMFORTABLE_DP, ModMenuMetrics.rowHeightFor(Density.COMFORTABLE));
        assertEquals(ModMenuMetrics.ROW_COMPACT_DP, ModMenuMetrics.rowHeightFor(Density.COMPACT));
    }

    @Test
    public void aNullDensityFallsBackToTheComfortableRow() {
        assertEquals(ModMenuMetrics.ROW_COMFORTABLE_DP, ModMenuMetrics.rowHeightFor(null));
    }

    @Test
    public void thePanelRadiusMatchesTheSharedRadiusScale() {
        assertEquals(28, ModMenuMetrics.PANEL_RADIUS_DP);
    }

    @Test
    public void theHairlineIsAHalfDpRule() {
        assertEquals(0.5f, ModMenuMetrics.HAIRLINE_DP, 0f);
    }

    @Test
    public void theTouchTargetFloorIsTheAndroidMinimum() {
        assertEquals(48, ModMenuMetrics.MIN_TOUCH_TARGET_DP);
        assertTrue(ModMenuMetrics.needsInvisiblePadding(24));
        assertTrue(ModMenuMetrics.needsInvisiblePadding(40));
    }

    @Test
    public void theActionChevronIsSmallerThanARowIcon() {
        assertTrue(ModMenuMetrics.ACTION_CHEVRON_DP < ModMenuMetrics.ROW_ICON_DP);
    }
}
