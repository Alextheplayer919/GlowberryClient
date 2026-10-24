package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * The Mod Menu's own appearance settings (M6), as one pure value.
 *
 * <p>The menu is "highly customizable" only within a bounded set: presets plus four sliders. Every
 * extra knob is QA surface and a support ticket ("why does my menu look broken"), so the excluded
 * list is deliberate — no free-form colour pickers, no per-module colours, no custom blur radii.
 *
 * <p>Two of the settings are <b>clamped by the system</b>: {@link #glassIntensity()} and
 * {@link #motion()} both fall back to the accessibility settings when the OS asks for them. A
 * user-facing override that beats the OS accessibility setting is a bug, not a feature — see
 * {@link #effectiveGlass(boolean, boolean)} and {@link #effectiveMotion(boolean, boolean)}.
 *
 * <p>Pure and Android-free, so the clamp rules are unit-tested without a device.
 */
public final class ModMenuSettings {

    /** The glass intensity steps (M4). */
    public enum GlassIntensity {
        /** Solid surfaces; no blur at all. */
        OFF,
        /** A light tint over a modest blur. */
        SUBTLE,
        /** The full three-tier glass material. */
        FULL;

        public GlassIntensity next() {
            GlassIntensity[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    /** The theme presets (M6). */
    public enum Theme {
        /** The dark-navy default (M1). */
        NAVY,
        /** Pure-black surfaces for OLED panels. */
        TRUE_BLACK,
        /** Follow the platform's light/dark setting. */
        SYSTEM;

        public Theme next() {
            Theme[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    /** The density steps. */
    public enum Density {
        COMPACT(ModMenuMetrics.ROW_COMPACT_DP),
        COMFORTABLE(ModMenuMetrics.ROW_COMFORTABLE_DP);

        public final int rowHeightDp;

        Density(int rowHeightDp) {
            this.rowHeightDp = rowHeightDp;
        }

        public Density next() {
            Density[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    /** The toggle style (M2/M3). */
    public enum ToggleStyle {
        /** The pill switch — the default, because a state change must look like one. */
        SWITCH,
        /** The switch with a state word beside it. */
        PILL_LABEL,
        /** A checkbox, for users who prefer the plainest control. */
        CHECKBOX;

        public ToggleStyle next() {
            ToggleStyle[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    /** The motion preference. */
    public enum Motion {
        FULL,
        REDUCED,
        /** Follow the platform's animation setting. */
        SYSTEM;

        public Motion next() {
            Motion[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    /** The default accent swatch index (blue). */
    public static final int DEFAULT_ACCENT_INDEX = 0;
    /** The accent swatches, in the order the settings tab shows them. */
    public static final int[] ACCENT_SWATCHES = {
            0xFF3E6FF0, // blue (default)
            0xFF8B5CF6, // purple
            0xFF35DC84, // green
            0xFFFFC944, // amber
            0xFFFF5C5C, // red
            0xFF9AA3AE, // mono
    };

    public static final int MIN_RADIUS_DP = 8;
    public static final int MAX_RADIUS_DP = 28;
    public static final int MIN_BLUR_FPS = 0;
    public static final int MAX_BLUR_FPS = 10;
    public static final int MIN_HUD_OPACITY = 40;
    public static final int MAX_HUD_OPACITY = 100;

    private final Theme theme;
    private final GlassIntensity glass;
    private final int cornerRadiusDp;
    private final Density density;
    private final ToggleStyle toggleStyle;
    private final Motion motion;
    private final int blurFps;
    private final int hudOpacity;
    private final int accentIndex;

    public ModMenuSettings(Theme theme, GlassIntensity glass, int cornerRadiusDp, Density density,
                           ToggleStyle toggleStyle, Motion motion, int blurFps, int hudOpacity,
                           int accentIndex) {
        this.theme = theme == null ? Theme.NAVY : theme;
        this.glass = glass == null ? GlassIntensity.FULL : glass;
        this.cornerRadiusDp = clamp(cornerRadiusDp, MIN_RADIUS_DP, MAX_RADIUS_DP);
        this.density = density == null ? Density.COMFORTABLE : density;
        this.toggleStyle = toggleStyle == null ? ToggleStyle.SWITCH : toggleStyle;
        this.motion = motion == null ? Motion.SYSTEM : motion;
        this.blurFps = clamp(blurFps, MIN_BLUR_FPS, MAX_BLUR_FPS);
        this.hudOpacity = clamp(hudOpacity, MIN_HUD_OPACITY, MAX_HUD_OPACITY);
        this.accentIndex = clamp(accentIndex, 0, ACCENT_SWATCHES.length - 1);
    }

    /** The spec's defaults: navy, full glass, 28dp, comfortable, switches, system motion, 5fps. */
    public static ModMenuSettings defaults() {
        return new ModMenuSettings(Theme.NAVY, GlassIntensity.FULL, MAX_RADIUS_DP, Density.COMFORTABLE,
                ToggleStyle.SWITCH, Motion.SYSTEM, 5, MAX_HUD_OPACITY, DEFAULT_ACCENT_INDEX);
    }

    public Theme theme() {
        return theme;
    }

    public GlassIntensity glassIntensity() {
        return glass;
    }

    public int cornerRadiusDp() {
        return cornerRadiusDp;
    }

    public Density density() {
        return density;
    }

    public ToggleStyle toggleStyle() {
        return toggleStyle;
    }

    public Motion motion() {
        return motion;
    }

    public int blurFps() {
        return blurFps;
    }

    public int hudOpacity() {
        return hudOpacity;
    }

    public int accentIndex() {
        return accentIndex;
    }

    /** The accent colour for the current swatch. */
    public int accent() {
        return ACCENT_SWATCHES[accentIndex];
    }

    /** The row height in dp for the current density. */
    public int rowHeightDp() {
        return density.rowHeightDp;
    }

    // --- Clamps the system wins ---------------------------------------------------------------

    /**
     * The glass actually drawn, after the system's accessibility settings (M6).
     *
     * <p>Reduce transparency wins outright: the OS setting is a medical/comfort need, and a
     * user-facing slider that beats it is the bug this rule prevents. A platform "remove
     * animations" reading also drops to solid, because an animating blur is motion.
     */
    public GlassIntensity effectiveGlass(boolean reduceTransparency, boolean platformAnimationsOff) {
        if (reduceTransparency || platformAnimationsOff) return GlassIntensity.OFF;
        return glass;
    }

    /**
     * The motion actually used, after the system's accessibility settings (M6).
     *
     * <p>{@link Motion#SYSTEM} resolves against the platform flag, and an explicit
     * {@link Motion#REDUCED} is honoured either way — but an explicit {@link Motion#FULL} is
     * overridden when the platform says animations are off. Same rule as the glass: the OS wins.
     */
    public Motion effectiveMotion(boolean reduceMotion, boolean platformAnimationsOff) {
        if (reduceMotion || platformAnimationsOff) return Motion.REDUCED;
        return motion == Motion.SYSTEM ? Motion.FULL : motion;
    }

    /** True when the current settings produce a blurred panel at all. */
    public boolean blurs(boolean reduceTransparency, boolean platformAnimationsOff) {
        return effectiveGlass(reduceTransparency, platformAnimationsOff) != GlassIntensity.OFF
                && blurFps > 0;
    }

    /** The white tint over the glass for the effective intensity (M4). */
    public float tint(boolean reduceTransparency, boolean platformAnimationsOff) {
        return ModMenuPalette.tintFor(effectiveGlass(reduceTransparency, platformAnimationsOff));
    }

    // --- Copy-with -----------------------------------------------------------------------------

    public ModMenuSettings withTheme(Theme next) {
        return new ModMenuSettings(next, glass, cornerRadiusDp, density, toggleStyle, motion,
                blurFps, hudOpacity, accentIndex);
    }

    public ModMenuSettings withGlassIntensity(GlassIntensity next) {
        return new ModMenuSettings(theme, next, cornerRadiusDp, density, toggleStyle, motion,
                blurFps, hudOpacity, accentIndex);
    }

    public ModMenuSettings withCornerRadiusDp(int next) {
        return new ModMenuSettings(theme, glass, next, density, toggleStyle, motion, blurFps,
                hudOpacity, accentIndex);
    }

    public ModMenuSettings withDensity(Density next) {
        return new ModMenuSettings(theme, glass, cornerRadiusDp, next, toggleStyle, motion, blurFps,
                hudOpacity, accentIndex);
    }

    public ModMenuSettings withToggleStyle(ToggleStyle next) {
        return new ModMenuSettings(theme, glass, cornerRadiusDp, density, next, motion, blurFps,
                hudOpacity, accentIndex);
    }

    public ModMenuSettings withMotion(Motion next) {
        return new ModMenuSettings(theme, glass, cornerRadiusDp, density, toggleStyle, next, blurFps,
                hudOpacity, accentIndex);
    }

    public ModMenuSettings withBlurFps(int next) {
        return new ModMenuSettings(theme, glass, cornerRadiusDp, density, toggleStyle, motion, next,
                hudOpacity, accentIndex);
    }

    public ModMenuSettings withHudOpacity(int next) {
        return new ModMenuSettings(theme, glass, cornerRadiusDp, density, toggleStyle, motion,
                blurFps, next, accentIndex);
    }

    public ModMenuSettings withAccentIndex(int next) {
        return new ModMenuSettings(theme, glass, cornerRadiusDp, density, toggleStyle, motion,
                blurFps, hudOpacity, next);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
