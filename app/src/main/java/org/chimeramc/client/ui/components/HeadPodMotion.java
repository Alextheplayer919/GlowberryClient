package org.chimeramc.client.ui.components;

/**
 * The launch-tab head pod's rotation and idle motion, as pure arithmetic.
 *
 * <p>The spec asks for four distinct behaviours and they are easy to get subtly wrong, so they are
 * modelled here rather than inline in the view:
 *
 * <ul>
 *   <li><b>Idle turn</b> — a slow, continuous spin (10–15°/s) so the player can see every side.</li>
 *   <li><b>Hover pause</b> — the idle spin stops while the pointer is over the pod.</li>
 *   <li><b>Drag</b> — a horizontal drag spins the model directly, 1:1 with the pointer.</li>
 *   <li><b>Momentum</b> — releasing a drag keeps the spin going, decaying by friction to a stop
 *       rather than snapping. A flick therefore coasts.</li>
 * </ul>
 *
 * <p>The micro-bob (a 1–2px vertical drift) is a separate, always-on output so the pod never looks
 * frozen even while the rotation is paused. Android-free, so "a flick coasts then stops", "hover
 * stops the idle turn" and "the bob stays inside its amplitude" are all unit-tested.
 */
public final class HeadPodMotion {

    /** Idle spin speed, degrees per second. */
    public static final float IDLE_DEGREES_PER_SECOND = 13f;
    /** Drag sensitivity: degrees of yaw per pixel of horizontal pointer travel. */
    public static final float DRAG_DEGREES_PER_PX = 0.6f;
    /** Per-second decay factor applied to the release velocity (momentum friction). */
    public static final float FRICTION_PER_SECOND = 0.12f;
    /** Below this speed (deg/s) a coasting spin is stopped, so it settles instead of creeping. */
    public static final float MIN_COAST_SPEED = 4f;
    /** The largest release velocity a single flick may carry, in deg/s. */
    public static final float MAX_FLICK_SPEED = 900f;
    /** The idle bob amplitude in px. */
    public static final float BOB_AMPLITUDE_PX = 1.5f;
    /** The idle bob period in ms. */
    public static final long BOB_PERIOD_MS = 3200L;
    /** How long the rotation pauses at the end of a bob cycle, for a "blink" beat. */
    public static final long BOB_PAUSE_MS = 420L;

    private float yawDeg;
    private float velocityDegPerSecond;
    private boolean dragging;
    private boolean hovered;

    public HeadPodMotion() {
        this(0f);
    }

    public HeadPodMotion(float startYawDeg) {
        this.yawDeg = normalize(startYawDeg);
    }

    /** Wraps a yaw into {@code [0, 360)}. */
    public static float normalize(float yawDeg) {
        float y = yawDeg % 360f;
        if (y < 0f) y += 360f;
        return y;
    }

    public float yaw() {
        return yawDeg;
    }

    public float velocity() {
        return velocityDegPerSecond;
    }

    public boolean isDragging() {
        return dragging;
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    public boolean isHovered() {
        return hovered;
    }

    /** Begins a drag. The idle spin and any momentum stop immediately. */
    public void beginDrag() {
        dragging = true;
        velocityDegPerSecond = 0f;
    }

    /**
     * Drags by a horizontal pointer delta.
     *
     * @param dxPx  pointer movement since the last drag sample, in pixels
     * @param dtSec elapsed time for the sample, used to estimate the release velocity
     */
    public void drag(float dxPx, float dtSec) {
        dragging = true;
        float delta = dxPx * DRAG_DEGREES_PER_PX;
        yawDeg = normalize(yawDeg + delta);
        if (dtSec > 0f && dtSec < 0.5f) {
            velocityDegPerSecond = delta / dtSec;
        } else {
            velocityDegPerSecond = 0f;
        }
    }

    /**
     * Ends a drag, keeping the release velocity as momentum.
     *
     * <p>A drag that ended while stationary has near-zero velocity and so simply stops — the flick is
     * what coasts. The speed is clamped so a very fast swipe cannot send the model spinning for
     * seconds.
     */
    public void endDrag() {
        dragging = false;
        if (Math.abs(velocityDegPerSecond) > MAX_FLICK_SPEED) {
            velocityDegPerSecond = Math.signum(velocityDegPerSecond) * MAX_FLICK_SPEED;
        }
        if (Math.abs(velocityDegPerSecond) < MIN_COAST_SPEED) {
            velocityDegPerSecond = 0f;
        }
    }

    /**
     * Advances one frame.
     *
     * @param dtSec elapsed time in seconds; a non-positive or implausibly large value is ignored so
     *              a stall cannot fling the model
     */
    public void tick(float dtSec) {
        if (dtSec <= 0f || dtSec > 0.25f) return;
        if (dragging) return;
        if (Math.abs(velocityDegPerSecond) > 0f) {
            // Coasting from a flick: keep turning, decay toward zero.
            yawDeg = normalize(yawDeg + velocityDegPerSecond * dtSec);
            velocityDegPerSecond *= (float) Math.pow(FRICTION_PER_SECOND, dtSec);
            if (Math.abs(velocityDegPerSecond) < MIN_COAST_SPEED) velocityDegPerSecond = 0f;
        } else if (!hovered) {
            // Idle: a slow, even turn. Hover pauses it, which is the whole point of the hover state.
            yawDeg = normalize(yawDeg + IDLE_DEGREES_PER_SECOND * dtSec);
        }
    }

    /**
     * The idle vertical bob in px at a monotonic time.
     *
     * <p>A sine over {@link #BOB_PERIOD_MS} with a short hold at each extreme, so the drift reads as
     * a breath with a beat rather than a constant jitter. Always available — it is not tied to the
     * rotation, so the pod keeps living while a hover has paused the spin.
     */
    public static float bobPx(long timeMs) {
        long cycle = BOB_PERIOD_MS + BOB_PAUSE_MS;
        if (cycle <= 0) return 0f;
        long phase = ((timeMs % cycle) + cycle) % cycle;
        if (phase >= BOB_PERIOD_MS) return 0f;
        double t = phase / (double) BOB_PERIOD_MS;
        return (float) (Math.sin(t * 2.0 * Math.PI) * BOB_AMPLITUDE_PX);
    }

    /** True while the pod should paint a "spinning fast" cue (a flick still coasting). */
    public boolean isCoasting() {
        return !dragging && Math.abs(velocityDegPerSecond) >= MIN_COAST_SPEED;
    }
}
