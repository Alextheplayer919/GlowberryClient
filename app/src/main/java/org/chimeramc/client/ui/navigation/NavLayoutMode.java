package org.chimeramc.client.ui.navigation;

/**
 * How the launcher's top-level navigation is presented at a given window width (U5).
 *
 * <p>One bar cannot serve every width: a phone has a short, narrow edge to spare and a wide
 * tablet has room for a rail that names every destination. The rule is a pure function of the
 * window width in dp so it is unit-tested rather than discovered by resizing a device, and so the
 * wrapper orientation, the layout and the label visibility all read the same answer.
 *
 * <p>The thresholds are the Material breakpoints: below 600dp is a compact window, 600-839dp is a
 * medium window, and 840dp and up is an expanded window.
 */
public enum NavLayoutMode {
    /** A horizontal bar docked to the bottom of the screen. Compact windows (phones). */
    BOTTOM_NAV,
    /** A vertical icon-only rail on the leading edge. Medium windows (small tablets, foldables). */
    NAV_RAIL,
    /** A vertical rail that also shows each destination's label. Large windows. */
    EXPANDED_RAIL;

    public static final int MEDIUM_WIDTH_DP = 600;
    public static final int EXPANDED_WIDTH_DP = 840;

    /**
     * The mode for a window [widthDp] dp wide. A non-positive width (before layout, or a bad
     * reading) resolves to {@link #BOTTOM_NAV}, which is the layout that needs the least room.
     */
    public static NavLayoutMode forWidthDp(int widthDp) {
        if (widthDp >= EXPANDED_WIDTH_DP) return EXPANDED_RAIL;
        if (widthDp >= MEDIUM_WIDTH_DP) return NAV_RAIL;
        return BOTTOM_NAV;
    }

    /** True for a vertical rail, whatever its width class. */
    public boolean isRail() {
        return this != BOTTOM_NAV;
    }

    /** True when the rail (or bar) shows a text label beside every icon. */
    public boolean showsAllLabels() {
        return this == EXPANDED_RAIL;
    }
}
