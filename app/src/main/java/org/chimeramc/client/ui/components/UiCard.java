package org.chimeramc.client.ui.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/** The shared card surface: a 10dp-radius surface with a 1dp hairline. */
public final class UiCard {

    private UiCard() {
    }

    public static void draw(Canvas c, Paint paint, RectF b, float radius) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(UiTokens.BG_SURFACE);
        c.drawRoundRect(b, radius, radius, paint);
        UiDraw.hairline(c, paint, b.left, b.top, b.right, b.bottom, radius,
                Math.max(1f, radius / 10f), UiTokens.STROKE_HAIRLINE);
    }
}
