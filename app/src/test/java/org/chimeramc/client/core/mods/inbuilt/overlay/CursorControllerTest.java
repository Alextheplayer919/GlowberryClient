package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The controller cursor's pure rules: integration with real delta time, radial dead zone, screen
 * clamping, magnetism, precision/fast multipliers. A wrong integration or a per-axis dead zone is
 * exactly the class of defect a screenshot cannot catch.
 */
public class CursorControllerTest {

    private static final float EPS = 1e-3f;

    private static CursorController cursor() {
        return new CursorController(1000f, 600f, 1f);
    }

    @Test
    public void startsCentredInTheViewport() {
        CursorController c = cursor();
        assertEquals(500f, c.x(), EPS);
        assertEquals(300f, c.y(), EPS);
    }

    @Test
    public void integratesAtFullDeflectionAtTheGivenSpeed() {
        CursorController c = cursor();
        // Full right deflection, 1 second, 900 dp/s at density 1 => 900px, clamped to 1000.
        c.update(1f, 0f, 1f, false, false, 900f);
        assertEquals(1000f, c.x(), EPS);
        assertEquals(300f, c.y(), EPS);
    }

    @Test
    public void aHalfSecondMovesHalfTheDistance() {
        CursorController c = cursor();
        c.update(1f, 0f, 0.5f, false, false, 900f);
        assertEquals(500f + 450f, c.x(), EPS);
    }

    @Test
    public void realDeltaTimeIsUsedNotAFixedStep() {
        // Two half-steps must equal one whole step (the integration is linear in dt).
        CursorController a = cursor();
        a.update(1f, 0f, 1f, false, false, 900f);
        CursorController b = cursor();
        b.update(1f, 0f, 0.5f, false, false, 900f);
        b.update(1f, 0f, 0.5f, false, false, 900f);
        assertEquals(a.x(), b.x(), EPS);
    }

    @Test
    public void precisionSlowsAndFastSpeedsUp() {
        // A wide viewport so nothing clamps and the distances compare directly.
        CursorController normal = new CursorController(5000f, 600f, 1f);
        normal.update(1f, 0f, 0.5f, false, false, 900f);
        float normalDist = normal.x() - 2500f;

        CursorController precise = new CursorController(5000f, 600f, 1f);
        precise.update(1f, 0f, 0.5f, true, false, 900f);
        float preciseDist = precise.x() - 2500f;

        CursorController fast = new CursorController(5000f, 600f, 1f);
        fast.update(1f, 0f, 0.5f, false, true, 900f);
        float fastDist = fast.x() - 2500f;

        assertTrue("precision must be slower", preciseDist < normalDist);
        assertTrue("fast must be faster", fastDist > normalDist);
        assertEquals(normalDist * CursorController.PRECISION_MULTIPLIER, preciseDist, 0.5f);
        assertEquals(normalDist * CursorController.FAST_MULTIPLIER, fastDist, 0.5f);
    }

    @Test
    public void theCursorNeverLeavesTheViewport() {
        CursorController c = cursor();
        for (int i = 0; i < 100; i++) {
            c.update(1f, 1f, 0.1f, false, true, 900f);
        }
        assertTrue(c.x() <= 1000f && c.x() >= 0f);
        assertTrue(c.y() <= 600f && c.y() >= 0f);
        assertEquals(1000f, c.x(), EPS);
        assertEquals(600f, c.y(), EPS);

        CursorController c2 = cursor();
        for (int i = 0; i < 100; i++) {
            c2.update(-1f, -1f, 0.1f, false, true, 900f);
        }
        assertEquals(0f, c2.x(), EPS);
        assertEquals(0f, c2.y(), EPS);
    }

    @Test
    public void magnetismPullsTowardANearbyTarget() {
        CursorController c = cursor();
        // Cursor at (500,300); a target 10px to the right, inside the 24px radius.
        c.setMagnetTarget(true, 510f, 300f);
        c.update(0f, 0f, 0.1f, false, false, 900f);
        assertTrue("the cursor must ease toward the target", c.x() > 500f);
        assertTrue("but not overshoot it", c.x() <= 510f + EPS);
    }

    @Test
    public void magnetismIsIgnoredBeyondTheRadius() {
        CursorController c = cursor();
        c.setMagnetTarget(true, 600f, 300f);  // 100px away, outside 24px
        c.update(0f, 0f, 0.1f, false, false, 900f);
        assertEquals(500f, c.x(), EPS);
    }

    @Test
    public void magnetismIsDisabledWhenNotArmed() {
        CursorController c = cursor();
        c.setMagnetTarget(false, 510f, 300f);
        c.update(0f, 0f, 0.1f, false, false, 900f);
        assertEquals(500f, c.x(), EPS);
    }

    @Test
    public void aZeroOrNegativeDeltaDoesNothing() {
        CursorController c = cursor();
        c.update(1f, 0f, 0f, false, false, 900f);
        assertEquals(500f, c.x(), EPS);
        c.update(1f, 0f, -0.5f, false, false, 900f);
        assertEquals(500f, c.x(), EPS);
    }

    // ---- shaping -----------------------------------------------------------

    @Test
    public void aStickInsideTheDeadZoneProducesNothing() {
        float[] out = new float[2];
        assertFalse(CursorController.shape(0.05f, 0.05f, 0.12f, 1.6f, out));
        assertEquals(0f, out[0], EPS);
        assertEquals(0f, out[1], EPS);
    }

    @Test
    public void theDeadZoneIsRadialNotPerAxis() {
        // A diagonal push just past the radius must produce output: the old per-axis test needed
        // threshold*sqrt(2) and rejected this.
        float[] out = new float[2];
        assertTrue(CursorController.shape(0.09f, 0.09f, 0.12f, 1.6f, out));
        assertTrue(out[0] > 0f && out[1] > 0f);
    }

    @Test
    public void shapingPreservesThePushDirection() {
        float[] out = new float[2];
        CursorController.shape(0.6f, 0.8f, 0.12f, 1.0f, out);
        // Direction must be unchanged: atan2(0.8, 0.6) == atan2(outY, outX).
        double expected = Math.atan2(0.8, 0.6);
        double actual = Math.atan2(out[1], out[0]);
        assertEquals(expected, actual, 1e-3);
    }

    @Test
    public void theCurveIsFinerNearCentre() {
        float[] linear = new float[2];
        float[] curved = new float[2];
        CursorController.shape(0.2f, 0f, 0.1f, 1.0f, linear);
        CursorController.shape(0.2f, 0f, 0.1f, 2.0f, curved);
        assertTrue("a >1 exponent must reduce small output", curved[0] < linear[0]);
    }

    @Test
    public void fullDeflectionReachesFullOutput() {
        float[] out = new float[2];
        assertTrue(CursorController.shape(1f, 0f, 0.12f, 1.6f, out));
        assertEquals(1f, out[0], 1e-3);
    }

    @Test
    public void aNullOutputIsRefused() {
        assertFalse(CursorController.shape(1f, 1f, 0.1f, 1.6f, null));
    }
}
