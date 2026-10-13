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

    ModMenuTheme(Context context) {
        this.accent = UiTokens.resolveAccent(context);
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

    boolean glowEnabled() {
        return glowEnabled;
    }

    /** The shared surface colour; the per-group hue is gone (the list reads one palette now). */
    int groupColor(String groupId) {
        return UiTokens.BG_SURFACE;
    }

    /** Slightly lifted card fill tinted toward the accent, for the enabled state. */
    int enabledCardColor() {
        return UiTokens.accentWash(accent, 0.14f);
    }

    int disabledCardColor() {
        return UiTokens.BG_SURFACE;
    }

    /** Translucent accent used for the enabled status pill's fill. */
    int accentFill(int alpha) {
        return UiTokens.withAlpha(accent, alpha);
    }

    /** Elevation for an enabled card; zero when the user turned glow effects off. */
    float enabledElevation() {
        return glowEnabled ? 6f : 0f;
    }

    float disabledElevation() {
        return glowEnabled ? 2f : 0f;
    }
}
