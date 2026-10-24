package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * Why a Mod Menu row is or is not usable (M10, and G8's UI form).
 *
 * <p>A row used to be either enabled or not, so a module that failed to register looked identical to
 * one the user had switched off — the switch was simply off and nothing said why. Every row now
 * carries one of these, and the row's appearance, its dot colour and its interactivity all derive
 * from it. Pure, so the mapping is unit-tested without a device.
 */
public enum ModMenuRowState {
    /** Registered and healthy. The switch is interactive. */
    LOADED(true, false, false),
    /** The user turned it off. Still healthy. */
    DISABLED(true, false, false),
    /** Registration failed. Dimmed, switch disabled, red dot, tap shows the reason. */
    FAILED(false, true, true),
    /** No hook for this Bedrock build. Dimmed, amber dot, switch disabled. */
    UNSUPPORTED(false, true, true),
    /** Auto-disabled after repeated crashes (G8). Dimmed, red dot, offers a re-enable. */
    CRASHED(false, true, true);

    private final boolean interactive;
    private final boolean dimmed;
    private final boolean hasReason;

    ModMenuRowState(boolean interactive, boolean dimmed, boolean hasReason) {
        this.interactive = interactive;
        this.dimmed = dimmed;
        this.hasReason = hasReason;
    }

    /** Whether the row's switch may be flipped. */
    public boolean isInteractive() {
        return interactive;
    }

    /** Whether the row is drawn at reduced opacity. */
    public boolean isDimmed() {
        return dimmed;
    }

    /** Whether a tap on the row should open the reason sheet rather than toggle. */
    public boolean hasReason() {
        return hasReason;
    }

    /** True for a state that means the module cannot run at all right now. */
    public boolean isBroken() {
        return this == FAILED || this == UNSUPPORTED || this == CRASHED;
    }

    /** The state for a row, given what the registry and the user's toggle say. */
    public static ModMenuRowState of(boolean registered, boolean available, boolean crashed,
                                     boolean enabled) {
        if (!registered) return FAILED;
        if (crashed) return CRASHED;
        if (!available) return UNSUPPORTED;
        return enabled ? LOADED : DISABLED;
    }
}
