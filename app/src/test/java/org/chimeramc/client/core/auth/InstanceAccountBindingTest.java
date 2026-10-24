package org.chimeramc.client.core.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The per-instance account binding rule (F2). Pure, no mocks: the fallback behaviour decides which
 * account a launch signs in as, so every branch is pinned.
 */
public class InstanceAccountBindingTest {

    /** A minimal account for the rule. */
    private static final class FakeAccount implements InstanceAccountBinding.Account {
        private final String id;
        private final boolean active;
        private final boolean hasAuth;

        FakeAccount(String id, boolean active, boolean hasAuth) {
            this.id = id;
            this.active = active;
            this.hasAuth = hasAuth;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public boolean active() {
            return active;
        }

        @Override
        public boolean hasAuthMaterial() {
            return hasAuth;
        }
    }

    private static List<InstanceAccountBinding.Account> accounts(InstanceAccountBinding.Account... a) {
        return new ArrayList<>(Arrays.asList(a));
    }

    /** An unbound instance must behave exactly as before: it uses the active account. */
    @Test
    public void anUnboundInstanceUsesTheActiveAccount() {
        List<InstanceAccountBinding.Account> list = accounts(
                new FakeAccount("a", false, true),
                new FakeAccount("b", true, true));

        InstanceAccountBinding.Resolution r = InstanceAccountBinding.resolve(null, list);

        assertNotNull(r.account);
        assertEquals("b", r.account.id());
        assertEquals(InstanceAccountBinding.REASON_UNBOUND, r.reason);
        assertFalse(r.bindingStale);
        assertFalse(r.usedBinding());
    }

    /** A live binding wins over the globally active account — that is the whole point. */
    @Test
    public void aBoundAccountWinsOverTheActiveAccount() {
        List<InstanceAccountBinding.Account> list = accounts(
                new FakeAccount("a", false, true),
                new FakeAccount("b", true, true));

        InstanceAccountBinding.Resolution r = InstanceAccountBinding.resolve("a", list);

        assertNotNull(r.account);
        assertEquals("a", r.account.id());
        assertNull(r.reason);
        assertFalse(r.bindingStale);
        assertTrue(r.usedBinding());
    }

    /** A deleted account must not make the instance unlaunchable; it falls back and says so. */
    @Test
    public void aBindingToARemovedAccountFallsBackToActiveAndReportsIt() {
        List<InstanceAccountBinding.Account> list = accounts(new FakeAccount("b", true, true));

        InstanceAccountBinding.Resolution r = InstanceAccountBinding.resolve("gone", list);

        assertEquals("b", r.account.id());
        assertEquals(InstanceAccountBinding.REASON_BOUND_ACCOUNT_MISSING, r.reason);
        assertTrue(r.bindingStale);
        // The dead id is still reported, so the caller can keep it rather than silently re-point.
        assertEquals("gone", r.boundAccountId);
        assertFalse(r.usedBinding());
    }

    /** An account with no auth material would sign the game out, so it is not a valid target. */
    @Test
    public void aBoundAccountWithNoAuthMaterialIsReportedAndFallsBack() {
        List<InstanceAccountBinding.Account> list = accounts(
                new FakeAccount("a", false, false),
                new FakeAccount("b", true, true));

        InstanceAccountBinding.Resolution r = InstanceAccountBinding.resolve("a", list);

        assertEquals("b", r.account.id());
        assertEquals(InstanceAccountBinding.REASON_NO_AUTH, r.reason);
        assertTrue(r.bindingStale);
    }

    /** With nothing usable at all, the launch proceeds with no account rather than failing. */
    @Test
    public void noUsableAccountResolvesToNullWithoutThrowing() {
        InstanceAccountBinding.Resolution r = InstanceAccountBinding.resolve("a", Collections.emptyList());
        assertNull(r.account);
        assertEquals(InstanceAccountBinding.REASON_NO_ACCOUNT, r.reason);
        assertTrue(r.bindingStale);
    }

    @Test
    public void aNullAccountListResolvesToNull() {
        InstanceAccountBinding.Resolution r = InstanceAccountBinding.resolve("a", null);
        assertNull(r.account);
        assertEquals(InstanceAccountBinding.REASON_NO_ACCOUNT, r.reason);
    }

    /** A blank or whitespace binding is unbound, not a lookup for an empty id. */
    @Test
    public void aBlankBindingIsTreatedAsUnbound() {
        List<InstanceAccountBinding.Account> list = accounts(new FakeAccount("b", true, true));

        for (String blank : new String[] {null, "", "   ", "\t"}) {
            InstanceAccountBinding.Resolution r = InstanceAccountBinding.resolve(blank, list);
            assertEquals("b", r.account.id());
            assertEquals(InstanceAccountBinding.REASON_UNBOUND, r.reason);
            assertFalse(r.bindingStale);
        }
    }

    /** Surrounding whitespace on a stored id must not break the match. */
    @Test
    public void aBindingWithSurroundingWhitespaceStillMatches() {
        List<InstanceAccountBinding.Account> list = accounts(new FakeAccount("a", false, true));
        InstanceAccountBinding.Resolution r = InstanceAccountBinding.resolve("  a  ", list);
        assertEquals("a", r.account.id());
        assertTrue(r.usedBinding());
    }

    /** The UI badge asks a separate question; it must agree with the resolution. */
    @Test
    public void isStaleAgreesWithResolve() {
        List<InstanceAccountBinding.Account> list = accounts(
                new FakeAccount("a", false, true),
                new FakeAccount("noauth", false, false),
                new FakeAccount("b", true, true));

        assertFalse(InstanceAccountBinding.isStale("a", list));
        assertFalse(InstanceAccountBinding.isStale(null, list));
        assertFalse(InstanceAccountBinding.isStale("  ", list));
        assertTrue(InstanceAccountBinding.isStale("gone", list));
        assertTrue(InstanceAccountBinding.isStale("noauth", list));

        assertFalse(InstanceAccountBinding.resolve("a", list).bindingStale);
        assertTrue(InstanceAccountBinding.resolve("gone", list).bindingStale);
        assertTrue(InstanceAccountBinding.resolve("noauth", list).bindingStale);
    }

    /** A null entry in the list must not crash the scan. */
    @Test
    public void aNullAccountEntryIsSkipped() {
        List<InstanceAccountBinding.Account> list = accounts(
                null,
                new FakeAccount("b", true, true));

        InstanceAccountBinding.Resolution r = InstanceAccountBinding.resolve(null, list);
        assertEquals("b", r.account.id());
        // "a" is not in the list at all, so it is stale; the null entry must not crash the scan.
        assertTrue(InstanceAccountBinding.isStale("a", list));
    }
}
