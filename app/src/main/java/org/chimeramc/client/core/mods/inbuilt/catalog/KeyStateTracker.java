package org.chimeramc.client.core.mods.inbuilt.catalog;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Tracks which of the movement keys are currently held, for the Keystrokes readout.
 *
 * <p>Pure and Android-free: it takes a raw key code and a down/up flag and keeps the set, so the
 * hold/release rules are unit-testable. The game activity feeds it the keys it already sees in
 * {@code dispatchKeyEvent}, which is the same input the game acts on, so the readout cannot show a
 * key the game did not receive.
 *
 * <p>Only the movement/action keys are tracked. A full keyboard mirror would be noise on a small
 * overlay and would leak unrelated typing into a PvP HUD.
 */
public final class KeyStateTracker {

    private final Set<Integer> held = new LinkedHashSet<>();

    /** Records a key state. Returns true when the set changed. */
    public boolean onKey(int keyCode, boolean down) {
        return down ? held.add(keyCode) : held.remove(keyCode);
    }

    public boolean isHeld(int keyCode) {
        return held.contains(keyCode);
    }

    /** Clears every held key; call when the session or focus is lost so a key cannot stick. */
    public void reset() {
        held.clear();
    }

    /** A compact "W A S D" style line for the readout, in the order given. */
    public String line(int[] keys, String[] labels) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < keys.length && i < labels.length; i++) {
            if (i > 0) b.append(' ');
            b.append(held.contains(keys[i]) ? '[' + labels[i] + ']' : ' ' + labels[i] + ' ');
        }
        return b.toString();
    }
}
