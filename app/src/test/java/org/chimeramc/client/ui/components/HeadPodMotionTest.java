package org.chimeramc.client.ui.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the head pod's motion rules: idle turn, hover pause, drag, momentum and the bob. Pure JVM.
 */
public class HeadPodMotionTest {

    private static final float DT = 1f / 60f;

    @Test
    public void idleTurnAdvancesAtTheDocumentedRate() {
        HeadPodMotion motion = new HeadPodMotion(0f);
        // One second of 60Hz frames.
        for (int i = 0; i < 60; i++) motion.tick(DT);
        assertEquals(HeadPodMotion.IDLE_DEGREES_PER_SECOND, motion.yaw(), 1.5f);
    }

    @Test
    public void hoverPausesTheIdleTurn() {
        HeadPodMotion motion = new HeadPodMotion(0f);
        motion.setHovered(true);
        for (int i = 0; i < 60; i++) motion.tick(DT);
        assertEquals(0f, motion.yaw(), 0.001f);
    }

    @Test
    public void aDragTurnsOneToOneWithThePointer() {
        HeadPodMotion motion = new HeadPodMotion(0f);
        motion.beginDrag();
        motion.drag(100f, DT); // 100px * 0.6 deg/px = 60 deg
        assertEquals(60f, motion.yaw(), 0.001f);
    }

    @Test
    public void aFlickCoastsThenSettles() {
        HeadPodMotion motion = new HeadPodMotion(0f);
        motion.beginDrag();
        // A fast swipe: 30px in one frame -> 18 deg / (1/60 s) = 1080 deg/s, clamped.
        motion.drag(30f, DT);
        motion.endDrag();
        assertTrue("a flick should be coasting", motion.isCoasting());
        float speedAfterRelease = motion.velocity();
        motion.tick(DT);
        assertTrue("coasting should keep turning", motion.yaw() > 0f);
        assertTrue("friction should slow the spin", Math.abs(motion.velocity()) < Math.abs(speedAfterRelease));
        // Run long enough for friction to stop it.
        for (int i = 0; i < 60 * 10; i++) motion.tick(DT);
        assertEquals(0f, motion.velocity(), 0.001f);
        assertFalse(motion.isCoasting());
    }

    @Test
    public void aSlowReleaseDoesNotCoast() {
        HeadPodMotion motion = new HeadPodMotion(0f);
        motion.beginDrag();
        motion.drag(1f, DT); // 0.6 deg in a frame = 36 deg/s
        motion.endDrag();
        // 36 deg/s is above the minimum, so it does coast briefly, but must settle quickly.
        for (int i = 0; i < 60 * 5; i++) motion.tick(DT);
        assertEquals(0f, motion.velocity(), 0.001f);
    }

    @Test
    public void aReleaseWithNoMovementStops() {
        HeadPodMotion motion = new HeadPodMotion(0f);
        motion.beginDrag();
        motion.drag(0f, DT);
        motion.endDrag();
        assertFalse(motion.isCoasting());
        assertEquals(0f, motion.velocity(), 0.001f);
    }

    @Test
    public void aStallDoesNotFlingTheModel() {
        HeadPodMotion motion = new HeadPodMotion(10f);
        motion.tick(5f); // absurd frame time
        assertEquals(10f, motion.yaw(), 0.001f);
        motion.tick(-1f);
        assertEquals(10f, motion.yaw(), 0.001f);
    }

    @Test
    public void theBobStaysInsideItsAmplitudeAndHasAHold() {
        float max = 0f;
        for (long t = 0; t < HeadPodMotion.BOB_PERIOD_MS + HeadPodMotion.BOB_PAUSE_MS; t += 25) {
            max = Math.max(max, Math.abs(HeadPodMotion.bobPx(t)));
        }
        assertTrue("bob must not exceed its amplitude", max <= HeadPodMotion.BOB_AMPLITUDE_PX + 0.01f);
        // During the hold window the bob is exactly zero (the "blink" beat).
        long holdStart = HeadPodMotion.BOB_PERIOD_MS + 10;
        assertEquals(0f, HeadPodMotion.bobPx(holdStart), 0.0001f);
    }

    @Test
    public void theYawStaysInRange() {
        HeadPodMotion motion = new HeadPodMotion(350f);
        for (int i = 0; i < 60 * 5; i++) motion.tick(DT);
        assertTrue(motion.yaw() >= 0f && motion.yaw() < 360f);
    }
}
