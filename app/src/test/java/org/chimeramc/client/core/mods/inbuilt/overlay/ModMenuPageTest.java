package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/**
 * The VIP-menu parity invariant: the VIP menu is the normal menu plus its own extras. Every page the
 * normal menu offers must appear in the VIP list, in the same order, with the VIP-only pages last.
 * A page added to the normal menu without a VIP counterpart would silently be missing in-game.
 */
public class ModMenuPageTest {

    @Test
    public void vipCoversEverySharedPage() {
        assertTrue("every normal-menu page must exist in the VIP menu",
                ModMenuPage.vipCoversEverySharedPage());
    }

    @Test
    public void theSharedOrderMatchesTheNormalMenu() {
        List<ModMenuPage> shared = ModMenuPage.sharedPages();
        assertEquals(ModMenuPage.GENERAL, shared.get(0));
        assertEquals(ModMenuPage.VOICE, shared.get(1));
        assertEquals(ModMenuPage.PACKS, shared.get(2));
        assertEquals(ModMenuPage.COSMETICS, shared.get(3));
        assertEquals(ModMenuPage.HUD_EDITOR, shared.get(4));
        assertEquals(ModMenuPage.REPLAY, shared.get(5));
        assertEquals(ModMenuPage.SETTINGS, shared.get(6));
    }

    @Test
    public void theVipOnlyPagesComeLast() {
        List<ModMenuPage> vip = ModMenuPage.vipPages();
        int controller = vip.indexOf(ModMenuPage.CONTROLLER);
        int keyboard = vip.indexOf(ModMenuPage.KEYBOARD);
        // Every shared page must precede the first VIP-only page.
        int firstVipOnly = Math.min(controller, keyboard);
        for (ModMenuPage page : ModMenuPage.sharedPages()) {
            assertTrue(page + " must precede the VIP-only pages", vip.indexOf(page) < firstVipOnly);
        }
        assertEquals(ModMenuPage.CONTROLLER, vip.get(vip.size() - 2));
        assertEquals(ModMenuPage.KEYBOARD, vip.get(vip.size() - 1));
    }

    @Test
    public void onlyControllerAndKeyboardAreVipOnly() {
        assertEquals(2, ModMenuPage.vipOnlyPages().size());
        assertTrue(ModMenuPage.CONTROLLER.isVipOnly());
        assertTrue(ModMenuPage.KEYBOARD.isVipOnly());
        for (ModMenuPage page : ModMenuPage.sharedPages()) {
            assertFalse(page + " must not be VIP-only", page.isVipOnly());
        }
    }

    @Test
    public void theVipCycleWrapsInBothDirections() {
        assertEquals(ModMenuPage.VOICE, ModMenuPage.vipNext(ModMenuPage.GENERAL));
        assertEquals(ModMenuPage.GENERAL, ModMenuPage.vipNext(ModMenuPage.KEYBOARD));
        assertEquals(ModMenuPage.KEYBOARD, ModMenuPage.vipPrevious(ModMenuPage.GENERAL));
        assertEquals(ModMenuPage.SETTINGS, ModMenuPage.vipPrevious(ModMenuPage.CONTROLLER));
    }

    @Test
    public void everyPageCarriesATitleAndIcon() {
        for (ModMenuPage page : ModMenuPage.values()) {
            assertTrue("title for " + page, page.getTitleRes() != 0);
            assertTrue("icon for " + page, page.getIconRes() != 0);
        }
    }

    @Test
    public void theVipListHasNoDuplicates() {
        List<ModMenuPage> vip = ModMenuPage.vipPages();
        assertEquals(ModMenuPage.values().length, vip.size());
        assertEquals(vip.size(), new java.util.HashSet<>(vip).size());
    }
}
