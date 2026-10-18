package org.chimeramc.client.core.mods.inbuilt.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.core.mods.inbuilt.model.ModAvailability;
import org.chimeramc.client.core.mods.pvp.PvpModuleCatalog;
import org.junit.Test;

/**
 * The badge reason is per-scope, not one shared string.
 *
 * <p>A module that is waiting for a feed this build cannot read and a module the server arbitrates
 * are two different things; a single "no game data" reason told the player the second was coming
 * eventually, which it is not. This pins that the catalogue's scope decides the reason, so the two
 * cannot be conflated again.
 */
public class CatalogScopeReasonTest {

    @Test
    public void aServerDecidedCatalogueEntryCarriesTheServerReason() {
        int checked = 0;
        for (PvpModuleCatalog.Module entry : PvpModuleCatalog.modules()) {
            if (entry.linksToExisting()) continue;
            if (PvpModuleScopes.scopeOf(entry.id) != ModuleScope.SERVER_DECIDED) continue;
            checked++;
            assertTrue("a server-decided entry must be badged: " + entry.id,
                    ModAvailability.isUnavailable(entry.id));
            assertEquals(entry.id + " must carry the server reason",
                    ModAvailability.REASON_SERVER_DECIDED,
                    ModAvailability.unavailableReason(entry.id));
        }
        assertTrue("expected the catalogue to declare server-decided entries", checked > 0);
    }

    @Test
    public void anEngineHookCatalogueEntryCarriesTheGameDataReason() {
        int checked = 0;
        for (PvpModuleCatalog.Module entry : PvpModuleCatalog.modules()) {
            if (entry.linksToExisting()) continue;
            if (PvpModuleScopes.scopeOf(entry.id) != ModuleScope.ENGINE_HOOK) continue;
            checked++;
            assertEquals(entry.id + " must carry the game-data reason",
                    ModAvailability.REASON_NO_GAME_DATA,
                    ModAvailability.unavailableReason(entry.id));
        }
        assertTrue("expected the catalogue to declare engine-hook entries", checked > 0);
    }

    @Test
    public void aReachableCatalogueEntryCarriesNoReason() {
        for (String id : PvpModuleScopes.reachableIds()) {
            assertFalse("a reachable entry must not be badged: " + id,
                    ModAvailability.isUnavailable(id));
            org.junit.Assert.assertNull(ModAvailability.unavailableReason(id));
        }
    }

    @Test
    public void theTwoReasonsAreDistinctIds() {
        assertNotNull(ModAvailability.REASON_NO_GAME_DATA);
        assertNotNull(ModAvailability.REASON_SERVER_DECIDED);
        assertFalse(ModAvailability.REASON_NO_GAME_DATA.equals(
                ModAvailability.REASON_SERVER_DECIDED));
    }
}
