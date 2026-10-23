package org.chimeramc.client.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WorldBackupPlanTest {

    private static WorldBackupPlan.WorldEntry world(String name) {
        return new WorldBackupPlan.WorldEntry(name, new File(name));
    }

    private static List<String> names(String... names) {
        return new ArrayList<>(Arrays.asList(names));
    }

    /** Builds a backup file whose timestamp encodes the given hour-of-day so ordering is assertable. */
    private static String backup(String world, String hhmmss) {
        return world + "_20240101_" + hhmmss + ".mcworld";
    }

    @Test
    public void emptyWorldsProduceEmptyPlan() {
        WorldBackupPlan plan = WorldBackupPlan.plan(new ArrayList<>(), names(), 5);
        assertTrue(plan.isEmpty());
        assertTrue(plan.getWorldsToBackup().isEmpty());
        assertTrue(plan.getFilesToDelete().isEmpty());
    }

    @Test
    public void plansAllDiscoveredWorldsSortedByName() {
        List<WorldBackupPlan.WorldEntry> worlds = names("Zeta", "Alpha", "Mid")
                .stream().map(WorldBackupPlanTest::world).collect(java.util.stream.Collectors.toList());
        WorldBackupPlan plan = WorldBackupPlan.plan(worlds, names(), 5);
        assertEquals(3, plan.getWorldsToBackup().size());
        assertEquals("Alpha", plan.getWorldsToBackup().get(0).name);
        assertEquals("Mid", plan.getWorldsToBackup().get(1).name);
        assertEquals("Zeta", plan.getWorldsToBackup().get(2).name);
    }

    @Test
    public void removesStaleBackupsWhoseWorldIsGone() {
        List<WorldBackupPlan.WorldEntry> worlds = names("Existing")
                .stream().map(WorldBackupPlanTest::world).collect(java.util.stream.Collectors.toList());
        WorldBackupPlan plan = WorldBackupPlan.plan(
                worlds,
                names(backup("Existing", "050000"), backup("DeletedWorld", "060000")),
                5
        );
        assertEquals(1, plan.getFilesToDelete().size());
        assertTrue(plan.getFilesToDelete().get(0).contains("DeletedWorld"));
    }

    @Test
    public void keepsNewestRetentionPerWorldAndDeletesTheOldest() {
        List<WorldBackupPlan.WorldEntry> worlds = names("A")
                .stream().map(WorldBackupPlanTest::world).collect(java.util.stream.Collectors.toList());
        // 4 backups of world A, retention 2 -> delete the two oldest.
        WorldBackupPlan plan = WorldBackupPlan.plan(
                worlds,
                names(
                        backup("A", "010000"),
                        backup("A", "020000"),
                        backup("A", "030000"),
                        backup("A", "040000")
                ),
                2
        );
        assertEquals(2, plan.getFilesToDelete().size());
        assertTrue(plan.getFilesToDelete().get(0).endsWith("010000.mcworld"));
        assertTrue(plan.getFilesToDelete().get(1).endsWith("020000.mcworld"));
    }

    @Test
    public void retentionIsPerWorldNotGlobal() {
        List<WorldBackupPlan.WorldEntry> worlds = names("A", "B")
                .stream().map(WorldBackupPlanTest::world).collect(java.util.stream.Collectors.toList());
        // A has 3 backups (retention 2 -> delete 1), B has 1 backup (keep all).
        WorldBackupPlan plan = WorldBackupPlan.plan(
                worlds,
                names(
                        backup("A", "010000"),
                        backup("A", "020000"),
                        backup("A", "030000"),
                        backup("B", "090000")
                ),
                2
        );
        assertEquals(1, plan.getFilesToDelete().size());
        assertTrue(plan.getFilesToDelete().get(0).startsWith("A_20240101_010000"));
    }

    @Test
    public void malformedBackupNamesAreNotDeleted() {
        List<WorldBackupPlan.WorldEntry> worlds = names("A")
                .stream().map(WorldBackupPlanTest::world).collect(java.util.stream.Collectors.toList());
        WorldBackupPlan plan = WorldBackupPlan.plan(
                worlds,
                names("unrelated.txt", "A.mcworld"),
                1
        );
        assertTrue(plan.getFilesToDelete().isEmpty());
    }
}