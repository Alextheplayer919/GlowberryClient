package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the cape thumbnail renderer (no mocks).
 *
 * <p>The closet grid shows a real picture of each cape's back. The invariant that matters: the
 * thumbnail's colour at a cloth coordinate is the same {@link CapePatterns} weave the preview and
 * the in-game cape use, so a swatch can never show a different pattern than the cape a player gets.
 */
public class CapeThumbnailTest {

    @Test
    public void theThumbnailMatchesTheWeaveAtTheSameCoordinate() {
        CosmeticCatalog.Cape cape = new CosmeticCatalog.Cape(
                "t", "Test", 0xFF102030, 0xFF405060, 0xFFA0B0C0,
                CosmeticCatalog.CapePattern.VERTICAL_STRIPES, false, false);
        int w = 20;
        int h = 32;
        int[] out = new int[w * h];
        CapeThumbnail.render(cape, w, h, out, 0xFF000000);

        // Check a handful of pixels against the weave at their own normalised coordinate. The
        // thumbnail applies a mild shade, so the channel ordering must match the weave colour's
        // relative brightness rather than the exact value.
        for (int y : new int[] {1, h / 2, h - 2}) {
            for (int x : new int[] {0, w / 3, w - 1}) {
                float u = (x + 0.5f) / w;
                float v = (y + 0.5f) / h;
                int weave = CapePatterns.colorAt(cape.pattern, u, v, cape.color, cape.accentColor);
                int pixel = out[y * w + x];
                // Both are either the base or the accent, and the thumbnail is a scaled version, so
                // compare which of the two colours each is closer to.
                assertEquals(nearest(weave, cape.color, cape.accentColor),
                        nearest(pixel, cape.color, cape.accentColor));
            }
        }
    }

    private static int nearest(int color, int a, int b) {
        long da = dist(color, a);
        long db = dist(color, b);
        return da <= db ? 0 : 1;
    }

    private static long dist(int x, int y) {
        long dr = ((x >>> 16) & 0xFF) - ((y >>> 16) & 0xFF);
        long dg = ((x >>> 8) & 0xFF) - ((y >>> 8) & 0xFF);
        long db = (x & 0xFF) - (y & 0xFF);
        return dr * dr + dg * dg + db * db;
    }

    @Test
    public void aNullCapePaintsTheEmptyColour() {
        int[] out = new int[8 * 8];
        CapeThumbnail.render(null, 8, 8, out, 0xFF123456);
        for (int p : out) assertEquals(0xFF123456, p);
    }

    @Test
    public void differentCapesProduceDifferentThumbnails() {
        CosmeticCatalog.Cape a = new CosmeticCatalog.Cape(
                "a", "A", 0xFF102030, 0xFF405060, 0xFFA0B0C0,
                CosmeticCatalog.CapePattern.SOLID, false, false);
        CosmeticCatalog.Cape b = new CosmeticCatalog.Cape(
                "b", "B", 0xFF902010, 0xFF604030, 0xFFC0A090,
                CosmeticCatalog.CapePattern.SOLID, false, false);
        int[] outA = new int[16 * 16];
        int[] outB = new int[16 * 16];
        CapeThumbnail.render(a, 16, 16, outA, 0);
        CapeThumbnail.render(b, 16, 16, outB, 0);
        assertNotEquals(outA[8 * 16 + 8], outB[8 * 16 + 8]);
    }

    @Test
    public void anUndersizedBufferIsIgnored() {
        int[] tooSmall = new int[4];
        // Must not throw or write out of bounds.
        CapeThumbnail.render(new CosmeticCatalog.Cape(
                        "a", "A", 0xFF102030, 0xFF405060, 0xFFA0B0C0,
                        CosmeticCatalog.CapePattern.SOLID, false, false),
                16, 16, tooSmall, 0);
        assertTrue(true);
    }
}
