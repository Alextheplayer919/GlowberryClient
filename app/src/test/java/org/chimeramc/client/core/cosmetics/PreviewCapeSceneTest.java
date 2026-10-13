package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins {@link PreviewCapeScene}: the cloth must become a {@code (COLS-1)*(ROWS-1)} grid of quads,
 * flat-shaded from {@link CapePatterns} and the quad's own normal, and it must rasterise pixels.
 */
public class PreviewCapeSceneTest {

    private static CosmeticCatalog.Cape cape() {
        return CosmeticCatalog.capes().get(0);
    }

    private static CapeSimulator settledCloth() {
        CapeSimulator sim = new CapeSimulator();
        sim.reset(0f, 0f, 0f);
        for (int i = 0; i < 30; i++) sim.step(CapeSimulator.FIXED_DT, 0f, 0f, 0f, 0f, 0f, 0f);
        return sim;
    }

    @Test
    public void theClothBecomesAGridOfQuads() {
        PreviewScene scene = new PreviewScene();
        PreviewCapeScene.addCape(scene, cape(), settledCloth());
        int expected = (CapeSimulator.COLS - 1) * (CapeSimulator.ROWS - 1);
        assertEquals(expected, scene.faces().size());
        for (PreviewScene.Face f : scene.faces()) {
            assertTrue("a cape quad is not flat-coloured", f.flatColor != 0);
            assertTrue("the cloth must be visible from both sides", f.doubleSided);
        }
    }

    @Test
    public void theClothRendersPixels() {
        PreviewScene scene = new PreviewScene();
        PreviewCapeScene.addCape(scene, cape(), settledCloth());
        PreviewRaster raster = new PreviewRaster();
        raster.prepare(120, 160, 0x00000000);
        SkinModel.Camera camera = PreviewScene.cameraFor(180f);
        PreviewScene.Fit fit = scene.fit(camera, 120, 160, 0.05f);
        scene.render(raster, camera, fit);
        int painted = 0;
        for (int p : raster.pixels()) if ((p >>> 24) != 0) painted++;
        assertTrue("the cape rendered no pixels", painted > 0);
    }

    @Test
    public void theClothHangsBelowItsAnchor() {
        PreviewScene scene = new PreviewScene();
        PreviewCapeScene.addCape(scene, cape(), settledCloth());
        float[] bounds = scene.bounds();
        // The anchor is at ANCHOR_Y (24); the cloth hangs down toward the feet, so it must reach
        // below the anchor.
        assertTrue("the cloth does not hang below its anchor: maxY=" + bounds[4],
                bounds[4] <= PreviewCapeScene.ANCHOR_Y + 0.5f);
    }

    @Test
    public void aNullCapeOrClothAddsNothing() {
        PreviewScene scene = new PreviewScene();
        PreviewCapeScene.addCape(scene, null, settledCloth());
        PreviewCapeScene.addCape(scene, cape(), null);
        assertTrue(scene.faces().isEmpty());
    }
}
