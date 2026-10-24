package org.chimeramc.client.util;

import java.util.Locale;

/**
 * Pure, unit-testable policy behind the storage migration interruption handling (G15).
 *
 * <p>A long migration runs in a foreground service with a wake lock, which keeps the hosting
 * process alive; when that process dies and starts fresh, the persisted {@code running} flag is
 * guaranteed stale — the previous run could not persist its own completion. From the persisted
 * state alone there is no way to tell a live run from a dead one, so the process-start reconcile
 * is the only hook that can see the truth. This class owns that policy and the user messaging so
 * they can be pinned by a plain JVM test without a Context.
 */
public final class StorageMigrationRunBook {

    public enum RunProbe {
        /** Nothing persisted: never started, or already finished. */
        NOT_RUNNING,
        /** The running flag is set and we are inside the original process. */
        LIVE,
        /** The running flag is set but this is a fresh process: the run was interrupted. */
        INTERRUPTED
    }

    private StorageMigrationRunBook() {
    }

    /**
     * Classifies a persisted run state against the calling context.
     *
     * @param runningFlag  the persisted {@code storage_migration_running} primitive
     * @param freshProcess true when invoked from {@code Application.onCreate} — a brand-new
     *                     process, where the previous foreground service cannot be alive
     * @return {@link RunProbe#INTERRUPTED} when a running flag is seen from a fresh process,
     *         {@link RunProbe#LIVE} for a running flag seen in the live process,
     *         {@link RunProbe#NOT_RUNNING} otherwise
     */
    public static RunProbe classifyRun(boolean runningFlag, boolean freshProcess) {
        if (!runningFlag) return RunProbe.NOT_RUNNING;
        return freshProcess ? RunProbe.INTERRUPTED : RunProbe.LIVE;
    }

    /**
     * Human-readable message for an interrupted migration, telling the user how far it got and
     * that re-running is safe because completed files are skipped the next pass.
     *
     * @param processedFiles files copied before the interruption
     * @param totalFiles     files the run had scanned
     * @param totalBytes     bytes the run had scanned
     * @return a localized-agnostic message fragment (the caller wraps it in a string resource)
     */
    public static String interruptedMessage(int processedFiles, int totalFiles, long totalBytes) {
        int clampedTotal = Math.max(0, totalFiles);
        int clampedProcessed = Math.max(0, Math.min(processedFiles, clampedTotal));
        String files;
        if (clampedTotal <= 0) {
            files = "0 of 0 files had been copied before interruption.";
        } else if (clampedProcessed >= clampedTotal) {
            files = "All scanned files were copied before interruption.";
        } else {
            files = String.format(Locale.US,
                    "%1$d of %2$d files had been copied before interruption.", clampedProcessed, clampedTotal);
        }
        String bytes = totalBytes < 0L ? "" : " ~" + humanBytes(totalBytes) + " of data";
        return files + bytes + " Re-running is safe — files that already finished are skipped.";
    }

    private static String humanBytes(long bytes) {
        if (bytes < 1024L) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024.0) return String.format(Locale.US, "%.1f MB", mb);
        return String.format(Locale.US, "%.1f GB", mb / 1024.0);
    }
}