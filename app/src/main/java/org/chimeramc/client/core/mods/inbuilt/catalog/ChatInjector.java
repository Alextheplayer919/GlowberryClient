package org.chimeramc.client.core.mods.inbuilt.catalog;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a chat line or command into the ordered keystrokes that type it.
 *
 * <p>Pure and Android-free: the caller feeds the produced steps to the launcher's existing native
 * key-injection path ({@code PreloaderInput.onKeyEvent}), which is the same path the on-screen
 * button modules already use for a real key press. Keeping the step list here means the typing
 * order is unit-testable without a device, and the Android side is a trivial loop.
 *
 * <p><b>How chat opens.</b> Bedrock opens chat on {@code T} and command chat on {@code /}. The
 * open key is sent with no character attached (a zero code point), so opening the box does not also
 * insert the key's own letter — the "my macro types a stray t" defect. Each following character is
 * sent as a key event carrying its own code point, and the line is committed with Enter.
 */
public final class ChatInjector {

    /** One injected key: a key code plus the code point to insert, or 0 for a control key. */
    public static final class Step {
        public final int keyCode;
        public final int codePoint;

        public Step(int keyCode, int codePoint) {
            this.keyCode = keyCode;
            this.codePoint = codePoint;
        }
    }

    /** Bedrock's key codes for the characters this needs. */
    public static final int KEYCODE_T = androidKeycode("T");
    public static final int KEYCODE_SLASH = androidKeycode("SLASH");
    public static final int KEYCODE_ENTER = androidKeycode("ENTER");
    public static final int KEYCODE_SPACE = androidKeycode("SPACE");

    private ChatInjector() {
    }

    /**
     * The steps to send a plain chat line: open chat, type the text, commit.
     *
     * <p>A leading {@code /} makes it a command, so the caller does not need a separate method —
     * the slash opens command chat by itself. Text is typed literally, one step per character.
     */
    public static List<Step> chat(String message) {
        return build(message == null ? "" : message.trim());
    }

    /** The steps to run a command; the leading slash is added when the caller omitted it. */
    public static List<Step> command(String command) {
        String body = command == null ? "" : command.trim();
        if (!body.startsWith("/")) body = "/" + body;
        return build(body);
    }

    private static List<Step> build(String text) {
        List<Step> steps = new ArrayList<>();
        if (text.isEmpty()) return steps;

        // A command opens with the slash key itself, which both opens chat and seeds the slash;
        // a plain message opens with T and no character so the box starts empty.
        if (text.startsWith("/")) {
            steps.add(new Step(KEYCODE_SLASH, '/'));
            text = text.substring(1);
        } else {
            steps.add(new Step(KEYCODE_T, 0));
        }

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            steps.add(new Step(keyCodeFor(c), c));
        }
        steps.add(new Step(KEYCODE_ENTER, 0));
        return steps;
    }

    /** The Android key code for a printable character, or 0 when the game takes it as a character. */
    static int keyCodeFor(char c) {
        if (c == ' ') return KEYCODE_SPACE;
        // Letters and digits have stable AOSP key codes (A=29..Z=54, 0=7..9=16); any other
        // character is delivered by code point alone, which the game inserts directly.
        if (c >= 'a' && c <= 'z') return 29 + (c - 'a');
        if (c >= 'A' && c <= 'Z') return 29 + (c - 'A');
        if (c >= '0' && c <= '9') return 7 + (c - '0');
        return 0;
    }

    private static int androidKeycode(String name) {
        try {
            return android.view.KeyEvent.class.getField("KEYCODE_" + name).getInt(null);
        } catch (Throwable ignored) {
            // Off-device (a JVM unit test) the class is stubbed; the value only matters on device.
            return 0;
        }
    }
}
