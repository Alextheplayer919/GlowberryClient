package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.app.Activity;

import org.chimeramc.client.R;
import org.chimeramc.client.core.mods.inbuilt.catalog.LocalPlayerReadouts;
import org.chimeramc.client.core.mods.inbuilt.model.ModIds;
import org.chimeramc.client.preloader.PreloaderInput;

/**
 * A client-side debug readout in the shape of the Java Edition F3 screen: position, the chunk and
 * its corner, facing and pitch, health and the frame rate.
 *
 * <p>Everything here comes from data the client already reads — the local-player feed
 * ({@link LocalPlayerFeed}/{@link PreloaderInput}) and the frame meter — so it needs no native
 * entity feed. The formatting is delegated to {@link LocalPlayerReadouts}, the same class the
 * catalogue's coordinates/direction/chunk modules use, so the debug menu and those readouts cannot
 * disagree about which way is north or where a chunk starts.
 *
 * <p>Lines the Java F3 screen shows but this build cannot source are omitted rather than faked: the
 * entity count, the biome and the light level all need the game's own world structures, which the
 * shipped builds do not expose (game classes export no symbols). A missing line reads as "not
 * available"; an invented one would read as a real measurement.
 */
public class DebugMenuOverlay extends TextOverlay {

    private final FrameRateMeter frameMeter = new FrameRateMeter();

    public DebugMenuOverlay(Activity activity) {
        super(activity, ModIds.DEBUG_MENU);
    }

    @Override
    public void show(int startX, int startY) {
        super.show(startX, startY);
        try {
            frameMeter.start(fps -> { });
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void hide() {
        try {
            frameMeter.stop();
        } catch (Throwable ignored) {
        }
        super.hide();
    }

    @Override
    protected long refreshIntervalMs() {
        return 500L;
    }

    @Override
    protected String buildText() {
        StringBuilder b = new StringBuilder(activity.getString(R.string.mod_overlay_debug_title));
        float[] pos = PreloaderInput.readLocalPlayerPosition();
        String coords = LocalPlayerReadouts.coordinates(pos);
        if (coords != null) {
            b.append('\n').append(activity.getString(R.string.mod_overlay_pos, coords));
            String chunk = LocalPlayerReadouts.chunk(pos);
            if (chunk != null) {
                b.append('\n').append(activity.getString(R.string.mod_overlay_chunk, chunk));
            }
            String origin = LocalPlayerReadouts.chunkOrigin(pos);
            if (origin != null) {
                b.append('\n').append(activity.getString(R.string.mod_overlay_chunk_origin, origin));
            }
        } else {
            b.append('\n').append(activity.getString(R.string.mod_overlay_data_unavailable));
        }
        float[] rot = PreloaderInput.readLocalPlayerRotation();
        String direction = LocalPlayerReadouts.direction(rot);
        if (direction != null) {
            b.append('\n').append(activity.getString(R.string.mod_overlay_facing, direction));
        }
        String pitch = LocalPlayerReadouts.pitch(rot);
        if (pitch != null) {
            b.append('\n').append(activity.getString(R.string.mod_overlay_pitch, pitch));
        }
        String health = LocalPlayerReadouts.health(PreloaderInput.readLocalPlayerHealth());
        if (health != null) {
            b.append('\n').append(activity.getString(R.string.mod_overlay_health, health));
        }
        int fps = frameMeter.currentFps();
        if (fps > 0) {
            b.append('\n').append(activity.getString(R.string.mod_overlay_fps_value, fps));
        }
        return b.toString();
    }
}
