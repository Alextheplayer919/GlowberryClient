package org.chimeramc.client.core.cosmetics;

/**
 * The pose/bone rotation vocabulary the preview shares between the character and the pet.
 *
 * <p>Both the head look and a pet's per-bone gait rotate a point about a pivot by X/Y/Z degrees in
 * the same Z-then-Y-then-X order the geometry builders use, so one helper keeps the preview and the
 * in-game geometry rotating identically. Pure and Android-free.
 */
public final class PreviewRotations {

    private PreviewRotations() {
    }

    /**
     * Rotates a model-space point about a pivot by the given X/Y/Z degrees, applied Z, then Y, then X.
     *
     * @return a new {x, y, z}; the input is not modified
     */
    public static float[] aboutPivot(float[] point, float[] pivot, float rotX, float rotY, float rotZ) {
        float lx = point[0] - pivot[0];
        float ly = point[1] - pivot[1];
        float lz = point[2] - pivot[2];
        float[] r = rotate(lx, ly, lz, rotX, rotY, rotZ);
        return new float[]{pivot[0] + r[0], pivot[1] + r[1], pivot[2] + r[2]};
    }

    /** Rotates a vector by X/Y/Z degrees, applied Z, then Y, then X. */
    public static float[] rotate(float x, float y, float z, float rotX, float rotY, float rotZ) {
        double rx = Math.toRadians(rotX);
        double ry = Math.toRadians(rotY);
        double rz = Math.toRadians(rotZ);

        // Z
        double cosZ = Math.cos(rz), sinZ = Math.sin(rz);
        double x1 = x * cosZ - y * sinZ;
        double y1 = x * sinZ + y * cosZ;
        double z1 = z;
        // Y
        double cosY = Math.cos(ry), sinY = Math.sin(ry);
        double x2 = x1 * cosY + z1 * sinY;
        double y2 = y1;
        double z2 = -x1 * sinY + z1 * cosY;
        // X
        double cosX = Math.cos(rx), sinX = Math.sin(rx);
        double x3 = x2;
        double y3 = y2 * cosX - z2 * sinX;
        double z3 = y2 * sinX + z2 * cosX;
        return new float[]{(float) x3, (float) y3, (float) z3};
    }

    /** The bone rotation, in degrees, the pose applies to a pet bone, or null when unanimated. */
    public static float[] petBoneRotation(PetPose pose, String bone) {
        if (pose == null || bone == null) return null;
        if (PetModel.BONE_HEAD.equals(bone)) {
            return new float[]{pose.headPitch, pose.headYaw, 0f};
        }
        if (PetModel.BONE_TAIL.equals(bone)) {
            return new float[]{0f, pose.tailWag * 16f, 0f};
        }
        if (PetModel.BONE_WING_L.equals(bone) || PetModel.BONE_WING_R.equals(bone)) {
            float dir = PetModel.BONE_WING_R.equals(bone) ? -1f : 1f;
            return new float[]{0f, 0f, pose.wingFlap * 55f * dir};
        }
        if (bone.startsWith("leg_")) {
            int idx = PetModel.BONE_LEG_A.equals(bone) ? 0
                    : PetModel.BONE_LEG_B.equals(bone) ? 1 : 2;
            float swing = pose.legSwing * (idx == 1 ? -1f : 1f);
            return new float[]{0f, 0f, swing * 30f};
        }
        return null;
    }
}
