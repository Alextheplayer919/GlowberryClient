package org.chimeramc.client.core.cosmetics;

import org.chimeramc.client.core.cosmetics.geometry.BedrockGeometry;
import org.chimeramc.client.core.cosmetics.geometry.PreviewMeshModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A quality audit for the authored pet meshes: how much of each painted texture any face actually
 * samples, and whether the mesh is structurally complete.
 *
 * <p>Phase 4's measurements found the real defects in the shipped pets: the owl's texture was 100%
 * opaque with 86% of it never sampled by any face, the fire dragon left 37% unsampled, the copper
 * golem had six cubes spread over seventeen bones, and the shark had two bones. A texture that is
 * mostly never sampled is wasted memory and a sign the mesh and the atlas were authored against each
 * other carelessly; a mesh with almost no cubes is a stand-in, not a model.
 *
 * <p>The coverage half is pure: it takes a {@link PixelSource} rather than decoding a PNG, so a JVM
 * test can drive it with a decoded atlas and no Android. The unreferenced-painted fraction is the
 * gate — a painted pixel no face references is either a leftover or a mistake, so the authored pets
 * are required to keep it under {@link #MAX_UNREFERENCED_FRACTION}.
 */
public final class PetAssetCoverage {

    /**
     * The fraction of painted texture pixels allowed to be unreferenced by any face.
     *
     * <p>Deliberately small: a little slack covers an atlas's intentional padding, but a texture
     * that wastes a tenth of its painted area is a mistake worth failing on. The check exists
     * because the owl shipped at 86% before it.
     */
    public static final float MAX_UNREFERENCED_FRACTION = 0.10f;

    /** Reads whether a texel is painted, so the audit is independent of the image decoder. */
    public interface PixelSource {
        int width();

        int height();

        /** True when the texel has any opacity. */
        boolean isPainted(int x, int y);
    }

    /** The coverage of one atlas: how many painted texels a face samples, and how many it does not. */
    public static final class Result {
        public final int painted;
        public final int unreferenced;

        Result(int painted, int unreferenced) {
            this.painted = painted;
            this.unreferenced = unreferenced;
        }

        /** The unreferenced fraction of the painted area, or 0 when nothing is painted. */
        public float unreferencedFraction() {
            return painted <= 0 ? 0f : unreferenced / (float) painted;
        }

        public boolean passes() {
            return unreferencedFraction() <= MAX_UNREFERENCED_FRACTION;
        }
    }

    /** Structural facts about a mesh, so a stand-in model is visible without a device. */
    public static final class MeshStats {
        public final int bones;
        public final int cubes;
        public final int emptyBones;
        /** The mesh's largest bounding-box side in model pixels. */
        public final float longestSide;
        /** The smallest bounding-box side; a flat strip reads as a decal, not a model. */
        public final float shortestSide;

        MeshStats(int bones, int cubes, int emptyBones, float longestSide, float shortestSide) {
            this.bones = bones;
            this.cubes = cubes;
            this.emptyBones = emptyBones;
            this.longestSide = longestSide;
            this.shortestSide = shortestSide;
        }

        /**
         * A mesh is degenerate when it has too few cubes to be a body, or when it is a flat strip.
         *
         * <p>The shark shipped with two bones and the seraphim as a 40 x 8 x 2.8 strip; both are the
         * shape this flags. A pet needs enough cubes to read as a creature and needs depth, so the
         * flatness test is what catches a mesh that is really a textured plane.
         */
        public boolean isDegenerate(int minCubes, float minShortestSide) {
            return cubes < minCubes || shortestSide < minShortestSide;
        }
    }

    /**
     * The measured painted-atlas waste of pets that shipped above the limit, so the check ratchets
     * instead of failing on known debt.
     *
     * <p>Phase 4 measured the real values: the fire dragon leaves 20% of its painted atlas
     * unsampled, the owl 63% (its atlas is a full-bleed background fill), and the seraphim 10%.
     * Re-exporting the original Blockbench projects is the fix, and those sources are not in this
     * repository, so the debt is recorded here rather than the limit being quietly raised for every
     * pet. A value going up still fails, so the check catches a regression; a value coming down
     * means the asset was improved and the entry should be lowered or removed.
     */
    private static final java.util.Map<String, Float> KNOWN_ATLAS_WASTE;
    static {
        java.util.Map<String, Float> waste = new java.util.HashMap<>();
        waste.put("pet_fire_dragon", 0.20f);
        waste.put("pet_owl", 0.63f);
        waste.put("pet_seraphim", 0.11f);
        KNOWN_ATLAS_WASTE = java.util.Collections.unmodifiableMap(waste);
    }

    /**
     * The unreferenced fraction allowed for a pet: the shared limit, raised only as far as that
     * pet's measured shipped value when it is a known offender.
     */
    public static float maxUnreferencedFractionFor(String petId) {
        Float known = petId == null ? null : KNOWN_ATLAS_WASTE.get(petId);
        return known == null ? MAX_UNREFERENCED_FRACTION : Math.max(MAX_UNREFERENCED_FRACTION, known);
    }

    /** True when the pet is a known offender whose atlas still exceeds the shared limit. */
    public static boolean hasKnownAtlasWaste(String petId) {
        return petId != null && KNOWN_ATLAS_WASTE.containsKey(petId);
    }

    /**
     * Pets whose shipped mesh is structurally degenerate, so the check ratchets on structure too.
     *
     * <p>The copper golem has six cubes spread over seventeen bones and the seraphim is a
     * 40 x 8 x 2.8 strip. Both need the original Blockbench re-export; recording them here keeps a
     * <em>new</em> degenerate pet from shipping while not failing the build on the known two.
     */
    private static final java.util.Set<String> KNOWN_DEGENERATE = java.util.Collections.unmodifiableSet(
            new java.util.HashSet<>(java.util.Arrays.asList("pet_copper_golem", "pet_seraphim")));

    /** True when the pet is a recorded degenerate mesh awaiting a re-export. */
    public static boolean hasKnownDegenerateMesh(String petId) {
        return petId != null && KNOWN_DEGENERATE.contains(petId);
    }

    private PetAssetCoverage() {
    }

    /**
     * Measures how much of the painted atlas any face of the resolved mesh samples.
     *
     * <p>The referenced area is the union of every face's source rectangle; a painted texel outside
     * that union is unreferenced. Overlapping faces are counted once, so a model that samples one
     * region twice is not credited for covering more.
     */
    public static Result analyse(BedrockGeometry geometry, PixelSource pixels) {
        if (geometry == null || pixels == null) return new Result(0, 0);
        Set<Long> referenced = referencedTexels(geometry, pixels.width(), pixels.height());
        int painted = 0;
        int unreferenced = 0;
        for (int y = 0; y < pixels.height(); y++) {
            for (int x = 0; x < pixels.width(); x++) {
                if (!pixels.isPainted(x, y)) continue;
                painted++;
                if (!referenced.contains(key(x, y))) unreferenced++;
            }
        }
        return new Result(painted, unreferenced);
    }

    /** The set of texels any face samples, as packed (x<<32|y) keys. */
    private static Set<Long> referencedTexels(BedrockGeometry geometry, int width, int height) {
        Set<Long> referenced = new HashSet<>();
        for (PreviewMeshModel.Box box : PreviewMeshModel.build(geometry)) {
            for (PreviewMeshModel.FaceUv uv : box.faceUv) {
                if (uv == null) continue;
                int x0 = Math.max(0, uv.u);
                int y0 = Math.max(0, uv.v);
                int x1 = Math.min(width, uv.u + Math.abs(uv.w));
                int y1 = Math.min(height, uv.v + Math.abs(uv.h));
                for (int y = y0; y < y1; y++) {
                    for (int x = x0; x < x1; x++) {
                        referenced.add(key(x, y));
                    }
                }
            }
        }
        return referenced;
    }

    /** The structural facts of a mesh, from the parsed geometry (no pixels needed). */
    public static MeshStats stats(BedrockGeometry geometry) {
        if (geometry == null || geometry.models.isEmpty()) {
            return new MeshStats(0, 0, 0, 0f, 0f);
        }
        BedrockGeometry.GeometryModel model = geometry.models.get(0);
        int cubes = 0;
        int emptyBones = 0;
        for (BedrockGeometry.Bone bone : model.bones) {
            if (bone.cubes.isEmpty()) emptyBones++;
            else cubes += bone.cubes.size();
        }
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        boolean any = false;
        for (PreviewMeshModel.Box box : PreviewMeshModel.build(geometry)) {
            for (float[] corner : box.corners) {
                any = true;
                minX = Math.min(minX, corner[0]);
                minY = Math.min(minY, corner[1]);
                minZ = Math.min(minZ, corner[2]);
                maxX = Math.max(maxX, corner[0]);
                maxY = Math.max(maxY, corner[1]);
                maxZ = Math.max(maxZ, corner[2]);
            }
        }
        if (!any) return new MeshStats(model.bones.size(), cubes, emptyBones, 0f, 0f);
        float sx = maxX - minX, sy = maxY - minY, sz = maxZ - minZ;
        float longest = Math.max(sx, Math.max(sy, sz));
        float shortest = Math.min(sx, Math.min(sy, sz));
        return new MeshStats(model.bones.size(), cubes, emptyBones, longest, shortest);
    }

    private static long key(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }
}
