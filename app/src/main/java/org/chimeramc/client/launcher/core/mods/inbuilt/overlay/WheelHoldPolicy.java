package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * The radial wheel's open/confirm rule.
 *
 * <p>The wheel is specified as a hold-to-open menu: the bind press opens it, the player flicks or
 * points at a sector while holding, and the release commits the selection. That is a different
 * input shape from a toggle (press to open, press again to close), and both are legitimate, so the
 * rule is one pure function rather than a branch buried in the key handler.
 *
 * <p>Android-free, so the decision table is unit-tested without a device — which matters because
 * the failure mode (a wheel that will not open, or one that opens and immediately closes on the
 * release of the same press) is only visible in play.
 */
public final class WheelHoldPolicy {

    /** What a bind press should do. */
    public enum OnPress {
        /** Open the wheel and hold it until the release. */
        OPEN_HOLD,
        /** Open it and keep it open (toggle-style open). */
        OPEN_TOGGLE,
        /** It is already open, so this press closes it. */
        CLOSE,
        /** Nothing to do. */
        IGNORE,
    }

    /** What a bind release should do. */
    public enum OnRelease {
        /** Commit whatever is highlighted (or dismiss when nothing is). */
        CONFIRM,
        /** Close without acting. */
        DISMISS,
        /** Nothing to do. */
        IGNORE,
    }

    private WheelHoldPolicy() {
    }

    /**
     * @param holdToOpen  the player's preference (hold-to-open when true, toggle when false)
     * @param isShowing   whether the wheel is already open
     * @param openedByHold whether the currently open wheel was opened by this same hold gesture
     */
    public static OnPress onPress(boolean holdToOpen, boolean isShowing, boolean openedByHold) {
        if (!holdToOpen) {
            return isShowing ? OnPress.CLOSE : OnPress.OPEN_TOGGLE;
        }
        // A hold gesture opens on the press. If the wheel is already open it was opened by an
        // earlier gesture (or a launcher tap), so this press closes it — otherwise the wheel could
        // never be dismissed with the bind.
        return isShowing ? OnPress.CLOSE : OnPress.OPEN_HOLD;
    }

    /**
     * @param holdToOpen  the player's preference
     * @param isShowing   whether the wheel is open at the release
     * @param openedByHold whether the open wheel belongs to this hold gesture
     * @param hasSelection whether a sector or entry is highlighted
     */
    public static OnRelease onRelease(boolean holdToOpen, boolean isShowing, boolean openedByHold,
                                      boolean hasSelection) {
        if (!holdToOpen || !isShowing || !openedByHold) return OnRelease.IGNORE;
        return hasSelection ? OnRelease.CONFIRM : OnRelease.DISMISS;
    }
}
