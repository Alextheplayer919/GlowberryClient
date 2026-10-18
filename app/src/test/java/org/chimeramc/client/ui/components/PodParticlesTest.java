package org.chimeramc.client.ui.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Pins the pod particle orbit: on the ring, varying speeds, bounded alpha. Pure JVM. */
public class PodParticlesTest {

    @Test
    public void everyParticleStaysOnTheRing() {
        float[] xs = new float[PodParticles.COUNT];
        float[] ys = new float[PodParticles.COUNT];
        PodParticles.positions(100f, 50f, 40f, 1234L, xs, ys);
        float expected = 40f * PodParticles.ORBIT_FRACTION;
        for (int i = 0; i < PodParticles.COUNT; i++) {
            float r = (float) Math.hypot(xs[i] - 100f, ys[i] - 50f);
            assertEquals("particle " + i + " off the ring", expected, r, 0.01f);
        }
    }

    @Test
    public void neighbouringParticlesHaveDifferentPeriods() {
        assertNotEquals(PodParticles.periodMs(0), PodParticles.periodMs(1));
        assertNotEquals(PodParticles.periodMs(1), PodParticles.periodMs(2));
        assertTrue(PodParticles.periodMs(0) > PodParticles.periodMs(PodParticles.COUNT - 1));
    }

    @Test
    public void particlesAreSpreadNotStacked() {
        float[] xs = new float[PodParticles.COUNT];
        float[] ys = new float[PodParticles.COUNT];
        PodParticles.positions(0f, 0f, 10f, 0L, xs, ys);
        for (int i = 0; i < PodParticles.COUNT; i++) {
            for (int j = i + 1; j < PodParticles.COUNT; j++) {
                float d = (float) Math.hypot(xs[i] - xs[j], ys[i] - ys[j]);
                assertTrue("particles " + i + "/" + j + " overlap", d > 1f);
            }
        }
    }

    @Test
    public void alphaIsAlwaysBoundedAndNeverFullyInvisible() {
        for (int i = 0; i < PodParticles.COUNT; i++) {
            for (long t = 0; t < 40000L; t += 500) {
                float a = PodParticles.alpha(i, t);
                assertTrue("alpha out of range: " + a, a >= 0.3f && a <= 0.71f);
            }
        }
    }

    @Test
    public void theOrbitWrapsCleanly() {
        long period = PodParticles.periodMs(3);
        assertEquals(PodParticles.angleDeg(3, 0L), PodParticles.angleDeg(3, period), 0.001f);
    }

    @Test
    public void anOutOfRangeIndexWrapsRatherThanThrowing() {
        assertEquals(PodParticles.periodMs(0), PodParticles.periodMs(PodParticles.COUNT));
        assertTrue(PodParticles.angleDeg(-1, 100L) >= 0f);
    }
}
