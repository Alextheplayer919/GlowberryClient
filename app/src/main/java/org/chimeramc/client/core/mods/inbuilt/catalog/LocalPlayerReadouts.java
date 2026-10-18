package org.chimeramc.client.core.mods.inbuilt.catalog;

import java.util.Locale;

/**
 * The arithmetic and formatting behind the local-feed readouts (coordinates, facing, speed,
 * velocity, fall distance).
 *
 * <p>Pure and Android-free on purpose: the local-player feed gives raw floats, and a formatting or
 * sign mistake in a coordinate readout is invisible on a build machine. Every method takes the raw
 * values and returns the string the overlay shows, so a unit test pins the mapping without a device.
 *
 * <p>Minecraft conventions used here: yaw is degrees clockwise from +Z, which the game calls
 * <em>south</em>; a chunk is a 16-block cell; speed is the horizontal distance between two samples
 * over the elapsed time.
 */
public final class LocalPlayerReadouts {

    private LocalPlayerReadouts() {
    }

    /**
     * The compass point for a yaw, using Minecraft's own mapping.
     *
     * <p>In Bedrock yaw 0 faces +Z, which the game calls <em>south</em>, and yaw increases turning
     * right (clockwise seen from above). So south=0, west=90, north=180, east=270. Getting this
     * backwards is the classic "my compass is mirrored" defect, so the four anchors are pinned.
     */
    public static String cardinal(float yaw) {
        float normalized = ((yaw % 360f) + 360f) % 360f;
        if (normalized < 45f || normalized >= 315f) return "S";
        if (normalized < 135f) return "W";
        if (normalized < 225f) return "N";
        return "E";
    }

    /** "x, y, z" to one decimal. */
    public static String coordinates(float[] position) {
        if (position == null || position.length < 3) return null;
        return String.format(Locale.US, "%.1f, %.1f, %.1f", position[0], position[1], position[2]);
    }

    /** The chunk the position is in, as "cx, cz". */
    public static String chunk(float[] position) {
        if (position == null || position.length < 3) return null;
        return ((int) Math.floor(position[0]) >> 4) + ", " + ((int) Math.floor(position[2]) >> 4);
    }

    /** The chunk's own corner block, which is what a chunk-border readout anchors to. */
    public static String chunkOrigin(float[] position) {
        if (position == null || position.length < 3) return null;
        int ox = ((int) Math.floor(position[0]) >> 4) << 4;
        int oz = ((int) Math.floor(position[2]) >> 4) << 4;
        return ox + ", " + oz;
    }

    /** "N 182°" — the facing with its yaw, for a direction/compass readout. */
    public static String direction(float[] rotation) {
        if (rotation == null || rotation.length < 1) return null;
        return cardinal(rotation[0]) + " " + String.format(Locale.US, "%.0f°", rotation[0]);
    }

    /** The pitch, positive looking up. */
    public static String pitch(float[] rotation) {
        if (rotation == null || rotation.length < 2) return null;
        return String.format(Locale.US, "%.0f°", rotation[1]);
    }

    /** A horizontal speed in blocks per second, or null when the value is unusable. */
    public static String speed(double blocksPerSecond) {
        if (Double.isNaN(blocksPerSecond) || Double.isInfinite(blocksPerSecond)) return null;
        return String.format(Locale.US, "%.2f b/s", blocksPerSecond);
    }

    /** The full velocity vector. */
    public static String velocity(float vx, float vy, float vz) {
        if (!finite(vx) || !finite(vy) || !finite(vz)) return null;
        return String.format(Locale.US, "%.2f, %.2f, %.2f", vx, vy, vz);
    }

    /**
     * The fall-damage warning for how far the player has descended since the fall began.
     *
     * <p>Bedrock starts damaging above three blocks of fall. The warning is deliberately worded as
     * a warning, not a prediction: the client cannot see the landing block, so it says how far the
     * fall has gone rather than claiming a damage figure it cannot know.
     *
     * @param fallBlocks how far the player has descended since the fall began, or a non-positive
     *                   value when not falling
     */
    public static String fallWarning(double fallBlocks) {
        if (Double.isNaN(fallBlocks) || fallBlocks <= 3.0) return null;
        return String.format(Locale.US, "Falling %.1f blocks", fallBlocks);
    }

    /** The health readout, or null when the feed has no plausible value. */
    public static String health(float[] health) {
        if (health == null || health.length < 1) return null;
        float value = health[0];
        if (!finite(value) || value < 0f || value > 1024f) return null;
        return String.format(Locale.US, "%.0f", value);
    }

    private static boolean finite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }
}
