package org.chimeramc.client.core.mods.inbuilt.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * The look-smoothing rule behind the catalogue's Rotation Smoother.
 *
 * <p>Pins the two properties that matter: fine aim is never damped (damping a micro-adjustment adds
 * latency to the track the player is settling onto), and a large swipe converges toward the input
 * without overshooting it.
 */
public class LookSmoothingTest {

    @Test
    public void microDeltasPassThroughUntouched() {
        float[] previous = new float[2];
        float[] out = LookSmoothing.smooth(0.05f, 0.04f, 0.5f, previous);
        assertEquals(0.05f, out[0], 1e-5f);
        assertEquals(0.04f, out[1], 1e-5f);
    }

    @Test
    public void aLargeSwipeIsDampedTowardThePreviousOutput() {
        float[] previous = new float[]{10f, 0f};
        float[] out = LookSmoothing.smooth(20f, 0f, 0.5f, previous);
        // Half of the previous output kept, so the result sits between 10 and 20.
        assertEquals(15f, out[0], 1e-4f);
    }

    @Test
    public void smoothingNeverOvershootsTheInput() {
        float[] previous = new float[]{0f, 0f};
        float[] out = LookSmoothing.smooth(20f, 20f, 0.9f, previous);
        org.junit.Assert.assertTrue(out[0] <= 20f);
        org.junit.Assert.assertTrue(out[1] <= 20f);
    }

    @Test
    public void zeroStrengthIsIdentity() {
        float[] previous = new float[]{5f, 5f};
        float[] out = LookSmoothing.smooth(20f, 20f, 0f, previous);
        assertEquals(20f, out[0], 1e-5f);
        assertEquals(20f, out[1], 1e-5f);
    }

    @Test
    public void aNullPreviousBufferIsTolerated() {
        float[] out = LookSmoothing.smooth(20f, 0f, 0.5f, null);
        assertNotNull(out);
    }

    @Test
    public void theOutputStaysFinite() {
        float[] previous = new float[2];
        float[] out = LookSmoothing.smooth(1e6f, -1e6f, 0.95f, previous);
        org.junit.Assert.assertTrue(Float.isFinite(out[0]));
        org.junit.Assert.assertTrue(Float.isFinite(out[1]));
    }
}
