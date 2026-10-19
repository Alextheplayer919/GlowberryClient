package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the N-sector radial geometry. The four-sector wheel's own test lives in
 * {@code RadialWheelLayoutTest}; this covers the generalisation the profile-driven wheel needs, and
 * the boundaries a screenshot cannot check: the sector centres for each count, the hub and off-wheel
 * rejects, and the parallel-sided gap.
 */
public class WheelLayoutTest {

    private static final float INNER = 40f;
    private static final float OUTER = 160f;
    private static final float GAP = 6f;

    private static int at(double deg, float radius, int count) {
        double rad = Math.toRadians(deg);
        return WheelLayout.sectorAt((float) (Math.cos(rad) * radius),
                (float) (Math.sin(rad) * radius), INNER, OUTER, GAP, count);
    }

    @Test
    public void sectorZeroIsAtTheTopAndSectorsRunClockwise() {
        // Screen space: y down, so the top is -90 degrees.
        assertEquals(0, at(-90, 100, 4));
        assertEquals(1, at(0, 100, 4));
        assertEquals(2, at(90, 100, 4));
        assertEquals(3, at(180, 100, 4));
    }

    @Test
    public void eightSectorsPlaceTheirCentresEveryFortyFiveDegrees() {
        assertEquals(45.0, WheelLayout.sectorSpanDegrees(8), 1e-9);
        for (int i = 0; i < 8; i++) {
            double expected = -90.0 + i * 45.0;
            assertEquals(expected, WheelLayout.sectorCenterDegrees(i, 8), 1e-9);
            assertEquals("sector " + i, i, at(expected, 100, 8));
        }
    }

    @Test
    public void aSingleSectorOwnsTheWholeRing() {
        assertEquals(360.0, WheelLayout.sectorSpanDegrees(1), 1e-9);
        // No internal boundary exists, so every angle in the band is that sector.
        assertEquals(0, at(-90, 100, 1));
        assertEquals(0, at(0, 100, 1));
        assertEquals(0, at(123, 100, 1));
        assertEquals(0, at(-179, 100, 1));
    }

    @Test
    public void theHubAndOutsideTheRingAreNotSectors() {
        assertEquals(-1, WheelLayout.sectorAt(0f, 0f, INNER, OUTER, GAP, 4));
        assertEquals(-1, WheelLayout.sectorAt(10f, 10f, INNER, OUTER, GAP, 8));
        assertEquals(-1, at(-90, OUTER + 5f, 4));
        assertEquals(-1, at(0, 500f, 8));
    }

    @Test
    public void aTouchOnASectorBoundaryFallsInTheParallelGap() {
        // With four sectors the boundary between sector 0 (top) and 1 (right) is at -45 degrees.
        // The gap is a linear pixel width, so its angular width at this radius is what the test
        // must clear — not the raw pixel value.
        double halfGapDeg = WheelLayout.angularHalfGapDegrees(100f, GAP);
        assertEquals(-1, at(-45, 100, 4));
        assertEquals(-1, at(-45 + halfGapDeg / 2, 100, 4));
        assertEquals(0, at(-45 - halfGapDeg - 1, 100, 4));
        assertEquals(1, at(-45 + halfGapDeg + 1, 100, 4));
    }

    @Test
    public void theGapIsParallelSidedSoItNarrowsInAngleWithRadius() {
        // A constant linear gap covers a smaller angle further out.
        double near = WheelLayout.angularHalfGapDegrees(50f, GAP);
        double far = WheelLayout.angularHalfGapDegrees(200f, GAP);
        assertTrue("the half-gap must shrink with radius", near > far);
        // At a radius where the gap spans the whole sector it can never exceed the sector's half.
        assertEquals(180.0, WheelLayout.angularHalfGapDegrees(0.5f, GAP), 1e-9);
    }

    @Test
    public void theSectorCountIsClampedIntoRange() {
        assertEquals(1, WheelLayout.clampSectorCount(0));
        assertEquals(1, WheelLayout.clampSectorCount(-5));
        assertEquals(8, WheelLayout.clampSectorCount(99));
        assertEquals(8, WheelLayout.clampSectorCount(WheelLayout.MAX_SECTORS));
    }

    @Test
    public void anIndexBeyondTheCountWraps() {
        assertEquals(WheelLayout.sectorCenterDegrees(0, 4),
                WheelLayout.sectorCenterDegrees(4, 4), 1e-9);
        assertEquals(WheelLayout.sectorCenterDegrees(1, 4),
                WheelLayout.sectorCenterDegrees(5, 4), 1e-9);
    }
}
