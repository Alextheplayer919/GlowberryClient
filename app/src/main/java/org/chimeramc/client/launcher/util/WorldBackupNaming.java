package org.chimeramc.client.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Pure filename rules for scheduled world backups.
 *
 * <p>The backup file name encodes the world's display name and the moment the run happened:
 * {@code <name>_<yyyyMMdd_HHmmss>.mcworld}. The timestamp occupies exactly the last 15
 * characters before the extension and is used for both sorting (retention) and display, so the
 * same format must be shared by the writer and the retention pruner. All methods here are pure
 * and Android-free so the format is pinned by unit tests.
 */
public final class WorldBackupNaming {

    public static final String EXTENSION = ".mcworld";
    public static final int TIMESTAMP_DIGITS = 15; // yyyyMMdd_HHmmss

    private static final String TIMESTAMP_PATTERN = "yyyyMMdd_HHmmss";

    private WorldBackupNaming() {}

    public static SimpleDateFormat timestampFormat() {
        return new SimpleDateFormat(TIMESTAMP_PATTERN, Locale.US);
    }

    /** Builds the file name for a world given a run timestamp. */
    public static String forWorld(String worldName, long timestampMs) {
        String safe = sanitizeWorldName(worldName);
        String stamp = timestampFormat().format(new Date(timestampMs));
        return safe + "_" + stamp + EXTENSION;
    }

    /**
     * Extracts the run timestamp (epoch millis, local timezone) from a backup file name.
     *
     * @return the parsed time, or {@code -1} when the name does not carry the expected suffix.
     */
    public static long timestampOf(String fileName) {
        if (fileName == null || !fileName.endsWith(EXTENSION)) return -1L;
        String base = fileName.substring(0, fileName.length() - EXTENSION.length());
        if (base.length() < TIMESTAMP_DIGITS + 1) return -1L;
        String stamp = base.substring(base.length() - TIMESTAMP_DIGITS);
        try {
            return timestampFormat().parse(stamp).getTime();
        } catch (Exception e) {
            return -1L;
        }
    }

    /**
     * The world key a backup file belongs to — the name with the timestamp suffix stripped.
     * Used so retention compares backups of the same world.
     */
    public static String worldKeyOf(String fileName) {
        if (fileName == null || !fileName.endsWith(EXTENSION)) return "";
        String base = fileName.substring(0, fileName.length() - EXTENSION.length());
        if (base.length() < TIMESTAMP_DIGITS + 1) return "";
        return base.substring(0, base.length() - TIMESTAMP_DIGITS - 1);
    }

    /** World names become file-name segments: path separators and control characters are stripped. */
    static String sanitizeWorldName(String worldName) {
        if (worldName == null || worldName.isEmpty()) return "world";
        StringBuilder sb = new StringBuilder(worldName.length());
        for (int i = 0; i < worldName.length(); i++) {
            char c = worldName.charAt(i);
            if (Character.isISOControl(c) || c == '/' || c == '\\' || c == ':' || c == '*'
                    || c == '?' || c == '"' || c == '<' || c == '>' || c == '|') {
                sb.append('_');
            } else {
                sb.append(c);
            }
        }
        String result = sb.toString().trim();
        if (result.isEmpty()) return "world";
        return result.length() > 80 ? result.substring(0, 80) : result;
    }
}