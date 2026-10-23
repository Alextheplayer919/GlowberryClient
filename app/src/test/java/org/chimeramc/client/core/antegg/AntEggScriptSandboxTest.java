package org.chimeramc.client.core.antegg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Pins the script-mod sandbox (item G7).
 *
 * <p>An .AntEgg script is attacker-controlled bytes, so the sandbox must be proven to close the
 * host escapes LuaJ's desktop globals open: {@code os.exit}, {@code os.execute},
 * {@code io.open} on arbitrary files, {@code dofile}/{@code loadfile} on arbitrary paths, and
 * {@code luajava}. It must also abort a runaway loop instead of hanging the import thread. These
 * tests run the real Lua VM (no mocks) and assert either a Lua error or a terminated run.
 */
public class AntEggScriptSandboxTest {

    private static File modDir() throws Exception {
        File dir = Files.createTempDirectory("antegg-sandbox-mod").toFile();
        dir.deleteOnExit();
        return dir;
    }

    private static File writeEntry(File dir, String name, String content) throws Exception {
        File file = new File(dir, name);
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    /** Runs source in a hardened sandbox and returns the thrown LuaError, or fails the test. */
    private static LuaError runExpectingError(File dir, String source) {
        Globals globals = AntEggScriptSandbox.build(dir, null);
        try {
            globals.load(source, "@test.lua").call();
            throw new AssertionError("script was expected to fail but succeeded");
        } catch (LuaError e) {
            return e;
        }
    }

    @Test
    public void osIoAndLuajavaAreNotExposed() throws Exception {
        Globals globals = AntEggScriptSandbox.build(null, null);
        assertTrue("os must be absent", globals.get("os").isnil());
        assertTrue("io must be absent", globals.get("io").isnil());
        assertTrue("luajava must be absent", globals.get("luajava").isnil());
        assertTrue("debug must be hidden (budget hook protection)", globals.get("debug").isnil());
    }

    @Test
    public void osExitIsAPlainLuaError() throws Exception {
        LuaError error = runExpectingError(null, "os.exit(9)");
        assertTrue(error.getMessage(), error.getMessage().contains("nil"));
    }

    @Test
    public void osExecuteIsBlocked() throws Exception {
        LuaError error = runExpectingError(null, "os.execute('echo pwned')");
        assertTrue(error.getMessage(), error.getMessage().contains("nil"));
    }

    @Test
    public void ioOpenCannotReadArbitraryFiles() throws Exception {
        LuaError error = runExpectingError(null,
                "local f = io.open('/etc/hosts', 'r') return f:read('*a')");
        assertTrue(error.getMessage(), error.getMessage().contains("nil"));
    }

    @Test
    public void dofileIsDisabled() throws Exception {
        LuaError error = runExpectingError(null, "return dofile('/etc/hosts')");
        assertTrue(error.getMessage(), error.getMessage().contains("disabled"));
    }

    @Test
    public void loadfileIsDisabled() throws Exception {
        LuaError error = runExpectingError(null, "return loadfile('/etc/hosts')");
        assertTrue(error.getMessage(), error.getMessage().contains("disabled"));
    }

    @Test
    public void aRunawayLoopTerminatesInsteadOfHanging() throws Exception {
        Globals globals = AntEggScriptSandbox.build(null, null);
        long startedAt = System.currentTimeMillis();
        try {
            globals.load("while true do end", "@runaway.lua").call();
            throw new AssertionError("runaway script was supposed to be aborted");
        } catch (LuaError e) {
            long elapsedMs = System.currentTimeMillis() - startedAt;
            assertTrue("abort took " + elapsedMs + "ms; it should be fast",
                    elapsedMs < 5_000L);
            assertTrue(e.getMessage(), e.getMessage().contains("budget"));
        }
    }

    @Test
    public void normalLuaCodeStillRuns() throws Exception {
        Globals globals = AntEggScriptSandbox.build(modDir(), null);
        LuaValue result = globals.load(
                "local s = 0 for i = 1, 100 do s = s + i end return s", "@good.lua").call();
        assertEquals("sum of 1..100", LuaValue.valueOf(5050), result);
    }

    @Test
    public void requireWorksInsideTheModDirectory() throws Exception {
        File dir = modDir();
        writeEntry(dir, "helper.lua",
                "return { twice = function(x) return x * 2 end }");
        File data = new File(dir, "data");
        assertTrue(data.mkdirs());
        writeEntry(data, "nested.lua", "return 'from-data'");

        Globals globals = AntEggScriptSandbox.build(dir, new LuaTable());
        assertEquals(LuaValue.valueOf(42),
                globals.load("local h = require('helper') return h.twice(21)", "@use.lua").call());
        assertEquals(LuaValue.valueOf("from-data"),
                globals.load("return require('nested')", "@use-data.lua").call());
    }

    @Test
    public void requireCannotEscapeTheModDirectory() throws Exception {
        // A host file that must never be readable through require.
        File outside = File.createTempFile("antegg-host-secret", ".lua");
        outside.deleteOnExit();
        Files.write(outside.toPath(), "return 'HOST-CONTENT'".getBytes(StandardCharsets.UTF_8));

        File dir = modDir();
        LuaError error = runExpectingError(dir, "return require('" + outside.getName() + "')");
        assertTrue(error.getMessage(), error.getMessage().contains("not found"));
    }

    @Test
    public void requireCannotTraverseUpwards() throws Exception {
        File dir = modDir();
        LuaError error = runExpectingError(dir, "return require('../../antegg-host-secret')");
        assertFalse("a traversal require must not reach files above the mod directory",
                error.getMessage().contains("HOST-CONTENT"));
    }

    @Test
    public void theModTableStillReachesTheScript() throws Exception {
        File dir = modDir();
        LuaTable mod = new LuaTable();
        mod.set("name", "Sandboxed");
        Globals globals = AntEggScriptSandbox.build(dir, mod);
        LuaValue result = globals.load("return mod.name", "@mod.lua").call();
        assertEquals(LuaValue.valueOf("Sandboxed"), result);
    }
}