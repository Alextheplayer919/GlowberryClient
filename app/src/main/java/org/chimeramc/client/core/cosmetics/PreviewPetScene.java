package org.chimeramc.client.core.cosmetics;

import java.util.List;

/**
 * Builds the procedural pet scene from {@link PetModel}, the shared vanilla-grounded body plan the
 * in-game geometry also uses, so the preview and the game cannot drift.
 *
 * <p>This is the pure replacement for the preview's Canvas pet pass and follows it exactly: parts are
 * flat-shaded boxes (no texture atlas), a box is rotated about its bone pivot by the {@link PetPose}
 * rotation for that bone, the face is culled by the <em>unrotated</em> normal and shaded by the
 * <em>rotated</em> normal (so an animated limb brightens and darkens through its arc), and the whole
 * pet is offset beside the player. Android-free, so it is unit-testable.
 */
public final class PreviewPetScene {

    /** Where the pet stands beside the player, in model pixels, matching the Canvas pass. */
    public static final float OFFSET_X = 20f;

    /** The stable bone order the Canvas pass used, kept so grouping is unchanged. */
    private static final String[] BONES = {
            PetModel.BONE_ROOT, PetModel.BONE_TAIL, PetModel.BONE_WING_L,
            PetModel.BONE_WING_R, PetModel.BONE_LEG_A, PetModel.BONE_LEG_B,
            PetModel.BONE_LEG_C, PetModel.BONE_HEAD};

    private PreviewPetScene() {
    }

    /**
     * Adds the procedural pet to {@code scene}.
     *
     * @param scene      the scene to add to (not cleared)
     * @param pet        the equipped pet (species + colours + scale)
     * @param pose       the current pose, or null for a rest pose
     * @param locomotion the current gait, for the flying/swimming lift
     */
    public static void addPet(PreviewScene scene, CosmeticCatalog.Pet pet, PetPose pose,
                              CosmeticCatalog.PetLocomotion locomotion) {
        if (scene == null || pet == null) return;
        PetModel.Body body = PetModel.body(pet.species, pet.scale);
        List<PetModel.Part> parts = PetModel.parts(pet.species, pet.scale);

        int bodyColor = pet.color;
        int accent = pet.accentColor;
        int dark = darker(bodyColor);

        float lift = 0f;
        if (locomotion == CosmeticCatalog.PetLocomotion.FLY) {
            lift = 5f + (pose == null ? 0f : pose.bodyBob * 0.6f);
        } else if (locomotion == CosmeticCatalog.PetLocomotion.SWIM) {
            lift = 1.5f;
        }
        float offsetY = lift + (pose == null ? 0f : pose.bodyBob - pose.crouchDrop);

        for (String bone : BONES) {
            float[] pivot = PetModel.pivot(bone, body);
            float[] rot = PreviewRotations.petBoneRotation(pose, bone);
            float rotX = rot == null ? 0f : rot[0];
            float rotY = rot == null ? 0f : rot[1];
            float rotZ = rot == null ? 0f : rot[2];
            for (PetModel.Part part : parts) {
                if (!part.bone.equals(bone)) continue;
                int color = part.accent ? accent : bodyColor;
                if (part.role == PetModel.Role.LEG) color = dark;
                addPart(scene, part, pivot, rotX, rotY, rotZ, color, OFFSET_X, offsetY);
            }
        }
    }

    private static void addPart(PreviewScene scene, PetModel.Part part, float[] pivot,
                                float rotX, float rotY, float rotZ, int color,
                                float offX, float offY) {
        float x0 = part.x, y0 = part.y, z0 = part.z;
        float x1 = part.x + part.sx, y1 = part.y + part.sy, z1 = part.z + part.sz;

        for (SkinModel.Face face : SkinModel.Face.values()) {
            float[] n = PreviewRotations.rotate(face.nx, face.ny, face.nz, rotX, rotY, rotZ);
            // The shade is the rotated-normal colour; a zero rotation uses the face-ramp colour,
            // matching the Canvas pass exactly.
            int shaded = (rotX == 0f && rotY == 0f && rotZ == 0f)
                    ? PreviewLighting.shadeColor(color, face, 1f)
                    : PreviewLighting.shadeColorForNormal(color, n[0], n[1], n[2]);

            float[][] c = faceCorners(face, x0, y0, z0, x1, y1, z1);
            float[][] corners = new float[4][3];
            for (int i = 0; i < 4; i++) {
                float[] p = PreviewRotations.aboutPivot(c[i], pivot, rotX, rotY, rotZ);
                corners[i][0] = p[0] + offX;
                corners[i][1] = p[1] + offY;
                corners[i][2] = p[2];
            }
            PreviewScene.Face f = scene.addFlatQuad(corners, shaded);
            if (f == null) continue;
            // Cull by the unrotated normal (the Canvas pass's camera.faceVisible), so which faces
            // appear is identical; the rotated normal only drives the shade above.
            f.nx = face.nx; f.ny = face.ny; f.nz = face.nz;
        }
    }

    /** The four grid-order corners of an axis-aligned box face, matching {@link SkinModel.Box}. */
    private static float[][] faceCorners(SkinModel.Face f,
                                         float x0, float y0, float z0,
                                         float x1, float y1, float z1) {
        switch (f) {
            case TOP:
                return new float[][]{{x0, y1, z0}, {x1, y1, z0}, {x0, y1, z1}, {x1, y1, z1}};
            case BOTTOM:
                return new float[][]{{x0, y0, z1}, {x1, y0, z1}, {x0, y0, z0}, {x1, y0, z0}};
            case LEFT:
                return new float[][]{{x0, y1, z1}, {x0, y1, z0}, {x0, y0, z1}, {x0, y0, z0}};
            case RIGHT:
                return new float[][]{{x1, y1, z0}, {x1, y1, z1}, {x1, y0, z0}, {x1, y0, z1}};
            case FRONT:
                return new float[][]{{x0, y1, z1}, {x1, y1, z1}, {x0, y0, z1}, {x1, y0, z1}};
            case BACK:
            default:
                return new float[][]{{x1, y1, z0}, {x0, y1, z0}, {x1, y0, z0}, {x0, y0, z0}};
        }
    }

    /** A colour darkened to 62%, matching the Canvas pass's leg colour. */
    private static int darker(int color) {
        int r = (int) (((color >>> 16) & 0xFF) * 0.62f);
        int g = (int) (((color >>> 8) & 0xFF) * 0.62f);
        int b = (int) ((color & 0xFF) * 0.62f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
