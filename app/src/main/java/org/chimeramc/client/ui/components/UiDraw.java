package org.chimeramc.client.ui.components;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

/**
 * Static canvas primitives shared by every GlowberryClient surface. The overlays draw themselves in
 * code (they cannot inflate a themed layout over the game), so the card, pill, hairline, gradient and
 * shadow geometry lives here rather than being re-derived per screen.
 *
 * <p>All sizes are taken in pixels by the caller; a caller converts dp with
 * {@code value * density}. Keeping the helpers pixel-based makes them usable from both a View and a
 * raw {@code Canvas} without a Context.
 */
public final class UiDraw {

    private UiDraw() {
    }

    /** Fills a rounded rectangle. */
    public static void roundedRect(Canvas c, Paint p, float l, float t, float r, float b,
                                   float radius) {
        c.drawRoundRect(new RectF(l, t, r, b), radius, radius, p);
    }

    /** Strokes a rounded rectangle with a 1dp hairline. */
    public static void hairline(Canvas c, Paint p, float l, float t, float r, float b,
                                float radius, float strokeWidth, int color) {
        p.reset();
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(strokeWidth);
        p.setColor(color);
        p.setAntiAlias(true);
        c.drawRoundRect(new RectF(l, t, r, b), radius, radius, p);
    }

    /** A left-to-right linear gradient fill inside a rounded rectangle. */
    public static void gradientRect(Canvas c, Paint p, float l, float t, float r, float b,
                                    float radius, int startColor, int endColor) {
        p.reset();
        p.setStyle(Paint.Style.FILL);
        p.setAntiAlias(true);
        p.setShader(new LinearGradient(l, t, r, t, startColor, endColor, Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(l, t, r, b), radius, radius, p);
        p.setShader(null);
    }

    /** A 1dp divider line. */
    public static void divider(Canvas c, Paint p, float l, float y, float r, float strokeWidth,
                               int color) {
        p.reset();
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(strokeWidth);
        p.setColor(color);
        c.drawLine(l, y, r, y, p);
    }

    /** The "corner fillet" path for a rounded annular sector, used by the radial wheel. */
    public static Path annularSectorPath(float cx, float cy, float innerR, float outerR,
                                         float startDeg, float sweepDeg, float cornerRadius) {
        Path path = new Path();
        float a0 = startDeg;
        float a1 = startDeg + sweepDeg;
        // Outer arc, then a fillet down to the inner arc, then the inner arc back, then a fillet up.
        float r = Math.min(cornerRadius, Math.min(outerR - innerR, Math.abs(sweepDeg)) / 2f);
        // Approximate rounded corners by insetting the arc endpoints; a full fillet is overkill for
        // a UI wedge and the parallel-gap geometry is what matters visually.
        float outerInsetDeg = (float) Math.toDegrees(r / Math.max(1f, outerR));
        float innerInsetDeg = (float) Math.toDegrees(r / Math.max(1f, innerR));
        path.moveTo(polarX(cx, outerR, a0 + outerInsetDeg), polarY(cy, outerR, a0 + outerInsetDeg));
        arcTo(path, cx, cy, outerR, a0 + outerInsetDeg, a1 - outerInsetDeg);
        path.lineTo(polarX(cx, innerR, a1 - innerInsetDeg), polarY(cy, innerR, a1 - innerInsetDeg));
        arcToReverse(path, cx, cy, innerR, a1 - innerInsetDeg, a0 + innerInsetDeg);
        path.close();
        return path;
    }

    private static void arcTo(Path path, float cx, float cy, float radius, float startDeg,
                              float endDeg) {
        RectF oval = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
        path.arcTo(oval, startDeg, endDeg - startDeg);
    }

    private static void arcToReverse(Path path, float cx, float cy, float radius, float startDeg,
                                     float endDeg) {
        RectF oval = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
        path.arcTo(oval, startDeg, endDeg - startDeg);
    }

    private static float polarX(float cx, float radius, float deg) {
        return cx + (float) (Math.cos(Math.toRadians(deg)) * radius);
    }

    private static float polarY(float cy, float radius, float deg) {
        return cy + (float) (Math.sin(Math.toRadians(deg)) * radius);
    }
}
