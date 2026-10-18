package org.chimeramc.client.ui.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Pins the pod entrance spring: one overshoot, settles at rest, head trails the disc. Pure JVM. */
public class PodEntranceTest {

    @Test
    public void itStartsAtZeroAndEndsAtRest() {
        assertEquals(0f, PodEntrance.scaleAt(0L), 0.001f);
        assertEquals(1f, PodEntrance.scaleAt(PodEntrance.DURATION_MS), 0.0001f);
        assertEquals(1f, PodEntrance.scaleAt(PodEntrance.DURATION_MS + 5000L), 0.0001f);
    }

    @Test
    public void itOvershootsExactlyOnceThenSettles() {
        int crossings = 0;
        boolean above = false;
        for (long t = 0; t <= PodEntrance.DURATION_MS; t += 4) {
            float s = PodEntrance.scaleAt(t);
            if (s > 1.001f && !above) {
                above = true;
                crossings++;
            } else if (s < 0.999f && above) {
                above = false;
            }
        }
        assertEquals("the pop should cross 1 exactly once", 1, crossings);
    }

    @Test
    public void itNeverGoesNegativeOrAbsurd() {
        for (long t = 0; t <= PodEntrance.DURATION_MS + 200; t += 2) {
            float s = PodEntrance.scaleAt(t);
            assertTrue("scale out of range at " + t + ": " + s, s >= 0f && s <= 1.2f);
        }
    }

    @Test
    public void theHeadTrailsTheDisc() {
        // At the start the disc is visible and the head is not.
        assertTrue(PodEntrance.scaleAt(60L) > 0f);
        assertEquals(0f, PodEntrance.headAlphaAt(60L), 0.0001f);
        // The head arrives later and finishes fully opaque.
        assertTrue(PodEntrance.headAlphaAt(PodEntrance.DURATION_MS / 2) > 0f);
        assertEquals(1f, PodEntrance.headAlphaAt(PodEntrance.DURATION_MS), 0.0001f);
    }

    @Test
    public void theHeadAlphaIsMonotonic() {
        float last = -1f;
        for (long t = 0; t <= PodEntrance.DURATION_MS; t += 8) {
            float a = PodEntrance.headAlphaAt(t);
            assertTrue("head alpha went backwards at " + t, a >= last - 0.0001f);
            last = a;
        }
    }

    @Test
    public void settledFollowsTheDuration() {
        assertFalse(PodEntrance.isSettled(PodEntrance.DURATION_MS - 1));
        assertTrue(PodEntrance.isSettled(PodEntrance.DURATION_MS));
    }
}
