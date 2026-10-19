package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The per-piece route classification, pinned so the Cosmetics panel cannot claim a piece is in game
 * when it is not.
 *
 * <p>Phase 3's finding is that a hat has no engine-geometry route on this build, so it is live only
 * through the native render hook; the cape is the exception because it reaches the game as a texture
 * swap that does not need the GL renderer. These tests pin exactly that asymmetry.
 */
public class CosmeticRouteTest {

    @Test
    public void theCapeIsLiveThroughTheEngineTextureSwapWithoutTheRenderer() {
        assertEquals(CosmeticRoute.Route.ENGINE_TEXTURE,
                CosmeticRoute.routeFor(CosmeticRoute.Piece.CAPE, true, false));
        assertTrue(CosmeticRoute.isLiveInGame(CosmeticRoute.Piece.CAPE, true, false));
        assertFalse(CosmeticRoute.isPreviewOnly(CosmeticRoute.Piece.CAPE, true, false));
    }

    @Test
    public void aHatIsPreviewOnlyWithoutTheRenderHookEvenIfTheCapeSwapWorks() {
        assertEquals(CosmeticRoute.Route.PREVIEW_ONLY,
                CosmeticRoute.routeFor(CosmeticRoute.Piece.HAT, true, false));
        assertTrue(CosmeticRoute.isPreviewOnly(CosmeticRoute.Piece.HAT, true, false));
        assertEquals(CosmeticRoute.Route.PREVIEW_ONLY,
                CosmeticRoute.routeFor(CosmeticRoute.Piece.PET, true, false));
    }

    @Test
    public void withTheRenderHookLiveEveryPieceIsInGame() {
        for (CosmeticRoute.Piece piece : CosmeticRoute.Piece.values()) {
            assertTrue("piece " + piece + " should be live",
                    CosmeticRoute.isLiveInGame(piece, false, true));
        }
        assertEquals(CosmeticRoute.Route.NATIVE_RENDER,
                CosmeticRoute.routeFor(CosmeticRoute.Piece.HAT, false, true));
        assertEquals(CosmeticRoute.Route.NATIVE_RENDER,
                CosmeticRoute.routeFor(CosmeticRoute.Piece.PET, false, true));
    }

    @Test
    public void theCapePrefersTheEngineRouteWhenBothAreAvailable() {
        assertEquals(CosmeticRoute.Route.ENGINE_TEXTURE,
                CosmeticRoute.routeFor(CosmeticRoute.Piece.CAPE, true, true));
    }

    @Test
    public void nothingLiveMeansEveryPieceIsPreviewOnly() {
        for (CosmeticRoute.Piece piece : CosmeticRoute.Piece.values()) {
            assertTrue(CosmeticRoute.isPreviewOnly(piece, false, false));
            assertEquals(CosmeticRoute.Route.PREVIEW_ONLY,
                    CosmeticRoute.routeFor(piece, false, false));
        }
    }

    @Test
    public void aNullPieceIsPreviewOnlyRatherThanCrashing() {
        assertEquals(CosmeticRoute.Route.PREVIEW_ONLY,
                CosmeticRoute.routeFor(null, true, true));
    }

    @Test
    public void theGraphicsBackendNeverBlocksAPiece() {
        // Item 12: hats and pets are not "unsupported on Vulkan". Every route a cosmetic takes is
        // backend-agnostic, so no backend may be reported as the reason a piece is preview-only.
        for (RenderBackend.Kind backend : RenderBackend.Kind.values()) {
            assertFalse("backend " + backend + " must not block a piece",
                    CosmeticRoute.backendBlocksThisPiece(backend));
        }
    }
}
