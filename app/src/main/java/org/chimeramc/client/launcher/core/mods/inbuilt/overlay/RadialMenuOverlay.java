package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import org.chimeramc.client.R;
import org.chimeramc.client.core.mods.inbuilt.manager.InbuiltModManager;
import org.chimeramc.client.core.mods.inbuilt.overlay.RadialWheelLayout.Sector;

/**
 * The radial mod wheel as an in-game overlay (section 1 of the update spec).
 *
 * <p>It is a second screen beside the touch Mod Menu (Screen A) and the VIP menu (Screen B): it is
 * opened from the same in-game bind and points at the same destinations. The four sectors route to
 * the module list, the cosmetics wardrobe, the settings and the HUD editor; the floating chips
 * toggle individual modules without opening a menu at all.
 *
 * <p>This is original work: the layout, colours and icons are the app's own (the violet/magenta
 * brand), built from {@link RadialMenuView} and the pure {@link RadialWheelLayout}. It deliberately
 * copies no other client's art, name or text.
 */
public class RadialMenuOverlay {

    private final Activity activity;
    private WindowManager windowManager;
    private WindowManager.LayoutParams wmParams;
    private View overlayView;
    private RadialMenuView menuView;
    private boolean isShowing;

    /** How the wheel reaches its destinations; supplied by whoever owns the other screens. */
    public interface Destinations {
        /** Open the module list (the MODS sector). */
        void openMods();
        /** Open the cosmetics wardrobe (the CLOSET sector). */
        void openCloset();
        /** Open the general settings (the SETTINGS sector). */
        void openSettings();
        /** Enter the HUD editor (the HUD sector). */
        void openHudEditor();
    }

    private Destinations destinations;

    public RadialMenuOverlay(Activity activity) {
        this.activity = activity;
        this.windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
    }

    public void setDestinations(Destinations destinations) {
        this.destinations = destinations;
    }

    public boolean isShowing() {
        return isShowing;
    }

    /** Opens or closes from the in-game bind. */
    public void toggle() {
        if (isShowing) {
            hide();
        } else {
            show();
        }
    }

    public void show() {
        if (isShowing || activity.isFinishing() || activity.isDestroyed()) return;
        try {
            showInternal();
        } catch (Throwable e) {
            detachQuietly();
        }
    }

    private void showInternal() {
        overlayView = LayoutInflater.from(activity).inflate(R.layout.overlay_radial_menu, null);
        menuView = overlayView.findViewById(R.id.radial_menu_view);
        menuView.setListener(new RadialMenuView.Listener() {
            @Override
            public void onSector(Sector sector) {
                hide();
                dispatch(sector);
            }

            @Override
            public void onDismiss() {
                hide();
            }
        });
        menuView.setChipToggle(new RadialMenuView.ChipToggle() {
            @Override
            public void onChipToggled(String moduleId) {
                InbuiltModManager manager = InbuiltModManager.getInstance(activity);
                boolean next = !manager.resolveInbuiltModEnabled(moduleId, false);
                manager.setInbuiltModEnabled(moduleId, next);
                InbuiltOverlayManager overlayManager = InbuiltOverlayManager.getInstance();
                if (overlayManager != null) {
                    overlayManager.handleModToggle(moduleId, next);
                }
                menuView.invalidate();
            }

            @Override
            public boolean isChipOn(String moduleId) {
                return InbuiltModManager.getInstance(activity)
                        .resolveInbuiltModEnabled(moduleId, false);
            }
        });

        wmParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.TRANSLUCENT);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            wmParams.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        wmParams.gravity = Gravity.CENTER;
        wmParams.token = activity.getWindow().getDecorView().getWindowToken();
        windowManager.addView(overlayView, wmParams);
        isShowing = true;
    }

    private void dispatch(Sector sector) {
        if (destinations == null) return;
        switch (sector) {
            case MODS:
                destinations.openMods();
                break;
            case CLOSET:
                destinations.openCloset();
                break;
            case SETTINGS:
                destinations.openSettings();
                break;
            case HUD:
                destinations.openHudEditor();
                break;
            default:
                break;
        }
    }

    public void hide() {
        isShowing = false;
        RadialMenuView view = menuView;
        menuView = null;
        if (view != null) view.playClose();
        // Detach after the short close animation so the reverse plays rather than popping.
        View overlay = overlayView;
        if (overlay != null) {
            overlay.postDelayed(this::detachQuietly, 120L);
        } else {
            detachQuietly();
        }
    }

    private void detachQuietly() {
        View view = overlayView;
        overlayView = null;
        if (view == null) return;
        try {
            if (view.getParent() != null) {
                windowManager.removeViewImmediate(view);
            }
        } catch (Exception ignored) {
            // Already detached or the token is gone; nothing to clean up.
        }
    }
}
