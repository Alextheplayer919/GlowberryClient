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
        r.triangle(0, 0, z, 16, 0, z, 16, 16, z, 0f, 0f, 1f, 0f, 1f, 1f, atlas, 2, 1, alphaTest, blend);
        r.triangle(0, 0, z, 16, 16, z, 0, 16, z, 0f, 0f, 1f, 1f, 0f, 1f, atlas, 2, 1, alphaTest, blend);
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
        assertTrue("depth recorded", r.depthAt(2, 2) > 0);
        // z is floored to an int, so a depth of 5 records as 5.
        assertEquals(5, r.depthAt(2, 2));
        assertFalse(r.depthTest(2, 2, 50));
        assertTrue(r.depthTest(2, 2, 4));
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
        int[] atlas = new int[]{0x00000000};  // fully transparent
        fullTexturedQuad(r, 1, atlas, true, true);
        assertEquals("a discarded texel writes nothing", BG, r.pixels()[2 * 4 + 2]);
        assertEquals("and records no depth", 0, r.depthAt(2, 2));
    }

    @Test
    public void theBaseSkinPathIsForcedOpaque() {
        PreviewRaster r = raster(4, 4);
        int[] atlas = new int[]{0x00000000};  // transparent texel, but alphaTest=false
        fullTexturedQuad(r, 1, atlas, false, false);
        assertEquals("base layer ignores atlas alpha and writes opaque",
                0xFF000000, r.pixels()[2 * 4 + 2]);
    }

    @Test
    public void aTranslucentTexelBlendsWhenBlendIsOn() {
        PreviewRaster r = raster(4, 4);
        int[] atlas = new int[]{0x80FFFFFF};
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
