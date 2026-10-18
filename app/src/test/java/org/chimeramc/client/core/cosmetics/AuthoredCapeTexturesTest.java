package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/**
 * The authored cape set: real 64x32 images with hand-written names.
 *
 * <p>The generated palette families are recolours of one weave; the authored set is the artwork a
 * generator cannot produce. These tests pin the two invariants that keep the set honest: every
 * authored id has an actual asset file, and every asset is a valid 2:1 cape panel (which the engine
 * requires to recognise it at the loader seam).
 */
public class AuthoredCapeTexturesTest {

    private static File assetDir() {
        // Walk up from the test working directory until the repo root (which holds both app/ and
        // resources/) is found, so the test works whether Gradle runs from the root or the module.
        File dir = new File("").getAbsoluteFile();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (new File(dir, "app").isDirectory() && new File(dir, "resources").isDirectory()) {
                return new File(dir, "resources/cosmetics/capes");
            }
            dir = dir.getParentFile();
        }
        return new File("resources/cosmetics/capes").getAbsoluteFile();
    }

    @Test
    public void everyAuthoredIdHasAnAssetFile() {
        File dir = assetDir();
        assertTrue("the authored cape directory must exist", dir.isDirectory());
        for (String id : AuthoredCapeTextures.IDS) {
            File file = new File(dir, id + ".png");
            assertTrue("missing authored cape asset: " + file, file.isFile());
            assertTrue("empty authored cape asset: " + file, file.length() > 0);
        }
    }

    @Test
    public void everyAuthoredCapeIsInTheCatalogue() {
        Set<String> catalogueIds = new HashSet<>();
        for (CosmeticCatalog.Cape cape : CosmeticCatalog.capes()) {
            catalogueIds.add(cape.id);
        }
        for (String id : AuthoredCapeTextures.IDS) {
            assertTrue("authored cape not in the catalogue: " + id, catalogueIds.contains(id));
        }
    }

    @Test
    public void everyAuthoredCapeHasAHandWrittenName() {
        Set<String> names = new HashSet<>();
        for (CosmeticCatalog.Cape cape : CosmeticCatalog.capes()) {
            if (!AuthoredCapeTextures.hasAuthoredTexture(cape.id)) continue;
            assertFalse("authored cape has a placeholder name: " + cape.name, cape.name.isEmpty());
            // A hand-written name is a real title, not a "<palette> <pattern>" template.
            assertFalse("authored cape name looks generated: " + cape.name,
                    cape.name.contains("_") || cape.name.contains("  "));
            assertTrue("two capes share the name " + cape.name, names.add(cape.name));
        }
    }

    @Test
    public void everyAuthoredAssetIsATwoToOnePng() {
        for (String id : AuthoredCapeTextures.IDS) {
            byte[] png = AuthoredCapeTextures.loadFromDir(assetDir(), id);
            assertNotNull("could not read " + id, png);
            assertTrue("not a PNG: " + id, png.length > 24);
            assertEquals((byte) 0x89, png[0]);
            assertEquals('P', png[1]);
            assertEquals('N', png[2]);
            assertEquals('G', png[3]);
            int width = readBe32(png, 16);
            int height = readBe32(png, 20);
            assertEquals("authored cape " + id + " must be 64 px wide", 64, width);
            assertEquals("authored cape " + id + " must be 32 px tall", 32, height);
            assertEquals("authored cape " + id + " is not a 2:1 panel", width, 2 * height);
        }
    }

    @Test
    public void anUnknownIdHasNoAuthoredTexture() {
        assertFalse(AuthoredCapeTextures.hasAuthoredTexture(null));
        assertFalse(AuthoredCapeTextures.hasAuthoredTexture("not_a_cape"));
        assertFalse(AuthoredCapeTextures.hasAuthoredTexture("chimera"));
    }

    private static int readBe32(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24) | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8) | (bytes[offset + 3] & 0xFF);
    }
}
