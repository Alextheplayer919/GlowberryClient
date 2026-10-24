package org.chimeramc.client.core.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.launcher.core.minecraft.LaunchFailurePresenter;
import org.chimeramc.client.launcher.core.minecraft.LaunchReadiness;
import org.junit.Test;

/**
 * The failure-state copy table (U14). Pure: resource ids only, no Context.
 */
public class LaunchFailurePresenterTest {

    @Test
    public void readyNeedsNoDialog() {
        assertEquals(0, LaunchFailurePresenter.titleRes(LaunchReadiness.State.READY));
        assertEquals(0, LaunchFailurePresenter.messageRes(LaunchReadiness.State.READY));
    }

    @Test
    public void everyBlockingStateHasTitleAndMessage() {
        for (LaunchReadiness.State state : LaunchReadiness.State.values()) {
            if (state == LaunchReadiness.State.READY) continue;
            assertNotEquals("missing title for " + state,
                    0, LaunchFailurePresenter.titleRes(state));
            assertNotEquals("missing message for " + state,
                    0, LaunchFailurePresenter.messageRes(state));
        }
    }

    @Test
    public void distinctStatesGetDistinctCopy() {
        // A shared message would make two different failures indistinguishable in a bug report.
        LaunchReadiness.State[] blocking = {
                LaunchReadiness.State.NO_VERSION_SELECTED,
                LaunchReadiness.State.GAME_FILES_MISSING,
                LaunchReadiness.State.ABI_INCOMPATIBLE,
                LaunchReadiness.State.STALE_NATIVE_LIBS,
                LaunchReadiness.State.SIGNATURE_RULES_MISSING,
                LaunchReadiness.State.GAME_NOT_INSTALLED,
                LaunchReadiness.State.SIGN_IN_REQUIRED,
        };
        for (int i = 0; i < blocking.length; i++) {
            for (int j = i + 1; j < blocking.length; j++) {
                assertNotEquals("shared message for " + blocking[i] + " and " + blocking[j],
                        LaunchFailurePresenter.messageRes(blocking[i]),
                        LaunchFailurePresenter.messageRes(blocking[j]));
            }
        }
    }

    @Test
    public void theWarningFlagAgreesWithTheModel() {
        for (LaunchReadiness.State state : LaunchReadiness.State.values()) {
            assertEquals("warning flag disagrees for " + state,
                    !LaunchReadiness.blocksLaunch(state),
                    LaunchFailurePresenter.isWarning(state));
        }
        assertTrue(LaunchFailurePresenter.isWarning(LaunchReadiness.State.STALE_NATIVE_LIBS));
        assertFalse(LaunchFailurePresenter.isWarning(LaunchReadiness.State.ABI_INCOMPATIBLE));
    }
}
