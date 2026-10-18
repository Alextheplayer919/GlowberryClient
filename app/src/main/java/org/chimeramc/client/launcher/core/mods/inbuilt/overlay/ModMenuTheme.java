package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.content.Context;

import org.chimeramc.client.ui.components.UiTokens;

/**
 * Colour decisions for the in-game mod menu.
 *
 * <p>All values now come from {@link UiTokens} — the shared blueberry palette — so the touch Mod Menu
 * cannot drift onto its own colours. The class is kept as a thin adapter because the adapter and the
 * overlay reference it by name; it holds no palette of its own any more.
 */
final class ModMenuTheme {

    /** Fallback accent, matching {@link UiTokens#ACCENT_500}. */
    static final int DEFAULT_ACCENT = UiTokens.ACCENT_500;

    private final int accent;
    private final boolean glowEnabled;
    private final org.chimeramc.client.ui.components.UiThemeState state;

    ModMenuTheme(Context context) {
        this(context, org.chimeramc.client.core.mods.inbuilt.manager.InbuiltModManager
                .getInstance(context).getModMenuThemeState(context));
    }

    /**
     * Builds the theme from an explicit {@link UiThemeState}, so the two independent opacity knobs
     * and the gradient accent the player chose all resolve here and the overlay reads one object.
     */
    ModMenuTheme(Context context, org.chimeramc.client.ui.components.UiThemeState state) {
        this.state = state == null
                ? org.chimeramc.client.ui.components.UiThemeState.defaults() : state;
        // A gradient accent resolves to its first stop for flat draws (text, icons); a gradient
        // surface asks for both stops through {@link #accentStart()}/{@link #accentEnd()}.
        this.accent = state == null
                ? UiTokens.resolveAccent(context)
                : state.opaqueAccent(0f);
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

    /** The shared surface colour; the per-group hue is gone (the list reads one palette now). */
    int groupColor(String groupId) {
        return UiTokens.BG_SURFACE;
    }

    /** Slightly lifted card fill tinted toward the accent, for the enabled state. */
    int enabledCardColor() {
        // The wash strength follows the primary opacity, so a low primary opacity leaves the card
        // closer to the plain surface instead of jumping to a solid accent tint.
        return UiTokens.accentWash(accent, 0.14f * state.primaryMultiplier());
    }

    int disabledCardColor() {
        return UiTokens.BG_SURFACE;
    }

    /** Translucent accent used for the enabled status pill's fill. */
    int accentFill(int alpha) {
        // The primary opacity scales every accent wash, so the "primary colour opacity" slider is
        // not cosmetic: a low value makes the accent surfaces genuinely translucent.
        int scaled = Math.round(alpha * state.primaryMultiplier());
        return UiTokens.withAlpha(accent, Math.max(0, Math.min(255, scaled)));
    }

    /** Elevation for an enabled card; zero when the user turned glow effects off. */
    float enabledElevation() {
        return glowEnabled ? 6f : 0f;
    }

    float disabledElevation() {
        return glowEnabled ? 2f : 0f;
    }
}
