package org.chimeramc.client.ui.components;

/**
 * The orbiting accent particles around the launch head pod, as pure arithmetic.
 *
 * <p>The spec asks for "very subtle, tiny glass-like particles [that] drift slowly around the pod's
 * outer edge ... at varying speeds". They are computed from the clock rather than stored as mutable
 * state, so a dropped frame cannot make them jump and the view allocates nothing per frame.
 *
 * <p>Android-free so the orbit rule — every particle stays on the ring, speeds differ, and the
 * pattern repeats — is unit-tested.
 */
public final class PodParticles {

    /** How many particles orbit the pod. Sparse on purpose; more reads as clutter. */
    public static final int COUNT = 7;
    /** The orbit radius as a fraction of the pod radius (just outside the rim). */
    public static final float ORBIT_FRACTION = 1.06f;
    /** The slowest orbit period, ms. */
    public static final long SLOWEST_PERIOD_MS = 14000L;
    /** The extra period variation between neighbouring particles, ms. */
    public static final long PERIOD_STEP_MS = 1700L;

    private PodParticles() {
    }

    /** The orbit period of particle {@code index}, in ms. */
    public static long periodMs(int index) {
        int i = Math.max(0, index) % COUNT;
        return SLOWEST_PERIOD_MS - i * PERIOD_STEP_MS;
    }

    /**
     * The angle of particle {@code index} at a time, in degrees.
     *
     * <p>Each particle starts offset from the previous one so the ring is never a synchronised
     * marching band.
     */
    public static float angleDeg(int index, long timeMs) {
        int i = Math.max(0, index) % COUNT;
        long period = Math.max(1L, periodMs(i));
        long phase = ((timeMs % period) + period) % period;
        float base = 360f * i / COUNT;
        return (base + 360f * (phase / (float) period)) % 360f;
    }

    /**
     * The alpha of particle {@code index}, 0..1.
     *
     * <p>Faded in and out over its own orbit so a particle appears to fade rather than pop when it
     * wraps, and staggered per particle so they do not all dim together.
     */
    public static float alpha(int index, long timeMs) {
        int i = Math.max(0, index) % COUNT;
        long period = Math.max(1L, periodMs(i));
        long phase = ((timeMs % period) + period) % period;
        double t = phase / (double) period;
        // One sine hump per orbit, floored so a particle is never fully invisible.
        float a = (float) (0.35 + 0.35 * (0.5 - 0.5 * Math.cos(t * 2.0 * Math.PI)));
        return Math.max(0f, Math.min(1f, a));
    }

    /**
     * Fills {@code outX}/{@code outY} with the particle positions on the pod's rim.
     *
     * @param outX,outY arrays of at least {@link #COUNT} floats (reused by the caller)
     */
    public static void positions(float cx, float cy, float podRadius, long timeMs,
                                 float[] outX, float[] outY) {
        float r = podRadius * ORBIT_FRACTION;
        for (int i = 0; i < COUNT; i++) {
            double a = Math.toRadians(angleDeg(i, timeMs));
            outX[i] = cx + (float) (Math.cos(a) * r);
            outY[i] = cy + (float) (Math.sin(a) * r);
        }
    }
}
