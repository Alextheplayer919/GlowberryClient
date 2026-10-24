package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the Mod Menu's dark-navy palette and its legibility (M1).
 *
 * <p>The two rules the palette exists to enforce are asserted here rather than trusted to review:
 * nothing is pure black or pure white, and the text tokens actually clear a contrast floor on the
 * surfaces they are used on.
 */
public class ModMenuPaletteTest {

    @Test
    public void noTokenIsPureBlackOrPureWhite() {
        int[] tokens = {
                ModMenuPalette.BG_BASE, ModMenuPalette.BG_SURFACE_1, ModMenuPalette.BG_SURFACE_2,
                ModMenuPalette.BG_SURFACE_3, ModMenuPalette.BORDER_HAIRLINE,
                ModMenuPalette.TEXT_PRIMARY, ModMenuPalette.TEXT_SECONDARY,
                ModMenuPalette.TEXT_TERTIARY, ModMenuPalette.ACCENT, ModMenuPalette.ACCENT_MUTED,
                ModMenuPalette.STATE_SUCCESS, ModMenuPalette.STATE_WARN, ModMenuPalette.STATE_ERROR,
        };
        for (int token : tokens) {
            assertNotEquals("pure black is not a token", 0xFF000000, token);
            assertNotEquals("pure white is not a token", 0xFFFFFFFF, token);
        }
    }

    @Test
    public void bodyTextClearsTheContrastFloorOnCards() {
        // 4.5:1 is the WCAG AA floor for body text. Module names and descriptions are body text.
        assertTrue(ModMenuPalette.contrastRatio(ModMenuPalette.TEXT_PRIMARY,
                ModMenuPalette.BG_SURFACE_2) >= 4.5);
        assertTrue(ModMenuPalette.contrastRatio(ModMenuPalette.TEXT_SECONDARY,
                ModMenuPalette.BG_SURFACE_2) >= 4.5);
    }

    @Test
    public void sectionLabelsClearTheLargeTextFloor() {
        // Section labels and disabled text use text.tertiary, which is deliberately dimmer. It is
        // held to the 3:1 non-body floor; anything that must be read as body text uses secondary.
        assertTrue(ModMenuPalette.contrastRatio(ModMenuPalette.TEXT_TERTIARY,
                ModMenuPalette.BG_SURFACE_2) >= 3.0);
    }

    @Test
    public void stateColorsAreLegibleOnACard() {
        assertTrue(ModMenuPalette.contrastRatio(ModMenuPalette.STATE_ERROR,
                ModMenuPalette.BG_SURFACE_2) >= 4.5);
        assertTrue(ModMenuPalette.contrastRatio(ModMenuPalette.STATE_WARN,
                ModMenuPalette.BG_SURFACE_2) >= 4.5);
        assertTrue(ModMenuPalette.contrastRatio(ModMenuPalette.STATE_SUCCESS,
                ModMenuPalette.BG_SURFACE_2) >= 4.5);
    }

    @Test
    public void theAccentIsDistinguishableFromTheSurfaceItSitsOn() {
        // The accent is the switch track and the focus ring; it has to be visible against the panel
        // base without being loud enough to read as a second accent.
        double ratio = ModMenuPalette.contrastRatio(ModMenuPalette.ACCENT,
                ModMenuPalette.BG_SURFACE_1);
        assertTrue("accent is visible on the panel", ratio >= 3.0);
        assertTrue("accent is not a wash", ratio <= 8.0);
    }

    @Test
    public void blendClampsItsFraction() {
        assertEquals(ModMenuPalette.BG_SURFACE_2,
                ModMenuPalette.blend(ModMenuPalette.BG_SURFACE_2, ModMenuPalette.ACCENT, -1f));
        assertEquals(ModMenuPalette.blend(ModMenuPalette.BG_SURFACE_2, ModMenuPalette.ACCENT, 1f),
                ModMenuPalette.blend(ModMenuPalette.BG_SURFACE_2, ModMenuPalette.ACCENT, 5f));
    }

    @Test
    public void blendKeepsTheUnderlyingAlpha() {
        int under = 0x80FF0000;
        int out = ModMenuPalette.blend(under, 0xFF00FF00, 0.5f);
        assertEquals(0x80, (out >>> 24) & 0xFF);
    }

    @Test
    public void accentAlphaMasksTheColorAndKeepsTheRequestedAlpha() {
        assertEquals(0x80, (ModMenuPalette.accentAlpha(0x80) >>> 24) & 0xFF);
        assertEquals(ModMenuPalette.ACCENT & 0x00FFFFFF, ModMenuPalette.accentAlpha(0x80) & 0x00FFFFFF);
        assertEquals(0xFF, (ModMenuPalette.accentAlpha(999) >>> 24) & 0xFF);
    }

    @Test
    public void theMutedTrackIsTranslucentSoThePanelShowsThrough() {
        int track = ModMenuPalette.mutedTrack();
        int alpha = (track >>> 24) & 0xFF;
        assertTrue("the off track is translucent", alpha < 0xFF);
        assertTrue("the off track is still visible", alpha > 0x40);
        assertEquals(ModMenuPalette.ACCENT_MUTED & 0x00FFFFFF, track & 0x00FFFFFF);
    }

    @Test
    public void theFailureDotIsRedAndTheUnsupportedDotIsAmber() {
        assertEquals(ModMenuPalette.STATE_ERROR, ModMenuPalette.dotFor(ModMenuRowState.FAILED));
        assertEquals(ModMenuPalette.STATE_ERROR, ModMenuPalette.dotFor(ModMenuRowState.CRASHED));
        assertEquals(ModMenuPalette.STATE_WARN, ModMenuPalette.dotFor(ModMenuRowState.UNSUPPORTED));
        assertEquals(0, ModMenuPalette.dotFor(ModMenuRowState.LOADED));
        assertEquals(0, ModMenuPalette.dotFor(ModMenuRowState.DISABLED));
    }

    @Test
    public void theGlassTintRisesWithIntensity() {
        assertTrue(ModMenuPalette.tintFor(ModMenuSettings.GlassIntensity.OFF)
                < ModMenuPalette.tintFor(ModMenuSettings.GlassIntensity.SUBTLE));
        assertTrue(ModMenuPalette.tintFor(ModMenuSettings.GlassIntensity.SUBTLE)
                < ModMenuPalette.tintFor(ModMenuSettings.GlassIntensity.FULL));
        assertEquals(0f, ModMenuPalette.tintFor(ModMenuSettings.GlassIntensity.OFF), 0f);
    }

    @Test
    public void thePanelScrimIsBlackAndTranslucent() {
        int alpha = (ModMenuPalette.PANEL_SCRIM >>> 24) & 0xFF;
        assertTrue(alpha > 0);
        assertFalse(alpha == 0xFF);
    }
}
