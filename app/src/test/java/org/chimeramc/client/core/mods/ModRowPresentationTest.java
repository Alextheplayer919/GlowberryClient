package org.chimeramc.client.core.mods;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The mod-row derivation (U17). Pure: no Android, no layout.
 */
public class ModRowPresentationTest {

    private static Mod mod(String entry, boolean enabled) {
        return new Mod("id", "mod.antegg", entry, "Example", enabled, 0);
    }

    @Test
    public void theKindComesFromTheEntryPoint() {
        assertEquals(ModRowPresentation.Kind.NATIVE, ModRowPresentation.kindOf(mod("lib/a.so", true)));
        assertEquals(ModRowPresentation.Kind.SCRIPT, ModRowPresentation.kindOf(mod("main.lua", true)));
        // Case is irrelevant on a case-insensitive filesystem.
        assertEquals(ModRowPresentation.Kind.NATIVE, ModRowPresentation.kindOf(mod("lib/A.SO", true)));
        assertEquals(ModRowPresentation.Kind.UNKNOWN, ModRowPresentation.kindOf(mod(null, true)));
        assertEquals(ModRowPresentation.Kind.UNKNOWN, ModRowPresentation.kindOf(mod("data.json", true)));
        assertEquals(ModRowPresentation.Kind.UNKNOWN, ModRowPresentation.kindOf(null));
    }

    @Test
    public void aRecordedFailureOutranksTheEnabledFlag() {
        ModLoadDiagnostics.Record failure = new ModLoadDiagnostics.Record(
                "id", "Example", "1.0", "cannot open lib", ModLoadDiagnostics.KIND_DLOPEN, 0L);
        // The mod is on and broken: the row must say failed, not enabled.
        assertEquals(ModRowPresentation.State.FAILED,
                ModRowPresentation.stateOf(mod("lib/a.so", true), failure));
        assertEquals(ModRowPresentation.State.FAILED,
                ModRowPresentation.stateOf(mod("lib/a.so", false), failure));
    }

    @Test
    public void withoutAFailureTheEnabledFlagDecides() {
        assertEquals(ModRowPresentation.State.ENABLED,
                ModRowPresentation.stateOf(mod("lib/a.so", true), null));
        assertEquals(ModRowPresentation.State.DISABLED,
                ModRowPresentation.stateOf(mod("lib/a.so", false), null));
    }

    @Test
    public void theFailureLineCarriesKindAndReason() {
        ModLoadDiagnostics.Record failure = new ModLoadDiagnostics.Record(
                "id", "Example", "1.0", "undefined symbol foo", ModLoadDiagnostics.KIND_SYMBOL, 0L);
        assertEquals("symbol: undefined symbol foo", ModRowPresentation.failureLine(failure));
    }

    @Test
    public void aBlankReasonStillYieldsTheKind() {
        ModLoadDiagnostics.Record blank = new ModLoadDiagnostics.Record(
                "id", "Example", "1.0", "   ", ModLoadDiagnostics.KIND_DLOPEN, 0L);
        assertEquals(ModLoadDiagnostics.KIND_DLOPEN, ModRowPresentation.failureLine(blank));
    }

    @Test
    public void noFailureMeansNoLine() {
        assertNull(ModRowPresentation.failureLine(null));
    }

    @Test
    public void aFailedModIsStillTogglable() {
        // Otherwise the user is trapped with a broken mod they cannot switch off.
        ModLoadDiagnostics.Record failure = new ModLoadDiagnostics.Record(
                "id", "Example", "1.0", "boom", ModLoadDiagnostics.KIND_DLOPEN, 0L);
        Mod broken = mod("lib/a.so", true);
        assertEquals(ModRowPresentation.State.FAILED,
                ModRowPresentation.stateOf(broken, failure));
        assertTrue(ModRowPresentation.toggleEnabled(broken));
        assertFalse(ModRowPresentation.toggleEnabled(null));
    }
}
