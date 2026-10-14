package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the graphics-backend classification (no mocks).
 *
 * <p>The native renderer resolves GLES with {@code dlopen} and is a no-op on a Vulkan-only build, so
 * a Vulkan backend must read as "cannot rasterise" rather than as a GLES failure. The label and the
 * drawability answer were previously duplicated switches in the diagnostics report and the debug
 * overlay; this is the single source, so the two cannot drift.
 */
public class RenderBackendTest {

    @Test
    public void nativeCodesMapToTheRightKind() {
        assertEquals(RenderBackend.Kind.GLES, RenderBackend.fromNativeCode(1));
        assertEquals(RenderBackend.Kind.VULKAN, RenderBackend.fromNativeCode(2));
        assertEquals(RenderBackend.Kind.UNKNOWN, RenderBackend.fromNativeCode(0));
        assertEquals(RenderBackend.Kind.UNKNOWN, RenderBackend.fromNativeCode(99));
        assertEquals(RenderBackend.Kind.UNKNOWN, RenderBackend.fromNativeCode(-1));
    }

    @Test
    public void onlyGlesCanRasterise() {
        assertTrue(RenderBackend.canRasterise(RenderBackend.Kind.GLES));
        assertFalse(RenderBackend.canRasterise(RenderBackend.Kind.VULKAN));
        assertFalse(RenderBackend.canRasterise(RenderBackend.Kind.UNKNOWN));
    }

    @Test
    public void vulkanIsCalledOutDistinctly() {
        assertTrue(RenderBackend.isVulkanOnly(RenderBackend.Kind.VULKAN));
        assertFalse(RenderBackend.isVulkanOnly(RenderBackend.Kind.GLES));
        assertTrue(RenderBackend.label(RenderBackend.Kind.VULKAN).contains("Vulkan"));
        assertTrue(RenderBackend.label(RenderBackend.Kind.VULKAN).contains("GLES unavailable"));
        assertEquals("GLES", RenderBackend.label(RenderBackend.Kind.GLES));
        assertEquals("unknown", RenderBackend.label(RenderBackend.Kind.UNKNOWN));
    }
}
