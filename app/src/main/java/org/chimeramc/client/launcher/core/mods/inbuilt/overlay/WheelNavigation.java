package org.chimeramc.client.core.mods.inbuilt.overlay;

import java.util.ArrayList;
import java.util.List;

/**
 * The open sub-wheel path, as a pure stack.
 *
 * <p>The spec's "nested sub-wheels for an unlimited number of actions" is a navigation problem, not
 * a drawing one: opening a sub-wheel pushes a profile, and backing out pops it. Modelling it as a
 * stack is what makes the depth, the back-out and the "we are back at the root" question testable
 * without a device.
 *
 * <p>The depth is bounded by {@link #MAX_DEPTH}. A profile may legitimately point back at an
 * ancestor (the built-in PvP wheel has a "Back" entry to the main wheel), so a cycle is not an error
 * — but an unbounded stack would let a repeated cycle grow without limit, so the bound is what
 * keeps a cycle from becoming a leak.
 */
public final class WheelNavigation {

    /** The most sub-wheels that may be open at once. */
    public static final int MAX_DEPTH = 8;

    private final List<String> stack = new ArrayList<>();

    public WheelNavigation() {
    }

    /** Starts at a root profile id. A null/blank id is ignored, so the stack is never seeded blank. */
    public WheelNavigation(String rootId) {
        reset(rootId);
    }

    /** Clears the stack and seeds it with a root profile id. */
    public void reset(String rootId) {
        stack.clear();
        if (rootId != null && !rootId.trim().isEmpty()) stack.add(rootId);
    }

    /**
     * Opens a sub-wheel.
     *
     * @return true when it was pushed; false when the id is blank, the stack is already at
     *         {@link #MAX_DEPTH}, or the id is already on the stack (pushing it would deepen a
     *         cycle the player did not ask for — the wheel's own "Back" entry is how they leave)
     */
    public boolean push(String id) {
        if (id == null || id.trim().isEmpty()) return false;
        if (stack.size() >= MAX_DEPTH) return false;
        if (stack.contains(id)) return false;
        stack.add(id);
        return true;
    }

    /**
     * Backs out one level.
     *
     * @return true when a level was popped; false when already at the root, which is what the caller
     *         uses to close the wheel instead
     */
    public boolean pop() {
        if (stack.size() <= 1) return false;
        stack.remove(stack.size() - 1);
        return true;
    }

    /** The id of the wheel currently open, or "" when the stack is empty. */
    public String currentId() {
        return stack.isEmpty() ? "" : stack.get(stack.size() - 1);
    }

    /** How many wheels are open, root included; 0 when the stack is empty. */
    public int depth() {
        return stack.size();
    }

    /** True while the root wheel is the open one. */
    public boolean atRoot() {
        return stack.size() <= 1;
    }
}
