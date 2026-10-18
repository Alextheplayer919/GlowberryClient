package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import org.chimeramc.client.ui.components.UiDraw;
import org.chimeramc.client.ui.components.UiTokens;

/**
 * A reusable controller cursor that overlays an in-game menu and turns stick input into taps, so the
 * same pointer works in the radial wheel, the Mod Menu, the VIP menu and the Closet.
 *
 * <p>It is a full-bleed, non-interactive child of the menu's root: {@code onTouchEvent} returns
 * false so it never eats a real touch, and it draws only the cursor. Selection is a synthesised
 * {@link MotionEvent} dispatched to the menu tree at the cursor point, so a controller "click"
 * lands on exactly the view a finger would have hit — no per-menu wiring, no second hit-test table
 * to keep in step with the layout.
 *
 * <p>The motion maths live in {@link CursorController} (pure, unit-tested); this class only paints
 * and dispatches.
 */
public final class MenuCursorOverlay extends View {

    /** The game-side seam, so "the menu consumes the input" is testable with a fake. */
    private final CursorRouter router;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private long lastMs;

    public MenuCursorOverlay(Context context) {
        super(context);
        setWillNotDraw(false);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        CursorController cursor = new CursorController(
                Math.max(1, getResources().getDisplayMetrics().widthPixels),
                Math.max(1, getResources().getDisplayMetrics().heightPixels),
                getResources().getDisplayMetrics().density);
        this.router = new CursorRouter(cursor, null);
    }

    /** True while the cursor is visible. */
    public boolean isCursorVisible() {
        return router.isMenuOpen();
    }

    /** Enables the cursor at the viewport centre (called when a pad opens the menu). */
    public void enable() {
        router.setMenuOpen(true);
        router.cursor().setViewport(getWidth() <= 0 ? 1 : getWidth(),
                getHeight() <= 0 ? 1 : getHeight());
        router.cursor().centre();
        lastMs = 0L;
        invalidate();
    }

    /** Hides the cursor (a touch, or the menu closing). */
    public void disable() {
        router.setMenuOpen(false);
        invalidate();
    }

    /**
     * Feeds one frame of stick input.
     *
     * @return true when the menu consumed it (the game must not also move the camera)
     */
    public boolean handleStick(float rawX, float rawY, boolean precision, boolean fast) {
        if (!router.isMenuOpen()) return false;
        long now = SystemClock.uptimeMillis();
        float dt = lastMs == 0L ? 1f / 60f : Math.min(0.1f, (now - lastMs) / 1000f);
        lastMs = now;
        float[] shaped = new float[2];
        CursorController.shape(rawX, rawY, CursorController.DEFAULT_DEAD_ZONE,
                CursorController.DEFAULT_CURVE_EXPONENT, shaped);
        // Android's stick Y is negative when pushed up, matching screen Y growing down (an up push
        // must reduce the cursor's y). Pass the shaped axes through unchanged.
        router.cursor().setMagnetTarget(false, 0f, 0f);
        router.cursor().update(shaped[0], shaped[1], dt, precision, fast,
                CursorController.DEFAULT_MAX_SPEED_DP);
        invalidate();
        return true;
    }

    /**
     * Selects the view under the cursor by dispatching a synthesised tap to the menu tree.
     *
     * @return true when the menu consumed it
     */
    public boolean select() {
        if (!router.isMenuOpen()) return false;
        View root = (View) getParent();
        if (root == null) return true;
        float x = router.cursor().x();
        float y = router.cursor().y();
        long now = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0);
        MotionEvent up = MotionEvent.obtain(now, now + 10, MotionEvent.ACTION_UP, x, y, 0);
        try {
            // Dispatch to the menu root (this overlay is not interactive, so it is transparent to
            // the events and the child under the point receives them).
            root.dispatchTouchEvent(down);
            root.dispatchTouchEvent(up);
        } catch (Throwable ignored) {
        }
        down.recycle();
        up.recycle();
        return true;
    }

    /** The controller cursor, for a caller that wants the raw position. */
    public CursorController cursor() {
        return router.cursor();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!router.isMenuOpen()) return;
        float density = getResources().getDisplayMetrics().density;
        UiDraw.pointerCursor(canvas, paint, paint2,
                router.cursor().x(), router.cursor().y(), density);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // A real touch hides the cursor and is handled by the menu below; this view never consumes.
        if (router.isMenuOpen()) {
            disable();
        }
        return false;
    }

    /** Attaches a cursor overlay to a menu root, sized to fill it. */
    public static MenuCursorOverlay attach(Context context, ViewGroup root) {
        if (context == null || root == null) return null;
        MenuCursorOverlay overlay = new MenuCursorOverlay(context);
        root.addView(overlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return overlay;
    }
}
