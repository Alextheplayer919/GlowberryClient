package org.chimeramc.client.core.cosmetics;

import java.util.List;

import org.chimeramc.client.core.cosmetics.geometry.PreviewMeshModel;

/**
 * Builds authored Blockbench meshes ({@link PreviewMeshModel}) into the software scene.
 *
 * <p>This replaces the Canvas authored-mesh pass. Each cube face becomes a textured
 * {@link PreviewScene} quad whose UVs come from the mesh's per-face {@link PreviewMeshModel.FaceUv}
 * (with a negative {@code uv_size} mirrored), whose normal is converted from Bedrock cosmetic space
 * into player space by {@link CosmeticOrientation}, and whose shading uses the same light as the
 * character. A pet cube is swung about its bone pivot by the {@link PetPose} rotation; a head-worn
 * accessory rotates about the neck pivot by the head look. Android-free, so it is unit-testable.
 */
public final class PreviewAuthoredScene {

    private PreviewAuthoredScene() {
    }

    /** Adds an authored accessory mesh (offset zero), tracking the head when {@code headLooked}. */
    public static void addAccessory(PreviewScene scene, List<PreviewMeshModel.Box> mesh,
                                    int[] atlas, int atlasW, int atlasH,
                                    int geoTexW, int geoTexH,
                                    float headPitch, float headYaw, boolean headLooked) {
        addMesh(scene, mesh, atlas, atlasW, atlasH, geoTexW, geoTexH, 0f, 0f, 0f,
                headPitch, headYaw, headLooked, null);
    }

    /** Adds an authored pet mesh at a model-space offset, animated by {@code pose}. */
    public static void addPet(PreviewScene scene, List<PreviewMeshModel.Box> mesh,
                              int[] atlas, int atlasW, int atlasH,
                              int geoTexW, int geoTexH,
                              float offsetX, float offsetY, float offsetZ, PetPose pose) {
        addMesh(scene, mesh, atlas, atlasW, atlasH, geoTexW, geoTexH, offsetX, offsetY, offsetZ,
                0f, 0f, false, pose);
    }

    private static void addMesh(PreviewScene scene, List<PreviewMeshModel.Box> mesh,
                                int[] atlas, int atlasW, int atlasH,
                                int geoTexW, int geoTexH,
                                float offsetX, float offsetY, float offsetZ,
                                float headPitch, float headYaw, boolean headLooked, PetPose pose) {
        if (scene == null || mesh == null || mesh.isEmpty() || atlas == null) return;
        // The geometry declares the atlas its UVs are written against; the decoded bitmap can be a
        // different size (many shipped PNGs are not 64x64). UV normalisation must use the geometry's
        // declared size, while the rasteriser samples the real bitmap size. Falling back to the real
        // size when the geometry declares nothing keeps a malformed file working.
        if (geoTexW <= 0) geoTexW = atlasW;
        if (geoTexH <= 0) geoTexH = atlasH;
        float[] p = new float[3];
        float[] n = new float[3];
        for (PreviewMeshModel.Box box : mesh) {
            float[] boneRot = pose == null ? null : PreviewRotations.petBoneRotation(pose, box.bone);
            for (int face = 0; face < 6; face++) {
                n[0] = faceNormalComponent(box, face, 0);
                n[1] = faceNormalComponent(box, face, 1);
                n[2] = faceNormalComponent(box, face, 2);
                if (boneRot != null) {
                    n = PreviewRotations.rotate(n[0], n[1], n[2], boneRot[0], boneRot[1], boneRot[2]);
                }
                if (headLooked) HeadLookTransform.applyVector(headPitch, headYaw, n);
                CosmeticOrientation.normalToPlayerSpace(n);

                int[] indices = PreviewMeshModel.faceCornerIndices(face);
                float[][] corners = new float[4][3];
                for (int i = 0; i < 4; i++) {
                    float[] corner = box.corners[indices[i]];
                    if (boneRot != null) {
                        corner = PreviewRotations.aboutPivot(corner, box.pivot,
                                boneRot[0], boneRot[1], boneRot[2]);
                    }
                    p[0] = corner[0]; p[1] = corner[1]; p[2] = corner[2];
                    CosmeticOrientation.toPlayerSpace(p);
                    if (headLooked) HeadLookTransform.apply(headPitch, headYaw, p);
                    corners[i][0] = p[0] + offsetX;
                    corners[i][1] = p[1] + offsetY;
                    corners[i][2] = p[2] + offsetZ;
                }

                PreviewMeshModel.FaceUv uv = box.faceUv[face];
                float[] uvArr = {0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f};
                if (uv != null) {
                    uvArr = faceUv(uv, geoTexW, geoTexH);
                }
                PreviewScene.Face f = scene.addQuad(corners, uvArr, atlas, atlasW, atlasH);
                if (f == null) continue;
                f.nx = n[0]; f.ny = n[1]; f.nz = n[2];
                // An authored mesh is a closed solid, so a back face is genuinely hidden; the cull
                // happens in PreviewScene.render by the normal.
                f.doubleSided = false;
                f.alphaTest = true;
                f.blend = true;
                f.shade = PreviewLighting.shadeGreyForNormal(n[0], n[1], n[2], 1f) / 255f;
            }
        }
    }

    /**
     * The four corner UVs for a face, applying the negative-uv_size mirror, normalised against the
     * size the <b>geometry</b> declares (not the decoded bitmap size). Clamping uses the same size,
     * so a face whose rectangle runs past the declared atlas edge is trimmed instead of silently
     * shrinking against a wrong bound.
     */
    private static float[] faceUv(PreviewMeshModel.FaceUv uv, int geoTexW, int geoTexH) {
        int u = Math.max(0, uv.u);
        int v = Math.max(0, uv.v);
        int w = uv.w;
        int h = uv.h;
        if (u + w > geoTexW) w = geoTexW - u;
        if (v + h > geoTexH) h = geoTexH - v;
        if (w <= 0 || h <= 0) {
            // A degenerate rect: sample a single texel rather than nothing.
            float su = u / (float) geoTexW;
            float sv = v / (float) geoTexH;
            return new float[]{su, sv, su, sv, su, sv, su, sv};
        }
        float su0 = uv.flipH ? (u + w) : u;
        float su1 = uv.flipH ? u : (u + w);
        float sv0 = uv.flipV ? (v + h) : v;
        float sv1 = uv.flipV ? v : (v + h);
        return new float[]{su0 / geoTexW, sv0 / geoTexH, su1 / geoTexW, sv0 / geoTexH,
                su0 / geoTexW, sv1 / geoTexH, su1 / geoTexW, sv1 / geoTexH};
    }

    /** One component of a cube face's normal. The mesh does not store normals per face, so the
     *  axis-aligned face normal for the face index is used (the same six the renderer uses). */
    private static float faceNormalComponent(PreviewMeshModel.Box box, int face, int component) {
        float[] n = faceNormal(face);
        return n[component];
    }

    private static float[] faceNormal(int face) {
        switch (face) {
            case 0: return new float[]{0, 1, 0};   // TOP
            case 1: return new float[]{0, -1, 0};  // BOTTOM
            case 2: return new float[]{-1, 0, 0};  // LEFT
            case 3: return new float[]{1, 0, 0};   // RIGHT
            case 4: return new float[]{0, 0, 1};   // FRONT
            default: return new float[]{0, 0, -1}; // BACK
        }
    }
}
