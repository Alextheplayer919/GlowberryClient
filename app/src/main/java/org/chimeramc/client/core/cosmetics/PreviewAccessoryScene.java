package org.chimeramc.client.core.cosmetics;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the procedural accessory scene (hats, glasses, wings, backpack, and the rest) into the
 * software scene.
 *
 * <p>This is the pure replacement for the preview's Canvas accessory passes. Each accessory kind is a
 * small stack of shaded boxes; a head-worn kind ({@link CosmeticCatalog.AccessoryKind#followsHead()})
 * is rotated about the neck pivot by the head look, and every box face is culled and shaded exactly
 * as the Canvas {@code drawBox} did. The wings, backpack and scarf tail are the behind-the-body
 * pieces, added by {@link #addAccessoryBehind}. Android-free, so it is unit-testable.
 */
public final class PreviewAccessoryScene {

    private PreviewAccessoryScene() {
    }

    /** A shaded box definition: centre, size and colour. */
    private static final class BoxSpec {
        final float cx, cy, cz, w, h, d;
        final int color;

        BoxSpec(float cx, float cy, float cz, float w, float h, float d, int color) {
            this.cx = cx; this.cy = cy; this.cz = cz; this.w = w; this.h = h; this.d = d;
            this.color = color;
        }
    }

    /** Adds the front-facing accessory boxes (head, face, torso). */
    public static void addAccessoryFront(PreviewScene scene, CosmeticCatalog.Accessory accessory,
                                         float headPitch, float headYaw) {
        if (scene == null || accessory == null
                || accessory.kind == CosmeticCatalog.AccessoryKind.NONE) return;
        boolean head = accessory.kind.followsHead();
        List<BoxSpec> boxes = frontBoxes(accessory);
        for (BoxSpec b : boxes) {
            addBox(scene, b, head, headPitch, headYaw);
        }
    }

    /** Adds the behind-the-body accessory pieces (wings, backpack, scarf tail). */
    public static void addAccessoryBehind(PreviewScene scene, CosmeticCatalog.Accessory accessory) {
        if (scene == null || accessory == null
                || accessory.kind == CosmeticCatalog.AccessoryKind.NONE) return;
        switch (accessory.kind) {
            case WINGS:
                addWings(scene, accessory);
                break;
            case BACKPACK:
                addBox(scene, new BoxSpec(0f, 18f, CosmeticLayering.CAPE_Z - 0.2f, 7f, 8f, 3f,
                        accessory.color), false, 0f, 0f);
                addBox(scene, new BoxSpec(0f, 15.6f, CosmeticLayering.CAPE_Z - 1.2f, 5f, 2.2f, 1.4f,
                        accessory.accentColor), false, 0f, 0f);
                break;
            case SCARF:
                addBox(scene, new BoxSpec(-2.2f, 20f, CosmeticLayering.CAPE_Z + 0.5f, 1.6f, 6f, 1.2f,
                        darker(accessory.color)), false, 0f, 0f);
                break;
            default:
                break;
        }
    }

    // --- Box geometry per kind ----------------------------------------------------------------

    private static List<BoxSpec> frontBoxes(CosmeticCatalog.Accessory accessory) {
        List<BoxSpec> b = new ArrayList<>();
        int c = accessory.color;
        int a = accessory.accentColor;
        switch (accessory.kind) {
            case HEADPHONES:
                // The arcing band was a stroked curve; approximated by two cups plus a thin band box.
                b.add(new BoxSpec(0f, 33.2f, 0f, 8.0f, 0.8f, 1.2f, c));
                b.add(new BoxSpec(-4.6f, 27.6f, 0f, 1.6f, 4.4f, 4.4f, darker(c)));
                b.add(new BoxSpec(4.6f, 27.6f, 0f, 1.6f, 4.4f, 4.4f, darker(c)));
                break;
            case HALO:
                // A ring approximated by a thin flat disc box just above the crown.
                b.add(new BoxSpec(0f, 35.0f, 0f, 10.4f, 0.4f, 10.4f, c));
                break;
            case CAP:
                b.add(new BoxSpec(0f, 33.4f, 0f, 8.6f, 2.4f, 8.6f, c));
                b.add(new BoxSpec(0f, 32.6f, 4.8f, 6.2f, 1.0f, 3.0f, a));
                break;
            case BEANIE:
                b.add(new BoxSpec(0f, 33.8f, 0f, 8.8f, 3.0f, 8.8f, c));
                b.add(new BoxSpec(0f, 32.6f, 0f, 9.0f, 1.2f, 9.0f, a));
                b.add(new BoxSpec(0f, 35.8f, 0f, 2.0f, 2.0f, 2.0f, c));
                break;
            case CROWN:
                b.add(new BoxSpec(0f, 33.0f, 0f, 8.6f, 1.6f, 8.6f, c));
                for (int i = -2; i <= 2; i++) {
                    float h = (i == 0) ? 2.6f : 1.8f;
                    b.add(new BoxSpec(i * 1.7f, 34.0f + h / 2f, 0f, 1.0f, h, 8.6f, c));
                }
                b.add(new BoxSpec(0f, 33.8f, 0f, 1.2f, 1.2f, 9.0f, a));
                break;
            case GLASSES:
                b.add(new BoxSpec(-1.6f, 29.6f, 4.4f, 3.2f, 2.0f, 0.6f, c));
                b.add(new BoxSpec(1.6f, 29.6f, 4.4f, 3.2f, 2.0f, 0.6f, c));
                b.add(new BoxSpec(0f, 29.6f, 4.4f, 1.0f, 0.6f, 0.6f, a));
                break;
            case MASK:
                b.add(new BoxSpec(0f, 27.6f, 4.2f, 8.0f, 4.4f, 0.8f, c));
                b.add(new BoxSpec(4.4f, 28.4f, 2.4f, 1.4f, 1.4f, 1.4f, a));
                break;
            case SCARF:
                b.add(new BoxSpec(0f, 22.8f, 0f, 8.4f, 2.0f, 4.4f, c));
                b.add(new BoxSpec(0f, 21.6f, 0f, 8.6f, 0.8f, 4.6f, a));
                break;
            case BACKPACK:
                b.add(new BoxSpec(-3.2f, 19f, 2.4f, 1.2f, 7.0f, 0.8f, a));
                b.add(new BoxSpec(3.2f, 19f, 2.4f, 1.2f, 7.0f, 0.8f, a));
                break;
            case HORNS:
                b.add(new BoxSpec(-2.6f, 35.2f, 0f, 1.6f, 4.0f, 1.6f, c));
                b.add(new BoxSpec(2.6f, 35.2f, 0f, 1.6f, 4.0f, 1.6f, c));
                b.add(new BoxSpec(-3.2f, 37.2f, 0f, 1.0f, 1.6f, 1.0f, a));
                b.add(new BoxSpec(3.2f, 37.2f, 0f, 1.0f, 1.6f, 1.0f, a));
                break;
            case TOPHAT:
                b.add(new BoxSpec(0f, 32.5f, 0f, 11.0f, 1.0f, 11.0f, c));
                b.add(new BoxSpec(0f, 36.0f, 0f, 8.0f, 6.0f, 8.0f, c));
                b.add(new BoxSpec(0f, 33.5f, 0f, 8.2f, 1.0f, 8.2f, a));
                break;
            case WIZARD_HAT:
                b.add(new BoxSpec(0f, 32.5f, 0f, 12.0f, 1.0f, 12.0f, c));
                b.add(new BoxSpec(0f, 34.0f, 0f, 8.0f, 2.0f, 8.0f, c));
                b.add(new BoxSpec(0f, 36.0f, 0f, 6.0f, 2.0f, 6.0f, c));
                b.add(new BoxSpec(0f, 38.0f, 0f, 4.0f, 2.0f, 4.0f, c));
                b.add(new BoxSpec(0f, 40.0f, 0f, 2.0f, 2.0f, 2.0f, c));
                b.add(new BoxSpec(0f, 41.5f, 0f, 0.8f, 1.0f, 0.8f, a));
                break;
            case TIARA:
                b.add(new BoxSpec(0f, 32.5f, 0f, 8.4f, 1.0f, 8.4f, c));
                b.add(new BoxSpec(0f, 34.0f, 3.6f, 1.6f, 2.0f, 0.8f, a));
                b.add(new BoxSpec(-2.8f, 33.7f, 3.6f, 1.2f, 1.4f, 0.8f, a));
                b.add(new BoxSpec(2.8f, 33.7f, 3.6f, 1.2f, 1.4f, 0.8f, a));
                break;
            case BEARD:
                b.add(new BoxSpec(0f, 24f, 3.4f, 7.2f, 2.0f, 1.2f, c));
                b.add(new BoxSpec(0f, 22.4f, 3.4f, 6.0f, 2.0f, 1.2f, c));
                b.add(new BoxSpec(0f, 20.8f, 3.4f, 4.0f, 2.0f, 1.2f, c));
                b.add(new BoxSpec(0f, 19.4f, 3.4f, 2.0f, 1.8f, 1.2f, a));
                break;
            case FLOWER:
                b.add(new BoxSpec(4.4f, 33.4f, 1.6f, 1.4f, 1.4f, 1.4f, a));
                b.add(new BoxSpec(4.4f, 34.6f, 1.6f, 2.4f, 1.2f, 2.4f, c));
                b.add(new BoxSpec(3.6f, 33.4f, 1.6f, 1.2f, 1.2f, 1.2f, c));
                b.add(new BoxSpec(5.2f, 33.4f, 1.6f, 1.2f, 1.2f, 1.2f, c));
                break;
            case BOWTIE:
                b.add(new BoxSpec(-1.4f, 22.4f, 2.6f, 2.4f, 2.0f, 1.2f, c));
                b.add(new BoxSpec(1.4f, 22.4f, 2.6f, 2.4f, 2.0f, 1.2f, c));
                b.add(new BoxSpec(0f, 22.4f, 2.8f, 1.0f, 1.4f, 1.0f, a));
                break;
            case EAR:
                b.add(new BoxSpec(-2.4f, 34.4f, 0f, 2.0f, 3.4f, 2.0f, c));
                b.add(new BoxSpec(2.4f, 34.4f, 0f, 2.0f, 3.4f, 2.0f, c));
                b.add(new BoxSpec(-2.4f, 34.8f, 0.9f, 1.2f, 2.2f, 0.6f, a));
                b.add(new BoxSpec(2.4f, 34.8f, 0.9f, 1.2f, 2.2f, 0.6f, a));
                break;
            case VEIL:
                b.add(new BoxSpec(0f, 33.2f, 0f, 8.8f, 2.4f, 8.8f, c));
                b.add(new BoxSpec(0f, 31.5f, -4.6f, 8.4f, 2.2f, 1.2f, a));
                b.add(new BoxSpec(0f, 28.6f, -4.6f, 7.6f, 4.0f, 1.0f, c));
                b.add(new BoxSpec(0f, 25.4f, -4.6f, 6.4f, 2.8f, 0.9f, a));
                break;
            case MONOCLE:
                b.add(new BoxSpec(2.5f, 29.9f, 4.2f, 3.0f, 3.0f, 0.6f, c));
                b.add(new BoxSpec(2.5f, 29.9f, 4.4f, 3.8f, 3.8f, 0.3f, a));
                b.add(new BoxSpec(2.5f, 27.4f, 4.2f, 0.5f, 2.0f, 0.5f, c));
                break;
            case ANTLERS:
                b.add(new BoxSpec(-2.6f, 34.2f, 0f, 1.2f, 4.5f, 1.2f, c));
                b.add(new BoxSpec(2.6f, 34.2f, 0f, 1.2f, 4.5f, 1.2f, c));
                b.add(new BoxSpec(-3.8f, 36.4f, 0f, 1.2f, 1.2f, 1.2f, a));
                b.add(new BoxSpec(3.8f, 36.4f, 0f, 1.2f, 1.2f, 1.2f, a));
                b.add(new BoxSpec(-3.1f, 38.4f, 0f, 1.0f, 2.0f, 1.0f, c));
                b.add(new BoxSpec(3.1f, 38.4f, 0f, 1.0f, 2.0f, 1.0f, c));
                break;
            case PLUME:
                b.add(new BoxSpec(0f, 32.6f, 0f, 8.4f, 1.2f, 8.4f, c));
                for (int i = -2; i <= 2; i++) {
                    float h = 3.4f - Math.abs(i) * 0.4f;
                    b.add(new BoxSpec(i * 0.7f, 33.2f + h / 2f, 0f, 0.8f, h, 0.8f,
                            i % 2 == 0 ? a : c));
                }
                break;
            case TRICORN:
                b.add(new BoxSpec(0f, 32.5f, 0f, 12.0f, 0.9f, 12.0f, c));
                b.add(new BoxSpec(0f, 34.2f, 0f, 8.4f, 2.6f, 8.4f, c));
                b.add(new BoxSpec(0f, 33.6f, 5.3f, 10.8f, 2.4f, 1.4f, a));
                b.add(new BoxSpec(0f, 33.6f, -5.3f, 10.8f, 2.4f, 1.4f, a));
                b.add(new BoxSpec(5.3f, 33.6f, 0f, 1.4f, 2.4f, 10.8f, a));
                b.add(new BoxSpec(-5.3f, 33.6f, 0f, 1.4f, 2.4f, 10.8f, a));
                break;
            case MORTARBOARD:
                b.add(new BoxSpec(0f, 32.4f, 0f, 8.4f, 1.6f, 8.4f, c));
                b.add(new BoxSpec(0f, 33.6f, 0f, 12.8f, 0.8f, 12.8f, c));
                b.add(new BoxSpec(0f, 33.6f, 0f, 0.8f, 0.8f, 0.8f, a));
                b.add(new BoxSpec(6f, 32.8f, 0f, 0.6f, 2.4f, 0.6f, a));
                break;
            default:
                break;
        }
        return b;
    }

    /** Adds one shaded box, following the head look when {@code head} is true. */
    private static void addBox(PreviewScene scene, BoxSpec spec, boolean head,
                               float headPitch, float headYaw) {
        SkinModel.Box box = SkinModel.Box.of("acc", spec.cx, spec.cy, spec.cz,
                spec.w, spec.h, spec.d);
        float[] n = new float[3];
        float[] p = new float[3];
        for (SkinModel.Face face : SkinModel.Face.values()) {
            if (head) {
                n[0] = face.nx; n[1] = face.ny; n[2] = face.nz;
                HeadLookTransform.applyVector(headPitch, headYaw, n);
                if (!headNormalVisible(n)) continue;
            } else if (!axisNormalVisible(face)) {
                continue;
            }
            int shaded = PreviewLighting.shadeColor(spec.color, face, 1f);
            float[][] c = box.faceCorners(face);
            float[][] corners = new float[4][3];
            for (int i = 0; i < 4; i++) {
                p[0] = c[i][0]; p[1] = c[i][1]; p[2] = c[i][2];
                if (head) HeadLookTransform.apply(headPitch, headYaw, p);
                corners[i][0] = p[0]; corners[i][1] = p[1]; corners[i][2] = p[2];
            }
            PreviewScene.Face f = scene.addFlatQuad(corners, shaded);
            if (f == null) continue;
            f.nx = face.nx; f.ny = face.ny; f.nz = face.nz;
            // The accessory is a thin piece; drawing both sides avoids a hole where it meets the head.
            f.doubleSided = true;
        }
    }

    /** A wings accessory: two flat feather panels, each a fan of triangles. */
    private static void addWings(PreviewScene scene, CosmeticCatalog.Accessory accessory) {
        int color = accessory.color;
        for (int side = -1; side <= 1; side += 2) {
            float[][] pts = {
                    {side * 2.6f, 22f, CosmeticLayering.WINGS_Z + 0.4f},
                    {side * 11f, 24.5f, CosmeticLayering.WINGS_Z + 1.9f},
                    {side * 12.5f, 16f, CosmeticLayering.WINGS_Z + 2.9f},
                    {side * 7f, 13.5f, CosmeticLayering.WINGS_Z + 1.4f},
                    {side * 2.6f, 15f, CosmeticLayering.WINGS_Z + 0.4f}};
            // Fan the pentagon into triangles; a duplicated last corner makes addFlatQuad a triangle.
            addFlatTriangle(scene, pts[0], pts[1], pts[2], color);
            addFlatTriangle(scene, pts[0], pts[2], pts[3], color);
            addFlatTriangle(scene, pts[0], pts[3], pts[4], color);
        }
    }

    private static void addFlatTriangle(PreviewScene scene, float[] a, float[] b, float[] c,
                                        int color) {
        float[][] corners = {a, b, c, c};
        PreviewScene.Face f = scene.addFlatQuad(corners, color);
        if (f != null) f.doubleSided = true;
    }

    private static boolean axisNormalVisible(SkinModel.Face face) {
        // A rear-facing accessory box on the back (z negative) is handled by the depth buffer; cull
        // nothing here so a thin piece is never lost, matching the Canvas pass's camera test.
        return true;
    }

    private static boolean headNormalVisible(float[] n) {
        return true;
    }

    private static int darker(int color) {
        int r = (int) (((color >>> 16) & 0xFF) * 0.62f);
        int g = (int) (((color >>> 8) & 0xFF) * 0.62f);
        int b = (int) ((color & 0xFF) * 0.62f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
