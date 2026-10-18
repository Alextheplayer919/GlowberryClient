package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.core.cosmetics.geometry.BedrockGeometry;
import org.chimeramc.client.core.cosmetics.geometry.BedrockGeometryParser;
import org.chimeramc.client.core.cosmetics.geometry.PreviewMeshModel;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.zip.Inflater;

/**
 * The authored pet assets: a real coverage and structure check, plus a turntable golden per pet.
 *
 * <p>Phase 4 found the shipped pets were authored carelessly - the owl's atlas is 63% never sampled
 * by a face, the fire dragon 20%, the seraphim 10%, the copper golem has six cubes, and the shark has
 * two bones. This test decodes each real PNG and geometry so a regression that re-introduces a mostly
 * unused atlas or a flat strip fails here rather than shipping. Pets that shipped above the shared
 * limit are held to their measured value ({@link PetAssetCoverage#maxUnreferencedFractionFor}) so the
 * check ratchets: it fails on an increase but not on the recorded debt, whose real fix is
 * re-exporting the original Blockbench projects (those sources are not in this repository).
 */
public class PetAssetCoverageTest {

    /** Eight yaw angles, the same spread the preview golden test uses. */
    private static final float[] YAWS = {0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f};

    private static final int W = 160;
    private static final int H = 200;
    private static final int CHANNEL_TOLERANCE = 16;
    private static final float DIFF_FRACTION = 0.01f;

    private static File repoRoot() {
        File dir = new File("").getAbsoluteFile();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (new File(dir, "app").isDirectory() && new File(dir, "resources").isDirectory()) {
                return dir;
            }
            dir = dir.getParentFile();
        }
        return new File("").getAbsoluteFile();
    }

    private static File assetDir() {
        return new File(repoRoot(), "resources/cosmetics/models/pets");
    }

    private static File goldenDir() {
        return new File(repoRoot(), "app/src/test/resources/golden-pets");
    }

    @Test
    public void everyAuthoredPetHasAGeometryAndTexture() {
        File dir = assetDir();
        assertTrue("pet asset directory missing: " + dir, dir.isDirectory());
        for (String id : AuthoredPetModels.IDS) {
            assertTrue(id + " geometry missing", new File(dir, id + ".geo.json").isFile());
            assertTrue(id + " texture missing", new File(dir, id + ".png").isFile());
        }
    }

    @Test
    public void everyAuthoredPetKeepsItsPaintedAtlasWithinItsBudget() {
        File dir = assetDir();
        StringBuilder failures = new StringBuilder();
        for (String id : AuthoredPetModels.IDS) {
            BedrockGeometry geometry = parse(dir, id);
            PngImage image = PngImage.decode(readBytes(new File(dir, id + ".png")));
            assertNotNull(id + " texture did not decode", image);
            PetAssetCoverage.Result result = PetAssetCoverage.analyse(geometry, image);
            assertTrue(id + " paints nothing", result.painted > 0);
            float budget = PetAssetCoverage.maxUnreferencedFractionFor(id);
            if (result.unreferencedFraction() > budget + 1e-4f) {
                failures.append(id).append(" wastes ")
                        .append(Math.round(result.unreferencedFraction() * 100))
                        .append("% (budget ").append(Math.round(budget * 100)).append("%); ");
            }
        }
        assertTrue("painted atlas waste over budget: " + failures, failures.length() == 0);
    }

    @Test
    public void aKnownOffenderIsActuallyOverTheSharedLimit() {
        // The baseline map must not become a blanket exemption: a listed pet has to still exceed the
        // shared limit, so the entry is removed (not forgotten) once the asset is improved.
        File dir = assetDir();
        for (String id : AuthoredPetModels.IDS) {
            if (!PetAssetCoverage.hasKnownAtlasWaste(id)) continue;
            BedrockGeometry geometry = parse(dir, id);
            PngImage image = PngImage.decode(readBytes(new File(dir, id + ".png")));
            PetAssetCoverage.Result result = PetAssetCoverage.analyse(geometry, image);
            assertTrue(id + " is listed as a known offender but is within the shared limit; remove "
                    + "it from KNOWN_ATLAS_WASTE",
                    result.unreferencedFraction() > PetAssetCoverage.MAX_UNREFERENCED_FRACTION);
        }
    }

    @Test
    public void noNewAuthoredPetIsADegenerateMesh() {
        // The copper golem (6 cubes in 17 bones) and the seraphim (a 40x8x2.8 strip) are recorded
        // debt awaiting the original Blockbench re-export. Any *other* degenerate mesh fails, so a
        // new stand-in model cannot ship unnoticed.
        File dir = assetDir();
        StringBuilder failures = new StringBuilder();
        for (String id : AuthoredPetModels.IDS) {
            BedrockGeometry geometry = parse(dir, id);
            PetAssetCoverage.MeshStats stats = PetAssetCoverage.stats(geometry);
            if (stats.isDegenerate(10, 3f) && !PetAssetCoverage.hasKnownDegenerateMesh(id)) {
                failures.append(id).append(" cubes=").append(stats.cubes)
                        .append(" shortest=").append(stats.shortestSide).append("; ");
            }
        }
        assertTrue("new degenerate pet meshes: " + failures, failures.length() == 0);
    }

    @Test
    public void aRecordedDegeneratePetIsActuallyDegenerate() {
        // The recorded set must not linger: once a pet is re-exported it is no longer degenerate, so
        // its entry has to be removed rather than quietly excusing a future regression.
        File dir = assetDir();
        for (String id : AuthoredPetModels.IDS) {
            if (!PetAssetCoverage.hasKnownDegenerateMesh(id)) continue;
            PetAssetCoverage.MeshStats stats = PetAssetCoverage.stats(parse(dir, id));
            assertTrue(id + " is recorded as degenerate but now passes; remove it from "
                    + "KNOWN_DEGENERATE", stats.isDegenerate(10, 3f));
        }
    }

    @Test
    public void everyAuthoredPetRendersATurntableGolden() {
        File dir = assetDir();
        for (String id : AuthoredPetModels.IDS) {
            BedrockGeometry geometry = parse(dir, id);
            PngImage image = PngImage.decode(readBytes(new File(dir, id + ".png")));
            assertNotNull(id + " texture did not decode", image);
            List<PreviewMeshModel.Box> mesh = PreviewMeshModel.build(geometry);
            assertFalse(id + " has no boxes", mesh.isEmpty());

            PreviewScene scene = new PreviewScene();
            int[] atlas = image.argb();
            PetPose pose = new PetPose();
            pose.compute(CosmeticCatalog.PetSpecies.CAT, CosmeticCatalog.PetLocomotion.WALK, 0f);
            PreviewAuthoredScene.addPet(scene, mesh, atlas, image.width, image.height,
                    geometry.models.get(0).description.textureWidth,
                    geometry.models.get(0).description.textureHeight,
                    0f, 0f, 0f, pose);
            compareOrWriteTurntable(id, scene);
        }
    }

    // ---- Turntable goldens -------------------------------------------------------------------

    private static void compareOrWriteTurntable(String id, PreviewScene scene) {
        File dir = goldenDir();
        assertTrue("could not create golden dir " + dir, dir.isDirectory() || dir.mkdirs());
        for (float yaw : YAWS) {
            String name = id + "_yaw" + (int) yaw;
            int[] pixels = render(scene, yaw);
            File golden = new File(dir, name + ".png");
            if (!golden.isFile()) {
                writePng(golden, pixels);
                continue;
            }
            PngImage expectedImage = PngImage.decode(readBytes(golden));
            if (expectedImage == null) {
                writePng(golden, pixels);
                continue;
            }
            int[] expected = expectedImage.argb();
            int diff = 0;
            for (int i = 0; i < pixels.length && i < expected.length; i++) {
                if (!within(pixels[i], expected[i])) diff++;
            }
            float fraction = diff / (float) pixels.length;
            assertTrue(name + " changed: " + Math.round(fraction * 100) + "% of pixels differ",
                    fraction <= DIFF_FRACTION);
        }
    }

    private static int[] render(PreviewScene scene, float yaw) {
        PreviewRaster raster = new PreviewRaster();
        raster.prepare(W, H, 0xFF101418);
        SkinModel.Camera camera = PreviewScene.cameraFor(yaw);
        PreviewScene.Fit fit = scene.fit(camera, W, H, 0.06f);
        scene.render(raster, camera, fit);
        return raster.pixels();
    }

    private static boolean within(int a, int b) {
        for (int shift = 0; shift < 32; shift += 8) {
            if (Math.abs(((a >> shift) & 0xFF) - ((b >> shift) & 0xFF)) > CHANNEL_TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    private static void writePng(File file, int[] pixels) {
        try {
            java.nio.file.Files.write(file.toPath(), PngWriter.encode(W, H, pixels));
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    // ---- helpers ---------------------------------------------------------------------------

    private static BedrockGeometry parse(File dir, String id) {
        BedrockGeometry geometry = BedrockGeometryParser.parse(
                readText(new File(dir, id + ".geo.json")));
        assertNotNull(id + " geometry did not parse", geometry);
        return geometry;
    }

    private static String readText(File file) {
        byte[] bytes = readBytes(file);
        return bytes == null ? null : new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static byte[] readBytes(File file) {
        try (InputStream in = new FileInputStream(file)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            return out.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    /** A minimal RGBA PNG decoder, so the coverage check runs on the real assets with no Android. */
    static final class PngImage implements PetAssetCoverage.PixelSource {
        private final int width;
        private final int height;
        private final byte[] rgba;

        private PngImage(int width, int height, byte[] rgba) {
            this.width = width;
            this.height = height;
            this.rgba = rgba;
        }

        static PngImage decode(byte[] png) {
            if (png == null || png.length < 24) return null;
            int width = be32(png, 16);
            int height = be32(png, 20);
            if (width <= 0 || height <= 0) return null;
            ByteArrayOutputStream idat = new ByteArrayOutputStream();
            int i = 8;
            while (i + 8 <= png.length) {
                int len = be32(png, i);
                String type = new String(png, i + 4, 4, java.nio.charset.StandardCharsets.US_ASCII);
                if (i + 8 + len > png.length) break;
                if ("IDAT".equals(type)) idat.write(png, i + 8, len);
                if ("IEND".equals(type)) break;
                i += 12 + len;
            }
            byte[] raw = inflate(idat.toByteArray());
            if (raw == null) return null;
            int stride = width * 4;
            byte[] out = new byte[stride * height];
            byte[] prev = new byte[stride];
            int pos = 0;
            for (int y = 0; y < height; y++) {
                if (pos >= raw.length) return null;
                int filter = raw[pos++] & 0xFF;
                byte[] line = new byte[stride];
                System.arraycopy(raw, pos, line, 0, stride);
                pos += stride;
                unfilter(filter, line, prev, stride);
                System.arraycopy(line, 0, out, y * stride, stride);
                prev = line;
            }
            return new PngImage(width, height, out);
        }

        /** The decoded pixels as ARGB ints, for the preview scene and the golden comparison. */
        int[] argb() {
            int[] out = new int[width * height];
            for (int i = 0; i < out.length; i++) {
                int o = i * 4;
                out[i] = ((rgba[o + 3] & 0xFF) << 24) | ((rgba[o] & 0xFF) << 16)
                        | ((rgba[o + 1] & 0xFF) << 8) | (rgba[o + 2] & 0xFF);
            }
            return out;
        }

        private static void unfilter(int filter, byte[] line, byte[] prev, int stride) {
            switch (filter) {
                case 1:
                    for (int x = 4; x < stride; x++) line[x] = (byte) (line[x] + line[x - 4]);
                    break;
                case 2:
                    for (int x = 0; x < stride; x++) line[x] = (byte) (line[x] + prev[x]);
                    break;
                case 3:
                    for (int x = 0; x < stride; x++) {
                        int a = x >= 4 ? line[x - 4] & 0xFF : 0;
                        line[x] = (byte) (line[x] + ((a + (prev[x] & 0xFF)) >> 1));
                    }
                    break;
                case 4:
                    for (int x = 0; x < stride; x++) {
                        int a = x >= 4 ? line[x - 4] & 0xFF : 0;
                        int b = prev[x] & 0xFF;
                        int c = x >= 4 ? prev[x - 4] & 0xFF : 0;
                        int p = a + b - c;
                        int pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
                        int pr = pa <= pb && pa <= pc ? a : (pb <= pc ? b : c);
                        line[x] = (byte) (line[x] + pr);
                    }
                    break;
                default:
                    break;
            }
        }

        private static byte[] inflate(byte[] deflated) {
            Inflater inflater = new Inflater();
            inflater.setInput(deflated);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            try {
                while (!inflater.finished()) {
                    int n = inflater.inflate(buffer);
                    if (n == 0) break;
                    out.write(buffer, 0, n);
                }
            } catch (Exception e) {
                return null;
            } finally {
                inflater.end();
            }
            return out.toByteArray();
        }

        private static int be32(byte[] bytes, int offset) {
            return ((bytes[offset] & 0xFF) << 24) | ((bytes[offset + 1] & 0xFF) << 16)
                    | ((bytes[offset + 2] & 0xFF) << 8) | (bytes[offset + 3] & 0xFF);
        }

        @Override
        public int width() {
            return width;
        }

        @Override
        public int height() {
            return height;
        }

        @Override
        public boolean isPainted(int x, int y) {
            return (rgba[(y * width + x) * 4 + 3] & 0xFF) > 0;
        }
    }
}
