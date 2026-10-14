package org.chimeramc.client.core.mods.inbuilt.vip;

import org.chimeramc.client.core.mods.inbuilt.UnifiedMod;
import org.chimeramc.client.core.mods.inbuilt.model.ModIds;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The mod panel's category tabs and the rule that decides which modules each shows.
 *
 * <p>Pure and Android-free (it works on {@link UnifiedMod} ids and group ids), so the tab behaviour
 * is unit-testable: a category must be a strict partition of the module list plus an "All" that
 * shows everything, and Favorites must show exactly the pinned set. Keeping the predicate here stops
 * the tab chips, the list and the empty state from disagreeing about what a category means.
 */
public final class ModCategory {

    public enum Tab {
        ALL,
        FAVORITES,
        VISUALS,
        PVP,
        MISC,
    }

    /** The visual modules the VISUALS tab collects. */
    private static final Set<String> VISUAL = Set.of(
            ModIds.FULLBRIGHT, ModIds.TIME_CHANGER, ModIds.LOW_FIRE, ModIds.BLOCK_OUTLINE,
            ModIds.MOTION_BLUR, ModIds.SATURATION, ModIds.CROSSHAIR, ModIds.ARMOR_VIEW_BENDING,
            ModIds.PROJECTILE_SCALER, ModIds.FREECAM, ModIds.TNT, ModIds.ZOOM, ModIds.FPS_DISPLAY,
            ModIds.CPS_DISPLAY);

    /** The information/utility modules the MISC tab collects. */
    private static final Set<String> MISC = Set.of(
            ModIds.LIST_PLAYERS, ModIds.DEBUG_MENU, ModIds.THIRD_PERSON_NAMETAG,
            ModIds.AUTO_SPRINT, ModIds.QUICK_DROP, ModIds.CAMERA_PERSPECTIVE, ModIds.TOGGLE_HUD,
            ModIds.SNAPLOOK, ModIds.VIRTUAL_CURSOR, ModIds.GYRO, ModIds.POJAV_CONTROLS,
            ModIds.MORE_BUTTONS, ModIds.HOTBAR_SLOT, ModIds.OFFHAND, ModIds.MOD_MENU);

    private ModCategory() {
    }

    /** Whether a module belongs to a tab. FAVORITES needs the pinned set; ALL matches everything. */
    public static boolean matches(Tab tab, UnifiedMod mod, Set<String> favorites) {
        if (tab == null || tab == Tab.ALL) return true;
        if (mod == null) return false;
        String id = mod.getId();
        switch (tab) {
            case FAVORITES:
                return favorites != null && favorites.contains(mod.getStableKey());
            case PVP:
                return ModIds.isPvpModule(id) || ModIds.isVoiceModule(id);
            case VISUALS:
                return VISUAL.contains(id);
            case MISC:
                return MISC.contains(id);
            default:
                return true;
        }
    }

    /**
     * Filters a list to a tab, keeping order. A Favorites tab with an empty pinned set yields an
     * empty list, which is what the empty state keys on.
     */
    public static List<UnifiedMod> filter(Tab tab, List<UnifiedMod> mods, Set<String> favorites) {
        List<UnifiedMod> out = new ArrayList<>();
        if (mods == null) return out;
        for (UnifiedMod mod : mods) {
            if (matches(tab, mod, favorites)) out.add(mod);
        }
        return out;
    }

    /** True when a module name contains the (case-insensitive) query, for the search box. */
    public static boolean matchesQuery(UnifiedMod mod, String query) {
        if (query == null || query.trim().isEmpty()) return true;
        if (mod == null) return false;
        String q = query.trim().toLowerCase(Locale.ROOT);
        // Search the name, the description and the category, so "zoom", "camera" or "pvp" all
        // find the module the player means.
        return contains(mod.getName(), q)
                || contains(mod.getDescription(), q)
                || contains(mod.getGroupName(), q)
                || contains(mod.getGroupId(), q);
    }

    private static boolean contains(String haystack, String lowerQuery) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(lowerQuery);
    }
}
