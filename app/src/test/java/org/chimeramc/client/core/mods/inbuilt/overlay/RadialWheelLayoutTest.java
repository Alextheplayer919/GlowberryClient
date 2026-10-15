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

    // --- The enabled-module list ------------------------------------------------------------

    @Test
    public void theEnabledListIsOneCompactColumn() {
        // At a natural count the list must read as a grouped stack: every row exactly one row pitch
        // below the last, all sharing one right edge.
        float density = 3f;
        for (int[] screen : new int[][]{{1920, 1080}, {1280, 720}, {2560, 1600}, {800, 480}}) {
            int w = screen[0], h = screen[1];
            float[][] rows = RadialWheelLayout.enabledListCenters(w, h, 4, density);
            assertEquals(4, rows.length);
            float pitch = RadialWheelLayout.LIST_ROW_DP * density;
            for (int i = 1; i < rows.length; i++) {
                assertEquals("rows share one right edge", rows[0][0], rows[i][0], 1e-3f);
                assertEquals("rows are one compact pitch apart", pitch,
                        rows[i][1] - rows[i - 1][1], 1e-3f);
            }
            // The pitch must be no more than a row height plus a small gap: "too spaced out" was the
            // old three-cluster spread (every 0.10 of the screen height).
            assertTrue("row pitch is compact", pitch <= 30f * density);
        }
    }

    @Test
    public void theEnabledListStaysOnScreenAndClearOfTheRing() {
        float density = 2f;
        for (int[] screen : new int[][]{{1920, 1080}, {1280, 720}, {2560, 1600}, {800, 480}}) {
            int w = screen[0], h = screen[1];
            float[][] rows = RadialWheelLayout.enabledListCenters(w, h, 24, density);
            float outer = RadialWheelLayout.outerRadius(w, h);
            float cx = w / 2f, cy = h / 2f;
            for (float[] r : rows) {
                assertTrue("row stays on screen (x)", r[0] > 0f && r[0] <= w);
                assertTrue("row stays on screen (y)", r[1] >= 0f && r[1] <= h);
                assertEquals("rows sit in one column", rows[0][0], r[0], 1e-3f);
                // Rows must sit outside the inner edge of the ring band, not on top of a sector.
                float dist = (float) Math.hypot(r[0] - cx, r[1] - cy);
                assertTrue("column clears the ring at " + w + "x" + h, dist > outer * 0.55f);
            }
            // A long list is compressed, not allowed to overflow the bottom.
            assertTrue("last row stays inside the view", rows[rows.length - 1][1] < h);
            for (int i = 1; i < rows.length; i++) {
                assertTrue("rows never overlap", rows[i][1] > rows[i - 1][1]);
            }
        }
    }

    @Test
    public void anEmptyEnabledListHasNoRows() {
        assertEquals(0, RadialWheelLayout.enabledListCenters(1000, 600, 0, 2f).length);
        assertEquals(0, RadialWheelLayout.enabledListCenters(1000, 600, -3, 2f).length);
    }

    @Test
    public void aSingleRowSitsCentredInTheBand() {
        float[][] rows = RadialWheelLayout.enabledListCenters(1920, 1080, 1, 3f);
        assertEquals(1, rows.length);
        float mid = (RadialWheelLayout.LIST_BAND_TOP_FY + RadialWheelLayout.LIST_BAND_BOTTOM_FY) / 2f;
        assertEquals(1080 * mid, rows[0][1], 1f);
    }

    @Test
    public void theTwoColumnsSitOnOppositeSidesAndStayClearOfTheRing() {
        // The wheel shows a quick-options column on the left and the enabled column on the right.
        float density = 2f;
        for (int[] screen : new int[][]{{1920, 1080}, {1280, 720}, {2560, 1600}, {800, 480}}) {
            int w = screen[0], h = screen[1];
            float[][] left = RadialWheelLayout.moduleListCenters(w, h, 4, density, false);
            float[][] right = RadialWheelLayout.moduleListCenters(w, h, 4, density, true);
            assertEquals(4, left.length);
            assertEquals(4, right.length);
            float outer = RadialWheelLayout.outerRadius(w, h);
            float cx = w / 2f, cy = h / 2f;
            for (int i = 0; i < 4; i++) {
                assertTrue("left column is left of centre", left[i][0] < cx);
                assertTrue("right column is right of centre", right[i][0] > cx);
                // Both columns must sit outside the ring band so a row never lands on a sector.
                assertTrue("left row clears the ring",
                        Math.hypot(left[i][0] - cx, left[i][1] - cy) > outer * 0.55f);
                assertTrue("right row clears the ring",
                        Math.hypot(right[i][0] - cx, right[i][1] - cy) > outer * 0.55f);
                // And neither runs off its own edge.
                assertTrue(left[i][0] >= 0f);
                assertTrue(right[i][0] <= w);
            }
        }
    }
}
