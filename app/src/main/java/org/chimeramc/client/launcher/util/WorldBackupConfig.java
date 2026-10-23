package org.chimeramc.client.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;

import java.util.List;

/**
 * Persistence and decision rules for the scheduled world backup feature (G13).
 *
 * <p>Holds the four knobs the feature has — enabled, interval (in whole hours), the SAF
 * destination tree, and how many backups per world to keep — and the pure derivation helpers
 * around them ({@link #nextRunAfter} and {@link #foldIntoIntervalHours}). Keeping these in one
 * place means the settings UI and the {@code WorldBackupService} read the same values and cannot
 * drift.
 */
public final class WorldBackupConfig {

    public static final String PREFS_NAME = "world_backup_schedule";
    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_INTERVAL_HOURS = "interval_hours";
    public static final String KEY_FOLDER_URI = "folder_uri";
    public static final String KEY_RETENTION = "retention_per_world";
    public static final String KEY_LAST_RUN_AT = "last_run_at";
    public static final String KEY_LAST_RESULT = "last_result";

    public static final int DEFAULT_INTERVAL_HOURS = 24;
    public static final int DEFAULT_RETENTION = 5;
    public static final int MIN_INTERVAL_HOURS = 1;
    public static final int MAX_INTERVAL_HOURS = 24 * 7;
    public static final int MIN_RETENTION = 1;
    public static final int MAX_RETENTION = 50;

    private final Context context;

    public WorldBackupConfig(Context context) {
        this.context = context.getApplicationContext();
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isEnabled() {
        return prefs().getBoolean(KEY_ENABLED, false);
    }

    public void setEnabled(boolean enabled) {
        prefs().edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public int getIntervalHours() {
        return prefs().getInt(KEY_INTERVAL_HOURS, DEFAULT_INTERVAL_HOURS);
    }

    public void setIntervalHours(int hours) {
        prefs().edit().putInt(KEY_INTERVAL_HOURS, foldIntoIntervalHours(hours)).apply();
    }

    public int getRetention() {
        return prefs().getInt(KEY_RETENTION, DEFAULT_RETENTION);
    }

    public void setRetention(int count) {
        prefs().edit().putInt(KEY_RETENTION, Math.max(MIN_RETENTION, Math.min(MAX_RETENTION, count))).apply();
    }

    public Uri getFolderUri() {
        String raw = prefs().getString(KEY_FOLDER_URI, null);
        if (raw == null) return null;
        try {
            return Uri.parse(raw);
        } catch (Exception e) {
            return null;
        }
    }

    public void setFolderUri(Uri uri) {
        prefs().edit().putString(KEY_FOLDER_URI, uri == null ? null : uri.toString()).apply();
    }

    public long getLastRunAt() {
        return prefs().getLong(KEY_LAST_RUN_AT, 0L);
    }

    public void setLastRunAt(long timestamp) {
        prefs().edit().putLong(KEY_LAST_RUN_AT, timestamp).apply();
    }

    public String getLastResult() {
        return prefs().getString(KEY_LAST_RESULT, "");
    }

    public void setLastResult(String result) {
        prefs().edit().putString(KEY_LAST_RESULT, result).apply();
    }

    /** True when a destination has been picked; the folder must also be a persistable tree URI. */
    public boolean hasDestination() {
        return getFolderUri() != null && isTreeUri(getFolderUri());
    }

    /** The saved destination is only usable while the OS still grants read+write on it. */
    public boolean hasDestinationPermission() {
        if (!hasDestination()) return false;
        Uri uri = getFolderUri();
        List<UriPermission> permissions = context.getContentResolver().getPersistedUriPermissions();
        for (UriPermission permission : permissions) {
            if (permission.isReadPermission() && permission.isWritePermission()
                    && uri.toString().equals(permission.getUri().toString())) {
                return true;
            }
        }
        return false;
    }

    /** RFC 3986 opaque tree check: {@code content://.../tree/<id>} (optionally {@code /document/...}). */
    static boolean isTreeUri(Uri uri) {
        return uri != null && isTreePath(uri.getPath());
    }

    /** Pure path marker check so the tree detection is unit-testable without Android. */
    static boolean isTreePath(String path) {
        return path != null && path.contains("/tree/");
    }

    /** Pure schedule math used by both the UI (status preview) and the alarm scheduler. */
    public static long nextRunAfter(long lastRunAt, int intervalHours) {
        int clamped = foldIntoIntervalHours(intervalHours);
        if (lastRunAt <= 0L) {
            return 0L;
        }
        long intervalMillis = clamped * 3600_000L;
        return lastRunAt + intervalMillis;
    }

    /** Clamps a user-entered interval (in hours) into the valid range. */
    static int foldIntoIntervalHours(int hours) {
        return Math.max(MIN_INTERVAL_HOURS, Math.min(MAX_INTERVAL_HOURS, hours));
    }

    /** True when the configured schedule is due at or before ({@code now}). */
    public boolean isDue(long now) {
        long lastRunAt = getLastRunAt();
        if (lastRunAt <= 0L) return true;
        return now >= nextRunAfter(lastRunAt, getIntervalHours());
    }
}