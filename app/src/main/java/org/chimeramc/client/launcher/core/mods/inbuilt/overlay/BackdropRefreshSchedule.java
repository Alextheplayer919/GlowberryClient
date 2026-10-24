package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * The throttling rule for the wheel's backdrop capture (R3).
 *
 * <p>The wheel sits over a live game, so its blurred backdrop is captured from the window rather
 * than drawn. Capturing every frame is the fastest way to lose the frame budget on a mid-range
 * Adreno, so the capture is rate-limited to a small number of refreshes per second and the last
 * result is reused in between. This class holds only the arithmetic of "is a refresh due", because
 * the timing rule is the part that is easy to get subtly wrong (a naive {@code now - last > period}
 * drifts and can skip a refresh entirely when the clock jumps).
 *
 * <p>Android-free and clock-injectable, so the throttle is unit tested with no device and no
 * sleeping.
 */
public final class BackdropRefreshSchedule {

    /** Never refresh faster than this, in milliseconds. */
    public static final long MIN_INTERVAL_MS = 1000L / RadialWheelMaterial.BLUR_FPS_MAX;

    private long lastCaptureMs = Long.MIN_VALUE;
    private int refreshes;

    /**
     * Whether a capture is due at {@code nowMs}.
     *
     * <p>A clock that moved backwards (an uptime wrap, or a test) is treated as "due": the worst
     * outcome of an extra capture is one wasted frame, while never refreshing again would freeze the
     * backdrop for the rest of the session.
     */
    public boolean isDue(long nowMs, int fps) {
        long interval = intervalMs(fps);
        if (interval < 0L) return false;
        if (lastCaptureMs == Long.MIN_VALUE) return true;
        if (nowMs < lastCaptureMs) return true;
        return nowMs - lastCaptureMs >= interval;
    }

    /** Records that a capture happened at {@code nowMs}. */
    public void markCaptured(long nowMs) {
        lastCaptureMs = nowMs;
        refreshes++;
    }

    /** How many captures have been taken; used to assert the throttle in tests. */
    public int refreshes() {
        return refreshes;
    }

    /** Resets the schedule, so the next {@link #isDue} call is true (used on open). */
    public void reset() {
        lastCaptureMs = Long.MIN_VALUE;
        refreshes = 0;
    }

    /**
     * The minimum interval between captures for a rate, in milliseconds, or {@code -1} when the
     * caller asked for no blur at all.
     */
    public static long intervalMs(int fps) {
        if (fps <= 0) return -1L;
        int clamped = Math.min(RadialWheelMaterial.BLUR_FPS_MAX, fps);
        return Math.max(MIN_INTERVAL_MS, 1000L / clamped);
    }
}
