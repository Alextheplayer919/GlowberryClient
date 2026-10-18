package org.chimeramc.client.ui.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the Mod Menu theme rules: the two opacity knobs move independently, the clamp holds, and a
 * gradient accent resolves along its axis. Pure JVM — no device.
 */
public class UiThemeStateTest {

    @Test
    public void theTwoOpacitiesAreIndependent() {
        UiThemeState state = UiThemeState.defaults().withBackgroundOpacity(40);
        assertEquals(40, state.backgroundOpacity());
        assertEquals(UiThemeState.DEFAULT_PRIMARY_OPACITY, state.primaryOpacity());
        UiThemeState other = state.withPrimaryOpacity(60);
        assertEquals(40, other.backgroundOpacity());
        assertEquals(60, other.primaryOpacity());
    }

    @Test
    public void opacityIsClampedIntoTheUsableRange() {
        assertEquals(UiThemeState.MIN_OPACITY, UiThemeState.clampOpacity(0));
        assertEquals(UiThemeState.MAX_OPACITY, UiThemeState.clampOpacity(500));
        assertEquals(55, UiThemeState.clampOpacity(55));
        assertEquals(UiThemeState.MIN_OPACITY, UiThemeState.defaults().withBackgroundOpacity(-10)
                .backgroundOpacity());
    }

    @Test
    public void multipliersTrackThePercentages() {
        UiThemeState state = new UiThemeState(50, 25, UiThemeState.Accent.flat(0xFF0F5BE4));
        assertEquals(0.5f, state.backgroundMultiplier(), 0.001f);
        assertEquals(0.25f, state.primaryMultiplier(), 0.001f);
    }

    @Test
    public void aFlatAccentHasNoGradient() {
        UiThemeState.Accent accent = UiThemeState.Accent.flat(0xFF112233);
        assertFalse(accent.gradient);
        assertEquals(0xFF112233, accent.colorAt(0f));
        assertEquals(0xFF112233, accent.colorAt(1f));
    }

    @Test
    public void aGradientResolvesFromStartToEnd() {
        UiThemeState.Accent accent = UiThemeState.Accent.gradient(0xFF000000, 0xFFFFFFFF);
        assertTrue(accent.gradient);
        assertEquals(0xFF000000, accent.colorAt(0f));
        assertEquals(0xFFFFFFFF, accent.colorAt(1f));
        int mid = accent.colorAt(0.5f);
        int r = (mid >>> 16) & 0xFF;
        assertTrue("midpoint should be a mid-grey, got " + r, r > 100 && r < 155);
    }

    @Test
    public void anIdenticalGradientCollapsesToFlat() {
        UiThemeState.Accent accent = UiThemeState.Accent.gradient(0xFF123456, 0xFF123456);
        assertFalse(accent.gradient);
    }

    @Test
    public void thePrimaryOpacityScalesTheAccentAlpha() {
        UiThemeState state = new UiThemeState(80, 50, UiThemeState.Accent.flat(0xFF0F5BE4));
        int resolved = state.resolvedAccent(0f);
        assertEquals(128, (resolved >>> 24));
        // Text/icon accent stays fully opaque so it never becomes unreadable.
        assertEquals(255, (state.opaqueAccent(0f) >>> 24));
    }

    @Test
    public void aNullAccentFallsBackToTheDefault() {
        UiThemeState state = new UiThemeState(80, 100, null);
        assertFalse(state.isGradient());
        assertEquals(UiTokens.ACCENT_500, state.opaqueAccent(0f));
    }

    @Test
    public void aGradientAccentReportsGradient() {
        UiThemeState state = UiThemeState.defaults()
                .withAccent(UiThemeState.Accent.gradient(0xFF0F5BE4, 0xFFA82E9E));
        assertTrue(state.isGradient());
        assertNotEquals(state.opaqueAccent(0f), state.opaqueAccent(1f));
    }
}
