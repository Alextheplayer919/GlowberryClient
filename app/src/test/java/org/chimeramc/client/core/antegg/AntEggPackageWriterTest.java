package org.chimeramc.client.core.antegg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * Pins the generated .AntEgg package against the real reader.
 *
 * <p>The writer's whole value is that a ported mod loads through the same pipeline an ordinary
 * import uses, so the test does not just check the bytes: it writes a package and reads it back
 * with {@link AntEggPackage#inspect} and {@link AntEggPackage#extract}. A manifest that the writer
 * emits but the parser rejects would be a silent failure that only shows up on a user's device.
 */
public class AntEggPackageWriterTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static AntEggManifest portedManifest() {
        return AntEggManifest.ported("Cool Mod", "1.2.0", "JavaBridge (AI port)", "script",
                "cool-mod.lua", "AI-ported from a Java Edition mod.",
                "coolmod 1.2.0", "Alice");
    }

    @Test
    public void writesAPackageTheReaderAccepts() throws Exception {
        File destination = new File(folder.getRoot(), "cool-mod.antegg");
        AntEggPackageWriter.PackageFiles files = new AntEggPackageWriter.PackageFiles()
                .add("cool-mod.lua", "print('hello')\n");
        AntEggPackageWriter.Result result =
                AntEggPackageWriter.write(portedManifest(), files, destination);
        assertTrue(result.error, result.success);
        assertTrue(destination.isFile());

        AntEggPackage.Result inspected = AntEggPackage.inspect(destination);
        assertTrue(inspected.isValid());
        assertEquals("Cool Mod", inspected.manifest.name);
        assertEquals("cool-mod.lua", inspected.manifest.entryPoint);
    }

    @Test
    public void preservesProvenanceThroughTheRoundTrip() throws Exception {
        File destination = new File(folder.getRoot(), "p.antegg");
        AntEggPackageWriter.write(portedManifest(),
                new AntEggPackageWriter.PackageFiles().add("cool-mod.lua", "x"), destination);

        AntEggPackage.Result extracted =
                AntEggPackage.extract(destination, folder.newFolder("sandbox"));
        assertTrue(extracted.isValid());
        AntEggManifest manifest = extracted.manifest;
        assertTrue("a ported package must be marked aiPorted", manifest.aiPorted);
        assertEquals("coolmod 1.2.0", manifest.originMod);
        assertEquals("Alice", manifest.originAuthor);
    }

    @Test
    public void extractsTheEntryPointToDisk() throws Exception {
        File destination = new File(folder.getRoot(), "p.antegg");
        AntEggPackageWriter.write(portedManifest(),
                new AntEggPackageWriter.PackageFiles().add("cool-mod.lua", "print(1)"),
                destination);
        File sandbox = folder.newFolder("sandbox");
        AntEggPackage.Result extracted = AntEggPackage.extract(destination, sandbox);
        assertTrue(extracted.isValid());
        assertTrue(new File(extracted.directory, "cool-mod.lua").isFile());
    }

    @Test
    public void refusesWhenTheEntryPointIsNotAmongTheFiles() {
        AntEggPackageWriter.Result result = AntEggPackageWriter.write(portedManifest(),
                new AntEggPackageWriter.PackageFiles().add("other.lua", "x"),
                new File(folder.getRoot(), "p.antegg"));
        assertFalse(result.success);
        assertNotNull(result.error);
    }

    @Test
    public void refusesAnEmptyPackage() {
        assertFalse(AntEggPackageWriter.write(portedManifest(),
                new AntEggPackageWriter.PackageFiles(), new File(folder.getRoot(), "p.antegg"))
                .success);
    }

    @Test
    public void refusesANullManifest() {
        assertFalse(AntEggPackageWriter.write(null,
                new AntEggPackageWriter.PackageFiles().add("a.lua", "x"),
                new File(folder.getRoot(), "p.antegg")).success);
    }

    @Test
    public void doesNotLeaveAFileWhenTheManifestIsRejected() {
        // A native manifest whose entry point is not a .so is invalid; nothing should be written.
        AntEggManifest bad = AntEggManifest.ported("Bad", "1.0.0", "a", "native",
                "bad.lua", "", "", "");
        File destination = new File(folder.getRoot(), "bad.antegg");
        AntEggPackageWriter.Result result = AntEggPackageWriter.write(bad,
                new AntEggPackageWriter.PackageFiles().add("bad.lua", "x"), destination);
        assertFalse(result.success);
        assertFalse(destination.exists());
    }

    @Test
    public void escapesManifestStringsSoTheJsonStaysValid() {
        AntEggManifest tricky = AntEggManifest.ported("Quote \" Mod", "1.0.0", "A\\B",
                "script", "q.lua", "line1\nline2", "", "");
        String json = AntEggPackageWriter.manifestJson(tricky);
        AntEggManifest.Result parsed = AntEggManifest.parse(json);
        assertTrue(json, parsed.isValid());
        assertEquals("Quote \" Mod", parsed.manifest.name);
        assertEquals("A\\B", parsed.manifest.author);
        assertEquals("line1\nline2", parsed.manifest.description);
    }

    @Test
    public void omitsProvenanceFieldsForAnOrdinaryManifest() {
        // A manifest parsed from a human-written egg.json keeps aiPorted false and writes no
        // ai_ported field, so the writer cannot turn an ordinary mod into a ported-looking one.
        AntEggManifest ordinary = AntEggManifest.parse(
                "{\"name\":\"Hand Made\",\"version\":\"1.0.0\",\"author\":\"Me\","
                        + "\"type\":\"script\",\"entry_point\":\"m.lua\"}").manifest;
        assertNotNull(ordinary);
        assertFalse(ordinary.aiPorted);
        String json = AntEggPackageWriter.manifestJson(ordinary);
        assertFalse(json.contains("ai_ported"));
        assertFalse(json.contains("origin_mod"));
        assertFalse(json.contains("sha256"));
        // C3: the permissions slot is always written, even for a manifest that declares none, so
        // the script-API surface is reserved without a schema change.
        assertTrue(json, json.contains("\"permissions\""));
        assertTrue(json, json.contains("\"permissions\": []"));
    }

    @Test
    public void emitsSha256AndPermissionsOnlyWhenPresent() {
        AntEggManifest pinned = AntEggManifest.parse(
                "{\"name\":\"Pinned\",\"version\":\"1.0.0\",\"author\":\"Me\","
                        + "\"type\":\"script\",\"entry_point\":\"m.lua\","
                        + "\"sha256\":\"" + "ab".repeat(32) + "\","
                        + "\"permissions\":[\"network\"]}").manifest;
        assertNotNull(pinned);
        String json = AntEggPackageWriter.manifestJson(pinned);
        assertTrue(json, json.contains("\"sha256\""));
        assertTrue(json, json.contains("\"permissions\""));
        assertTrue(json, json.contains("\"network\""));

        // Round trip: the writer's JSON must parse back to the same fields.
        AntEggManifest reparsed = AntEggManifest.parse(json).manifest;
        assertNotNull(reparsed);
        assertEquals(pinned.sha256, reparsed.sha256);
        assertEquals(1, reparsed.permissions.size());
        assertEquals("network", reparsed.permissions.get(0));
    }

    @Test
    public void aPinnedPackageVerifiesAndAnAlteredOneDoesNot() throws Exception {
        AntEggManifest base = AntEggManifest.parse(
                "{\"name\":\"Pinned Mod\",\"version\":\"1.0.0\",\"author\":\"Me\","
                        + "\"type\":\"script\",\"entry_point\":\"m.lua\"}").manifest;

        // The pin must reference the entry file (non-circular), not the whole archive. Compute
        // the entry bytes' hash first, pin it, then write and read the package back through the
        // real reader to confirm the round trip keeps the pin.
        String entryHash = ModHash.sha256(writeBytes(folder.getRoot(), "entry.dat",
                "print('x')".getBytes(StandardCharsets.UTF_8)));
        assertNotNull(entryHash);

        AntEggManifest pinned = base.withSha256(entryHash);
        AntEggPackageWriter.Result pinnedResult = AntEggPackageWriter.write(pinned,
                new AntEggPackageWriter.PackageFiles().add("m.lua", "print('x')"),
                new File(folder.getRoot(), "pinned.antegg"));
        assertTrue(pinnedResult.error, pinnedResult.success);

        AntEggPackage.Result extracted = AntEggPackage.extract(pinnedResult.file,
                folder.getRoot());
        assertTrue(extracted.error, extracted.isValid());
        File entry = new File(extracted.directory, "m.lua");
        assertTrue(ModHash.matches(entry, entryHash));

        // The same pin on a package whose entry bytes differ must not match.
        File other = new File(folder.getRoot(), "other.antegg");
        assertTrue(AntEggPackageWriter.write(pinned,
                new AntEggPackageWriter.PackageFiles().add("m.lua", "print('y')"), other).success);
        AntEggPackage.Result otherExtracted = AntEggPackage.extract(other, folder.getRoot());
        assertTrue(otherExtracted.error, otherExtracted.isValid());
        assertFalse(ModHash.matches(new File(otherExtracted.directory, "m.lua"), entryHash));
    }

    private static File writeBytes(File dir, String name, byte[] bytes) throws Exception {
        File file = new File(dir, name);
        java.nio.file.Files.write(file.toPath(), bytes);
        return file;
    }

    @Test
    public void writesNativePackagesWithASoEntryPoint() throws Exception {
        AntEggManifest nativeManifest = AntEggManifest.ported("Native Mod", "1.0.0", "JavaBridge",
                "native", "native-mod.so", "", "orig", "");
        File destination = new File(folder.getRoot(), "native-mod.antegg");
        AntEggPackageWriter.Result result = AntEggPackageWriter.write(nativeManifest,
                new AntEggPackageWriter.PackageFiles().addBytes("native-mod.so", new byte[]{0, 1}),
                destination);
        assertTrue(result.error, result.success);
        assertTrue(AntEggPackage.inspect(destination).isValid());
    }
}
