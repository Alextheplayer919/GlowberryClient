package org.chimeramc.client.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WorldBackupNamingTest {

    @Test
    public void forWorldProducesTimestampedMcworld() {
        String name = WorldBackupNaming.forWorld("My Survival", 1_700_000_000_000L);
        assertTrue(name.startsWith("My Survival_"));
        assertTrue(name.endsWith(".mcworld"));
        assertEquals("_".length() + 15 + ".mcworld".length(),
                name.length() - "My Survival".length());
    }

    @Test
    public void sanitizeWorldNameStripsPathSeparatorsAndControlChars() {
        assertEquals("a_b_c", WorldBackupNaming.sanitizeWorldName("a/b\\c"));
        assertEquals("A_B_C", WorldBackupNaming.sanitizeWorldName("A:B*C"));
        assertEquals("world", WorldBackupNaming.sanitizeWorldName(null));
        assertEquals("world", WorldBackupNaming.sanitizeWorldName(""));
    }

    @Test
    public void sanitizeWorldNameTrimsAndCapsLength() {
        String longName = new String(new char[120]).replace('\0', 'x');
        assertEquals(80, WorldBackupNaming.sanitizeWorldName(longName).length());
    }

    @Test
    public void timestampOfRoundsTripsTheFormatter() {
        String name = WorldBackupNaming.forWorld("Test", 1_700_000_000_000L);
        long parsed = WorldBackupNaming.timestampOf(name);
        assertTrue(parsed > 0L);
        assertEquals(WorldBackupNaming.forWorld("Test", parsed), name);
    }

    @Test
    public void timestampOfRejectsNonBackupNames() {
        assertEquals(-1L, WorldBackupNaming.timestampOf(null));
        assertEquals(-1L, WorldBackupNaming.timestampOf("something.txt"));
        assertEquals(-1L, WorldBackupNaming.timestampOf("world.mcworld"));
        assertEquals(-1L, WorldBackupNaming.timestampOf("world_20240101.mcworld"));
    }

    @Test
    public void worldKeyOfStripsTheTimestamp() {
        String name = WorldBackupNaming.forWorld("Nether Hub", 1_700_000_000_000L);
        assertEquals("Nether Hub", WorldBackupNaming.worldKeyOf(name));
        assertEquals("", WorldBackupNaming.worldKeyOf("random.bin"));
        assertEquals("", WorldBackupNaming.worldKeyOf(null));
    }
}