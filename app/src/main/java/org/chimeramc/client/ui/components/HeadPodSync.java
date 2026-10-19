package org.chimeramc.client.ui.components;

/**
 * The launch head pod's sync indicator state, as a pure rule.
 *
 * <p>The pod draws a small pulsing dot at its bottom edge to report whether the skin data is synced
 * with the cosmetic backend. The dot existed but nothing ever set it, so it never appeared. This is
 * the rule that maps the cosmetic-sync module's real state onto the indicator: a running module is
 * "synced", a module that is enabled but still opening its sockets is "loading", and a module that
 * is not running at all is "unknown" so the dot is hidden rather than showing a status the client
 * cannot back up.
 *
 * <p>Android-free so the mapping is unit-tested, the same reason the pod's motion and particles are.
 */
public final class HeadPodSync {

    private HeadPodSync() {
    }

    /**
     * The indicator state for the sync module.
     *
     * @param enabled whether the player has cosmetic sync switched on
     * @param running whether the module's transports are actually open and announcing
     * @return {@link HeadPodView#SYNC_SYNCED}, {@link HeadPodView#SYNC_LOADING} or
     *         {@link HeadPodView#SYNC_UNKNOWN}
     */
    public static int stateFor(boolean enabled, boolean running) {
        if (running) return HeadPodView.SYNC_SYNCED;
        // Enabled but not yet up: the sockets open on a background thread, so this is a real,
        // transient state rather than a failure. Reporting it as "loading" is honest; reporting it
        // as synced would claim a link that is not open yet.
        if (enabled) return HeadPodView.SYNC_LOADING;
        return HeadPodView.SYNC_UNKNOWN;
    }
}
