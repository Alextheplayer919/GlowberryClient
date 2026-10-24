package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the backdrop capture's rate limit (R3).
 *
 * <p>The wheel captures the game behind it and blurs that capture; refreshing it every frame is how
 * a mid-range GPU drops to 20fps, so the throttle is a correctness requirement, not an optimisation.
 * The schedule takes its clock as a parameter, so every case here is deterministic.
 */
public class BackdropRefreshScheduleTest {

    @Test
    public void theFirstCaptureIsAlwaysDue() {
        BackdropRefreshSchedule schedule = new BackdropRefreshSchedule();
        assertTrue(schedule.isDue(0L, 5));
        assertTrue(schedule.isDue(1_000_000L, 5));
    }

    @Test
    public void aSecondCaptureInsideTheIntervalIsNotDue() {
        BackdropRefreshSchedule schedule = new BackdropRefreshSchedule();
        schedule.markCaptured(1_000L);
        assertFalse(schedule.isDue(1_000L, 5));
        assertFalse(schedule.isDue(1_100L, 5));
        assertFalse(schedule.isDue(1_199L, 5));
    }

    @Test
    public void aCaptureAfterTheIntervalIsDue() {
        BackdropRefreshSchedule schedule = new BackdropRefreshSchedule();
        schedule.markCaptured(1_000L);
        assertTrue(schedule.isDue(1_200L, 5));
    }

    @Test
    public void theIntervalIsInverseToTheRate() {
        assertEquals(200L, BackdropRefreshSchedule.intervalMs(5));
        assertEquals(100L, BackdropRefreshSchedule.intervalMs(10));
    }

    @Test
    public void theIntervalNeverGoesFasterThanTheCap() {
        // Asking for 120fps must not produce an 8ms interval; the cap is the point of the class.
        assertEquals(RadialWheelMaterial.BLUR_FPS_MAX, 10);
        assertEquals(100L, BackdropRefreshSchedule.intervalMs(120));
        assertEquals(BackdropRefreshSchedule.MIN_INTERVAL_MS, BackdropRefreshSchedule.intervalMs(60));
    }

    @Test
    public void aZeroRateMeansNeverRefresh() {
        BackdropRefreshSchedule schedule = new BackdropRefreshSchedule();
        assertEquals(-1L, BackdropRefreshSchedule.intervalMs(0));
        assertFalse(schedule.isDue(10_000L, 0));
        schedule.markCaptured(1_000L);
        assertFalse(schedule.isDue(99_999L, 0));
    }

    @Test
    public void aBackwardsClockIsTreatedAsDueRatherThanFrozen() {
        // An uptime wrap must not freeze the backdrop for the rest of the session; one extra capture
        // is the cheaper failure.
        BackdropRefreshSchedule schedule = new BackdropRefreshSchedule();
        schedule.markCaptured(5_000L);
        assertTrue(schedule.isDue(10L, 5));
    }

    @Test
    public void resetMakesTheNextCaptureDue() {
        BackdropRefreshSchedule schedule = new BackdropRefreshSchedule();
        schedule.markCaptured(1_000L);
        assertFalse(schedule.isDue(1_050L, 5));
        schedule.reset();
        assertTrue(schedule.isDue(1_050L, 5));
        assertEquals(0, schedule.refreshes());
    }

    @Test
    public void theCaptureCountTracksTheRefreshes() {
        BackdropRefreshSchedule schedule = new BackdropRefreshSchedule();
        schedule.markCaptured(0L);
        schedule.markCaptured(200L);
        assertEquals(2, schedule.refreshes());
    }

    @Test
    public void aFiveSecondBurstTakesAtMostOneCapturePerInterval() {
        // The behavioural check: 1 second of frames at 60fps against a 5fps cap must yield 5
        // captures, not 60.
        BackdropRefreshSchedule schedule = new BackdropRefreshSchedule();
        int captures = 0;
        for (int frame = 0; frame < 60; frame++) {
            long now = frame * 16L;
            if (schedule.isDue(now, 5)) {
                schedule.markCaptured(now);
                captures++;
            }
        }
        assertTrue("expected the 5fps cap, got " + captures, captures <= 6);
        assertTrue("expected at least a few captures, got " + captures, captures >= 4);
    }
}
