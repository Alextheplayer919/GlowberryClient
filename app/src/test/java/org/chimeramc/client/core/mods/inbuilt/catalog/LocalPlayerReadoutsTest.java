package org.chimeramc.client.core.mods.inbuilt.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * The local-player readouts: the arithmetic and formatting that turns the raw local-player feed
 * into the strings the catalogue overlays show.
 *
 * <p>Pure, so it is pinned here without a device. The compass mapping is the classic defect (a
 * mirrored compass looks fine until it sends a player the wrong way), so all four anchors are
 * asserted, along with the "no data" nulls that keep a readout from inventing a value.
 */
public class LocalPlayerReadoutsTest {

    @Test
    public void compassUsesMinecraftsYawMapping() {
        // Bedrock yaw 0 faces +Z (south) and increases clockwise: south=0, west=90, north=180,
        // east=270. Reversing this is the "my compass is mirrored" bug.
        assertEquals("S", LocalPlayerReadouts.cardinal(0f));
        assertEquals("W", LocalPlayerReadouts.cardinal(90f));
        assertEquals("N", LocalPlayerReadouts.cardinal(180f));
        assertEquals("E", LocalPlayerReadouts.cardinal(270f));
    }

    @Test
    public void compassWrapsNegativeAndOverThreeSixty() {
        assertEquals("S", LocalPlayerReadouts.cardinal(360f));
        assertEquals("S", LocalPlayerReadouts.cardinal(-360f));
        assertEquals("E", LocalPlayerReadouts.cardinal(-90f));
        assertEquals("W", LocalPlayerReadouts.cardinal(450f));
    }

    @Test
    public void missingPositionReadsAsNoData() {
        assertNull(LocalPlayerReadouts.coordinates(null));
        assertNull(LocalPlayerReadouts.coordinates(new float[]{1f, 2f}));
        assertNull(LocalPlayerReadouts.chunk(null));
        assertNull(LocalPlayerReadouts.chunkOrigin(null));
    }

    @Test
    public void coordinatesAndChunksAreFormatted() {
        float[] at = {12.34f, 64f, -20.5f};
        assertEquals("12.3, 64.0, -20.5", LocalPlayerReadouts.coordinates(at));
        // A chunk is a 16-block cell, floored, so a negative coordinate floors toward negative.
        assertEquals("0, -2", LocalPlayerReadouts.chunk(at));
        assertEquals("0, -32", LocalPlayerReadouts.chunkOrigin(at));
    }

    @Test
    public void speedRejectsNonFiniteValues() {
        assertNull(LocalPlayerReadouts.speed(Double.NaN));
        assertNull(LocalPlayerReadouts.speed(Double.POSITIVE_INFINITY));
        assertEquals("3.25 b/s", LocalPlayerReadouts.speed(3.25));
    }

    @Test
    public void fallWarningOnlyAppearsAboveThreeBlocks() {
        // Bedrock starts damaging above three blocks; a warning at three or below would cry wolf.
        assertNull(LocalPlayerReadouts.fallWarning(0));
        assertNull(LocalPlayerReadouts.fallWarning(3.0));
        assertNull(LocalPlayerReadouts.fallWarning(Double.NaN));
        assertEquals("Falling 8.5 blocks", LocalPlayerReadouts.fallWarning(8.5));
    }

    @Test
    public void healthRejectsImplausibleValues() {
        assertNull(LocalPlayerReadouts.health(null));
        assertNull(LocalPlayerReadouts.health(new float[]{Float.NaN}));
        assertNull(LocalPlayerReadouts.health(new float[]{-1f}));
        assertNull(LocalPlayerReadouts.health(new float[]{2000f}));
        assertEquals("20", LocalPlayerReadouts.health(new float[]{20f}));
    }
}
