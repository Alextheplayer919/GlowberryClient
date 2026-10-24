package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins where a one-shot action row appears (M3).
 *
 * <p>The action catalog and the row kind existed but nothing rendered them: the adapter had no
 * action view type, so "Reset HUD Position" and "Reload Modules" were unreachable. This rule is what
 * decides the section an action lands in, so it is the piece that has to be right.
 */
public class ModMenuActionVisibilityTest {

    @Test
    public void aSectionFilterShowsItsOwnActions() {
        assertTrue(ModMenuActionVisibility.isVisibleIn("INBUILT", "inbuilt"));
        assertTrue(ModMenuActionVisibility.isVisibleIn("PVP", "pvp"));
        assertFalse(ModMenuActionVisibility.isVisibleIn("PVP", "inbuilt"));
        assertFalse(ModMenuActionVisibility.isVisibleIn("INBUILT", "pvp"));
    }

    @Test
    public void allShowsEveryActionBecauseEverySectionIsOnScreen() {
        for (ModMenuAction action : ModMenuActionCatalog.all()) {
            assertTrue(action.getId(),
                    ModMenuActionVisibility.isVisibleIn("ALL", action.getGroupId()));
        }
    }

    @Test
    public void aMixedViewShowsNoActionsRatherThanAGuessedSubset() {
        // Favourites, enabled and the visuals/misc tabs are views across several sections, not a
        // section, so there is no correct action set for them.
        assertFalse(ModMenuActionVisibility.isVisibleIn("FAVORITES", "inbuilt"));
        assertFalse(ModMenuActionVisibility.isVisibleIn("ENABLED", "inbuilt"));
        assertFalse(ModMenuActionVisibility.isVisibleIn("VISUALS", "inbuilt"));
        assertFalse(ModMenuActionVisibility.isVisibleIn("MISC", "inbuilt"));
        assertFalse(ModMenuActionVisibility.isVisibleIn("EXTERNAL", "inbuilt"));
    }

    @Test
    public void nullInputsAreSafeAndShowNothing() {
        assertFalse(ModMenuActionVisibility.isVisibleIn(null, "inbuilt"));
        assertFalse(ModMenuActionVisibility.isVisibleIn("INBUILT", null));
        assertNull(ModMenuActionVisibility.sectionIdFor(null));
        assertNull(ModMenuActionVisibility.sectionIdFor("FAVORITES"));
    }

    @Test
    public void everyCatalogActionLandsInASectionThatCanShowIt() {
        // Every action's own section must show it, or the action is dead on arrival.
        for (ModMenuAction action : ModMenuActionCatalog.all()) {
            String section = ModMenuActionVisibility.sectionIdFor(
                    action.getGroupId().toUpperCase(java.util.Locale.ROOT));
            assertEquals("action " + action.getId() + " is filed under a real section",
                    action.getGroupId(), section);
        }
    }
}
