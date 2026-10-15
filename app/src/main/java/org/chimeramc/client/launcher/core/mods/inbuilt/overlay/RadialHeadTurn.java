package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * The radial wheel hub head's turn rule, as pure arithmetic.
 *
 * <p>The hub shows the player's own head and it must stay still until the player moves something.
 * The complaint was the opposite: the hub spun on a timer, so it turned on its own. This rule is
 * driven <em>only</em> from an input handler (the right stick or the mouse), and the turn is
 * proportional to the input's horizontal component plus a small vertical nudge, so the head follows
 * the direction the cursor is being pushed.
 *
 * <p>Pure so it is unit-tested without a device: "a zero input does not turn", "a horizontal push
 * turns", "a diagonal push turns more than a pure vertical one" and "yaw stays in [0, 360)" are all
 * properties a screenshot cannot check.
 */
public final class RadialHeadTurn {

    /** Degrees of yaw per second at full horizontal deflection (one full turn in 4 seconds). */
    public static final float DEGREES_PER_SECOND = 90f;
    /** How much a full vertical push adds, in degrees per second (a subtle tilt cue, not a spin). */
    public static final float VERTICAL_DEGREES_PER_SECOND = 6f;

    private RadialHeadTurn() {
    }

    /**
     * Advances a yaw by one input frame.
     *
     * @param yawDeg     the current yaw in degrees
     * @param inputX     shaped horizontal input, -1..1
     * @param inputY     shaped vertical input, -1..1
     * @param dtSeconds  elapsed time; a non-positive or implausibly large value falls back to one
     *                   60Hz frame so a long stall cannot fling the head around
     * @return the new yaw in {@code [0, 360)}
     */
    public static float step(float yawDeg, float inputX, float inputY, float dtSeconds) {
        if (inputX == 0f && inputY == 0f) {
            // No input: the head must not move at all. This is the whole point of the rule.
            return normalize(yawDeg);
        }
        float dt = dtSeconds <= 0f || dtSeconds > 0.1f ? 1f / 60f : dtSeconds;
        float delta = inputX * DEGREES_PER_SECOND * dt + inputY * VERTICAL_DEGREES_PER_SECOND * dt;
        return normalize(yawDeg + delta);
    }

    /** Wraps a yaw into {@code [0, 360)}. */
    public static float normalize(float yawDeg) {
        float y = yawDeg % 360f;
        if (y < 0f) y += 360f;
        return y;
    }
}
