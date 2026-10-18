package org.chimeramc.client.core.mods.inbuilt.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The input-driven cooldown timers.
 *
 * <p>Pure and clock-free (the caller passes the time), so the countdown, the re-arm on a second
 * use, and the honest empty state are all pinned without a device. The empty state matters: a
 * timer that was never started must read as "no data", not as "0.0s", or a readout claims a
 * cooldown that is not running.
 */
public class ActionTimersTest {

    @Test
    public void anUnstartedTimerIsInactiveAndHasNoText() {
        ActionTimers timer = new ActionTimers();
        assertFalse(timer.isActive(1000L));
        assertEquals(0, timer.remainingMs(1000L));
        assertNull(timer.remainingText(1000L));
    }

    @Test
    public void aTimerCountsDownAndExpires() {
        ActionTimers timer = new ActionTimers();
        timer.arm(1000L, 1000L);
        assertTrue(timer.isActive(1000L));
        assertEquals(500, timer.remainingMs(1500L));
        assertEquals("0.5s", timer.remainingText(1500L));
        assertTrue(timer.isActive(1999L));
        // At exactly the end it has expired.
        assertFalse(timer.isActive(2000L));
        assertNull(timer.remainingText(2000L));
    }

    @Test
    public void aSecondUseRearmsTheTimer() {
        ActionTimers timer = new ActionTimers();
        timer.arm(1000L, 1000L);
        timer.arm(1800L, 1000L);
        assertEquals(1000, timer.remainingMs(1800L));
        assertTrue(timer.isActive(2500L));
        assertFalse(timer.isActive(2800L));
    }

    @Test
    public void aNonPositiveDurationDisarms() {
        ActionTimers timer = new ActionTimers();
        timer.arm(1000L, 1000L);
        timer.arm(1500L, 0L);
        assertFalse(timer.isActive(1500L));
        assertNull(timer.remainingText(1500L));
    }
}
