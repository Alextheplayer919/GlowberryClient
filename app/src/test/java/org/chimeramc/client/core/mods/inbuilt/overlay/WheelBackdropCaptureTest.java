package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the backdrop capture's shape rules (R3, R12).
 *
 * <p>The capture itself needs a device, but its size policy and its "no capture" contract are pure
 * and are the parts that decide whether the wheel is affordable: a full-resolution capture is the
 * difference between a soft sheet and a frame drop.
 */
public class WheelBackdropCaptureTest {

    @Test
    public void theCaptureIsDownscaledToAQuarterResolution() {
        assertEquals(4, WheelBackdropCapture.DOWNSCALE);
        // A 1920x1080 window captures at 480x270, which is 1/16 of the pixel count.
        assertEquals(480, WheelBackdropCapture.captureDimension(1920));
        assertEquals(270, WheelBackdropCapture.captureDimension(1080));
    }

    @Test
    public void theCaptureIsBoundedSoATabletCannotAllocateASlab() {
        assertEquals(512, WheelBackdropCapture.MAX_DIMENSION);
        // A 4K-wide window is clamped rather than allocating a 960px-wide slab.
        assertEquals(WheelBackdropCapture.MAX_DIMENSION,
                WheelBackdropCapture.captureDimension(3840));
    }

    @Test
    public void aZeroOrNegativeWindowSizeYieldsNoCapture() {
        assertEquals(0, WheelBackdropCapture.captureDimension(0));
        assertEquals(0, WheelBackdropCapture.captureDimension(-40));
        // A tiny window still yields at least one pixel rather than a zero-size bitmap.
        assertEquals(1, WheelBackdropCapture.captureDimension(3));
    }

    @Test
    public void aFreshCaptureHasNoBackdropAndHasNotFailed() {
        WheelBackdropCapture capture = new WheelBackdropCapture();
        assertFalse(capture.hasCapture());
        assertFalse(capture.hasFailed());
        assertEquals(0, capture.schedule().refreshes());
    }

    @Test
    public void theCaptureExposesTheSameScheduleTheRefreshLoopUses() {
        // The overlay's loop and the capture's own throttle must be one object, or the loop could
        // capture faster than the schedule allows.
        WheelBackdropCapture capture = new WheelBackdropCapture();
        assertTrue(capture.schedule().isDue(0L, 5));
        capture.schedule().markCaptured(0L);
        assertFalse(capture.schedule().isDue(10L, 5));
    }

    @Test
    public void releaseResetsTheScheduleSoAReopenCapturesImmediately() {
        WheelBackdropCapture capture = new WheelBackdropCapture();
        capture.schedule().markCaptured(1_000L);
        capture.release();
        assertTrue(capture.schedule().isDue(1_000L, 5));
        assertFalse(capture.hasCapture());
        assertFalse(capture.hasFailed());
    }

    @Test
    public void supportFollowsThePixelCopyApiLevel() {
        // PixelCopy is API 24+; the class reports the platform's own answer rather than a literal,
        // so a minSdk change cannot leave a stale claim behind.
        assertEquals(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O,
                WheelBackdropCapture.isSupported());
    }
}
