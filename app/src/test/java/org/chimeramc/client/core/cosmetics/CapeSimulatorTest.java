package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Shape and motion tests for the rigid cape panel.
 *
 * <p>The cape is one flat quad hinged at the shoulders, so these pin the properties that make it
 * read as fabric rather than a board: the rest shape matches Minecraft's cape dimensions and hangs
 * straight, a still player's cape does not move at all, and moving leans it back through a single
 * smoothed swing that settles instead of ringing.
 */
public class CapeSimulatorTest {

    private static final float EPS = 0.02f;

    @Test
    public void restWidthMatchesMinecraftCapeWidth() {
        // Minecraft's cape texture is 10 px wide out of 16, so the panel is 0.625 blocks across.
        assertEquals(0.625f, CapeSimulator.WIDTH_BLOCKS, 1e-5f);
        assertEquals(1f, CapeSimulator.HEIGHT_BLOCKS, 1e-5f);
    }

    @Test
    public void restShapeHangsBelowTheAnchor() {
        CapeSimulator sim = new CapeSimulator();
        sim.reset(0f, 0f, 0f);

        // The top row is pinned at the anchor, everything below hangs downward.
        for (int c = 0; c < CapeSimulator.COLS; c++) {
            assertEquals("top row must sit on the anchor", 0f, sim.y(c), EPS);
        }
        int bottomLeft = (CapeSimulator.ROWS - 1) * CapeSimulator.COLS;
        assertTrue("the hem must hang below the anchor", sim.y(bottomLeft) < -0.9f);
    }

    @Test
    public void restShapeIsHorizontalAtTheTop() {
        CapeSimulator sim = new CapeSimulator();
        sim.reset(0f, 0f, 0f);
        float left = sim.x(0);
        float right = sim.x(CapeSimulator.COLS - 1);
        assertEquals(-CapeSimulator.WIDTH_BLOCKS / 2f, left, EPS);
        assertEquals(CapeSimulator.WIDTH_BLOCKS / 2f, right, EPS);
    }

    /**
     * A cape on a still player must not move at all. The old Verlet cloth kept swaying on its own
     * breeze, which is what made the cape look like a flag; the rigid panel has no self-motion.
     */
    @Test
    public void aStillPlayerCapeDoesNotMoveAtAll() {
        CapeSimulator sim = new CapeSimulator();
        sim.reset(0f, 0f, 0f);
        float x0 = sim.x(0), y0 = sim.y(0), z0 = sim.z(0);
        int hem = (CapeSimulator.ROWS - 1) * CapeSimulator.COLS;
        float hx = sim.x(hem), hy = sim.y(hem), hz = sim.z(hem);
        for (int i = 0; i < 600; i++) {
            sim.step(CapeSimulator.FIXED_DT, 0f, 0f, 0f, 0f, 0f, 0f);
        }
        assertEquals("a still cape must not drift", x0, sim.x(0), 1e-4f);
        assertEquals(y0, sim.y(0), 1e-4f);
        assertEquals(z0, sim.z(0), 1e-4f);
        assertEquals("a still cape must not sway", hx, sim.x(hem), 1e-4f);
        assertEquals(hy, sim.y(hem), 1e-4f);
        assertEquals(hz, sim.z(hem), 1e-4f);
    }

    /** Moving leans the hem back: the hem swings to a positive Z (behind the player). */
    @Test
    public void movingLeansTheHemBack() {
        CapeSimulator sim = new CapeSimulator();
        sim.reset(0f, 0f, 0f);
        int hem = (CapeSimulator.ROWS - 1) * CapeSimulator.COLS;
        for (int i = 0; i < 120; i++) {
            sim.step(CapeSimulator.FIXED_DT, 0f, 0f, 0f, 0f, 0f, CapeSimulator.FULL_SWING_SPEED);
        }
        assertTrue("a running player's cape must lean back", sim.z(hem) > 0.1f);
        assertTrue("the hem must still hang below the hinge", sim.y(hem) < 0f);
    }

    /** The swing eases out; it does not snap back the instant the player stops. */
    @Test
    public void theSwingIsSmoothedNotInstant() {
        CapeSimulator sim = new CapeSimulator();
        sim.reset(0f, 0f, 0f);
        int hem = (CapeSimulator.ROWS - 1) * CapeSimulator.COLS;
        // One frame of running barely moves the panel...
        sim.step(CapeSimulator.FIXED_DT, 0f, 0f, 0f, 0f, 0f, CapeSimulator.FULL_SWING_SPEED);
        float afterOneFrame = sim.z(hem);
        // ...but sustained running settles it fully back.
        for (int i = 0; i < 240; i++) {
            sim.step(CapeSimulator.FIXED_DT, 0f, 0f, 0f, 0f, 0f, CapeSimulator.FULL_SWING_SPEED);
        }
        float settled = sim.z(hem);
        assertTrue("the first frame must not snap to full lean", afterOneFrame < settled * 0.5f);
        assertTrue("sustained running must reach a real lean", settled > 0.15f);
    }

    @Test
    public void aLongStallDoesNotExplodeTheCape() {
        CapeSimulator sim = new CapeSimulator();
        sim.reset(0f, 0f, 0f);
        // A five-second hitch must be clamped, not integrated in one giant slice.
        sim.step(5f, 0f, 0f, 0f, 0f, 0f, 10f);
        for (int i = 0; i < sim.size(); i++) {
            assertTrue("finite position after a stall", Float.isFinite(sim.x(i)));
            assertTrue("finite position after a stall", Float.isFinite(sim.y(i)));
            assertTrue("finite position after a stall", Float.isFinite(sim.z(i)));
            assertTrue("cape must stay near the anchor", Math.abs(sim.y(i)) < 5f);
        }
    }

    @Test
    public void anchoredParticlesFollowTheirAnchor() {
        CapeSimulator sim = new CapeSimulator();
        sim.reset(0f, 0f, 0f);
        sim.step(CapeSimulator.FIXED_DT, 10f, 5f, 2f, 0f, 0f, 0f);
        // The top row is pinned: moving the anchor moves those particles with it exactly.
        assertEquals(10f - CapeSimulator.WIDTH_BLOCKS / 2f, sim.x(0), 0.02f);
        assertEquals(5f, sim.y(0), 0.02f);
        assertEquals(2f, sim.z(0), 0.02f);
    }

    @Test
    public void gridHasTheStatedSize() {
        CapeSimulator sim = new CapeSimulator();
        assertEquals(CapeSimulator.COLS * CapeSimulator.ROWS, sim.size());
    }

    /** The panel is rigid: every particle rotates by the same angle, so rows stay parallel. */
    @Test
    public void thePanelIsRigidNotCloth() {
        CapeSimulator sim = new CapeSimulator();
        sim.reset(0f, 0f, 0f);
        for (int i = 0; i < 200; i++) {
            sim.step(CapeSimulator.FIXED_DT, 0f, 0f, 0f, 0f, 0f, CapeSimulator.FULL_SWING_SPEED);
        }
        // The left and right edges of the hem must share the same depth (no twist, no shear).
        int left = (CapeSimulator.ROWS - 1) * CapeSimulator.COLS;
        int right = left + CapeSimulator.COLS - 1;
        assertEquals(sim.z(left), sim.z(right), 1e-3f);
        assertEquals(sim.y(left), sim.y(right), 1e-3f);
        // The top row is still pinned on the hinge.
        assertEquals(0f, sim.z(0), 1e-3f);
        assertEquals(0f, sim.y(0), 1e-3f);
    }
}
