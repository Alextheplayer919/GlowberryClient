package org.chimeramc.client.core.mods.inbuilt.overlay;

import org.chimeramc.client.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The one page registry both mod-menu windows read, so the touch Mod Menu (Screen A) and the VIP
 * Mod Menu (Screen B) cannot drift.
 *
 * <p>The VIP menu is "the normal menu plus its own extras": every page the normal menu offers is
 * available in VIP, in the same order, and the VIP-only pages (Controller, Keyboard) come last.
 * Adding a page here makes it appear in both windows.
 *
 * <p>This enum is the pure identity of a page (id, title, icon, whether it is VIP-only). The
 * {@code build(view)} / {@code onShow()} / {@code onHide()} behaviour stays in each overlay, because
 * it needs Android views; the registry is deliberately Android-light (resource ids only) so the
 * parity rule is unit-tested without a device.
 */
public enum ModMenuPage {
    GENERAL(R.string.mod_menu_general, R.drawable.ic_modules, false),
    VOICE(R.string.nav_voice, R.drawable.ic_nav_voice, false),
    PACKS(R.string.mod_menu_packs, R.drawable.ic_content, false),
    COSMETICS(R.string.mod_menu_cosmetics, R.drawable.ic_tshirt, false),
    HUD_EDITOR(R.string.mod_menu_hud_editor, R.drawable.ic_edit, false),
    REPLAY(R.string.nav_replay, R.drawable.ic_replay, false),
    SETTINGS(R.string.settings, R.drawable.ic_settings, false),
    // VIP-only pages, always last.
    CONTROLLER(R.string.vip_tab_controller, R.drawable.ic_controller, true),
    KEYBOARD(R.string.vip_tab_keyboard, R.drawable.ic_keyboard, true);

    private final int titleRes;
    private final int iconRes;
    private final boolean vipOnly;

    ModMenuPage(int titleRes, int iconRes, boolean vipOnly) {
        this.titleRes = titleRes;
        this.iconRes = iconRes;
        this.vipOnly = vipOnly;
    }

    public int getTitleRes() {
        return titleRes;
    }

    public int getIconRes() {
        return iconRes;
    }

    /** True when the page is offered only by the VIP menu (Controller, Keyboard). */
    public boolean isVipOnly() {
        return vipOnly;
    }

    /** Every page both windows share, in the order the normal menu shows them. */
    public static List<ModMenuPage> sharedPages() {
        List<ModMenuPage> out = new ArrayList<>();
        for (ModMenuPage page : values()) {
            if (!page.vipOnly) out.add(page);
        }
        return out;
    }

    /**
     * The VIP menu's pages: every shared page in the normal order, then the VIP-only pages last.
     * This is the list the VIP sidebar/tab bar is built from.
     */
    public static List<ModMenuPage> vipPages() {
        List<ModMenuPage> out = new ArrayList<>(sharedPages());
        for (ModMenuPage page : values()) {
            if (page.vipOnly) out.add(page);
        }
        return out;
    }

    /** The page after {@code page} in the VIP order, wrapping. */
    public static ModMenuPage vipNext(ModMenuPage page) {
        List<ModMenuPage> pages = vipPages();
        int i = pages.indexOf(page);
        return pages.get((i + 1 + pages.size()) % pages.size());
    }

    /** The page before {@code page} in the VIP order, wrapping. */
    public static ModMenuPage vipPrevious(ModMenuPage page) {
        List<ModMenuPage> pages = vipPages();
        int i = pages.indexOf(page);
        return pages.get((i - 1 + pages.size()) % pages.size());
    }

    /** True when every shared page id appears in the VIP list (the parity invariant). */
    public static boolean vipCoversEverySharedPage() {
        return vipPages().containsAll(sharedPages());
    }

    /** The VIP-only pages, in order. */
    public static List<ModMenuPage> vipOnlyPages() {
        return Arrays.asList(CONTROLLER, KEYBOARD);
    }

    /**
     * The page the VIP menu opens on for a given input family. A pad user lands on the Controller
     * surface and a keyboard user on the Keyboard surface, so the relevant input page is one press
     * away; the module grid stays a shoulder press away either way.
     */
    public static ModMenuPage defaultForInput(boolean hasGamepad) {
        return hasGamepad ? CONTROLLER : KEYBOARD;
    }
}
