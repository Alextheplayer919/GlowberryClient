package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.chimeramc.client.R;
import org.chimeramc.client.ui.components.UiTokens;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The touch Mod Menu's icon rail.
 *
 * <p>The rail is built from {@link ModMenuPage#pages()}, the single page registry the whole menu
 * reads, so a new destination is one enum entry. A page flagged
 * {@link ModMenuPage#isSupporter()} gets a small accent dot on its tile, which is the Supporter
 * badge: the page is otherwise an ordinary page of the same menu (U4).
 *
 * <p>This class owns only view construction and the selection fill; which page is currently shown
 * stays with the overlay, which passes a selection callback.
 */
final class ModMenuRail {

    private final Activity activity;
    private final LinearLayout container;
    private final Map<ModMenuPage, FrameLayout> tiles = new EnumMap<>(ModMenuPage.class);
    private final Map<ModMenuPage, ImageView> icons = new EnumMap<>(ModMenuPage.class);
    private final int accent;
    private ModMenuPage selected;

    ModMenuRail(Activity activity, LinearLayout container, int accent) {
        this.activity = activity;
        this.container = container;
        this.accent = accent;
    }

    /** Builds one tile per shared page. Idempotent: an existing rail is cleared first. */
    void build(Consumer<ModMenuPage> onSelect) {
        if (container == null) return;
        container.removeAllViews();
        tiles.clear();
        icons.clear();

        int size = (int) (UiTokens.RAIL_BUTTON * density());
        int gap = (int) (8 * density());
        for (final ModMenuPage page : ModMenuPage.pages()) {
            // A fixed-size tile with a centred icon, matching the VIP rail. A TextView with a
            // compound drawable positions the icon against an empty text line box, so it drifts
            // off centre at some densities.
            FrameLayout tile = new FrameLayout(activity);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.bottomMargin = gap;
            tile.setLayoutParams(lp);
            tile.setClickable(true);
            tile.setFocusable(false);
            tile.setContentDescription(activity.getString(page.getTitleRes()));

            ImageView icon = new ImageView(activity);
            int iconSize = (int) (22 * density());
            FrameLayout.LayoutParams iconLp =
                    new FrameLayout.LayoutParams(iconSize, iconSize);
            iconLp.gravity = Gravity.CENTER;
            icon.setLayoutParams(iconLp);
            icon.setImageResource(page.getIconRes());
            tile.addView(icon);

            if (page.isSupporter()) {
                // The Supporter badge: a small accent dot in the tile's corner. A dot rather than a
                // second label keeps the rail icon-only at every width, which is what lets nine
                // destinations fit the landscape height.
                View badge = new View(activity);
                int badgeSize = (int) (7 * density());
                FrameLayout.LayoutParams badgeLp =
                        new FrameLayout.LayoutParams(badgeSize, badgeSize);
                badgeLp.gravity = Gravity.END | Gravity.TOP;
                badgeLp.topMargin = (int) (5 * density());
                badgeLp.rightMargin = (int) (5 * density());
                badge.setLayoutParams(badgeLp);
                android.graphics.drawable.GradientDrawable dot =
                        new android.graphics.drawable.GradientDrawable();
                dot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                dot.setColor(accent);
                badge.setBackground(dot);
                tile.addView(badge);
            }

            tile.setOnClickListener(v -> onSelect.accept(page));
            tile.setOnLongClickListener(v -> {
                Toast.makeText(activity, page.getTitleRes(), Toast.LENGTH_SHORT).show();
                return true;
            });

            container.addView(tile);
            tiles.put(page, tile);
            icons.put(page, icon);
        }
        applySelection();
    }

    /** Paints the selection: the active tile gets an accent-tinted icon and a filled pill. */
    void setSelected(ModMenuPage page) {
        if (page == null || !tiles.containsKey(page)) return;
        selected = page;
        applySelection();
    }

    private void applySelection() {
        for (Map.Entry<ModMenuPage, ImageView> entry : icons.entrySet()) {
            boolean active = entry.getKey() == selected;
            entry.getValue().setColorFilter(active ? accent : UiTokens.TEXT_SECONDARY);
            FrameLayout tile = tiles.get(entry.getKey());
            if (tile != null) {
                tile.setBackground(active ? activeTile() : null);
            }
        }
    }

    private android.graphics.drawable.GradientDrawable activeTile() {
        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        bg.setCornerRadius(12 * density());
        bg.setColor(UiTokens.BG_SURFACE_HOVER);
        bg.setStroke(Math.max(1, (int) density()), withAlpha(accent, 0x55));
        return bg;
    }

    private float density() {
        return activity.getResources().getDisplayMetrics().density;
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }
}
