package org.chimeramc.client.core.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.chimeramc.client.core.mods.Mod;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Pins the bundled-pack sync's failure contract (no device, no mocks).
 *
 * <p>The installer writes into every candidate game-data root because the game picks its storage
 * from the instance's isolation setting and internal/external preference, so only one root is
 * actually read. It used to throw when any root failed, and the launch path turned that into a
 * fatal "Failed to prepare bundled native-mod packs" — so a candidate root the game would never
 * read could stop a session entirely. These tests pin that the sync reports instead of throwing,
 * and that a root which does work still receives the packs.
 */
public class BundledResourcePackInstallerTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private static final String PACK_UUID = "3f1e2d4c-5b6a-4788-99aa-bbccddeeff00";

    /** A mod whose bundled resource pack exists on disk, so discovery finds something real. */
    private Mod modWithBundledResourcePack(File modRoot, String modId, boolean enabled)
            throws Exception {
        File packDir = new File(new File(modRoot, "resources/minecraft_resource_packs"), "pack_one");
        assertTrue(packDir.mkdirs());
        writeManifest(new File(packDir, "manifest.json"), PACK_UUID, "resources");
        return new Mod(modId, modId + ".so", "lib.so", modId,
                Collections.<String>emptyList(), modRoot.getAbsolutePath(), null,
                false, 0, enabled, 0);
    }

    private static void writeManifest(File target, String uuid, String moduleType)
            throws Exception {
        JsonObject header = new JsonObject();
        header.addProperty("uuid", uuid);
        JsonArray version = new JsonArray();
        version.add(1);
        version.add(0);
        version.add(0);
        header.add("version", version);

        JsonObject module = new JsonObject();
        module.addProperty("type", moduleType);
        module.addProperty("uuid", "11111111-2222-3333-4444-555555555555");

        JsonArray modules = new JsonArray();
        modules.add(module);

        JsonObject document = new JsonObject();
        document.add("header", header);
        document.add("modules", modules);

        try (FileWriter writer = new FileWriter(target, false)) {
            writer.write(document.toString());
        }
    }

    private Set<String> loaded(String... ids) {
        return new HashSet<>(java.util.Arrays.asList(ids));
    }

    /** A directory that cannot be written to, so {@code ensureDirectory} fails inside the sync. */
    private File unwritableRoot(String name) throws Exception {
        File root = temp.newFolder(name);
        assertTrue("test setup needs the root to be made read-only", root.setWritable(false, false));
        return root;
    }

    @Test
    public void aFailingCandidateRootDoesNotAbortTheSync() throws Exception {
        File modRoot = temp.newFolder("modroot");
        Mod mod = modWithBundledResourcePack(modRoot, "packmod", true);
        File good = temp.newFolder("good");
        File bad = unwritableRoot("bad");

        BundledResourcePackInstaller.Result result =
                BundledResourcePackInstaller.sync(Collections.singletonList(mod), loaded("packmod"), good, bad);

        assertFalse("a root the game may never read must not fail the sync outright", result.isComplete());
        assertTrue(result.succeededRoots.contains(good));
        assertTrue(result.failedRoots.contains(bad));
        assertEquals(1, result.packCount);
        // The writable root still received the pack, so the session it serves is unaffected.
        assertTrue(new File(good, "resource_packs/chimeralauncher_rp_" + PACK_UUID).isDirectory());
    }

    @Test
    public void theSyncNeverThrowsEvenWhenEveryRootFails() throws Exception {
        File modRoot = temp.newFolder("modroot2");
        Mod mod = modWithBundledResourcePack(modRoot, "packmod", true);

        BundledResourcePackInstaller.Result result = BundledResourcePackInstaller.sync(
                Collections.singletonList(mod), loaded("packmod"),
                unwritableRoot("bad1"), unwritableRoot("bad2"));

        assertFalse(result.isComplete());
        assertEquals(2, result.failedRoots.size());
        assertTrue(result.succeededRoots.isEmpty());
        // The first error is retained and the second is attached, so nothing is silently dropped.
        assertTrue(result.firstError != null);
        assertEquals(1, result.firstError.getSuppressed().length);
    }

    @Test
    public void everyCandidateRootSucceedsWhenAllAreWritable() throws Exception {
        File modRoot = temp.newFolder("modroot3");
        Mod mod = modWithBundledResourcePack(modRoot, "packmod", true);
        File a = temp.newFolder("a");
        File b = temp.newFolder("b");

        BundledResourcePackInstaller.Result result =
                BundledResourcePackInstaller.sync(Collections.singletonList(mod), loaded("packmod"), a, b);

        assertTrue(result.isComplete());
        assertEquals(2, result.succeededRoots.size());
        assertTrue(result.firstError == null);
        assertTrue(new File(a, "resource_packs/chimeralauncher_rp_" + PACK_UUID).isDirectory());
        assertTrue(new File(b, "resource_packs/chimeralauncher_rp_" + PACK_UUID).isDirectory());
    }

    @Test
    public void thePackIsRegisteredInTheGlobalListAndTheManagedState() throws Exception {
        File modRoot = temp.newFolder("modroot4");
        Mod mod = modWithBundledResourcePack(modRoot, "packmod", true);
        File root = temp.newFolder("root4");

        BundledResourcePackInstaller.sync(Collections.singletonList(mod), loaded("packmod"), root);

        File global = new File(root, "minecraftpe/global_resource_packs.json");
        assertTrue("the game only applies a pack listed here", global.isFile());
        JsonArray entries = JsonParser.parseString(
                new String(Files.readAllBytes(global.toPath()), StandardCharsets.UTF_8)).getAsJsonArray();
        assertEquals(1, entries.size());
        assertEquals(PACK_UUID, entries.get(0).getAsJsonObject().get("pack_id").getAsString());
        // The version must be a three-number array; a string is not comparable and the pack is
        // silently dropped by the game.
        JsonArray version = entries.get(0).getAsJsonObject().getAsJsonArray("version");
        assertEquals(3, version.size());
        assertTrue(version.get(0).isJsonPrimitive() && version.get(0).getAsJsonPrimitive().isNumber());

        assertTrue(new File(root, "minecraftpe/chimeralauncher_bundled_mod_packs.json").isFile());
    }

    @Test
    public void aDisabledModsBundledPackIsNotInstalled() throws Exception {
        File modRoot = temp.newFolder("modroot5");
        Mod mod = modWithBundledResourcePack(modRoot, "packmod", false);
        File root = temp.newFolder("root5");

        BundledResourcePackInstaller.Result result =
                BundledResourcePackInstaller.sync(Collections.singletonList(mod), loaded("packmod"), root);

        assertEquals(0, result.packCount);
        assertFalse(new File(root, "resource_packs/chimeralauncher_rp_" + PACK_UUID).exists());
    }

    @Test
    public void aModThatFailedToLoadDoesNotContributeItsPack() throws Exception {
        File modRoot = temp.newFolder("modroot6");
        Mod mod = modWithBundledResourcePack(modRoot, "packmod", true);
        File root = temp.newFolder("root6");

        // loadedModIds is the set the loader actually brought up; a mod that failed to load is
        // absent, and its bundled pack must not be installed as though it had.
        BundledResourcePackInstaller.Result result =
                BundledResourcePackInstaller.sync(Collections.singletonList(mod), loaded(), root);

        assertEquals(0, result.packCount);
    }

    @Test
    public void aNullCandidateRootIsSkippedRatherThanFailingTheSync() throws Exception {
        File modRoot = temp.newFolder("modroot7");
        Mod mod = modWithBundledResourcePack(modRoot, "packmod", true);
        File root = temp.newFolder("root7");

        // The storage resolver returns null for a root it cannot resolve; that is "no such root",
        // not "a root that failed", so it must not appear in the failure list.
        BundledResourcePackInstaller.Result result =
                BundledResourcePackInstaller.sync(Collections.singletonList(mod), loaded("packmod"), null, root);

        assertTrue(result.isComplete());
        assertEquals(1, result.succeededRoots.size());
        assertEquals(0, result.failedRoots.size());
    }

    @Test
    public void aPackWithNoCompatibleModuleIsIgnored() throws Exception {
        File modRoot = temp.newFolder("modroot8");
        File packDir = new File(new File(modRoot, "resources/minecraft_resource_packs"), "wrong");
        assertTrue(packDir.mkdirs());
        // A behaviour module inside the *resource* pack root is not a resource pack.
        writeManifest(new File(packDir, "manifest.json"), PACK_UUID, "data");
        Mod mod = new Mod("packmod", "packmod.so", "lib.so", "packmod",
                Collections.<String>emptyList(), modRoot.getAbsolutePath(), null,
                false, 0, true, 0);
        File root = temp.newFolder("root8");

        BundledResourcePackInstaller.Result result =
                BundledResourcePackInstaller.sync(Collections.singletonList(mod), loaded("packmod"), root);

        assertEquals(0, result.packCount);
    }
}
