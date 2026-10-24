package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * The rule that decides whether a one-shot action belongs in the section currently on screen (M3).
 *
 * <p>An action is filed under a module-list section, and the menu's filter is what chooses that
 * section: the PvP filter shows the PvP section, the Inbuilt filter the inbuilt section. Rendering
 * every action on every filter would put "Reset HUD Position" beside the external-mod list, which
 * reads as a bug, so the action set follows the section exactly as the module set does.
 *
 * <p>The action's group id and a module's group id share one vocabulary ({@code inbuilt}, {@code pvp}),
 * so the rule is a plain comparison. {@code ALL} shows every section at once, so it shows every
 * action. The favourites, enabled and visuals/misc views are a mixed view of several sections
 * rather than a section of their own, so they show none rather than a guessed subset.
 *
 * <p>Pure and Android-free, so the placement is unit-tested without a device.
 */
public final class ModMenuActionVisibility {

    private ModMenuActionVisibility() {}

    /**
     * Whether an action belongs in the section a filter shows.
     *
     * @param filterName the active filter's name ({@code ALL}, {@code INBUILT}, {@code PVP}, …), as
     *     the overlay already persists it for the saved order
     * @param actionGroupId the section the action is filed under
     */
    public static boolean isVisibleIn(String filterName, String actionGroupId) {
        if (filterName == null || actionGroupId == null) {
            return false;
        }
        if ("ALL".equals(filterName)) {
            // Every section is on screen at once, so every action is too.
            return true;
        }
        String section = sectionIdFor(filterName);
        return section != null && section.equals(actionGroupId);
    }

    /** The module-list section a filter shows, or null when the filter is not a single section. */
    public static String sectionIdFor(String filterName) {
        if (filterName == null) {
            return null;
        }
        switch (filterName) {
            case "INBUILT":
                return "inbuilt";
            case "PVP":
                return "pvp";
            default:
                // ALL is handled by the caller; FAVORITES, ENABLED, EXTERNAL, VISUALS and MISC are
                // mixed views rather than a section.
                return null;
        }
    }
}
