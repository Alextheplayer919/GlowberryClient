package org.chimeramc.client.ui.components;

/**
 * The shared "frosted glass" material: the fill alpha, the 1dp stroke, the depth (shadow/inner
 * glow) and the blur level for every panel, dock, pod and widget.
 *
 * <p>The design language has one material and several intensities of it, so a screen must not pick
 * its own translucency — that is how the launch panel ends up foggier than the mod menu. The three
 * blur levels (world background, menu panel, widget) and the panel opacity are named here, and the
 * derivations are pure integer maths so the contrast and the "still readable at 40% opacity" rules
 * are unit-testable without a device.
 *
 * <p>Alpha is the honest part of the material: a real-time blur needs a shader over the game's
 * frame, which the launcher cannot install on every surface, so the glass is approximated by a
 * translucent fill plus a hairline and a shadow. The fill alpha is the knob that trades solidity for
 * see-through, and {@link #readableFill} is the floor below which text stops being legible.
 */
public final class UiGlass {

    private UiGlass() {
    }

    // --- Blur levels -------------------------------------------------------------------------

    /** The world backdrop behind the launcher window: the heaviest blur, the most transparency. */
    public static final float BLUR_WORLD = 1.0f;
    /** A menu panel floating over the world. */
    public static final float BLUR_PANEL = 0.72f;
    /** A widget (button, dock, chip) on top of a panel: the lightest blur, the most solid fill. */
    public static final float BLUR_WIDGET = 0.46f;

    // --- Fill alpha bounds -------------------------------------------------------------------

    /** Fully see-through. */
    public static final int ALPHA_CLEAR = 0;
    /** The lightest fill a surface may use and stay a surface (rgba alpha 0.24). */
    public static final int ALPHA_MIN = 61;
    /** A solid-enough panel that text on it is readable over any background (alpha 0.72). */
    public static final int ALPHA_READABLE = 184;
    /** A near-opaque surface (alpha 0.96). */
    public static final int ALPHA_SOLID = 245;

    // --- Depth -------------------------------------------------------------------------------

    /** The window's drop shadow: a soft, wide, low-alpha halo so the panel floats. */
    public static final float SHADOW_RADIUS_DP = 24f;
    public static final float SHADOW_DY_DP = 8f;
    public static final float SHADOW_ALPHA = 0.38f;

    /** The pod's tinted pedestal shadow, which grounds the head. */
    public static final float PEDESTAL_RADIUS_FRACTION = 0.62f;
    public static final float PEDESTAL_ALPHA = 0.30f;

    /** The inner glow that catches the light on a hovered or focused surface. */
    public static final float GLOW_ALPHA_IDLE = 0.16f;
    public static final float GLOW_ALPHA_HOVER = 0.42f;

    // --- Stroke ------------------------------------------------------------------------------

    /** The 1dp hairline that defines an edge. */
    public static final float STROKE_HAIRLINE_DP = 1f;
    /** A focused/selected edge, which the spec draws thicker so it reads as "caught the light". */
    public static final float STROKE_FOCUS_DP = 1.5f;

    /**
     * The fill alpha for a surface at a blur level, clamped into the legible range.
     *
     * <p>A higher blur (the world backdrop) means a more transparent fill; a widget blurs least and
     * is therefore the most solid. The result never drops below {@link #ALPHA_READABLE} for a
     * text-bearing surface, because a "glass" panel that makes its own text unreadable is a defect,
     * not a style.
     */
    public static int panelAlpha(float blur, boolean textBearing) {
        float clamped = Math.max(0f, Math.min(1f, blur));
        // Linear from ALPHA_SOLID at blur 0 to ALPHA_MIN at blur 1.
        int alpha = (int) (ALPHA_SOLID - clamped * (ALPHA_SOLID - ALPHA_MIN));
        if (textBearing) alpha = Math.max(alpha, ALPHA_READABLE);
        return Math.max(ALPHA_MIN, Math.min(ALPHA_SOLID, alpha));
    }

    /** A surface fill: the window colour at the alpha for the given blur level. */
    public static int panelFill(int baseColor, float blur, boolean textBearing) {
        return UiTokens.withAlpha(baseColor, panelAlpha(blur, textBearing));
    }

    /**
     * The border stroke for a surface: the accent at full strength when hovered/focused, otherwise
     * the faint hairline. A hovered element's edge is the accent so the interactivity is visible.
     */
    public static int strokeColor(int accent, boolean highlighted) {
        return highlighted ? UiTokens.withAlpha(accent, 0xCC) : UiTokens.STROKE_HAIRLINE;
    }

    /** The stroke width in px for a state, converted from the dp token. */
    public static float strokeWidth(boolean highlighted, float density) {
        float dp = highlighted ? STROKE_FOCUS_DP : STROKE_HAIRLINE_DP;
        return Math.max(1f, dp * Math.max(0.5f, density));
    }

    /** The inner-glow alpha for a state. */
    public static float glowAlpha(boolean hovered) {
        return hovered ? GLOW_ALPHA_HOVER : GLOW_ALPHA_IDLE;
    }

    /** The drop-shadow radius in px for the window at a density. */
    public static float shadowRadiusPx(float density) {
        return SHADOW_RADIUS_DP * Math.max(0.5f, density);
    }

    /**
     * The breathing factor for an idle glow, 0..1, from a monotonic clock.
     *
     * <p>A pure sine on the clock so the pulse is smooth and the caller only feeds the time; a
     * hardcoded animation curve in a view would not be testable. A non-finite or negative time
     * reads as the start of the cycle rather than producing a NaN alpha.
     */
    public static float breathe(long timeMs, long periodMs) {
        if (periodMs <= 0) return 0f;
        double phase = ((timeMs % periodMs) + periodMs) % periodMs / (double) periodMs;
        return (float) (0.5 - 0.5 * Math.cos(phase * 2.0 * Math.PI));
    }

    /**
     * The fill alpha a translucent surface should use so its own text stays readable, given the
     * backdrop's average luminance (0 = black, 1 = white) and the text colour.
     *
     * <p>Used by the launch tab, whose panel sits over a live game panorama of unknown brightness.
     * A darker backdrop needs less fill; a bright one needs more so dark text does not wash out.
     */
    public static int alphaForBackdrop(double backdropLuminance, int textColor) {
        double lum = Math.max(0.0, Math.min(1.0, backdropLuminance));
        boolean darkText = UiTokens.luminance(textColor) < 0.5;
        // Dark text on a bright backdrop wants a more solid fill; light text on a dark backdrop
        // can afford to be clearer. The two curves meet at a mid-grey backdrop.
        double base = darkText ? lum : (1.0 - lum);
        int alpha = (int) (ALPHA_MIN + base * (ALPHA_SOLID - ALPHA_MIN));
        return Math.max(ALPHA_READABLE, Math.min(ALPHA_SOLID, alpha));
    }

    /** A rounded-corner radius for a material tier (window, card, chip, button). */
    public static int radius(int tier) {
        switch (tier) {
            case 0:
                return UiTokens.RADIUS_WINDOW;
            case 1:
                return UiTokens.RADIUS_CARD;
            case 2:
                return UiTokens.RADIUS_CHIP;
            default:
                return UiTokens.RADIUS_BUTTON;
        }
    }
}
