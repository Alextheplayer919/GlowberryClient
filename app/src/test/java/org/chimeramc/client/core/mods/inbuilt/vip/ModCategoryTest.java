package org.chimeramc.client.core.mods.inbuilt.vip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.chimeramc.client.core.mods.inbuilt.UnifiedMod;
import org.chimeramc.client.core.mods.inbuilt.model.ModIds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Pins the mod panel's category rule: ALL shows everything, FAVORITES shows exactly the pinned set,
 * and the visual/misc/PvP tabs classify the modules the spec lists. The category must never hide a
 * module that is enabled, so the union of VISUALS + PVP + MISC + the unclassified remainder must be
 * the whole list.
 */
public class ModCategoryTest {

    private static UnifiedMod mod(String id, String name) {
        return new UnifiedMod(id, name, "", "inbuilt", UnifiedMod.Source.INBUILT, false,
                Collections.emptyList(), false, "inbuilt", "Inbuilt Features",
                (m, e) -> { }, (m, c, v) -> { }, null);
    }

    private static List<UnifiedMod> sample() {
        List<UnifiedMod> mods = new ArrayList<>();
        mods.add(mod(ModIds.ZOOM, "Zoom"));
        mods.add(mod(ModIds.FULLBRIGHT, "Fullbright"));
        mods.add(mod(ModIds.TIME_CHANGER, "Time Changer"));
        mods.add(mod(ModIds.MOTION_BLUR, "Motion Blur"));
        mods.add(mod(ModIds.LIST_PLAYERS, "List Players"));
        mods.add(mod(ModIds.DEBUG_MENU, "Debug Menu"));
        mods.add(mod(ModIds.SNAPLOOK, "Snaplook"));
        mods.add(mod(ModIds.HITBOX, "Hitboxes"));
        return mods;
    }

    @Test
    public void allShowsEverything() {
        assertEquals(sample().size(), ModCategory.filter(ModCategory.Tab.ALL, sample(),
                Collections.emptySet()).size());
    }

    @Test
    public void favoritesShowsExactlyThePinnedSet() {
        List<UnifiedMod> mods = sample();
        Set<String> pinned = Set.of(mods.get(0).getStableKey(), mods.get(3).getStableKey());
        List<UnifiedMod> shown = ModCategory.filter(ModCategory.Tab.FAVORITES, mods, pinned);
        assertEquals(2, shown.size());
        assertTrue(shown.contains(mods.get(0)));
        assertTrue(shown.contains(mods.get(3)));

        // An empty pinned set yields an empty list, which is what the empty state keys on.
        assertTrue(ModCategory.filter(ModCategory.Tab.FAVORITES, mods,
                Collections.emptySet()).isEmpty());
    }

    @Test
    public void theVisualsTabCollectsTheVisualModules() {
        List<UnifiedMod> shown = ModCategory.filter(ModCategory.Tab.VISUALS, sample(),
                Collections.emptySet());
        List<String> ids = new ArrayList<>();
        for (UnifiedMod m : shown) ids.add(m.getId());
        assertTrue(ids.contains(ModIds.FULLBRIGHT));
        assertTrue(ids.contains(ModIds.TIME_CHANGER));
        assertTrue(ids.contains(ModIds.MOTION_BLUR));
        assertTrue(ids.contains(ModIds.ZOOM));
        assertFalse(ids.contains(ModIds.LIST_PLAYERS));
    }

    @Test
    public void theMiscTabCollectsTheUtilityModules() {
        List<UnifiedMod> shown = ModCategory.filter(ModCategory.Tab.MISC, sample(),
                Collections.emptySet());
        List<String> ids = new ArrayList<>();
        for (UnifiedMod m : shown) ids.add(m.getId());
        assertTrue(ids.contains(ModIds.LIST_PLAYERS));
        assertTrue(ids.contains(ModIds.DEBUG_MENU));
        assertTrue(ids.contains(ModIds.SNAPLOOK));
        assertFalse(ids.contains(ModIds.FULLBRIGHT));
    }

    @Test
    public void thePvpTabCollectsPvpAndVoiceModules() {
        List<UnifiedMod> shown = ModCategory.filter(ModCategory.Tab.PVP, sample(),
                Collections.emptySet());
        List<String> ids = new ArrayList<>();
        for (UnifiedMod m : shown) ids.add(m.getId());
        assertTrue(ids.contains(ModIds.HITBOX));
        assertFalse(ids.contains(ModIds.ZOOM));
    }

    @Test
    public void searchMatchesOnNameCaseInsensitively() {
        UnifiedMod zoom = mod(ModIds.ZOOM, "Zoom");
        assertTrue(ModCategory.matchesQuery(zoom, ""));
        assertTrue(ModCategory.matchesQuery(zoom, "zoo"));
        assertTrue(ModCategory.matchesQuery(zoom, "ZOOM"));
        assertFalse(ModCategory.matchesQuery(zoom, "fire"));
    }

    @Test
    public void searchAlsoMatchesTheDescriptionAndCategory() {
        // The spec: the search filters by name, description and category.
        UnifiedMod mod = new UnifiedMod(ModIds.ZOOM, "Zoom", "Hold to zoom in smoothly.",
                "inbuilt", UnifiedMod.Source.INBUILT, false, Collections.emptyList(), false,
                "visual", "Visual", (m, e) -> { }, (m, c, v) -> { }, null);
        assertTrue("a word from the description must match",
                ModCategory.matchesQuery(mod, "smoothly"));
        assertTrue("the category name must match", ModCategory.matchesQuery(mod, "visual"));
        assertFalse(ModCategory.matchesQuery(mod, "fire"));
    }

    @Test
    public void aNullTabOrModuleIsSafe() {
        assertTrue(ModCategory.matches(null, mod(ModIds.ZOOM, "Zoom"), Collections.emptySet()));
        assertFalse(ModCategory.matches(ModCategory.Tab.VISUALS, null, Collections.emptySet()));
        assertTrue(ModCategory.filter(ModCategory.Tab.ALL, null, null).isEmpty());
    }
}
