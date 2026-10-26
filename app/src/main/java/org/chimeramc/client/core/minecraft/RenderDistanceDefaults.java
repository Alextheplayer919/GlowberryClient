package org.chimeramc.client.core.minecraft;

import android.app.ActivityManager;
import android.content.Context;

/**
 * Defaults for the dynamic-render-distance governor.
 *
 * <p>These are the bounds the launcher pushes to the preloader; the hysteresis itself lives in the
 * preloader's {@code OptifineGovernor} so the step rule is unit-tested once.
 *
 * <p>The bounds are device-tier aware: a low-end phone never gets asked to load 12 chunks, which
 * would choke its CPU and memory bandwidth and tank in-game FPS. The tier is detected once and
 * cached.
 */
public final class RenderDistanceDefaults {

    /** High-end defaults: never step below this many chunks. */
    public static final int MIN_CHUNKS = 4;

    /** High-end defaults: start at this many chunks and never step above it. */
    public static final int MAX_CHUNKS = 12;

    /** High-end defaults: step down when the frame rate sits below this for the sustain window. */
    public static final int FPS_THRESHOLD = 45;

    /** Low-end tier caps. */
    public static final int LOW_END_MAX_CHUNKS = 6;
    public static final int LOW_END_MIN_CHUNKS = 3;
    public static final int LOW_END_FPS_THRESHOLD = 30;

    /** Mid-range tier caps. */
    public static final int MID_RANGE_MAX_CHUNKS = 8;
    public static final int MID_RANGE_MIN_CHUNKS = 4;
    public static final int MID_RANGE_FPS_THRESHOLD = 40;

    /** Fewer than this many CPU cores counts as low-end. */
    private static final int LOW_END_CORES = 4;

    /** Less than this many bytes of available RAM counts as low-end. */
    private static final long LOW_END_RAM_BYTES = 2L * 1024L * 1024L * 1024L;

    /** Between low-end and high-end core counts counts as mid-range. */
    private static final int MID_RANGE_CORES = 6;

    private static Integer cachedTier;

    private RenderDistanceDefaults() {
    }

    /**
     * Device tier: {@code 0} low-end, {@code 1} mid-range, {@code 2} high-end.
     * Detected from CPU core count and available RAM; cached for the life of the process.
     */
    public static int deviceTier(Context context) {
        if (cachedTier != null) {
            return cachedTier;
        }
        int cores = HardwareInformation.getNumCores();
        if (cores == 0) {
            // Fall back to the platform's own count when our parser has nothing.
            cores = Runtime.getRuntime().availableProcessors();
        }
        long availRam = availableRam(context);
        int tier;
        if (cores < LOW_END_CORES || availRam < LOW_END_RAM_BYTES) {
            tier = 0;
        } else if (cores < MID_RANGE_CORES || availRam < 3L * 1024L * 1024L * 1024L) {
            tier = 1;
        } else {
            tier = 2;
        }
        cachedTier = tier;
        return tier;
    }

    /** Minimum chunks for the given tier. */
    public static int minChunksForTier(int tier) {
        switch (tier) {
            case 0: return LOW_END_MIN_CHUNKS;
            case 1: return MID_RANGE_MIN_CHUNKS;
            case 2:
            default: return MIN_CHUNKS;
        }
    }

    /** Maximum chunks for the given tier. */
    public static int maxChunksForTier(int tier) {
        switch (tier) {
            case 0: return LOW_END_MAX_CHUNKS;
            case 1: return MID_RANGE_MAX_CHUNKS;
            case 2:
            default: return MAX_CHUNKS;
        }
    }

    /** FPS threshold for the given tier. */
    public static int fpsThresholdForTier(int tier) {
        switch (tier) {
            case 0: return LOW_END_FPS_THRESHOLD;
            case 1: return MID_RANGE_FPS_THRESHOLD;
            case 2:
            default: return FPS_THRESHOLD;
        }
    }

    private static long availableRam(Context context) {
        ActivityManager am =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return 0L;
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(info);
        return info.availMem;
    }
}
