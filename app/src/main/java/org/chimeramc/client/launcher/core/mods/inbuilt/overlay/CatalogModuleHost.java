package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.app.Activity;

import org.chimeramc.client.core.mods.inbuilt.catalog.PvpModuleScopes;
import org.chimeramc.client.core.mods.inbuilt.manager.InbuiltModManager;

/**
 * Routes a reachable catalogue module's enable/disable to the right implementation.
 *
 * <p>The overlay manager already knows how to show and hide the shipped modules. A catalogue module
 * is one of four shapes, so instead of a switch with 40 branches inside the manager this class
 * decides the shape once and calls the matching helper:
 *
 * <ul>
 *   <li>a <b>readout</b> → {@link CatalogModuleRuntime#showReadout}</li>
 *   <li>an <b>action</b> → performed immediately on enable, nothing to show</li>
 *   <li>an <b>options write</b> → written to the instance's options file</li>
 *   <li>an <b>input-shaping</b> module → owned by the controller/look path, nothing to show here</li>
 * </ul>
 *
 * <p>Keeping the decision here means the manager's own switches stay about the shipped modules, and
 * a new catalogue module is registered in one place. The classification itself is
 * {@link PvpModuleScopes}, so "is this a catalogue module" and "what does it need" cannot disagree.
 */
public final class CatalogModuleHost {

    private CatalogModuleHost() {
    }

    /** The catalogue ids that draw a text readout. */
    private static final java.util.Set<String> READOUTS = new java.util.HashSet<>(java.util.Arrays.asList(
            "hud_clock", "hud_keystrokes", "hud_memory", "hud_watermark",
            "hud_combo_display", "combat_combo_counter",
            "combat_death_counter", "combat_kdr_tracker", "combat_streak_tracker",
            "combat_kill_counter",
            "hud_coordinates", "hud_direction", "hud_compass", "hud_chunk_borders",
            "hud_speedometer", "hud_health_display",
            "move_position_display", "move_speed_display", "move_velocity_display",
            "move_fall_damage_warn", "world_waypoints", "world_death_waypoint",
            "combat_attack_cooldown", "combat_attack_indicator", "combat_crit_timer",
            "combat_pearl_timer", "combat_gapple_timer", "combat_shield_timer",
            "combat_hit_marker", "combat_reach_circle"));

    /** The catalogue ids that perform a one-shot when switched on. */
    private static final java.util.Set<String> ACTIONS = new java.util.HashSet<>(java.util.Arrays.asList(
            "util_drop_all", "util_chat_macro", "util_command_bind",
            "util_auto_gg", "util_auto_tip"));

    /** The catalogue ids that write the instance's options file. */
    private static final java.util.Set<String> OPTIONS_WRITES = new java.util.HashSet<>(java.util.Arrays.asList(
            "move_bob_off", "util_fullbright", "world_fullbright", "world_particle_off",
            "move_no_sprint_particle", "move_sprint_particles", "move_fov_boost"));

    /**
     * The catalogue ids that re-send the sprint key after the client's own attack.
     *
     * <p>This is the same input a player would send; it does not change the game's physics, which
     * the server arbitrates, so it is honest as a device-input helper.
     */
    private static final java.util.Set<String> SPRINT_HELPERS = new java.util.HashSet<>(java.util.Arrays.asList(
            "combat_no_sprint_reset", "move_sprint_swing", "util_keep_sprint"));

    /** True when this id is a reachable catalogue module the host can drive. */
    static boolean handles(String modId) {
        return PvpModuleScopes.isReachable(modId)
                && (READOUTS.contains(modId) || ACTIONS.contains(modId)
                || OPTIONS_WRITES.contains(modId) || SPRINT_HELPERS.contains(modId));
    }

    /** True when the module draws a draggable text readout, so the dialog offers its appearance. */
    public static boolean isReadout(String modId) {
        return READOUTS.contains(modId);
    }

    /** Every id that should be re-shown when a session starts (readouts and options writes). */
    static java.util.Set<String> restorableIds() {
        java.util.Set<String> ids = new java.util.LinkedHashSet<>(READOUTS);
        ids.addAll(OPTIONS_WRITES);
        return ids;
    }

    /**
     * Re-applies the catalogue modules a session had enabled.
     *
     * <p>Only readouts and options writes are restored. A one-shot action is deliberately skipped:
     * re-running "send gg" or "drop all" on every session start would fire it at a moment the
     * player did not ask for it. A readout is idempotent (it just shows its window) and an options
     * write is idempotent (it writes the same value), so both are safe to restore.
     */
    static void restoreEnabled(Activity activity, InbuiltOverlayManager overlayManager,
                               InbuiltModManager manager) {
        for (String modId : restorableIds()) {
            if (manager.resolveInbuiltModEnabled(modId, false)) {
                overlayManager.handleModToggle(modId, true);
            }
        }
    }

    /** Shows or performs the module. */
    static void onEnable(Activity activity, InbuiltOverlayManager overlayManager,
                         InbuiltModManager manager, String modId, int x, int y) {
        if (READOUTS.contains(modId)) {
            overlayManager.catalogRuntime().showReadout(modId, x, y);
            // A waypoint module marks where the player is standing as it turns on, so the readout
            // has something to measure from immediately. Failing to read the feed leaves it unset,
            // which the readout says rather than showing a distance from nowhere.
            if ("world_waypoints".equals(modId) || "world_death_waypoint".equals(modId)) {
                overlayManager.catalogRuntime().setWaypointHere();
            }
            return;
        }
        if (ACTIONS.contains(modId)) {
            overlayManager.catalogRuntime().performAction(modId);
            return;
        }
        if (SPRINT_HELPERS.contains(modId)) {
            overlayManager.catalogRuntime().setSprintHelper(modId, true);
            return;
        }
        if (OPTIONS_WRITES.contains(modId)) {
            applyOptionsWrite(activity, modId);
        }
    }

    /** Hides or undoes the module. */
    static void onDisable(InbuiltOverlayManager overlayManager, String modId) {
        if (READOUTS.contains(modId)) {
            overlayManager.catalogRuntime().hideReadout(modId);
        }
        if (SPRINT_HELPERS.contains(modId)) {
            overlayManager.catalogRuntime().setSprintHelper(modId, false);
        }
        // An action or an options write has nothing to tear down: the action already ran, and the
        // options file is re-read by the game on its next load.
    }

    private static void applyOptionsWrite(Activity activity, String modId) {
        switch (modId) {
            case "move_bob_off":
                CatalogOptionsWriter.apply(activity, "bobView", "0");
                break;
            case "util_fullbright":
            case "world_fullbright":
                CatalogOptionsWriter.apply(activity, "gamma", "1.0");
                break;
            case "world_particle_off":
                CatalogOptionsWriter.apply(activity, "particles", "0");
                break;
            case "move_no_sprint_particle":
            case "move_sprint_particles":
                // Bedrock's particles option: 0 all, 1 decreased, 2 minimal.
                CatalogOptionsWriter.apply(activity, "particles", "2");
                break;
            case "move_fov_boost":
                // The instance's base FOV; the game applies it on the next load. This is a
                // settings write, not a live FOV hook, and the scope note says so.
                CatalogOptionsWriter.apply(activity, "fov", "80");
                break;
            default:
                break;
        }
    }
}
