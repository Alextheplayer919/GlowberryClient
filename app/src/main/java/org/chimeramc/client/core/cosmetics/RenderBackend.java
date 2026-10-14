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
 * <p>The reading is {@link #fromNativeCode} of the native {@code GlBackend} value. The native
 * {@code isRendererDrawable} flag remains the authority for whether a draw can happen; this class
 * only says what the backend is and how to describe it.
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

    /**
     * True when the native renderer can rasterise on this backend.
     *
     * <p>Only {@link Kind#GLES} can. A Vulkan-only build fires the render hook but has no GLES entry
     * points, so a module must treat it as "preview only" rather than attempting a legacy GLES draw
     * (which is the mismatch that crashed modules before the GLES table was resolved dynamically).
     */
    public static boolean canRasterise(Kind kind) {
        return kind == Kind.GLES;
    }

    /** True when the backend is a Vulkan-only build (the case the status wording must call out). */
    public static boolean isVulkanOnly(Kind kind) {
        return kind == Kind.VULKAN;
    }
}
