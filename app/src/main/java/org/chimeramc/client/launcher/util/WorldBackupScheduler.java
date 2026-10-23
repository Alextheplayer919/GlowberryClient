package org.chimeramc.client.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * AlarmManager driver for the scheduled world backups (G13).
 *
 * <p>Backups are time-based, not exact-time, so {@code AlarmManager} (not WorkManager — there is
 * no WorkManager dependency in this project) with {@code RTC_WAKEUP} + {@code setInexactRepeating}
 * is the right primitive: a few minutes of jitter is irrelevant for a backup window and inexact
 * alarms respect Doze. Fires {@link WorldBackupService} as a foreground service so the OS cannot
 * reclaim the run mid-copy.
 */
public final class WorldBackupScheduler {

    private static final int REQUEST_CODE = 0xA1B1;

    private WorldBackupScheduler() {}

    /**
     * (Re)schedules the next run when the feature is enabled and a destination is granted.
     * Cancels all pending alarms otherwise.
     */
    public static void reschedule(Context context) {
        WorldBackupConfig config = new WorldBackupConfig(context);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        if (!config.isEnabled() || !config.hasDestinationPermission()) {
            alarmManager.cancel(backupPendingIntent(context));
            return;
        }

        long lastRun = config.getLastRunAt();
        long next = lastRun > 0L
                ? WorldBackupConfig.nextRunAfter(lastRun, config.getIntervalHours())
                : System.currentTimeMillis() + 60_000L;
        long now = System.currentTimeMillis();
        if (next < now) {
            // Missed a run while on a long Doze; fire now.
            next = now;
        }

        long interval = config.getIntervalHours() * 3600_000L;
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, next, interval,
                backupPendingIntent(context));
    }

    /** Fires the service once immediately (Settings "Back up now" path). */
    public static void runNow(Context context) {
        WorldBackupConfig config = new WorldBackupConfig(context);
        if (!config.isEnabled() || !config.hasDestinationPermission()) return;
        Context appContext = context.getApplicationContext();
        Intent intent = new Intent(appContext, WorldBackupService.class);
        intent.setAction(WorldBackupService.ACTION_RUN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent);
        } else {
            appContext.startService(intent);
        }
    }

    private static PendingIntent backupPendingIntent(Context context) {
        Context appContext = context.getApplicationContext();
        Intent intent = new Intent(appContext, WorldBackupService.class);
        intent.setAction(WorldBackupService.ACTION_RUN);
        return PendingIntent.getForegroundService(appContext, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}