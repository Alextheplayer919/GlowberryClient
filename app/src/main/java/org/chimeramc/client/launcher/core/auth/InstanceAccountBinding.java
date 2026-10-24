package org.chimeramc.client.core.auth;

import java.util.List;

/**
 * Which Microsoft account an instance launches with (F2).
 *
 * <p>Accounts are stored globally: one is marked active and {@code XalExporter} writes that
 * account's tokens into the game's XAL directory at launch. That is fine until two instances want
 * two different accounts — an alt for a second world, a shared device, a separate Realm. Binding an
 * account to an instance makes the launch choose the bound account instead of whichever one
 * happened to be globally active.
 *
 * <p>Pure and Android-free, so the choice is unit-testable without a device or a preferences file.
 * The rules are deliberately conservative:
 *
 * <ul>
 *   <li><b>No binding means the active account.</b> An instance that was never bound behaves
 *       exactly as before, so this feature cannot change an existing setup.</li>
 *   <li><b>A binding to a removed account falls back to active, and says so.</b> A user who deletes
 *       an account must not end up with an instance that refuses to launch. The stale binding is
 *       reported through {@link Resolution#bindingStale} so the UI can show it rather than
 *       silently launching as somebody else with no explanation.</li>
 *   <li><b>A binding is never silently re-pointed.</b> If the bound account is gone, the instance
 *       keeps the dead id in its metadata; it does not rewrite itself to the fallback account. The
 *       user decides when the binding changes, which is what keeps "this instance is account X" an
 *       honest statement.</li>
 *   <li><b>An account with no usable auth material is not a valid target.</b> Binding to an account
 *       whose {@code serializedAuthManager} is absent would export nothing and the game would sign
 *       out; that is reported as {@link #REASON_NO_AUTH} rather than attempted.</li>
 * </ul>
 */
public final class InstanceAccountBinding {

    /** Reported when an instance has no bound account; the active account is used. */
    public static final String REASON_UNBOUND = "unbound";

    /** Reported when the bound account no longer exists; the active account is used. */
    public static final String REASON_BOUND_ACCOUNT_MISSING = "bound_account_missing";

    /** Reported when the bound account exists but carries no auth material. */
    public static final String REASON_NO_AUTH = "no_auth";

    /** Reported when neither a binding nor an active account resolves to a usable account. */
    public static final String REASON_NO_ACCOUNT = "no_account";

    private InstanceAccountBinding() {}

    /** The minimal view of an account this rule needs, so it can be tested without the store. */
    public interface Account {
        String id();

        boolean active();

        boolean hasAuthMaterial();
    }

    /**
     * The account an instance should launch with, plus why.
     *
     * <p>{@code account} is null only when nothing usable exists; in that case the launch proceeds
     * with no exported XAL data, exactly as it does today when no account is signed in.
     */
    public static final class Resolution {
        public final Account account;
        public final String reason;
        public final boolean bindingStale;
        public final String boundAccountId;

        private Resolution(Account account, String reason, boolean bindingStale,
                           String boundAccountId) {
            this.account = account;
            this.reason = reason;
            this.bindingStale = bindingStale;
            this.boundAccountId = boundAccountId;
        }

        /** True when the instance's own binding decided the account, with no fallback. */
        public boolean usedBinding() {
            return account != null && boundAccountId != null
                    && boundAccountId.equals(account.id());
        }
    }

    /**
     * Chooses the launch account.
     *
     * @param boundAccountId the instance's bound account id, or null/blank when unbound
     * @param accounts       every stored account; may be null
     */
    public static Resolution resolve(String boundAccountId, List<? extends Account> accounts) {
        String bound = blankToNull(boundAccountId);

        if (bound == null) {
            Account active = activeAccount(accounts);
            return new Resolution(active,
                    active == null ? REASON_NO_ACCOUNT : REASON_UNBOUND, false, null);
        }

        Account boundAccount = findById(accounts, bound);
        if (boundAccount == null) {
            Account active = activeAccount(accounts);
            return new Resolution(active,
                    active == null ? REASON_NO_ACCOUNT : REASON_BOUND_ACCOUNT_MISSING, true, bound);
        }
        if (!boundAccount.hasAuthMaterial()) {
            Account active = activeAccount(accounts);
            return new Resolution(active,
                    active == null ? REASON_NO_ACCOUNT : REASON_NO_AUTH, true, bound);
        }
        return new Resolution(boundAccount, null, false, bound);
    }

    /**
     * Whether a bound id is still usable, so the UI can badge a stale binding without resolving.
     *
     * <p>A blank id is unbound and is therefore not stale.
     */
    public static boolean isStale(String boundAccountId, List<? extends Account> accounts) {
        String bound = blankToNull(boundAccountId);
        if (bound == null) return false;
        Account account = findById(accounts, bound);
        return account == null || !account.hasAuthMaterial();
    }

    private static Account activeAccount(List<? extends Account> accounts) {
        if (accounts == null) return null;
        for (Account account : accounts) {
            if (account != null && account.active()) return account;
        }
        return null;
    }

    private static Account findById(List<? extends Account> accounts, String id) {
        if (accounts == null || id == null) return null;
        for (Account account : accounts) {
            if (account != null && id.equals(account.id())) return account;
        }
        return null;
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
