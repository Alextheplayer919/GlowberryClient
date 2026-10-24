package org.chimeramc.client.launcher.core.minecraft;

/**
 * The named pre-launch failure states (U14).
 *
 * <p>"It doesn't work" is not a report a launcher can act on. Each way a launch can be refused is a
 * distinct condition with its own cause, so this enumerates them and lets the UI say which one is
 * true and what to do about it, rather than collapsing them into a generic error.
 *
 * <p>The decision itself is pure: the caller gathers the facts (is a version selected, does the APK
 * ship a loadable ABI, is the extraction cache current, is Minecraft installed, is the account
 * signed in) and this maps them to one state. Keeping it pure is what makes the state machine
 * testable without a device — a wrong precedence here is exactly the bug that shows the "old
 * version" message for a missing game.
 *
 * <p>Precedence matters and is deliberate: the most fundamental missing thing wins. There is no
 * point reporting a stale library cache for a version whose APK cannot load in this process at all.
 */
public final class LaunchReadiness {

    private LaunchReadiness() {
    }

    /** Every reason a launch can be blocked, in the order they are checked. */
    public enum State {
        /** Ready: nothing is missing. */
        READY,
        /** No instance is selected yet. */
        NO_VERSION_SELECTED,
        /** The selected instance's game files are missing or its folder was removed. */
        GAME_FILES_MISSING,
        /** The APK ships only a 32-bit ABI and this is a 64-bit process. */
        ABI_INCOMPATIBLE,
        /** The extracted native libraries are from an older extractor and must be re-extracted. */
        STALE_NATIVE_LIBS,
        /** The signature rules do not cover this build, so the byte-pattern hooks stay off. */
        SIGNATURE_RULES_MISSING,
        /** The Minecraft package itself is not installed on the device. */
        GAME_NOT_INSTALLED,
        /** The launcher-managed account is required but not signed in. */
        SIGN_IN_REQUIRED,
    }

    /** The facts a launch preflight needs. A builder keeps the call site readable. */
    public static final class Facts {
        public boolean versionSelected;
        public boolean gameFilesPresent = true;
        public boolean abiIncompatible;
        public boolean extractionStale;
        public boolean signatureRulesCoverBuild = true;
        public boolean minecraftInstalled = true;
        public boolean signInRequired;
        public boolean signedIn = true;

        public Facts versionSelected(boolean v) { versionSelected = v; return this; }
        public Facts gameFilesPresent(boolean v) { gameFilesPresent = v; return this; }
        public Facts abiIncompatible(boolean v) { abiIncompatible = v; return this; }
        public Facts extractionStale(boolean v) { extractionStale = v; return this; }
        public Facts signatureRulesCoverBuild(boolean v) { signatureRulesCoverBuild = v; return this; }
        public Facts minecraftInstalled(boolean v) { minecraftInstalled = v; return this; }
        public Facts signInRequired(boolean v) { signInRequired = v; return this; }
        public Facts signedIn(boolean v) { signedIn = v; return this; }
    }

    /** The first condition that blocks a launch, or {@link State#READY}. */
    public static State evaluate(Facts facts) {
        if (facts == null) return State.NO_VERSION_SELECTED;
        if (!facts.versionSelected) return State.NO_VERSION_SELECTED;
        if (!facts.gameFilesPresent) return State.GAME_FILES_MISSING;
        if (facts.abiIncompatible) return State.ABI_INCOMPATIBLE;
        if (facts.extractionStale) return State.STALE_NATIVE_LIBS;
        if (!facts.signatureRulesCoverBuild) return State.SIGNATURE_RULES_MISSING;
        if (!facts.minecraftInstalled) return State.GAME_NOT_INSTALLED;
        if (facts.signInRequired && !facts.signedIn) return State.SIGN_IN_REQUIRED;
        return State.READY;
    }

    /**
     * Whether a state should stop the launch outright.
     *
     * <p>{@link State#STALE_NATIVE_LIBS} and {@link State#SIGNATURE_RULES_MISSING} are *warnings*:
     * the launcher can repair the first itself and the second only disables optional hooks, so both
     * still launch. The rest genuinely cannot proceed. Making this a property of the state rather
     * than a second list at the call site is what stops the two from disagreeing.
     */
    public static boolean blocksLaunch(State state) {
        switch (state) {
            case READY:
            case STALE_NATIVE_LIBS:
            case SIGNATURE_RULES_MISSING:
                return false;
            default:
                return true;
        }
    }

    /** A stable, non-translatable key for the state, for the diagnostics report and bug reports. */
    public static String key(State state) {
        return state == null ? "unknown" : state.name().toLowerCase(java.util.Locale.ROOT);
    }
}
