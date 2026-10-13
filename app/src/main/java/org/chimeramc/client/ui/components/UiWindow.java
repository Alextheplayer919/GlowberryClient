package org.chimeramc.client.ui.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * The window chrome shared by both mod menus and the Closet: a rounded 16dp near-black panel with a
 * 1dp hairline and a shadow, a 48dp title bar with a hairline divider, and a centred layout rule.
 */
public final class UiWindow {

    private UiWindow() {
    }

    /**
     * The window rectangle for a screen: centred, width {@code min(0.66 * screenW, 720dp)} and height
     * {@code 0.78 * screenH}. Pure so a test can pin it across screen sizes.
     */
    public static RectF bounds(int screenW, int screenH, float density) {
        float[] b = boundsArray(screenW, screenH, density);
        return new RectF(b[0], b[1], b[2], b[3]);
    }

    /** The window rectangle as {@code {left, top, right, bottom}}, pure and device-free. */
    public static float[] boundsArray(int screenW, int screenH, float density) {
        float maxW = UiTokens.WINDOW_MAX_WIDTH_DP * density;
        float w = Math.min(screenW * UiTokens.WINDOW_WIDTH_FRACTION, maxW);
        float h = screenH * UiTokens.WINDOW_HEIGHT_FRACTION;
        float left = (screenW - w) / 2f;
        float top = (screenH - h) / 2f;
        return new float[]{left, top, left + w, top + h};
    }

    /** The title bar rectangle inside a window. */
    public static RectF titleBar(RectF window, float density) {
        float h = UiTokens.TITLEBAR_HEIGHT * density;
        return new RectF(window.left, window.top, window.right, window.top + h);
    }

    /** The content rectangle below the title bar. */
    public static RectF content(RectF window, float density) {
        RectF bar = titleBar(window, density);
        return new RectF(window.left, bar.bottom, window.right, window.bottom);
    }

    /** Draws the window background, the title bar and the divider. */
    public static void draw(Canvas c, Paint paint, RectF window, float radius, float density,
                            float shadowRadius, float shadowAlpha) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(UiTokens.withAlpha(0xFF000000, (int) (shadowAlpha * 255)));
        paint.setShadowLayer(shadowRadius, 0f, 0f,
                UiTokens.withAlpha(0xFF000000, (int) (shadowAlpha * 255)));
        c.drawRoundRect(window, radius, radius, paint);
        paint.clearShadowLayer();

        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(UiTokens.BG_WINDOW);
        c.drawRoundRect(window, radius, radius, paint);

        RectF bar = titleBar(window, density);
        paint.setColor(UiTokens.BG_TITLEBAR);
        c.drawRoundRect(new RectF(bar.left, bar.top, bar.right, bar.bottom), radius, radius, paint);
        UiDraw.divider(c, paint, window.left, bar.bottom, window.right, Math.max(1f, density),
                UiTokens.STROKE_HAIRLINE);
        UiDraw.hairline(c, paint, window.left, window.top, window.right, window.bottom, radius,
                Math.max(1f, density), UiTokens.STROKE_HAIRLINE);
    }
}
