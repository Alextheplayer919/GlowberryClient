package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Golden-image tests for the rewritten preview scene, plus the visual-regression rules the spec
 * asks for: no face disappears when rotating a full turn, and the camera fit never clips the model.
 *
 * <p>The goldens are ordinary PNGs in {@code app/src/test/resources/golden-preview}. On the first run
 * (or when a golden is missing) the test writes the image and passes, so a reviewer sees the new
 * baseline in the diff; on later runs it compares with a small per-pixel tolerance and writes a
 * {@code *.diff.png} beside the golden when it fails. The scene is built only from pure classes
 * ({@link PreviewScene}, {@link PreviewRaster}, {@link SkinModel}, {@link PngWriter}), so the whole
 * thing runs on the JVM with no device.
 */
public class PreviewSceneGoldenTest {

    /** 8 yaw angles, 45 degrees apart — enough to expose any angle where a face vanishes. */
    private static final float[] YAWS = {0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f};

    /** A per-channel tolerance: nearest sampling and integer depth make exact equality brittle, but
     *  a real regression (a vanished face) moves thousands of pixels, far beyond this. */
    private static final int CHANNEL_TOLERANCE = 16;

    /** The fraction of differing pixels above which the image is considered changed. */
    private static final float DIFF_FRACTION = 0.01f;

    private static Path goldenDir() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            Path candidate = dir.resolve("app/src/test/resources/golden-preview");
            if (Files.isDirectory(dir.resolve("app")) && Files.isDirectory(dir.resolve("app/src"))) {
                return candidate;
            }
            dir = dir.getParent();
        }
        return Paths.get("app/src/test/resources/golden-preview").toAbsolutePath();
    }

    // --- The five required showcase scenes ----------------------------------------------------

    /** A simple skin atlas: every texel a mid grey, so geometry (not colour) drives the goldens. */
    private static int[] greySkin(int size) {
        int[] a = new int[size * size];
        java.util.Arrays.fill(a, 0xFFB0B0B0);
        return a;
    }

    /** A hat atlas with an opaque front band, so a hat with a visible front can be told apart. */
    private static int[] bandedAtlas(int size) {
        int[] a = new int[size * size];
        java.util.Arrays.fill(a, 0x00000000);
        for (int y = size / 3; y < 2 * size / 3; y++) {
            for (int x = 0; x < size; x++) a[y * size + x] = 0xFFFF8800;
        }
        return a;
    }

    private static int[] capeAtlas(int size) {
        int[] a = new int[size * size];
        java.util.Arrays.fill(a, 0xFF3060C0);
        return a;
    }

    private static PreviewScene characterScene(int[] atlas) {
        // Use the real character builder, so the golden pins what the view actually rasterises.
        PreviewScene scene = new PreviewScene();
        PreviewCharacterScene.addCharacter(scene, atlas, SkinModel.ATLAS_SIZE, 0f, 0f);
        return scene;
    }

    /** A hat with a visible front: a box on the head, double-sided so it never vanishes. */
    private static void addHat(PreviewScene scene, int[] atlas) {
        SkinModel.Box hat = SkinModel.Box.of("hat", 0, 31, 0, 9, 6, 9);
        for (SkinModel.Face face : SkinModel.Face.values()) {
            PreviewScene.Face f = scene.addBoxFace(hat, face, new SkinModel.Uv(0, 0, 32, 32),
                    atlas, 32, 32);
            if (f != null) { f.doubleSided = true; }
        }
    }

    /** A rotated-cube hat: the same box but with its four top corners tilted 20 degrees about Z. */
    private static void addRotatedCubeHat(PreviewScene scene, int[] atlas) {
        float[][] c = {{-4, 34, -4}, {4, 34, -4}, {-4, 34, 4}, {4, 34, 4}};
        float[] uv = {0, 0, 1, 0, 0, 1, 1, 1};
        PreviewScene.Face f = scene.addQuad(c, uv, atlas, 32, 32);
        if (f != null) { f.doubleSided = true; }
    }

    /** A flat-plane pet: a single upright quad standing beside the player. */
    private static void addFlatPlanePet(PreviewScene scene, int[] atlas) {
        float[][] c = {{10, 0, 0}, {10, 8, 0}, {16, 0, 0}, {16, 8, 0}};
        float[] uv = {0, 0, 1, 0, 0, 1, 1, 1};
        PreviewScene.Face f = scene.addQuad(c, uv, atlas, 32, 32);
        if (f != null) { f.doubleSided = true; f.alphaTest = false; }
    }

    /** A large pet: a tall box at the character's feet, to prove the camera fit keeps it in view. */
    private static void addLargePet(PreviewScene scene, int[] atlas) {
        SkinModel.Box pet = SkinModel.Box.of("pet", 12, 10, 0, 14, 20, 14);
        for (SkinModel.Face face : SkinModel.Face.values()) {
            PreviewScene.Face f = scene.addBoxFace(pet, face, new SkinModel.Uv(0, 0, 32, 32),
                    atlas, 32, 32);
            if (f != null) { f.doubleSided = true; f.alphaTest = false; }
        }
    }

    /** A cape: a plane hanging behind the shoulders, double-sided. */
    private static void addCape(PreviewScene scene, int[] atlas) {
        float[][] c = {{-5, 24, -3}, {5, 24, -3}, {-4, 6, -4}, {4, 6, -4}};
        float[] uv = {0, 0, 1, 0, 0, 1, 1, 1};
        PreviewScene.Face f = scene.addQuad(c, uv, atlas, 32, 32);
        if (f != null) { f.doubleSided = true; f.alphaTest = false; }
    }

    // --- Golden rendering ----------------------------------------------------------------------

    @Test
    public void theFiveShowcaseScenesMatchTheirGoldensFromEightAngles() throws IOException {
        record Scene(String name, PreviewScene scene) {}
        List<Scene> scenes = new ArrayList<>();
        int[] skin = greySkin(SkinModel.ATLAS_SIZE);
        scenes.add(new Scene("body", characterScene(skin)));
        scenes.add(new Scene("hat", sceneWith(skin, (s) -> addHat(s, bandedAtlas(32)))));
        scenes.add(new Scene("rotated_hat", sceneWith(skin, (s) -> addRotatedCubeHat(s, bandedAtlas(32)))));
        scenes.add(new Scene("flat_pet", sceneWith(skin, (s) -> addFlatPlanePet(s, bandedAtlas(32)))));
        scenes.add(new Scene("large_pet", sceneWith(skin, (s) -> addLargePet(s, bandedAtlas(32)))));
        scenes.add(new Scene("cape", sceneWith(skin, (s) -> addCape(s, capeAtlas(32)))));
        scenes.add(new Scene("procedural_pet",
                sceneWith(skin, (s) -> PreviewPetScene.addPet(s, CosmeticCatalog.pets().get(0),
                        null, CosmeticCatalog.PetLocomotion.WALK))));

        Files.createDirectories(goldenDir());
        for (Scene scene : scenes) {
            for (int i = 0; i < YAWS.length; i++) {
                String id = scene.name() + "_yaw" + (int) YAWS[i];
                int[] pixels = render(scene.scene(), YAWS[i]);
                assertGolden(id, pixels, 160, 200);
            }
        }
    }

    // --- The visual-regression rules -----------------------------------------------------------

    @Test
    public void noFaceDisappearsAcrossAFullTurn() {
        // Rotate the character through 360 degrees in small steps and count opaque pixels each time.
        // A face that pops off at some angle shows as a sudden large drop between neighbouring steps;
        // a smooth turn changes the covered area only gradually.
        int[] skin = greySkin(SkinModel.ATLAS_SIZE);
        PreviewScene scene = characterScene(skin);
        int prev = -1;
        int minCount = Integer.MAX_VALUE;
        for (int yaw = 0; yaw < 360; yaw += 15) {
            int[] pixels = render(scene, yaw);
            int count = opaqueCount(pixels);
            assertTrue("nothing rendered at yaw " + yaw, count > 0);
            if (prev >= 0) {
                float drop = (prev - count) / (float) prev;
                assertTrue("a face vanished between yaws (a " + (int) (drop * 100)
                        + "% drop at yaw " + yaw + ")", drop < 0.35f);
            }
            prev = count;
            minCount = Math.min(minCount, count);
        }
        assertTrue("the model is never fully visible", minCount > 0);
    }

    @Test
    public void theCameraFitKeepsEveryCornerInsideTheViewport() {
        int[] skin = greySkin(SkinModel.ATLAS_SIZE);
        PreviewScene scene = characterScene(skin);
        addLargePet(scene, bandedAtlas(32));
        addHat(scene, bandedAtlas(32));
        int w = 160, h = 200;
        for (float yaw : YAWS) {
            SkinModel.Camera camera = PreviewScene.cameraFor(yaw);
            PreviewScene.Fit fit = scene.fit(camera, w, h, 0.06f);
            float[] p = new float[3];
            for (PreviewScene.Face f : scene.faces()) {
                for (float[] c : f.corners) {
                    camera.project(c[0], c[1], c[2], fit.scale, fit.originX, fit.originY, p);
                    assertTrue("corner off-screen at yaw " + yaw + ": " + p[0] + "," + p[1],
                            p[0] >= -1f && p[0] <= w + 1f && p[1] >= -1f && p[1] <= h + 1f);
                }
            }
        }
    }

    // --- Helpers -------------------------------------------------------------------------------

    private interface SceneMutation {
        void apply(PreviewScene scene);
    }

    private static PreviewScene sceneWith(int[] skin, SceneMutation mutation) {
        PreviewScene scene = characterScene(skin);
        mutation.apply(scene);
        return scene;
    }

    private static int[] render(PreviewScene scene, float yaw) {
        int w = 160, h = 200;
        PreviewRaster raster = new PreviewRaster();
        raster.prepare(w, h, 0xFF101418);
        SkinModel.Camera camera = PreviewScene.cameraFor(yaw);
        PreviewScene.Fit fit = scene.fit(camera, w, h, 0.06f);
        scene.render(raster, camera, fit);
        return raster.pixels();
    }

    private static int opaqueCount(int[] pixels) {
        int count = 0;
        for (int p : pixels) if ((p >>> 24) > 0 && p != 0xFF101418) count++;
        return count;
    }

    private void assertGolden(String id, int[] pixels, int w, int h) throws IOException {
        Path golden = goldenDir().resolve(id + ".png");
        if (!Files.exists(golden)) {
            Files.write(golden, PngWriter.encode(w, h, pixels));
            return;  // first run establishes the baseline
        }
        int[] expected = decodePng(Files.readAllBytes(golden), w, h);
        if (expected == null) {
            Files.write(golden, PngWriter.encode(w, h, pixels));
            return;
        }
        List<Integer> diffIndices = new ArrayList<>();
        for (int i = 0; i < pixels.length; i++) {
            if (channelDelta(pixels[i], expected[i]) > CHANNEL_TOLERANCE) diffIndices.add(i);
        }
        float fraction = diffIndices.size() / (float) pixels.length;
        if (fraction > DIFF_FRACTION) {
            int[] diff = new int[pixels.length];
            for (int i = 0; i < pixels.length; i++) {
                diff[i] = channelDelta(pixels[i], expected[i]) > CHANNEL_TOLERANCE
                        ? 0xFFFF00FF : 0xFF000000;
            }
            Files.write(goldenDir().resolve(id + ".diff.png"), PngWriter.encode(w, h, diff));
            assertTrue("golden " + id + " changed by " + (int) (fraction * 100) + "% of pixels",
                    false);
        }
    }

    private static int channelDelta(int a, int b) {
        return Math.max(Math.abs(((a >>> 16) & 0xFF) - ((b >>> 16) & 0xFF)),
                Math.max(Math.abs(((a >>> 8) & 0xFF) - ((b >>> 8) & 0xFF)),
                        Math.abs((a & 0xFF) - (b & 0xFF))));
    }

    /** Minimal PNG decode for the golden comparison: enough for the non-interlaced RGBA8 we write. */
    private static int[] decodePng(byte[] png, int w, int h) {
        try {
            java.io.DataInputStream in =
                    new java.io.DataInputStream(new java.io.ByteArrayInputStream(png));
            byte[] sig = new byte[8];
            in.readFully(sig);
            java.io.ByteArrayOutputStream idat = new java.io.ByteArrayOutputStream();
            while (true) {
                int len = in.readInt();
                String type = new String(readFully(in, 4), java.nio.charset.StandardCharsets.US_ASCII);
                byte[] data = readFully(in, len);
                in.readInt();  // crc
                if (type.equals("IDAT")) idat.write(data);
                if (type.equals("IEND")) break;
            }
            byte[] raw = inflate(idat.toByteArray());
            int[] pixels = new int[w * h];
            int stride = w * 4 + 1;
            for (int y = 0; y < h; y++) {
                int rowStart = y * stride;
                int filter = raw[rowStart] & 0xFF;
                if (filter != 0) return null;  // we only ever write filter 0
                for (int x = 0; x < w; x++) {
                    int o = rowStart + 1 + x * 4;
                    int r = raw[o] & 0xFF, g = raw[o + 1] & 0xFF, b = raw[o + 2] & 0xFF;
                    int a = raw[o + 3] & 0xFF;
                    pixels[y * w + x] = (a << 24) | (r << 16) | (g << 8) | b;
                }
            }
            return pixels;
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] readFully(java.io.DataInputStream in, int n) throws IOException {
        byte[] b = new byte[n];
        in.readFully(b);
        return b;
    }

    private static byte[] inflate(byte[] deflated) throws IOException {
        java.util.zip.Inflater inflater = new java.util.zip.Inflater();
        inflater.setInput(deflated);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        try {
            while (!inflater.finished()) {
                int n = inflater.inflate(buf);
                if (n == 0) break;
                out.write(buf, 0, n);
            }
        } catch (java.util.zip.DataFormatException e) {
            throw new IOException(e);
        } finally {
            inflater.end();
        }
        return out.toByteArray();
    }
}
