package org.chimeramc.client.launcher.core.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the diagnostics wording for the sign-in and skin-sync state.
 *
 * <p>These labels are read by a user filing a bug report, so the interesting cases are the ones that
 * must NOT over-claim: a present-but-old token is not "absent", an unknown age is not "stale", and an
 * account skin is only claimed when the cache actually holds one.
 */
public class AccountDiagnosticsTest {

    @Test
    public void aMissingTokenBlobSaysToSignInAgain() {
        assertTrue(AccountDiagnostics.describeToken(false, 0).contains("absent"));
        assertTrue(AccountDiagnostics.describeToken(false, -1).contains("absent"));
    }

    @Test
    public void anUnknownAgeIsPresentRatherThanStale() {
        // A freshly migrated account has no timestamp; calling it stale would send the user to
        // re-authenticate for no reason.
        String label = AccountDiagnostics.describeToken(true, -1);
        assertEquals("present", label);
        assertFalse(label.contains("stale"));
    }

    @Test
    public void aFreshTokenIsCurrentAndAnOldOneIsStale() {
        assertTrue(AccountDiagnostics.describeToken(true, 0).contains("current"));
        assertTrue(AccountDiagnostics.describeToken(true, 60_000L).contains("current"));
        // Just under the threshold is still current.
        assertTrue(AccountDiagnostics.describeToken(
                true, AccountDiagnostics.TOKEN_STALE_MS - 1).contains("current"));
        // At the threshold it flips.
        assertTrue(AccountDiagnostics.describeToken(
                true, AccountDiagnostics.TOKEN_STALE_MS).contains("stale"));
        assertTrue(AccountDiagnostics.describeToken(
                true, AccountDiagnostics.TOKEN_STALE_MS * 5).contains("stale"));
    }

    @Test
    public void aStaleTokenSaysItRefreshesOnLaunch() {
        // The launcher refreshes silently at startup, so "stale" must not read as "broken".
        String label = AccountDiagnostics.describeToken(true, AccountDiagnostics.TOKEN_STALE_MS * 2);
        assertTrue(label.contains("refreshes on launch"));
    }

    @Test
    public void anAccountSkinIsOnlyClaimedWhenItIsCached() {
        // The resolution chain reads the cache first; an empty cache means the next rung, so the
        // label must not claim an account skin.
        assertEquals("account skin (cached)",
                AccountDiagnostics.describeSkinSource(true, true, true));
        // The negative label must not contain the phrase "account skin" at all, or a reader would
        // take it as the source rather than as the rung it fell through to.
        assertFalse(AccountDiagnostics.describeSkinSource(true, true, false).contains("account skin"));
        assertTrue(AccountDiagnostics.describeSkinSource(true, true, false).contains("not cached"));
    }

    @Test
    public void theSkinSourceNamesTheFallbackChainWhenThereIsNoAccount() {
        String label = AccountDiagnostics.describeSkinSource(false, true, true);
        assertTrue(label.contains("imported"));
        assertTrue(label.contains("Steve"));
        assertFalse(label.contains("account skin"));
    }

    @Test
    public void aDisabledLookupIsNamedAsSuchRatherThanLookingBroken() {
        String off = AccountDiagnostics.describeSkinSource(true, false, false);
        assertTrue(off.contains("lookup off"));
        assertTrue(off.contains("Steve"));
    }

    @Test
    public void theLookupStateDistinguishesOffFromUnavailable() {
        assertEquals("on", AccountDiagnostics.describeSkinLookup(true, true, true));
        assertTrue(AccountDiagnostics.describeSkinLookup(true, false, false).contains("off"));
        assertTrue(AccountDiagnostics.describeSkinLookup(true, true, false).contains("no XUID"));
        assertTrue(AccountDiagnostics.describeSkinLookup(false, true, true).contains("no account"));
    }

    @Test
    public void onlyAMissingTokenBlobOnASignedInAccountNeedsUserAction() {
        assertFalse("not signed in is not a fault",
                AccountDiagnostics.needsUserAction(
                        new AccountDiagnostics.Snapshot().signedIn(false)));
        assertTrue(AccountDiagnostics.needsUserAction(
                new AccountDiagnostics.Snapshot().signedIn(true).hasTokenBlob(false)));
        assertFalse(AccountDiagnostics.needsUserAction(
                new AccountDiagnostics.Snapshot().signedIn(true).hasTokenBlob(true)));
        assertFalse(AccountDiagnostics.needsUserAction(null));
    }
}
