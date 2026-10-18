package org.chimeramc.client.ui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * A small View that draws one {@link UiIcons.Icon} in a given colour.
 *
 * <p>The icon set is drawn in code (the overlays cannot inflate a themed layout over the game), so a
 * screen that wants an icon as an actual view — the Closet's category rail — needs a view that owns
 * the paint and the size. This keeps a single cached {@link Paint} per view (never allocated in
 * {@link #onDraw}) and re-draws only when the icon or colour changes.
 *
 * <p>Both constructors are declared so the view can be built in code or inflated from XML.
 */
public class UiIconView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private UiIcons.Icon icon = UiIcons.Icon.CUBE;
    private int color = UiTokens.TEXT_PRIMARY;

    public UiIconView(Context context) {
        super(context);
    }

    public UiIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setIcon(UiIcons.Icon icon) {
        if (icon == null || icon == this.icon) return;
        this.icon = icon;
        invalidate();
    }

    public void setIconColor(int color) {
        if (color == this.color) return;
        this.color = color;
        invalidate();
    }

    public UiIcons.Icon getIcon() {
        return icon;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float size = Math.min(getWidth(), getHeight());
        UiIcons.draw(canvas, paint, icon, getWidth() / 2f, getHeight() / 2f, size, color, density);
    }
}
