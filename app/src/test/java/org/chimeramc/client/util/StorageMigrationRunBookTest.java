package org.chimeramc.client.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Pins the process-start classification and the interruption messaging (G15). The policy is pure,
 * so this needs no Context and no mocks.
 */
public class StorageMigrationRunBookTest {

    @Test
    public void freshProcessWithRunningFlagIsInterrupted() {
        assertEquals(StorageMigrationRunBook.RunProbe.INTERRUPTED,
                StorageMigrationRunBook.classifyRun(true, true));
    }

    @Test
    public void liveProcessWithRunningFlagIsLive() {
        assertEquals(StorageMigrationRunBook.RunProbe.LIVE,
                StorageMigrationRunBook.classifyRun(true, false));
    }

    @Test
    public void noRunningFlagAlwaysNotRunning() {
        assertEquals(StorageMigrationRunBook.RunProbe.NOT_RUNNING,
                StorageMigrationRunBook.classifyRun(false, true));
        assertEquals(StorageMigrationRunBook.RunProbe.NOT_RUNNING,
                StorageMigrationRunBook.classifyRun(false, false));
    }

    @Test
    public void interruptedMessageReportsProgress() {
        String message = StorageMigrationRunBook.interruptedMessage(40, 100, 2_500_000_000L);
        assertTrue(message.contains("40 of 100 files"));
        assertTrue(message.contains("2.3 GB"));
        assertTrue(message.contains("skipped"));
    }

    @Test
    public void interruptedMessageHandlesEdgeCounts() {
        String allDone = StorageMigrationRunBook.interruptedMessage(100, 100, 1024L);
        assertTrue(allDone.contains("All scanned files"));
        // no files scanned yet reads as "0 of 0", not as "complete"
        assertTrue(StorageMigrationRunBook.interruptedMessage(0, 0, 0L).contains("0 of 0"));
        // a negative total is clamped to zero and reports nothing yet
        assertTrue(StorageMigrationRunBook.interruptedMessage(5, -3, -1L).contains("0 of 0"));
    }
}