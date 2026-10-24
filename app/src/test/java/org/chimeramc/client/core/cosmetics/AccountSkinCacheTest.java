package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

/**
 * Pins that a cached account skin is only served to the account it was fetched for.
 *
 * <p>The key was written from the start but never compared, so a cache populated for one account was
 * shown to whichever account signed in next. These tests fail if the comparison is dropped again,
 * because the writer and the reader share {@link AccountSkinCache#accountKeyFor}.
 */
public class AccountSkinCacheTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static byte[] png() {
        // The content is irrelevant to the key rule; only that it is non-empty.
        return new byte[] {(byte) 0x89, 'P', 'N', 'G'};
    }

    @Test
    public void aCacheWrittenForOneAccountIsNotValidForAnother() {
        AccountSkinCache.Entry entry = new AccountSkinCache.Entry(png(), "account:111");
        assertTrue(AccountSkinCache.isValidFor(entry, "111"));
        assertFalse(AccountSkinCache.isValidFor(entry, "222"));
    }

    @Test
    public void anEntryWithNoKeyIsNotValidForAnyAccount() {
        // A cache written before the key was honoured must be refreshed, not shown.
        assertFalse(AccountSkinCache.isValidFor(
                new AccountSkinCache.Entry(png(), ""), "111"));
        assertFalse(AccountSkinCache.isValidFor(
                new AccountSkinCache.Entry(png(), null), "111"));
    }

    @Test
    public void aNullOrEmptyXuidHasNoValidCache() {
        AccountSkinCache.Entry entry = new AccountSkinCache.Entry(png(), "account:111");
        assertFalse(AccountSkinCache.isValidFor(entry, null));
        assertFalse(AccountSkinCache.isValidFor(entry, ""));
    }

    @Test
    public void anEmptyEntryIsNeverValid() {
        assertFalse(AccountSkinCache.isValidFor(null, "111"));
        assertFalse(AccountSkinCache.isValidFor(new AccountSkinCache.Entry(null, "account:111"), "111"));
        assertFalse(AccountSkinCache.isValidFor(new AccountSkinCache.Entry(new byte[0], "account:111"), "111"));
    }

    @Test
    public void theWriterAndReaderAgreeOnTheKeyFormat() throws Exception {
        // The end-to-end rule: write through the shared helper, read back, and it must validate.
        File root = folder.newFolder("files");
        String xuid = "2533274812345678";
        assertTrue(AccountSkinCache.write(root,
                new AccountSkinCache.Entry(png(), AccountSkinCache.accountKeyFor(xuid))));

        AccountSkinCache.Entry read = AccountSkinCache.read(root);
        assertTrue(AccountSkinCache.isValidFor(read, xuid));
        // And a different account reading the same cache must reject it.
        assertFalse(AccountSkinCache.isValidFor(read, "9999"));
    }

    @Test
    public void theKeyFormatIsStable() {
        // A change here silently invalidates every existing cache, which is a one-time re-fetch, so
        // it is pinned rather than left to drift.
        assertEquals("account:abc", AccountSkinCache.accountKeyFor("abc"));
        assertEquals("", AccountSkinCache.accountKeyFor(null));
        assertEquals("", AccountSkinCache.accountKeyFor(""));
    }
}
