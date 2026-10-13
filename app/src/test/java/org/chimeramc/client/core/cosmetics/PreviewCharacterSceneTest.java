package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins {@link PreviewCharacterScene}: the character scene it builds must carry the same rules the
 * Canvas character pass did, now as data — six boxes, a base and an overlay layer per face, the base
 * layer forced opaque, the overlay layer alpha-tested and nudged toward the viewer, and the head box
 * (and its overlay) rotated by the head look.
 *
 * <p>This is the parity gate for replacing the Canvas character pass: if a rule here breaks, the
 * software character differs from what the Canvas pass drew, and the old pass must not be deleted.
 */
public class PreviewCharacterSceneTest {

    private static int[] skin() {
        int[] a = new int[SkinModel.ATLAS_SIZE * SkinModel.ATLAS_SIZE];
        java.util.Arrays.fill(a, 0xFFB0B0B0);
        return a;
    }

    @Test
    public void theCharacterHasEveryBoxFaceInBothLayers() {
        PreviewScene scene = new PreviewScene();
        PreviewCharacterScene.addCharacter(scene, skin(), SkinModel.ATLAS_SIZE, 0f, 0f);
        // Six boxes x six faces x two layers (base + overlay).
        assertEquals(6 * 6 * 2, scene.faces().size());
    }

    @Test
    public void theBaseLayerIsForcedOpaqueAndTheOverlayIsAlphaTested() {
        PreviewScene scene = new PreviewScene();
        PreviewCharacterScene.addCharacter(scene, skin(), SkinModel.ATLAS_SIZE, 0f, 0f);
        int opaqueBase = 0;
        int alphaTestedOverlay = 0;
        int biased = 0;
        for (PreviewScene.Face f : scene.faces()) {
            if (!f.alphaTest && !f.blend) opaqueBase++;
            if (f.alphaTest && f.blend) alphaTestedOverlay++;
            if (f.depthBias > 0f) biased++;
        }
        assertEquals("36 base faces", 36, opaqueBase);
        assertEquals("36 overlay faces", 36, alphaTestedOverlay);
        assertEquals("the overlay layer is nudged toward the viewer", 36, biased);
    }

    @Test
    public void theHeadBoxFollowsTheHeadLookAndTheBodyDoesNot() {
        // A large yaw should move the head box's corners but leave a leg box untouched.
        PreviewScene rest = new PreviewScene();
        PreviewCharacterScene.addCharacter(rest, skin(), SkinModel.ATLAS_SIZE, 0f, 0f);
        PreviewScene turned = new PreviewScene();
        PreviewCharacterScene.addCharacter(turned, skin(), SkinModel.ATLAS_SIZE, 0f, 45f);

        // The scene order is deterministic (head first, then body, arms, legs), so compare per index.
        boolean headMoved = false;
        boolean nonHeadMoved = false;
        for (int i = 0; i < rest.faces().size(); i++) {
            boolean differs = cornerDiffers(rest.faces().get(i), turned.faces().get(i));
            // Head faces are the first 12 (6 base + 6 overlay).
            if (i < 12) headMoved |= differs;
            else nonHeadMoved |= differs;
        }
        assertTrue("the head should follow the head look", headMoved);
        assertFalse("a non-head box should not move", nonHeadMoved);
    }

    private static boolean cornerDiffers(PreviewScene.Face a, PreviewScene.Face b) {
        for (int c = 0; c < 4; c++) {
            for (int k = 0; k < 3; k++) {
                if (Math.abs(a.corners[c][k] - b.corners[c][k]) > 1e-4f) return true;
            }
        }
        return false;
    }

    @Test
    public void theHeadLookAlsoTurnsTheHeadNormals() {
        PreviewScene rest = new PreviewScene();
        PreviewCharacterScene.addCharacter(rest, skin(), SkinModel.ATLAS_SIZE, 0f, 0f);
        PreviewScene turned = new PreviewScene();
        PreviewCharacterScene.addCharacter(turned, skin(), SkinModel.ATLAS_SIZE, 0f, 45f);
        // Index 4 is the head's LEFT base face. A yaw about Y leaves the TOP/BOTTOM normals
        // unchanged, so a side face is the one that proves the normal was rotated.
        PreviewScene.Face a = rest.faces().get(4);
        PreviewScene.Face b = turned.faces().get(4);
        boolean normalDiffers = Math.abs(a.nx - b.nx) > 1e-4f
                || Math.abs(a.ny - b.ny) > 1e-4f || Math.abs(a.nz - b.nz) > 1e-4f;
        assertTrue("head normals should turn with the head", normalDiffers);
    }

    @Test
    public void aNullSkinAddsNothing() {
        PreviewScene scene = new PreviewScene();
        PreviewCharacterScene.addCharacter(scene, null, SkinModel.ATLAS_SIZE, 0f, 0f);
        assertTrue(scene.faces().isEmpty());
    }
}
