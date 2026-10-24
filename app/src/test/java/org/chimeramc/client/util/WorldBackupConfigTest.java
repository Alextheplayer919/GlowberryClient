package org.chimeramc.client.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WorldBackupConfigTest {

    @Test
    public void isTreePathRecognizesOnlyTreePaths() {
        assertTrue(WorldBackupConfig.isTreePath(
                "/tree/primary%3ADownload%2FBackups"));
        assertFalse(WorldBackupConfig.isTreePath(
                "/document/primary%3Afoo"));
        assertFalse(WorldBackupConfig.isTreePath(null));
        assertFalse(WorldBackupConfig.isTreePath(""));
    }

    @Test
    public void nextRunAfterIsNowPlusInterval() {
        long now = 1_700_000_000_000L;
        assertTrue(WorldBackupConfig.nextRunAfter(now, 24) > now);
        assertTrue(WorldBackupConfig.nextRunAfter(0L, 24) == 0L);
        assertTrue(WorldBackupConfig.nextRunAfter(now, 1) == now + 3_600_000L);
    }

    @Test
    public void foldIntoIntervalHoursClampsToRange() {
        assertTrue(WorldBackupConfig.foldIntoIntervalHours(0) == WorldBackupConfig.MIN_INTERVAL_HOURS);
        assertTrue(WorldBackupConfig.foldIntoIntervalHours(-5) == WorldBackupConfig.MIN_INTERVAL_HOURS);
        assertTrue(WorldBackupConfig.foldIntoIntervalHours(24 * 7 * 2) == WorldBackupConfig.MAX_INTERVAL_HOURS);
        assertTrue(WorldBackupConfig.foldIntoIntervalHours(12) == 12);
    }

    @Test
    public void foldIntoIntervalHoursClampsHugeValuesAtMax() {
        assertTrue(WorldBackupConfig.foldIntoIntervalHours(100_000) == WorldBackupConfig.MAX_INTERVAL_HOURS);
    }
}