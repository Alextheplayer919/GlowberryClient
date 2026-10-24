package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * The Mod Menu's geometry, in dp (M1, M5, M9).
 *
 * <p>Kept separate from {@link ModMenuSettings} because these are the spec's fixed numbers, not
 * user settings: a row is 64dp comfortable or 56dp compact, the pill switch is 52x32dp with a 24dp
 * thumb, and a touch target is never below 48dp even when the visual is smaller. A test pins every
 * one of them, so a layout edit that quietly changes the row height fails the build.
 */
public final class ModMenuMetrics {

    private ModMenuMetrics() {
    }

    /** Row height, comfortable (M5). */
    public static final int ROW_COMFORTABLE_DP = 64;
    /** Row height, compact (M5). */
    public static final int ROW_COMPACT_DP = 56;

    /** The pill switch track (M5). */
    public static final int SWITCH_WIDTH_DP = 52;
    public static final int SWITCH_HEIGHT_DP = 32;
    /** The switch thumb (M5). */
    public static final int SWITCH_THUMB_DP = 24;
    /** The thumb's inset inside the track (M5). */
    public static final int SWITCH_THUMB_INSET_DP = 4;
    /** The minimum touch target, even when the visual control is smaller (M5). */
    public static final int MIN_TOUCH_TARGET_DP = 48;

    /** Panel corner radius (M4). Matches {@code UiTokens.RADIUS_XLARGE}. */
    public static final int PANEL_RADIUS_DP = 28;
    /** Panel backdrop blur radius, the regular glass tier (M4). */
    public static final int PANEL_BLUR_DP = 32;
    /** Panel hairline (M4). */
    public static final float HAIRLINE_DP = 0.5f;

    /** The section rail width on a wide screen. */
    public static final int RAIL_WIDTH_DP = 132;
    /** The icon tile inside a row. */
    public static final int ROW_ICON_DP = 40;
    /** The status dot (M10). */
    public static final int STATUS_DOT_DP = 8;
    /** The chevron on an action row (M3). */
    public static final int ACTION_CHEVRON_DP = 16;

    /** The minimum segment count a search field needs before it is shown. */
    public static final int MIN_SEARCHABLE_MODULES = 4;

    /**
     * The row height a density uses, clamped to the two spec values.
     *
     * <p>A pure helper rather than a lookup in the enum so a persisted density that no longer
     * matches an enum constant still yields a usable row height instead of throwing at bind time.
     */
    public static int rowHeightFor(ModMenuSettings.Density density) {
        return density == ModMenuSettings.Density.COMPACT ? ROW_COMPACT_DP : ROW_COMFORTABLE_DP;
    }

    /** True when a control's visual size is below the minimum touch target. */
    public static boolean needsInvisiblePadding(int visualSizeDp) {
        return visualSizeDp < MIN_TOUCH_TARGET_DP;
    }
}
