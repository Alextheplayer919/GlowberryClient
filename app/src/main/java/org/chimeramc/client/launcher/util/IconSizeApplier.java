package org.chimeramc.client.launcher.util;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.chimeramc.client.R;

/**
 * Applies the "Icon Size" preference to a view tree.
 *
 * <p>The preference existed and was persisted, but nothing consumed it: {@code applyIconSize} was a
 * no-op with a "placeholder for future icon scaling" comment, so moving the slider changed a number
 * and nothing else. This class is the consumer.
 *
 * <p>The scaled size is always derived from a <em>cached base</em>, never from the view's current
 * size. Deriving from the current size compounds the scale on every re-apply, and this runs on each
 * {@code onResume} — so 110% would become 121%, then 133%. The base is captured the first time a
 * view is seen and kept in a tag, which survives view recycling for the same reason the press
 * animation state does.
 *
 * <p>Only fixed-size dimensions are touched: a {@code match_parent} view is a layout instruction,
 * not an icon size, and scaling it would collapse a full-width element to a few dp.
 */
public final class IconSizeApplier {

    private IconSizeApplier() {
    }

    /** Applies {@code percent} to every icon in {@code root}. */
    public static void apply(View root, int percent) {
        if (root == null) return;
        applyRecursive(root, percent);
    }

    private static void applyRecursive(View view, int percent) {
        if (view instanceof ImageView) {
            applyToImageView((ImageView) view, percent);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyRecursive(group.getChildAt(i), percent);
            }
        }
    }

    private static void applyToImageView(ImageView image, int percent) {
        ViewGroup.LayoutParams params = image.getLayoutParams();
        if (params == null) return;
        // A view that has not been measured yet reports a negative dimension for match_parent /
        // wrap_content, which is exactly what must not be scaled; the base is captured on the next
        // pass once the layout has resolved it.
        if (!IconSizePolicy.isScalableDimension(params.width)) return;

        int baseWidth = baseSize(image, R.id.icon_size_base_width, params.width);
        int baseHeight = baseSize(image, R.id.icon_size_base_height, params.height);

        int width = IconSizePolicy.scaledPx(baseWidth, percent);
        int height = baseHeight > 0 ? IconSizePolicy.scaledPx(baseHeight, percent) : params.height;
        if (params.width == width && params.height == height) return;
        params.width = width;
        params.height = height;
        image.setLayoutParams(params);
    }

    /**
     * The view's unscaled size, captured once.
     *
     * <p>Stored as a tag rather than recomputed because the live dimension has already been scaled
     * by a previous pass.
     */
    private static int baseSize(ImageView image, int tagKey, int current) {
        Object cached = image.getTag(tagKey);
        if (cached instanceof Integer) {
            int base = (Integer) cached;
            // A base of 0 means the first pass saw the view before it was laid out; re-capture it
            // now rather than locking in a zero that would hide the icon.
            if (base > 0) return base;
        }
        image.setTag(tagKey, current);
        return current;
    }
}
