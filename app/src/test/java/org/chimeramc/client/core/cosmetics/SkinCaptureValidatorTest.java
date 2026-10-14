package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins {@link SkinCaptureValidator}: every legal size, the exact-ratio downscale, the all-zero and
 * no-opaque-pixel rejection, and the arm-model parse.
 */
public class SkinCaptureValidatorTest {

    private static int[] opaque(int size) {
        int[] a = new int[size];
        java.util.Arrays.fill(a, 0xFFFFFFFF);
        return a;
    }

    @Test
    public void everyLegalSizeIsAccepted() {
        assertTrue(SkinCaptureValidator.isLegalSize(64, 32));
        assertTrue(SkinCaptureValidator.isLegalSize(64, 64));
        assertTrue(SkinCaptureValidator.isLegalSize(128, 128));
        assertTrue(SkinCaptureValidator.isLegalSize(256, 256));
        assertFalse(SkinCaptureValidator.isLegalSize(64, 128));
        assertFalse(SkinCaptureValidator.isLegalSize(48, 48));
        assertFalse(SkinCaptureValidator.isLegalSize(1024, 1024));
    }

    @Test
    public void anAllZeroBufferIsRejected() {
        int[] zero = new int[64 * 64];
        assertFalse(SkinCaptureValidator.hasContent(zero));
        assertNull(SkinCaptureValidator.validate(64, 64, zero, SkinCaptureValidator.ArmModel.WIDE));
    }

    @Test
    public void aBufferWithNoOpaquePixelIsRejected() {
        int[] transparent = new int[64 * 64];
        java.util.Arrays.fill(transparent, 0x00FFFFFF);
        assertTrue(SkinCaptureValidator.hasContent(transparent));
        assertFalse(SkinCaptureValidator.hasOpaquePixel(transparent));
        assertNull(SkinCaptureValidator.validate(64, 64, transparent,
                SkinCaptureValidator.ArmModel.WIDE));
    }

    @Test
    public void aGoodCaptureValidates() {
        SkinCaptureValidator.Validated v = SkinCaptureValidator.validate(
                128, 128, opaque(128 * 128), SkinCaptureValidator.ArmModel.SLIM);
        assertNotNull(v);
        assertEquals(128, v.width);
        assertEquals(SkinCaptureValidator.ArmModel.SLIM, v.armModel);
    }

    @Test
    public void aWrongLengthBufferIsRejected() {
        assertNull(SkinCaptureValidator.validate(64, 64, new int[10],
                SkinCaptureValidator.ArmModel.WIDE));
    }

    @Test
    public void anUnknownArmModelDefaultsToWide() {
        SkinCaptureValidator.Validated v = SkinCaptureValidator.validate(
                64, 64, opaque(64 * 64), null);
        assertNotNull(v);
        assertEquals(SkinCaptureValidator.ArmModel.WIDE, v.armModel);
    }

    @Test
    public void a64x64AtlasIsCopiedUnchanged() {
        int[] src = opaque(64 * 64);
        src[0] = 0xFF112233;
        int[] out = SkinCaptureValidator.normaliseToAtlas(64, 64, src);
        assertEquals(64 * 64, out.length);
        assertEquals(0xFF112233, out[0]);
    }

    @Test
    public void aLegacy64x32SkinKeepsItsTopHalf() {
        int[] src = opaque(64 * 32);
        src[5] = 0xFFAABBCC;
        int[] out = SkinCaptureValidator.normaliseToAtlas(64, 32, src);
        assertEquals(64 * 64, out.length);
        assertEquals(0xFFAABBCC, out[5]);
        // The bottom half is transparent (the legacy skin has no overlay there).
        assertEquals(0, out[40 * 64]);
    }

    @Test
    public void a128SkinIsDownscaledByTwo() {
        int[] src = new int[128 * 128];
        java.util.Arrays.fill(src, 0xFFFFFFFF);
        src[0] = 0xFF010203;
        int[] out = SkinCaptureValidator.normaliseToAtlas(128, 128, src);
        assertEquals(64 * 64, out.length);
        // Pixel (0,0) maps to source (0,0).
        assertEquals(0xFF010203, out[0]);
        // Pixel (1,0) maps to source (2,0), which is opaque white.
        assertEquals(0xFFFFFFFF, out[1]);
    }

    @Test
    public void a256SkinIsDownscaledByFour() {
        int[] src = new int[256 * 256];
        java.util.Arrays.fill(src, 0xFFFFFFFF);
        src[4] = 0xFF445566;
        int[] out = SkinCaptureValidator.normaliseToAtlas(256, 256, src);
        assertEquals(64 * 64, out.length);
        assertEquals(0xFF445566, out[1]);
    }

    @Test
    public void theArmModelParseIsCaseInsensitive() {
        assertEquals(SkinCaptureValidator.ArmModel.SLIM,
                SkinCaptureValidator.parseArmModel("slim"));
        assertEquals(SkinCaptureValidator.ArmModel.SLIM,
                SkinCaptureValidator.parseArmModel("SLIM"));
        assertEquals(SkinCaptureValidator.ArmModel.WIDE,
                SkinCaptureValidator.parseArmModel("wide"));
        assertEquals(SkinCaptureValidator.ArmModel.WIDE,
                SkinCaptureValidator.parseArmModel(null));
    }
}
