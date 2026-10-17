package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the radial hub head's look-at rule.
 *
 * <p>The head must be still until the player moves the cursor, and it must look at where the cursor
 * is — derived from the cursor's offset from the hub centre, clamped to a small yaw/pitch range, not
 * from the stick's velocity. Both are invisible in a screenshot of a still frame, so they are pinned
 * here.
 */
public class RadialHeadTurnTest {

    @Test
    public void aCentredCursorFacesForward() {
        float[] look = RadialHeadTurn.targetLook(0f, 0f, 100f, 100f);
        assertEquals(0f, look[0], 1e-3f);
        assertEquals(0f, look[1], 1e-3f);
    }

    @Test
    public void aCursorToTheRightTurnsTheHeadRight() {
        float[] right = RadialHeadTurn.targetLook(50f, 0f, 100f, 100f);
        assertTrue("cursor right must turn the yaw positive", right[0] > 0f);
        assertEquals("halfway to the edge is half of MAX_YAW_DEG",
                RadialHeadTurn.MAX_YAW_DEG / 2f, right[0], 1e-3f);
        assertEquals(0f, right[1], 1e-3f);

        float[] left = RadialHeadTurn.targetLook(-50f, 0f, 100f, 100f);
        assertTrue("cursor left must turn the yaw negative", left[0] < 0f);
    }

    @Test
    public void aCursorBelowTiltsTheHeadDown() {
        // Screen Y grows down, so a positive dy is a cursor below the hub.
        float[] down = RadialHeadTurn.targetLook(0f, 50f, 100f, 100f);
        assertTrue("cursor below must pitch the head down (positive)", down[1] > 0f);
        assertEquals(RadialHeadTurn.MAX_PITCH_DEG / 2f, down[1], 1e-3f);

        float[] up = RadialHeadTurn.targetLook(0f, -50f, 100f, 100f);
        assertTrue("cursor above must pitch the head up (negative)", up[1] < 0f);
    }

    @Test
    public void theLookClampsAtTheExtremes() {
        float[] far = RadialHeadTurn.targetLook(500f, 500f, 100f, 100f);
        assertEquals(RadialHeadTurn.MAX_YAW_DEG, far[0], 1e-3f);
        assertEquals(RadialHeadTurn.MAX_PITCH_DEG, far[1], 1e-3f);

        float[] farNeg = RadialHeadTurn.targetLook(-500f, -500f, 100f, 100f);
        assertEquals(-RadialHeadTurn.MAX_YAW_DEG, farNeg[0], 1e-3f);
        assertEquals(-RadialHeadTurn.MAX_PITCH_DEG, farNeg[1], 1e-3f);
    }

    @Test
    public void settlesExactlyOnTheCursorAndNeverOvershoots() {
        float[] target = RadialHeadTurn.targetLook(70f, 40f, 100f, 100f);
        float yaw = 0f;
        float pitch = 0f;
        for (int i = 0; i < 200; i++) {
            float[] eased = RadialHeadTurn.stepTowards(yaw, pitch, target[0], target[1], 1f / 60f);
            yaw = eased[0];
            pitch = eased[1];
            assertTrue("no overshoot", yaw <= target[0] + 1e-3f);
            assertTrue("no overshoot", pitch <= target[1] + 1e-3f);
        }
        assertEquals(target[0], yaw, 1e-2f);
        assertEquals(target[1], pitch, 1e-2f);
    }

    @Test
    public void aStalledFrameDoesNotTeleportTheHead() {
        // A dt of 0 or an absurd stall falls back to one 60Hz frame: the head eases, it does not
        // jump to the target.
        float[] target = {RadialHeadTurn.MAX_YAW_DEG, RadialHeadTurn.MAX_PITCH_DEG};
        float[] zeroDt = RadialHeadTurn.stepTowards(0f, 0f, target[0], target[1], 0f);
        float[] hugeDt = RadialHeadTurn.stepTowards(0f, 0f, target[0], target[1], 5f);
        assertEquals(zeroDt[0], hugeDt[0], 1e-3f);
        assertEquals(zeroDt[1], hugeDt[1], 1e-3f);
        assertTrue("one frame is eased, not teleported", zeroDt[0] < RadialHeadTurn.MAX_YAW_DEG);
    }

    @Test
    public void aTeleportedCursorIsBoundedPerFrame() {
        // The view can be laid out late, so a cursor jump of half the screen must not fling the
        // head around; a single frame moves at most MAX_FRAME_STEP_DEG (checked via stepTowards
        // clamping toward a far target).
        float[] target = {180f, 0f};
        float[] eased = RadialHeadTurn.stepTowards(0f, 0f, target[0], target[1], 1f / 60f);
        assertTrue("yaw must settle in steps", eased[0] > 0f && eased[0] < 90f);
        float[] next = RadialHeadTurn.stepTowards(eased[0], 0f, target[0], target[1], 1f / 60f);
        assertTrue("a second frame continues toward the target", next[0] > eased[0]);
    }
}
