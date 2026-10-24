package org.chimeramc.client.core.mods.pvp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.core.mods.inbuilt.model.ModIds;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pins the PvP positioning decision (B9).
 *
 * <p>The decision is "reposition, not split and not remove": the section stays, and what it may be
 * advertised as is bounded to the client-side modules. These tests are what stop a catalogue entry
 * whose outcome the server decides from being promoted into the advertisable set by a later edit.
 */
public class PvpSectionPolicyTest {

    @Test
    public void theAdvertisableSetIsExactlyTheShippedModules() {
        List<String> advertisable = PvpSectionPolicy.advertisableIds();
        assertEquals(PvpSectionPolicy.shippedModules().size(), advertisable.size());
        for (PvpSectionPolicy.ShippedModule module : PvpSectionPolicy.shippedModules()) {
            assertTrue(module.id() + " is advertisable", advertisable.contains(module.id()));
        }
    }

    @Test
    public void everyAdvertisableIdIsAnActualShippedModule() {
        // A policy entry naming a module the launcher does not ship would be a claim with nothing
        // behind it, which is exactly the mistake this class exists to prevent.
        Set<String> shipped = new HashSet<>();
        for (String id : ModIds.pvpModuleIds()) shipped.add(id);
        for (String id : PvpSectionPolicy.advertisableIds()) {
            assertTrue(id + " is a shipped PvP module", shipped.contains(id));
        }
    }

    @Test
    public void everyShippedPvpModuleIsAccountedForByThePolicy() {
        // The reverse direction: a module added to the section must get a boundary sentence, or it
        // could be marketed without one.
        for (String id : ModIds.pvpModuleIds()) {
            assertTrue(id + " has a policy entry",
                    PvpSectionPolicy.mayBeAdvertisedAsWorking(id));
            assertNotNull(id + " has a boundary", PvpSectionPolicy.boundaryFor(id));
        }
    }

    @Test
    public void aServerArbitratedCatalogueEntryIsNeverAdvertisable() {
        assertFalse(PvpSectionPolicy.mayBeAdvertisedAsWorking("combat_reach_extend"));
        assertFalse(PvpSectionPolicy.mayBeAdvertisedAsWorking("combat_anti_kb"));
        assertTrue(PvpSectionPolicy.isServerArbitrated("combat_reach_extend"));
        assertTrue(PvpSectionPolicy.isServerArbitrated("combat_anti_kb"));
    }

    @Test
    public void anEngineHookCatalogueEntryIsNotAdvertisableEither() {
        // Not server-arbitrated, but still not something this build can do.
        assertFalse(PvpSectionPolicy.isServerArbitrated("world_mob_esp"));
        assertFalse(PvpSectionPolicy.mayBeAdvertisedAsWorking("world_mob_esp"));
    }

    @Test
    public void nullAndUnknownIdsAreRefusedRatherThanAssumedFine() {
        assertFalse(PvpSectionPolicy.mayBeAdvertisedAsWorking(null));
        assertFalse(PvpSectionPolicy.mayBeAdvertisedAsWorking(""));
        assertFalse(PvpSectionPolicy.mayBeAdvertisedAsWorking("no_such_module"));
        assertEquals(null, PvpSectionPolicy.boundaryFor(null));
        assertEquals(null, PvpSectionPolicy.boundaryFor("no_such_module"));
    }

    @Test
    public void everyBoundarySentenceNamesWhatTheModuleDoesNotDo() {
        // A boundary that only describes the feature is marketing copy, not a boundary. Each one
        // must contain a limiting clause.
        for (PvpSectionPolicy.ShippedModule module : PvpSectionPolicy.shippedModules()) {
            String note = module.boundary().toLowerCase(java.util.Locale.ROOT);
            boolean limits = note.contains("cannot") || note.contains("does not")
                    || note.contains("never") || note.contains("server")
                    || note.contains("no ") || note.contains("only");
            assertTrue(module.id() + " states its limit: " + module.boundary(), limits);
        }
    }

    @Test
    public void thePositioningStatementStatesTheBoundary() {
        String statement = PvpSectionPolicy.positioning().toLowerCase(java.util.Locale.ROOT);
        assertTrue(statement.contains("client-side"));
        assertTrue("the statement must refuse the server-side claims",
                statement.contains("server"));
        assertFalse(statement.isEmpty());
    }
}
