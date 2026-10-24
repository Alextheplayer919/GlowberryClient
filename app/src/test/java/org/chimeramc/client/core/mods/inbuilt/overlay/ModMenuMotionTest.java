package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the two motion schemes (M5, M6, U11).
 *
 * <p>Reduced motion must still be a transition — an instant pop is not "reduced motion", it is a
 * missing animation — but it must have no spring travel.
 */
public class ModMenuMotionTest {

    @Test
    public void fullMotionIsLongerThanReducedMotion() {
        assertTrue(ModMenuMotion.durationMs(false) > ModMenuMotion.durationMs(true));
    }

    @Test
    public void reducedMotionIsStillATransitionNotAnInstantPop() {
        assertTrue(ModMenuMotion.REDUCED_MS > 0);
        assertEquals(ModMenuMotion.REDUCED_MS, ModMenuMotion.durationMs(true));
    }

    @Test
    public void theReducedEasingIsLinear() {
        assertEquals(0.5f, ModMenuMotion.reducedInterpolation(0.5f), 0.0001f);
        assertEquals(0.25f, ModMenuMotion.reducedInterpolation(0.25f), 0.0001f);
        assertEquals(0f, ModMenuMotion.reducedInterpolation(-1f), 0.0001f);
        assertEquals(1f, ModMenuMotion.reducedInterpolation(4f), 0.0001f);
    }
}
