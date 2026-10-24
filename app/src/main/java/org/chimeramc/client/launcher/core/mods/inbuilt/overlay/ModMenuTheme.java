package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.content.Context;

import org.chimeramc.client.ui.components.UiTokens;

/**
 * Colour decisions for the in-game mod menu (M1).
 *
 * <p>The surfaces come from {@link ModMenuPalette} — the menu's own dark-navy token set — rather
 * than from {@link UiTokens}. The menu is drawn over a live game, so its palette must not follow
 * the launcher's surfaces; the shared tokens remain the fallback for anything the menu does not
 * define, and the accent comes from the menu's own settings so the swatch the player picked is the
 * one on the switch.
 */
final class ModMenuTheme {

    /** Fallback accent, matching {@link ModMenuPalette#ACCENT}. */
    static final int DEFAULT_ACCENT = ModMenuPalette.ACCENT;

    private final int accent;
    private final boolean glowEnabled;
    private final org.chimeramc.client.ui.components.UiThemeState state;
    private final ModMenuSettings settings;

    ModMenuTheme(Context context) {
        this(context, org.chimeramc.client.core.mods.inbuilt.manager.InbuiltModManager
                .getInstance(context).getModMenuThemeState(context), null);
    }

    /**
     * Builds the theme from the menu's own settings (M6), so the accent swatch and the opacity
     * slider both resolve here and the overlay reads one object.
     */
    ModMenuTheme(Context context, ModMenuSettings settings) {
        this(context, org.chimeramc.client.core.mods.inbuilt.manager.InbuiltModManager
                .getInstance(context).getModMenuThemeState(context), settings);
    }

    /**
     * Builds the theme from an explicit {@link UiThemeState}, so the two independent opacity knobs
     * and the gradient accent the player chose all resolve here and the overlay reads one object.
     */
    ModMenuTheme(Context context, org.chimeramc.client.ui.components.UiThemeState state) {
        this(context, state, null);
    }

    private ModMenuTheme(Context context, org.chimeramc.client.ui.components.UiThemeState state,
                         ModMenuSettings settings) {
        this.state = state == null
                ? org.chimeramc.client.ui.components.UiThemeState.defaults() : state;
        this.settings = settings;
        // The menu's own accent swatch wins when the menu supplied settings; otherwise the theme
        // state's accent is used, which is what the launcher-side surfaces follow.
        this.accent = settings != null
                ? settings.accent()
                : (state == null ? UiTokens.resolveAccent(context) : state.opaqueAccent(0f));
        boolean glow = true;
        try {
            glow = new org.chimeramc.client.util.PersonalizationManager(context)
                    .isEnableGlowEffects();
        } catch (Throwable ignored) {
        }
        this.glowEnabled = glow;
    }

    int accent() {
        return accent;
    }

    /** The accent's first stop, for a gradient surface. */
    int accentStart() {
        return state.opaqueAccent(0f);
    }

    /** The accent's second stop, for a gradient surface (equal to the first for a flat accent). */
    int accentEnd() {
        return state.opaqueAccent(1f);
    }

    /** True when the accent is a two-stop gradient. */
    boolean accentIsGradient() {
        return state.isGradient();
    }

    /** The background opacity multiplier (0..1) the player chose. */
    float backgroundMultiplier() {
        return state.backgroundMultiplier();
    }

    /** The primary/accent opacity multiplier (0..1), independent of the background. */
    float primaryMultiplier() {
        return state.primaryMultiplier();
    }

    boolean glowEnabled() {
        return glowEnabled;
    }

    /** The menu's surface colour; the per-group hue is gone (the list reads one palette now). */
    int groupColor(String groupId) {
        return ModMenuPalette.BG_SURFACE_2;
    }

    /** The menu's own settings, for the callers that need the glass/density/motion decisions. */
    ModMenuSettings settings() {
        return settings;
    }

    /** The card surface for a row, from the menu's palette. */
    int cardSurface() {
        return ModMenuPalette.BG_SURFACE_2;
    }

    /** The pressed/selected row surface. */
    int pressedSurface() {
        return ModMenuPalette.BG_SURFACE_3;
    }

    /** Slightly lifted card fill tinted toward the accent, for the enabled state. */
    int enabledCardColor() {
        // The wash strength follows the primary opacity, so a low primary opacity leaves the card
        // closer to the plain surface instead of jumping to a solid accent tint. The wash is built
        // from the menu palette so a themed accent cannot pull the card onto the launcher surface.
        return ModMenuPalette.accentWash(ModMenuPalette.BG_SURFACE_2,
                0.14f * state.primaryMultiplier());
    }

    int disabledCardColor() {
        return ModMenuPalette.BG_SURFACE_2;
    }

    /** Translucent accent used for the enabled status pill's fill. */
    int accentFill(int alpha) {
        // The primary opacity scales every accent wash, so the "primary colour opacity" slider is
        // not cosmetic: a low value makes the accent surfaces genuinely translucent.
        int scaled = Math.round(alpha * state.primaryMultiplier());
        return (Math.max(0, Math.min(255, scaled)) << 24) | (accent & 0x00FFFFFF);
    }

    /**
     * Elevation for an enabled card.
     *
     * <p>Zero by default: the navy cards are separated by their fill and their hairline, and a
     * lifted card over a live game reads as fog. The glow toggle can still raise them for a player
     * who wants the lift.
     */
    float enabledElevation() {
        return glowEnabled ? 2f : 0f;
    }

    float disabledElevation() {
        return 0f;
    }

    /** The row's disabled opacity, from the tested state rule (M10). */
    float rowAlpha(ModMenuRowState rowState) {
        return rowState != null && rowState.isDimmed() ? 0.6f : 1f;
    }
}
