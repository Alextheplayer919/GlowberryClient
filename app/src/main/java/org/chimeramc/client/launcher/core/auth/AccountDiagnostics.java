package org.chimeramc.client.launcher.core.auth;


import java.util.Locale;

/**
 * A read-only description of the sign-in and skin-sync state, for the diagnostics screen and its
 * copy-report.
 *
 * <p>This exists because the two reported "auth is broken" and "my skin is not syncing" cases have
 * no observable state today. The screen could say whether the game launched, but not whether there
 * was an account, whether its token blob was present, or whether a skin was cached — so a user
 * report could not be diagnosed without logcat, which the app cannot read.
 *
 * <p>The formatting is pure: it takes plain values and returns a label, so the thresholds and the
 * wording are unit-testable without a device, an account or a network. The caller reads the values
 * off the real account store and passes them in.
 */
public final class AccountDiagnostics {

    /** How old the persisted token blob may be before it is called stale rather than current. */
    public static final long TOKEN_STALE_MS = 24L * 60L * 60L * 1000L;

    /** The sign-in and skin-sync state, as read from the account store and the skin cache. */
    public static final class Snapshot {
        public boolean signedIn;
        public boolean hasXuid;
        public boolean hasTokenBlob;
        public long tokenAgeMs;
        public boolean skinCached;
        public boolean skinLookupEnabled;
        public boolean skinLookupAvailable;

        public Snapshot signedIn(boolean v) { signedIn = v; return this; }
        public Snapshot hasXuid(boolean v) { hasXuid = v; return this; }
        public Snapshot hasTokenBlob(boolean v) { hasTokenBlob = v; return this; }
        public Snapshot tokenAgeMs(long v) { tokenAgeMs = v; return this; }
        public Snapshot skinCached(boolean v) { skinCached = v; return this; }
        public Snapshot skinLookupEnabled(boolean v) { skinLookupEnabled = v; return this; }
        public Snapshot skinLookupAvailable(boolean v) { skinLookupAvailable = v; return this; }
    }

    /** A one-line description of the token blob: absent, current, or stale. */
    public static String describeToken(boolean hasTokenBlob, long tokenAgeMs) {
        if (!hasTokenBlob) {
            return "absent - sign in again";
        }
        if (tokenAgeMs < 0) {
            // An unknown age is not "stale": a freshly migrated account has no timestamp, and
            // calling it stale would send the user to re-authenticate for no reason.
            return "present";
        }
        long hours = tokenAgeMs / (60L * 60L * 1000L);
        if (tokenAgeMs >= TOKEN_STALE_MS) {
            return String.format(Locale.US, "stale (%dh old) - refreshes on launch", hours);
        }
        return String.format(Locale.US, "current (%dh old)", hours);
    }

    /**
     * A one-line description of where the preview's skin comes from.
     *
     * <p>Order matters and matches {@code PlayerSkinProvider}'s resolution chain: an account skin is
     * only used when the lookup is on and a cached atlas exists, so the label must not claim an
     * account skin when the cache is empty.
     */
    public static String describeSkinSource(boolean signedIn, boolean skinLookupEnabled,
                                            boolean skinCached) {
        if (!signedIn) {
            return "imported / applied pack / Steve";
        }
        if (!skinLookupEnabled) {
            return "imported / applied pack / Steve (account lookup off)";
        }
        if (!skinCached) {
            return "imported / applied pack / Steve (not cached yet)";
        }
        return "account skin (cached)";
    }

    /** A one-line description of whether the account skin lookup can run. */
    public static String describeSkinLookup(boolean signedIn, boolean enabled, boolean available) {
        if (!signedIn) {
            return "unavailable (no account)";
        }
        if (!enabled) {
            return "off (Cosmetics panel toggle)";
        }
        if (!available) {
            return "unavailable (no XUID)";
        }
        return "on";
    }

    /** True when [snapshot] is a state the user can act on, so the screen can say what to do. */
    public static boolean needsUserAction(Snapshot snapshot) {
        if (snapshot == null || !snapshot.signedIn) {
            return false; // Signing in is optional; not signed in is not a fault.
        }
        return !snapshot.hasTokenBlob;
    }

    private AccountDiagnostics() {
    }
}
