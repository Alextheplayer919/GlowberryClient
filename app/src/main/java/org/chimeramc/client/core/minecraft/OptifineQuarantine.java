package org.chimeramc.client.core.minecraft;

/**
 * The pure rule behind the Optifine crash-loop quarantine (item 19).
 *
 * <p>An item that is switched off by the crash guard reads the same as one the user turned off by
 * hand, so the settings screen could not say why. This rule turns the persisted state into an
 * explanation: how many consecutive failures are on record, whether the item is one failure away
 * from being quarantined, and — once it is off with a non-zero counter — that the client disabled
 * it and the user can re-enable it.
 *
 * <p>Pure and Android-free, so the wording and the threshold agree with
 * {@code OptifineModeManager.CRASH_LOOP_THRESHOLD} in one unit test rather than in the field.
 */
public final class OptifineQuarantine {

    /** The consecutive-failure count at which an item is disabled, matching the manager. */
    public static final int THRESHOLD = 3;

    /** What the settings screen should say about one item's crash state. */
    public enum State {
        /** No failures on record, or the suite is off so nothing is applied. */
        CLEAR,
        /** The item is enabled and has failures on record but is below the threshold. */
        RETRYING,
        /** The item is enabled and this is its last attempt before it is disabled. */
        LAST_ATTEMPT,
        /**
         * The item is off with a failure count on record while the suite is on, so the client
         * disabled it. The user can turn it back on; doing so should clear the counter.
         */
        QUARANTINED,
    }

    private OptifineQuarantine() {
    }

    /**
     * Classifies an item's crash state.
     *
     * @param masterEnabled  whether the Optifine suite is on at all; with it off nothing is applied,
     *                       so a leftover count is not a quarantine
     * @param enabled        whether the item is currently on
     * @param crashCount     the persisted consecutive-failure count (0 when none)
     * @return the state the settings row should describe
     */
    public static State classify(boolean masterEnabled, boolean enabled, int crashCount) {
        int count = Math.max(0, crashCount);
        if (!masterEnabled || count == 0) return State.CLEAR;
        if (!enabled) return State.QUARANTINED;
        // Enabled with failures on record: the next one disables it.
        return count >= THRESHOLD - 1 ? State.LAST_ATTEMPT : State.RETRYING;
    }

    /**
     * Whether the state means the client switched the item off rather than the user.
     *
     * <p>This is the "show why" predicate: it is true only when the suite is on, the item is off
     * and a failure count is on record, so a user-disabled item is never blamed on a crash.
     */
    public static boolean wasDisabledByCrashGuard(State state) {
        return state == State.QUARANTINED;
    }

    /**
     * The short explanation for the row, or null when there is nothing to explain.
     *
     * <p>Kept here so the settings screen and any future surface (the diagnostics report) say the
     * same thing, and so the wording is unit-tested.
     */
    public static String describe(State state) {
        switch (state) {
            case QUARANTINED:
                return "disabled after repeated launch failures — re-enable to try again";
            case LAST_ATTEMPT:
                return "failed to start the last " + (THRESHOLD - 1)
                        + " launches; one more disables it";
            case RETRYING:
                return "recovering after a failed launch";
            case CLEAR:
            default:
                return null;
        }
    }
}
