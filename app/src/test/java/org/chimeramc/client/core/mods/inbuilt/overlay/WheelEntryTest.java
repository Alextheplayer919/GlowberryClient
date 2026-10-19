package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import org.chimeramc.client.core.mods.inbuilt.overlay.WheelEntry.Action;
import org.chimeramc.client.core.mods.inbuilt.overlay.WheelEntry.Profile;

/**
 * Pins the wheel model's invariants: a label is never blank, a profile never exceeds the sector cap,
 * and the store's sanitiser repairs the shapes that would otherwise draw unhittable sectors or an
 * ambiguous sub-wheel reference.
 */
public class WheelEntryTest {

    @Test
    public void aBlankLabelIsNormalisedSoASectorIsNeverEmpty() {
        assertEquals("—", WheelEntry.normalizeLabel(null));
        assertEquals("—", WheelEntry.normalizeLabel(""));
        assertEquals("—", WheelEntry.normalizeLabel("   "));
        assertEquals("Zoom", WheelEntry.normalizeLabel("  Zoom  "));
        assertEquals("—", new WheelEntry(Action.TOGGLE_MODULE, "zoom", null).label);
    }

    @Test
    public void anEntryFallsBackToTheAccentWhenItHasNoColourOfItsOwn() {
        WheelEntry e = new WheelEntry(Action.TOGGLE_MODULE, "zoom", "Zoom");
        int accent = 0xFF0F5BE4;
        assertEquals(accent, e.effectiveColor(accent));
        e.color = 0xFFAA22;
        assertEquals(0xFFAA22, e.effectiveColor(accent));
    }

    @Test
    public void aSubWheelEntryIsRecognisedAsOne() {
        assertTrue(new WheelEntry(Action.OPEN_SUBWHEEL, "pvp", "PvP").isSubWheel());
        assertFalse(new WheelEntry(Action.OPEN_MODS, "", "Mods").isSubWheel());
    }

    @Test
    public void aProfileIsCappedAtTheMaximumSectorCount() {
        Profile p = new Profile("main", "Main");
        for (int i = 0; i < WheelLayout.MAX_SECTORS + 5; i++) {
            p.entries.add(new WheelEntry(Action.TOGGLE_MODULE, "m" + i, "M" + i));
        }
        assertEquals(WheelLayout.MAX_SECTORS, p.safeEntries().size());
        // The original list is not mutated by reading it.
        assertEquals(WheelLayout.MAX_SECTORS + 5, p.entries.size());
    }

    @Test
    public void aProfileWithNoEntriesReadsAsEmpty() {
        assertTrue(new Profile("empty", "Empty").isEmpty());
        Profile p = new Profile("one", "One");
        p.entries = null;
        assertTrue(p.isEmpty());
    }

    @Test
    public void sanitizeDropsProfilesWithoutAnIdAndRepairsTheRest() {
        List<Profile> in = new ArrayList<>();
        in.add(null);
        in.add(new Profile("", "No id"));
        Profile good = new Profile("main", null);
        good.entries.add(new WheelEntry(Action.OPEN_MODS, "", null));
        in.add(good);
        List<Profile> out = WheelProfileStore.sanitize(in);
        assertEquals(1, out.size());
        assertEquals("main", out.get(0).id);
        // A blank name is replaced by the id so the manager has something to show.
        assertEquals("main", out.get(0).name);
        assertEquals("—", out.get(0).entries.get(0).label);
    }

    @Test
    public void sanitizeKeepsTheFirstProfileForADuplicateId() {
        List<Profile> in = new ArrayList<>();
        in.add(new Profile("dup", "First"));
        in.add(new Profile("dup", "Second"));
        List<Profile> out = WheelProfileStore.sanitize(in);
        assertEquals(1, out.size());
        assertEquals("First", out.get(0).name);
    }

    @Test
    public void findByIdReturnsNullForAMissingReference() {
        List<Profile> profiles = WheelProfileStore.defaultProfiles();
        assertNotNull(WheelProfileStore.findById(profiles, WheelProfileStore.DEFAULT_ID));
        assertNull(WheelProfileStore.findById(profiles, "does-not-exist"));
        assertNull(WheelProfileStore.findById(null, "x"));
    }

    @Test
    public void theBuiltInDefaultIsUsableAndHasANestedWheel() {
        List<Profile> profiles = WheelProfileStore.defaultProfiles();
        assertFalse(profiles.isEmpty());
        Profile root = WheelProfileStore.findById(profiles, WheelProfileStore.DEFAULT_ID);
        assertNotNull(root);
        boolean hasSubWheel = false;
        for (WheelEntry e : root.safeEntries()) {
            if (e.isSubWheel()) hasSubWheel = true;
            // Every sub-wheel reference must resolve, or the entry opens nothing.
            if (e.isSubWheel()) assertNotNull(WheelProfileStore.findById(profiles, e.target));
        }
        assertTrue("the default wheel must demonstrate a nested sub-wheel", hasSubWheel);
    }
}
