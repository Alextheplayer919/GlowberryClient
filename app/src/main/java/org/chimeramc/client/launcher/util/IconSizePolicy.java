package org.chimeramc.client.launcher.util;

import org.chimeramc.client.util.PersonalizationManager;

/**
 * The rules behind the "Icon Size" preference.
 *
 * <p>Split out from the view walk so the arithmetic is unit-testable: a scaling bug is silent on a
 * device (icons are simply the wrong size), and the two ways to get it wrong — compounding the
 * scale on every re-apply, and scaling a dimension that is not a fixed size — are both decided
 * here rather than inside a recursive method.
 */
public final class IconSizePolicy {

    private IconSizePolicy() {
    }

    /**
     * Whether a view's measured dimension is a fixed size that scaling may touch.
     *
     * <p>{@code WRAP_CONTENT} (-2) and {@code MATCH_PARENT} (-1) are layout instructions, not
     * sizes; multiplying one would turn "fill the row" into "22dp wide" and break the layout. Only
     * a positive pixel value is a real size.
     */
    public static boolean isScalableDimension(int sizePx) {
        return sizePx > 0;
    }

    /**
     * The scaled size for a base dimension.
     *
     * <p>The percentage is clamped to the preference's own range so a corrupted stored value cannot
     * produce a zero or negative dimension, and the result is never allowed below one pixel: a
     * zero-sized view is invisible and reads as a missing icon rather than a small one.
     */
    public static int scaledPx(int basePx, int percent) {
        if (!isScalableDimension(basePx)) return basePx;
        int clamped = Math.max(PersonalizationManager.ICON_SIZE_MIN,
                Math.min(PersonalizationManager.ICON_SIZE_MAX, percent));
        int scaled = Math.round(basePx * (clamped / 100f));
        return Math.max(1, scaled);
    }
}
