package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.core.cosmetics.geometry.BedrockGeometry;
import org.chimeramc.client.core.cosmetics.geometry.BedrockGeometryParser;
import org.chimeramc.client.core.cosmetics.geometry.PreviewMeshModel;
import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.List;

/**
 * Coverage regression for the preview's triangle split (BUG 1).
 *
 * <p>Every textured or flat quad is two triangles. The shipped split used corner indices that both
 * covered the left half of the quad, leaving a triangular notch on the right of every face. These
 * tests assert the quad is fully covered, the shared diagonal has no fill-rule gap, and — for every
 * shipped authored hat and pet — no pixel inside a face's silhouette is see-through.
 *
 * <p>All pure JVM: no device, no Android.
 */
public class PreviewSceneCoverageTest {

    private static final int W = 160;
    private static final int H = 200;
    private static final int BG = 0xFF101418;

    private static File modelsDir(String sub) {
        for (File f : new File[]{
                new File("src/main/../../resources/cosmetics/models/" + sub),
                new File("../resources/cosmetics/models/" + sub),
                new File("resources/cosmetics/models/" + sub)}) {
            if (f.isDirectory()) return f;
        }
        return null;
    }

    private static int[] solidAtlas(int size, int color) {
        int[] a = new int[size * size];
        java.util.Arrays.fill(a, color);
        return a;
    }

    private static PreviewScene.Fit fitFor(PreviewScene scene, float yaw) {
        SkinModel.Camera cam = PreviewScene.cameraFor(yaw);
        return scene.fit(cam, W, H, 0.06f);
    }

    // --- 1. Solid-quad coverage -----------------------------------------------------------------

    @Test
    public void aSolidQuadIsFullyCoveredAtManyPositions() {
        // A textured quad filling the frame, drawn through the real PreviewScene.render split.
        int[] atlas = solidAtlas(2, 0xFFFF0000);
        for (int trial = 0; trial < 16; trial++) {
            PreviewScene scene = new PreviewScene();
            // Four corners forming an axis-aligned rectangle in model space at various places/sizes.
            float size = 4f + trial;
            float cx = (trial % 4) * 2f;
            float cy = (trial / 4) * 2f;
            float[][] c = {
                    {cx, cy, 0f}, {cx + size, cy, 0f}, {cx, cy + size, 0f}, {cx + size, cy + size, 0f}};
            float[] uv = {0, 0, 1, 0, 0, 1, 1, 1};
            PreviewScene.Face f = scene.addQuad(c, uv, atlas, 2, 2);
            f.doubleSided = true;
            f.alphaTest = false;

            PreviewRaster raster = new PreviewRaster();
            raster.prepare(W, H, BG);
            SkinModel.Camera cam = PreviewScene.cameraFor(0f);
            scene.render(raster, cam, scene.fit(cam, W, H, 0.06f));

            // Count painted pixels and compare to the projected polygon area.
            int painted = 0;
            for (int p : raster.pixels()) if (p == 0xFFFF0000) painted++;

            // Compute the projected polygon area (shoelace, perimeter order TL,TR,BR,BL).
            float[][] screen = new float[4][2];
            float[] out = new float[3];
            PreviewScene.Fit fit = scene.fit(cam, W, H, 0.06f);
            int[] order = {0, 1, 3, 2};
            for (int i = 0; i < 4; i++) {
                int k = order[i];
                cam.project(c[k][0], c[k][1], c[k][2], fit.scale, fit.originX, fit.originY, out);
                screen[i][0] = out[0];
                screen[i][1] = out[1];
            }
            float area = 0f;
            for (int i = 0; i < 4; i++) {
                int j = (i + 1) % 4;
                area += screen[i][0] * screen[j][1] - screen[j][0] * screen[i][1];
            }
            area = Math.abs(area) / 2f;
            // Allow a perimeter-sized slack for the half-pixel sampling rule.
            float perimeter = 0f;
            for (int i = 0; i < 4; i++) {
                int j = (i + 1) % 4;
                perimeter += Math.hypot(screen[j][0] - screen[i][0], screen[j][1] - screen[i][1]);
            }
            assertTrue("trial " + trial + ": painted " + painted + " vs area " + area
                            + " (perimeter " + perimeter + ")",
                    painted >= area - perimeter && painted <= area + perimeter);
        }
    }

    @Test
    public void noPixelOnTheSharedDiagonalIsMissing() {
        int[] atlas = solidAtlas(2, 0xFFFF0000);
        PreviewScene scene = new PreviewScene();
        float[][] c = {{-8, 0, 0}, {8, 0, 0}, {-8, 16, 0}, {8, 16, 0}};
        float[] uv = {0, 0, 1, 0, 0, 1, 1, 1};
        PreviewScene.Face f = scene.addQuad(c, uv, atlas, 2, 2);
        f.doubleSided = true;
        f.alphaTest = false;

        PreviewRaster raster = new PreviewRaster();
        raster.prepare(W, H, BG);
        SkinModel.Camera cam = PreviewScene.cameraFor(0f);
        PreviewScene.Fit fit = scene.fit(cam, W, H, 0.06f);
        scene.render(raster, cam, fit);

        // Walk the projected TL-BR diagonal (the shared edge of the two triangles) in its interior;
        // a fill-rule gap between the triangles would drop pixels along it.
        float[] tl = project(cam, fit, c[0]);
        float[] br = project(cam, fit, c[3]);
        int steps = 64;
        for (int i = 0; i <= steps; i++) {
            float t = 0.15f + 0.7f * (i / (float) steps);
            int x = (int) (tl[0] + (br[0] - tl[0]) * t);
            int y = (int) (tl[1] + (br[1] - tl[1]) * t);
            if (x < 0 || y < 0 || x >= W || y >= H) continue;
            assertEquals("diagonal pixel (" + x + "," + y + ") missing",
                    0xFFFF0000, raster.pixels()[y * W + x]);
        }
    }

    private static float[] project(SkinModel.Camera cam, PreviewScene.Fit fit, float[] corner) {
        float[] out = new float[3];
        cam.project(corner[0], corner[1], corner[2], fit.scale, fit.originX, fit.originY, out);
        return out;
    }

    // --- 2. Authored hats and pets: no see-through inside a face --------------------------------

    @Test
    public void everyAuthoredHatAndPetHasNoSeeThroughPixelsInsideAFace() throws Exception {
        File hatsDir = modelsDir("hats");
        File petsDir = modelsDir("pets");
        Assume.assumeTrue("assets not found", hatsDir != null && petsDir != null);
        int checked = 0;
        for (String id : AuthoredHatModels.IDS) {
            checked += checkModel(hatsDir, id, false);
        }
        for (String id : AuthoredPetModels.IDS) {
            checked += checkModel(petsDir, id, true);
        }
        assertTrue("no authored models were checked", checked > 0);
    }

    /**
     * Builds the authored mesh from its real assets, renders it at 8 yaws, and asserts no pixel
     * inside the union of its faces is see-through.
     */
    private int checkModel(File dir, String id, boolean pet) throws Exception {
        String json = pet ? AuthoredPetModels.loadFromDir(dir, id)
                : AuthoredHatModels.loadFromDir(dir, id);
        if (json == null) return 0;
        BedrockGeometry geometry = BedrockGeometryParser.parse(json);
        if (geometry == null) return 0;
        List<PreviewMeshModel.Box> boxes = PreviewMeshModel.build(geometry);
        if (boxes.isEmpty()) return 0;
        if (pet) PreviewMeshModel.normalizeToHeight(boxes, 14f);

        int[] pngSize = pngSize(dir, pet ? AuthoredPetModels.textureFor(id)
                : AuthoredHatModels.textureFor(id));
        if (pngSize == null) return 0;
        int real = pngSize[0];
        int realH = pngSize[1];
        // Synthesise a fully-opaque atlas of the PNG's real size, so no face is see-through for a
        // reason unrelated to the coverage rule under test.
        int[] atlas = new int[real * realH];
        java.util.Arrays.fill(atlas, 0xFFFFFFFF);
        int texW = geometry.models.isEmpty() ? 64 : geometry.models.get(0).description.textureWidth;
        int texH = geometry.models.isEmpty() ? 64 : geometry.models.get(0).description.textureHeight;

        for (int yaw = 0; yaw < 360; yaw += 45) {
            PreviewScene scene = new PreviewScene();
            if (pet) {
                PreviewAuthoredScene.addPet(scene, boxes, atlas, real, realH, texW, texH,
                        0f, 0f, 0f, null);
            } else {
                PreviewAuthoredScene.addAccessory(scene, boxes, atlas, real, realH, texW, texH,
                        0f, 0f, false);
            }
            if (scene.faces().isEmpty()) continue;

            PreviewRaster raster = new PreviewRaster();
            raster.prepare(W, H, BG);
            SkinModel.Camera cam = PreviewScene.cameraFor(yaw);
            PreviewScene.Fit fit = scene.fit(cam, W, H, 0.06f);
            scene.render(raster, cam, fit);

            // Union mask: render each face alone and mark every pixel it covers. Then every marked
            // pixel in the full render must not be background (a face was see-through there).
            boolean[] union = new boolean[W * H];
            PreviewRaster one = new PreviewRaster();
            for (PreviewScene.Face face : scene.faces()) {
                PreviewScene single = new PreviewScene();
                single.faces().add(face);
                one.prepare(W, H, BG);
                single.render(one, cam, fit);
                int[] px = one.pixels();
                for (int i = 0; i < px.length; i++) if (px[i] != BG) union[i] = true;
            }
            int[] full = raster.pixels();
            for (int i = 0; i < union.length; i++) {
                if (union[i] && full[i] == BG) {
                    assertTrue(id + " has a see-through pixel at yaw " + yaw, false);
                }
            }
        }
        return 1;
    }

    /** Reads a PNG's real width/height from its IHDR chunk, or null. JVM-safe (no java.awt). */
    private static int[] pngSize(File dir, String file) {
        if (file == null) return null;
        File f = new File(dir, file);
        if (!f.isFile()) return null;
        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            byte[] header = new byte[24];
            if (raf.read(header) != 24) return null;
            // Bytes 0..7 signature, 8..11 length, 12..15 "IHDR", 16..19 width, 20..23 height.
            int w = ((header[16] & 0xFF) << 24) | ((header[17] & 0xFF) << 16)
                    | ((header[18] & 0xFF) << 8) | (header[19] & 0xFF);
            int h = ((header[20] & 0xFF) << 24) | ((header[21] & 0xFF) << 16)
                    | ((header[22] & 0xFF) << 8) | (header[23] & 0xFF);
            if (w <= 0 || h <= 0) return null;
            return new int[]{w, h};
        } catch (Exception e) {
            return null;
        }
    }
}
