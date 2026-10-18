package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.app.Activity;

/**
 * A draggable text readout for a reachable catalogue module.
 *
 * <p>The catalogue's readout modules (clock, keystrokes, memory, coordinates, speed, health, the
 * session counters, …) all share the same plumbing: a small window, a timer, drag/lock/opacity and
 * an honest empty state. {@link TextOverlay} already owns that, so this subclass adds nothing but
 * the module id and a supplier for the text — which keeps a new readout a one-line registration in
 * {@code CatalogModuleRuntime} rather than a new window implementation.
 *
 * <p>The supplier returns null for the honest empty state; {@link #buildText} turns that into the
 * shared "waiting for game data" string so a readout with no feed says so instead of showing a
 * fabricated zero.
 */
public class CatalogReadoutOverlay extends TextOverlay {

    /** Supplies the readout text, or null when there is no reading yet. */
    public interface TextSource {
        String build();
    }

    private final TextSource source;

    public CatalogReadoutOverlay(Activity activity, String modId, TextSource source) {
        super(activity, modId);
        this.source = source;
    }

    @Override
    protected long refreshIntervalMs() {
        return 500L;
    }

    @Override
    protected String buildText() {
        String text = source == null ? null : source.build();
        if (text == null || text.isEmpty()) {
            return activity.getString(org.chimeramc.client.R.string.mod_overlay_data_unavailable);
        }
        return text;
    }
}
