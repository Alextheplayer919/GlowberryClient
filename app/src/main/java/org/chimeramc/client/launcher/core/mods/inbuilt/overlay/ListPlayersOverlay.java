package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.app.Activity;

import org.chimeramc.client.R;
import org.chimeramc.client.core.mods.inbuilt.model.ModIds;
import org.chimeramc.client.preloader.PreloaderInput;

import java.util.List;
import java.util.Locale;

/**
 * Lists the players the client can see, with distance, as a small draggable readout.
 *
 * <p>The peers come from the proximity-voice feed ({@link PeerPositions}), which needs no native
 * entity feed: peers advertise their own position over the voice protocol. Vanilla players are not
 * in that feed, so this lists other Glowberry users — the same limit the Hitboxes peer boxes
 * document. The local player's own position/health comes from the local-player feed.
 */
public class ListPlayersOverlay extends TextOverlay {

    public ListPlayersOverlay(Activity activity) {
        super(activity, ModIds.LIST_PLAYERS);
    }

    @Override
    protected long refreshIntervalMs() {
        return 500L;
    }

    @Override
    protected String buildText() {
        StringBuilder b = new StringBuilder();
        List<ReachIndicator.VoicePeerPosition> peers = PeerPositions.read();
        b.append(activity.getString(R.string.mod_overlay_players_title, peers.size()));
        if (peers.isEmpty()) {
            b.append('\n').append(activity.getString(R.string.mod_overlay_no_peers));
            return b.toString();
        }
        float[] self = PreloaderInput.readLocalPlayerPosition();
        for (ReachIndicator.VoicePeerPosition peer : peers) {
            b.append('\n').append(peer.name == null || peer.name.isEmpty() ? "?" : peer.name);
            if (self != null && self.length >= 3) {
                double d = Math.sqrt(Math.pow(peer.x - self[0], 2)
                        + Math.pow(peer.y - self[1], 2) + Math.pow(peer.z - self[2], 2));
                b.append("  ").append(String.format(Locale.US, "%.0fm", d));
            }
        }
        return b.toString();
    }
}
