package org.chimeramc.client.ui.components;

/**
 * The head pod's entrance: a spring pop from zero with the bust fading in behind it.
 *
 * <p>The spec asks for "the head pod scales up from 0 with a spring-like pop, and the head model
 * fades in from transparency, settling into its idle rotation after a brief, smooth ease-out". A
 * single spring drives the scale, and the head's alpha follows a shorter, later curve so the disc
 * arrives first and the bust resolves into it rather than both appearing at once.
 *
 * <p>Pure and clock-free (it takes elapsed time), so "it overshoots once then settles", "it ends
 * exactly at rest" and "the head trails the disc" are unit-tested.
 */
public final class PodEntrance {

    /** Total entrance duration, ms. */
    public static final long DURATION_MS = 620L;
    /** The spring stiffness; higher settles faster. */
    private static final float STIFFNESS = 190f;
    /** The damping ratio; below 1 overshoots once, which is the "pop". */
    private static final float DAMPING = 0.62f;
    /** How far into the entrance the head starts fading in (fraction of the duration). */
    private static final float HEAD_DELAY = 0.18f;

    private PodEntrance() {
    }

    /**
     * The scale at an elapsed time, 0..~1.08..1.
     *
     * <p>A critically-ish damped spring: it rises past 1 once, comes back, and settles at exactly 1
     * by {@link #DURATION_MS} so nothing is left mid-overshoot when the animation ends.
     */
    public static float scaleAt(long elapsedMs) {
        if (elapsedMs <= 0L) return 0f;
        if (elapsedMs >= DURATION_MS) return 1f;
        float t = elapsedMs / 1000f;
        float w = (float) Math.sqrt(STIFFNESS);
        float envelope = (float) Math.exp(-DAMPING * w * t);
        float value = 1f - envelope * (float) Math.cos(w * (float) Math.sqrt(1f - DAMPING * DAMPING) * t);
        // Fade the very start in from 0 so the pop does not begin at full size.
        float start = Math.min(1f, elapsedMs / 90f);
        return Math.max(0f, value * start);
    }

    /** The head's alpha at an elapsed time, 0..1. Trails the disc by {@link #HEAD_DELAY}. */
    public static float headAlphaAt(long elapsedMs) {
        if (elapsedMs <= 0L) return 0f;
        float delay = DURATION_MS * HEAD_DELAY;
        if (elapsedMs <= delay) return 0f;
        float span = DURATION_MS - delay;
        float t = (elapsedMs - delay) / span;
        if (t >= 1f) return 1f;
        // Ease-out, so the bust resolves rather than snapping to full opacity.
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    /** True once the entrance has fully settled. */
    public static boolean isSettled(long elapsedMs) {
        return elapsedMs >= DURATION_MS;
    }
}
