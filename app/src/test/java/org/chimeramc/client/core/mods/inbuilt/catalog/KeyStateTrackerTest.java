package org.chimeramc.client.core.mods.inbuilt.catalog;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The keystrokes hold tracker.
 *
 * <p>Pure, so the hold/release rules are pinned without a device. The one that matters is the
 * reset: a key that stays "held" after focus is lost would light up forever in the readout and
 * never clear.
 */
public class KeyStateTrackerTest {

    @Test
    public void aKeyIsHeldBetweenDownAndUp() {
        KeyStateTracker tracker = new KeyStateTracker();
        tracker.onKey(29, true);
        assertTrue(tracker.isHeld(29));
        tracker.onKey(29, false);
        assertFalse(tracker.isHeld(29));
    }

    @Test
    public void anUnknownKeyIsNotHeld() {
        KeyStateTracker tracker = new KeyStateTracker();
        assertFalse(tracker.isHeld(99));
    }

    @Test
    public void resetReleasesEverything() {
        KeyStateTracker tracker = new KeyStateTracker();
        tracker.onKey(29, true);
        tracker.onKey(47, true);
        tracker.reset();
        assertFalse(tracker.isHeld(29));
        assertFalse(tracker.isHeld(47));
    }

    @Test
    public void theLineShowsHeldKeysBracketed() {
        KeyStateTracker tracker = new KeyStateTracker();
        tracker.onKey(51, true); // W
        String line = tracker.line(new int[]{51, 29}, new String[]{"W", "A"});
        assertTrue(line.contains("[W]"));
        assertTrue(line.contains(" A "));
    }
}
