package org.chimeramc.client.core.mods.pvp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

/**
 * Pins the large PvP module catalogue (no mocks).
 *
 * <p>Two invariants matter. The catalogue must hold the requested 200+ modules with the requested
 * per-category counts (45/40/35/50/30), and the categories must be a strict partition — every module
 * maps to exactly the category it is filed under, so the list, the counts and the category tabs
 * cannot disagree. And the catalogue must stay honest: a module named after a server-decided outcome
 * carries a scope note, and a module this build cannot drive is marked unimplemented rather than
 * presented as working.
 */
public class PvpModuleCatalogTest {

    @Test
    public void theCatalogueHoldsTwoHundredOrMoreModules() {
        assertTrue("catalogue must be 200+", PvpModuleCatalog.count() >= 200);
    }

    @Test
    public void theCategoriesHaveTheRequestedCounts() {
        assertEquals(45, PvpModuleCatalog.byCategory(PvpCategory.COMBAT).size());
        assertEquals(40, PvpModuleCatalog.byCategory(PvpCategory.MOVEMENT).size());
        assertEquals(35, PvpModuleCatalog.byCategory(PvpCategory.VISUAL).size());
        assertEquals(50, PvpModuleCatalog.byCategory(PvpCategory.UTILITY).size());
        assertEquals(30, PvpModuleCatalog.byCategory(PvpCategory.WORLD).size());
    }

    @Test
    public void everyModuleIdIsUnique() {
        Set<String> seen = new HashSet<>();
        for (String id : PvpModuleCatalog.moduleIds()) {
            assertTrue("duplicate module id: " + id, seen.add(id));
        }
    }

    @Test
    public void theCategoriesPartitionTheCatalogue() {
        // Every module must map (by its id prefix) to exactly the category it is filed under.
        assertTrue(PvpCategory.isExhaustiveAndDisjoint(PvpModuleCatalog.moduleIds()));
        for (PvpModuleCatalog.Module module : PvpModuleCatalog.modules()) {
            assertEquals(module.id + " is filed under the wrong category",
                    module.category, PvpCategory.forModuleId(module.id));
        }
    }

    @Test
    public void everyModuleCarriesANonEmptyScopeNote() {
        for (PvpModuleCatalog.Module module : PvpModuleCatalog.modules()) {
            assertNotNull(module.id + " has no scope note", module.scopeNote);
            assertFalse(module.id + " has an empty scope note", module.scopeNote.trim().isEmpty());
            assertNotNull(module.name);
            assertFalse(module.description.trim().isEmpty());
        }
    }

    @Test
    public void implementedModulesAreASubsetAndNonEmpty() {
        List<PvpModuleCatalog.Module> implemented = PvpModuleCatalog.implemented();
        assertFalse("some modules must be implemented", implemented.isEmpty());
        assertTrue(implemented.size() < PvpModuleCatalog.count());
        for (PvpModuleCatalog.Module m : implemented) {
            assertTrue(m.implemented);
            assertNotNull(PvpModuleCatalog.find(m.id));
        }
    }

    @Test
    public void aReachExtendOrTimerModuleIsHonestlyMarkedUnimplemented() {
        // These describe a server-decided outcome a client cannot force, so they must not be
        // presented as working.
        assertFalse(PvpModuleCatalog.find("combat_reach_extend").implemented);
        assertFalse(PvpModuleCatalog.find("move_timer").implemented);
        assertFalse(PvpModuleCatalog.find("combat_anti_kb").implemented);
    }

    @Test
    public void unknownIdMapsToUtilityAndFindReturnsNull() {
        assertEquals(PvpCategory.UTILITY, PvpCategory.forModuleId("something_else"));
        assertEquals(PvpCategory.UTILITY, PvpCategory.forModuleId(null));
        org.junit.Assert.assertNull(PvpModuleCatalog.find("does_not_exist"));
    }

    @Test
    public void implementedEntriesLinkToAShippedModuleOrAreListedAsRoadmap() {
        // Every implemented entry either links to a shipped module (so its toggle drives the real
        // one) or is a genuine standalone module. A roadmap entry must not be implemented.
        for (PvpModuleCatalog.Module m : PvpModuleCatalog.modules()) {
            if (m.implemented && m.linksToExisting()) {
                assertNotNull("linked entry must name a module", m.existingModId);
                assertFalse(m.existingModId.isEmpty());
            }
            if (!m.implemented) {
                assertFalse("a roadmap entry must not link", m.linksToExisting());
            }
        }
    }

    @Test
    public void aCatalogueIdNeverCollidesWithAShippedModuleId() {
        // A catalogue id that equals a shipped ModIds value would make isPvpModule() classify the
        // shipped module as PvP and split the PvP run. Every catalogue entry that stands for a
        // shipped module must therefore use its own id and link to the shipped one.
        java.util.Set<String> catalogueIds = new java.util.HashSet<>(PvpModuleCatalog.moduleIds());
        for (String shipped : new String[]{"quick_drop", "zoom", "auto_sprint", "snaplook",
                "cps_display", "fps_display", "hitbox", "hit_registration", "hit_timing"}) {
            if (catalogueIds.contains(shipped)) {
                PvpModuleCatalog.Module m = PvpModuleCatalog.find(shipped);
                org.junit.Assert.fail("catalogue id collides with shipped module id: " + shipped
                        + " (use a distinct id and link to it instead)");
            }
        }
    }
}
