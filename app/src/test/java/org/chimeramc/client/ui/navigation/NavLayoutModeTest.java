package org.chimeramc.client.ui.navigation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The adaptive-navigation rule (U5). A phone gets a bottom bar, a medium window an icon rail and a
 * large window an expanded rail, and the boundaries are exactly the Material breakpoints.
 */
public class NavLayoutModeTest {

    @Test
    public void aPhoneGetsABottomBar() {
        assertEquals(NavLayoutMode.BOTTOM_NAV, NavLayoutMode.forWidthDp(360));
        assertEquals(NavLayoutMode.BOTTOM_NAV, NavLayoutMode.forWidthDp(599));
    }

    @Test
    public void aMediumWindowGetsAnIconRail() {
        assertEquals(NavLayoutMode.NAV_RAIL, NavLayoutMode.forWidthDp(600));
        assertEquals(NavLayoutMode.NAV_RAIL, NavLayoutMode.forWidthDp(839));
    }

    @Test
    public void aLargeWindowGetsAnExpandedRail() {
        assertEquals(NavLayoutMode.EXPANDED_RAIL, NavLayoutMode.forWidthDp(840));
        assertEquals(NavLayoutMode.EXPANDED_RAIL, NavLayoutMode.forWidthDp(1280));
    }

    @Test
    public void aBadWidthFallsBackToTheSmallestLayout() {
        // Before layout the width can read as zero; the bottom bar is the layout that needs the
        // least room, so it is the safe default rather than a rail that would be clipped.
        assertEquals(NavLayoutMode.BOTTOM_NAV, NavLayoutMode.forWidthDp(0));
        assertEquals(NavLayoutMode.BOTTOM_NAV, NavLayoutMode.forWidthDp(-1));
    }

    @Test
    public void onlyTheBottomBarIsNotARail() {
        assertFalse(NavLayoutMode.BOTTOM_NAV.isRail());
        assertTrue(NavLayoutMode.NAV_RAIL.isRail());
        assertTrue(NavLayoutMode.EXPANDED_RAIL.isRail());
    }

    @Test
    public void onlyTheExpandedRailShowsEveryLabel() {
        assertFalse(NavLayoutMode.BOTTOM_NAV.showsAllLabels());
        assertFalse(NavLayoutMode.NAV_RAIL.showsAllLabels());
        assertTrue(NavLayoutMode.EXPANDED_RAIL.showsAllLabels());
    }
}
