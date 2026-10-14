package org.chimeramc.client.core.cosmetics;

/**
 * Validation and normalisation rules for a captured Minecraft skin, kept pure so every legal size,
 * the exact-ratio downscale and the alpha checks are unit-tested without a device.
 *
 * <p>The capture comes from the game, so it can be a legacy 64x32 skin, a modern 64x64 one, or an
 * HD 128x128/256x256 one, and it can (rarely) be a garbage read. The rules:
 * <ul>
 *   <li>Only the standard skin sizes are accepted: 64x32, 64x64, 128x128, 256x256.</li>
 *   <li>A buffer that is entirely zero (a failed read) is rejected.</li>
 *   <li>A skin with no opaque pixel at all is rejected.</li>
 *   <li>Non-64x64 skins are downscaled by an exact integer ratio (128→64 is /2, 256→64 is /4,
 *       64x32 keeps its height in the top half).</li>
 *   <li>The arm model (wide/slim) is carried through, defaulting to wide when unknown.</li>
 * </ul>
 */
public final class SkinCaptureValidator {

    /** The sizes a skin may be. */
    public static final int[][] LEGAL_SIZES = {
            {64, 32}, {64, 64}, {128, 128}, {256, 256}
    };

    /** The atlas the preview samples. */
    public static final int ATLAS = 64;

    /** The arm models a skin can declare. */
    public enum ArmModel { WIDE, SLIM }

    private SkinCaptureValidator() {
    }

    /** True when width x height is one of the standard skin sizes. */
    public static boolean isLegalSize(int width, int height) {
        for (int[] size : LEGAL_SIZES) {
            if (size[0] == width && size[1] == height) return true;
        }
        return false;
    }

    /** True when the buffer has at least one non-zero byte (a failed read is all zeros). */
    public static boolean hasContent(int[] argb) {
        if (argb == null) return false;
        for (int p : argb) {
            if (p != 0) return true;
        }
        return false;
    }

    /** True when at least one pixel is opaque (alpha >= the skin threshold). */
    public static boolean hasOpaquePixel(int[] argb) {
        if (argb == null) return false;
        for (int p : argb) {
            if (((p >>> 24) & 0xFF) >= SkinAlphaFilter.ALPHA_THRESHOLD) return true;
        }
        return false;
    }

    /**
     * Validates a raw capture. Returns null when it is unusable (bad size, all zero, no opaque
     * pixel, wrong buffer length), otherwise a {@link Validated} carrying the size and arm model.
     */
    public static Validated validate(int width, int height, int[] argb, ArmModel armModel) {
        if (!isLegalSize(width, height)) return null;
        if (argb == null || argb.length != width * height) return null;
        if (!hasContent(argb)) return null;
        if (!hasOpaquePixel(argb)) return null;
        return new Validated(width, height, armModel == null ? ArmModel.WIDE : armModel);
    }

    /** A capture that passed validation. */
    public static final class Validated {
        public final int width;
        public final int height;
        public final ArmModel armModel;

        Validated(int width, int height, ArmModel armModel) {
            this.width = width;
            this.height = height;
            this.armModel = armModel;
        }
    }

    /**
     * Normalises a validated capture to the 64x64 atlas by an exact integer ratio, top-left anchored.
     *
     * <p>64x64 is copied as-is; 128x128 is downscaled /2 and 256x256 /4 (nearest sample); a 64x32
     * legacy skin is copied into the top half with the bottom half transparent, which is the layout
     * the modern UV table expects. The output length is always {@code 64*64}.
     */
    public static int[] normaliseToAtlas(int width, int height, int[] argb) {
        int[] out = new int[ATLAS * ATLAS];
        if (argb == null || width <= 0 || height <= 0) return out;
        if (width == ATLAS && height == ATLAS) {
            System.arraycopy(argb, 0, out, 0, Math.min(argb.length, out.length));
            return out;
        }
        if (width == 64 && height == 32) {
            // Legacy: top half only.
            for (int y = 0; y < 32; y++) {
                System.arraycopy(argb, y * 64, out, y * 64, 64);
            }
            return out;
        }
        // Exact integer ratio downscale (128 -> 2, 256 -> 4).
        int ratio = width / ATLAS;
        if (ratio <= 0) ratio = 1;
        for (int y = 0; y < ATLAS; y++) {
            for (int x = 0; x < ATLAS; x++) {
                int sx = Math.min(width - 1, x * ratio);
                int sy = Math.min(height - 1, y * ratio);
                int src = sy * width + sx;
                out[y * ATLAS + x] = src >= 0 && src < argb.length ? argb[src] : 0;
            }
        }
        return out;
    }

    /** Parses an arm-model string ("slim"/"wide") case-insensitively, defaulting to WIDE. */
    public static ArmModel parseArmModel(String value) {
        if (value == null) return ArmModel.WIDE;
        return "slim".equalsIgnoreCase(value.trim()) ? ArmModel.SLIM : ArmModel.WIDE;
    }
}
