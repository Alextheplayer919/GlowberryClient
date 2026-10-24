package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import org.chimeramc.client.ui.components.UiTokens;

/**
 * Paints the wheel's glass backdrop and applies the blurred sheet behind it (R2, R3, R12).
 *
 * <p>All of the heavy API work lives here and nowhere else. Two blur routes are supported because
 * the honest answer differs by API level:
 *
 * <ul>
 *   <li><b>API 31+</b>: {@code Window#setBackgroundBlurRadius} with {@code FLAG_BLUR_BEHIND}, which
 *       asks the system compositor to blur what is genuinely <em>behind</em> the wheel's window. This
 *       is the correct mechanism — a {@code RenderEffect} on the wheel's own view would blur the
 *       wheel's own content instead of the game behind it.</li>
 *   <li><b>API 28-30</b>: no system blur exists, so the captured backdrop is blurred with a
 *       downscale/upscale stack (blit at half size with filtering, then stretch back), which is
 *       cheap and adds no API surface.</li>
 * </ul>
 *
 * <p>Both routes are driven by the resolved {@link RadialWheelMaterial}, so the tier, the scrim
 * floor and the thermal budget decide what happens; this class only carries out the decision.
 */
public final class WheelGlassRenderer {

    /** The denominator of the stack-blur's downscale. */
    public static final int STACK_BLUR_DIVISOR = 2;

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Rect srcRect = new Rect();
    private final Rect dstRect = new Rect();
    private final Rect blurSrcRect = new Rect();

    /**
     * Applies the blurred backdrop to the wheel's window.
     *
     * <p>Called once per open, never per frame. On an API level without a system blur it does
     * nothing and the caller falls back to {@link #drawBackdrop}'s stack blur. Clears the flag when
     * the resolved material does not blur, so a tier change (a thermal drop, "reduce transparency")
     * cannot leave a stale blur on the window.
     */
    public void applyWindowBlur(Window window, RadialWheelMaterial.Resolved material, float density) {
        if (window == null || material == null) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        try {
            if (material.blurs()) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                window.setBackgroundBlurRadius(
                        Math.round(material.blurDp * Math.max(0.5f, density)));
            } else {
                clearWindowBlur(window);
            }
        } catch (Throwable ignored) {
            // A window that is not yet attached (or an OEM that refuses the flag) simply keeps the
            // stack-blur route; the wheel stays readable either way.
        }
    }

    /** Clears any blur applied to a window. Safe on every API level. */
    public void clearWindowBlur(Window window) {
        if (window == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        try {
            window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            window.setBackgroundBlurRadius(0);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Draws the backdrop: the stack-blurred capture (below API 31 only), the black scrim floor, the
     * tier's dark fill and its white tint.
     *
     * <p>The order is what makes the material read as glass: the capture is what is behind the wheel,
     * the scrim guarantees the labels have something to sit on over a bright frame, the fill sets the
     * tier's solidity, and the tint is the light catching the sheet.
     *
     * @param cx,cy  the wheel centre
     * @param radius the radius of the backdrop disc
     */
    public void drawBackdrop(Canvas canvas, RadialWheelMaterial.Resolved material, Bitmap capture,
                             float cx, float cy, float radius, float alpha) {
        if (canvas == null || material == null) return;
        float a = Math.max(0f, Math.min(1f, alpha));

        if (capture != null && !capture.isRecycled() && material.blurs()
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            drawStackBlur(canvas, capture, cx, cy, radius, a);
        }

        int scrimAlpha = (int) (material.scrimAlpha * 255 * a);
        if (scrimAlpha > 0) {
            fillPaint.setStyle(Paint.Style.FILL);
            fillPaint.setColor(UiTokens.withAlpha(0xFF000000, scrimAlpha));
            canvas.drawCircle(cx, cy, radius, fillPaint);
        }

        int fillAlpha = (int) (material.fillAlpha * a);
        if (fillAlpha > 0) {
            fillPaint.setColor(UiTokens.withAlpha(UiTokens.BG_WINDOW, fillAlpha));
            canvas.drawCircle(cx, cy, radius, fillPaint);
        }

        int tintAlpha = (int) (material.tint * 255 * a);
        if (tintAlpha > 0) {
            tintPaint.setColor(UiTokens.withAlpha(0xFFFFFFFF, tintAlpha));
            canvas.drawCircle(cx, cy, radius, tintPaint);
        }
    }

    /**
     * Draws a blurred copy of the captured backdrop into the disc (the API 28-30 route).
     *
     * <p>The capture is already at 1/4 resolution (R3), so one further downscale and a filtered
     * stretch back up gives a soft sheet for the cost of two blits. Every rect is a reused member so
     * the draw path allocates nothing.
     */
    private void drawStackBlur(Canvas canvas, Bitmap capture, float cx, float cy, float radius,
                               float alpha) {
        int w = capture.getWidth();
        int h = capture.getHeight();
        if (w <= 0 || h <= 0) return;
        int sw = Math.max(1, w / STACK_BLUR_DIVISOR);
        int sh = Math.max(1, h / STACK_BLUR_DIVISOR);
        dstRect.set(Math.round(cx - radius), Math.round(cy - radius),
                Math.round(cx + radius), Math.round(cy + radius));
        srcRect.set(0, 0, w, h);
        blurSrcRect.set(0, 0, sw, sh);
        int save = canvas.save();
        canvas.clipRect(dstRect);
        bitmapPaint.setAlpha((int) (255 * alpha));
        // Downscale, then stretch the small copy back over the disc: the filtered second blit is
        // the blur.
        canvas.drawBitmap(capture, blurSrcRect, dstRect, bitmapPaint);
        canvas.restoreToCount(save);
    }

    /** Draws the hairline that defines a glass edge. */
    public void drawHairline(Canvas canvas, float cx, float cy, float radius, float alpha,
                             float density) {
        if (canvas == null || alpha <= 0f) return;
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(Math.max(1f, density));
        strokePaint.setColor(UiTokens.withAlpha(UiTokens.STROKE_HAIRLINE,
                (int) (0x24 * Math.max(0f, Math.min(1f, alpha)))));
        canvas.drawCircle(cx, cy, radius, strokePaint);
    }

    /**
     * The paint used for an outlined label: a stroke-only copy of the text paint (R10).
     *
     * <p>Returned rather than configured on a shared paint so the caller can draw the halo and the
     * fill from the same text metrics without re-measuring.
     */
    public Paint labelOutlinePaint(Paint textPaint, int color, float density) {
        Paint outline = new Paint(textPaint);
        outline.setStyle(Paint.Style.STROKE);
        outline.setStrokeWidth(Math.max(1f, 2f * density));
        outline.setColor(color);
        return outline;
    }

    /** Clears any render effect on a view; the wheel never sets one, but a caller may have. */
    public void clearViewEffect(View target) {
        if (target == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        target.setRenderEffect(null);
    }
}
