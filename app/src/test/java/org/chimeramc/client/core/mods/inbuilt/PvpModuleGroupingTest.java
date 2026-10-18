package org.chimeramc.client.core.mods.inbuilt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.core.mods.inbuilt.model.ModIds;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The Mod Menu draws one section header per contiguous run of a group id and lists each section
 * alphabetically, so these pin both: the PvP modules must stay adjacent, and every section must be
 * sorted by display name.
 */
public class PvpModuleGroupingTest {

    private static UnifiedMod mod(String id, String name) {
        return new UnifiedMod(id, name, name, "inbuilt", UnifiedMod.Source.INBUILT,
                false, null, false, "inbuilt", "Inbuilt Features", null, null);
    }

    private static UnifiedMod mod(String id) {
        return mod(id, id);
    }

    private static List<String> ids(List<UnifiedMod> mods) {
        List<String> out = new ArrayList<>();
        for (UnifiedMod mod : mods) out.add(mod.getId());
        return out;
    }

    @Test
    public void pvpModulesAreContiguousAtTheEnd() {
        List<UnifiedMod> ordered = InbuiltModuleProvider.groupPvpLast(Arrays.asList(
                mod(ModIds.AIM_SETTINGS, "Aim Settings"),
                mod(ModIds.QUICK_DROP, "Quick Drop"),
                mod(ModIds.CPS_DISPLAY, "CPS Display"),
                mod(ModIds.ZOOM, "Zoom"),
                mod(ModIds.SNAPLOOK, "Snaplook")));

        assertEquals(Arrays.asList(
                ModIds.QUICK_DROP, ModIds.ZOOM,
                ModIds.AIM_SETTINGS, ModIds.CPS_DISPLAY, ModIds.SNAPLOOK),
                ids(ordered));
    }

    @Test
    public void everyPvpModuleLandsInOneRun() {
        List<UnifiedMod> ordered = InbuiltModuleProvider.groupPvpLast(Arrays.asList(
                mod(ModIds.AIM_SETTINGS),
                mod(ModIds.QUICK_DROP),
                mod(ModIds.CPS_DISPLAY),
                mod(ModIds.SNAPLOOK),
                mod(ModIds.ZOOM)));

        int runs = 0;
        boolean inPvpRun = false;
        for (UnifiedMod mod : ordered) {
            boolean pvp = ModIds.isPvpModule(mod.getId());
            if (pvp && !inPvpRun) runs++;
            inPvpRun = pvp;
        }
        assertEquals("PvP modules must form a single section", 1, runs);
    }

    /** Each section is sorted by display name, not by declaration order. */
    @Test
    public void sectionsAreSortedAlphabetically() {
        List<UnifiedMod> ordered = InbuiltModuleProvider.groupPvpLast(Arrays.asList(
                mod(ModIds.QUICK_DROP, "Quick Drop"),
                mod(ModIds.ZOOM, "Zoom"),
                mod(ModIds.FPS_DISPLAY, "FPS Display")));
        assertEquals(Arrays.asList(ModIds.FPS_DISPLAY, ModIds.QUICK_DROP, ModIds.ZOOM),
                ids(ordered));
    }

    @Test
    public void sortingIsCaseInsensitive() {
        List<UnifiedMod> ordered = InbuiltModuleProvider.groupPvpLast(Arrays.asList(
                mod("a", "banana"),
                mod("b", "Apple"),
                mod("c", "cherry")));
        assertEquals(Arrays.asList("b", "a", "c"), ids(ordered));
    }

    @Test
    public void sortingTiesBreakOnIdForStability() {
        List<UnifiedMod> ordered = InbuiltModuleProvider.groupPvpLast(Arrays.asList(
                mod("b", "Same"),
                mod("a", "Same")));
        assertEquals(Arrays.asList("a", "b"), ids(ordered));
    }

    /** A group boundary must survive the sort, or its header would render twice. */
    @Test
    public void distinctGroupsAreSortedWithinTheirOwnRun() {
        List<UnifiedMod> ordered = InbuiltModuleProvider.groupPvpLast(Arrays.asList(
                mod(ModIds.QUICK_DROP, "Quick Drop"),
                mod(ModIds.ZOOM, "Zoom"),
                new UnifiedMod(ModIds.VOICE_CHAT, "Voice Chat", "Voice Chat", "inbuilt",
                        UnifiedMod.Source.INBUILT, false, null, false,
                        ModIds.GROUP_VOICE, "Voice", null, null),
                mod(ModIds.FPS_DISPLAY, "FPS Display")));

        assertEquals(Arrays.asList(ModIds.FPS_DISPLAY, ModIds.QUICK_DROP, ModIds.ZOOM,
                ModIds.VOICE_CHAT), ids(ordered));
    }

    @Test
    public void pvpClassificationIsStable() {
        assertTrue(ModIds.isPvpModule(ModIds.AIM_SETTINGS));
        assertTrue(ModIds.isPvpModule(ModIds.CPS_DISPLAY));
        assertTrue(ModIds.isPvpModule(ModIds.SNAPLOOK));
        assertTrue(ModIds.isPvpModule(ModIds.CRYSTAL_OPTIMIZER));
        assertTrue(ModIds.isPvpModule(ModIds.HIT_REGISTRATION));
        assertTrue(ModIds.isPvpModule(ModIds.HIT_TIMING));
        assertTrue(ModIds.isPvpModule(ModIds.HITBOX));
        assertFalse(ModIds.isPvpModule(ModIds.ZOOM));
        assertFalse(ModIds.isPvpModule(ModIds.MOD_MENU));
        assertFalse(ModIds.isPvpModule(null));
    }

    /** The combat modules must also fold into the single PvP run, sorted among themselves. */
    @Test
    public void theCombatModulesJoinTheSinglePvpRun() {
        List<UnifiedMod> ordered = InbuiltModuleProvider.groupPvpLast(Arrays.asList(
                mod(ModIds.HIT_TIMING, "Hit Timing"),
                mod(ModIds.QUICK_DROP, "Quick Drop"),
                mod(ModIds.HITBOX, "Hitboxes"),
                mod(ModIds.HIT_REGISTRATION, "Hit Registration"),
                mod(ModIds.ZOOM, "Zoom")));

        assertEquals(Arrays.asList(
                ModIds.QUICK_DROP, ModIds.ZOOM,
                ModIds.HIT_REGISTRATION, ModIds.HIT_TIMING, ModIds.HITBOX),
                ids(ordered));
    }

    @Test
    public void groupingEmptyAndAllPvpInputsAreSafe() {
        assertTrue(InbuiltModuleProvider.groupPvpLast(new ArrayList<>()).isEmpty());

        List<UnifiedMod> allPvp = InbuiltModuleProvider.groupPvpLast(Arrays.asList(
                mod(ModIds.AIM_SETTINGS, "Aim Settings"), mod(ModIds.CPS_DISPLAY, "CPS Display")));
        assertEquals(Arrays.asList(ModIds.AIM_SETTINGS, ModIds.CPS_DISPLAY), ids(allPvp));
    }
}
