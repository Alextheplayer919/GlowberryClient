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
}
