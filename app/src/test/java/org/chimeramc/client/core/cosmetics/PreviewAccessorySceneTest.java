package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins {@link PreviewAccessoryScene}: every accessory kind must build a non-empty, flat-coloured
 * scene that rasterises, and a head-worn kind must move with the head look while a neck/back kind
 * does not.
 */
public class PreviewAccessorySceneTest {

    private static CosmeticCatalog.Accessory accessoryOfKind(CosmeticCatalog.AccessoryKind kind) {
        for (CosmeticCatalog.Accessory a : CosmeticCatalog.accessories()) {
            if (a.kind == kind) return a;
        }
        return null;
    }

    @Test
    public void everyAccessoryKindBuildsAFlatScene() {
        for (CosmeticCatalog.AccessoryKind kind : CosmeticCatalog.AccessoryKind.values()) {
            if (kind == CosmeticCatalog.AccessoryKind.NONE) continue;
            CosmeticCatalog.Accessory a = accessoryOfKind(kind);
            if (a == null) continue;
            PreviewScene scene = new PreviewScene();
            PreviewAccessoryScene.addAccessoryFront(scene, a, 0f, 0f);
            PreviewAccessoryScene.addAccessoryBehind(scene, a);
            assertFalse("accessory kind " + kind + " produced no faces", scene.faces().isEmpty());
            for (PreviewScene.Face f : scene.faces()) {
                assertTrue("accessory face is not flat-coloured", f.flatColor != 0);
            }
        }
    }

    @Test
    public void everyAccessoryKindRendersPixels() {
        for (CosmeticCatalog.AccessoryKind kind : CosmeticCatalog.AccessoryKind.values()) {
            if (kind == CosmeticCatalog.AccessoryKind.NONE) continue;
            CosmeticCatalog.Accessory a = accessoryOfKind(kind);
            if (a == null) continue;
            PreviewScene scene = new PreviewScene();
            PreviewAccessoryScene.addAccessoryFront(scene, a, 0f, 0f);
            PreviewAccessoryScene.addAccessoryBehind(scene, a);
            PreviewRaster raster = new PreviewRaster();
            raster.prepare(120, 160, 0x00000000);
            SkinModel.Camera camera = PreviewScene.cameraFor(30f);
            PreviewScene.Fit fit = scene.fit(camera, 120, 160, 0.05f);
            scene.render(raster, camera, fit);
            int painted = 0;
            for (int p : raster.pixels()) if ((p >>> 24) != 0) painted++;
            assertTrue("accessory kind " + kind + " rendered no pixels", painted > 0);
        }
    }

    @Test
    public void aHeadWornAccessoryMovesWithTheHeadLook() {
        CosmeticCatalog.Accessory hat = accessoryOfKind(CosmeticCatalog.AccessoryKind.TOPHAT);
        assertTrue("no tophat accessory", hat != null && hat.kind.followsHead());
        PreviewScene rest = new PreviewScene();
        PreviewAccessoryScene.addAccessoryFront(rest, hat, 0f, 0f);
        PreviewScene turned = new PreviewScene();
        PreviewAccessoryScene.addAccessoryFront(turned, hat, 0f, 40f);
        boolean moved = false;
        for (int i = 0; i < rest.faces().size() && !moved; i++) {
            for (int c = 0; c < 4; c++) {
                for (int k = 0; k < 3; k++) {
                    if (Math.abs(rest.faces().get(i).corners[c][k]
                            - turned.faces().get(i).corners[c][k]) > 1e-3f) moved = true;
                }
            }
        }
        assertTrue("a head-worn accessory should follow the head look", moved);
    }

    @Test
    public void aNeckAccessoryDoesNotFollowTheHead() {
        CosmeticCatalog.Accessory scarf = accessoryOfKind(CosmeticCatalog.AccessoryKind.SCARF);
        assertTrue("no scarf accessory", scarf != null && !scarf.kind.followsHead());
        PreviewScene rest = new PreviewScene();
        PreviewAccessoryScene.addAccessoryFront(rest, scarf, 0f, 0f);
        PreviewScene turned = new PreviewScene();
        PreviewAccessoryScene.addAccessoryFront(turned, scarf, 0f, 40f);
        // Every corner must be identical: a neck piece ignores the head look.
        for (int i = 0; i < rest.faces().size(); i++) {
            for (int c = 0; c < 4; c++) {
                for (int k = 0; k < 3; k++) {
                    assertTrue("a neck accessory must not follow the head look",
                            Math.abs(rest.faces().get(i).corners[c][k]
                                    - turned.faces().get(i).corners[c][k]) < 1e-4f);
                }
            }
        }
    }

    @Test
    public void aNullAccessoryAddsNothing() {
        PreviewScene scene = new PreviewScene();
        PreviewAccessoryScene.addAccessoryFront(scene, null, 0f, 0f);
        PreviewAccessoryScene.addAccessoryBehind(scene, null);
        assertTrue(scene.faces().isEmpty());
    }
}
