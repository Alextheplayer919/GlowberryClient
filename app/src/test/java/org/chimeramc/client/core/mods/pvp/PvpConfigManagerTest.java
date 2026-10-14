package org.chimeramc.client.core.mods.pvp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * Pins the central PvP config manager (no mocks — a real in-memory {@link PvpConfigManager.Store}).
 *
 * <p>The invariants: a module this build cannot drive cannot be enabled (so no switch toggles to no
 * effect), and a remote merge is local-first — an import never silently flips a module the user set.
 */
public class PvpConfigManagerTest {

    /** A real in-memory store, not a mock. */
    private static final class MemoryStore implements PvpConfigManager.Store {
        final Map<String, Boolean> enabled = new HashMap<>();
        final Map<String, String> values = new HashMap<>();

        @Override
        public boolean getEnabled(String moduleId, boolean fallback) {
            return enabled.getOrDefault(moduleId, fallback);
        }

        @Override
        public void putEnabled(String moduleId, boolean value) {
            enabled.put(moduleId, value);
        }

        @Override
        public String getValue(String moduleId, String key, String fallback) {
            return values.getOrDefault(moduleId + "/" + key, fallback);
        }

        @Override
        public void putValue(String moduleId, String key, String value) {
            values.put(moduleId + "/" + key, value);
        }
    }

    @Test
    public void anImplementedModuleCanBeEnabledAndPersists() {
        MemoryStore store = new MemoryStore();
        PvpConfigManager manager = new PvpConfigManager(store);
        assertTrue(manager.setEnabled("combat_hit_registration", true));
        assertTrue(manager.isEnabled("combat_hit_registration"));
        assertTrue(store.enabled.get("combat_hit_registration"));
    }

    @Test
    public void anUnimplementedModuleCannotBeEnabled() {
        MemoryStore store = new MemoryStore();
        PvpConfigManager manager = new PvpConfigManager(store);
        // combat_reach_extend is honestly marked unimplemented (the server validates reach).
        assertFalse(manager.setEnabled("combat_reach_extend", true));
        assertFalse(manager.isEnabled("combat_reach_extend"));
        assertFalse(store.enabled.containsKey("combat_reach_extend"));
    }

    @Test
    public void anUnknownModuleIsRefused() {
        PvpConfigManager manager = new PvpConfigManager(new MemoryStore());
        assertFalse(manager.setEnabled("no_such_module", true));
        assertFalse(manager.isEnabled("no_such_module"));
    }

    @Test
    public void optionValuesRoundTrip() {
        PvpConfigManager manager = new PvpConfigManager(new MemoryStore());
        manager.setValue("combat_hit_registration", "smoothing", "7");
        assertEquals("7", manager.getValue("combat_hit_registration", "smoothing", "0"));
        assertEquals("3", manager.getValue("combat_hit_registration", "missing", "3"));
    }

    @Test
    public void aRemoteMergeIsLocalFirst() {
        MemoryStore store = new MemoryStore();
        PvpConfigManager manager = new PvpConfigManager(store);
        // The user has explicitly set this one on.
        manager.setEnabled("combat_hit_registration", true);

        Map<String, Boolean> remote = new HashMap<>();
        remote.put("combat_hit_registration", false); // must NOT override the local choice
        remote.put("combat_cps_limit", true);         // adopted (no local value)
        remote.put("combat_reach_extend", true);      // refused (unimplemented)

        int adopted = manager.mergeRemote(remote, Set.of("combat_hit_registration"));
        assertEquals(1, adopted);
        assertTrue("a local choice must win over a remote one",
                manager.isEnabled("combat_hit_registration"));
        assertTrue(manager.isEnabled("combat_cps_limit"));
        assertFalse(manager.isEnabled("combat_reach_extend"));
    }

    @Test
    public void invalidateReloadsFromTheStore() {
        MemoryStore store = new MemoryStore();
        PvpConfigManager manager = new PvpConfigManager(store);
        manager.setEnabled("combat_hit_registration", true);
        store.enabled.put("combat_hit_registration", false); // an external change
        manager.invalidate();
        assertFalse(manager.isEnabled("combat_hit_registration"));
    }
}
