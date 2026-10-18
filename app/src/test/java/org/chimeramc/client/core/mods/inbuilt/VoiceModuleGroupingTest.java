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
 * The Mod Menu emits one section header per contiguous run of a group id, so the voice section
 * must both classify correctly and survive the PvP reordering as a single run.
 */
public class VoiceModuleGroupingTest {

    private static UnifiedMod mod(String id, String name, String group) {
        return new UnifiedMod(id, name, name, "inbuilt", UnifiedMod.Source.INBUILT,
                false, null, false, group, group, null, null);
    }

    private static List<String> ids(List<UnifiedMod> mods) {
        List<String> out = new ArrayList<>();
        for (UnifiedMod mod : mods) out.add(mod.getId());
        return out;
    }

    @Test
    public void voiceClassificationIsStable() {
        assertTrue(ModIds.isVoiceModule(ModIds.VOICE_CHAT));
        assertFalse(ModIds.isVoiceModule(ModIds.HITBOX));
        assertFalse(ModIds.isVoiceModule(ModIds.ZOOM));
        assertFalse(ModIds.isVoiceModule(null));
        // Voice is not PvP; the two sections must not overlap.
        assertFalse(ModIds.isPvpModule(ModIds.VOICE_CHAT));
        assertFalse(ModIds.isVoiceModule(ModIds.CPS_DISPLAY));
    }

    @Test
    public void voiceStaysASingleRunAfterPvpReordering() {
        // Declaration order mirroring the provider: voice is added after the other non-PvP
        // inbuilt modules and before the combat modules, which the reorder then moves to the end.
        List<UnifiedMod> ordered = InbuiltModuleProvider.groupPvpLast(Arrays.asList(
                mod(ModIds.QUICK_DROP, "Quick Drop", "inbuilt"),
                mod(ModIds.ZOOM, "Zoom", "inbuilt"),
                mod(ModIds.VOICE_CHAT, "Voice Chat", ModIds.GROUP_VOICE),
                mod(ModIds.HITBOX, "Hitboxes", ModIds.GROUP_PVP)));

        int runs = 0;
        boolean inVoiceRun = false;
        for (UnifiedMod m : ordered) {
            boolean voice = ModIds.isVoiceModule(m.getId());
            if (voice && !inVoiceRun) runs++;
            inVoiceRun = voice;
        }
        assertEquals("Voice must form a single section", 1, runs);
        // Each contiguous run is sorted; voice is its own run, so the inbuilt pair keeps its
        // alphabetical order and voice stays between it and the PvP section.
        assertEquals(Arrays.asList(ModIds.QUICK_DROP, ModIds.ZOOM, ModIds.VOICE_CHAT, ModIds.HITBOX),
                ids(ordered));
    }
}
