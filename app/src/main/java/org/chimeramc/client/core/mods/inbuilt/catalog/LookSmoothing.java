package org.chimeramc.client.core.mods.inbuilt.catalog;

/**
 * Eases the look deltas the device sends, so a jittery touch or pad stick produces a steadier
 * camera.
 *
 * <p>This is the shaping behind the catalogue's {@code combat_rotation_smoother}. It reads nothing
 * from the game: it filters the input the launcher is about to send, which is why it works on every
 * build. It is deliberately separate from {@link org.chimeramc.client.core.mods.inbuilt.overlay.HitRegistrationMod}
 * and {@code AimSettingsMod}: each carries its own smoothing, so running more than one would compound
 * the damping. The look path picks exactly one.
 *
 * <p>An exponential moving average, applied only above a small threshold so fine aim stays 1:1 — the
 * same rule the other shapers follow, because damping a micro-adjustment adds latency to the track
 * the player is settling onto. Pure and Android-free so the rule is unit-tested without a device.
 */
public final class LookSmoothing {

    private LookSmoothing() {
    }

    /** Deltas below this are left untouched, so fine aim is never damped. */
    public static final float MICRO_DELTA = 0.12f;

    /** Default blend toward the new sample (0 = no smoothing, 1 = frozen). */
    public static final float DEFAULT_STRENGTH = 0.35f;

    /**
     * Applies one frame of smoothing.
     *
     * @param deltaX    the raw x delta
     * @param deltaY    the raw y delta
     * @param strength  how much of the previous output to keep, 0..1
     * @param previous  the previous output pair, length 2, updated in place
     * @return the shaped delta pair (the caller's array is returned, so nothing is allocated)
     */
    public static float[] smooth(float deltaX, float deltaY, float strength, float[] previous) {
        float clamp = clamp(strength, 0f, 0.95f);
        float magnitude = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        if (magnitude < MICRO_DELTA || clamp <= 0f) {
            if (previous != null && previous.length >= 2) {
                previous[0] = deltaX;
                previous[1] = deltaY;
            }
            return new float[]{deltaX, deltaY};
        }
        float px = previous != null && previous.length >= 2 ? previous[0] : deltaX;
        float py = previous != null && previous.length >= 2 ? previous[1] : deltaY;
        float outX = px + (deltaX - px) * (1f - clamp);
        float outY = py + (deltaY - py) * (1f - clamp);
        if (previous != null && previous.length >= 2) {
            previous[0] = outX;
            previous[1] = outY;
        }
        return new float[]{outX, outY};
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
