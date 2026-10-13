package org.chimeramc.client.core.cosmetics;

/**
 * A pure-Java software rasteriser for the cosmetics preview.
 *
 * <p>The preview used to draw through {@code Canvas.drawBitmapMesh}, which cannot write a per-pixel
 * depth value and always bilinear-filters its samples. That is the root of two visible defects: the
 * body and cosmetics had to be ordered by a painter's sort (so faces vanish at some angles), and the
 * transparent edge of every texture bled into the opaque texels (so the character read as a
 * see-through sheet). Neither can be fixed on top of {@code Canvas}; both need a real rasteriser.
 *
 * <p>This class is that rasteriser, and it is deliberately <b>Android-free</b>: it reads textures
 * from an {@code int[]} and writes into an {@code int[]} ARGB buffer, so every rule here is
 * unit-testable on the JVM with no device.
 *
 * <p>Rules it implements, matching the spec for the preview rewrite:
 * <ul>
 *   <li><b>Per-pixel depth for every quad.</b> A z-buffer where smaller depth is nearer, so a
 *       nearer surface always wins regardless of draw order (the painter's sort is gone).</li>
 *   <li><b>Nearest-neighbour sampling only.</b> A texel is chosen by truncation, never blended, so
 *       a transparent texel next to an opaque one cannot smear.</li>
 *   <li><b>Alpha cutout.</b> A texel below {@link SkinAlphaFilter#ALPHA_THRESHOLD} is discarded.</li>
 *   <li><b>Deferred blending.</b> Opaque texels write straight over the destination; a translucent
 *       texel is alpha-blended, so a glass pane or ghost still blends while a solid body does not.</li>
 * </ul>
 */
public final class PreviewRaster {

    /**
     * The depth value meaning "nothing recorded". The camera projects real depths as positive values
     * (its minimum depth is clamped above zero), so 0 is a safe sentinel for an empty pixel.
     */
    private static final int NO_DEPTH = 0;

    private int width;
    private int height;
    private int[] color;
    private int[] depth;

    /** Sizes the buffers to the frame and fills the colour buffer with {@code background}. */
    public void prepare(int frameWidth, int frameHeight, int background) {
        if (frameWidth <= 0 || frameHeight <= 0) {
            width = 0;
            height = 0;
            color = null;
            depth = null;
            return;
        }
        if (color == null || width != frameWidth || height != frameHeight) {
            width = frameWidth;
            height = frameHeight;
            color = new int[frameWidth * frameHeight];
            depth = new int[frameWidth * frameHeight];
        }
        java.util.Arrays.fill(color, background);
        java.util.Arrays.fill(depth, NO_DEPTH);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** The ARGB buffer. Owned by the rasteriser; callers must copy before the next frame. */
    public int[] pixels() {
        return color;
    }

    /**
     * The depth currently recorded at a pixel (0 when nothing is there). Exposed for tests and for a
     * caller that needs to composite by hand.
     */
    public int depthAt(int x, int y) {
        if (depth == null || x < 0 || y < 0 || x >= width || y >= height) return NO_DEPTH;
        return depth[y * width + x];
    }

    /** Whether a surface at {@code z} is visible at the pixel (nearer than what is recorded). */
    public boolean depthTest(int x, int y, float z) {
        if (depth == null || x < 0 || y < 0 || x >= width || y >= height) return false;
        int stored = depth[y * width + x];
        int zi = Math.max(1, (int) Math.floor(z));
        return stored == NO_DEPTH || zi < stored;
    }

    /** Records a depth at a pixel (used after a successful {@link #depthTest}). */
    public void writeDepth(int x, int y, float z) {
        if (depth == null) return;
        int zi = Math.max(1, (int) Math.floor(z));
        depth[y * width + x] = zi;
    }

    /**
     * Rasterises one textured triangle into the buffer.
     *
     * @param x0,y0,z0 … x2,y2,z2 the three vertices in screen space, with camera depth (smaller =
     *                             nearer)
     * @param u0,v0 … u2,v2        the texture coordinates, normalised 0..1
     * @param argb                 the ARGB atlas
     * @param texW,texH            the atlas dimensions
     * @param alphaTest            when true a texel below the alpha threshold is discarded (the
     *                             overlay/cosmetic path); when false the texel is written whole (the
     *                             base-skin path, forced opaque)
     * @param blend                when true a translucent texel is alpha-blended (deferred
     *                             blending); when false every surviving texel writes opaque
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public void triangle(float x0, float y0, float z0,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float u0, float v0, float u1, float v1, float u2, float v2,
                         int[] argb, int texW, int texH,
                         boolean alphaTest, boolean blend) {
        if (color == null || argb == null || texW <= 0 || texH <= 0) return;

        // Bounding box.
        int minX = (int) Math.floor(Math.min(x0, Math.min(x1, x2)));
        int maxX = (int) Math.ceil(Math.max(x0, Math.max(x1, x2)));
        int minY = (int) Math.floor(Math.min(y0, Math.min(y1, y2)));
        int maxY = (int) Math.ceil(Math.max(y0, Math.max(y1, y2)));
        minX = Math.max(0, minX);
        minY = Math.max(0, minY);
        maxX = Math.min(width - 1, maxX);
        maxY = Math.min(height - 1, maxY);
        if (maxX < minX || maxY < minY) return;

        float area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
        if (area == 0f) return;
        float invArea = 1f / area;

        for (int py = minY; py <= maxY; py++) {
            float fy = py + 0.5f;
            int row = py * width;
            for (int px = minX; px <= maxX; px++) {
                float fx = px + 0.5f;
                // Barycentric coordinates.
                float w0 = ((x1 - fx) * (y2 - fy) - (x2 - fx) * (y1 - fy)) * invArea;
                float w1 = ((x2 - fx) * (y0 - fy) - (x0 - fx) * (y2 - fy)) * invArea;
                float w2 = 1f - w0 - w1;
                if (w0 < 0f || w1 < 0f || w2 < 0f) continue;

                float z = w0 * z0 + w1 * z1 + w2 * z2;
                int stored = depth[row + px];
                int zi = Math.max(1, (int) Math.floor(z));
                if (stored != NO_DEPTH && zi >= stored) continue;

                // Nearest-neighbour sample: truncate, and mirror any face that ran past the edge.
                float fu = w0 * u0 + w1 * u1 + w2 * u2;
                float fv = w0 * v0 + w1 * v1 + w2 * v2;
                int tx = (int) (fu * texW);
                int ty = (int) (fv * texH);
                if (tx < 0) tx = 0; else if (tx >= texW) tx = texW - 1;
                if (ty < 0) ty = 0; else if (ty >= texH) ty = texH - 1;
                int texel = argb[ty * texW + tx];

                int out;
                if (alphaTest) {
                    int a = (texel >>> 24) & 0xFF;
                    if (a >= SkinAlphaFilter.ALPHA_THRESHOLD) {
                        out = blend ? blendOver(color[row + px], texel) : texel;
                    } else {
                        continue;  // discarded: no depth written, so a nearer layer can still win
                    }
                } else {
                    // Base-skin path: forced opaque, ignore the atlas alpha entirely.
                    out = (texel & 0x00FFFFFF) | 0xFF000000;
                }
                color[row + px] = out;
                depth[row + px] = zi;
            }
        }
    }

    /**
     * Rasterises a flat-coloured, untextured triangle (used for the debug fills and the shadow).
     * Still depth-tested, so it composites correctly against the textured passes.
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public void triangleFlat(float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             int argb, boolean blend) {
        if (color == null) return;
        int minX = Math.max(0, (int) Math.floor(Math.min(x0, Math.min(x1, x2))));
        int maxX = Math.min(width - 1, (int) Math.ceil(Math.max(x0, Math.max(x1, x2))));
        int minY = Math.max(0, (int) Math.floor(Math.min(y0, Math.min(y1, y2))));
        int maxY = Math.min(height - 1, (int) Math.ceil(Math.max(y0, Math.max(y1, y2))));
        if (maxX < minX || maxY < minY) return;

        float area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
        if (area == 0f) return;
        float invArea = 1f / area;

        for (int py = minY; py <= maxY; py++) {
            float fy = py + 0.5f;
            int row = py * width;
            for (int px = minX; px <= maxX; px++) {
                float fx = px + 0.5f;
                float w0 = ((x1 - fx) * (y2 - fy) - (x2 - fx) * (y1 - fy)) * invArea;
                float w1 = ((x2 - fx) * (y0 - fy) - (x0 - fx) * (y2 - fy)) * invArea;
                float w2 = 1f - w0 - w1;
                if (w0 < 0f || w1 < 0f || w2 < 0f) continue;
                float z = w0 * z0 + w1 * z1 + w2 * z2;
                int stored = depth[row + px];
                int zi = Math.max(1, (int) Math.floor(z));
                if (stored != NO_DEPTH && zi >= stored) continue;
                color[row + px] = blend ? blendOver(color[row + px], argb) : argb;
                depth[row + px] = zi;
            }
        }
    }

    /**
     * Source-over alpha blend of {@code src} onto {@code dst}. Un-premultiplied ARGB, the layout the
     * preview's textures use.
     */
    public static int blendOver(int dst, int src) {
        int sa = (src >>> 24) & 0xFF;
        if (sa == 255) return src;
        if (sa == 0) return dst;
        int da = (dst >>> 24) & 0xFF;
        int outA = sa + da * (255 - sa) / 255;
        if (outA == 0) return 0;
        int r = blendChannel(src >>> 16, dst >>> 16, sa, da, outA);
        int g = blendChannel(src >>> 8, dst >>> 8, sa, da, outA);
        int b = blendChannel(src, dst, sa, da, outA);
        return (outA << 24) | (r << 16) | (g << 8) | b;
    }

    private static int blendChannel(int s, int d, int sa, int da, int outA) {
        int sc = s & 0xFF;
        int dc = d & 0xFF;
        int value = (sc * sa * 255 + dc * da * (255 - sa)) / (outA * 255);
        return value < 0 ? 0 : Math.min(value, 255);
    }
}
