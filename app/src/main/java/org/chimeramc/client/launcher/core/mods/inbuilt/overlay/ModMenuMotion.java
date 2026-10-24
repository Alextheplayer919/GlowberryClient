package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * The Mod Menu's motion vocabulary (M5, M6, U11).
 *
 * <p>Two schemes, one switch: {@code full} is a spring, {@code reduced} is a short linear fade. The
 * durations live here rather than at the call sites so "Reduce motion" is one decision, and the OS
 * accessibility setting (which the settings model already folds in) cannot be overridden by a
 * per-widget literal.
 *
 * <p>Deliberately Android-free: the reduced easing is exposed as a plain interpolation function
 * rather than an {@code android.view.animation.Interpolator}, because that class is a stub
 * off-device and the rule has to be unit-testable without one. The view wraps it when it needs a
 * real {@code Interpolator}.
 */
public final class ModMenuMotion {

    private ModMenuMotion() {
    }

    /** The full-motion switch duration: long enough for the spring's overshoot to read. */
    public static final long FULL_MS = 220L;
    /** The reduced-motion duration: a short linear fade, never an instant pop. */
    public static final long REDUCED_MS = 120L;

    /** The duration for the current scheme. */
    public static long durationMs(boolean reduced) {
        return reduced ? REDUCED_MS : FULL_MS;
    }

    /** The reduced-motion easing: linear, so there is no travel and no overshoot. */
    public static float reducedInterpolation(float input) {
        return Math.max(0f, Math.min(1f, input));
    }
}
