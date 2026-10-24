package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.chimeramc.client.core.mods.inbuilt.overlay.ModMenuSettings.Density;
import org.chimeramc.client.core.mods.inbuilt.overlay.ModMenuSettings.GlassIntensity;
import org.chimeramc.client.core.mods.inbuilt.overlay.ModMenuSettings.Motion;
import org.chimeramc.client.core.mods.inbuilt.overlay.ModMenuSettings.Theme;
import org.chimeramc.client.core.mods.inbuilt.overlay.ModMenuSettings.ToggleStyle;

/**
 * Pins the Mod Menu's appearance settings, including the clamps the system wins (M6, U11).
 *
 * <p>The clamp rules are the point: a user-facing override that beats the OS accessibility setting
 * is a bug, not a feature, so they are asserted rather than left to the UI to remember.
 */
public class ModMenuSettingsTest {

    @Test
    public void theDefaultsMatchTheSpec() {
        ModMenuSettings s = ModMenuSettings.defaults();
        assertEquals(Theme.NAVY, s.theme());
        assertEquals(GlassIntensity.FULL, s.glassIntensity());
        assertEquals(28, s.cornerRadiusDp());
        assertEquals(Density.COMFORTABLE, s.density());
        assertEquals(ToggleStyle.SWITCH, s.toggleStyle());
        assertEquals(Motion.SYSTEM, s.motion());
        assertEquals(5, s.blurFps());
        assertEquals(100, s.hudOpacity());
        assertEquals(ModMenuPalette.ACCENT, s.accent());
    }

    @Test
    public void theCornerRadiusIsClampedToTheSliderBounds() {
        assertEquals(ModMenuSettings.MIN_RADIUS_DP, s(ModMenuSettings.defaults().withCornerRadiusDp(-40)));
        assertEquals(ModMenuSettings.MAX_RADIUS_DP, s(ModMenuSettings.defaults().withCornerRadiusDp(900)));
        assertEquals(16, s(ModMenuSettings.defaults().withCornerRadiusDp(16)));
    }

    @Test
    public void theBlurRefreshIsClampedToTheSliderBounds() {
        assertEquals(0, ModMenuSettings.defaults().withBlurFps(-5).blurFps());
        assertEquals(10, ModMenuSettings.defaults().withBlurFps(60).blurFps());
    }

    @Test
    public void theHudOpacityIsClampedToTheSliderBounds() {
        assertEquals(40, ModMenuSettings.defaults().withHudOpacity(1).hudOpacity());
        assertEquals(100, ModMenuSettings.defaults().withHudOpacity(300).hudOpacity());
    }

    @Test
    public void anOutOfRangeAccentFallsBackToASwatchNotAnException() {
        ModMenuSettings low = ModMenuSettings.defaults().withAccentIndex(-3);
        ModMenuSettings high = ModMenuSettings.defaults().withAccentIndex(99);
        assertEquals(ModMenuPalette.ACCENT, low.accent());
        assertEquals(ModMenuSettings.ACCENT_SWATCHES[ModMenuSettings.ACCENT_SWATCHES.length - 1],
                high.accent());
    }

    @Test
    public void everyAccentSwatchIsDistinctAndOpaque() {
        int[] swatches = ModMenuSettings.ACCENT_SWATCHES;
        assertEquals(6, swatches.length);
        for (int i = 0; i < swatches.length; i++) {
            assertEquals("swatches are opaque", 0xFF, (swatches[i] >>> 24) & 0xFF);
            for (int j = i + 1; j < swatches.length; j++) {
                assertNotEquals("swatches are distinct", swatches[i], swatches[j]);
            }
        }
    }

    @Test
    public void aNullSettingFallsBackRatherThanThrowing() {
        ModMenuSettings s = new ModMenuSettings(null, null, 28, null, null, null, 5, 100, 0);
        assertEquals(Theme.NAVY, s.theme());
        assertEquals(GlassIntensity.FULL, s.glassIntensity());
        assertEquals(Density.COMFORTABLE, s.density());
        assertEquals(ToggleStyle.SWITCH, s.toggleStyle());
        assertEquals(Motion.SYSTEM, s.motion());
    }

    @Test
    public void reduceTransparencyForcesSolidGlass() {
        ModMenuSettings s = ModMenuSettings.defaults();
        assertEquals(GlassIntensity.OFF, s.effectiveGlass(true, false));
        assertFalse("no blur is drawn when the system asks for none", s.blurs(true, false));
        assertEquals(ModMenuPalette.TINT_OFF, s.tint(true, false), 0f);
    }

    @Test
    public void removeAnimationsAlsoForcesSolidGlass() {
        // An animating blur is motion; a platform "remove animations" must drop the glass too.
        assertEquals(GlassIntensity.OFF, ModMenuSettings.defaults().effectiveGlass(false, true));
    }

    @Test
    public void theInMenuChoiceAppliesWhenTheSystemIsNeutral() {
        ModMenuSettings s = ModMenuSettings.defaults().withGlassIntensity(GlassIntensity.SUBTLE);
        assertEquals(GlassIntensity.SUBTLE, s.effectiveGlass(false, false));
        assertEquals(ModMenuPalette.TINT_SUBTLE, s.tint(false, false), 0.0001f);
    }

    @Test
    public void aUserChosenFullMotionCannotBeatTheSystem() {
        ModMenuSettings s = ModMenuSettings.defaults().withMotion(Motion.FULL);
        assertEquals(Motion.REDUCED, s.effectiveMotion(true, false));
        assertEquals(Motion.REDUCED, s.effectiveMotion(false, true));
        assertEquals(Motion.FULL, s.effectiveMotion(false, false));
    }

    @Test
    public void systemMotionResolvesAgainstThePlatformFlag() {
        ModMenuSettings s = ModMenuSettings.defaults();
        assertEquals(Motion.FULL, s.effectiveMotion(false, false));
        assertEquals(Motion.REDUCED, s.effectiveMotion(false, true));
    }

    @Test
    public void aZeroBlurBudgetDisablesTheBlurEvenAtFullGlass() {
        ModMenuSettings s = ModMenuSettings.defaults().withBlurFps(0);
        assertFalse("a 0fps blur budget is no blur", s.blurs(false, false));
    }

    @Test
    public void theRowHeightFollowsTheDensity() {
        assertEquals(ModMenuMetrics.ROW_COMFORTABLE_DP,
                ModMenuSettings.defaults().withDensity(Density.COMFORTABLE).rowHeightDp());
        assertEquals(ModMenuMetrics.ROW_COMPACT_DP,
                ModMenuSettings.defaults().withDensity(Density.COMPACT).rowHeightDp());
    }

    @Test
    public void everyEnumCyclesBackToItself() {
        for (Theme t : Theme.values()) {
            assertEquals(t, cycle(t, Theme.values().length));
        }
        for (GlassIntensity g : GlassIntensity.values()) {
            assertEquals(g, cycleGlass(g, GlassIntensity.values().length));
        }
        for (Density d : Density.values()) {
            assertEquals(d, cycleDensity(d, Density.values().length));
        }
        for (ToggleStyle t : ToggleStyle.values()) {
            assertEquals(t, cycleToggle(t, ToggleStyle.values().length));
        }
        for (Motion m : Motion.values()) {
            assertEquals(m, cycleMotion(m, Motion.values().length));
        }
    }

    @Test
    public void withMethodsDoNotMutateTheOriginal() {
        ModMenuSettings original = ModMenuSettings.defaults();
        ModMenuSettings changed = original.withTheme(Theme.TRUE_BLACK)
                .withDensity(Density.COMPACT)
                .withAccentIndex(4)
                .withToggleStyle(ToggleStyle.CHECKBOX);
        assertEquals(Theme.NAVY, original.theme());
        assertEquals(Density.COMFORTABLE, original.density());
        assertEquals(ModMenuPalette.ACCENT, original.accent());
        assertEquals(ToggleStyle.SWITCH, original.toggleStyle());
        assertEquals(Theme.TRUE_BLACK, changed.theme());
        assertEquals(Density.COMPACT, changed.density());
        assertEquals(ModMenuSettings.ACCENT_SWATCHES[4], changed.accent());
        assertEquals(ToggleStyle.CHECKBOX, changed.toggleStyle());
    }

    private static int s(ModMenuSettings settings) {
        return settings.cornerRadiusDp();
    }

    private static Theme cycle(Theme start, int steps) {
        Theme t = start;
        for (int i = 0; i < steps; i++) t = t.next();
        return t;
    }

    private static GlassIntensity cycleGlass(GlassIntensity start, int steps) {
        GlassIntensity g = start;
        for (int i = 0; i < steps; i++) g = g.next();
        return g;
    }

    private static Density cycleDensity(Density start, int steps) {
        Density d = start;
        for (int i = 0; i < steps; i++) d = d.next();
        return d;
    }

    private static ToggleStyle cycleToggle(ToggleStyle start, int steps) {
        ToggleStyle t = start;
        for (int i = 0; i < steps; i++) t = t.next();
        return t;
    }

    private static Motion cycleMotion(Motion start, int steps) {
        Motion m = start;
        for (int i = 0; i < steps; i++) m = m.next();
        return m;
    }
}
