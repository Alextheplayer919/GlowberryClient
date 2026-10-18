package org.chimeramc.client.ui.components;

/**
 * The Mod Menu's user-facing theme: the background opacity and the primary (accent) opacity are
 * independent knobs, and the primary colour may be a flat RGB value or a two-stop gradient.
 *
 * <p>The spec calls for "adjust the opacity of the background and primary colors independently" and
 * a "theme manager [that] supports RGB and gradient colors". Those are two separate values that used
 * to be one (the panel alpha), so a player who wanted a clear panel but a solid accent had no way to
 * ask for it. This holds the pair plus the accent shape, and resolves the actual ARGB values.
 *
 * <p>Android-free so the resolution rules — the opacity clamp, the gradient midpoint, the fallback
 * when only one stop is set — are unit-tested without a device.
 */
public final class UiThemeState {

    /** A flat or gradient accent. */
    public static final class Accent {
        public final int start;
        public final int end;
        public final boolean gradient;

        private Accent(int start, int end, boolean gradient) {
            this.start = start;
            this.end = end;
            this.gradient = gradient;
        }

        /** A flat accent (start == end, no gradient). */
        public static Accent flat(int color) {
            return new Accent(color, color, false);
        }

        /** A two-stop gradient. A missing second stop collapses to the first. */
        public static Accent gradient(int start, int end) {
            if (start == end) return flat(start);
            return new Accent(start, end, true);
        }

        /** The colour at a fraction along the accent, 0 = start, 1 = end. */
        public int colorAt(float fraction) {
            if (!gradient) return start;
            return UiTokens.blend(start, end, Math.max(0f, Math.min(1f, fraction)));
        }
    }

    /** The default background opacity, percent. */
    public static final int DEFAULT_BACKGROUND_OPACITY = 78;
    /** The default primary/accent opacity, percent. */
    public static final int DEFAULT_PRIMARY_OPACITY = 100;
    /** The lowest a slider may go; below this a panel stops being a panel. */
    public static final int MIN_OPACITY = 20;
    public static final int MAX_OPACITY = 100;

    private final int backgroundOpacity;
    private final int primaryOpacity;
    private final Accent accent;

    public UiThemeState(int backgroundOpacity, int primaryOpacity, Accent accent) {
        this.backgroundOpacity = clampOpacity(backgroundOpacity);
        this.primaryOpacity = clampOpacity(primaryOpacity);
        this.accent = accent == null ? Accent.flat(UiTokens.ACCENT_500) : accent;
    }

    /** The shipped defaults: a clear-ish panel and a fully solid accent. */
    public static UiThemeState defaults() {
        return new UiThemeState(DEFAULT_BACKGROUND_OPACITY, DEFAULT_PRIMARY_OPACITY,
                Accent.flat(UiTokens.ACCENT_500));
    }

    public static int clampOpacity(int percent) {
        return Math.max(MIN_OPACITY, Math.min(MAX_OPACITY, percent));
    }

    public int backgroundOpacity() {
        return backgroundOpacity;
    }

    public int primaryOpacity() {
        return primaryOpacity;
    }

    public Accent accent() {
        return accent;
    }

    /**
     * The background opacity as a 0..1 multiplier for a drawing surface.
     *
     * <p>Applied on top of {@link UiGlass}'s own fill, so the material still decides the base
     * translucency and the player's slider only scales it — the two cannot fight and produce a
     * panel that is neither glass nor solid.
     */
    public float backgroundMultiplier() {
        return backgroundOpacity / 100f;
    }

    /** The primary opacity as a 0..1 multiplier for accent fills and strokes. */
    public float primaryMultiplier() {
        return primaryOpacity / 100f;
    }

    /** The accent with the player's primary opacity applied. */
    public int resolvedAccent(float fraction) {
        int base = accent.colorAt(fraction);
        return UiTokens.withAlpha(base, Math.round(255 * primaryMultiplier()));
    }

    /** The accent at full opacity, for text and icons that must stay legible. */
    public int opaqueAccent(float fraction) {
        return UiTokens.withAlpha(accent.colorAt(fraction), 0xFF);
    }

    /** True when the accent is a gradient (two different stops). */
    public boolean isGradient() {
        return accent.gradient;
    }

    /** A copy with a new background opacity, clamped. */
    public UiThemeState withBackgroundOpacity(int percent) {
        return new UiThemeState(percent, primaryOpacity, accent);
    }

    /** A copy with a new primary opacity, clamped. */
    public UiThemeState withPrimaryOpacity(int percent) {
        return new UiThemeState(backgroundOpacity, percent, accent);
    }

    /** A copy with a new accent. */
    public UiThemeState withAccent(Accent next) {
        return new UiThemeState(backgroundOpacity, primaryOpacity, next);
    }
}
