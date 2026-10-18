package org.chimeramc.client.core.cosmetics;

/**
 * The graphics backend the game is running, and what it means for the native draw path.
 *
 * <p>Bedrock's newer RenderDragon builds run Vulkan on Android/Fire OS with no GLES surface. The
 * native cosmetic renderer resolves its GLES entry points with {@code dlopen} and stays a no-op when
 * the table is empty, so a Vulkan-only build is already safe — but the backend was classified by a
 * {@code switch} duplicated in the diagnostics report and the on-screen debug lines, and the two
 * could drift. This is that single classification, and it is pure so the wording and the
 * "can we draw" answer are pinned without a device.
 *
 * <p>The reading is {@link #fromNativeCode} of the native {@code GlBackend} value. A cosmetic no
 * longer reaches the game through a GLES draw — it goes through the engine's own texture/image
 * pipeline, which does not need a GLES surface — so the backend is informational only and no longer
 * decides whether a cosmetic can be drawn.
 */
public final class RenderBackend {

    /** The backend, mirroring the native {@code GlBackend} enum. */
    public enum Kind {
        /** No GLES library and no Vulkan library resolved. */
        UNKNOWN,
        /** A GLES surface resolved; the native renderer can rasterise. */
        GLES,
        /** No GLES, but a Vulkan library is loaded (a Vulkan-only RenderDragon build). */
        VULKAN,
    }

    private RenderBackend() {
    }

    /**
     * Maps the native {@code GlBackend} reading to a {@link Kind}.
     *
     * @param nativeCode 0 unknown, 1 GLES, 2 Vulkan; anything else reads as {@link Kind#UNKNOWN}
     */
    public static Kind fromNativeCode(int nativeCode) {
        switch (nativeCode) {
            case 1: return Kind.GLES;
            case 2: return Kind.VULKAN;
            default: return Kind.UNKNOWN;
        }
    }

    /** The short label for the backend, as shown in the diagnostics report and debug overlay. */
    public static String label(Kind kind) {
        switch (kind) {
            case GLES: return "GLES";
            case VULKAN: return "Vulkan (GLES unavailable)";
            default: return "unknown";
        }
    }
}
