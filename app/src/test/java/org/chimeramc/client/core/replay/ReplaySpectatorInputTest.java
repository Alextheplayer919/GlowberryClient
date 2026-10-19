package org.chimeramc.client.core.replay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the spectator flight input: drag-to-look deltas, the fly stick schema and the boost.
 *
 * <p>The update describes a replay spectator who can "fly around" the reconstructed scene. The
 * camera model already covers the flight; this pins the raw-input conversion so a drag turns the
 * view, a stick deflection moves it and a held boost actually goes faster.
 */
public class ReplaySpectatorInputTest {

    @Test
    public void noDragLeavesTheViewAlone() {
        float[] look = ReplaySpectatorInput.dragToLook(0f, 0f);
        assertEquals(0f, look[0], 0.0001f);
        assertEquals(0f, look[1], 0.0001f);
    }

    @Test
    public void aDragTurnsAndPitchesByTheSensitivity() {
        float[] look = ReplaySpectatorInput.dragToLook(100f, -50f);
        assertEquals(100f * ReplaySpectatorInput.LOOK_DEGREES_PER_PX, look[0], 0.0001f);
        assertEquals(-50f * ReplaySpectatorInput.LOOK_DEGREES_PER_PX, look[1], 0.0001f);
    }

    @Test
    public void aNonFiniteDragIsIgnored() {
        float[] look = ReplaySpectatorInput.dragToLook(Float.NaN, 10f);
        assertEquals(0f, look[0], 0.0001f);
        assertEquals(0f, look[1], 0.0001f);
    }

    @Test
    public void forwardStickMovesAlongTheLookRow() {
        ReplayCamera camera = new ReplayCamera();
        camera.set(0f, 0f, 0f, 0f, 0f);
        camera.setSpeed(10f);
        ReplaySpectatorInput.fly(camera, 1f, 0f, 0f, false, 1_000L);
        assertEquals(0f, camera.x(), 0.001f);
        assertEquals(10f, camera.z(), 0.001f);
    }

    @Test
    public void aHeldBoostMultipliesTheDistanceButRestoresTheSpeed() {
        ReplayCamera camera = new ReplayCamera();
        camera.set(0f, 0f, 0f, 0f, 0f);
        camera.setSpeed(10f);
        ReplaySpectatorInput.fly(camera, 1f, 0f, 0f, true, 1_000L);
        assertEquals(10f * ReplaySpectatorInput.BOOST_MULTIPLIER, camera.z(), 0.001f);
        // The camera's stored speed must be preserved: boost is per-gesture, not a mode.
        assertEquals(10f, camera.speed(), 0.001f);
    }

    @Test
    public void theRailMovesVertically() {
        ReplayCamera camera = new ReplayCamera();
        camera.set(0f, 5f, 0f, 0f, 0f);
        camera.setSpeed(4f);
        ReplaySpectatorInput.fly(camera, 0f, 0f, 1f, false, 1_000L);
        assertEquals(9f, camera.y(), 0.001f);
    }

    @Test
    public void inputsBeyondTheUnitRangeAreClamped() {
        ReplayCamera camera = new ReplayCamera();
        camera.set(0f, 0f, 0f, 0f, 0f);
        camera.setSpeed(4f);
        ReplaySpectatorInput.fly(camera, 7f, 0f, 0f, false, 1_000L);
        assertEquals(4f, camera.z(), 0.001f);
    }

    @Test
    public void aNullCameraIsAHarmlessNoOp() {
        float speed = ReplaySpectatorInput.fly(null, 1f, 0f, 0f, true, 100L);
        assertTrue(speed >= 0f);
    }
}