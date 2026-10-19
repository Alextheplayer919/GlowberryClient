package org.chimeramc.client.ui.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * The GlowberryClient outline icon set: 24dp grid, 2dp stroke, round caps. Original artwork drawn as
 * vector paths (no third-party assets). Used by the radial wheel sectors and the mod-menu rail.
 *
 * <p>Each icon is drawn centred on {@code (cx, cy)} with the given {@code size} (the 24dp box in px)
 * and colour. The paths are built from the unit grid so a caller can scale freely.
 */
public final class UiIcons {

    public enum Icon { CUBE, PENCIL, HANGER, GEAR, ALL, PVP, VISUAL, MOVEMENT, UTILITY, HUD, VOICE,
        COSMETICS, SEARCH, CLOSE, SETTINGS, REPLAY,
        // The Closet's category rail: one icon per cosmetic section, so the rail reads as icons
        // rather than a column of words.
        CAPE, HAT, PAW, BACKPACK, OFFHAND, SPARKLE,
        // A "this opens another level" affordance, for a nested radial sub-wheel and a back entry.
        CHEVRON }

    private UiIcons() {
    }

    /** Draws an icon centred at (cx, cy), sized to fit a {@code size}-px box. */
    public static void draw(Canvas c, Paint paint, Icon icon, float cx, float cy, float size,
                            int color, float density) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1.5f, 2f * density));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(color);

        float s = size / 24f;  // unit scale: a 24-unit grid.
        float h = size / 2f;
        switch (icon) {
            case CUBE: drawCube(c, paint, cx, cy, s); break;
            case PENCIL: drawPencil(c, paint, cx, cy, s); break;
            case HANGER: drawHanger(c, paint, cx, cy, s); break;
            case GEAR: drawGear(c, paint, cx, cy, s); break;
            case ALL: drawGrid(c, paint, cx, cy, s); break;
            case PVP: drawTarget(c, paint, cx, cy, s); break;
            case VISUAL: drawEye(c, paint, cx, cy, s); break;
            case MOVEMENT: drawArrow(c, paint, cx, cy, s); break;
            case UTILITY: drawWrench(c, paint, cx, cy, s); break;
            case HUD: drawHud(c, paint, cx, cy, s); break;
            case VOICE: drawVoice(c, paint, cx, cy, s); break;
            case COSMETICS: drawHanger(c, paint, cx, cy, s); break;
            case SEARCH: drawSearch(c, paint, cx, cy, s); break;
            case CLOSE: drawClose(c, paint, cx, cy, s); break;
            case SETTINGS: drawGear(c, paint, cx, cy, s); break;
            case REPLAY: drawReplay(c, paint, cx, cy, s); break;
            case CAPE: drawCape(c, paint, cx, cy, s); break;
            case HAT: drawHat(c, paint, cx, cy, s); break;
            case PAW: drawPaw(c, paint, cx, cy, s); break;
            case BACKPACK: drawBackpack(c, paint, cx, cy, s); break;
            case OFFHAND: drawOffhand(c, paint, cx, cy, s); break;
            case SPARKLE: drawSparkle(c, paint, cx, cy, s); break;
            case CHEVRON: drawChevron(c, paint, cx, cy, s); break;
            default: break;
        }
    }

    /** A right-pointing chevron: the "opens another level / back" affordance. */
    private static void drawChevron(Canvas c, Paint p, float cx, float cy, float s) {
        float r = 6f * s;
        Path path = new Path();
        path.moveTo(cx - r * 0.5f, cy - r);
        path.lineTo(cx + r * 0.5f, cy);
        path.lineTo(cx - r * 0.5f, cy + r);
        c.drawPath(path, p);
    }

    private static void drawCube(Canvas c, Paint p, float cx, float cy, float s) {
        float r = 9f * s;
        // Front face.
        float fl = cx - r, fr = cx + r * 0.1f, ft = cy - r * 0.3f, fb = cy + r;
        Path front = new Path();
        front.moveTo(fl, ft);
        front.lineTo(fr, ft);
        front.lineTo(fr, fb);
        front.lineTo(fl, fb);
        front.close();
        c.drawPath(front, p);
        // Top and right parallelograms (an isometric cube).
        Path top = new Path();
        top.moveTo(fl, ft);
        top.lineTo(fl + r * 0.5f, ft - r * 0.5f);
        top.lineTo(fr + r * 0.5f, ft - r * 0.5f);
        top.lineTo(fr, ft);
        c.drawPath(top, p);
        Path right = new Path();
        right.moveTo(fr, ft);
        right.lineTo(fr + r * 0.5f, ft - r * 0.5f);
        right.lineTo(fr + r * 0.5f, fb - r * 0.5f);
        right.lineTo(fr, fb);
        c.drawPath(right, p);
    }

    private static void drawPencil(Canvas c, Paint p, float cx, float cy, float s) {
        float r = 8f * s;
        Path body = new Path();
        body.moveTo(cx - r, cy + r);
        body.lineTo(cx - r + 2 * s, cy + r - 2 * s);
        body.lineTo(cx + r - 4 * s, cy - r + 4 * s);
        body.lineTo(cx + r - 2 * s, cy - r + 2 * s);
        body.lineTo(cx + r - 8 * s, cy - r);
        body.close();
        c.drawPath(body, p);
        // Tip.
        c.drawLine(cx - r, cy + r, cx - r + 3 * s, cy + r - 1 * s, p);
    }

    private static void drawHanger(Canvas c, Paint p, float cx, float cy, float s) {
        float r = 8f * s;
        // Head.
        c.drawCircle(cx, cy - r * 0.7f, r * 0.35f, p);
        // Shoulders / hanger bar.
        Path bar = new Path();
        bar.moveTo(cx - r, cy + r * 0.5f);
        bar.lineTo(cx - r * 0.4f, cy - r * 0.1f);
        bar.lineTo(cx, cy + r * 0.1f);
        bar.lineTo(cx + r * 0.4f, cy - r * 0.1f);
        bar.lineTo(cx + r, cy + r * 0.5f);
        c.drawPath(bar, p);
        // Hook.
        c.drawLine(cx, cy - r * 0.35f, cx, cy - r * 0.1f, p);
    }

    private static void drawGear(Canvas c, Paint p, float cx, float cy, float s) {
        float r = 7f * s;
        c.drawCircle(cx, cy, r * 0.45f, p);
        c.drawCircle(cx, cy, r, p);
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45.0);
            float x0 = cx + (float) (Math.cos(a) * r);
            float y0 = cy + (float) (Math.sin(a) * r);
            float x1 = cx + (float) (Math.cos(a) * (r + 2.5f * s));
            float y1 = cy + (float) (Math.sin(a) * (r + 2.5f * s));
            c.drawLine(x0, y0, x1, y1, p);
        }
    }

    private static void drawGrid(Canvas c, Paint p, float cx, float cy, float s) {
        float r = 8f * s;
        float g = r;
        for (int i = 0; i <= 1; i++) {
            for (int j = 0; j <= 1; j++) {
                float x = cx - r + i * g;
                float y = cy - r + j * g;
                c.drawRect(x, y, x + g * 0.7f, y + g * 0.7f, p);
            }
        }
    }

    private static void drawTarget(Canvas c, Paint p, float cx, float cy, float s) {
        float r = 8f * s;
        c.drawCircle(cx, cy, r, p);
        c.drawCircle(cx, cy, r * 0.4f, p);
        c.drawLine(cx - r, cy, cx - r * 0.4f, cy, p);
        c.drawLine(cx + r * 0.4f, cy, cx + r, cy, p);
        c.drawLine(cx, cy - r, cx, cy - r * 0.4f, p);
        c.drawLine(cx, cy + r * 0.4f, cx, cy + r, p);
    }

    private static void drawEye(Canvas c, Paint p, float cx, float cy, float s) {
        float r = 9f * s;
        Path eye = new Path();
        eye.moveTo(cx - r, cy);
        eye.quadTo(cx, cy - r * 0.8f, cx + r, cy);
        eye.quadTo(cx, cy + r * 0.8f, cx - r, cy);
        c.drawPath(eye, p);
        c.drawCircle(cx, cy, r * 0.28f, p);
    }

    private static void drawArrow(Canvas c, Paint p, float cx, float cy, float s) {
        float r = 8f * s;
        c.drawLine(cx, cy + r, cx, cy - r, p);
        Path head = new Path();
        head.moveTo(cx - r * 0.5f, cy - r * 0.4f);
        head.lineTo(cx, cy - r);
        head.lineTo(cx + r * 0.5f, cy - r * 0.4f);
        c.drawPath(head, p);
    }

    private static void drawWrench(Canvas c, Paint p, float cx, float cy, float s) {
        float r = 8f * s;
        c.drawCircle(cx - r * 0.5f, cy - r * 0.5f, r * 0.35f, p);
        c.drawLine(cx - r * 0.25f, cy - r * 0.25f, cx + r * 0.7f, cy + r * 0.7f, p);
        c.drawLine(cx + r * 0.4f, cy + r * 0.4f, cx + r * 0.8f, cy + r * 0.8f, p);
    }

    private static void drawHud(Canvas c, Paint p, float cx, float cy, float s) {
        float r = 8f * s;
        c.drawRect(cx - r, cy - r * 0.7f, cx + r, cy + r * 0.7f, p);
        c.drawLine(cx - r, cy - r * 0.3f, cx + r, cy - r * 0.3f, p);
    }

    private static void drawVoice(Canvas c, Paint p, float cx, float cy, float s) {
        float r = 6f * s;
        c.drawRoundRect(cx - r * 0.4f, cy - r, cx + r * 0.4f, cy + r * 0.2f, r * 0.4f, r * 0.4f, p);
        Path stand = new Path();
        stand.moveTo(cx - r * 0.8f, cy);
        stand.quadTo(cx, cy + r * 1.2f, cx + r * 0.8f, cy);
        c.drawPath(stand, p);
        c.drawLine(cx, cy + r * 1.1f, cx, cy + r * 1.6f, p);
    }

    private static void drawSearch(Canvas c, Paint p, float cx, float cy, float s) {
        float r = 6f * s;
        c.drawCircle(cx - 1 * s, cy - 1 * s, r, p);
        c.drawLine(cx - 1 * s + r * 0.7f, cy - 1 * s + r * 0.7f,
                cx + r * 1.1f, cy + r * 1.1f, p);
    }

    private static void drawClose(Canvas c, Paint p, float cx, float cy, float s) {
        float r = 6f * s;
        c.drawLine(cx - r, cy - r, cx + r, cy + r, p);
        c.drawLine(cx + r, cy - r, cx - r, cy + r, p);
    }

    private static void drawReplay(Canvas c, Paint p, float cx, float cy, float s) {
        float r = 7f * s;
        android.graphics.RectF oval = new android.graphics.RectF(cx - r, cy - r, cx + r, cy + r);
        c.drawArc(oval, 40f, 280f, false, p);
        Path head = new Path();
        head.moveTo(cx + r * 0.9f, cy - r * 0.4f);
        head.lineTo(cx + r * 1.2f, cy + r * 0.1f);
        head.lineTo(cx + r * 0.5f, cy + r * 0.1f);
        c.drawPath(head, p);
    }

    /** A cape: a collar bar with the cloth hanging in a soft trapezoid. */
    private static void drawCape(Canvas c, Paint p, float cx, float cy, float s) {
        float r = 8f * s;
        Path cloth = new Path();
        cloth.moveTo(cx - r * 0.7f, cy - r * 0.7f);
        cloth.lineTo(cx + r * 0.7f, cy - r * 0.7f);
        cloth.lineTo(cx + r, cy + r);
        cloth.lineTo(cx - r, cy + r);
        cloth.close();
        c.drawPath(cloth, p);
        c.drawLine(cx - r * 0.7f, cy - r * 0.7f, cx + r * 0.7f, cy - r * 0.7f, p);
    }

    /** A hat: a brim plus a domed crown. */
    private static void drawHat(Canvas c, Paint p, float cx, float cy, float s) {
        float r = 8f * s;
        c.drawLine(cx - r, cy + r * 0.4f, cx + r, cy + r * 0.4f, p);
        Path crown = new Path();
        crown.moveTo(cx - r * 0.6f, cy + r * 0.4f);
        crown.lineTo(cx - r * 0.5f, cy - r * 0.5f);
        crown.quadTo(cx, cy - r, cx + r * 0.5f, cy - r * 0.5f);
        crown.lineTo(cx + r * 0.6f, cy + r * 0.4f);
        c.drawPath(crown, p);
    }

    /** A paw print: a pad plus four toes. */
    private static void drawPaw(Canvas c, Paint p, float cx, float cy, float s) {
        float r = 8f * s;
        Path pad = new Path();
        pad.moveTo(cx, cy - r * 0.2f);
        pad.quadTo(cx + r * 0.7f, cy + r * 0.3f, cx + r * 0.35f, cy + r * 0.9f);
        pad.lineTo(cx - r * 0.35f, cy + r * 0.9f);
        pad.quadTo(cx - r * 0.7f, cy + r * 0.3f, cx, cy - r * 0.2f);
        c.drawPath(pad, p);
        c.drawCircle(cx - r * 0.7f, cy - r * 0.35f, r * 0.22f, p);
        c.drawCircle(cx - r * 0.25f, cy - r * 0.75f, r * 0.22f, p);
        c.drawCircle(cx + r * 0.25f, cy - r * 0.75f, r * 0.22f, p);
        c.drawCircle(cx + r * 0.7f, cy - r * 0.35f, r * 0.22f, p);
    }

    /** A backpack: a rounded pack body with a lid and straps. */
    private static void drawBackpack(Canvas c, Paint p, float cx, float cy, float s) {
        float r = 8f * s;
        c.drawRoundRect(cx - r * 0.7f, cy - r * 0.6f, cx + r * 0.7f, cy + r, r * 0.35f, r * 0.35f, p);
        c.drawLine(cx - r * 0.7f, cy - r * 0.1f, cx + r * 0.7f, cy - r * 0.1f, p);
        Path straps = new Path();
        straps.moveTo(cx - r * 0.35f, cy - r * 0.6f);
        straps.quadTo(cx - r * 0.9f, cy - r, cx - r * 0.35f, cy + r * 0.2f);
        straps.moveTo(cx + r * 0.35f, cy - r * 0.6f);
        straps.quadTo(cx + r * 0.9f, cy - r, cx + r * 0.35f, cy + r * 0.2f);
        c.drawPath(straps, p);
    }

    /** An offhand: a shield outline with a centre boss. */
    private static void drawOffhand(Canvas c, Paint p, float cx, float cy, float s) {
        float r = 8f * s;
        Path shield = new Path();
        shield.moveTo(cx - r * 0.7f, cy - r * 0.8f);
        shield.lineTo(cx + r * 0.7f, cy - r * 0.8f);
        shield.lineTo(cx + r * 0.7f, cy + r * 0.2f);
        shield.quadTo(cx, cy + r, cx - r * 0.7f, cy + r * 0.2f);
        shield.close();
        c.drawPath(shield, p);
        c.drawCircle(cx, cy - r * 0.15f, r * 0.25f, p);
    }

    /** An effects sparkle: one large four-point star with two smaller ones. */
    private static void drawSparkle(Canvas c, Paint p, float cx, float cy, float s) {
        drawStar(c, p, cx, cy - s, 7f * s);
        drawStar(c, p, cx - 5.5f * s, cy + 4f * s, 3.5f * s);
        drawStar(c, p, cx + 5.5f * s, cy + 5f * s, 3f * s);
    }

    /** A four-point sparkle: a diamond whose concave sides read as a star. */
    private static void drawStar(Canvas c, Paint p, float cx, float cy, float r) {
        Path star = new Path();
        star.moveTo(cx, cy - r);
        star.quadTo(cx + r * 0.18f, cy - r * 0.18f, cx + r, cy);
        star.quadTo(cx + r * 0.18f, cy + r * 0.18f, cx, cy + r);
        star.quadTo(cx - r * 0.18f, cy + r * 0.18f, cx - r, cy);
        star.quadTo(cx - r * 0.18f, cy - r * 0.18f, cx, cy - r);
        star.close();
        c.drawPath(star, p);
    }
}
