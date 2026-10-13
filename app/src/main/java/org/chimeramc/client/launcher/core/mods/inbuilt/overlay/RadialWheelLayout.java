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

    /** The wheel's outer radius, as a fraction of {@code min(screenW, screenH)}. */
    public static final float OUTER_FRACTION = 0.40f;
    /** The ring's inner radius, as a fraction of the outer radius. */
    public static final float RING_INNER_FRACTION = 0.52f;
    /** The hub (avatar disc) radius, as a fraction of the outer radius. */
    public static final float HUB_FRACTION = 0.46f;
    /** The constant linear gap between neighbouring sectors, in dp. */
    public static final float GAP_DP = 6f;
    /** The corner radius of a sector, in dp. */
    public static final float SECTOR_CORNER_DP = 12f;

    private RadialWheelLayout() {
    }

    /** The wheel's outer radius in px for a view. */
    public static float outerRadius(int w, int h) {
        return Math.min(w, h) * OUTER_FRACTION;
    }

    /** The ring's inner radius in px. */
    public static float ringInnerRadius(float outerRadius) {
        return outerRadius * RING_INNER_FRACTION;
    }

    /** The hub radius in px. */
    public static float hubRadius(float outerRadius) {
        return outerRadius * HUB_FRACTION;
    }

    /**
     * The angular half-gap at a radius for a constant linear gap. A fixed linear gap is
     * parallel-sided; a fixed angular gap widens with radius (the cheap pie-chart look).
     */
    public static double angularHalfGapDegrees(float radius, float gapPx) {
        if (radius <= 1f) return 90.0;
        return Math.toDegrees((gapPx / 2.0) / radius);
    }

    /**
     * The sector under a pointer with a <b>parallel-sided</b> linear gap.
     *
     * @param dx,dy        pointer minus the wheel centre
     * @param innerRadius  the ring's inner edge; inside this is CENTER
     * @param outerRadius  the ring's outer edge; outside this is NONE
     * @param gapPx        the constant linear gap between sectors
     */
    public static Sector sectorAtLinear(float dx, float dy, float innerRadius, float outerRadius,
                                        float gapPx) {
        float distance = (float) Math.hypot(dx, dy);
        if (distance <= innerRadius) return Sector.CENTER;
        if (distance > outerRadius) return Sector.NONE;
        double deg = Math.toDegrees(Math.atan2(dy, dx));
        if (deg < -180.0) deg += 360.0;
        double halfGap = angularHalfGapDegrees(distance, gapPx);
        if (nearestBoundaryDistance(deg) < halfGap) return Sector.NONE;
        return quadrantFor(deg);
    }

    /** The quadrant sector for an angle in degrees (screen space, y down). */
    public static Sector quadrantFor(double deg) {
        if (deg >= -135.0 && deg < -45.0) return Sector.MODS;
        if (deg >= -45.0 && deg < 45.0) return Sector.CLOSET;
        if (deg >= 45.0 && deg < 135.0) return Sector.SETTINGS;
        return Sector.HUD;
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

        return quadrantFor(deg);
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

    // --- Chip clusters (pure layout) ---------------------------------------------------------

    /** Cluster anchors as fractions of the view. Top-left 2, top-right 3, right-middle 1. */
    public static final float CHIP_LEFT_FX = 0.14f;
    public static final float CHIP_RIGHT_FX = 0.86f;
    public static final float CHIP_TOP_FY = 0.14f;
    public static final float CHIP_RIGHT_MID_FX = 0.92f;
    public static final float CHIP_RIGHT_MID_FY = 0.60f;
    public static final float CHIP_STEP_FY = 0.10f;

    /**
     * The centre points of every chip, in draw order. Pure so a test can assert no chip overlaps
     * the ring or another chip at any screen size.
     *
     * @return an array of {@code {x, y}} pairs; cluster order is top-left, top-right, right-middle.
     */
    public static float[][] chipCenters(int w, int h, int topLeftCount, int topRightCount,
                                        int rightMidCount) {
        int total = Math.max(0, topLeftCount) + Math.max(0, topRightCount)
                + Math.max(0, rightMidCount);
        float[][] out = new float[total][2];
        int i = 0;
        for (int k = 0; k < topLeftCount; k++) {
            out[i][0] = w * CHIP_LEFT_FX;
            out[i][1] = h * (CHIP_TOP_FY + k * CHIP_STEP_FY);
            i++;
        }
        for (int k = 0; k < topRightCount; k++) {
            out[i][0] = w * CHIP_RIGHT_FX;
            out[i][1] = h * (CHIP_TOP_FY + k * CHIP_STEP_FY);
            i++;
        }
        for (int k = 0; k < rightMidCount; k++) {
            out[i][0] = w * CHIP_RIGHT_MID_FX;
            out[i][1] = h * (CHIP_RIGHT_MID_FY + k * CHIP_STEP_FY);
            i++;
        }
        return out;
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
