package org.chimeramc.client.core.cosmetics;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persists whether the account skin lookup is allowed, per install.
 *
 * <p>The lookup sends the player's Xbox XUID to a third-party service, so it is <b>off by default</b>
 * and only ever runs after the player turns it on in the Cosmetics panel. Kept in its own prefs file
 * so "reset cosmetics" clears it with the rest of the cosmetics state.
 */
public final class AccountSkinPrefs {

    private static final String PREFS = "cosmetics_account_skin";
    private static final String KEY_ENABLED = "lookup_enabled";

    private AccountSkinPrefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** True only when the player has explicitly enabled the lookup. Default false. */
    public static boolean isEnabled(Context context) {
        if (context == null) return false;
        return prefs(context).getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        if (context == null) return;
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }
}
