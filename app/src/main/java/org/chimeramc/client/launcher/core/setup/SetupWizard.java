package org.chimeramc.client.launcher.core.setup;

import java.util.Locale;

/**
 * The first-run wizard's step machine (U13).
 *
 * <p>A first launch has four things to establish — the game files exist, an instance is chosen, an
 * account is signed in, and the launcher is ready — and doing them in a different order produces the
 * dead ends the wizard exists to prevent (choosing a version before there is one to choose, being
 * asked to sign in before the game is even importable).
 *
 * <p>The machine is pure: each step declares whether it is complete given the facts, and the wizard
 * advances to the first incomplete step. That makes "resume where the user left off" fall out of the
 * design instead of needing its own bookkeeping, and lets the ordering be unit-tested.
 *
 * <p>Sign-in is the one conditional step: it is skipped entirely when the launcher-managed login is
 * off, because asking a user to sign in to a feature they are not using is exactly the kind of
 * first-run friction this removes.
 */
public final class SetupWizard {

    private SetupWizard() {
    }

    /** The wizard's steps, in order. */
    public enum Step {
        /** Import or locate the Minecraft game files. */
        IMPORT_GAME,
        /** Choose the instance to play. */
        PICK_VERSION,
        /** Sign in to the launcher-managed account (skipped when the feature is off). */
        SIGN_IN,
        /** Everything is in place. */
        READY,
    }

    /** The facts the wizard reads. */
    public static final class Facts {
        public boolean gameImported;
        public boolean versionChosen;
        public boolean signInRequired;
        public boolean signedIn;

        public Facts gameImported(boolean v) { gameImported = v; return this; }
        public Facts versionChosen(boolean v) { versionChosen = v; return this; }
        public Facts signInRequired(boolean v) { signInRequired = v; return this; }
        public Facts signedIn(boolean v) { signedIn = v; return this; }
    }

    /** The step the wizard should be showing: the first one that is not complete. */
    public static Step currentStep(Facts facts) {
        if (facts == null) return Step.IMPORT_GAME;
        if (!facts.gameImported) return Step.IMPORT_GAME;
        if (!facts.versionChosen) return Step.PICK_VERSION;
        if (facts.signInRequired && !facts.signedIn) return Step.SIGN_IN;
        return Step.READY;
    }

    /**
     * The steps the progress rail should show.
     *
     * <p>The rail is built from the *active* steps, so a wizard with sign-in turned off shows three
     * dots and one with it on shows four. A fixed four-dot rail that hides one would make the
     * progress look stuck.
     */
    public static Step[] activeSteps(Facts facts) {
        boolean signIn = facts != null && facts.signInRequired;
        if (signIn) {
            return new Step[] { Step.IMPORT_GAME, Step.PICK_VERSION, Step.SIGN_IN, Step.READY };
        }
        return new Step[] { Step.IMPORT_GAME, Step.PICK_VERSION, Step.READY };
    }

    /** The index of the current step within {@link #activeSteps}, for the rail's fill. */
    public static int currentIndex(Facts facts) {
        Step current = currentStep(facts);
        Step[] steps = activeSteps(facts);
        for (int i = 0; i < steps.length; i++) {
            if (steps[i] == current) return i;
        }
        return 0;
    }

    /** Whether the wizard is finished and the launcher should open normally. */
    public static boolean isComplete(Facts facts) {
        return currentStep(facts) == Step.READY;
    }

    /** A stable key for the step, for persistence and the diagnostics report. */
    public static String key(Step step) {
        return step == null ? "unknown" : step.name().toLowerCase(Locale.ROOT);
    }
}
