package org.chimeramc.client.core.cosmetics;

/**
 * Builds the preview's character scene — the six player boxes with their base and overlay layers —
 * as {@link PreviewScene} geometry.
 *
 * <p>This is the pure replacement for the preview's old {@code Canvas} character pass. It carries the
 * same rules that pass had, now as data: each face's texture rectangle comes from
 * {@link SkinModel.Box}, the head box (and its overlay) follows the idle head look about the neck
 * pivot, the base layer is forced opaque and the overlay layer is alpha-tested, and both are shaded by
 * the one directional light in {@link PreviewLighting}. Because the rasteriser holds a depth buffer,
 * the overlay is nudged toward the viewer by a small bias rather than being ordered by hand.
 *
 * <p>Android-free: it reads the skin from an {@code int[]} atlas, so the scene is unit-testable and
 * can be snapped to a golden PNG on the JVM.
 */
public final class PreviewCharacterScene {

    /** How far the overlay layer is nudged toward the viewer, so it wins over its base face. */
    public static final float OVERLAY_DEPTH_BIAS = 0.5f;

    private PreviewCharacterScene() {
    }

    /**
     * Adds the character to {@code scene} from a skin atlas.
     *
     * @param scene      the scene to add to (not cleared)
     * @param atlas      the skin atlas pixels, {@code atlasSize * atlasSize} ARGB
     * @param atlasSize  the atlas edge length (64)
     * @param headPitch  the head look pitch in degrees
     * @param headYaw    the head look yaw in degrees
     */
    public static void addCharacter(PreviewScene scene, int[] atlas, int atlasSize,
                                    float headPitch, float headYaw) {
        if (scene == null || atlas == null || atlasSize <= 0) return;
        float[] p = new float[3];
        float[] n = new float[3];
        for (SkinModel.Box box : SkinModel.boxes()) {
            boolean head = "head".equals(box.id);
            for (SkinModel.Face face : SkinModel.Face.values()) {
                SkinModel.Uv baseUv = box.baseUv(face);
                if (baseUv != null) {
                    PreviewScene.Face f = addFace(scene, box, face, baseUv, atlas, atlasSize,
                            head, headPitch, headYaw, p, n);
                    if (f != null) {
                        f.alphaTest = false;
                        f.blend = false;
                        f.shade = PreviewLighting.shadeGreyFor(face, 1f) / 255f;
                    }
                }
                SkinModel.Uv overUv = box.overlayUv(face);
                if (overUv != null) {
                    PreviewScene.Face f = addFace(scene, box, face, overUv, atlas, atlasSize,
                            head, headPitch, headYaw, p, n);
                    if (f != null) {
                        f.alphaTest = true;
                        f.blend = true;
                        f.depthBias = OVERLAY_DEPTH_BIAS;
                        f.shade = PreviewLighting.shadeGreyForOverlayLayer(face, 1f) / 255f;
                    }
                }
            }
        }
    }

    /**
     * Adds one box face, rotating its corners and normal by the head look when it is the head box.
     * The texture corner order matches {@link SkinModel.Box#faceCorners} (grid order), so a texture
     * is upright on every side.
     */
    private static PreviewScene.Face addFace(PreviewScene scene, SkinModel.Box box,
                                             SkinModel.Face face, SkinModel.Uv uv, int[] atlas,
                                             int atlasSize, boolean head, float headPitch,
                                             float headYaw, float[] p, float[] n) {
        float[][] c = box.faceCorners(face);
        float[][] corners = new float[4][3];
        for (int i = 0; i < 4; i++) {
            if (head) {
                p[0] = c[i][0]; p[1] = c[i][1]; p[2] = c[i][2];
                HeadLookTransform.apply(headPitch, headYaw, p);
                corners[i][0] = p[0]; corners[i][1] = p[1]; corners[i][2] = p[2];
            } else {
                corners[i][0] = c[i][0]; corners[i][1] = c[i][1]; corners[i][2] = c[i][2];
            }
        }
        float u0 = uv.u / (float) atlasSize, v0 = uv.v / (float) atlasSize;
        float u1 = (uv.u + uv.w) / (float) atlasSize, v1 = (uv.v + uv.h) / (float) atlasSize;
        float[] uvArr = {u0, v0, u1, v0, u0, v1, u1, v1};
        PreviewScene.Face f = scene.addQuad(corners, uvArr, atlas, atlasSize, atlasSize);
        if (f == null) return null;
        if (head) {
            n[0] = face.nx; n[1] = face.ny; n[2] = face.nz;
            HeadLookTransform.applyVector(headPitch, headYaw, n);
            f.nx = n[0]; f.ny = n[1]; f.nz = n[2];
        } else {
            f.nx = face.nx; f.ny = face.ny; f.nz = face.nz;
        }
        return f;
    }
}
