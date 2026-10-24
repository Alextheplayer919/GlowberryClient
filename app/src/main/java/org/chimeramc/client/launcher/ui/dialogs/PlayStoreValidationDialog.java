package org.chimeramc.client.ui.dialogs;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import org.chimeramc.client.R;
import org.chimeramc.client.ui.activities.LibraryActivity;

public class PlayStoreValidationDialog {
    private static final String MINECRAFT_PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.mojang.minecraftpe";
    public static void showNotFromPlayStoreDialog(Context context) {
        new CustomAlertDialog(context)
                .setTitleText(context.getString(R.string.minecraft_playstore_required_title))
                .setMessage(context.getString(R.string.minecraft_playstore_required_message))
                .setPositiveButton(context.getString(R.string.buy_minecraft), v -> {
                    openPlayStore(context);
                })
                .setNegativeButton(context.getString(R.string.cancel), null)
                .show();
    }

    public static void showNotInstalledDialog(Context context) {
        // The primary action is Import, not the Play Store. A Play Store install is precisely the
        // copy this launcher cannot load (its native library stays inside the installed app), so
        // offering "Install Minecraft" first sent the user to a download that then failed the
        // launch with the Google-Play message. The import flow is the path that actually works.
        new CustomAlertDialog(context)
                .setTitleText(context.getString(R.string.minecraft_not_installed_title))
                .setMessage(context.getString(R.string.minecraft_not_installed_message))
                .setPositiveButton(context.getString(R.string.import_from_library), v -> {
                    Intent intent = new Intent(context, LibraryActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                })
                .setNegativeButton(context.getString(R.string.dialog_positive_ok), null)
                .show();
    }

    private static void openPlayStore(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.mojang.minecraftpe"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(MINECRAFT_PLAY_STORE_URL));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(context, context.getString(R.string.error_no_browser), Toast.LENGTH_SHORT).show();
            }
        }
    }
}