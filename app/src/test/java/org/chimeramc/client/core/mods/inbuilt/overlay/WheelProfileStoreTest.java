package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * The radial wheel's profile-manager rules that do not need a Context: id derivation, id
 * de-duplication, and the repointing of sub-wheel references when a profile is deleted. No mocks.
 */
public class WheelProfileStoreTest {

    @Test
    public void aNameBecomesAStableId() {
        assertEquals("pvp", WheelProfileStore.slug("PvP"));
        assertEquals("survival", WheelProfileStore.slug("Survival!"));
        assertEquals("mywheel", WheelProfileStore.slug("My Wheel"));
    }

    @Test
    public void aNameWithNoAlphanumericsStillYieldsAnId() {
        // A profile with an empty id cannot be referenced by a sub-wheel, so the fallback must be
        // a real id rather than "".
        assertEquals("wheel", WheelProfileStore.slug("!!!"));
        assertEquals("wheel", WheelProfileStore.slug(null));
    }

    @Test
    public void aDuplicateNameGetsADistinctId() {
        List<WheelEntry.Profile> profiles = new ArrayList<>();
        profiles.add(new WheelEntry.Profile("pvp", "PvP"));
        // The base id is taken, so the second wheel must not collide with the first.
        assertNotEquals("pvp", WheelProfileStore.uniqueId(profiles, "pvp"));
        assertEquals("pvp2", WheelProfileStore.uniqueId(profiles, "pvp"));
    }

    @Test
    public void anUnusedIdIsKept() {
        List<WheelEntry.Profile> profiles = new ArrayList<>();
        profiles.add(new WheelEntry.Profile("pvp", "PvP"));
        assertEquals("survival", WheelProfileStore.uniqueId(profiles, "survival"));
    }

    @Test
    public void deletingAProfileRepointsEverySubWheelThatOpenedIt() {
        // A dangling OPEN_SUBWHEEL would draw a sector that does nothing, so every reference to the
        // deleted id must move to the fallback.
        List<WheelEntry.Profile> profiles = new ArrayList<>();
        WheelEntry.Profile root = new WheelEntry.Profile("main", "Main");
        root.entries.add(new WheelEntry(WheelEntry.Action.OPEN_SUBWHEEL, "pvp", "PvP"));
        profiles.add(root);
        WheelEntry.Profile other = new WheelEntry.Profile("other", "Other");
        other.entries.add(new WheelEntry(WheelEntry.Action.OPEN_SUBWHEEL, "pvp", "PvP"));
        profiles.add(other);

        WheelProfileStore.repointSubWheels(profiles, "pvp", "main");

        assertEquals("main", root.entries.get(0).target);
        assertEquals("main", other.entries.get(0).target);
    }

    @Test
    public void repointingLeavesUnrelatedEntriesAlone() {
        List<WheelEntry.Profile> profiles = new ArrayList<>();
        WheelEntry.Profile root = new WheelEntry.Profile("main", "Main");
        root.entries.add(new WheelEntry(WheelEntry.Action.OPEN_SUBWHEEL, "survival", "Survival"));
        root.entries.add(new WheelEntry(WheelEntry.Action.TOGGLE_MODULE, "pvp", "CPS"));
        profiles.add(root);

        WheelProfileStore.repointSubWheels(profiles, "pvp", "main");

        // The sub-wheel opened "survival", not the deleted id, so it stays; and a TOGGLE_MODULE
        // entry whose target happens to equal the id is a module id, not a profile reference.
        assertEquals("survival", root.entries.get(0).target);
        assertEquals("pvp", root.entries.get(1).target);
    }

    @Test
    public void repointingToleratesNulls() {
        WheelProfileStore.repointSubWheels(null, "a", "b");
        List<WheelEntry.Profile> profiles = new ArrayList<>();
        profiles.add(null);
        profiles.add(new WheelEntry.Profile("main", "Main"));
        WheelProfileStore.repointSubWheels(profiles, "a", "b");
        assertTrue(profiles.size() == 2);
    }
}
