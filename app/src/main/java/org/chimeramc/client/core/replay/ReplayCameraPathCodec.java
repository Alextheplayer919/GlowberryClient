package org.chimeramc.client.core.replay;

/**
 * Encodes a {@link ReplayCameraPath} to a single sidecar value and back.
 *
 * <p>A camera path is a small list of timed poses, so it is stored as one compact, human-readable
 * line rather than a nested JSON document the sidecar has no parser for. The format is
 * {@code preset;time,x,y,z,yaw,pitch;time,x,y,z,yaw,pitch;...}, and the rules are strict enough that
 * a corrupt line degrades to "no path" rather than a half-parsed shot: a segment with the wrong
 * field count, a non-numeric field or an unknown preset is skipped, and an empty result is an empty
 * path.
 *
 * <p>Pure and Android-free, so the round-trip is unit-tested.
 */
public final class ReplayCameraPathCodec {

    private ReplayCameraPathCodec() {
    }

    /** Serialises a path; a null or empty path yields an empty string. */
    public static String serialize(ReplayCameraPath path) {
        if (path == null || path.isEmpty()) return "";
        StringBuilder builder = new StringBuilder(path.preset().name());
        for (ReplayCameraPath.Keyframe k : path.keyframes()) {
            builder.append(';').append(k.timeMs).append(',').append(fmt(k.x)).append(',')
                    .append(fmt(k.y)).append(',').append(fmt(k.z)).append(',')
                    .append(fmt(k.yawDeg)).append(',').append(fmt(k.pitchDeg));
        }
        return builder.toString();
    }

    /** Parses a stored path; malformed input yields an empty path, never a partial throw. */
    public static ReplayCameraPath parse(String encoded) {
        ReplayCameraPath path = new ReplayCameraPath();
        if (encoded == null || encoded.isEmpty()) return path;
        String[] segments = encoded.split(";");
        if (segments.length == 0) return path;
        ReplayCameraPath.Preset preset = parsePreset(segments[0]);
        for (int i = 1; i < segments.length; i++) {
            String[] fields = segments[i].split(",");
            if (fields.length != 6) continue;
            try {
                path.addKeyframe(new ReplayCameraPath.Keyframe(
                        Long.parseLong(fields[0].trim()),
                        Float.parseFloat(fields[1].trim()),
                        Float.parseFloat(fields[2].trim()),
                        Float.parseFloat(fields[3].trim()),
                        Float.parseFloat(fields[4].trim()),
                        Float.parseFloat(fields[5].trim())));
            } catch (NumberFormatException ignored) {
                // Skip a malformed keyframe rather than discarding the whole path.
            }
        }
        path.setPreset(preset);
        return path;
    }

    private static ReplayCameraPath.Preset parsePreset(String name) {
        if (name == null) return ReplayCameraPath.Preset.STATIC;
        try {
            return ReplayCameraPath.Preset.valueOf(name.trim());
        } catch (IllegalArgumentException e) {
            return ReplayCameraPath.Preset.STATIC;
        }
    }

    private static String fmt(float value) {
        if (value == (long) value) return String.valueOf((long) value);
        return String.valueOf(value);
    }
}
