package org.chimeramc.client.core.mods.inbuilt.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

/**
 * The chat/command injector: the ordered keystrokes that type a chat line or run a command.
 *
 * <p>Pure (the Android key codes resolve to 0 off-device, which the assertions allow), so the
 * ordering rules are pinned here. The important one is that opening chat sends no character: a
 * macro that sends {@code T} with its own letter attached types a stray "t" before the message.
 */
public class ChatInjectorTest {

    @Test
    public void aChatLineOpensTypesAndCommits() {
        List<ChatInjector.Step> steps = ChatInjector.chat("hi");
        assertEquals(4, steps.size());          // open, h, i, enter
        assertEquals(0, steps.get(0).codePoint); // the open key inserts nothing
        assertEquals('h', steps.get(1).codePoint);
        assertEquals('i', steps.get(2).codePoint);
        assertEquals(0, steps.get(3).codePoint); // enter inserts nothing
    }

    @Test
    public void aCommandOpensWithTheSlashAndAddsAMissingOne() {
        List<ChatInjector.Step> withSlash = ChatInjector.command("/spawn");
        List<ChatInjector.Step> without = ChatInjector.command("spawn");
        assertEquals(withSlash.size(), without.size());
        // The slash key both opens command chat and seeds the slash.
        assertEquals('/', withSlash.get(0).codePoint);
        assertEquals('/', without.get(0).codePoint);
    }

    @Test
    public void anEmptyMessageProducesNoSteps() {
        assertTrue(ChatInjector.chat("").isEmpty());
        assertTrue(ChatInjector.chat(null).isEmpty());
        assertTrue(ChatInjector.chat("   ").isEmpty());
    }

    @Test
    public void lettersDigitsAndSpacesGetStableKeyCodes() {
        // AOSP codes: A=29..Z=54, 0=7..9=16. Off-device the named control codes resolve to 0, so
        // only the character mapping is asserted here.
        assertEquals(29 + ('c' - 'a'), ChatInjector.keyCodeFor('c'));
        assertEquals(29 + ('C' - 'A'), ChatInjector.keyCodeFor('C'));
        assertEquals(7 + 5, ChatInjector.keyCodeFor('5'));
        assertEquals(ChatInjector.KEYCODE_SPACE, ChatInjector.keyCodeFor(' '));
        // Any other character travels by code point alone.
        assertEquals(0, ChatInjector.keyCodeFor('!'));
    }

    @Test
    public void theMessageBodyIsTrimmed() {
        assertEquals(ChatInjector.chat("gg").size(), ChatInjector.chat("  gg  ").size());
    }

    @Test
    public void aTypedCharacterCarriesItsOwnCodePoint() {
        List<ChatInjector.Step> steps = ChatInjector.chat("ok");
        assertEquals('o', steps.get(1).codePoint);
        assertEquals('k', steps.get(2).codePoint);
        assertFalse(steps.get(1).keyCode == 0 && steps.get(1).codePoint == 0);
    }
}
