package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * The right-stick mouse cursor used by the in-game menus (the radial wheel, the Mod Menu and the
 * VIP menu) when a controller is driving them.
 *
 * <p>Pure arithmetic: it integrates a shaped stick vector into a screen position with real delta
 * time, clamps to the screen, applies a light "magnetism" toward a nearby target, and reports which
 * hit target is under the cursor. The view layer only reads {@link #x()}/{@link #y()} and paints;
 * everything that can be wrong (integration, clamping, dead zone, magnetism) is unit-tested without
 * a device.
 *
 * <p>All units are pixels; the caller converts dp with its density. The dead zone and curve are the
 * same ones the gameplay path uses, so the cursor feels like the look stick: a radial dead zone
 * (not per-axis, which needs {@code threshold * sqrt(2)} of travel for a diagonal push) and a
 * response curve so fine movement near centre is precise.
 */
public final class CursorController {

    /** Default radial dead zone when no profile supplies one. */
    public static final float DEFAULT_DEAD_ZONE = 0.12f;
    /** Default response-curve exponent; 1 = linear, >1 = finer near centre. */
    public static final float DEFAULT_CURVE_EXPONENT = 1.6f;
    /** Cursor travel at full deflection, in dp per second. */
    public static final float DEFAULT_MAX_SPEED_DP = 900f;
    /** Precision multiplier while the left stick is held. */
    public static final float PRECISION_MULTIPLIER = 0.4f;
    /** Speed multiplier while the right stick is clicked (fast). */
    public static final float FAST_MULTIPLIER = 1.8f;
    /** How far from a target the cursor is pulled toward it, in dp. */
    public static final float MAGNET_RADIUS_DP = 24f;
    /** The magnet's strength: the fraction of the remaining gap it closes per second. */
    public static final float MAGNET_STRENGTH = 0.25f;
    /** Below this deflection (after the dead zone) the cursor does not move. */
    private static final float MIN_DEFLECTION = 1e-3f;

    private float x;
    private float y;
    private float viewportW;
    private float viewportH;
    private float density = 1f;

    /** The magnet target, or null. Set each frame from the hit-tested element. */
    private float magnetX;
    private float magnetY;
    private boolean hasMagnet;

    /** Creates a cursor centred in a viewport. */
    public CursorController(float viewportWidth, float viewportHeight, float density) {
        this.viewportW = Math.max(1f, viewportWidth);
        this.viewportH = Math.max(1f, viewportHeight);
        this.density = density <= 0f ? 1f : density;
        this.x = this.viewportW / 2f;
        this.y = this.viewportH / 2f;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    /** Recentres on the viewport (called when the wheel/menu opens). */
    public void centre() {
        x = viewportW / 2f;
        y = viewportH / 2f;
    }

    /** Updates the viewport (rotation, resize). Keeps the cursor inside the new bounds. */
    public void setViewport(float width, float height) {
        this.viewportW = Math.max(1f, width);
        this.viewportH = Math.max(1f, height);
        clampToViewport();
    }

    /** Moves the cursor directly (e.g. a touch hides it and a stick re-shows it at the touch point). */
    public void setPosition(float nx, float ny) {
        this.x = nx;
        this.y = ny;
        clampToViewport();
    }

    /**
     * Arms the magnet toward a target this frame. Pass {@code false} to disable.
     * The magnet eases the cursor toward the target so a near-miss still selects.
     */
    public void setMagnetTarget(boolean enabled, float targetX, float targetY) {
        this.hasMagnet = enabled;
        this.magnetX = targetX;
        this.magnetY = targetY;
    }

    /**
     * Integrates one frame of stick input.
     *
     * @param stickX      shaped right-stick X, -1..1 (already dead-zoned by the caller, or raw and
     *                    the dead zone applied here — see {@link #shape})
     * @param stickY      shaped right-stick Y, -1..1 (screen Y grows down, so the caller inverts)
     * @param dtSeconds   real elapsed time; a long stall is clamped by the caller
     * @param precision   true while the left stick is held (slow, fine control)
     * @param fast        true while the right stick is clicked (fast)
     * @param maxSpeedDp  travel at full deflection, in dp/s
     */
    public void update(float stickX, float stickY, float dtSeconds, boolean precision,
                       boolean fast, float maxSpeedDp) {
        if (dtSeconds <= 0f) return;
        float speed = maxSpeedDp * density;
        if (precision) speed *= PRECISION_MULTIPLIER;
        if (fast) speed *= FAST_MULTIPLIER;
        x += stickX * speed * dtSeconds;
        y += stickY * speed * dtSeconds;
        clampToViewport();
        applyMagnet(dtSeconds);
        clampToViewport();
    }

    private void applyMagnet(float dtSeconds) {
        if (!hasMagnet) return;
        float dx = magnetX - x;
        float dy = magnetY - y;
        float dist = (float) Math.hypot(dx, dy);
        float radius = MAGNET_RADIUS_DP * density;
        if (dist <= 0.001f || dist > radius) return;
        // Close a fraction of the gap per second; frame-rate independent via (1 - e^-k*dt).
        float k = MAGNET_STRENGTH * 8f;
        float t = (float) (1.0 - Math.exp(-k * dtSeconds));
        x += dx * t;
        y += dy * t;
    }

    private void clampToViewport() {
        x = Math.max(0f, Math.min(x, viewportW));
        y = Math.max(0f, Math.min(y, viewportH));
    }

    /**
     * Applies the radial dead zone and response curve to a raw stick pair.
     *
     * <p>Radial, not per-axis: a per-axis test demands the threshold on each axis separately, so a
     * diagonal push needs {@code threshold * sqrt(2)} of travel and a stick resting off-centre reads
     * as a constant push. The magnitude is rescaled so the push direction is preserved. The output
     * is written into {@code out} (length 2) and the function returns true when the stick is outside
     * the dead zone.
     */
    public static boolean shape(float rawX, float rawY, float deadZone, float curveExponent,
                                float[] out) {
        if (out == null || out.length < 2) return false;
        float dz = Math.max(0f, Math.min(0.95f, deadZone));
        float mag = (float) Math.hypot(rawX, rawY);
        if (mag <= dz || mag <= MIN_DEFLECTION) {
            out[0] = 0f;
            out[1] = 0f;
            return false;
        }
        // Rescale so the stick starts from 0 just outside the dead zone and reaches 1 at the edge.
        float scaled = Math.min(1f, (mag - dz) / (1f - dz));
        float curved = (float) Math.pow(scaled, Math.max(0.1f, curveExponent));
        float inv = 1f / mag;
        out[0] = rawX * inv * curved;
        out[1] = rawY * inv * curved;
        return true;
    }
}
