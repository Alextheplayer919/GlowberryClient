package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import org.chimeramc.client.R;
import org.chimeramc.client.core.content.OptionsEditor;
import org.chimeramc.client.core.versions.GameVersion;
import org.chimeramc.client.core.versions.VersionManager;
import org.chimeramc.client.util.LauncherStorage;
import org.chimeramc.client.settings.FeatureSettings;

import java.io.File;

/**
 * Writes a catalogue module's value into the instance's {@code minecraftpe/options.txt}.
 *
 * <p>Several catalogue modules are settings, not hooks: hide the view bob, brighten the world, turn
 * particles down. The game exposes those only through its own options file, so the honest
 * implementation writes that file — and says the change applies on the next world load, because a
 * running world has already read it.
 *
 * <p>The options path is resolved exactly as {@code ContentManagementActivity} resolves it (the
 * selected version's storage profile through {@link LauncherStorage#getContentGameDataDir}), so a
 * module can never write to a different root than the one the game actually loads. When no version
 * is selected or the file cannot be written, the caller is told rather than left thinking the
 * setting took effect.
 */
final class CatalogOptionsWriter {

    private CatalogOptionsWriter() {
    }

    /**
     * Applies a key/value to the selected instance's options file.
     *
     * @return true when the file was written; false when no instance/options file was reachable
     */
    static boolean apply(Activity activity, String key, String value) {
        File optionsFile = optionsFile(activity);
        if (optionsFile == null) {
            toast(activity, R.string.catalog_options_no_instance);
            return false;
        }
        try {
            OptionsEditor editor = new OptionsEditor(optionsFile);
            editor.load();
            editor.setValue(key, value);
            editor.save();
            toast(activity, R.string.catalog_options_applied_next_load);
            return true;
        } catch (Throwable t) {
            android.util.Log.w("CatalogOptionsWriter", "Could not write " + key, t);
            toast(activity, R.string.catalog_options_write_failed);
            return false;
        }
    }

    private static File optionsFile(Context context) {
        VersionManager manager = VersionManager.getIfInitialized();
        if (manager == null) return null;
        GameVersion version = manager.getSelectedVersion();
        if (version == null) return null;

        SharedPreferences prefs = context.getSharedPreferences(
                LauncherStorage.CONTENT_MANAGEMENT_PREFS_NAME, Context.MODE_PRIVATE);
        FeatureSettings.StorageType stored = FeatureSettings.StorageType.INTERNAL;
        try {
            stored = FeatureSettings.StorageType.valueOf(prefs.getString(
                    LauncherStorage.CONTENT_STORAGE_TYPE_KEY,
                    FeatureSettings.StorageType.INTERNAL.name()));
        } catch (Throwable ignored) {
            // A malformed preference reads as the default rather than aborting the write.
        }
        FeatureSettings.StorageType resolved = LauncherStorage.normalizeContentStorageType(
                stored, version.versionIsolation);
        File gameDataDir = LauncherStorage.getContentGameDataDir(
                context, version.getStorageProfileId(), resolved);
        if (gameDataDir == null) return null;
        return new File(gameDataDir, "minecraftpe/options.txt");
    }

    private static void toast(Context context, int stringRes) {
        Toast.makeText(context, stringRes, Toast.LENGTH_SHORT).show();
    }
}
