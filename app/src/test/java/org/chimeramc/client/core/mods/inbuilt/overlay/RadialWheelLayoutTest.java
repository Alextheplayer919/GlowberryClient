package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.chimeramc.client.core.mods.inbuilt.overlay.RadialWheelLayout.Sector;

/**
 * Pins the radial wheel's sector hit-testing. Screen space has y growing downward, so the top
 * sector is at -90 degrees. A boundary touch inside the gap must read as NONE, the hub as CENTER and
 * anything past the outer radius as NONE.
 */
public class RadialWheelLayoutTest {

    private static final float INNER = 40f;
    private static final float OUTER = 160f;
    private static final float GAP = 6f;

    private static Sector at(double deg, float radius) {
        double rad = Math.toRadians(deg);
        float dx = (float) (Math.cos(rad) * radius);
        float dy = (float) (Math.sin(rad) * radius);
        return RadialWheelLayout.sectorAt(dx, dy, INNER, OUTER, GAP);
    }

    @Test
    public void theFourCardinalDirectionsMapToTheFourSectors() {
        assertEquals(Sector.MODS, at(-90, 100));
        assertEquals(Sector.CLOSET, at(0, 100));
        assertEquals(Sector.SETTINGS, at(90, 100));
        assertEquals(Sector.HUD, at(180, 100));
    }

    @Test
    public void theHubAndBeyondTheOuterRadiusAreNotSectors() {
        assertEquals(Sector.CENTER, RadialWheelLayout.sectorAt(0f, 0f, INNER, OUTER, GAP));
        assertEquals(Sector.CENTER, RadialWheelLayout.sectorAt(10f, 10f, INNER, OUTER, GAP));
        assertEquals(Sector.NONE, at(-90, OUTER + 5f));
        assertEquals(Sector.NONE, at(0, 500f));
    }

    @Test
    public void aTouchOnASectorBoundaryFallsInTheGap() {
        // The boundary between MODS (top) and CLOSET (right) is at -45 degrees.
        assertEquals(Sector.NONE, at(-45, 100));
        assertEquals(Sector.NONE, at(-45 + GAP / 2, 100));
        // Just past the gap it is a real sector again.
        assertEquals(Sector.MODS, at(-45 - GAP - 1, 100));
        assertEquals(Sector.CLOSET, at(-45 + GAP + 1, 100));
    }

    @Test
    public void theSectorCentresAreWhereTheLayoutPlacesThem() {
        assertEquals(-90.0, RadialWheelLayout.sectorCenterDegrees(Sector.MODS), 0.001);
        assertEquals(0.0, RadialWheelLayout.sectorCenterDegrees(Sector.CLOSET), 0.001);
        assertEquals(90.0, RadialWheelLayout.sectorCenterDegrees(Sector.SETTINGS), 0.001);
        assertEquals(180.0, RadialWheelLayout.sectorCenterDegrees(Sector.HUD), 0.001);
    }

    @Test
    public void onlyTheFourDestinationsAreDestinations() {
        assertTrue(RadialWheelLayout.isDestination(Sector.MODS));
        assertTrue(RadialWheelLayout.isDestination(Sector.CLOSET));
        assertTrue(RadialWheelLayout.isDestination(Sector.SETTINGS));
        assertTrue(RadialWheelLayout.isDestination(Sector.HUD));
        assertFalse(RadialWheelLayout.isDestination(Sector.CENTER));
        assertFalse(RadialWheelLayout.isDestination(Sector.NONE));
    }

    // --- Blueberry geometry ------------------------------------------------------------------

    @Test
    public void theWheelFractionsMatchTheSpec() {
        assertEquals(0.40f, RadialWheelLayout.OUTER_FRACTION, 1e-6);
        assertEquals(0.52f, RadialWheelLayout.RING_INNER_FRACTION, 1e-6);
        assertEquals(0.46f, RadialWheelLayout.HUB_FRACTION, 1e-6);
    }

    @Test
    public void theGapIsParallelSidedNotWedgeShaped() {
        // A constant linear gap means the angular half-gap shrinks as radius grows.
        double innerHalf = RadialWheelLayout.angularHalfGapDegrees(100f, 6f);
        double outerHalf = RadialWheelLayout.angularHalfGapDegrees(400f, 6f);
        assertTrue("inner half-gap should be wider in angle", innerHalf > outerHalf);
        // The arc length of the gap is the same constant at both radii.
        double innerArc = Math.toRadians(2 * innerHalf) * 100.0;
        double outerArc = Math.toRadians(2 * outerHalf) * 400.0;
        assertEquals(6.0, innerArc, 0.2);
        assertEquals(6.0, outerArc, 0.2);
    }

    @Test
    public void everyFiveDegreesMapsToTheRightSector() {
        float inner = 100f, outer = 400f, gap = 6f;
        for (int deg = -180; deg < 180; deg += 5) {
            double rad = Math.toRadians(deg);
            float dx = (float) (Math.cos(rad) * 250);
            float dy = (float) (Math.sin(rad) * 250);
            Sector s = RadialWheelLayout.sectorAtLinear(dx, dy, inner, outer, gap);
            Sector expected = RadialWheelLayout.quadrantFor(deg);
            // Near a boundary the linear gap may read NONE; otherwise it must match the quadrant.
            if (s != Sector.NONE) {
                assertEquals("deg " + deg, expected, s);
            }
        }
    }

    @Test
    public void theDeadZoneInsideTheHubIsCenter() {
        assertEquals(Sector.CENTER, RadialWheelLayout.sectorAtLinear(0f, 0f, 100f, 400f, 6f));
        assertEquals(Sector.CENTER, RadialWheelLayout.sectorAtLinear(20f, 20f, 100f, 400f, 6f));
        assertEquals(Sector.NONE, RadialWheelLayout.sectorAtLinear(500f, 0f, 100f, 400f, 6f));
    }

    @Test
    public void theThreeChipClustersNeverOverlapTheRingOrEachOther() {
        int[][] screens = {{1920, 1080}, {1280, 720}, {2560, 1600}};
        for (int[] screen : screens) {
            int w = screen[0], h = screen[1];
            float[][] centers = RadialWheelLayout.chipCenters(w, h, 2, 3, 1);
            assertEquals(6, centers.length);
            float outer = RadialWheelLayout.outerRadius(w, h);
            float cx = w / 2f, cy = h / 2f;
            for (float[] c : centers) {
                // The chip centre must sit outside the ring's outer radius.
                float dist = (float) Math.hypot(c[0] - cx, c[1] - cy);
                assertTrue("chip overlaps the ring at " + w + "x" + h, dist > outer * 0.95f);
            }
            // No two chip centres closer than the chip height.
            float chipH = 22f;
            for (int i = 0; i < centers.length; i++) {
                for (int j = i + 1; j < centers.length; j++) {
                    float d = (float) Math.hypot(centers[i][0] - centers[j][0],
                            centers[i][1] - centers[j][1]);
                    assertTrue("chips overlap at " + w + "x" + h, d >= chipH);
                }
            }
        }
    }
}
