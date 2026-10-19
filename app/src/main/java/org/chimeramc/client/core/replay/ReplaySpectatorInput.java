package org.chimeramc.client.core.replay;

/**
 * The free-camera spectator's raw input, as pure arithmetic.
 *
 * <p>The replay update asks for a spectator who can "fly around" the reconstructed scene rather
 * than only watching an authored shot. {@link ReplayCamera} already models the flight itself; this
 * class converts pointer drags and a flight stick into the deltas that drive it, keeping the
 * gathering of raw pointer/stick values out of the model so the conversion is unit-testable.
 *
 * <p>Drag-to-look: horizontal drag turns yaw, vertical drag pitches, both scaled by a sensitivity
 * (degrees of look per pixel of travel). The fly stick is the usual spectator schema — forward,
 * strafe and vertical in -1..1, with a boost that multiplies the camera's fly speed while held, so
 * a held sprint feels like sprinting through the clip rather than slowly drifting.
 *
 * <p>Android-free so "a quick drag turns further than a slow one at the same distance", "no drag
 * leaves the view alone" and "boost only scales while held" are unit-tested.
 */
public final class ReplaySpectatorInput {

    /** Look sensitivity: degrees of yaw/pitch per pixel of pointer travel. */
    public static final float LOOK_DEGREES_PER_PX = 0.18f;
    /** The speed multiplier when the boost input is held. */
    public static final float BOOST_MULTIPLIER = 4f;

    private ReplaySpectatorInput() {
    }

    /**
     * Converts a pointer drag into a look delta.
     *
     * @param dxPx horizontal pointer travel, pixels (right positive)
     * @param dyPx vertical pointer travel, pixels (down positive; the view pitches down for a drag
     *             pointing downward, matching how every first-person look feels)
     * @return {@code {dYawDeg, dPitchDeg}}
     */
    public static float[] dragToLook(float dxPx, float dyPx) {
        if (Float.isNaN(dxPx) || Float.isNaN(dyPx)) {
            return new float[]{0f, 0f};
        }
        return new float[]{dxPx * LOOK_DEGREES_PER_PX, dyPx * LOOK_DEGREES_PER_PX};
    }

    /**
     * Applies a flight stick's inputs onto a camera.
     *
     * @param camera    the spectator camera to move
     * @param forward   forward/back in -1..1 (forward positive)
     * @param strafe    left/right in -1..1 (right positive)
     * @param vertical  up/down in -1..1 (up positive)
     * @param boost     true while the boost gesture is held
     * @param elapsedMs wall time to integrate, capped so a stall cannot fling the camera
     * @return the speed the camera moved at (post-boost), so a caller can fade a HUD cue
     */
    public static float fly(ReplayCamera camera, float forward, float strafe,
                            float vertical, boolean boost, long elapsedMs) {
        if (camera == null) {
            return 0f;
        }
        float speed = camera.speed();
        if (boost && (forward != 0f || strafe != 0f || vertical != 0f)) {
            camera.setSpeed(speed * BOOST_MULTIPLIER);
        }
        camera.move(clamp(forward), clamp(strafe), clamp(vertical), elapsedMs);
        float used = camera.speed();
        camera.setSpeed(speed);
        return used;
    }

    private static float clamp(float value) {
        if (Float.isNaN(value)) return 0f;
        if (value > 1f) return 1f;
        if (value < -1f) return -1f;
        return value;
    }
}