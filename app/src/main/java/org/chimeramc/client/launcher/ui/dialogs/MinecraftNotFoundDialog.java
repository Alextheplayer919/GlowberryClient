package org.chimeramc.client.ui.dialogs;

import android.content.Context;
import android.content.Intent;

import org.chimeramc.client.R;
import org.chimeramc.client.ui.activities.LibraryActivity;

/**
 * The "Minecraft not found" dialog.
 *
 * <p>The only action is Import. A Play Store install is exactly the copy this launcher cannot
 * launch, so offering the store here would send the user to a download that fails the launch with
 * the Google-Play message. There is deliberately no Play Store link in this dialog.
 */
public class MinecraftNotFoundDialog {

    public static void showNotInstalledDialog(Context context) {
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
}