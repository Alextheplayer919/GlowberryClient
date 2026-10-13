package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the software rasteriser's four rules: per-pixel depth, nearest sampling, alpha cutout and
 * deferred blending. These are the properties the {@code Canvas} preview could not have, so they are
 * the whole reason this class exists.
 */
public class PreviewRasterTest {

    private static final int BG = 0xFF000000;

    private static PreviewRaster raster(int w, int h) {
        PreviewRaster r = new PreviewRaster();
        r.prepare(w, h, BG);
        return r;
    }

    /** A tiny 2x1 atlas: left texel red opaque, right texel green opaque. */
    private static int[] redGreenAtlas() {
        return new int[]{0xFFFF0000, 0xFF00FF00};
    }

    /** Draws a full 16x16 square as two triangles at a single depth, textured by a 2x1 atlas. */
    private static void fullTexturedQuad(PreviewRaster r, float z, int[] atlas,
                                         boolean alphaTest, boolean blend) {
        // v0=(0,0) uv(0,0)  v1=(16,0) uv(1,0)  v2=(16,16) uv(1,1)  v3=(0,16) uv(0,1)
        r.triangle(0, 0, z, 16, 0, z, 16, 16, z, 0f, 0f, 1f, 0f, 1f, 1f, atlas, 2, 1, alphaTest, blend, 1f);
        r.triangle(0, 0, z, 16, 16, z, 0, 16, z, 0f, 0f, 1f, 1f, 0f, 1f, atlas, 2, 1, alphaTest, blend, 1f);
    }

    @Test
    public void aNearerSurfaceWinsRegardlessOfDrawOrder() {
        PreviewRaster r = raster(16, 16);
        r.triangleFlat(0, 0, 50, 16, 0, 50, 16, 16, 50, 0xFF0000FF, false);
        r.triangleFlat(0, 0, 50, 16, 16, 50, 0, 16, 50, 0xFF0000FF, false);
        r.triangleFlat(0, 0, 5, 16, 0, 5, 16, 16, 5, 0xFFFF0000, false);
        r.triangleFlat(0, 0, 5, 16, 16, 5, 0, 16, 5, 0xFFFF0000, false);
        assertEquals("nearer (red) should win", 0xFFFF0000, r.pixels()[8 * 16 + 8]);

        PreviewRaster r2 = raster(16, 16);
        r2.triangleFlat(0, 0, 5, 16, 0, 5, 16, 16, 5, 0xFFFF0000, false);
        r2.triangleFlat(0, 0, 5, 16, 16, 5, 0, 16, 5, 0xFFFF0000, false);
        r2.triangleFlat(0, 0, 50, 16, 0, 50, 16, 16, 50, 0xFF0000FF, false);
        r2.triangleFlat(0, 0, 50, 16, 16, 50, 0, 16, 50, 0xFF0000FF, false);
        assertEquals(0xFFFF0000, r2.pixels()[8 * 16 + 8]);
    }

    @Test
    public void theDepthBufferHoldsTheNearestValue() {
        PreviewRaster r = raster(16, 16);
        r.triangleFlat(0, 0, 5, 16, 0, 5, 4, 4, 5, 0xFFFFFFFF, false);
        assertTrue("depth recorded", r.depthAt(2, 2) < Float.POSITIVE_INFINITY);
        // The buffer is float now, so a fractional depth is stored exactly rather than floored.
        assertEquals(5f, r.depthAt(2, 2), 1e-3f);
        assertFalse(r.depthTest(2, 2, 50));
        assertTrue(r.depthTest(2, 2, 4));
    }

    @Test
    public void aFractionalDepthIsNotFloored() {
        // The overlay layer is nudged by 0.5 model pixels over its base face. With the old int
        // (floored) buffer both landed in the same bucket about half the time and the overlay showed
        // in stripes; a float buffer keeps them distinct.
        PreviewRaster r = raster(16, 16);
        r.triangleFlat(0, 0, 46.0f, 16, 0, 46.0f, 16, 16, 46.0f, 0xFF0000FF, false);
        r.triangleFlat(0, 0, 46.0f, 16, 16, 46.0f, 0, 16, 46.0f, 0xFF0000FF, false);
        assertEquals(46.0f, r.depthAt(8, 8), 1e-3f);
        // A surface 0.5 nearer (the overlay bias) must win.
        assertTrue(r.depthTest(8, 8, 45.5f));
        // A surface 0.5 farther must not.
        assertFalse(r.depthTest(8, 8, 46.5f));
    }

    @Test
    public void anOverlayBiasAlwaysWinsOverItsBaseFace() {
        // For 1000 random fractional base depths, a face 0.5 nearer must always pass the depth test.
        java.util.Random rnd = new java.util.Random(1234);
        for (int i = 0; i < 1000; i++) {
            float base = 46f + rnd.nextFloat() * 18f;
            PreviewRaster r = raster(4, 4);
            r.triangleFlat(0, 0, base, 4, 0, base, 4, 4, base, 0xFF0000FF, false);
            r.triangleFlat(0, 0, base, 4, 4, base, 0, 4, base, 0xFF0000FF, false);
            assertTrue("overlay bias must win at base=" + base,
                    r.depthTest(2, 2, base - 0.5f));
        }
    }

    @Test
    public void aSolidQuadIsFullyCovered() {
        // A quad split into two triangles must cover its whole area with no wedge missing. The old
        // (0,1,2)+(0,2,3) split left a triangular notch on the right of every face.
        int w = 64, h = 64;
        PreviewRaster r = raster(w, h);
        // A 16x16 solid quad in the middle, drawn as the two correct triangles.
        float x0 = 24, y0 = 24, x1 = 40, y1 = 40;
        int[] atlas = new int[]{0xFFFF0000};
        r.triangle(x0, y0, 1, x1, y0, 1, x1, y1, 1, 0f, 0f, 1f, 0f, 1f, 1f,
                atlas, 1, 1, false, false, 1f);
        r.triangle(x0, y0, 1, x1, y1, 1, x0, y1, 1, 0f, 0f, 1f, 1f, 0f, 1f,
                atlas, 1, 1, false, false, 1f);
        int painted = 0;
        for (int y = (int) y0; y < (int) y1; y++) {
            for (int x = (int) x0; x < (int) x1; x++) {
                if (r.pixels()[y * w + x] == 0xFFFF0000) painted++;
            }
        }
        assertEquals("every interior pixel of a solid quad must be painted",
                (int) (x1 - x0) * (int) (y1 - y0), painted);
    }

    @Test
    public void noPixelOnTheSharedDiagonalIsMissing() {
        // The two triangles share the TL-BR diagonal; a fill-rule gap would drop pixels along it.
        int w = 64, h = 64;
        PreviewRaster r = raster(w, h);
        int[] atlas = new int[]{0xFFFF0000};
        r.triangle(0, 0, 1, 32, 0, 1, 32, 32, 1, 0f, 0f, 1f, 0f, 1f, 1f,
                atlas, 1, 1, false, false, 1f);
        r.triangle(0, 0, 1, 32, 32, 1, 0, 32, 1, 0f, 0f, 1f, 1f, 0f, 1f,
                atlas, 1, 1, false, false, 1f);
        // Walk the diagonal band; every pixel within a pixel of it must be painted.
        for (int i = 0; i < 32; i++) {
            for (int d = -1; d <= 1; d++) {
                int x = i + d;
                if (x < 0 || x >= 32) continue;
                assertEquals("diagonal pixel (" + x + "," + i + ") missing",
                        0xFFFF0000, r.pixels()[i * w + x]);
            }
        }
    }

    @Test
    public void aTooSmallAtlasIsRefusedRatherThanCrashing() {
        PreviewRaster r = raster(16, 16);
        int[] tooSmall = new int[4];  // declares 8x8 but only 4 pixels
        r.triangle(0, 0, 1, 16, 0, 1, 16, 16, 1, 0f, 0f, 1f, 0f, 1f, 1f,
                tooSmall, 8, 8, true, true, 1f);
        assertEquals("a too-small atlas must draw nothing", BG, r.pixels()[8 * 16 + 8]);
    }

    @Test
    public void randomAtlasSizesAndUvsNeverThrow() {
        java.util.Random rnd = new java.util.Random(99);
        for (int i = 0; i < 2000; i++) {
            int tw = 1 + rnd.nextInt(64);
            int th = 1 + rnd.nextInt(64);
            int[] atlas = new int[tw * th];
            java.util.Arrays.fill(atlas, 0xFF00FF00);
            PreviewRaster r = raster(32, 32);
            float u0 = rnd.nextFloat() * 2f - 0.5f;
            float v0 = rnd.nextFloat() * 2f - 0.5f;
            float u1 = rnd.nextFloat() * 2f - 0.5f;
            float v1 = rnd.nextFloat() * 2f - 0.5f;
            r.triangle(0, 0, 1, 32, 0, 1, 16, 32, 1,
                    u0, v0, u1, v0, u1, v1, atlas, tw, th, true, true, 1f);
        }
    }

    @Test
    public void samplingIsNearestNeighbourAndMirrorsToTheEdges() {
        PreviewRaster r = raster(16, 8);
        fullTexturedQuad(r, 1, redGreenAtlas(), false, false);
        // u<0.5 (left half) samples the red texel; u>=0.5 samples green.
        assertEquals(0xFFFF0000, r.pixels()[4 * 16 + 4]);   // left
        assertEquals(0xFF00FF00, r.pixels()[4 * 16 + 12]);  // right
    }

    @Test
    public void aTransparentTexelIsDiscardedByCutout() {
        PreviewRaster r = raster(4, 4);
        int[] atlas = new int[]{0x00000000, 0x00000000};  // fully transparent (2x1, matches texW/H)
        fullTexturedQuad(r, 1, atlas, true, true);
        assertEquals("a discarded texel writes nothing", BG, r.pixels()[2 * 4 + 2]);
        assertEquals("and records no depth", Float.POSITIVE_INFINITY, r.depthAt(2, 2), 0f);
    }

    @Test
    public void theBaseSkinPathIsForcedOpaque() {
        PreviewRaster r = raster(4, 4);
        int[] atlas = new int[]{0x00000000, 0x00000000};  // transparent, but alphaTest=false
        fullTexturedQuad(r, 1, atlas, false, false);
        assertEquals("base layer ignores atlas alpha and writes opaque",
                0xFF000000, r.pixels()[2 * 4 + 2]);
    }

    @Test
    public void aTranslucentTexelBlendsWhenBlendIsOn() {
        PreviewRaster r = raster(4, 4);
        int[] atlas = new int[]{0x80FFFFFF, 0x80FFFFFF};
        fullTexturedQuad(r, 1, atlas, true, true);
        int mid = r.pixels()[2 * 4 + 2];
        int grey = mid & 0xFF;
        assertTrue("expected a mid grey, got " + Integer.toHexString(mid), grey > 60 && grey < 200);
    }

    @Test
    public void blendOverIsSourceOverForUnPremultipliedArgb() {
        assertEquals(0xFFFF0000, PreviewRaster.blendOver(0xFF00FF00, 0xFFFF0000));  // opaque src wins
        assertEquals(0xFF00FF00, PreviewRaster.blendOver(0xFF00FF00, 0x00000000));  // clear src keeps dst
        // Half red over half-blue alpha: red channel from src, blue from dst.
        int blended = PreviewRaster.blendOver(0xFF0000FF, 0x80FF0000);
        assertTrue((blended >>> 16 & 0xFF) > 100);
        assertTrue((blended & 0xFF) > 40);
    }

    @Test
    public void aDegenerateTriangleDrawsNothing() {
        PreviewRaster r = raster(4, 4);
        r.triangleFlat(0, 0, 1, 0, 0, 1, 0, 0, 1, 0xFFFF0000, false);  // zero area
        assertEquals(BG, r.pixels()[0]);
    }
}
