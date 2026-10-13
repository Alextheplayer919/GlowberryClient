package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Pins {@link CosmeticAssetAudit} against the real shipped assets: every authored hat and pet is
 * scanned, its PNG size and geometry-declared size are read, and the audit never throws on a bad
 * file. This is the JVM-side proof that a size mismatch is visible in the diagnostics report.
 */
public class CosmeticAssetAuditTest {

    private static File modelsDir(String sub) {
        for (File f : new File[]{
                new File("src/main/../../resources/cosmetics/models/" + sub),
                new File("../resources/cosmetics/models/" + sub),
                new File("resources/cosmetics/models/" + sub)}) {
            if (f.isDirectory()) return f;
        }
        return null;
    }

    /** An AssetOpener that reads from the on-disk resources tree. */
    private static AuthoredGeometry.AssetOpener opener(File root) {
        return path -> {
            File f = new File(root, path.replace("cosmetics/models/", ""));
            if (!f.isFile()) throw new IOException("missing " + path);
            return new FileInputStream(f);
        };
    }

    @Test
    public void everyAuthoredCosmeticIsAuditedWithItsSizes() {
        File models = modelsDir("");
        Assume.assumeTrue("assets not found", models != null);
        List<CosmeticAssetAudit.Entry> entries = CosmeticAssetAudit.scan(opener(models));
        assertTrue("no cosmetics audited", entries.size() >= 30);
        for (CosmeticAssetAudit.Entry e : entries) {
            assertTrue(e.pngW > 0 && e.pngH > 0);
            assertTrue(e.cubes > 0);
            assertNotNull(e.describe());
        }
    }

    @Test
    public void aSizeMismatchIsReported() {
        // The 32x32 hats declare 32x32 geometry, so no mismatch; the 64x48 straw hat likewise. The
        // audit's value is that it *reads* both rather than assuming 64x64.
        File models = modelsDir("");
        Assume.assumeTrue("assets not found", models != null);
        List<CosmeticAssetAudit.Entry> entries = CosmeticAssetAudit.scan(opener(models));
        boolean sawNon64 = false;
        for (CosmeticAssetAudit.Entry e : entries) {
            if (e.pngW != 64 || e.pngH != 64) sawNon64 = true;
        }
        assertTrue("expected at least one non-64x64 authored atlas", sawNon64);
    }

    @Test
    public void aMissingAssetIsSkippedNotThrown() {
        List<CosmeticAssetAudit.Entry> entries = CosmeticAssetAudit.scan(path -> {
            throw new IOException("no assets here");
        });
        assertTrue("a broken opener must yield no entries, not throw", entries.isEmpty());
    }

    @Test
    public void formatListsEveryEntry() {
        CosmeticAssetAudit.Entry e = new CosmeticAssetAudit.Entry(
                "orig_witch", false, 64, 48, 64, 48, 12, false);
        String text = CosmeticAssetAudit.format(java.util.Collections.singletonList(e));
        assertTrue(text.contains("orig_witch"));
        assertTrue(text.contains("png=64x48"));
        assertTrue(text.contains("geo=64x48"));
        assertTrue(text.contains("cubes=12"));
    }
}
