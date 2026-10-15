package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the radial hub head's turn rule.
 *
 * <p>The head must be still until the player moves the cursor, and it must follow the direction the
 * cursor is pushed. Both are invisible in a screenshot of a still frame, so they are pinned here.
 */
public class RadialHeadTurnTest {

    @Test
    public void aZeroInputNeverTurnsTheHead() {
        float yaw = 42f;
        for (int i = 0; i < 600; i++) {
            yaw = RadialHeadTurn.step(yaw, 0f, 0f, 1f / 60f);
        }
        assertEquals("ten seconds of no input must not move the head", 42f, yaw, 1e-3f);
    }

    @Test
    public void aHorizontalPushTurnsTheHead() {
        float yaw = RadialHeadTurn.step(0f, 1f, 0f, 1f / 60f);
        assertTrue("pushing right must turn the head", yaw > 0f);
        // One 60Hz frame at full deflection is ~1.5 degrees.
        assertEquals(1.5f, yaw, 0.05f);

        float back = RadialHeadTurn.step(0f, -1f, 0f, 1f / 60f);
        assertTrue("pushing left must turn the other way", back > 357f && back < 360f);
    }

    @Test
    public void aDiagonalPushTurnsMoreThanAPureVerticalOne() {
        float diagonal = RadialHeadTurn.step(0f, 1f, 1f, 1f / 60f);
        float vertical = RadialHeadTurn.step(0f, 0f, 1f, 1f / 60f);
        assertTrue("a diagonal push must turn more than a vertical-only one",
                diagonal > vertical);
        assertTrue("a vertical push still nudges the head", vertical > 0f);
    }

    @Test
    public void yawAlwaysStaysInOneTurn() {
        float yaw = 0f;
        for (int i = 0; i < 2000; i++) {
            yaw = RadialHeadTurn.step(yaw, 1f, 0.5f, 1f / 30f);
            assertTrue("yaw in range", yaw >= 0f && yaw < 360f);
        }
    }

    @Test
    public void aStalledFrameFallsBackToOneStep() {
        // A dt of 0 (a double event in one frame) or an absurd stall must not fling the head.
        float zeroDt = RadialHeadTurn.step(0f, 1f, 0f, 0f);
        float hugeDt = RadialHeadTurn.step(0f, 1f, 0f, 5f);
        assertEquals(zeroDt, hugeDt, 1e-3f);
        assertEquals(1.5f, zeroDt, 0.05f);
    }

    @Test
    public void negativeYawNormalisesIntoRange() {
        float yaw = RadialHeadTurn.step(0f, -1f, 0f, 1f / 60f);
        assertTrue(yaw >= 0f && yaw < 360f);
        assertEquals(358.5f, yaw, 0.05f);
    }
}
