package org.chimeramc.client.core.mods.inbuilt.catalog;

import org.chimeramc.client.core.mods.pvp.PvpModuleCatalog;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The scope of every catalogue module that this build does not already ship as its own id.
 *
 * <p>The catalogue is the single source of the module list; this class is the single source of what
 * each non-linked entry needs. It exists because the previous rule — "a catalogue entry is either a
 * link to a shipped module or an unimplemented roadmap item" — threw away the whole reachable half
 * of the list. Modules that work purely from device input, the local-player feed or the game
 * options file were badged and refused exactly like the ones that genuinely need a native entity
 * feed, so the menu said "no game data" for a clock.
 *
 * <p>Every entry here is reachable ({@link ModuleScope#isReachable()}) and therefore gets a real
 * toggle backed by a real overlay, writer or action. The entries <em>not</em> in this map are the
 * ones this build cannot drive; {@link #scopeOf} classifies them as {@link ModuleScope#ENGINE_HOOK}
 * or {@link ModuleScope#SERVER_DECIDED} from the catalogue's own scope note, so a module can never
 * be silently reachable without an implementation behind it.
 *
 * <p>Pure and Android-free so the classification is unit-tested and the menu, the badge and the
 * tests all read the same table.
 */
public final class PvpModuleScopes {

    private PvpModuleScopes() {
    }

    private static final Map<String, ModuleScope> REACHABLE = buildReachable();

    private static Map<String, ModuleScope> buildReachable() {
        Map<String, ModuleScope> map = new HashMap<>();

        // --- Device input / launcher host only -------------------------------------------------
        // These read nothing from the game. Each is backed by an overlay drawn from the launcher's
        // own state or an input the launcher injects, so it works on every build.
        put(map, ModuleScope.DEVICE_INPUT,
                // readouts drawn from the launcher's own state
                "hud_clock",              // the device clock
                "hud_keystrokes",         // the device's own key state
                "hud_memory",             // this process's memory
                "hud_watermark",          // a static client watermark
                "hud_combo_display",      // the client's own landed-attack counter
                "combat_combo_counter",   // the client's own landed-attack counter
                "combat_death_counter",   // the local health feed
                "combat_kdr_tracker",     // the client's own counters
                "combat_streak_tracker",  // the client's own credited kills
                "combat_kill_counter",    // the client's own credited kills
                // actions driven purely by injected input
                "util_drop_all",          // the drop key, repeated
                "util_chat_macro",        // types a saved chat line
                "util_command_bind",      // types a saved command
                "util_auto_gg",           // types "gg"
                "util_auto_tip",          // types "/tip"
                // input-driven cooldown readouts: the timer starts from the player's own press
                "combat_attack_cooldown",  // armed on a swing
                "combat_attack_indicator", // armed on a swing
                "combat_crit_timer",       // armed on a swing
                "combat_pearl_timer",      // armed on a pearl use
                "combat_gapple_timer",     // armed on an item use
                "combat_shield_timer",     // armed on a shield use
                "combat_hit_marker",       // flashes on the client's own landed hit
                "combat_no_sprint_reset",  // re-sends sprint after the client's own attack
                "util_keep_sprint",        // re-sends sprint after the client's own action
                "move_sprint_swing");      // re-sends sprint after the client's own swing

        // --- Game options file -----------------------------------------------------------------
        // Each writes a key in the instance's minecraftpe/options.txt, which the game applies on
        // its next load. A settings writer, not a live hook, and the scope note says so.
        put(map, ModuleScope.GAME_OPTIONS,
                "move_bob_off",            // bobView:0
                "util_fullbright",         // gamma:1.0
                "world_fullbright",        // gamma:1.0
                "world_particle_off",      // particles:0
                "move_no_sprint_particle", // particles:2 (minimal)
                "move_sprint_particles",   // particles:2 (minimal)
                "move_fov_boost");         // fov, raised while sprinting

        // --- Peer feed (other players from proximity voice) -------------------------------------
        // Reaches other players through the proximity-voice peer feed, which needs no game hook.
        // Vanilla players are invisible to it and the readout says so.
        put(map, ModuleScope.PEER_FEED,
                "combat_reach_circle");    // distance to the nearest peer vs the reach radius

        // --- Local-player feed (position / rotation / health) -----------------------------------
        // Read through the already-installed native local-player feed (the ClientInstance
        // local-player slot). Degrades to an honest empty state when the feed is absent.
        put(map, ModuleScope.LOCAL_FEED,
                "hud_coordinates",
                "hud_direction",
                "hud_compass",
                "hud_chunk_borders",
                "hud_speedometer",
                "hud_health_display",
                "move_position_display",
                "move_speed_display",
                "move_velocity_display",
                "move_fall_damage_warn",
                "world_waypoints",
                "world_death_waypoint");

        return Collections.unmodifiableMap(map);
    }

    private static void put(Map<String, ModuleScope> map, ModuleScope scope, String... ids) {
        for (String id : ids) {
            map.put(id, scope);
        }
    }

    /**
     * The scope of a catalogue id.
     *
     * <p>A reachable module returns its route. Anything else is classified from the catalogue's own
     * scope note: a note that names the server (or a ban risk) is {@link ModuleScope#SERVER_DECIDED},
     * and everything else is {@link ModuleScope#ENGINE_HOOK}. That keeps the two documented groups
     * apart without a second hand-maintained list, and means a roadmap entry added to the catalogue
     * is classified correctly the moment it is added.
     */
    public static ModuleScope scopeOf(String moduleId) {
        ModuleScope reachable = REACHABLE.get(moduleId);
        if (reachable != null) return reachable;

        PvpModuleCatalog.Module module = PvpModuleCatalog.find(moduleId);
        if (module == null) return ModuleScope.ENGINE_HOOK;
        String note = module.scopeNote == null ? "" : module.scopeNote.toLowerCase(Locale.US);
        if (note.contains("server") || note.contains("ban")
                || note.contains("restrict") || note.contains("automating")) {
            return ModuleScope.SERVER_DECIDED;
        }
        return ModuleScope.ENGINE_HOOK;
    }

    /** True when this build can drive the module through a route it already owns. */
    public static boolean isReachable(String moduleId) {
        return scopeOf(moduleId).isReachable();
    }

    /** Every reachable catalogue id. */
    public static Set<String> reachableIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(REACHABLE.keySet()));
    }
}
