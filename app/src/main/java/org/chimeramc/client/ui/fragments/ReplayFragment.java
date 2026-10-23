package org.chimeramc.client.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.chimeramc.client.R;
import org.chimeramc.client.core.replay.ReplayPanel;

/**
 * The clip library, hosted inside {@link org.chimeramc.client.ui.activities.LibraryActivity} (U3).
 *
 * <p>It hosts the same {@link ReplayPanel} the two in-game Mod Menu screens host, so there is no
 * second clip list, sort, filter or export implementation. The panel owns a background executor and
 * a live recorder listener, so it is disposed in {@link #onDestroyView()} rather than merely
 * dropped, and {@code onShown}/{@code onHidden} bracket its live refresh around this fragment's
 * visibility.
 */
public class ReplayFragment extends Fragment {

    private ReplayPanel replayPanel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_replay, container, false);
        FrameLayout host = root.findViewById(R.id.replay_fragment_container);
        replayPanel = new ReplayPanel(requireActivity(), false);
        host.addView(replayPanel.getView(), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        replayPanel.setGamepadDetected(hasConnectedGamepad());
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (replayPanel != null) replayPanel.onShown();
    }

    @Override
    public void onPause() {
        if (replayPanel != null) replayPanel.onHidden();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (replayPanel != null) {
            replayPanel.dispose();
            replayPanel = null;
        }
        super.onDestroyView();
    }

    /** The panel switches its controller hint on the detected input, so it needs a real scan. */
    private boolean hasConnectedGamepad() {
        for (int id : android.view.InputDevice.getDeviceIds()) {
            android.view.InputDevice device = android.view.InputDevice.getDevice(id);
            if (device == null) continue;
            int sources = device.getSources();
            if ((sources & android.view.InputDevice.SOURCE_GAMEPAD) == android.view.InputDevice.SOURCE_GAMEPAD
                    || (sources & android.view.InputDevice.SOURCE_JOYSTICK) == android.view.InputDevice.SOURCE_JOYSTICK) {
                return true;
            }
        }
        return false;
    }
}
