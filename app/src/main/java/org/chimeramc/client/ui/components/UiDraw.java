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

    /**
     * Draws the controller cursor as a clean pointer arrow with a small soft shadow.
     *
     * <p>Replaces the old 10dp disc + glow + ring: the glow read as a smear and the disc hid what
     * it was hovering over. The arrow is a standard mouse-pointer silhouette (~7dp wide, ~10dp tall)
     * with a 1dp white fill, a 1dp dark outline and a faint offset shadow. The tip is the hotspot,
     * so a click lands exactly where the arrow points.
     *
     * @param shadowFill the paint used for the shadow (reused across draws, reset each call)
     * @param arrowFill  the paint used for the arrow body (reused across draws, reset each call)
     */
    public static void pointerCursor(Canvas canvas, Paint shadowFill, Paint arrowFill,
                                     float x, float y, float density) {
        float s = 7f * density;      // arrow width
        float h = 10f * density;     // arrow height
        // Standard pointer silhouette: tip, then a right edge that steps down into the tail, with a
        // notch on the bottom edge before the tail's lower-right corner.
        Path shape = new Path();
        shape.moveTo(x, y);
        shape.lineTo(x + s * 0.55f, y + h * 0.55f);
        shape.lineTo(x + s * 0.22f, y + h * 0.55f);
        shape.lineTo(x + s * 0.22f, y + h);
        shape.lineTo(x - s * 0.1f, y + h * 0.75f);
        shape.lineTo(x - s * 0.1f, y + h * 0.55f);
        shape.lineTo(x - s * 0.32f, y + h * 0.78f);
        shape.lineTo(x - s * 0.68f, y + h * 0.40f);
        // Close the shape back to the tip via the upper-left side.
        shape.lineTo(x - s * 0.35f, y + s * 0.30f);
        shape.close();

        // Shadow: a 1dp offset copy in black at 18% alpha, blurred by the caller (we do not paint a
        // shadow layer here; the caller's paint can carry one as a small convenience, but a plain
        // offset fill reads cleaner and costs nothing).
        shadowFill.reset();
        shadowFill.setAntiAlias(true);
        shadowFill.setColor(0x2E000000);
        canvas.save();
        canvas.translate(0.8f * density, 1.1f * density);
        canvas.drawPath(shape, shadowFill);
        canvas.restore();

        // Arrow body: near-white fill with a 1dp dark outline so it reads on any backdrop.
        arrowFill.reset();
        arrowFill.setAntiAlias(true);
        arrowFill.setStyle(Paint.Style.FILL);
        arrowFill.setColor(0xFFFAFAFA);
        canvas.drawPath(shape, arrowFill);
        arrowFill.setStyle(Paint.Style.STROKE);
        arrowFill.setStrokeWidth(Math.max(1f, 0.75f * density));
        arrowFill.setColor(0xB3000000);
        canvas.drawPath(shape, arrowFill);
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
