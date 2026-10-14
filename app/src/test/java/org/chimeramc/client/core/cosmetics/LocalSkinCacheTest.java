package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

/**
 * Pins {@link LocalSkinCache}: the round-trip, the missing-cache read, the identity hash and the
 * clear. The cache is what lets the launcher show the real skin before the game runs.
 */
public class LocalSkinCacheTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private static int[] atlas(int seed) {
        int[] a = new int[64 * 64];
        java.util.Arrays.fill(a, 0xFF000000 | seed);
        return a;
    }

    @Test
    public void aWrittenEntryRoundTrips() {
        LocalSkinCache.Entry entry = new LocalSkinCache.Entry(64, 64,
                SkinCaptureValidator.ArmModel.SLIM, "geometry.humanoid.customSlim",
                1234567L, "abc123", atlas(0x112233));
        assertTrue(LocalSkinCache.write(temp.getRoot(), entry));

        LocalSkinCache.Entry read = LocalSkinCache.read(temp.getRoot());
        assertNotNull(read);
        assertEquals(64, read.width);
        assertEquals(64, read.height);
        assertEquals(SkinCaptureValidator.ArmModel.SLIM, read.armModel);
        assertEquals("geometry.humanoid.customSlim", read.geometryId);
        assertEquals(1234567L, read.captureTimeMs);
        assertEquals("abc123", read.identityHash);
        assertEquals(0xFF112233, read.argb[0]);
        assertEquals(64 * 64, read.argb.length);
    }

    @Test
    public void aMissingCacheReadsAsNull() {
        assertNull(LocalSkinCache.read(temp.getRoot()));
        assertFalse(LocalSkinCache.exists(temp.getRoot()));
    }

    @Test
    public void aWideArmModelRoundTrips() {
        LocalSkinCache.Entry entry = new LocalSkinCache.Entry(64, 64,
                SkinCaptureValidator.ArmModel.WIDE, "geometry.humanoid.custom", 1L, "", atlas(1));
        assertTrue(LocalSkinCache.write(temp.getRoot(), entry));
        assertEquals(SkinCaptureValidator.ArmModel.WIDE,
                LocalSkinCache.read(temp.getRoot()).armModel);
    }

    @Test
    public void clearRemovesTheCache() {
        LocalSkinCache.write(temp.getRoot(), new LocalSkinCache.Entry(64, 64,
                SkinCaptureValidator.ArmModel.WIDE, "", 1L, "", atlas(2)));
        assertTrue(LocalSkinCache.exists(temp.getRoot()));
        LocalSkinCache.clear(temp.getRoot());
        assertFalse(LocalSkinCache.exists(temp.getRoot()));
        assertNull(LocalSkinCache.read(temp.getRoot()));
    }

    @Test
    public void aCorruptBinReadsAsNull() {
        File dir = LocalSkinCache.dir(temp.getRoot());
        try (java.io.FileOutputStream out =
                     new java.io.FileOutputStream(new File(dir, LocalSkinCache.BIN_NAME))) {
            out.write(new byte[]{1, 2, 3});  // too short for the header
        } catch (java.io.IOException ignored) {
        }
        assertNull(LocalSkinCache.read(temp.getRoot()));
    }

    @Test
    public void theIdentityHashIsStableAndNonEmpty() {
        String a = LocalSkinCache.identityHash("xuid-12345");
        String b = LocalSkinCache.identityHash("xuid-12345");
        String c = LocalSkinCache.identityHash("xuid-99999");
        assertEquals(a, b);
        assertFalse(a.equals(c));
        assertFalse(a.isEmpty());
        assertEquals("", LocalSkinCache.identityHash(null));
    }

    @Test
    public void theJsonFieldExtractionIsSafe() {
        String json = "{\"geometry\":\"geometry.humanoid.custom\",\"identity\":\"abc\"}";
        assertEquals("geometry.humanoid.custom", LocalSkinCache.jsonString(json, "geometry"));
        assertEquals("abc", LocalSkinCache.jsonString(json, "identity"));
        assertEquals("", LocalSkinCache.jsonString(json, "missing"));
        assertEquals("", LocalSkinCache.jsonString(null, "geometry"));
    }
}
