package org.chimeramc.client.core.mods.inbuilt.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Pins the classification of the visual/HUD/utility modules added to the mod panel.
 *
 * <p>A module that needs a native frame or entity feed this build does not provide must be badged
 * and refused, not offered as if it worked. The ones that reach a result through an existing route
 * (Fullbright through the brightness setting, Crosshair through an overlay, List Players through the
 * proximity-voice peer feed) must stay usable. This test is the single place that pins which is
 * which, so the provider, the availability badge and the UI cannot disagree.
 */
public class NewModulesTest {

    private static final String[] NEW_VISUAL = {
            ModIds.FULLBRIGHT, ModIds.TIME_CHANGER, ModIds.LOW_FIRE, ModIds.BLOCK_OUTLINE,
            ModIds.MOTION_BLUR, ModIds.SATURATION, ModIds.CROSSHAIR, ModIds.ARMOR_VIEW_BENDING,
            ModIds.PROJECTILE_SCALER, ModIds.FREECAM, ModIds.TNT, ModIds.LIST_PLAYERS,
            ModIds.DEBUG_MENU, ModIds.THIRD_PERSON_NAMETAG,
    };

    @Test
    public void everyNewModuleIdIsUnique() {
        Set<String> ids = new HashSet<>();
        for (String id : NEW_VISUAL) {
            assertTrue("duplicate module id: " + id, ids.add(id));
            assertTrue("blank module id", id != null && !id.isEmpty());
        }
    }

    @Test
    public void theNewModuleIdsDoNotCollideWithTheExistingOnes() {
        String[] existing = {ModIds.ZOOM, ModIds.FULLBRIGHT, ModIds.CROSSHAIR, ModIds.AUTO_SPRINT,
                ModIds.FPS_DISPLAY, ModIds.HITBOX, ModIds.VOICE_CHAT};
        // FULLBRIGHT and CROSSHAIR are new ids; the rest of `existing` are pre-existing ids that must
        // not be reused as new ones. This asserts the new set does not shadow a live id.
        Set<String> live = new HashSet<>();
        for (String id : existing) {
            assertFalse("new module reuses an unrelated existing id: " + id,
                    Set.of(ModIds.TIME_CHANGER, ModIds.LOW_FIRE, ModIds.BLOCK_OUTLINE,
                            ModIds.MOTION_BLUR, ModIds.SATURATION, ModIds.ARMOR_VIEW_BENDING,
                            ModIds.PROJECTILE_SCALER, ModIds.FREECAM, ModIds.TNT,
                            ModIds.LIST_PLAYERS, ModIds.DEBUG_MENU, ModIds.THIRD_PERSON_NAMETAG)
                            .contains(id));
            live.add(id);
        }
        assertTrue(live.contains(ModIds.ZOOM));
    }

    @Test
    public void theRoutelessModulesRequireGameDataAndTheReachableOnesDoNot() {
        // These reach a result through a route this build already has.
        assertFalse(ModIds.requiresGameData(ModIds.FULLBRIGHT));
        assertFalse(ModIds.requiresGameData(ModIds.CROSSHAIR));
        assertFalse(ModIds.requiresGameData(ModIds.LIST_PLAYERS));

        // These need a native frame or entity feed that no mod can read in this build.
        for (String id : new String[]{ModIds.TIME_CHANGER, ModIds.LOW_FIRE, ModIds.BLOCK_OUTLINE,
                ModIds.MOTION_BLUR, ModIds.SATURATION, ModIds.ARMOR_VIEW_BENDING,
                ModIds.PROJECTILE_SCALER, ModIds.FREECAM, ModIds.TNT, ModIds.DEBUG_MENU,
                ModIds.THIRD_PERSON_NAMETAG}) {
            assertTrue("module should need game data: " + id, ModIds.requiresGameData(id));
        }
    }

    @Test
    public void theAvailabilityBadgeFollowsRequiresGameDataWithoutDrifting() {
        for (String id : NEW_VISUAL) {
            assertEquals("availability drifted for " + id,
                    ModIds.requiresGameData(id), ModAvailability.isUnavailable(id));
            assertEquals("interactivity drifted for " + id,
                    !ModIds.requiresGameData(id), ModAvailability.isInteractive(id));
        }
    }
}
