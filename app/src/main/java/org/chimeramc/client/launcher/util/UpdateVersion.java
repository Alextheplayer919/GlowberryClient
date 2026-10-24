package org.chimeramc.client.launcher.util;

import java.util.ArrayList;
import java.util.List;

/**
 * The rule that decides whether a published release is newer than the installed build.
 *
 * <p>The updater used to split a tag on {@code "."} and hand every part to
 * {@link Integer#parseInt(String)}, so a real release tag threw. This project tags its releases by
 * name ({@code OV.2}, {@code BETA5}) rather than by a dotted number, so {@code "BETA5".split(".")}
 * yields {@code ["BETA5"]} and {@code parseInt} raises {@code NumberFormatException}. The call site
 * caught it as a generic parse error and logged it, so the update check failed silently — the app
 * never told anyone a new version existed.
 *
 * <p>The comparison therefore reads the <em>numeric runs</em> out of each tag, the same way
 * {@code PreloaderRules.versionParts} reads a Bedrock version: {@code BETA5} is {@code [5]} and
 * {@code OV.2} is {@code [2]}, so {@code BETA5} is newer than {@code OV.2}, which matches the
 * publish order of those releases. A missing component counts as zero, so {@code BETA5} and
 * {@code 5.0} compare equal. It never throws: a tag with no digits at all is treated as older than
 * any tag that has one, so a stray name cannot hide a real update.
 *
 * <p>Pure and Android-free, so the ordering rule is unit-tested without a device.
 */
public final class UpdateVersion {

    private UpdateVersion() {}

    /** The numeric runs of a version string, with a leading {@code v} dropped. Never throws. */
    public static int[] parts(String value) {
        if (value == null) {
            return new int[0];
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("v") || trimmed.startsWith("V")) {
            trimmed = trimmed.substring(1);
        }
        List<Integer> found = new ArrayList<>();
        long current = 0;
        boolean inNumber = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (ch >= '0' && ch <= '9') {
                // Saturate rather than overflow: a pathological tag must not wrap into a negative
                // part and read as an older release.
                current = Math.min(current * 10 + (ch - '0'), Integer.MAX_VALUE);
                inNumber = true;
            } else if (inNumber) {
                found.add((int) current);
                current = 0;
                inNumber = false;
            }
        }
        if (inNumber) {
            found.add((int) current);
        }
        int[] out = new int[found.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = found.get(i);
        }
        return out;
    }

    /**
     * Compares two release tags.
     *
     * @return a positive value when {@code candidate} is newer than {@code installed}, a negative
     *     value when it is older, and zero when they name the same release.
     */
    public static int compare(String candidate, String installed) {
        int[] newer = parts(candidate);
        int[] current = parts(installed);
        if (newer.length == 0 && current.length == 0) {
            return 0;
        }
        // A tag with no digits names no release number; it cannot outrank one that has a number.
        if (newer.length == 0) {
            return -1;
        }
        if (current.length == 0) {
            return 1;
        }
        int length = Math.max(newer.length, current.length);
        for (int i = 0; i < length; i++) {
            int a = i < newer.length ? newer[i] : 0;
            int b = i < current.length ? current[i] : 0;
            if (a != b) {
                return a < b ? -1 : 1;
            }
        }
        return 0;
    }

    /** True when {@code candidate} names a release newer than {@code installed}. */
    public static boolean isNewer(String candidate, String installed) {
        return compare(candidate, installed) > 0;
    }
}
