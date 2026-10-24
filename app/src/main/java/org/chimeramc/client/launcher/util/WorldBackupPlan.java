package org.chimeramc.client.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Pure planning for a scheduled world backup run.
 *
 * <p>Given the worlds currently present and the backup files already on disk, this answers two
 * questions with no I/O: which worlds to back up, and which of the existing files to remove to
 * honour the retention cap. Both halves are deterministic and unit-tested so a scheduling change
 * can never silently start backing up the wrong worlds or delete a retained backup.
 */
public final class WorldBackupPlan {

    public static final class WorldEntry {
        public final String name;
        public final File directory;

        public WorldEntry(String name, File directory) {
            this.name = name;
            this.directory = directory;
        }
    }

    private final List<WorldEntry> worldsToBackup;
    private final List<String> filesToDelete;

    private WorldBackupPlan(List<WorldEntry> worldsToBackup, List<String> filesToDelete) {
        this.worldsToBackup = worldsToBackup;
        this.filesToDelete = filesToDelete;
    }

    public List<WorldEntry> getWorldsToBackup() {
        return worldsToBackup;
    }

    public List<String> getFilesToDelete() {
        return filesToDelete;
    }

    public boolean isEmpty() {
        return worldsToBackup.isEmpty();
    }

    /**
     * Builds the plan from the discovered world directories and the file names already stored in
     * the destination folder.
     *
     * @param worlds        world names and their directories
     * @param existingNames file names already present in the destination (their {@link
     *                      WorldBackupNaming#worldKeyOf} tells which world they belong to)
     * @param retention     how many of the newest backups per world to keep (>= 1)
     */
    public static WorldBackupPlan plan(List<WorldEntry> worlds, List<String> existingNames, int retention) {
        int cap = Math.max(1, retention);

        List<WorldEntry> toBackup = new ArrayList<>(worlds);
        toBackup.sort(Comparator.comparing(e -> e.name, String.CASE_INSENSITIVE_ORDER));

        // Existing files whose world no longer exists are not retained — they are stale. A file
        // that does not carry our timestamp format (empty key) is left alone: it may be a backup
        // from an older format or a user's unrelated file, and this feature must fail closed
        // rather than delete something it does not own.
        List<String> delete = new ArrayList<>();
        for (String name : existingNames) {
            String key = WorldBackupNaming.worldKeyOf(name);
            if (key.isEmpty() || !containsWorld(toBackup, key)) {
                if (!key.isEmpty()) {
                    delete.add(name);
                }
            }
        }

        // Per-world, keep the newest `cap` and delete the rest.
        List<String> sameWorldFiles = new ArrayList<>();
        for (WorldEntry world : toBackup) {
            sameWorldFiles.clear();
            String worldKey = caseInsensitiveKey(world.name);
            for (String name : existingNames) {
                if (caseInsensitiveKey(WorldBackupNaming.worldKeyOf(name)).equals(worldKey)) {
                    sameWorldFiles.add(name);
                }
            }
            if (sameWorldFiles.size() <= cap) continue;

            sameWorldFiles.sort(Comparator.comparingLong(WorldBackupNaming::timestampOf));
            for (int i = 0; i < sameWorldFiles.size() - cap; i++) {
                delete.add(sameWorldFiles.get(i));
            }
        }

        // Deterministic output ordering so the prune pass is stable.
        Collections.sort(delete);
        return new WorldBackupPlan(toBackup, delete);
    }

    private static boolean containsWorld(List<WorldEntry> worlds, String worldName) {
        String key = caseInsensitiveKey(worldName);
        for (WorldEntry world : worlds) {
            if (caseInsensitiveKey(world.name).equals(key)) return true;
        }
        return false;
    }

    private static String caseInsensitiveKey(String value) {
        return value == null ? "" : value.toLowerCase(Locale.US);
    }
}