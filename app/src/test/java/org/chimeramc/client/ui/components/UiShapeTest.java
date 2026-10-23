package org.chimeramc.client.ui.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The shape scale and its morph (U6). Pure maths, no device.
 */
public class UiShapeTest {

    @Test
    public void theScaleAgreesWithTheTokenRadii() {
        assertEquals(UiTokens.RADIUS_SMALL, UiShape.SMALL);
        assertEquals(UiTokens.RADIUS_MEDIUM, UiShape.MEDIUM);
        assertEquals(UiTokens.RADIUS_LARGE, UiShape.LARGE);
        assertEquals(UiTokens.RADIUS_XLARGE, UiShape.EXTRA_LARGE);
        assertEquals(UiTokens.RADIUS_TOGGLE, UiShape.FULL);
    }

    @Test
    public void theScaleIsAscending() {
        assertTrue(UiShape.EXTRA_SMALL < UiShape.SMALL);
        assertTrue(UiShape.SMALL < UiShape.MEDIUM);
        assertTrue(UiShape.MEDIUM < UiShape.LARGE);
        assertTrue(UiShape.LARGE < UiShape.EXTRA_LARGE);
        assertTrue(UiShape.EXTRA_LARGE < UiShape.FULL);
    }

    @Test
    public void theRolesMapToTheScale() {
        assertEquals(UiShape.EXTRA_SMALL, UiShape.Role.INDICATOR.radiusDp);
        assertEquals(UiShape.SMALL, UiShape.Role.CHIP.radiusDp);
        assertEquals(UiShape.MEDIUM, UiShape.Role.FIELD.radiusDp);
        assertEquals(UiShape.LARGE, UiShape.Role.CARD.radiusDp);
        assertEquals(UiShape.EXTRA_LARGE, UiShape.Role.WINDOW.radiusDp);
        assertEquals(UiShape.FULL, UiShape.Role.PILL.radiusDp);
    }

    @Test
    public void morphInterpolatesAndClamps() {
        assertEquals(8f, UiShape.morph(UiShape.SMALL, UiShape.LARGE, 0f), 0.001f);
        assertEquals(16f, UiShape.morph(UiShape.SMALL, UiShape.LARGE, 1f), 0.001f);
        assertEquals(12f, UiShape.morph(UiShape.SMALL, UiShape.LARGE, 0.5f), 0.001f);
        // Out-of-range fractions clamp rather than overshooting the shape.
        assertEquals(8f, UiShape.morph(UiShape.SMALL, UiShape.LARGE, -1f), 0.001f);
        assertEquals(16f, UiShape.morph(UiShape.SMALL, UiShape.LARGE, 2f), 0.001f);
    }

    @Test
    public void morphToOrFromAPillUsesTheOtherEndsLength() {
        // FULL is a sentinel, not a length: the in-between must stay a real radius.
        assertEquals(16f, UiShape.morph(UiShape.FULL, UiShape.LARGE, 0f), 0.001f);
        assertEquals(16f, UiShape.morph(UiShape.FULL, UiShape.LARGE, 1f), 0.001f);
        // The pill end contributes no length, so the whole morph stays at the real end's radius:
        // the corner is already a stadium, and a size change is what reads as "became a pill".
        assertEquals(8f, UiShape.morph(UiShape.SMALL, UiShape.FULL, 0f), 0.001f);
        assertEquals(8f, UiShape.morph(UiShape.SMALL, UiShape.FULL, 0.5f), 0.001f);
        assertEquals(8f, UiShape.morph(UiShape.SMALL, UiShape.FULL, 1f), 0.001f);
        assertEquals(8f, UiShape.morph(UiShape.FULL, UiShape.SMALL, 0.5f), 0.001f);
    }

    @Test
    public void morphThroughRolesMatchesTheRawRadii() {
        assertEquals(UiShape.morph(UiShape.SMALL, UiShape.LARGE, 0.5f),
                UiShape.morph(UiShape.Role.CHIP, UiShape.Role.CARD, 0.5f), 0.001f);
    }

    @Test
    public void aRadiusNeverExceedsHalfTheShorterSide() {
        assertEquals(16f, UiShape.clampToBox(28f, 64f, 32f), 0.001f);
        assertEquals(28f, UiShape.clampToBox(28f, 200f, 200f), 0.001f);
        assertEquals(0f, UiShape.clampToBox(28f, 0f, 32f), 0.001f);
    }
}
