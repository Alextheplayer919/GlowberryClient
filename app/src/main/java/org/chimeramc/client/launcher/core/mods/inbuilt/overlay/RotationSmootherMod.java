package org.chimeramc.client.launcher.core.mods.inbuilt.overlay;

import org.chimeramc.client.core.mods.inbuilt.catalog.LookSmoothing;

/**
 * Eases the look deltas the device sends, for the catalogue's {@code combat_rotation_smoother}.
 *
 * <p>Honest scope: Bedrock decides what the camera does on the server side from the aim it receives,
 * so this cannot change where the game points you. What it controls is the quality of the aim you
 * send — it damps frame-to-frame jitter on large swipes while leaving fine aim 1:1, which is what
 * makes a jittery touch or a worn stick feel steadier.
 *
 * <p>Mutually exclusive with the other two shapers at the send step: {@link HitRegistrationMod} and
 * {@code AimSettingsMod} each carry their own smoothing, so running more than one would compound the
 * damping. {@code MinecraftActivity.pojavSendLookDelta} picks exactly one, preferring Hit
 * Registration, then this, then Aim Settings.
 *
 * <p>The arithmetic lives in the pure {@link LookSmoothing}, so the rule is unit-tested without a
 * device.
 */
public final class RotationSmootherMod {

    private static volatile boolean active;
    private static volatile float strength = LookSmoothing.DEFAULT_STRENGTH;
    private static final float[] PREVIOUS = new float[2];

    private RotationSmootherMod() {}

    public static void setEnabled(boolean enabled) {
        active = enabled;
        reset();
    }

    public static boolean isActive() {
        return active;
    }

    /** Sets how much of the previous output to keep, 0..1. */
    public static void setStrength(float value) {
        strength = Math.max(0f, Math.min(0.95f, value));
    }

    public static float getStrength() {
        return strength;
    }

    public static void reset() {
        PREVIOUS[0] = 0f;
        PREVIOUS[1] = 0f;
    }

    /**
     * Shapes one frame of look input.
     *
     * <p>Returns the input unchanged when the module is off, so the caller can route through it
     * unconditionally without a branch of its own.
     */
    public static float[] smoothLookDelta(float deltaX, float deltaY) {
        if (!active) {
            reset();
            return new float[]{deltaX, deltaY};
        }
        return LookSmoothing.smooth(deltaX, deltaY, strength, PREVIOUS);
    }
}
