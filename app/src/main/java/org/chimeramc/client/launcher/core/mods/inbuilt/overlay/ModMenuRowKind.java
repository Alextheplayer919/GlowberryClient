package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * The one rule that decides a row's control type (M2, M3).
 *
 * <p>The menu used to render every entry as a tap target, so a <em>stateful module</em> looked like
 * a <em>one-shot button</em> and a tap never showed whether it was on. The preloader API already
 * distinguishes the two — {@code ModuleBuilder(...).registerModule()} has an enable state,
 * {@code ButtonBuilder(...).registerButton()} fires an action — and this class is that distinction
 * expressed once, so the UI cannot drift from the API.
 *
 * <p><b>The rule:</b> anything with an enable state gets a switch; anything that fires once gets an
 * action row and never a switch. Never both. A switch that instantly snaps back is the single most
 * confusing control in any mod menu.
 *
 * <p>Pure, so the rule is unit-tested without a device.
 */
public enum ModMenuRowKind {
    /** A module: it has an on/off state and is drawn with a switch. */
    SWITCH,
    /** An action: it fires once and is drawn with a chevron, never a switch. */
    ACTION;

    /**
     * The kind for an entry.
     *
     * @param hasEnableState true for a registered module (it can be toggled on and off)
     */
    public static ModMenuRowKind of(boolean hasEnableState) {
        return hasEnableState ? SWITCH : ACTION;
    }

    /** True when the row shows a switch control. */
    public boolean showsSwitch() {
        return this == SWITCH;
    }

    /** True when the row shows a chevron and fires on tap. */
    public boolean showsChevron() {
        return this == ACTION;
    }

    /**
     * Whether a row may present both controls.
     *
     * <p>Always false, and it exists as a named rule rather than a comment so the invariant is
     * asserted rather than assumed: a row that both toggles a state and fires an action is exactly
     * the ambiguity this classification removes.
     */
    public static boolean mayCombine() {
        return false;
    }
}
