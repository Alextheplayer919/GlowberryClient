package org.chimeramc.client.launcher.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.util.PersonalizationManager;
import org.junit.Test;

/**
 * Pins the Icon Size arithmetic. The preference was persisted but unused, so a regression here is
 * silent: the slider moves and nothing on screen changes.
 */
public class IconSizePolicyTest {

    @Test
    public void aFixedDimensionIsScalable() {
        assertTrue(IconSizePolicy.isScalableDimension(22));
        assertTrue(IconSizePolicy.isScalableDimension(1));
    }

    @Test
    public void layoutInstructionsAreNotScalable() {
        // WRAP_CONTENT (-2) and MATCH_PARENT (-1) are instructions, not sizes. Scaling one turns a
        // full-width row into a 22dp icon.
        assertFalse(IconSizePolicy.isScalableDimension(-1));
        assertFalse(IconSizePolicy.isScalableDimension(-2));
        assertFalse(IconSizePolicy.isScalableDimension(0));
    }

    @Test
    public void oneHundredPercentIsIdentity() {
        assertEquals(22, IconSizePolicy.scaledPx(22, 100));
        assertEquals(40, IconSizePolicy.scaledPx(40, 100));
    }

    @Test
    public void theRangeScalesUpAndDown() {
        assertEquals(33, IconSizePolicy.scaledPx(22, 150));
        assertEquals(11, IconSizePolicy.scaledPx(22, 50));
    }

    @Test
    public void theResultNeverDropsBelowOnePixel() {
        // A zero-sized view is invisible and reads as a missing icon rather than a small one.
        assertEquals(1, IconSizePolicy.scaledPx(1, 50));
    }

    @Test
    public void anOutOfRangePercentageIsClampedToThePreferencesOwnBounds() {
        assertEquals(
                IconSizePolicy.scaledPx(20, PersonalizationManager.ICON_SIZE_MAX),
                IconSizePolicy.scaledPx(20, 10_000));
        assertEquals(
                IconSizePolicy.scaledPx(20, PersonalizationManager.ICON_SIZE_MIN),
                IconSizePolicy.scaledPx(20, -50));
    }

    @Test
    public void anUnscalableDimensionIsReturnedUnchanged() {
        assertEquals(-1, IconSizePolicy.scaledPx(-1, 150));
        assertEquals(-2, IconSizePolicy.scaledPx(-2, 150));
    }
}
