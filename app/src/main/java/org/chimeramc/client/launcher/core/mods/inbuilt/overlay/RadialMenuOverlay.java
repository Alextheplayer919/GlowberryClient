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
                // FLAG_NOT_FOCUSABLE is load-bearing, not cosmetic. The wheel must never take window
                // focus: a focusable overlay becomes the key/event target, so the Activity stops
                // receiving the pad's motion events and the controller cursor has nothing to move
                // on. The other overlays already carry this flag; the wheel did not, which is why
                // the cursor appeared but never moved. The wheel drives itself from the events the
                // Activity forwards to handleControllerKey/Motion, so it needs no focus of its own.
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
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
        // A pad-opened wheel enters controller-cursor mode: the cursor appears at the hub and the
        // stick drives it. Touch still works and hides the cursor on the first tap.
        menuView.post(() -> menuView.enableControllerCursor());
    }

    /**
     * Hands one controller key to the open wheel.
     *
     * @return true when the wheel consumed the press (so the game must not also act on it)
     */
    public boolean handleControllerKey(int keyCode, boolean down) {
        if (!isShowing || menuView == null || !down) return false;
        switch (keyCode) {
            case android.view.KeyEvent.KEYCODE_BUTTON_A:
            case android.view.KeyEvent.KEYCODE_DPAD_CENTER:
            case android.view.KeyEvent.KEYCODE_ENTER:
                return menuView.controllerSelect();
            case android.view.KeyEvent.KEYCODE_BUTTON_B:
            case android.view.KeyEvent.KEYCODE_BACK:
                return menuView.controllerBack();
            case android.view.KeyEvent.KEYCODE_BUTTON_X:
                return menuView.controllerPinHoveredChip();
            case android.view.KeyEvent.KEYCODE_DPAD_LEFT:
                menuView.focusNextSector(-1);
                return true;
            case android.view.KeyEvent.KEYCODE_DPAD_RIGHT:
                menuView.focusNextSector(1);
                return true;
            case android.view.KeyEvent.KEYCODE_BUTTON_R1:
                menuView.cycleChip(1);
                return true;
            case android.view.KeyEvent.KEYCODE_BUTTON_L1:
                menuView.cycleChip(-1);
                return true;
            default:
                return false;
        }
    }

    /**
     * Hands one analogue stick frame to the open wheel.
     *
     * @return true when the wheel consumed it (the game must not also move the camera)
     */
    public boolean handleControllerMotion(android.view.MotionEvent event) {
        if (!isShowing || menuView == null) return false;
        // Resolve the right stick by trying each axis convention and taking the first that carries a
        // deflection. Reading only AXIS_Z/AXIS_RZ meant a pad that reports its right stick on
        // AXIS_RX/AXIS_RY (or AXIS_RZ/AXIS_RX) delivered an all-zero vector, so the cursor never
        // moved. GamepadAxes owns that rule and is unit-tested.
        GamepadAxes.Stick stick = GamepadAxes.read(event);
        float lx = event.getAxisValue(android.view.MotionEvent.AXIS_X);
        float ly = event.getAxisValue(android.view.MotionEvent.AXIS_Y);
        boolean precision = Math.hypot(lx, ly) > 0.5f;
        boolean fast = event.getButtonState() != 0;
        long now = android.os.SystemClock.uptimeMillis();
        float dt = lastStickMs == 0L ? 1f / 60f : Math.min(0.1f, (now - lastStickMs) / 1000f);
        lastStickMs = now;
        float deadZone = CursorController.DEFAULT_DEAD_ZONE;
        float curve = CursorController.DEFAULT_CURVE_EXPONENT;
        lastStickSource = stick.source;
        return menuView.handleControllerStick(stick.x, stick.y, dt, precision, fast, deadZone, curve);
    }

    /** The axis convention the last stick frame used; reported by the diagnostics. */
    public String lastControllerStickSource() {
        return lastStickSource;
    }

    private String lastStickSource = "";

    private long lastStickMs;

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
        // Detach after the short close animation so the reverse plays rather than popping. The
        // callback is posted on the main Handler rather than on the View: a View's own postDelayed
        // is queued behind attachment, so if the window was torn down by anything else first the
        // callback never ran and the full-screen translucent window stayed attached — the frozen
        // "white transparent overlay" over the game. A Handler post always runs.
        mainHandler.removeCallbacks(detachRunnable);
        mainHandler.postDelayed(detachRunnable, 120L);
    }

    private final android.os.Handler mainHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable detachRunnable = this::detachQuietly;

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
        // A second, unconditional attempt: removeViewImmediate can throw for a window the system
        // has already detached, and a translucent MATCH_PARENT window that survives that is exactly
        // the stuck overlay. A removeView of a non-attached view is a safe no-op.
        try {
            windowManager.removeView(view);
        } catch (Exception ignored) {
        }
    }
}
