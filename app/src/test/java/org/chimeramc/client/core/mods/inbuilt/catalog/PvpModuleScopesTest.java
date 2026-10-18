package org.chimeramc.client.core.mods.inbuilt.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.core.mods.pvp.PvpModuleCatalog;
import org.junit.Test;

/**
 * The catalogue scope classification.
 *
 * <p>This is the single predicate that decides whether a catalogue entry gets a real toggle or is
 * badged. It exists because the previous rule badged the whole reachable half of the 200+ list with
 * one shared "no game data" reason — refusing a clock for lack of a native entity feed. These pin
 * that a reachable route is reachable, and that a genuinely native/server module stays refused.
 */
public class PvpModuleScopesTest {

    @Test
    public void deviceInputModulesAreReachable() {
        assertTrue(PvpModuleScopes.isReachable("hud_clock"));
        assertTrue(PvpModuleScopes.isReachable("hud_keystrokes"));
        assertTrue(PvpModuleScopes.isReachable("util_auto_gg"));
        assertEquals(ModuleScope.DEVICE_INPUT, PvpModuleScopes.scopeOf("hud_clock"));
    }

    @Test
    public void optionsFileModulesAreReachable() {
        assertEquals(ModuleScope.GAME_OPTIONS, PvpModuleScopes.scopeOf("move_bob_off"));
        assertEquals(ModuleScope.GAME_OPTIONS, PvpModuleScopes.scopeOf("util_fullbright"));
        assertTrue(PvpModuleScopes.isReachable("world_particle_off"));
    }

    @Test
    public void localFeedModulesAreReachable() {
        assertEquals(ModuleScope.LOCAL_FEED, PvpModuleScopes.scopeOf("hud_coordinates"));
        assertTrue(PvpModuleScopes.isReachable("hud_compass"));
        assertTrue(PvpModuleScopes.isReachable("world_waypoints"));
    }

    @Test
    public void peerFeedModulesAreReachable() {
        assertEquals(ModuleScope.PEER_FEED, PvpModuleScopes.scopeOf("combat_reach_circle"));
    }

    @Test
    public void aNativeEntityModuleStaysRefused() {
        // These genuinely need a feed this build does not expose; refusing them is the honest
        // answer, and a real toggle here would be a module that silently does nothing.
        assertFalse(PvpModuleScopes.isReachable("world_mob_esp"));
        assertFalse(PvpModuleScopes.isReachable("combat_opponent_armor"));
        assertFalse(PvpModuleScopes.isReachable("hud_entity_count"));
        assertEquals(ModuleScope.ENGINE_HOOK, PvpModuleScopes.scopeOf("world_mob_esp"));
    }

    @Test
    public void aServerDecidedModuleIsClassifiedFromItsScopeNote() {
        // Reach extend / anti-knockback are server-arbitrated; the classification reads the
        // catalogue's own note rather than a second hand-maintained list.
        assertEquals(ModuleScope.SERVER_DECIDED, PvpModuleScopes.scopeOf("combat_reach_extend"));
        assertEquals(ModuleScope.SERVER_DECIDED, PvpModuleScopes.scopeOf("combat_anti_kb"));
    }

    @Test
    public void everyReachableIdExistsInTheCatalogue() {
        // A scope entry for an id that is not in the catalogue would be an implementation with no
        // menu row; it would never run and would hide a typo.
        for (String id : PvpModuleScopes.reachableIds()) {
            assertTrue("catalogue is missing " + id, PvpModuleCatalog.find(id) != null);
        }
    }

    @Test
    public void unreachableScopesAreExactlyTheTwoNativeOnes() {
        assertFalse(ModuleScope.ENGINE_HOOK.isReachable());
        assertFalse(ModuleScope.SERVER_DECIDED.isReachable());
        assertTrue(ModuleScope.DEVICE_INPUT.isReachable());
        assertTrue(ModuleScope.GAME_OPTIONS.isReachable());
        assertTrue(ModuleScope.LOCAL_FEED.isReachable());
        assertTrue(ModuleScope.PEER_FEED.isReachable());
    }
}
