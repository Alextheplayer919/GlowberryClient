package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;

/**
 * The one-menu registry rule (U4).
 *
 * <p>There used to be two page lists, one per mod-menu window, and the whole point of the merge is
 * that there is now exactly one. These tests pin that: every page has a title and an icon, the list
 * has no duplicates, the shoulders wrap in both directions, and the Supporter pages are ordinary
 * pages that merely carry the badge — not a separate list the menu could forget to build.
 */
public class ModMenuPageTest {

    @Test
    public void everyPageCarriesATitleAndAnIcon() {
        for (ModMenuPage page : ModMenuPage.values()) {
            assertTrue("title for " + page, page.getTitleRes() != 0);
            assertTrue("icon for " + page, page.getIconRes() != 0);
        }
    }

    @Test
    public void thePageListIsEveryEnumValueWithNoDuplicates() {
        List<ModMenuPage> pages = ModMenuPage.pages();
        assertEquals(ModMenuPage.values().length, pages.size());
        assertEquals(pages.size(), new HashSet<>(pages).size());
    }

    @Test
    public void theSupporterPagesAreOrdinaryPagesOfTheSameList() {
        List<ModMenuPage> pages = ModMenuPage.pages();
        List<ModMenuPage> supporters = ModMenuPage.supporterPages();
        assertFalse("the badge must mark something", supporters.isEmpty());
        for (ModMenuPage page : supporters) {
            assertTrue(page + " must be in the one page list", pages.contains(page));
            assertTrue(page + " must be flagged supporter", page.isSupporter());
        }
        // The badge is the only difference: a non-supporter page must not claim it.
        for (ModMenuPage page : pages) {
            if (!supporters.contains(page)) {
                assertFalse(page + " must not carry the badge", page.isSupporter());
            }
        }
    }

    @Test
    public void theShouldersWrapInBothDirections() {
        List<ModMenuPage> pages = ModMenuPage.pages();
        ModMenuPage first = pages.get(0);
        ModMenuPage last = pages.get(pages.size() - 1);
        assertEquals(pages.get(1), ModMenuPage.next(first));
        assertEquals(first, ModMenuPage.next(last));
        assertEquals(last, ModMenuPage.previous(first));
        assertEquals(pages.get(pages.size() - 2), ModMenuPage.previous(last));
    }

    @Test
    public void theDefaultPageMatchesTheInputFamily() {
        assertEquals(ModMenuPage.CONTROLLER, ModMenuPage.defaultForInput(true));
        assertEquals(ModMenuPage.KEYBOARD, ModMenuPage.defaultForInput(false));
    }

    @Test
    public void theSupporterPagesAreTheInputPages() {
        // The two pages that used to be premium-only are exactly the two input surfaces, so the
        // badge and the input routing cannot drift apart.
        List<ModMenuPage> supporters = ModMenuPage.supporterPages();
        assertTrue(supporters.contains(ModMenuPage.CONTROLLER));
        assertTrue(supporters.contains(ModMenuPage.KEYBOARD));
        assertEquals(2, supporters.size());
    }
}
