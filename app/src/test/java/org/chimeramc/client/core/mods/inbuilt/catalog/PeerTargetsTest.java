package org.chimeramc.client.core.mods.inbuilt.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * The nearest-peer rule behind the catalogue's target readouts.
 *
 * <p>Pins the honest-empty-state contract: with no local position or no peers the rule returns null
 * (so the readout hides), a non-finite peer is skipped rather than drawn at an undefined spot, and
 * the nearest of several peers is chosen.
 */
public class PeerTargetsTest {

    private static PeerTargets.Peer peer(String id, String name, float x, float y, float z) {
        return new PeerTargets.Peer(id, name, x, y, z);
    }

    @Test
    public void theNearestPeerWins() {
        PeerTargets.Nearest nearest = PeerTargets.nearest(new float[]{0f, 0f, 0f},
                Arrays.asList(peer("a", "Alex", 30f, 0f, 0f), peer("b", "Bob", 4f, 0f, 0f)));
        assertNotNull(nearest);
        assertEquals("b", nearest.id);
        assertEquals(4f, nearest.distanceBlocks, 1e-3f);
    }

    @Test
    public void noLocalPositionReadsAsNoTarget() {
        assertNull(PeerTargets.nearest(null, Arrays.asList(peer("a", "Alex", 1f, 0f, 0f))));
        assertNull(PeerTargets.nearest(new float[]{0f}, Arrays.asList(peer("a", "Alex", 1f, 0f, 0f))));
    }

    @Test
    public void noPeersReadsAsNoTarget() {
        assertNull(PeerTargets.nearest(new float[]{0f, 0f, 0f}, Collections.emptyList()));
        assertNull(PeerTargets.nearest(new float[]{0f, 0f, 0f}, null));
    }

    @Test
    public void aNonFinitePeerIsSkipped() {
        PeerTargets.Nearest nearest = PeerTargets.nearest(new float[]{0f, 0f, 0f},
                Arrays.asList(peer("bad", "Ghost", Float.NaN, 0f, 0f),
                        peer("good", "Alex", 5f, 0f, 0f)));
        assertNotNull(nearest);
        assertEquals("good", nearest.id);
    }

    @Test
    public void onlyNonFinitePeersReadAsNoTarget() {
        assertNull(PeerTargets.nearest(new float[]{0f, 0f, 0f},
                Collections.singletonList(peer("bad", "Ghost", 0f, Float.POSITIVE_INFINITY, 0f))));
    }

    @Test
    public void describeUsesTheNameAndDistance() {
        String line = PeerTargets.describe(
                PeerTargets.nearest(new float[]{0f, 0f, 0f}, Collections.singletonList(
                        peer("a", "Alex", 3f, 4f, 0f))));
        assertNotNull(line);
        assertEquals("Alex 5.0 blocks", line);
    }

    @Test
    public void describeFallsBackToTheIdWhenTheNameIsBlank() {
        String line = PeerTargets.describe(
                PeerTargets.nearest(new float[]{0f, 0f, 0f}, Collections.singletonList(
                        peer("id42", "  ", 0f, 0f, 1f))));
        assertNotNull(line);
        org.junit.Assert.assertTrue(line.startsWith("id42"));
    }

    @Test
    public void describeOfNoTargetIsNull() {
        assertNull(PeerTargets.describe(null));
    }
}
