package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * The geometry of the radial mod wheel, as pure arithmetic.
 *
 * <p>The wheel is four curved sectors around a central hub. Which sector a touch is over is a
 * function of the pointer's position relative to the wheel's centre and radii, so it is computed
 * here and unit-tested, and {@link RadialMenuOverlay} only paints and dispatches. Keeping it pure is
 * what makes "the sector under the finger" testable without a device — the class of bug that only
 * shows up when a sector's boundary is a few degrees off is exactly what a screenshot cannot catch.
 *
 * <p>Screen space: x grows right, y grows down, so the top sector is around -90 degrees, the right
 * around 0, the bottom around +90 and the left around 180. A small gap around each boundary keeps a
 * boundary touch from being ambiguous.
 */
public final class RadialWheelLayout {

    /** The four destinations, plus the centre hub and "outside the wheel". */
    public enum Sector {
        MODS,
        CLOSET,
        SETTINGS,
        HUD,
        /** The centre hub (the player's head). */
        CENTER,
        /** Inside the outer radius check failed, or a boundary gap, or inside the inner dead hub. */
        NONE,
    }

    private RadialWheelLayout() {
    }

    /**
     * The sector under a pointer.
     *
     * @param dx          pointer x minus the wheel centre x
     * @param dy          pointer y minus the wheel centre y
     * @param innerRadius the inner edge of the sectors (the head hub radius); inside this is CENTER
     * @param outerRadius the outer edge of the sectors; outside this is NONE
     * @param gapDegrees  half-width of the dead gap on each sector boundary
     */
    public static Sector sectorAt(float dx, float dy, float innerRadius, float outerRadius,
                                  float gapDegrees) {
        float distance = (float) Math.hypot(dx, dy);
        if (distance <= innerRadius) return Sector.CENTER;
        if (distance > outerRadius) return Sector.NONE;

        // Screen-space angle: atan2(dy, dx) with y down, in [-180, 180).
        double deg = Math.toDegrees(Math.atan2(dy, dx));
        if (deg < -180.0) deg += 360.0;

        // Distance to the nearest cardinal boundary (±45, ±135). A hit within the gap is dropped.
        double nearestBoundary = nearestBoundaryDistance(deg);
        if (nearestBoundary < gapDegrees) return Sector.NONE;

        if (deg >= -135.0 && deg < -45.0) return Sector.MODS;
        if (deg >= -45.0 && deg < 45.0) return Sector.CLOSET;
        if (deg >= 45.0 && deg < 135.0) return Sector.SETTINGS;
        return Sector.HUD;
    }

    /** The angular distance from {@code deg} to the nearest sector boundary (±45, ±135). */
    static double nearestBoundaryDistance(double deg) {
        double[] boundaries = {-135.0, -45.0, 45.0, 135.0};
        double best = Double.MAX_VALUE;
        for (double b : boundaries) {
            double d = Math.abs(wrap180(deg - b));
            best = Math.min(best, d);
        }
        return best;
    }

    private static double wrap180(double deg) {
        double d = deg % 360.0;
        if (d >= 180.0) d -= 360.0;
        if (d < -180.0) d += 360.0;
        return d;
    }

    /** The centre angle of a sector, in degrees, for laying out its icon and label. */
    public static double sectorCenterDegrees(Sector sector) {
        switch (sector) {
            case MODS: return -90.0;
            case CLOSET: return 0.0;
            case SETTINGS: return 90.0;
            case HUD: return 180.0;
            default: return 0.0;
        }
    }

    /** True when a sector is one of the four real destinations (not the hub or "none"). */
    public static boolean isDestination(Sector sector) {
        return sector == Sector.MODS || sector == Sector.CLOSET
                || sector == Sector.SETTINGS || sector == Sector.HUD;
    }
}
