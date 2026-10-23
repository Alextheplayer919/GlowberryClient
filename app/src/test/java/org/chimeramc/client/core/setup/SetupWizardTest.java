package org.chimeramc.client.core.setup;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.chimeramc.client.launcher.core.setup.SetupWizard;

/**
 * The first-run wizard's step order (U13). Pure: no device.
 */
public class SetupWizardTest {

    private static SetupWizard.Facts fresh() {
        return new SetupWizard.Facts()
                .gameImported(false)
                .versionChosen(false)
                .signInRequired(true)
                .signedIn(false);
    }

    @Test
    public void aFreshInstallStartsAtImport() {
        assertEquals(SetupWizard.Step.IMPORT_GAME, SetupWizard.currentStep(fresh()));
        assertEquals(0, SetupWizard.currentIndex(fresh()));
        assertFalse(SetupWizard.isComplete(fresh()));
    }

    @Test
    public void eachStepCompletesInOrder() {
        SetupWizard.Facts facts = fresh().gameImported(true);
        assertEquals(SetupWizard.Step.PICK_VERSION, SetupWizard.currentStep(facts));
        facts.versionChosen(true);
        assertEquals(SetupWizard.Step.SIGN_IN, SetupWizard.currentStep(facts));
        facts.signedIn(true);
        assertEquals(SetupWizard.Step.READY, SetupWizard.currentStep(facts));
        assertTrue(SetupWizard.isComplete(facts));
    }

    @Test
    public void signInIsSkippedWhenTheFeatureIsOff() {
        SetupWizard.Facts facts = fresh()
                .gameImported(true)
                .versionChosen(true)
                .signInRequired(false)
                .signedIn(false);
        // No sign-in step is shown at all, so an unsigned-in user is still ready.
        assertEquals(SetupWizard.Step.READY, SetupWizard.currentStep(facts));
        assertTrue(SetupWizard.isComplete(facts));
    }

    @Test
    public void theRailOnlyShowsTheActiveSteps() {
        SetupWizard.Facts withSignIn = fresh();
        assertArrayEquals(new SetupWizard.Step[] {
                SetupWizard.Step.IMPORT_GAME,
                SetupWizard.Step.PICK_VERSION,
                SetupWizard.Step.SIGN_IN,
                SetupWizard.Step.READY,
        }, SetupWizard.activeSteps(withSignIn));

        SetupWizard.Facts withoutSignIn = fresh().signInRequired(false);
        assertArrayEquals(new SetupWizard.Step[] {
                SetupWizard.Step.IMPORT_GAME,
                SetupWizard.Step.PICK_VERSION,
                SetupWizard.Step.READY,
        }, SetupWizard.activeSteps(withoutSignIn));
    }

    @Test
    public void theRailIndexTracksTheActiveRail() {
        // Without sign-in the rail has three stops, so READY must be index 2, not 3.
        SetupWizard.Facts facts = fresh()
                .gameImported(true)
                .versionChosen(true)
                .signInRequired(false)
                .signedIn(false);
        assertEquals(2, SetupWizard.currentIndex(facts));
    }

    @Test
    public void aResumedWizardLandsOnTheFirstIncompleteStep() {
        // The user imported and chose, then quit before signing in.
        SetupWizard.Facts facts = fresh().gameImported(true).versionChosen(true);
        assertEquals(SetupWizard.Step.SIGN_IN, SetupWizard.currentStep(facts));
    }

    @Test
    public void everyStepHasAKey() {
        for (SetupWizard.Step step : SetupWizard.Step.values()) {
            String key = SetupWizard.key(step);
            assertTrue(key, key.length() > 0);
            assertEquals(key, key.toLowerCase(java.util.Locale.ROOT));
        }
        assertEquals("unknown", SetupWizard.key(null));
    }

    @Test
    public void nullFactsReadAsAFreshStart() {
        assertEquals(SetupWizard.Step.IMPORT_GAME, SetupWizard.currentStep(null));
    }
}
