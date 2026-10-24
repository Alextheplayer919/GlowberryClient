package org.chimeramc.client.core.antegg;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.CoroutineLib;
import org.luaj.vm2.lib.MathLib;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.TwoArgFunction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Hardened Lua VM for {@code type: "script"} .AntEgg mods (item G7).
 *
 * <p>LuaJ's {@code JsePlatform.standardGlobals()} is a full desktop surface: it wires
 * {@code os.execute} to {@code Runtime.exec}, {@code os.exit} to {@code System.exit},
 * {@code io.open} to arbitrary file access and {@code luajava} to reflection on the host
 * classpath. An .AntEgg script is attacker-controlled bytes, so running it on the stock globals
 * gives that script the launcher's own process. This sandbox closes every one of those escapes
 * while keeping the language itself intact:
 *
 * <ul>
 *   <li><b>No {@code os}, no {@code io}, no {@code luajava}.</b> The globals are built from the
 *       pure libraries only (base, package, bit32, table, string, coroutine, math) plus the
 *       debug library for the budget hook. A script that calls {@code os.exit(9)} or
 *       {@code io.open(secretFile)} gets a normal Lua runtime error.</li>
 *   <li><b>Hard instruction budget.</b> A {@code debug.sethook(…, "", N)} count hook raises a
 *       Lua error when the script has executed more than {@link #INSTRUCTION_BUDGET} VM
 *       instructions, so {@code while true do end} aborts in tens of milliseconds instead of
 *       hanging the import thread forever. {@code debug} is then removed from the globals so the
 *       script cannot see or clear the hook.</li>
 *   <li><b>{@code dofile} / {@code loadfile} disabled.</b> The base library's file loaders read
 *       any path on the device; they are replaced with functions that raise a Lua error.</li>
 *   <li><b>{@code require} confined to the mod directory.</b> A module is only ever loaded from
 *       {@code <modDir>} or {@code <modDir>/data} (the sandbox the manifest promises); a
 *       traversal or absolute path cannot reach outside it, because {@code name} is joined to
 *       those roots exactly as written and the resulting candidate must be a regular file under
 *       one of them.</li>
 * </ul>
 *
 * <p>The surface is deliberately documented: {@link AntEggScriptHost#exposedStandardLibraries}
 * names what a script author can rely on, and changing the set here changes that list.
 */
public final class AntEggScriptSandbox {

    /** Max VM instructions a single script run may execute before it is aborted. */
    static final int INSTRUCTION_BUDGET = 250_000;

    private AntEggScriptSandbox() {
    }

    /** Builds a hardened {@link Globals} with the given mod table already installed. */
    static Globals build(File modDirectory, LuaValue modTable) {
        Globals globals = new Globals();
        globals.load(new org.luaj.vm2.lib.BaseLib());
        globals.load(new PackageLib());
        globals.load(new Bit32Lib());
        globals.load(new TableLib());
        globals.load(new StringLib());
        globals.load(new CoroutineLib());
        globals.load(new MathLib());
        globals.load(new org.luaj.vm2.lib.DebugLib());
        org.luaj.vm2.compiler.LuaC.install(globals);

        if (modTable != null) {
            globals.set("mod", modTable);
        }

        // Instruction budget: a count hook fires after INSTRUCTION_BUDGET instructions and raises
        // a Lua error. debug is hidden afterwards so the script cannot clear or replace the hook.
        LuaValue budgetHook = globals.load(
                "return function() error('instruction budget exhausted', 0) end",
                "@budget.lua").call();
        globals.get("debug").get("sethook")
                .call(budgetHook, LuaValue.valueOf(""), LuaValue.valueOf(INSTRUCTION_BUDGET));
        globals.set("debug", LuaValue.NIL);

        // BaseLib still ships dofile/loadfile which read arbitrary paths. Provide inert stubs.
        globals.set("dofile", fileLoaderStub("dofile"));
        globals.set("loadfile", fileLoaderStub("loadfile"));

        if (modDirectory != null) {
            globals.set("require", requireConfinedTo(modDirectory, globals));
        } else {
            // No mod directory (e.g. the raw-source test seam): requiring anything is pointless
            // because there is no package to load from, so it is disabled entirely.
            globals.set("require", fileLoaderStub("require"));
        }
        return globals;
    }

    private static LuaValue fileLoaderStub(final String name) {
        return new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue ignored) {
                throw new LuaError(name + " is disabled in the mod sandbox");
            }
        };
    }

    /**
     * A {@code require} that only resolves modules under the mod directory (or its {@code data}
     * subdirectory). The name is treated as a plain file name; separators are legal within a
     * module path but the join is never allowed to climb above the root because the candidate is
     * built by concatenation and {@link File#isFile()} then has to agree with a file that is
     * actually present under the root.
     */
    private static LuaValue requireConfinedTo(final File modDirectory, final Globals globals) {
        final File[] roots = { modDirectory, new File(modDirectory, "data") };
        return new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue name) {
                String moduleName = name.tojstring();
                for (File root : roots) {
                    File candidate = new File(root, moduleName + ".lua");
                    if (candidate.isFile()) {
                        try {
                            String source = new String(
                                    Files.readAllBytes(candidate.toPath()), StandardCharsets.UTF_8);
                            return globals.load(source, "@" + moduleName + ".lua").call();
                        } catch (IOException e) {
                            throw new LuaError("could not read module " + moduleName);
                        }
                    }
                }
                throw new LuaError("module '" + moduleName + "' not found in the mod directory");
            }
        };
    }
}