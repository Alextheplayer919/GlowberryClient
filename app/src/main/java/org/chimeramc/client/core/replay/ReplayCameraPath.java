package org.chimeramc.client.core.replay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A cinematic camera path: keyframes and the presets that generate them.
 *
 * <p>The spec asks for camera keyframes, smooth paths and a set of shot presets (orbit, dolly, drone
 * and the rest). All of that is the same pure object: a list of timed keyframes sampled by the
 * timeline, plus generators that fill the list from a preset and the current view. Keeping it pure
 * means a shot can be authored, saved and unit-tested without a renderer, and the free camera and
 * the path share one pose representation.
 *
 * <p>Sampling is Catmull-Rom through the keyframes so a path is smooth rather than a chain of
 * straight hops; a path with fewer than two keyframes is a no-op. A camera cannot be asked to look
 * straight up, so pitch is clamped exactly as {@link ReplayCamera} clamps it.
 */
public final class ReplayCameraPath {

    /** The built-in shot presets. */
    public enum Preset {
        ORBIT,
        DOLLY_ZOOM,
        DRONE,
        CRANE,
        STATIC
    }

    /** One timed camera pose. */
    public static final class Keyframe {
        public final long timeMs;
        public final float x, y, z;
        public final float yawDeg, pitchDeg;

        public Keyframe(long timeMs, float x, float y, float z, float yawDeg, float pitchDeg) {
            this.timeMs = Math.max(0L, timeMs);
            this.x = x;
            this.y = y;
            this.z = z;
            this.yawDeg = yawDeg;
            this.pitchDeg = ReplayCamera.clampPitch(pitchDeg);
        }
    }

    private final List<Keyframe> keyframes = new ArrayList<>();
    private Preset preset = Preset.STATIC;

    public List<Keyframe> keyframes() {
        return Collections.unmodifiableList(keyframes);
    }

    public Preset preset() {
        return preset;
    }

    /** Records which preset produced this path (does not regenerate the keyframes). */
    public void setPreset(Preset preset) {
        if (preset != null) this.preset = preset;
    }

    public boolean isEmpty() {
        return keyframes.isEmpty();
    }

    public int size() {
        return keyframes.size();
    }

    public void clear() {
        keyframes.clear();
    }

    /** Appends a keyframe, keeping the list ordered by time. */
    public void addKeyframe(Keyframe keyframe) {
        if (keyframe == null) return;
        keyframes.add(keyframe);
        keyframes.sort((a, b) -> Long.compare(a.timeMs, b.timeMs));
    }

    /** Appends the camera's current pose at {@code timeMs}. */
    public void capture(ReplayCamera camera, long timeMs) {
        if (camera == null) return;
        addKeyframe(new Keyframe(timeMs, camera.x(), camera.y(), camera.z(),
                camera.yawDeg(), camera.pitchDeg()));
    }

    /**
     * Samples the path into {@code out}.
     *
     * @return true when a pose was produced; false for a path with no keyframes, in which case the
     *         camera is left untouched.
     */
    public boolean sample(long timeMs, ReplayCamera out) {
        if (out == null || keyframes.isEmpty()) return false;
        if (keyframes.size() == 1) {
            apply(out, keyframes.get(0));
            return true;
        }
        if (timeMs <= keyframes.get(0).timeMs) {
            apply(out, keyframes.get(0));
            return true;
        }
        Keyframe last = keyframes.get(keyframes.size() - 1);
        if (timeMs >= last.timeMs) {
            apply(out, last);
            return true;
        }
        int i = 0;
        while (i + 1 < keyframes.size() && keyframes.get(i + 1).timeMs <= timeMs) i++;
        Keyframe a = keyframes.get(i);
        Keyframe b = keyframes.get(i + 1);
        float span = Math.max(1f, b.timeMs - a.timeMs);
        float t = (timeMs - a.timeMs) / span;
        // Catmull-Rom needs the neighbours on either side; at an end the segment repeats itself,
        // which flattens the tangent rather than overshooting off the path.
        Keyframe p0 = keyframes.get(Math.max(0, i - 1));
        Keyframe p3 = keyframes.get(Math.min(keyframes.size() - 1, i + 2));
        out.set(catmull(p0.x, a.x, b.x, p3.x, t),
                catmull(p0.y, a.y, b.y, p3.y, t),
                catmull(p0.z, a.z, b.z, p3.z, t),
                catmull(p0.yawDeg, a.yawDeg, b.yawDeg, p3.yawDeg, t),
                catmull(p0.pitchDeg, a.pitchDeg, b.pitchDeg, p3.pitchDeg, t));
        return true;
    }

    /**
     * Generates a preset path around the camera's current view.
     *
     * @param durationMs the clip length the path should span
     */
    public void generate(Preset preset, ReplayCamera camera, long durationMs) {
        if (preset == null || camera == null) return;
        this.preset = preset;
        keyframes.clear();
        long duration = Math.max(1000L, durationMs);
        float cx = camera.x(), cy = camera.y(), cz = camera.z();
        switch (preset) {
            case ORBIT:
                orbit(camera, cx, cy, cz, duration, 24f);
                break;
            case DOLLY_ZOOM:
                dolly(camera, cx, cy, cz, duration);
                break;
            case DRONE:
                drone(camera, cx, cy, cz, duration);
                break;
            case CRANE:
                crane(camera, cx, cy, cz, duration);
                break;
            case STATIC:
            default:
                capture(camera, 0L);
                capture(camera, duration);
                break;
        }
    }

    /** A full circle at a fixed radius, always facing the subject. */
    private void orbit(ReplayCamera camera, float cx, float cy, float cz, long duration,
                       float radius) {
        int steps = 16;
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            double angle = Math.toRadians(t * 360.0);
            float x = cx + (float) (Math.sin(angle) * radius);
            float z = cz + (float) (Math.cos(angle) * radius);
            float yaw = (float) Math.toDegrees(angle) + 180f;
            addKeyframe(new Keyframe((long) (t * duration), x, cy + 2f, z, yaw, 8f));
        }
    }

    /** A slow push-in from behind the subject to just over its shoulder. */
    private void dolly(ReplayCamera camera, float cx, float cy, float cz, long duration) {
        float yaw = camera.yawDeg();
        double yawRad = Math.toRadians(yaw);
        float fx = (float) Math.sin(yawRad);
        float fz = (float) Math.cos(yawRad);
        addKeyframe(new Keyframe(0L, cx - fx * 8f, cy + 1.5f, cz - fz * 8f, yaw, 6f));
        addKeyframe(new Keyframe(duration, cx - fx * 2f, cy + 1f, cz - fz * 2f, yaw, 4f));
    }

    /** A high, gently descending aerial sweep. */
    private void drone(ReplayCamera camera, float cx, float cy, float cz, long duration) {
        float yaw = camera.yawDeg();
        double yawRad = Math.toRadians(yaw);
        float rx = (float) Math.cos(yawRad);
        float rz = (float) -Math.sin(yawRad);
        addKeyframe(new Keyframe(0L, cx - rx * 14f, cy + 20f, cz - rz * 14f, yaw, 42f));
        addKeyframe(new Keyframe(duration / 2, cx, cy + 16f, cz, yaw, 36f));
        addKeyframe(new Keyframe(duration, cx + rx * 14f, cy + 12f, cz + rz * 14f, yaw, 28f));
    }

    /** A vertical rise from ground level, looking down. */
    private void crane(ReplayCamera camera, float cx, float cy, float cz, long duration) {
        float yaw = camera.yawDeg();
        addKeyframe(new Keyframe(0L, cx, cy - 2f, cz, yaw, 4f));
        addKeyframe(new Keyframe(duration, cx, cy + 14f, cz, yaw, 34f));
    }

    private static void apply(ReplayCamera out, Keyframe k) {
        out.set(k.x, k.y, k.z, k.yawDeg, k.pitchDeg);
    }

    /** Uniform Catmull-Rom: the standard smooth interpolation through four control values. */
    static float catmull(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return 0.5f * ((2f * p1)
                + (-p0 + p2) * t
                + (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2
                + (-p0 + 3f * p1 - 3f * p2 + p3) * t3);
    }
}
