package org.chimeramc.client.core.mods.inbuilt.catalog;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.core.mods.inbuilt.overlay.CatalogModuleHost;
import org.chimeramc.client.core.mods.pvp.PvpModuleCatalog;
import org.junit.Test;

/**
 * The "enabled but does nothing" guard for the reachable catalogue.
 *
 * <p>{@link PvpModuleScopes} decides which catalogue entries get a real toggle. That classification
 * is only honest if every id it calls reachable is also wired into one of {@link CatalogModuleHost}'s
 * four shapes — a readout, an action, an options write or a sprint helper. A reachable id with no
 * shape flips its toggle and draws nothing, which is the exact defect the classification exists to
 * remove, so this pins the two together rather than trusting review to keep them in step.
 */
public class CatalogModuleWiringTest {

    @Test
    public void everyReachableCatalogueEntryIsWiredIntoTheHost() {
        int reachable = 0;
        for (PvpModuleCatalog.Module entry : PvpModuleCatalog.modules()) {
            if (entry.linksToExisting()) continue;
            if (!PvpModuleScopes.isReachable(entry.id)) continue;
            reachable++;
            assertTrue("reachable entry has no host shape: " + entry.id,
                    CatalogModuleHost.handles(entry.id));
        }
        // The reachable half is not a token set: if a refactor empties the classification this test
        // would otherwise pass vacuously.
        assertTrue("expected a substantial reachable set, saw " + reachable, reachable >= 40);
    }

    @Test
    public void anUnreachableEntryIsNotClaimedAsHandled() {
        // An engine-hook entry must not be wired, or the toggle would appear to work while the
        // module can never draw anything.
        assertFalse(CatalogModuleHost.handles("world_mob_esp"));
        assertFalse(CatalogModuleHost.handles("combat_opponent_armor"));
    }

    @Test
    public void everyShippedCatalogueLinkStaysOutOfTheReachableMap() {
        // A link entry is a view of an already-shipped module; classifying it as reachable would
        // give the menu a second switch for one feature, which can then disagree with the first.
        for (PvpModuleCatalog.Module entry : PvpModuleCatalog.modules()) {
            if (!entry.linksToExisting()) continue;
            assertFalse("a link entry must not be classified reachable: " + entry.id,
                    PvpModuleScopes.isReachable(entry.id));
        }
    }
}
