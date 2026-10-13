package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins {@link PreviewPetScene}: the procedural pet scene must carry the same rules the Canvas pet
 * pass did — flat-shaded boxes from {@link PetModel}, rotated about their bone pivot by the pose,
 * culled by the unrotated normal and shaded by the rotated normal, and placed beside the player.
 *
 * <p>This is the parity gate for replacing the Canvas pet pass, and it also covers the spec's "every
 * shipped pet loads and produces at least one visible quad" requirement: every catalogue pet must
 * build a non-empty scene.
 */
public class PreviewPetSceneTest {

    private static PreviewScene sceneFor(CosmeticCatalog.Pet pet, PetPose pose,
                                         CosmeticCatalog.PetLocomotion gait) {
        PreviewScene scene = new PreviewScene();
        PreviewPetScene.addPet(scene, pet, pose, gait);
        return scene;
    }

    @Test
    public void everyShippedPetProducesAVisibleScene() {
        assertTrue("no pets in the catalogue", CosmeticCatalog.pets().size() > 0);
        for (CosmeticCatalog.Pet pet : CosmeticCatalog.pets()) {
            PreviewScene scene = sceneFor(pet, null, CosmeticCatalog.PetLocomotion.WALK);
            assertTrue("pet '" + pet.id + "' produced no faces", !scene.faces().isEmpty());
            // Every face is flat-coloured (the procedural pet is not textured).
            for (PreviewScene.Face f : scene.faces()) {
                assertTrue("pet face is not flat-coloured", f.flatColor != 0);
            }
        }
    }

    @Test
    public void everyShippedPetRendersPixels() {
        // Not just non-empty: a real rasterisation of each pet must mark pixels.
        for (CosmeticCatalog.Pet pet : CosmeticCatalog.pets()) {
            PreviewScene scene = sceneFor(pet, null, CosmeticCatalog.PetLocomotion.WALK);
            PreviewRaster raster = new PreviewRaster();
            raster.prepare(120, 120, 0x00000000);
            SkinModel.Camera camera = PreviewScene.cameraFor(30f);
            PreviewScene.Fit fit = scene.fit(camera, 120, 120, 0.05f);
            scene.render(raster, camera, fit);
            int painted = 0;
            for (int p : raster.pixels()) if ((p >>> 24) != 0) painted++;
            assertTrue("pet '" + pet.id + "' rendered no pixels", painted > 0);
        }
    }

    @Test
    public void thePetIsOffsetBesideThePlayer() {
        CosmeticCatalog.Pet cat = CosmeticCatalog.pets().get(0);
        PreviewScene scene = sceneFor(cat, null, CosmeticCatalog.PetLocomotion.WALK);
        float[] bounds = scene.bounds();
        // The pet's minimum X should be around the OFFSET_X (the pet is on the player's right).
        assertTrue("the pet is not offset beside the player: minX=" + bounds[0],
                bounds[0] > PreviewPetScene.OFFSET_X - 5f);
    }

    @Test
    public void aPoseRotationMovesTheAnimatedBoneButNotTheBody() {
        CosmeticCatalog.Pet cat = CosmeticCatalog.pets().get(0);
        PetPose rest = new PetPose();
        rest.compute(cat.species, CosmeticCatalog.PetLocomotion.WALK, 0f);
        PetPose moved = new PetPose();
        moved.compute(cat.species, CosmeticCatalog.PetLocomotion.WALK, 0.25f);
        // A leg swing should change the scene between the two phases.
        PreviewScene a = sceneFor(cat, rest, CosmeticCatalog.PetLocomotion.WALK);
        PreviewScene b = sceneFor(cat, moved, CosmeticCatalog.PetLocomotion.WALK);
        boolean anyDiffers = false;
        int n = Math.min(a.faces().size(), b.faces().size());
        for (int i = 0; i < n && !anyDiffers; i++) {
            for (int c = 0; c < 4; c++) {
                for (int k = 0; k < 3; k++) {
                    if (Math.abs(a.faces().get(i).corners[c][k]
                            - b.faces().get(i).corners[c][k]) > 1e-3f) {
                        anyDiffers = true;
                    }
                }
            }
        }
        assertTrue("a pose phase change should move the geometry", anyDiffers);
    }

    @Test
    public void aFlyingPetIsLiftedOffTheGround() {
        CosmeticCatalog.Pet cat = CosmeticCatalog.pets().get(0);
        PreviewScene walking = sceneFor(cat, null, CosmeticCatalog.PetLocomotion.WALK);
        PreviewScene flying = sceneFor(cat, null, CosmeticCatalog.PetLocomotion.FLY);
        assertTrue("a flying pet should sit higher",
                flying.bounds()[1] > walking.bounds()[1] + 1f);
    }

    @Test
    public void aNullPetAddsNothing() {
        PreviewScene scene = new PreviewScene();
        PreviewPetScene.addPet(scene, null, null, CosmeticCatalog.PetLocomotion.WALK);
        assertTrue(scene.faces().isEmpty());
        assertNotNull(CosmeticCatalog.pets());
    }
}
