package org.chimeramc.client.ui.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * A module list row: a 44dp surface with a 20dp leading icon, a 13sp/500 name, an optional 11sp
 * description and a trailing toggle. The row rectangle and the toggle's bounds are pure so a test can
 * assert the layout without a device.
 */
public final class UiRow {

    private UiRow() {
    }

    /** The toggle's bounds within a row of the given rectangle. */
    public static RectF toggleBounds(RectF row, float density) {
        float tw = UiTokens.TOGGLE_WIDTH * density;
        float th = UiTokens.TOGGLE_HEIGHT * density;
        float right = row.right - 12f * density;
        float top = row.centerY() - th / 2f;
        return new RectF(right - tw, top, right, top + th);
    }

    /** The leading icon's square within a row. */
    public static RectF iconBounds(RectF row, float density) {
        float size = 20f * density;
        float left = row.left + 12f * density;
        float top = row.centerY() - size / 2f;
        return new RectF(left, top, left + size, top + size);
    }

    /** Draws the row background: surface fill, accent wash when enabled, hover when pressed. */
    public static void drawBackground(Canvas c, Paint paint, RectF row, float radius,
                                      int accent, boolean enabled, boolean pressed) {
        paint.reset();
        paint.setAntiAlias(true);
        if (pressed) {
            paint.setColor(UiTokens.BG_SURFACE_HOVER);
        } else if (enabled) {
            paint.setColor(UiTokens.accentWash(accent, 0.14f));
        } else {
            paint.setColor(UiTokens.BG_SURFACE);
        }
        c.drawRoundRect(row, radius, radius, paint);
        UiDraw.hairline(c, paint, row.left, row.top, row.right, row.bottom, radius,
                Math.max(1f, radius / 10f), UiTokens.STROKE_HAIRLINE);
    }
}
