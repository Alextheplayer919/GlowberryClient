package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.core.cosmetics.geometry.BedrockGeometry;
import org.chimeramc.client.core.cosmetics.geometry.BedrockGeometryParser;
import org.chimeramc.client.core.cosmetics.geometry.PreviewMeshModel;
import org.junit.Test;

import java.util.List;

/**
 * Pins {@link PreviewAuthoredScene}: an authored Blockbench mesh must become textured scene quads
 * with a normal converted into player space, a pet cube must swing about its bone pivot, and a
 * head-worn accessory must follow the head look.
 */
public class PreviewAuthoredSceneTest {

    private static final String GEO = "{"
            + "\"format_version\":\"1.12.0\","
            + "\"minecraft:geometry\":[{"
            + "  \"description\":{\"identifier\":\"geometry.test\",\"texture_width\":64,\"texture_height\":64},"
            + "  \"bones\":[{"
            + "    \"name\":\"leg_a\",\"pivot\":[0,4,0],"
            + "    \"cubes\":[{\"origin\":[-4,0,-4],\"size\":[8,8,8],"
            + "      \"uv\":[0,0]}]"
            + "  }]"
            + "}]}";

    private static List<PreviewMeshModel.Box> mesh() {
        BedrockGeometry geometry = BedrockGeometryParser.parse(GEO);
        return PreviewMeshModel.build(geometry);
    }

    private static int[] atlas() {
        int[] a = new int[64 * 64];
        java.util.Arrays.fill(a, 0xFF888888);
        return a;
    }

    @Test
    public void anAuthoredMeshBecomesTexturedFaces() {
        PreviewScene scene = new PreviewScene();
        PreviewAuthoredScene.addAccessory(scene, mesh(), atlas(), 64, 64, 64, 64, 0f, 0f, false);
        assertFalse("the authored mesh produced no faces", scene.faces().isEmpty());
        for (PreviewScene.Face f : scene.faces()) {
            assertTrue("an authored face is not textured", f.atlas != null);
            assertTrue("an authored face is not alpha-tested", f.alphaTest);
        }
    }

    @Test
    public void anAuthoredMeshRendersPixels() {
        PreviewScene scene = new PreviewScene();
        PreviewAuthoredScene.addAccessory(scene, mesh(), atlas(), 64, 64, 64, 64, 0f, 0f, false);
        PreviewRaster raster = new PreviewRaster();
        raster.prepare(120, 120, 0x00000000);
        SkinModel.Camera camera = PreviewScene.cameraFor(30f);
        PreviewScene.Fit fit = scene.fit(camera, 120, 120, 0.05f);
        scene.render(raster, camera, fit);
        int painted = 0;
        for (int p : raster.pixels()) if ((p >>> 24) != 0) painted++;
        assertTrue("the authored mesh rendered no pixels", painted > 0);
    }

    @Test
    public void aPetMeshSwingsWithThePose() {
        PetPose rest = new PetPose();
        rest.compute(CosmeticCatalog.PetSpecies.CAT, CosmeticCatalog.PetLocomotion.WALK, 0f);
        PetPose moved = new PetPose();
        moved.compute(CosmeticCatalog.PetSpecies.CAT, CosmeticCatalog.PetLocomotion.WALK, 0.25f);

        PreviewScene a = new PreviewScene();
        PreviewAuthoredScene.addPet(a, mesh(), atlas(), 64, 64, 64, 64, 20f, 0f, 0f, rest);
        PreviewScene b = new PreviewScene();
        PreviewAuthoredScene.addPet(b, mesh(), atlas(), 64, 64, 64, 64, 20f, 0f, 0f, moved);
        boolean differs = false;
        int n = Math.min(a.faces().size(), b.faces().size());
        for (int i = 0; i < n && !differs; i++) {
            for (int c = 0; c < 4; c++) {
                for (int k = 0; k < 3; k++) {
                    if (Math.abs(a.faces().get(i).corners[c][k]
                            - b.faces().get(i).corners[c][k]) > 1e-3f) differs = true;
                }
            }
        }
        assertTrue("a posed authored pet should move its bone", differs);
    }

    @Test
    public void aHeadWornAuthoredMeshFollowsTheHeadLook() {
        PreviewScene rest = new PreviewScene();
        PreviewAuthoredScene.addAccessory(rest, mesh(), atlas(), 64, 64, 64, 64, 0f, 0f, true);
        PreviewScene turned = new PreviewScene();
        PreviewAuthoredScene.addAccessory(turned, mesh(), atlas(), 64, 64, 64, 64, 0f, 45f, true);
        boolean moved = false;
        for (int i = 0; i < rest.faces().size() && !moved; i++) {
            for (int c = 0; c < 4; c++) {
                for (int k = 0; k < 3; k++) {
                    if (Math.abs(rest.faces().get(i).corners[c][k]
                            - turned.faces().get(i).corners[c][k]) > 1e-3f) moved = true;
                }
            }
        }
        assertTrue("a head-worn authored mesh should follow the head look", moved);
    }

    @Test
    public void aNullMeshOrAtlasAddsNothing() {
        PreviewScene scene = new PreviewScene();
        PreviewAuthoredScene.addAccessory(scene, null, atlas(), 64, 64, 64, 64, 0f, 0f, false);
        PreviewAuthoredScene.addAccessory(scene, mesh(), null, 64, 64, 64, 64, 0f, 0f, false);
        assertTrue(scene.faces().isEmpty());
    }
}
