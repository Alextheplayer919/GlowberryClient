package org.chimeramc.client.core.mods.pvp;

/**
 * The categories a large PvP module set is organised into, and the rule that maps a module to one.
 *
 * <p>The set is large enough (200+) that a flat list is unusable, so every module belongs to exactly
 * one category. The mapping is a pure function of the module's id prefix, which is what keeps the
 * category a strict partition: a module cannot appear in two categories, and every module appears in
 * one. {@link #isExhaustiveAndDisjoint} and the unit test pin that invariant.
 *
 * <p><b>Server-side honesty.</b> Most of these modules describe a <em>client-side</em> helper for a
 * server-side outcome (hit registration, reach, velocity). A client cannot make a miss land, change
 * its own hitbox on the server, or alter server physics — those are decided by the server. Each
 * module carries a scope note saying so; the category is only navigation.
 */
public enum PvpCategory {
    /** Combat &amp; targeting: hit registration, reach, rotation, swing timing. */
    COMBAT("combat", "Combat & Targeting"),
    /** Movement &amp; physics: friction, jump height, step assist, fluid movement. */
    MOVEMENT("movement", "Movement & Physics"),
    /** Visual overlays &amp; HUD: keystrokes, armour, ping, CPS, coordinates, crosshair. */
    VISUAL("visual", "Visual Overlays & HUD"),
    /** Utility &amp; automation: fast place, tool switch, sorting, macros, disconnect. */
    UTILITY("utility", "Utility & Automation"),
    /** World &amp; environment: brightness, weather, tracers, ESP helpers. */
    WORLD("world", "World & Environment");

    private final String id;
    private final String title;

    PvpCategory(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    /**
     * The category for a module id, from its prefix. Unknown prefixes fall to {@link #UTILITY}, so a
     * module is never left uncategorised.
     */
    public static PvpCategory forModuleId(String moduleId) {
        if (moduleId == null || moduleId.isEmpty()) return UTILITY;
        if (moduleId.startsWith("combat_") || moduleId.startsWith("hit_")
                || moduleId.startsWith("reach_") || moduleId.startsWith("rotation_")
                || moduleId.startsWith("swing_") || moduleId.startsWith("target_")) {
            return COMBAT;
        }
        if (moduleId.startsWith("move_") || moduleId.startsWith("jump_")
                || moduleId.startsWith("step_") || moduleId.startsWith("fluid_")
                || moduleId.startsWith("strafe_")) {
            return MOVEMENT;
        }
        if (moduleId.startsWith("hud_") || moduleId.startsWith("vis_")
                || moduleId.startsWith("overlay_")) {
            return VISUAL;
        }
        if (moduleId.startsWith("world_") || moduleId.startsWith("env_")) {
            return WORLD;
        }
        return UTILITY;
    }

    /** True when the categories partition the given module ids: every id maps to exactly one. */
    public static boolean isExhaustiveAndDisjoint(java.util.List<String> moduleIds) {
        if (moduleIds == null) return true;
        for (String id : moduleIds) {
            int matches = 0;
            for (PvpCategory category : values()) {
                if (category == forModuleId(id)) matches++;
            }
            if (matches != 1) return false;
        }
        return true;
    }
}
