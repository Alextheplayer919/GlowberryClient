package org.chimeramc.client.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Re-arms the scheduled backup alarm after a reboot or package update.
 *
 * <p>AlarmManager alarms are cleared on device reboot, so a schedule that is enabled but never
 * re-armed would silently stop firing. This receiver is registered for {@code BOOT_COMPLETED}
 * and {@code MY_PACKAGE_REPLACED} in the manifest and simply calls the same
 * {@link WorldBackupScheduler#reschedule} the settings screen uses.
 */
public class WorldBackupBootReceiver extends BroadcastReceiver {

    private static final String TAG = "WorldBackupBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return;
        }
        try {
            WorldBackupScheduler.reschedule(context);
        } catch (Exception e) {
            // A boot receiver must never crash the system boot flow.
            Log.w(TAG, "Failed to reschedule world backups", e);
        }
    }
}