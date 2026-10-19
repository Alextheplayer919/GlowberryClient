package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the sub-wheel navigation stack: open, back out, the depth bound and the cycle guard.
 */
public class WheelNavigationTest {

    @Test
    public void aFreshStackIsAtItsRoot() {
        WheelNavigation nav = new WheelNavigation("main");
        assertEquals("main", nav.currentId());
        assertEquals(1, nav.depth());
        assertTrue(nav.atRoot());
    }

    @Test
    public void pushingASubWheelMovesOffTheRoot() {
        WheelNavigation nav = new WheelNavigation("main");
        assertTrue(nav.push("pvp"));
        assertEquals("pvp", nav.currentId());
        assertEquals(2, nav.depth());
        assertFalse(nav.atRoot());
    }

    @Test
    public void poppingReturnsToTheRootAndThenRefuses() {
        WheelNavigation nav = new WheelNavigation("main");
        nav.push("pvp");
        assertTrue(nav.pop());
        assertEquals("main", nav.currentId());
        assertTrue(nav.atRoot());
        // At the root there is nothing to pop; the caller closes the wheel instead.
        assertFalse(nav.pop());
        assertEquals("main", nav.currentId());
    }

    @Test
    public void aBlankIdIsNeverPushed() {
        WheelNavigation nav = new WheelNavigation("main");
        assertFalse(nav.push(null));
        assertFalse(nav.push(""));
        assertFalse(nav.push("   "));
        assertEquals(1, nav.depth());
    }

    @Test
    public void pushingAnIdAlreadyOnTheStackIsRefusedSoACycleCannotGrowWithoutLimit() {
        WheelNavigation nav = new WheelNavigation("main");
        nav.push("pvp");
        // The built-in PvP wheel points back at "main"; re-entering it would loop.
        assertFalse(nav.push("main"));
        assertEquals(2, nav.depth());
    }

    @Test
    public void theDepthIsBounded() {
        WheelNavigation nav = new WheelNavigation("root");
        for (int i = 0; i < WheelNavigation.MAX_DEPTH + 5; i++) {
            nav.push("level-" + i);
        }
        assertEquals(WheelNavigation.MAX_DEPTH, nav.depth());
    }

    @Test
    public void resetClearsThePathAndSeedsANewRoot() {
        WheelNavigation nav = new WheelNavigation("main");
        nav.push("pvp");
        nav.reset("pvp");
        assertEquals("pvp", nav.currentId());
        assertEquals(1, nav.depth());
        // A blank reset leaves an empty stack rather than a blank entry.
        nav.reset(null);
        assertEquals(0, nav.depth());
        assertEquals("", nav.currentId());
    }
}
