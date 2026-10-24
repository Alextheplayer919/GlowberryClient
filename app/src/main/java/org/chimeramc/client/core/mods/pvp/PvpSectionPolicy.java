package org.chimeramc.client.core.mods.pvp;

import org.chimeramc.client.core.mods.inbuilt.catalog.ModuleScope;
import org.chimeramc.client.core.mods.inbuilt.catalog.PvpModuleScopes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The decision about the PvP section: what it is, and what it may be advertised as.
 *
 * <p>The question this answers is a positioning question, not a code question. The launcher ships
 * two different kinds of "PvP" content and they must not be marketed as one thing:
 *
 * <ul>
 *   <li>The <b>shipped PvP modules</b> — a small set that acts only on the player's own input
 *       (aim shaping, CPS, snap-look) or draws only on the player's own screen (armour HUD, hit
 *       timing, hitboxes). They work on every build the launcher runs on, they fail closed, and
 *       they cannot make a miss land. These are the ones that may be advertised.</li>
 *   <li>The <b>PvP catalogue</b> — 200+ entries describing mostly <em>server-arbitrated</em>
 *       outcomes (reach, knockback, velocity, TPS) or automation that a server restricts. They are
 *       listed for discoverability with their reason shown and their toggle refused. They are
 *       <b>not</b> a feature list.</li>
 * </ul>
 *
 * <p>The decision is therefore <b>reposition, not split and not remove</b>. Splitting would create a
 * second section for the same idea; removing would throw away the honest documentation of what the
 * client cannot do. The section stays, and the boundary is what the section claims.
 *
 * <p>Pure and Android-free so the boundary is unit-tested rather than a paragraph in a doc that
 * drifts: {@link #advertisableIds()} is exactly the set the release notes and store copy may name,
 * and {@link #mayBeAdvertisedAsWorking(String)} refuses a catalogue id.
 */
public final class PvpSectionPolicy {

    /**
     * The shipped PvP modules, and the boundary for each: what the player may expect from it.
     *
     * <p>Every entry is client-side. A module whose outcome the server decides is deliberately
     * absent — it belongs to the catalogue, where it is listed with its reason and no toggle.
     */
    public enum ShippedModule {
        AIM_SETTINGS("aim_settings",
                "Shapes the look input your device sends. Whether a hit lands is the server's call."),
        CPS_DISPLAY("cps_display",
                "Counts your own clicks. It reports, it does not click."),
        SNAPLOOK("snaplook",
                "Turns your view while you hold the key. It only moves the view you are already "
                        + "turning \u2014 it does not aim at a target for you."),
        ARMOR_HUD("armor_hud",
                "Draws durability you already know, on your own screen. It reads nothing from the server."),
        CRYSTAL_OPTIMIZER("crystal_optimizer",
                "Highlights a placement spot. Manual assist never acts; automatic placement is an "
                        + "explicit opt-in and is what servers restrict."),
        HIT_REGISTRATION("hit_registration",
                "Shapes the aim your device sends. It cannot make a miss hit."),
        HIT_TIMING("hit_timing",
                "Tells you when your next click will count. It never clicks for you."),
        HITBOX("hitbox",
                "Draws boxes for launcher users whose position arrived over the voice link, on your "
                        + "own screen. Mobs and items need an entity feed this build does not have.");

        private final String id;
        private final String boundary;

        ShippedModule(String id, String boundary) {
            this.id = id;
            this.boundary = boundary;
        }

        /** The module id. */
        public String id() {
            return id;
        }

        /** One sentence stating exactly what the module does and does not do. */
        public String boundary() {
            return boundary;
        }
    }

    private PvpSectionPolicy() {
    }

    /** Every shipped PvP module, in the order the section lists them. */
    public static List<ShippedModule> shippedModules() {
        return Collections.unmodifiableList(java.util.Arrays.asList(ShippedModule.values()));
    }

    /**
     * The ids the release notes and store copy may name as working PvP features.
     *
     * <p>This is the whole advertisable surface. Anything not in it is either not shipped or is a
     * server-arbitrated outcome, and naming it as a feature would be a claim the code cannot back.
     */
    public static List<String> advertisableIds() {
        List<String> ids = new ArrayList<>();
        for (ShippedModule module : ShippedModule.values()) {
            ids.add(module.id());
        }
        return Collections.unmodifiableList(ids);
    }

    /** True when this id may be advertised as a working PvP feature. */
    public static boolean mayBeAdvertisedAsWorking(String moduleId) {
        if (moduleId == null) return false;
        for (ShippedModule module : ShippedModule.values()) {
            if (module.id().equals(moduleId)) return true;
        }
        return false;
    }

    /** The boundary sentence for a shipped module, or null when it is not a shipped one. */
    public static String boundaryFor(String moduleId) {
        if (moduleId == null) return null;
        for (ShippedModule module : ShippedModule.values()) {
            if (module.id().equals(moduleId)) return module.boundary();
        }
        return null;
    }

    /**
     * True when a catalogue entry describes an outcome the server decides, so it can never be a
     * shipped feature and must not be presented as one.
     */
    public static boolean isServerArbitrated(String catalogueId) {
        return PvpModuleScopes.scopeOf(catalogueId) == ModuleScope.SERVER_DECIDED;
    }

    /**
     * The section's one-line positioning statement, for the menu and the docs.
     *
     * <p>It names what the section is and what it refuses to claim, which is the whole decision.
     */
    public static String positioning() {
        return "Client-side combat helpers only: everything here shapes your own input or draws on "
                + "your own screen. Nothing here makes a miss land, changes your hitbox on the "
                + "server, or alters server physics \u2014 those are the server's decision.";
    }
}
