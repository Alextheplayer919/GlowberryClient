package org.chimeramc.client.ui.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * The shared primary/secondary button: a 14dp-radius rounded rectangle, the primary one an accent
 * gradient with a soft glow, the secondary a surface with a hairline. Heights and touch targets are
 * pure so a test can assert the 48dp minimum.
 */
public final class UiButton {

    private UiButton() {
    }

    public enum Style { PRIMARY, SECONDARY }

    /** Draws the button. {@code radius} and {@code glowRadius} are px. */
    public static void draw(Canvas c, Paint paint, RectF b, float radius, int accent, Style style,
                            boolean pressed, boolean enabled) {
        if (style == Style.PRIMARY) {
            int start = enabled ? accent : UiTokens.blend(accent, UiTokens.BG_SURFACE, 0.6f);
            int end = enabled ? UiTokens.accentHover(accent)
                    : UiTokens.blend(accent, UiTokens.BG_SURFACE, 0.6f);
            if (pressed && enabled) {
                start = UiTokens.blend(start, 0xFFFFFFFF, 0.12f);
                end = UiTokens.blend(end, 0xFFFFFFFF, 0.12f);
            }
            UiDraw.gradientRect(c, paint, b.left, b.top, b.right, b.bottom, radius, start, end);
        } else {
            paint.reset();
            paint.setAntiAlias(true);
            paint.setColor(pressed ? UiTokens.BG_SURFACE_HOVER : UiTokens.BG_SURFACE);
            c.drawRoundRect(b, radius, radius, paint);
            UiDraw.hairline(c, paint, b.left, b.top, b.right, b.bottom, radius,
                    Math.max(1f, radius / 14f), UiTokens.STROKE_HAIRLINE);
        }
    }

    /** A soft accent glow behind the primary button (a translucent rounded rect). */
    public static void drawGlow(Canvas c, Paint paint, RectF b, float radius, int accent,
                                float blurRadius, float alpha) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(UiTokens.withAlpha(accent, (int) (alpha * 255)));
        paint.setShadowLayer(blurRadius, 0f, 0f, UiTokens.withAlpha(accent, (int) (alpha * 255)));
        c.drawRoundRect(new RectF(b.left, b.top, b.right, b.bottom), radius, radius, paint);
        paint.clearShadowLayer();
    }
}
