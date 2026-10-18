package org.chimeramc.client.core.replay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the replay transport, the free camera and the cinematic camera path. These are the pure rules
 * behind the "record & replay" upgrade, so the behaviour the player feels (reverse, scrub, a smooth
 * orbit) is checked here rather than only on a device.
 */
public class ReplayTimelineTest {

    @Test
    public void reverseSpeedRunsThePositionBackwards() {
        ReplayTimeline timeline = new ReplayTimeline(10_000L);
        timeline.seekMs(5_000L);
        timeline.setSpeed(-1f);
        timeline.play();
        assertTrue(timeline.isReversed());
        timeline.advance(1_000L);
        assertEquals(4_000L, timeline.positionMs());
    }

    @Test
    public void aForwardTransportStopsAtTheEndAndReportsIt() {
        ReplayTimeline timeline = new ReplayTimeline(1_000L);
        timeline.seekMs(900L);
        timeline.setSpeed(1f);
        timeline.play();
        assertTrue("reaching the end must report a boundary stop", timeline.advance(500L));
        assertEquals(1_000L, timeline.positionMs());
        assertFalse(timeline.isPlaying());
    }

    @Test
    public void aReversedTransportStopsAtTheStart() {
        ReplayTimeline timeline = new ReplayTimeline(1_000L);
        timeline.seekMs(100L);
        timeline.setSpeed(-1f);
        timeline.play();
        assertTrue(timeline.advance(500L));
        assertEquals(0L, timeline.positionMs());
        assertFalse(timeline.isPlaying());
    }

    @Test
    public void playingAtTheEndDoesNotStartAForwardTransport() {
        ReplayTimeline timeline = new ReplayTimeline(1_000L);
        timeline.seekMs(1_000L);
        timeline.setSpeed(1f);
        timeline.play();
        assertFalse(timeline.isPlaying());
    }

    @Test
    public void seekFractionClampsAndIgnoresNonFinite() {
        ReplayTimeline timeline = new ReplayTimeline(2_000L);
        timeline.seekFraction(0.5f);
        assertEquals(1_000L, timeline.positionMs());
        timeline.seekFraction(4f);
        assertEquals(2_000L, timeline.positionMs());
        timeline.seekFraction(-3f);
        assertEquals(0L, timeline.positionMs());
        timeline.seekFraction(Float.NaN);
        assertEquals(0L, timeline.positionMs());
    }

    @Test
    public void theSpeedCycleIncludesReverseAndForward() {
        ReplayTimeline timeline = new ReplayTimeline(1_000L);
        boolean sawReverse = false;
        boolean sawForward = false;
        for (int i = 0; i < ReplayTimeline.SPEEDS.length; i++) {
            if (timeline.isReversed()) sawReverse = true;
            else sawForward = true;
            timeline.cycleSpeed();
        }
        assertTrue(sawReverse);
        assertTrue(sawForward);
    }

    @Test
    public void setSpeedPicksTheNearestSlot() {
        ReplayTimeline timeline = new ReplayTimeline(1_000L);
        timeline.setSpeed(-1.9f);
        assertEquals(-2.0f, timeline.speed(), 0.001f);
        timeline.setSpeed(0.6f);
        assertEquals(0.5f, timeline.speed(), 0.001f);
    }

    @Test
    public void aShrunkDurationClampsThePosition() {
        ReplayTimeline timeline = new ReplayTimeline(10_000L);
        timeline.seekMs(9_000L);
        timeline.setDurationMs(1_000L);
        assertEquals(1_000L, timeline.positionMs());
    }
}
