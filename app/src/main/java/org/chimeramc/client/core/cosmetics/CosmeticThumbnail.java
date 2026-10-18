package org.chimeramc.client.core.cosmetics;

import java.util.List;

import org.chimeramc.client.core.cosmetics.geometry.PreviewMeshModel;

/**
 * Renders a single cosmetic on its own into an ARGB buffer, for the Closet's card grid.
 *
 * <p>The cape grid already showed the cloth itself (see {@link CapeThumbnail}); hats, back pieces and
 * pets were still listed as a name and a state pill, so a player had to equip one to see what it
 * looked like. This draws each of those on its own — no player body — through the same
 * {@link PreviewScene} / {@link PreviewRaster} software pipeline the live preview uses, so a card can
 * never show a different mesh or colour than the cosmetic a player gets.
 *
 * <p>Pure and Android-free: it fills an {@code int[]}, and the caller (which owns the decoded atlas
 * and the authored mesh) only turns the buffer into a bitmap. A thumbnail therefore costs no native
 * work and is unit-testable, and a cosmetic whose mesh or atlas is missing paints an empty tile
 * rather than failing the grid.
 */
public final class CosmeticThumbnail {

    /** A standalone cosmetic is drawn as a square tile. */
    public static final float ASPECT = 1f;

    /** The 3/4 view every standalone cosmetic is drawn from, so the tiles read as one set. */
    public static final float YAW_DEG = 34f;

    /** A hair of margin, so a wide hat or a long pet is not clipped by the tile edge. */
    private static final float MARGIN = 0.06f;

    private CosmeticThumbnail() {
    }

    /**
     * Renders a head-worn or body-worn accessory on its own.
     *
     * <p>An accessory with a hand-authored mesh ({@code mesh} non-null) is drawn textured; anything
     * else falls back to the procedural box stack the live preview uses, so a hat that has no authored
     * model still gets a picture rather than a blank card.
     */
    public static void renderAccessory(CosmeticCatalog.Accessory accessory,
                                       List<PreviewMeshModel.Box> mesh, int[] atlas,
                                       int atlasW, int atlasH, int geoTexW, int geoTexH,
                                       int width, int height, int[] out, int background) {
        if (!prepare(width, height, out, background)) return;
        PreviewScene scene = new PreviewScene();
        if (accessory == null) {
            renderScene(scene, width, height, out, background);
            return;
        }
        if (mesh != null && !mesh.isEmpty() && atlas != null) {
            PreviewAuthoredScene.addAccessory(scene, mesh, atlas, atlasW, atlasH,
                    geoTexW, geoTexH, 0f, 0f, accessory.kind.followsHead());
        } else {
            PreviewAccessoryScene.addAccessoryFront(scene, accessory, 0f, 0f);
            PreviewAccessoryScene.addAccessoryBehind(scene, accessory);
        }
        renderScene(scene, width, height, out, background);
    }

    /** Renders a pet on its own, posed at rest so every tile shows the same stance. */
    public static void renderPet(CosmeticCatalog.Pet pet,
                                 List<PreviewMeshModel.Box> mesh, int[] atlas,
                                 int atlasW, int atlasH, int geoTexW, int geoTexH,
                                 int width, int height, int[] out, int background) {
        if (!prepare(width, height, out, background)) return;
        PreviewScene scene = new PreviewScene();
        if (pet == null) {
            renderScene(scene, width, height, out, background);
            return;
        }
        if (mesh != null && !mesh.isEmpty() && atlas != null) {
            PreviewAuthoredScene.addPet(scene, mesh, atlas, atlasW, atlasH,
                    geoTexW, geoTexH, 0f, 0f, 0f, restPose(pet));
        } else {
            PreviewPetScene.addPet(scene, pet, restPose(pet), CosmeticCatalog.PetLocomotion.WALK);
        }
        renderScene(scene, width, height, out, background);
    }

    /** A settled stand pose, so a tile does not capture a mid-stride leg. */
    private static PetPose restPose(CosmeticCatalog.Pet pet) {
        PetPose pose = new PetPose();
        pose.compute(pet == null ? CosmeticCatalog.PetSpecies.CAT : pet.species,
                CosmeticCatalog.PetLocomotion.WALK, 0f);
        return pose;
    }

    private static boolean prepare(int width, int height, int[] out, int background) {
        if (out == null || width <= 0 || height <= 0 || out.length < width * height) return false;
        java.util.Arrays.fill(out, 0, width * height, background);
        return true;
    }

    private static void renderScene(PreviewScene scene, int width, int height, int[] out,
                                    int background) {
        PreviewRaster raster = new PreviewRaster();
        raster.prepare(width, height, background);
        SkinModel.Camera camera = PreviewScene.cameraFor(YAW_DEG);
        PreviewScene.Fit fit = scene.fit(camera, width, height, MARGIN);
        scene.render(raster, camera, fit);
        int[] pixels = raster.pixels();
        if (pixels != null) System.arraycopy(pixels, 0, out, 0, Math.min(pixels.length, out.length));
    }
}
