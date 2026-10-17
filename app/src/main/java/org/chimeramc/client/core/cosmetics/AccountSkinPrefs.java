package org.chimeramc.client.core.cosmetics;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persists whether the account skin lookup is allowed, per install.
 *
 * <p>The lookup sends the player's Xbox XUID to a third-party service. It is <b>on after a
 * Microsoft sign-in</b> (the sign-in is the one-line consent) and can be switched off in the
 * Cosmetics panel. Kept in its own prefs file so "reset cosmetics" clears it with the rest of the
 * cosmetics state.
 */
public final class AccountSkinPrefs {

    private static final String PREFS = "cosmetics_account_skin";
    private static final String KEY_ENABLED = "lookup_enabled";

    private AccountSkinPrefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Whether the account skin lookup is on (true after a sign-in, or when re-enabled). */
    public static boolean isEnabled(Context context) {
        if (context == null) return false;
        return prefs(context).getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        if (context == null) return;
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }
}
