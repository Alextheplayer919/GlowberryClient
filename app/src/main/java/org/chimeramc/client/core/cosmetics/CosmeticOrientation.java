package org.chimeramc.client.core.cosmetics;

/**
 * The single conversion from Bedrock cosmetic model space to the player model space the preview and
 * the native renderer use.
 *
 * <p>The player model in this project faces <b>+Z</b> (see {@link SkinModel}: the FRONT face is at
 * {@code maxZ}). Bedrock-authored cosmetic geometry — hats and pets exported from Blockbench for the
 * Bedrock format, and armour-style attachables — faces <b>−Z</b>. Attaching a −Z-facing model to a
 * +Z-facing player without converting leaves it backwards: a hat's brim points out the back of the
 * head and a pet walks away from the camera while the player walks toward it. That was the
 * "models can appear backwards" defect.
 *
 * <p>The fix is one half-turn about Y, applied to a cosmetic's model-space points before they are
 * anchored to the player. It is stated once here rather than as a scatter of {@code + 180f}s, so the
 * preview and the native renderer cannot convert differently — a second yaw constant is exactly how
 * the two drift.
 *
 * <p>The cape chain is the one exception: {@code CapeGeometry} already bakes its own 180° facing
 * into the chain's bones, so the cape must <em>not</em> be converted again. Callers convert only the
 * authored hat and pet meshes.
 *
 * <p>Pure and Android-free, so the rule is unit-testable without a device.
 */
public final class CosmeticOrientation {

    /** The Y rotation, in degrees, that turns Bedrock cosmetic space onto the player's +Z front. */
    public static final float YAW_TO_PLAYER_DEGREES = 180f;

    private CosmeticOrientation() {
    }

    /**
     * Rotates a Bedrock model-space point into player space, in place.
     *
     * @param point {x, y, z} in Bedrock cosmetic space; overwritten with the player-space point
     */
    public static void toPlayerSpace(float[] point) {
        if (point == null || point.length < 3) return;
        // A 180° turn about Y: x -> -x, z -> -z, y unchanged.
        point[0] = -point[0];
        point[2] = -point[2];
    }

    /** The X component of a Bedrock point after the conversion (no allocation). */
    public static float playerX(float x) {
        return -x;
    }

    /** The Z component of a Bedrock point after the conversion (no allocation). */
    public static float playerZ(float z) {
        return -z;
    }

    /** Rotates a Bedrock model-space direction/normal into player space, in place. */
    public static void normalToPlayerSpace(float[] normal) {
        toPlayerSpace(normal);
    }
}
