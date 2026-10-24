package org.chimeramc.client.core.mods.inbuilt.overlay;

import org.chimeramc.client.R;

import java.util.ArrayList;
import java.util.List;

/**
 * The one page registry the Mod Menu is built from.
 *
 * <p>There is a single Mod Menu now (U4). The touch screen reached from the launcher and the
 * bind-opened screen used to be two separate windows with two page lists; that is why a module or a
 * destination could exist in one and not the other. They are one screen with one registry, and the
 * pages that used to be premium-only (Controller, Keyboard) are ordinary pages marked
 * {@link #isSupporter()} — the badge is what marks them, not a second window.
 *
 * <p>This enum is the pure identity of a page (id, title, icon, supporter flag). The
 * {@code build(view)} / {@code onShow()} / {@code onHide()} behaviour stays in the overlay, because
 * it needs Android views; the registry is deliberately Android-light (resource ids only) so the
 * ordering and badge rules are unit-tested without a device.
 */
public enum ModMenuPage {
    GENERAL(R.string.mod_menu_general, R.drawable.ic_modules, false),
    VOICE(R.string.nav_voice, R.drawable.ic_nav_voice, false),
    PACKS(R.string.mod_menu_packs, R.drawable.ic_content, false),
    COSMETICS(R.string.mod_menu_cosmetics, R.drawable.ic_tshirt, false),
    HUD_EDITOR(R.string.mod_menu_hud_editor, R.drawable.ic_edit, false),
    REPLAY(R.string.nav_replay, R.drawable.ic_replay, false),
    SETTINGS(R.string.settings, R.drawable.ic_settings, false),
    // The Supporter pages. They are the same menu, the same rail and the same input routing as any
    // other page; the badge is the only difference, so a Supporter page cannot be missing from the
    // menu the way a VIP-only page could be.
    CONTROLLER(R.string.vip_tab_controller, R.drawable.ic_controller, true),
    KEYBOARD(R.string.vip_tab_keyboard, R.drawable.ic_keyboard, true);

    private final int titleRes;
    private final int iconRes;
    private final boolean supporter;

    ModMenuPage(int titleRes, int iconRes, boolean supporter) {
        this.titleRes = titleRes;
        this.iconRes = iconRes;
        this.supporter = supporter;
    }

    public int getTitleRes() {
        return titleRes;
    }

    public int getIconRes() {
        return iconRes;
    }

    /** True when the page carries the Supporter badge. */
    public boolean isSupporter() {
        return supporter;
    }

    /** Every page, in the order the rail shows them and the shoulders cycle through them. */
    public static List<ModMenuPage> pages() {
        List<ModMenuPage> out = new ArrayList<>();
        for (ModMenuPage page : values()) out.add(page);
        return out;
    }

    /** The pages that carry the Supporter badge, in order. */
    public static List<ModMenuPage> supporterPages() {
        List<ModMenuPage> out = new ArrayList<>();
        for (ModMenuPage page : values()) {
            if (page.supporter) out.add(page);
        }
        return out;
    }

    /** The page after {@code page}, wrapping. */
    public static ModMenuPage next(ModMenuPage page) {
        List<ModMenuPage> pages = pages();
        int i = pages.indexOf(page);
        return pages.get((i + 1 + pages.size()) % pages.size());
    }

    /** The page before {@code page}, wrapping. */
    public static ModMenuPage previous(ModMenuPage page) {
        List<ModMenuPage> pages = pages();
        int i = pages.indexOf(page);
        return pages.get((i - 1 + pages.size()) % pages.size());
    }

    /**
     * The page the menu opens on for a given input family. A pad user lands on the Controller
     * surface and a keyboard user on the Keyboard surface, so the relevant input page is one press
     * away; the module grid stays a shoulder press away either way.
     */
    public static ModMenuPage defaultForInput(boolean hasGamepad) {
        return hasGamepad ? CONTROLLER : KEYBOARD;
    }
}
