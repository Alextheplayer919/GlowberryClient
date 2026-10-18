package org.chimeramc.client.core.mods.inbuilt.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.core.mods.inbuilt.overlay.CatalogModuleHost;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * The "enabled but not working" guard for the input-driven timer readouts.
 *
 * <p>Several reachable readouts are countdowns that must be armed by a player action. A timer that
 * is wired into the menu but never armed by any action is invisible on a build machine — the readout
 * exists, the overlay draws, and it shows the empty state forever. The shield timer was exactly that
 * defect, so the arming rules are pinned here rather than left to review.
 */
public class CatalogTimerRulesTest {

    private static Set<String> ids(CatalogTimerRules.Arm[] arms) {
        Set<String> out = new HashSet<>();
        for (CatalogTimerRules.Arm arm : arms) out.add(arm.modId);
        return out;
    }

    @Test
    public void aSwingArmsTheCritCooldownAndIndicator() {
        Set<String> armed = ids(CatalogTimerRules.forAttack());
        assertTrue(armed.contains("combat_crit_timer"));
        assertTrue(armed.contains("combat_attack_cooldown"));
        assertTrue(armed.contains("combat_attack_indicator"));
    }

    @Test
    public void aUseArmsThePearlGappleAndShieldTimers() {
        Set<String> armed = ids(CatalogTimerRules.forUse());
        assertTrue("the shield timer must be armed by a use action", armed.contains("combat_shield_timer"));
        assertTrue(armed.contains("combat_pearl_timer"));
        assertTrue(armed.contains("combat_gapple_timer"));
    }

    @Test
    public void everyArmedTimerIsAReadoutTheHostCanShow() {
        // A timer armed for an id that is not a readout would run against nothing; a readout with no
        // arming action is the defect this class exists to prevent.
        for (CatalogTimerRules.Arm arm : CatalogTimerRules.forAttack()) {
            assertTrue(arm.modId + " must be a wired readout", CatalogModuleHost.isReadout(arm.modId));
        }
        for (CatalogTimerRules.Arm arm : CatalogTimerRules.forUse()) {
            assertTrue(arm.modId + " must be a wired readout", CatalogModuleHost.isReadout(arm.modId));
        }
    }

    @Test
    public void theTimersTheRuntimeShowsAreAllArmedSomewhere() {
        // The ids the runtime's textFor switch returns a timer for. Each must appear in an arming
        // rule, or the readout can never leave its empty state.
        String[] shown = {"combat_crit_timer", "combat_attack_cooldown", "combat_attack_indicator",
                "combat_pearl_timer", "combat_gapple_timer", "combat_shield_timer"};
        Set<String> armed = new HashSet<>();
        armed.addAll(ids(CatalogTimerRules.forAttack()));
        armed.addAll(ids(CatalogTimerRules.forUse()));
        for (String id : shown) {
            assertTrue("no action arms " + id, armed.contains(id));
        }
    }

    @Test
    public void durationsArePositive() {
        for (CatalogTimerRules.Arm arm : CatalogTimerRules.forAttack()) {
            assertTrue(arm.modId, arm.durationMs > 0);
        }
        for (CatalogTimerRules.Arm arm : CatalogTimerRules.forUse()) {
            assertTrue(arm.modId, arm.durationMs > 0);
        }
        assertTrue(CatalogTimerRules.HIT_MARKER_MS > 0);
        assertEquals(625L, CatalogTimerRules.SWING_MS);
    }
}
