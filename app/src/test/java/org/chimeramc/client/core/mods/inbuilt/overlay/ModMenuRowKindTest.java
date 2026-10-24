package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the switch-vs-action classification (M2, M3).
 *
 * <p>This is the rule that fixes the "why does tapping my module do nothing visible" complaint: an
 * entry with an enable state is a switch, an entry that fires once is an action row, and nothing is
 * both.
 */
public class ModMenuRowKindTest {

    @Test
    public void aModuleWithAnEnableStateIsASwitch() {
        assertEquals(ModMenuRowKind.SWITCH, ModMenuRowKind.of(true));
        assertTrue(ModMenuRowKind.of(true).showsSwitch());
        assertFalse(ModMenuRowKind.of(true).showsChevron());
    }

    @Test
    public void aOneShotEntryIsAnActionRow() {
        assertEquals(ModMenuRowKind.ACTION, ModMenuRowKind.of(false));
        assertTrue(ModMenuRowKind.of(false).showsChevron());
        assertFalse(ModMenuRowKind.of(false).showsSwitch());
    }

    @Test
    public void noRowIsEverBothAControlAndAnAction() {
        assertFalse(ModMenuRowKind.mayCombine());
        for (ModMenuRowKind kind : ModMenuRowKind.values()) {
            assertFalse("a row is exactly one kind", kind.showsSwitch() && kind.showsChevron());
            assertTrue("a row is at least one kind", kind.showsSwitch() || kind.showsChevron());
        }
    }
}
