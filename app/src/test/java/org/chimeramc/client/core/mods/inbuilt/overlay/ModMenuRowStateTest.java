package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the row-state machine and its UI consequences (M10, and G8 in UI form).
 *
 * <p>The order of the checks matters: a module that never registered cannot also be "unsupported",
 * and a crash-disabled module must read as crashed rather than as a plain user disable.
 */
public class ModMenuRowStateTest {

    @Test
    public void anUnregisteredModuleReadsAsFailed() {
        assertEquals(ModMenuRowState.FAILED,
                ModMenuRowState.of(false, false, false, false));
    }

    @Test
    public void aCrashDisabledModuleReadsAsCrashedNotAsAUserDisable() {
        assertEquals(ModMenuRowState.CRASHED,
                ModMenuRowState.of(true, true, true, false));
    }

    @Test
    public void anUnavailableModuleReadsAsUnsupported() {
        assertEquals(ModMenuRowState.UNSUPPORTED,
                ModMenuRowState.of(true, false, false, true));
    }

    @Test
    public void aHealthyModuleReadsAsLoadedOrDisabled() {
        assertEquals(ModMenuRowState.LOADED, ModMenuRowState.of(true, true, false, true));
        assertEquals(ModMenuRowState.DISABLED, ModMenuRowState.of(true, true, false, false));
    }

    @Test
    public void failedBeatsCrashedAndUnsupported() {
        // Registration is the outermost failure: nothing else can be true about a module that never
        // made it into the registry.
        assertEquals(ModMenuRowState.FAILED, ModMenuRowState.of(false, true, true, true));
    }

    @Test
    public void onlyHealthyStatesAreInteractive() {
        assertTrue(ModMenuRowState.LOADED.isInteractive());
        assertTrue(ModMenuRowState.DISABLED.isInteractive());
        assertFalse(ModMenuRowState.FAILED.isInteractive());
        assertFalse(ModMenuRowState.UNSUPPORTED.isInteractive());
        assertFalse(ModMenuRowState.CRASHED.isInteractive());
    }

    @Test
    public void onlyBrokenStatesAreDimmedAndOfferAReason() {
        for (ModMenuRowState state : ModMenuRowState.values()) {
            assertEquals(state.isBroken(), state.isDimmed());
            assertEquals(state.isBroken(), state.hasReason());
        }
        assertFalse(ModMenuRowState.LOADED.isBroken());
        assertFalse(ModMenuRowState.DISABLED.isBroken());
    }

    @Test
    public void everyBrokenStateHasADotColour() {
        for (ModMenuRowState state : ModMenuRowState.values()) {
            if (state.isBroken()) {
                assertTrue(state + " has a dot", ModMenuPalette.dotFor(state) != 0);
            } else {
                assertEquals(state + " has no dot", 0, ModMenuPalette.dotFor(state));
            }
        }
    }
}
