package org.chimeramc.client.core.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the Optifine crash-loop quarantine rule (item 19). No mocks: the rule is pure.
 */
public class OptifineQuarantineTest {

    @Test
    public void theThresholdMatchesTheManager() {
        assertEquals(OptifineModeManager.CRASH_LOOP_THRESHOLD, OptifineQuarantine.THRESHOLD);
    }

    @Test
    public void noFailuresIsClear() {
        assertEquals(OptifineQuarantine.State.CLEAR,
                OptifineQuarantine.classify(true, true, 0));
        assertNull(OptifineQuarantine.describe(OptifineQuarantine.State.CLEAR));
    }

    @Test
    public void theSuiteBeingOffIsNeverAQuarantine() {
        // A leftover counter with the master switch off must not be reported as a crash disable.
        assertEquals(OptifineQuarantine.State.CLEAR,
                OptifineQuarantine.classify(false, false, 3));
        assertFalse(OptifineQuarantine.wasDisabledByCrashGuard(
                OptifineQuarantine.classify(false, false, 3)));
    }

    @Test
    public void anEnabledItemRecoveringIsNotAQuarantine() {
        assertEquals(OptifineQuarantine.State.RETRYING,
                OptifineQuarantine.classify(true, true, 1));
        assertFalse(OptifineQuarantine.wasDisabledByCrashGuard(OptifineQuarantine.State.RETRYING));
        assertNotNull(OptifineQuarantine.describe(OptifineQuarantine.State.RETRYING));
    }

    @Test
    public void theLastAttemptIsCalledOut() {
        assertEquals(OptifineQuarantine.State.LAST_ATTEMPT,
                OptifineQuarantine.classify(true, true, OptifineQuarantine.THRESHOLD - 1));
        assertTrue(OptifineQuarantine.describe(OptifineQuarantine.State.LAST_ATTEMPT)
                .contains("one more"));
    }

    @Test
    public void aDisabledItemWithFailuresIsQuarantined() {
        OptifineQuarantine.State state = OptifineQuarantine.classify(true, false, 2);
        assertEquals(OptifineQuarantine.State.QUARANTINED, state);
        assertTrue(OptifineQuarantine.wasDisabledByCrashGuard(state));
        assertTrue(OptifineQuarantine.describe(state).contains("disabled"));
    }

    @Test
    public void aDisabledItemWithNoFailuresIsAUserChoice() {
        OptifineQuarantine.State state = OptifineQuarantine.classify(true, false, 0);
        assertEquals(OptifineQuarantine.State.CLEAR, state);
        assertFalse(OptifineQuarantine.wasDisabledByCrashGuard(state));
    }

    @Test
    public void aNegativeCountIsTreatedAsZero() {
        assertEquals(OptifineQuarantine.State.CLEAR,
                OptifineQuarantine.classify(true, true, -5));
    }
}
