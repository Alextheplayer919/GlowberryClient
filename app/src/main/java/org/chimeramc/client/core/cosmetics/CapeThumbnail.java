package org.chimeramc.client.core.cosmetics;

/**
 * Renders a cape's cloth into an ARGB pixel buffer, seen from the back, for the cosmetics grid.
 *
 * <p>The closet used to list capes as a name and a state pill, so a player had to equip a cape to
 * see what it looked like. This paints the cloth itself — the same {@link CapePatterns} weave the
 * preview and the in-game cape use, so a swatch cannot show a different pattern than the cape a
 * player gets — into a small buffer the grid blits as a thumbnail. The name then sits under a real
 * picture rather than standing in for one.
 *
 * <p>Pure and Android-free (it fills an {@code int[]}), so the render is unit-testable: a test can
 * assert the pixel at a known cloth coordinate matches {@link CapePatterns#colorAt} at that same
 * coordinate. The view layer only turns the buffer into a bitmap.
 *
 * <p>The view is the back of the cape, which is the surface a player sees when another player faces
 * them and the one the grid should show. The cloth is drawn with a gentle left-to-right and
 * top-to-bottom shade so it reads as a hanging sheet rather than a flat swatch, and the Optifine
 * monogram is painted as its own solid colour (it is lettering, not a weave).
 */
public final class CapeThumbnail {

    /** A hanging cloth is taller than it is wide. */
    public static final float ASPECT = 10f / 16f;

    private CapeThumbnail() {
    }

    /**
     * Fills {@code out} (width x height, row-major ARGB) with the cape's back.
     *
     * @param cape   the cape to render (null paints the empty colour)
     * @param width  the buffer width in pixels
     * @param height the buffer height in pixels
     * @param out    the buffer; must hold at least {@code width * height} pixels
     * @param empty  the colour for a null cape
     */
    public static void render(CosmeticCatalog.Cape cape, int width, int height, int[] out,
                              int empty) {
        if (out == null || width <= 0 || height <= 0 || out.length < width * height) return;

        if (cape == null) {
            java.util.Arrays.fill(out, 0, width * height, empty);
            return;
        }

        // The cloth occupies the full buffer; a small margin makes the rounded card's edge show the
        // card colour rather than clipping the weave.
        for (int y = 0; y < height; y++) {
            float v = (y + 0.5f) / height;
            // A vertical shade: the hem is a touch darker, so the sheet reads as hanging.
            float verticalShade = 1.0f - 0.16f * v;
            for (int x = 0; x < width; x++) {
                float u = (x + 0.5f) / width;
                int color = CapePatterns.colorAt(cape.pattern, u, v, cape.color, cape.accentColor);
                if (cape.pattern == CosmeticCatalog.CapePattern.OPTIFINE
                        && CapePatterns.optifineMonogramAt(u, v)) {
                    color = cape.accentColor;
                }
                // A soft side shade so the sheet has a lit and a shadow edge rather than being flat.
                float sideShade = 0.94f + 0.06f * (1f - Math.abs(u - 0.5f) * 2f);
                out[y * width + x] = scale(color, verticalShade * sideShade);
            }
        }
    }

    /** Pure ARGB scaling, preserving alpha. */
    static int scale(int color, float factor) {
        int a = (color >>> 24) & 0xFF;
        int r = clamp(Math.round(((color >>> 16) & 0xFF) * factor));
        int g = clamp(Math.round(((color >>> 8) & 0xFF) * factor));
        int b = clamp(Math.round((color & 0xFF) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int clamp(int value) {
        return value < 0 ? 0 : Math.min(value, 255);
    }
}
