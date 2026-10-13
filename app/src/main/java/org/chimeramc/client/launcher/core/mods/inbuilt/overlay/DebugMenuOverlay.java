package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.app.Activity;

import org.chimeramc.client.R;
import org.chimeramc.client.core.mods.inbuilt.model.ModIds;
import org.chimeramc.client.preloader.PreloaderInput;

import java.util.Locale;

/**
 * A client-side debug readout: position, chunk, view rotation, health and the frame rate.
 *
 * <p>Everything here comes from data the client already reads — the local-player feed
 * ({@link LocalPlayerFeed}/{@link PreloaderInput}) and the frame meter — so it needs no native
 * entity feed. The entity count is not shown: this build cannot read the game's entity list, and a
 * fabricated number would be worse than omitting it.
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
        if (pos != null && pos.length >= 3) {
            b.append('\n').append(activity.getString(R.string.mod_overlay_pos,
                    String.format(Locale.US, "%.1f, %.1f, %.1f", pos[0], pos[1], pos[2])));
            b.append('\n').append(activity.getString(R.string.mod_overlay_chunk,
                    (int) Math.floor(pos[0]) >> 4, (int) Math.floor(pos[2]) >> 4));
        } else {
            b.append('\n').append(activity.getString(R.string.mod_overlay_data_unavailable));
        }
        float[] rot = PreloaderInput.readLocalPlayerRotation();
        if (rot != null && rot.length >= 2) {
            b.append('\n').append(activity.getString(R.string.mod_overlay_yaw,
                    String.format(Locale.US, "%.0f / %.0f", rot[0], rot[1])));
        }
        float[] health = PreloaderInput.readLocalPlayerHealth();
        if (health != null && health.length >= 1) {
            b.append('\n').append(activity.getString(R.string.mod_overlay_health,
                    String.format(Locale.US, "%.0f", health[0])));
        }
        int fps = frameMeter.currentFps();
        if (fps > 0) {
            b.append('\n').append(activity.getString(R.string.mod_overlay_fps_value, fps));
        }
        return b.toString();
    }
}
