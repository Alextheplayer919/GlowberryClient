package org.chimeramc.client.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Discovers Minecraft worlds under a set of game-data roots.
 *
 * <p>A world is a directory under {@code <root>/minecraftWorlds/<name>} that carries a
 * {@code level.dat} — the same validity check {@code WorldManager} uses. The discovery is kept
 * Android-free so the enumeration rule is unit-testable against real temp directories, and the
 * caller (the backup service) is responsible for handing over the right roots per installed
 * version/profile.
 */
public final class WorldBackupDiscovery {

    public static final String WORLDS_DIR = "minecraftWorlds";

    private WorldBackupDiscovery() {}

    /**
     * Walks each root looking for {@code minecraftWorlds/<name>/level.dat}.
     *
     * @param gameDataRoots top-level profile data dirs from {@code LauncherStorage}
     * @return the discovered worlds; stable order by name
     */
    public static List<WorldBackupPlan.WorldEntry> discover(List<File> gameDataRoots) {
        List<WorldBackupPlan.WorldEntry> worlds = new ArrayList<>();
        if (gameDataRoots == null) return worlds;

        for (File root : gameDataRoots) {
            File worldsRoot = root == null ? null : new File(root, WORLDS_DIR);
            if (worldsRoot == null || !worldsRoot.isDirectory()) continue;
            File[] entries = worldsRoot.listFiles();
            if (entries == null) continue;
            for (File entry : entries) {
                if (!entry.isDirectory()) continue;
                if (new File(entry, "level.dat").isFile()) {
                    worlds.add(new WorldBackupPlan.WorldEntry(entry.getName(), entry));
                }
            }
        }

        worlds.sort(java.util.Comparator.comparing(
                e -> e.name, String.CASE_INSENSITIVE_ORDER));
        return worlds;
    }
}