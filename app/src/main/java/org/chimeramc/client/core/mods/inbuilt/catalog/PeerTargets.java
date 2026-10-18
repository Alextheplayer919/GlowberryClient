package org.chimeramc.client.core.mods.inbuilt.catalog;

import java.util.Locale;

/**
 * Picks the nearest peer from the proximity-voice feed, for the catalogue's target readouts
 * ({@code combat_target_hud}, {@code hud_target_info}).
 *
 * <p>The peer feed is the one target source this build owns that needs no game hook: other players
 * advertise their own position and name over the voice protocol, so a "who is near me" readout works
 * without reading the game's entity list. Vanilla players are invisible to it, and the readout says
 * so rather than showing an empty box that looks broken.
 *
 * <p>Pure and Android-free: it takes the peer list and the local position and returns a formatted
 * line, so the distance and the "no target" rules are unit-testable without a device. A non-finite
 * coordinate is skipped, because a torn native read projected into the world would place a marker at
 * an undefined spot the player might act on.
 */
public final class PeerTargets {

    private PeerTargets() {
    }

    /** One peer with the minimal shape this rule needs. */
    public static final class Peer {
        public final String id;
        public final String name;
        public final float x, y, z;

        public Peer(String id, String name, float x, float y, float z) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    /** The nearest peer and its distance. */
    public static final class Nearest {
        public final String id;
        public final String name;
        public final float distanceBlocks;

        Nearest(String id, String name, float distanceBlocks) {
            this.id = id;
            this.name = name;
            this.distanceBlocks = distanceBlocks;
        }
    }

    /**
     * The nearest peer to the local position, or null when there is none.
     *
     * <p>Null (not a zero-distance reading) whenever the local position or the peer list is
     * missing, so the readout hides rather than claiming an adjacent player.
     */
    public static Nearest nearest(float[] localPosition, Iterable<Peer> peers) {
        if (localPosition == null || localPosition.length < 3 || peers == null) return null;
        Nearest best = null;
        for (Peer peer : peers) {
            if (peer == null) continue;
            if (!finite(peer.x) || !finite(peer.y) || !finite(peer.z)) continue;
            float dx = peer.x - localPosition[0];
            float dy = peer.y - localPosition[1];
            float dz = peer.z - localPosition[2];
            float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (best == null || distance < best.distanceBlocks) {
                best = new Nearest(peer.id, peer.name, distance);
            }
        }
        return best;
    }

    /** "Name 12.3 blocks" for the target readouts, or null when there is no target. */
    public static String describe(Nearest nearest) {
        if (nearest == null) return null;
        String name = nearest.name == null || nearest.name.trim().isEmpty()
                ? nearest.id
                : nearest.name;
        if (name == null || name.trim().isEmpty()) return null;
        return String.format(Locale.US, "%s %.1f blocks", name, nearest.distanceBlocks);
    }

    private static boolean finite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }
}
