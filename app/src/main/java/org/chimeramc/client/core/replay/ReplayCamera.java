package org.chimeramc.client.core.replay;

/**
 * The detached replay camera: a free-flying spectator view with no gravity or collision.
 *
 * <p>Playback used to be locked to the recorded first-person view. This is the pure model behind the
 * free camera: position, yaw/pitch and movement. It is deliberately a small fly-camera rather than a
 * full engine camera so it can be unit-tested and driven from either a stick or a mouse, and so the
 * cinematic path can keyframe the exact same fields.
 *
 * <p>Pitch is clamped to just under a right angle, because looking straight up or down makes the yaw
 * axis undefined and the view snaps. Movement is on the horizontal plane plus an explicit vertical
 * input, matching how spectator flight feels: you look where you want and move relative to that.
 */
public final class ReplayCamera {

    /** The look pitch limit, in degrees, kept clear of the poles. */
    public static final float MAX_PITCH_DEG = 89f;

    /** The default fly speed, in blocks per second. */
    public static final float DEFAULT_SPEED = 12f;

    private float x, y, z;
    private float yawDeg;
    private float pitchDeg;
    private float speed = DEFAULT_SPEED;

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float z() {
        return z;
    }

    public float yawDeg() {
        return yawDeg;
    }

    public float pitchDeg() {
        return pitchDeg;
    }

    public float speed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = Math.max(0.5f, speed);
    }

    /** Places the camera at a pose, e.g. the recorded view when detaching. */
    public void set(float x, float y, float z, float yawDeg, float pitchDeg) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yawDeg = yawDeg;
        this.pitchDeg = clampPitch(pitchDeg);
    }

    /** Turns the camera by a look delta, in degrees. */
    public void look(float dYawDeg, float dPitchDeg) {
        if (Float.isNaN(dYawDeg) || Float.isNaN(dPitchDeg)) return;
        yawDeg = wrapYaw(yawDeg + dYawDeg);
        pitchDeg = clampPitch(pitchDeg + dPitchDeg);
    }

    /**
     * Moves the camera relative to where it is looking.
     *
     * @param forward    forward/back input, -1..1
     * @param strafe     left/right input, -1..1
     * @param vertical   up/down input, -1..1
     * @param elapsedMs  wall time to integrate
     */
    public void move(float forward, float strafe, float vertical, long elapsedMs) {
        if (elapsedMs <= 0L) return;
        float seconds = elapsedMs / 1000f;
        float distance = speed * seconds;
        double yaw = Math.toRadians(yawDeg);
        // Yaw is clockwise from +Z, so forward is (sin, 0, cos) and right is (cos, 0, -sin). Getting
        // the sign of the strafe term wrong mirrors every strafe, which is only visible in motion.
        float fx = (float) Math.sin(yaw);
        float fz = (float) Math.cos(yaw);
        float rx = (float) Math.cos(yaw);
        float rz = (float) -Math.sin(yaw);
        x += (fx * forward + rx * strafe) * distance;
        z += (fz * forward + rz * strafe) * distance;
        y += vertical * distance;
    }

    /** The unit forward vector, for a renderer or a path tangency. */
    public float[] forward() {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        double cosPitch = Math.cos(pitch);
        return new float[]{
                (float) (Math.sin(yaw) * cosPitch),
                (float) (-Math.sin(pitch)),
                (float) (Math.cos(yaw) * cosPitch)};
    }

    /** Copies another camera's pose and speed. */
    public void copyFrom(ReplayCamera other) {
        if (other == null) return;
        set(other.x, other.y, other.z, other.yawDeg, other.pitchDeg);
        setSpeed(other.speed);
    }

    static float clampPitch(float pitch) {
        if (Float.isNaN(pitch)) return 0f;
        if (pitch > MAX_PITCH_DEG) return MAX_PITCH_DEG;
        if (pitch < -MAX_PITCH_DEG) return -MAX_PITCH_DEG;
        return pitch;
    }

    static float wrapYaw(float yaw) {
        if (Float.isNaN(yaw)) return 0f;
        float wrapped = yaw % 360f;
        if (wrapped < 0f) wrapped += 360f;
        return wrapped;
    }
}
