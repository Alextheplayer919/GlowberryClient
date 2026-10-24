package org.chimeramc.client.core.crash;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The rule that decides which crash reports still need uploading, and which have already gone.
 *
 * <p>Pure and Android-free so the rule is unit-tested without a device. It exists because the
 * upload path must never re-send the same tombstone on every launch: a report that is uploaded
 * repeatedly both wastes the user's data and fills the report volume with duplicates, and the only
 * stable identity a crash file has is its name plus its length.
 *
 * <p>Both halves of the identity are required. A name alone collides when xCrash reuses a file name
 * for a later crash, and a length alone collides between two different crashes that happen to
 * produce the same number of bytes. Together they distinguish a genuinely new report from a repeat.
 */
public final class CrashUploadPolicy {

    private CrashUploadPolicy() {
    }

    /** The largest report sent in one request, so a tombstone carrying a heap dump is skipped. */
    public static final long MAX_UPLOAD_BYTES = 2L * 1024L * 1024L;

    /** A stable identity for a crash file: its name and its exact size. */
    public static String identity(File file) {
        if (file == null) return "";
        return file.getName() + ":" + file.length();
    }

    /**
     * True when this file is a candidate to upload.
     *
     * <p>A missing, empty or oversized file is refused: there is nothing to send, and sending a
     * multi-megabyte tombstone over a mobile connection to report one stack trace is not a trade
     * the user agreed to.
     */
    public static boolean isUploadable(File file) {
        if (file == null || !file.isFile()) return false;
        long length = file.length();
        return length > 0 && length <= MAX_UPLOAD_BYTES;
    }

    /**
     * The files that still need uploading, given the identities already sent.
     *
     * <p>Ordered newest-first so the most recent crash — the one the user is actually asking about
     * — is sent first when a batch is capped.
     */
    public static Set<File> pending(Collection<File> candidates, Collection<String> alreadySent) {
        Set<String> sent = alreadySent == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(alreadySent);
        List<File> ordered = new ArrayList<>();
        if (candidates != null) {
            for (File file : candidates) {
                if (file != null && file.isFile()) ordered.add(file);
            }
        }
        ordered.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));

        Set<File> result = new LinkedHashSet<>();
        for (File file : ordered) {
            if (!isUploadable(file)) continue;
            if (sent.contains(identity(file))) continue;
            result.add(file);
        }
        return result;
    }
}
