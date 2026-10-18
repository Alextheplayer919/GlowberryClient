package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the graphics-backend classification (no mocks).
 *
 * <p>The backend is informational: a cosmetic reaches the game through the engine's own texture
 * pipeline, which does not need a GLES surface, so the classification only drives the wording in the
 * diagnostics report and the debug overlay. The label was previously a switch duplicated across the
 * two; this is the single source, so the two cannot drift.
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
    public void vulkanIsCalledOutDistinctly() {
        assertTrue(RenderBackend.label(RenderBackend.Kind.VULKAN).contains("Vulkan"));
        assertTrue(RenderBackend.label(RenderBackend.Kind.VULKAN).contains("GLES unavailable"));
        assertEquals("GLES", RenderBackend.label(RenderBackend.Kind.GLES));
        assertEquals("unknown", RenderBackend.label(RenderBackend.Kind.UNKNOWN));
    }
}
