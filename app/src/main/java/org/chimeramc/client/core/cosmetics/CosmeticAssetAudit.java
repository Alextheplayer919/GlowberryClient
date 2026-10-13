package org.chimeramc.client.core.cosmetics;

import org.chimeramc.client.core.cosmetics.geometry.BedrockGeometry;
import org.chimeramc.client.core.cosmetics.geometry.BedrockGeometryParser;
import org.chimeramc.client.core.cosmetics.geometry.PreviewMeshModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A per-cosmetic asset audit for the diagnostics screen: for every authored hat and pet, the real
 * PNG size, the size the geometry declares its UVs against, the cube count, and whether the preview
 * refused to draw it (a size mismatch).
 *
 * <p>This is the honest answer to "why does my hat not appear". The preview used to build a scene
 * with a hard-coded 64x64 atlas while the shipped PNGs are 32x32, 64x48, 128x128 and larger, so the
 * rasteriser indexed out of bounds and the app crashed. The audit shows the two sizes side by side
 * so a mismatch is visible without a device.
 *
 * <p>The asset reading takes an {@link AuthoredGeometry.AssetOpener}, so the same code runs against
 * the app assets and against a directory on the JVM; the formatting half is pure and unit-tested.
 */
public final class CosmeticAssetAudit {

    /** One cosmetic's asset facts. */
    public static final class Entry {
        public final String id;
        public final boolean pet;
        public final int pngW;
        public final int pngH;
        public final int geoW;
        public final int geoH;
        public final int cubes;
        /** True when the geometry declared no texture size (so the PNG size was used as a fallback). */
        public final boolean geoSizeMissing;
        /** True when the PNG size disagrees with the geometry's declared size. */
        public final boolean sizeMismatch;

        public Entry(String id, boolean pet, int pngW, int pngH, int geoW, int geoH, int cubes,
                     boolean geoSizeMissing) {
            this.id = id;
            this.pet = pet;
            this.pngW = pngW;
            this.pngH = pngH;
            this.geoW = geoW;
            this.geoH = geoH;
            this.cubes = cubes;
            this.geoSizeMissing = geoSizeMissing;
            this.sizeMismatch = !geoSizeMissing && (pngW != geoW || pngH != geoH);
        }

        /** A one-line description for the report. */
        public String describe() {
            StringBuilder b = new StringBuilder();
            b.append(id).append(" [").append(pet ? "pet" : "hat").append("]: ");
            b.append("png=").append(pngW).append('x').append(pngH);
            b.append(", geo=").append(geoSizeMissing ? "unset" : (geoW + "x" + geoH));
            b.append(", cubes=").append(cubes);
            if (geoSizeMissing) b.append(" (geometry declares no texture size)");
            else if (sizeMismatch) b.append(" (PNG size differs from geometry — scaled)");
            return b.toString();
        }
    }

    private CosmeticAssetAudit() {
    }

    /** Scans every authored hat and pet through the given asset opener. Never throws. */
    public static List<Entry> scan(AuthoredGeometry.AssetOpener assets) {
        List<Entry> out = new ArrayList<>();
        if (assets == null) return out;
        for (String id : AuthoredHatModels.IDS) {
            Entry e = scanOne(assets, id, false);
            if (e != null) out.add(e);
        }
        for (String id : AuthoredPetModels.IDS) {
            Entry e = scanOne(assets, id, true);
            if (e != null) out.add(e);
        }
        return out;
    }

    private static Entry scanOne(AuthoredGeometry.AssetOpener assets, String id, boolean pet) {
        try {
            String json = pet ? AuthoredPetModels.loadFromAssets(assets, id)
                    : AuthoredHatModels.loadFromAssets(assets, id);
            if (json == null) return null;
            BedrockGeometry geometry = BedrockGeometryParser.parse(json);
            if (geometry == null) return null;
            List<PreviewMeshModel.Box> boxes = PreviewMeshModel.build(geometry);
            int geoW = 0, geoH = 0;
            if (!geometry.models.isEmpty()) {
                geoW = geometry.models.get(0).description.textureWidth;
                geoH = geometry.models.get(0).description.textureHeight;
            }
            String png = pet ? AuthoredPetModels.textureFor(id) : AuthoredHatModels.textureFor(id);
            String dir = pet ? AuthoredPetModels.DIR : AuthoredHatModels.DIR;
            int[] size = pngSize(assets, dir + "/" + png);
            if (size == null) return null;
            return new Entry(id, pet, size[0], size[1], geoW, geoH, boxes.size(),
                    geoW <= 0 || geoH <= 0);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Reads a PNG's real width/height from its IHDR chunk over the asset opener, or null. */
    static int[] pngSize(AuthoredGeometry.AssetOpener assets, String path) {
        try (InputStream in = assets.open(path)) {
            if (in == null) return null;
            byte[] header = new byte[24];
            int read = 0;
            while (read < 24) {
                int n = in.read(header, read, 24 - read);
                if (n <= 0) break;
                read += n;
            }
            if (read < 24) return null;
            int w = ((header[16] & 0xFF) << 24) | ((header[17] & 0xFF) << 16)
                    | ((header[18] & 0xFF) << 8) | (header[19] & 0xFF);
            int h = ((header[20] & 0xFF) << 24) | ((header[21] & 0xFF) << 16)
                    | ((header[22] & 0xFF) << 8) | (header[23] & 0xFF);
            if (w <= 0 || h <= 0) return null;
            return new int[]{w, h};
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** Formats the per-cosmetic lines for the report. Pure. */
    public static String format(List<Entry> entries) {
        if (entries == null || entries.isEmpty()) return "(no authored cosmetics found)";
        StringBuilder b = new StringBuilder();
        for (Entry e : entries) {
            b.append("  ").append(e.describe()).append('\n');
        }
        return b.toString();
    }
}
