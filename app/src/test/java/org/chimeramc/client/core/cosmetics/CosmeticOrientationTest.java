package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The Bedrock-cosmetic → player-space orientation conversion. Bedrock-authored cosmetic geometry
 * faces −Z while the player model faces +Z, so the conversion is the half-turn that stops a hat's
 * brim pointing backwards and a pet walking away from the camera.
 */
public class CosmeticOrientationTest {

    @Test
    public void theConversionIsAHalfTurnAboutY() {
        // x and z are negated (a 180-degree turn about Y); y is untouched.
        assertEquals(180f, CosmeticOrientation.YAW_TO_PLAYER_DEGREES, 0.0001f);
        float[] point = {3f, 7f, 5f};
        CosmeticOrientation.toPlayerSpace(point);
        assertEquals(-3f, point[0], 0.0001f);
        assertEquals(7f, point[1], 0.0001f);
        assertEquals(-5f, point[2], 0.0001f);
    }

    @Test
    public void theConversionIsItsOwnInverse() {
        float[] point = {-2f, 4f, -9f};
        float[] original = {point[0], point[1], point[2]};
        CosmeticOrientation.toPlayerSpace(point);
        CosmeticOrientation.toPlayerSpace(point);
        assertEquals(original[0], point[0], 0.0001f);
        assertEquals(original[1], point[1], 0.0001f);
        assertEquals(original[2], point[2], 0.0001f);
    }

    @Test
    public void aBedrockFrontNormalPointsAtThePlayerFront() {
        // Bedrock front is -Z; after conversion it must be +Z, which is SkinModel.FRONT.
        float[] normal = {0f, 0f, -1f};
        CosmeticOrientation.normalToPlayerSpace(normal);
        assertEquals(SkinModel.Face.FRONT.nz, normal[2], 0.0001f);
    }

    @Test
    public void aNullOrShortPointIsIgnored() {
        CosmeticOrientation.toPlayerSpace(null);
        float[] shortPoint = {1f};
        CosmeticOrientation.toPlayerSpace(shortPoint);
        assertEquals(1f, shortPoint[0], 0.0001f);
        assertNotNull(CosmeticOrientation.class);
    }

    @Test
    public void theAxisHelpersAgreeWithThePointConversion() {
        assertEquals(-3f, CosmeticOrientation.playerX(3f), 0.0001f);
        assertEquals(-5f, CosmeticOrientation.playerZ(5f), 0.0001f);
        assertTrue(true);
    }
}
