package org.chimeramc.client.ui.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the frosted-glass material: the fill-alpha bounds that keep text readable, the blur tiers, the
 * breathing curve and the backdrop-driven alpha. Pure JVM — no device.
 */
public class UiGlassTest {

    @Test
    public void aHigherBlurMeansAMoreTransparentFill() {
        int world = UiGlass.panelAlpha(UiGlass.BLUR_WORLD, false);
        int panel = UiGlass.panelAlpha(UiGlass.BLUR_PANEL, false);
        int widget = UiGlass.panelAlpha(UiGlass.BLUR_WIDGET, false);
        assertTrue("world blur should be the clearest", world < panel);
        assertTrue("widget blur should be the most solid", widget > panel);
    }

    @Test
    public void textBearingSurfacesNeverDropBelowReadable() {
        for (float blur = 0f; blur <= 1f; blur += 0.1f) {
            int alpha = UiGlass.panelAlpha(blur, true);
            assertTrue("alpha " + alpha + " at blur " + blur,
                    alpha >= UiGlass.ALPHA_READABLE);
        }
    }

    @Test
    public void theAlphaAlwaysStaysInBounds() {
        assertEquals(UiGlass.ALPHA_MIN, UiGlass.panelAlpha(2f, false));
        assertEquals(UiGlass.ALPHA_SOLID, UiGlass.panelAlpha(-1f, false));
    }

    @Test
    public void aHoveredEdgeIsTheAccentAndAFocusedOneIsThicker() {
        assertEquals(UiTokens.STROKE_HAIRLINE, UiGlass.strokeColor(UiTokens.ACCENT_500, false));
        assertEquals(UiTokens.withAlpha(UiTokens.ACCENT_500, 0xCC),
                UiGlass.strokeColor(UiTokens.ACCENT_500, true));
        assertTrue(UiGlass.strokeWidth(true, 1f) > UiGlass.strokeWidth(false, 1f));
    }

    @Test
    public void theGlowBrightensOnHover() {
        assertTrue(UiGlass.glowAlpha(true) > UiGlass.glowAlpha(false));
    }

    @Test
    public void breatheIsASmoothZeroToOneCycle() {
        assertEquals(0f, UiGlass.breathe(0, 1000), 0.001f);
        assertEquals(1f, UiGlass.breathe(500, 1000), 0.001f);
        assertEquals(0f, UiGlass.breathe(1000, 1000), 0.001f);
        // A non-positive period must not produce NaN.
        assertEquals(0f, UiGlass.breathe(10, 0), 0.001f);
        // A negative time still yields a value in range.
        float v = UiGlass.breathe(-250, 1000);
        assertTrue(v >= 0f && v <= 1f);
    }

    @Test
    public void aBrightBackdropNeedsAMoreSolidFillForDarkText() {
        // Dark text over a bright backdrop needs a more solid fill; over black it can be clearer.
        int darkOnBlack = UiGlass.alphaForBackdrop(0.0, 0xFF000000);
        int darkOnWhite = UiGlass.alphaForBackdrop(1.0, 0xFF000000);
        assertTrue("dark text on a bright backdrop needs more fill", darkOnWhite > darkOnBlack);
        assertTrue(darkOnBlack >= UiGlass.ALPHA_READABLE);
    }

    @Test
    public void lightTextNeedsAMoreSolidFillOnADarkBackdrop() {
        int lightOnBlack = UiGlass.alphaForBackdrop(0.0, UiTokens.TEXT_PRIMARY);
        int lightOnWhite = UiGlass.alphaForBackdrop(1.0, UiTokens.TEXT_PRIMARY);
        assertTrue("light text on a dark backdrop needs more fill", lightOnBlack > lightOnWhite);
        assertTrue(lightOnWhite >= UiGlass.ALPHA_READABLE);
    }

    @Test
    public void radiusTiersAreOrderedFromWindowToButton() {
        assertEquals(UiTokens.RADIUS_WINDOW, UiGlass.radius(0));
        assertEquals(UiTokens.RADIUS_CARD, UiGlass.radius(1));
        assertEquals(UiTokens.RADIUS_CHIP, UiGlass.radius(2));
        assertEquals(UiTokens.RADIUS_BUTTON, UiGlass.radius(3));
    }

    @Test
    public void panelFillPacksTheAlphaIntoTheColour() {
        int fill = UiGlass.panelFill(UiTokens.BG_SURFACE, UiGlass.BLUR_WIDGET, true);
        assertEquals(UiGlass.panelAlpha(UiGlass.BLUR_WIDGET, true), (fill >>> 24) & 0xFF);
        assertEquals(UiTokens.BG_SURFACE & 0x00FFFFFF, fill & 0x00FFFFFF);
    }
}
