package org.chimeramc.client.core.cosmetics;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The hand-designed cape textures, keyed by catalogue cape id, loaded from the pack's assets.
 *
 * <p>The procedural {@link CapeTexturePainter} generates a weave from a palette and a pattern, which
 * is fine for the hundred-plus palette variants but reads as a template. These are real authored
 * images (see {@code resources/cosmetics/capes}) drawn at the vanilla 64x32 cape layout, so a cape
 * can carry artwork a generator cannot. The catalogue id is the file base name, so
 * {@code aurora_borealis} maps to {@code aurora_borealis.png} with no separate table.
 *
 * <p>{@link #IDS} is the single source of truth for which capes are authored; the catalogue and the
 * in-game route both read it, so a file and a catalogue entry cannot drift. An id that is not here
 * keeps the procedural texture.
 */
public final class AuthoredCapeTextures {

    /** Asset directory the authored cape images live in. */
    public static final String DIR = "cosmetics/capes";

    /** Every authored cape id; any other cape keeps the procedural texture. */
    public static final Set<String> IDS = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
            "aurora_borealis", "ember_forge", "glacial_veil", "void_rift", "solar_flare",
            "midnight_rose", "toxic_bloom", "deep_current", "golden_ash", "phantom_shroud",
            "crimson_tide", "frostbite", "obsidian_wing", "meadow_light", "storm_caller",
            "royal_scepter", "shadow_fade", "coral_reef", "neon_grid", "bronze_scale",
            "skyline", "amethyst_geode", "honey_drift", "ink_brush"
    )));

    private AuthoredCapeTextures() {
    }

    /** True when a catalogue cape id has an authored texture. */
    public static boolean hasAuthoredTexture(String capeId) {
        return capeId != null && IDS.contains(capeId);
    }

    /** The authored texture file for a cape id, or null when it has none. */
    public static String fileFor(String capeId) {
        return hasAuthoredTexture(capeId) ? capeId + ".png" : null;
    }

    /**
     * Loads a cape's authored PNG from the pack assets.
     *
     * @return the PNG bytes, or null when the id has no authored texture or the asset is missing
     */
    public static byte[] loadFromAssets(AuthoredGeometry.AssetOpener assets, String capeId) {
        String file = fileFor(capeId);
        if (assets == null || file == null) return null;
        try (InputStream input = assets.open(DIR + "/" + file)) {
            if (input == null) return null;
            return readFully(input);
        } catch (IOException | RuntimeException e) {
            // An absent asset is a fallback to the procedural texture, never an error.
            return null;
        }
    }

    /** Loads a cape's authored PNG from a directory on disk (used by the JVM tests). */
    public static byte[] loadFromDir(File dir, String capeId) {
        String file = fileFor(capeId);
        if (dir == null || file == null) return null;
        File target = new File(dir, file);
        if (!target.isFile()) return null;
        try (InputStream input = new FileInputStream(target)) {
            return readFully(input);
        } catch (IOException e) {
            return null;
        }
    }

    private static byte[] readFully(InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
        byte[] buffer = new byte[4096];
        int read;
        while ((read = input.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
