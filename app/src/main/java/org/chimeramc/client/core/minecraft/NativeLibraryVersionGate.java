package org.chimeramc.client.core.minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Which optional native libraries a Minecraft build ships, decided by its version code.
 *
 * <p>This rule used to live in two places — {@code GamePackageManager} (which decides what the
 * extracted cache must contain) and {@code MinecraftRuntimePreparer} (which decides what the loader
 * tries to {@code dlopen}) — each with its own copy of the version comparison. That is a launch
 * failure waiting to happen: if the two disagree, the cache considers a build complete without
 * {@code libHttpClient.Android.so} while the loader then tries to open it, and the launch dies with
 * a "library not found" that names a file the launcher deliberately did not extract. Neither copy
 * was covered by a test, because both classes load a native library during class initialisation and
 * cannot be asserted against from a JVM test.
 *
 * <p>So the rule is here: pure, Android-free, and the single source both callers read. Changing a
 * threshold changes both at once.
 *
 * <p>Each threshold has a beta form. A beta build's version code carries the pre-release number
 * ({@code 1.21.130.20}) which sorts *below* the release it precedes ({@code 1.21.130} would compare
 * as {@code 130.0.0}, so {@code 130.20} is in fact above it) — the explicit beta target is what
 * keeps a beta's library layout tied to the beta, not to the release. The beta target is chosen per
 * feature because the builds that introduced them were different betas.
 */
public final class NativeLibraryVersionGate {

    private NativeLibraryVersionGate() {
    }

    /** The {@code libHttpClient.Android.so} layout arrived in 1.21.130 (beta 1.21.130.20). */
    public static final String HTTP_CLIENT_RELEASE = "1.21.130";
    public static final String HTTP_CLIENT_BETA = "1.21.130.20";

    /** The {@code libmaesdk.so} bundle layout arrived in 1.21.110 (beta 1.21.110.22). */
    public static final String MAESDK_RELEASE = "1.21.110";
    public static final String MAESDK_BETA = "1.21.110.22";

    /** {@code libPlayFabMultiplayer.so} is required from the same build as the HTTP client. */
    public static final String PLAY_FAB_RELEASE = "1.21.130";
    public static final String PLAY_FAB_BETA = "1.21.130.20";

    /** Whether the build ships and needs {@code libHttpClient.Android.so}. */
    public static boolean shouldLoadHttpClient(String versionCode) {
        return atLeast(versionCode, HTTP_CLIENT_RELEASE, HTTP_CLIENT_BETA);
    }

    /** Whether the build uses the bundled library layout that includes {@code libmaesdk.so}. */
    public static boolean shouldLoadMaesdk(String versionCode) {
        return atLeast(versionCode, MAESDK_RELEASE, MAESDK_BETA);
    }

    /** Whether the build needs {@code libPlayFabMultiplayer.so}. */
    public static boolean shouldLoadPlayFab(String versionCode) {
        return atLeast(versionCode, PLAY_FAB_RELEASE, PLAY_FAB_BETA);
    }

    /**
     * Every library the loader opens from the extracted cache because of a version gate, in load
     * order. The cache's completeness check and the loader both derive from this one list, so the
     * cache can never call itself complete while the loader is still going to open one of these.
     */
    public static List<String> gatedLibraries(String versionCode) {
        List<String> libs = new ArrayList<>(3);
        if (shouldLoadHttpClient(versionCode)) libs.add("libHttpClient.Android.so");
        if (shouldLoadMaesdk(versionCode)) libs.add("libmaesdk.so");
        if (shouldLoadPlayFab(versionCode)) libs.add("libPlayFabMultiplayer.so");
        return libs;
    }

    /** Compares against the release target, or the beta target when the code is a beta. */
    private static boolean atLeast(String versionCode, String releaseTarget, String betaTarget) {
        if (versionCode == null) return false;
        String target = isBeta(versionCode) ? betaTarget : releaseTarget;
        return isVersionAtLeast(versionCode, target);
    }

    /** A beta version code names its channel, e.g. {@code 1.21.130.20-beta}. */
    public static boolean isBeta(String versionCode) {
        return versionCode != null
                && versionCode.toLowerCase(java.util.Locale.ROOT).contains("beta");
    }

    /**
     * Component-wise "is {@code current} at least {@code target}".
     *
     * <p>Non-numeric characters are dropped from the current code (so {@code 1.21.130.20-beta}
     * compares as {@code 1.21.130.20}), a missing component reads as zero (so {@code 1.21.130}
     * equals {@code 1.21.130.0}), and an unparseable input is not "at least" anything — the
     * fail-closed answer for a gate that decides whether a library is required.
     */
    public static boolean isVersionAtLeast(String currentVersion, String targetVersion) {
        if (currentVersion == null || targetVersion == null) return false;
        try {
            String[] current = currentVersion.replaceAll("[^0-9.]", "").split("\\.");
            String[] target = targetVersion.split("\\.");
            int maxLength = Math.max(current.length, target.length);
            for (int i = 0; i < maxLength; i++) {
                int currentPart = part(current, i);
                int targetPart = part(target, i);
                if (currentPart > targetPart) return true;
                if (currentPart < targetPart) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static int part(String[] parts, int index) {
        if (index >= parts.length) return 0;
        try {
            return Integer.parseInt(parts[index].trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
