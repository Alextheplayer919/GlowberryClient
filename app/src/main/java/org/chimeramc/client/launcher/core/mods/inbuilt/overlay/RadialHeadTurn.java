package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * The radial wheel hub head's look rule, as pure arithmetic.
 *
 * <p>The hub shows the player's own head and it must stay still until the player moves something —
 * and when they do, the head should <em>look at what they are pointing at</em>. The old rule spun
 * the head like a turntable from stick velocity and never used the cursor's position, so it kept
 * turning after the cursor stopped. This rule is a clamped look-at: yaw and pitch are derived from
 * the cursor's offset from the hub's centre (about ±35° horizontally and ±25° vertically) and then
 * smoothed, so the head settles exactly on the cursor and works identically for touch and stick.
 *
 * <p>Pure so it is unit-tested without a device: "a centred cursor faces forward", "a cursor to the
 * right turns the head right", "a cursor below tilts the head down", "the angles clamp at the
 * extremes" and "motion is smoothed toward the target" are all properties a screenshot cannot check.
 */
public final class RadialHeadTurn {

    /** Degrees of yaw at the horizontal edge of the hub (cursor hard right/left). */
    public static final float MAX_YAW_DEG = 35f;
    /** Degrees of pitch at the vertical edge of the hub (cursor hard up/down). */
    public static final float MAX_PITCH_DEG = 25f;
    /** How fast the head settles on the cursor: fraction of the remaining gap closed per second. */
    public static final float SMOOTHING_PER_SECOND = 8f;

    /** How far the look may fall behind a fast cursor sweep in one frame (smoothing bound). */
    private static final float MAX_FRAME_STEP_DEG = 18f;

    private RadialHeadTurn() {
    }

    /**
     * The target head look for a cursor at {@code cursorDx},{@code cursorDy} relative to the hub
     * centre, in screen pixels. Both axes clamp so the head cannot spin past its sockets.
     *
     * @param maxHorizontal the horizontal radius of the hub, in pixels (clamps yaw)
     * @param maxVertical   the vertical radius of the hub, in pixels (clamps pitch)
     * @return {@code {yawDeg, pitchDeg}}, where yaw is positive looking right (screen +X) and pitch
     *         is positive looking down (screen +Y, which grows downward)
     */
    public static float[] targetLook(float cursorDx, float cursorDy,
                                     float maxHorizontal, float maxVertical) {
        float hx = Math.max(1f, maxHorizontal);
        float hy = Math.max(1f, maxVertical);
        float yaw = clamp(cursorDx / hx, -1f, 1f) * MAX_YAW_DEG;
        // Screen Y grows down, so a cursor below the centre (positive dy) is a downward look.
        float pitch = clamp(cursorDy / hy, -1f, 1f) * MAX_PITCH_DEG;
        return new float[] {yaw, pitch};
    }

    /**
     * Steps the current head look toward the target by one frame.
     *
     * @param currentYaw     the current yaw, degrees
     * @param currentPitch   the current pitch, degrees
     * @param targetYaw      the target yaw, degrees
     * @param targetPitch    the target pitch, degrees
     * @param dtSeconds      elapsed time; a non-positive or implausibly large value falls back to
     *                       one 60Hz frame so a long stall cannot teleport the head
     * @return {@code {yawDeg, pitchDeg}} eased toward the target
     */
    public static float[] stepTowards(float currentYaw, float currentPitch,
                                      float targetYaw, float targetPitch, float dtSeconds) {
        float dt = dtSeconds <= 0f || dtSeconds > 0.1f ? 1f / 60f : dtSeconds;
        float t = (float) (1.0 - Math.exp(-SMOOTHING_PER_SECOND * dt));
        float yaw = currentYaw + (targetYaw - currentYaw) * t;
        float pitch = currentPitch + (targetPitch - currentPitch) * t;
        // Bound a single frame so a teleported cursor (e.g. the view being laid out late) does not
        // fling the head around; the exponential already bounds it, this is a hard safety cap.
        yaw = clampStep(currentYaw, yaw);
        pitch = clampStep(currentPitch, pitch);
        return new float[] {yaw, pitch};
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    private static float clampStep(float from, float to) {
        float delta = to - from;
        if (delta > MAX_FRAME_STEP_DEG) return from + MAX_FRAME_STEP_DEG;
        if (delta < -MAX_FRAME_STEP_DEG) return from - MAX_FRAME_STEP_DEG;
        return to;
    }
}
