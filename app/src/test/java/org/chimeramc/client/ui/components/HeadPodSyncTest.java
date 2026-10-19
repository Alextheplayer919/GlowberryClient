package org.chimeramc.client.ui.components;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for the launch head pod's sync indicator rule. No mocks: the rule is pure.
 */
public class HeadPodSyncTest {

    @Test
    public void aRunningModuleReadsAsSynced() {
        assertEquals(HeadPodView.SYNC_SYNCED, HeadPodSync.stateFor(true, true));
        // Running wins even if the pref read back false, because the module is demonstrably up.
        assertEquals(HeadPodView.SYNC_SYNCED, HeadPodSync.stateFor(false, true));
    }

    @Test
    public void anEnabledModuleThatIsNotUpYetReadsAsLoading() {
        assertEquals(HeadPodView.SYNC_LOADING, HeadPodSync.stateFor(true, false));
    }

    @Test
    public void aDisabledModuleHidesTheIndicator() {
        assertEquals(HeadPodView.SYNC_UNKNOWN, HeadPodSync.stateFor(false, false));
    }

    @Test
    public void theThreeStatesAreDistinct() {
        int synced = HeadPodSync.stateFor(true, true);
        int loading = HeadPodSync.stateFor(true, false);
        int unknown = HeadPodSync.stateFor(false, false);
        org.junit.Assert.assertNotEquals(synced, loading);
        org.junit.Assert.assertNotEquals(loading, unknown);
        org.junit.Assert.assertNotEquals(synced, unknown);
    }
}
