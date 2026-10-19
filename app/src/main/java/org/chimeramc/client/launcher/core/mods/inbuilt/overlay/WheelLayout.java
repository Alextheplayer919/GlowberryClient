package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * The geometry of an N-sector radial wheel, as pure arithmetic.
 *
 * <p>{@link RadialWheelLayout} is the fixed four-sector wheel the Mod Menu ships with; this is the
 * generalisation the profile-driven wheel needs, because a profile may carry anywhere from one to
 * {@link #MAX_SECTORS} entries and the sector count changes with it. Both live here because the
 * hit-testing rules are the same shape and keeping a second copy would let the drawn sectors and the
 * touched sectors drift — the class of bug a screenshot cannot catch.
 *
 * <p>The sector boundary is a <b>parallel-sided linear gap</b>, not a fixed angular gap: a fixed
 * angle widens with radius and reads as a cheap pie chart, so the gap is expressed in pixels and
 * converted to an angle at the pointer's own radius. That conversion is why the hit test needs the
 * radius and not just the angle.
 *
 * <p>Screen space: x grows right, y grows down, so sector 0 is centred at the top (-90 degrees) and
 * the sectors run clockwise. Pure and Android-free, so every rule below is unit-tested without a
 * device.
 */
public final class WheelLayout {

    /** The most entries a single wheel may hold; beyond this a sector is too thin to hit. */
    public static final int MAX_SECTORS = 8;

    /** The constant linear gap between neighbouring sectors, in dp. */
    public static final float GAP_DP = 6f;

    /** The angle at which sector 0 is centred: straight up, in screen space (y down). */
    public static final double FIRST_SECTOR_DEGREES = -90.0;

    private WheelLayout() {
    }

    /**
     * The angular width of one sector for a sector count.
     *
     * <p>One sector owns the whole circle rather than a slice of it: a single-entry wheel is a ring,
     * not a 360-degree sector with a seam, so the caller draws a full ring and the hit test always
     * returns that one sector.
     */
    public static double sectorSpanDegrees(int sectorCount) {
        int count = clampSectorCount(sectorCount);
        return 360.0 / count;
    }

    /** Clamps a sector count into {@code [1, MAX_SECTORS]}. */
    public static int clampSectorCount(int sectorCount) {
        return Math.max(1, Math.min(MAX_SECTORS, sectorCount));
    }

    /** The centre angle of one sector, in screen space (y down), in degrees. */
    public static double sectorCenterDegrees(int index, int sectorCount) {
        int count = clampSectorCount(sectorCount);
        int i = Math.floorMod(index, count);
        return FIRST_SECTOR_DEGREES + i * sectorSpanDegrees(count);
    }

    /**
     * The angular half-gap at a radius for a constant linear gap.
     *
     * <p>A fixed linear gap is parallel-sided; a fixed angular gap widens with radius, which is the
     * cheap pie-chart look. The half-gap therefore shrinks as the radius grows.
     */
    public static double angularHalfGapDegrees(float radius, float gapPx) {
        if (radius <= 1f) return 180.0;
        return Math.toDegrees((gapPx / 2.0) / radius);
    }

    /**
     * The sector index under a pointer, or {@code -1} for the hub, the gap or outside the wheel.
     *
     * <p>The hub is the centre disc (the player's head), so a pointer inside it is never a sector.
     * A pointer inside the parallel-sided gap between two sectors is deliberately {@code -1} rather
     * than snapped to the nearer one: a boundary touch is ambiguous, and guessing made a selection
     * the player did not aim at.
     *
     * @param dx,dy        pointer minus the wheel centre
     * @param innerRadius  the ring's inner edge; inside this is the hub
     * @param outerRadius  the ring's outer edge; outside this is off-wheel
     * @param gapPx        the constant linear gap between sectors
     * @param sectorCount  how many sectors the wheel is divided into
     * @return the sector index, or -1
     */
    public static int sectorAt(float dx, float dy, float innerRadius, float outerRadius,
                               float gapPx, int sectorCount) {
        int count = clampSectorCount(sectorCount);
        float distance = (float) Math.hypot(dx, dy);
        if (distance <= innerRadius) return -1;
        if (distance > outerRadius) return -1;
        // A one-sector wheel is a ring with no internal boundary, so the whole band is that sector.
        if (count == 1) return 0;

        double span = sectorSpanDegrees(count);
        double deg = Math.toDegrees(Math.atan2(dy, dx));
        // Shift so sector 0's centre is at the origin, then wrap into [0, 360).
        double relative = wrap360(deg - FIRST_SECTOR_DEGREES);
        // The distance to the nearest boundary. Sector boundaries sit at relative == halfSpan
        // (mod span), so measure from there and fold into [0, span/2].
        double halfSpan = span / 2.0;
        double fromBoundary = wrap360(relative - halfSpan) % span;
        if (fromBoundary > halfSpan) fromBoundary = span - fromBoundary;
        double halfGap = angularHalfGapDegrees(distance, gapPx);
        if (fromBoundary < halfGap) return -1;

        // Sector i is centred at relative == i*span, so the nearest centre wins.
        long index = Math.round(relative / span);
        return (int) Math.floorMod(index, (long) count);
    }

    /** Wraps an angle into {@code [0, 360)}. */
    public static double wrap360(double deg) {
        double d = deg % 360.0;
        if (d < 0.0) d += 360.0;
        return d;
    }
}
