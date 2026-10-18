package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.chimeramc.client.core.cosmetics.geometry.BedrockGeometry;
import org.chimeramc.client.core.cosmetics.geometry.BedrockGeometryParser;
import org.chimeramc.client.core.cosmetics.geometry.PreviewMeshModel;
import org.junit.Test;

/**
 * Pins {@link CosmeticThumbnail}: the Closet's standalone tiles must paint the real cosmetic (or an
 * empty tile for a piece with no mesh), never crash the grid, and fill exactly the tile bounds.
 *
 * <p>The renderer is pure, so these run with no device: a procedural hat and a procedural pet render
 * from the catalogue alone, and an authored hat renders from the real asset on disk.
 */
public class CosmeticThumbnailTest {

    private static final int W = 72;
    private static final int H = 72;
    private static final int BACKGROUND = 0xFF17191D;

    @Test
    public void aProceduralAccessoryRendersPixels() {
        CosmeticCatalog.Accessory hat = accessoryOfKind(CosmeticCatalog.AccessoryKind.TOPHAT);
        int[] out = new int[W * H];
        CosmeticThumbnail.renderAccessory(hat, null, null, 0, 0, 0, 0, W, H, out, BACKGROUND);
        assertTrue("procedural hat painted no pixels", painted(out) > 0);
    }

    @Test
    public void aProceduralPetRendersPixels() {
        CosmeticCatalog.Pet pet = CosmeticCatalog.pets().get(0);
        int[] out = new int[W * H];
        CosmeticThumbnail.renderPet(pet, null, null, 0, 0, 0, 0, W, H, out, BACKGROUND);
        assertTrue("procedural pet painted no pixels", painted(out) > 0);
    }

    @Test
    public void anAuthoredHatRendersFromTheRealAsset() {
        String id = firstAuthoredHat();
        if (id == null) return;
        BedrockGeometry geometry = parseHat(id);
        if (geometry == null) return;
        java.util.List<PreviewMeshModel.Box> mesh = PreviewMeshModel.build(geometry);
        if (mesh.isEmpty()) return;
        int[] atlas = readAtlas(new File(assetDir("hats"), id + ".png"));
        if (atlas == null) return;
        int atlasW = atlasWidth;
        int atlasH = atlasHeight;
        CosmeticCatalog.Accessory hat = accessoryById(id);
        if (hat == null) return;

        int[] out = new int[W * H];
        CosmeticThumbnail.renderAccessory(hat, mesh, atlas, atlasW, atlasH,
                geometry.models.get(0).description.textureWidth,
                geometry.models.get(0).description.textureHeight,
                W, H, out, BACKGROUND);
        assertTrue("authored hat painted no pixels", painted(out) > 0);
    }

    @Test
    public void aNullCosmeticFillsTheTileWithTheBackground() {
        int[] out = new int[W * H];
        CosmeticThumbnail.renderAccessory(null, null, null, 0, 0, 0, 0, W, H, out, BACKGROUND);
        for (int p : out) assertEquals(BACKGROUND, p);
    }

    @Test
    public void anUndersizedBufferIsRefusedNotWrittenPastTheEnd() {
        int[] out = new int[4];
        // Must not throw and must not write past the buffer.
        CosmeticThumbnail.renderPet(CosmeticCatalog.pets().get(0), null, null, 0, 0, 0, 0,
                W, H, out, BACKGROUND);
        assertEquals(4, out.length);
    }

    // ---- helpers -------------------------------------------------------------------------

    private static int painted(int[] out) {
        int n = 0;
        for (int p : out) if (p != BACKGROUND) n++;
        return n;
    }

    private static CosmeticCatalog.Accessory accessoryOfKind(CosmeticCatalog.AccessoryKind kind) {
        for (CosmeticCatalog.Accessory a : CosmeticCatalog.accessories()) {
            if (a.kind == kind) return a;
        }
        return null;
    }

    private static CosmeticCatalog.Accessory accessoryById(String id) {
        for (CosmeticCatalog.Accessory a : CosmeticCatalog.accessories()) {
            if (id.equals(a.modelId)) return a;
        }
        return null;
    }

    private static String firstAuthoredHat() {
        for (String id : AuthoredHatModels.IDS) {
            if (new File(assetDir("hats"), id + ".geo.json").isFile()) return id;
        }
        return null;
    }

    private static BedrockGeometry parseHat(String id) {
        try {
            String json = new String(Files.readAllBytes(
                    new File(assetDir("hats"), id + ".geo.json").toPath()),
                    java.nio.charset.StandardCharsets.UTF_8);
            return BedrockGeometryParser.parse(json);
        } catch (Exception e) {
            return null;
        }
    }

    private static int atlasWidth;
    private static int atlasHeight;

    /** Decodes a PNG to ARGB with the tiny decoder the pet coverage test already uses. */
    private static int[] readAtlas(File file) {
        try {
            byte[] png = Files.readAllBytes(file.toPath());
            PetAssetCoverageTest.PngImage image = PetAssetCoverageTest.PngImage.decode(png);
            if (image == null) return null;
            atlasWidth = image.width();
            atlasHeight = image.height();
            return image.argb();
        } catch (Exception e) {
            return null;
        }
    }

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

    private static File assetDir(String kind) {
        return new File(repoRoot(), "resources/cosmetics/models/" + kind);
    }
}
