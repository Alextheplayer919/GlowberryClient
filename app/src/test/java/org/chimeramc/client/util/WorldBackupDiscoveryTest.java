package org.chimeramc.client.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WorldBackupDiscoveryTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private File world(String rootName, String worldName) throws Exception {
        File root = temp.newFolder(rootName);
        File worldDir = new File(new File(root, WorldBackupDiscovery.WORLDS_DIR), worldName);
        assertTrue(worldDir.mkdirs());
        assertTrue(new File(worldDir, "level.dat").createNewFile());
        return root;
    }

    @Test
    public void discoversWorldsWithLevelDat() throws Exception {
        File rootA = world("profileA", "World One");
        File rootB = world("profileB", "Third World");

        // A directory without level.dat is not a world.
        File stray = new File(new File(rootA, WorldBackupDiscovery.WORLDS_DIR), "not-a-world");
        assertTrue(stray.mkdirs());

        List<File> roots = new ArrayList<>();
        roots.add(rootA);
        roots.add(rootB);
        List<WorldBackupPlan.WorldEntry> worlds = WorldBackupDiscovery.discover(roots);

        assertEquals(2, worlds.size());
        assertEquals("Third World", worlds.get(0).name);
        assertEquals("World One", worlds.get(1).name);
    }

    @Test
    public void emptyOrNullRootsProduceNothing() {
        assertTrue(WorldBackupDiscovery.discover(null).isEmpty());
        assertTrue(WorldBackupDiscovery.discover(new ArrayList<>()).isEmpty());
        List<File> missing = new ArrayList<>();
        missing.add(new File(temp.getRoot(), "does-not-exist"));
        assertTrue(WorldBackupDiscovery.discover(missing).isEmpty());
    }

    @Test
    public void duplicateWorldNamesAcrossProfilesAreAllIncluded() throws Exception {
        File rootA = world("profileA", "SameName");
        File rootB = world("profileB", "SameName");
        List<File> roots = new ArrayList<>();
        roots.add(rootA);
        roots.add(rootB);
        assertEquals(2, WorldBackupDiscovery.discover(roots).size());
    }
}