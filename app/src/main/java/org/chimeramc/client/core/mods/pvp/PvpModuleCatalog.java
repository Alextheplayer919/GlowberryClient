package org.chimeramc.client.core.mods.pvp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The large PvP module catalogue: 200+ entries across the five {@link PvpCategory categories}.
 *
 * <p>This is a catalogue, not an implementation. Each entry declares what the module is <em>for</em>
 * and, crucially, its honest scope: most of these describe a client-side helper for a server-side
 * outcome, and the client cannot change what the server decides. The catalogue is the single source
 * the list, the category counts and the tests read, so a module cannot be added to one category and
 * counted in another.
 *
 * <p><b>Why a catalogue and not 200 switches wired to nothing.</b> A module that is listed but does
 * nothing is the defect this project keeps fixing (see the "module availability: badge, do not
 * silently no-op" note). So each entry carries a {@link Module#implemented} flag: the ones this build
 * can actually drive are marked implemented and get a real toggle; the rest are listed as a roadmap
 * with their scope note, and the UI can badge them rather than pretending they work. The catalogue is
 * honest about which is which, and {@link #implemented()} is the query the UI uses.
 */
public final class PvpModuleCatalog {

    /** One PvP module entry. */
    public static final class Module {
        public final String id;
        public final String name;
        public final String description;
        public final PvpCategory category;
        /**
         * True when this build can actually drive the module through an existing route. A false
         * entry is listed (so the roadmap is visible and categorised) but must not be presented as
         * working — the UI badges it rather than toggling to no effect.
         */
        public final boolean implemented;
        /** What the module can and cannot do, so a named-after-an-outcome module is not a lie. */
        public final String scopeNote;
        /**
         * The {@code ModIds} id of the module that already implements this capability, or null when
         * this entry is a roadmap item.
         *
         * <p>Most of the "implemented" entries are the PvP catalogue's own naming of a module the
         * launcher already ships (Hit Registration, Zoom, Auto Sprint, …). Rather than list a second
         * copy of each — which would give the player two switches for one feature and let them
         * disagree — an entry with a {@link #existingModId} is a <em>view</em> of that module: its
         * toggle drives the real one, and its enabled state is read from the real one.
         */
        public final String existingModId;

        Module(String id, String name, String description, PvpCategory category,
               boolean implemented, String scopeNote, String existingModId) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.category = category;
            this.implemented = implemented;
            this.scopeNote = scopeNote;
            this.existingModId = existingModId;
        }

        /** True when this entry drives a module the launcher already ships. */
        public boolean linksToExisting() {
            return existingModId != null && !existingModId.isEmpty();
        }
    }

    private static final List<Module> MODULES;

    /** The scope note for a purely client-side helper of a server-decided outcome. */
    private static final String CLIENT_HELPER =
            "Client-side helper only: the server decides the outcome, so this cannot guarantee it.";
    /** The scope note for an overlay that draws on your own screen only. */
    private static final String LOCAL_OVERLAY =
            "Draws on your own screen only; it does not change the game or what others see.";

    static {
        List<Module> modules = new ArrayList<>();

        // --- Combat & Targeting (45) --------------------------------------------------------------
        // The seven existing combat modules are implemented; the rest are the requested roadmap.
        link(modules, "combat_hit_registration", "Hit Registration",
                "Shapes the look input the device sends so aim tracks smoothly.", PvpCategory.COMBAT,
                "hit_registration", CLIENT_HELPER);
        link(modules, "combat_reach_indicator", "Reach Indicator",
                "Shows the distance to the player under the crosshair.", PvpCategory.COMBAT,
                "reach_indicator", "Reads the peer feed; vanilla players are invisible to it.");
        link(modules, "combat_crystal_optimizer", "Crystal Optimizer",
                "Highlights a safe crystal placement spot.", PvpCategory.COMBAT,
                "crystal_optimizer", "Manual assist by default; automatic placement is opt-in and server-restricted.");
        link(modules, "combat_hit_timing", "Select Hit",
                "A green/red pill for the post-hit window and combo count.", PvpCategory.COMBAT,
                "hit_timing", "Reads your own attack input; the server still decides whether a hit lands.");
        link(modules, "combat_hitboxes", "Hitboxes",
                "Draws entity bounding boxes and combat guides.", PvpCategory.COMBAT,
                "hitbox", "Peer boxes need the voice feed; mob/item boxes need a native entity feed.");
        link(modules, "combat_aim_settings", "Aim Settings",
                "Crosshair, smoothing and sensitivity for the look input.", PvpCategory.COMBAT,
                "aim_settings", CLIENT_HELPER);
        link(modules, "combat_snaplook", "Snaplook",
                "Snaps the view to a saved angle while a key is held.", PvpCategory.COMBAT,
                "snaplook", CLIENT_HELPER);

        add(modules, "combat_velocity_modifier", "Velocity Modifier",
                "Adjusts how incoming knockback is applied on your client.", PvpCategory.COMBAT,
                false, CLIENT_HELPER);
        add(modules, "combat_attack_range_sync", "Attack Range Synchronizer",
                "Aligns the client's attack range with the server's reported reach.", PvpCategory.COMBAT,
                false, CLIENT_HELPER);
        add(modules, "combat_auto_block", "Auto Block",
                "Blocks when a hit is predicted, within server rules.", PvpCategory.COMBAT,
                false, "A client that blocks for the player is what servers restrict or ban.");
        add(modules, "combat_rotation_smoother", "Rotation Smoother",
                "Eases the camera rotation to reduce jitter in the sent look.", PvpCategory.COMBAT,
                false, CLIENT_HELPER);
        add(modules, "combat_swing_adjuster", "Packet-Timed Swing Adjuster",
                "Times the swing to the server's tick window.", PvpCategory.COMBAT,
                false, CLIENT_HELPER);
        add(modules, "combat_w_tap", "W-Tap Assist",
                "Resets sprint on hit to keep knockback consistent.", PvpCategory.COMBAT,
                false, "Sends the same input a player would; the server still arbitrates.");
        add(modules, "combat_s_tap", "S-Tap Assist",
                "Briefly backs off after a hit to reduce knockback.", PvpCategory.COMBAT,
                false, CLIENT_HELPER);
        add(modules, "combat_crit_timer", "Crit Timer",
                "Shows the window in which a falling attack crits.", PvpCategory.COMBAT,
                false, LOCAL_OVERLAY);
        add(modules, "combat_shield_timer", "Shield Timer",
                "Shows the shield cooldown after a disable.", PvpCategory.COMBAT,
                false, LOCAL_OVERLAY);
        add(modules, "combat_attack_cooldown", "Attack Cooldown",
                "Shows the vanilla attack-cooldown meter.", PvpCategory.COMBAT,
                false, LOCAL_OVERLAY);
        add(modules, "combat_target_hud", "Target HUD",
                "Shows the current target's health and armour.", PvpCategory.COMBAT,
                false, "Needs the entity feed for the target's health.");
        add(modules, "combat_damage_tint", "Damage Tint",
                "Flashes the screen when you take damage.", PvpCategory.COMBAT,
                false, LOCAL_OVERLAY);
        add(modules, "combat_hit_marker", "Hit Marker",
                "Draws a marker when a hit is registered locally.", PvpCategory.COMBAT,
                false, LOCAL_OVERLAY);
        add(modules, "combat_kill_counter", "Kill Counter",
                "Counts credited kills this session.", PvpCategory.COMBAT,
                false, "Credit is inferred from a hit then a death; never a server-confirmed kill.");
        add(modules, "combat_death_counter", "Death Counter",
                "Counts your deaths this session.", PvpCategory.COMBAT,
                false, CLIENT_HELPER);
        add(modules, "combat_kdr_tracker", "K/D Tracker",
                "Tracks the session kill/death ratio.", PvpCategory.COMBAT,
                false, CLIENT_HELPER);
        add(modules, "combat_streak_tracker", "Killstreak Tracker",
                "Counts consecutive credited kills.", PvpCategory.COMBAT,
                false, CLIENT_HELPER);
        add(modules, "combat_opponent_armor", "Opponent Armor HUD",
                "Shows the target's armour durability.", PvpCategory.COMBAT,
                false, "Needs the native entity feed; shows nothing without it.");
        add(modules, "combat_opponent_held", "Opponent Held Item",
                "Shows the item the target is holding.", PvpCategory.COMBAT,
                false, "Needs the native entity feed.");
        add(modules, "combat_reach_circle", "Reach Circle",
                "Draws a ground circle at your attack reach.", PvpCategory.COMBAT,
                false, LOCAL_OVERLAY);
        add(modules, "combat_attack_indicator", "Attack Indicator",
                "Shows whether the next click will register.", PvpCategory.COMBAT,
                false, CLIENT_HELPER);
        add(modules, "combat_combo_counter", "Combo Counter",
                "Counts your landed-in-window attacks.", PvpCategory.COMBAT,
                false, "Counts your own attacks, not the game's server-side combo value.");
        link(modules, "combat_cps_limit", "CPS Limit",
                "Caps clicks per second per button to protect the hardware.", PvpCategory.COMBAT,
                "cps_display", "Drops presses above the cap; the server sees the reduced rate.");
        add(modules, "combat_click_delay", "Click Delay Randomiser",
                "Adds a small random delay between clicks.", PvpCategory.COMBAT,
                false, "A client that adds timing noise is what servers restrict or ban.");
        add(modules, "combat_swing_through", "Swing Through",
                "Keeps swinging while the attack key is held.", PvpCategory.COMBAT,
                false, "An autoclicker-like repeat; may be against server rules.");
        add(modules, "combat_aim_assist", "Aim Assist",
                "Nudges the look toward a nearby target.", PvpCategory.COMBAT,
                false, "A client that moves your aim for you is what servers restrict or ban.");
        add(modules, "combat_reach_extend", "Reach Extend",
                "Extends the client's attack reach.", PvpCategory.COMBAT,
                false, "The server validates reach, so this cannot work and is not shipped.");
        add(modules, "combat_anti_kb", "Anti-Knockback",
                "Reduces the knockback the client applies.", PvpCategory.COMBAT,
                false, "Knockback is applied by the server; a client cannot remove it.");
        add(modules, "combat_backtrack", "Backtrack",
                "Delays displaying an incoming hit.", PvpCategory.COMBAT,
                false, CLIENT_HELPER);
        add(modules, "combat_lag_range", "Lag Range",
                "Compensates reach for latency.", PvpCategory.COMBAT,
                false, "The server arbitrates reach; this cannot change what it accepts.");
        add(modules, "combat_no_hurt_cam", "No Hurt Camera",
                "Removes the camera tilt on damage.", PvpCategory.COMBAT,
                false, LOCAL_OVERLAY);
        add(modules, "combat_no_sprint_reset", "No Sprint Reset",
                "Keeps sprint while attacking.", PvpCategory.COMBAT,
                false, CLIENT_HELPER);
        add(modules, "combat_crit_every_hit", "Crit Every Hit",
                "Attempts a crit on every attack.", PvpCategory.COMBAT,
                false, "Crits are a server-side rule; a client cannot force one.");
        add(modules, "combat_weapon_switch", "Auto Weapon Switch",
                "Switches to the best weapon for the target.", PvpCategory.COMBAT,
                false, "Automating inventory actions is what servers restrict or ban.");
        add(modules, "combat_potion_timer", "Potion Timer",
                "Shows remaining potion effect time.", PvpCategory.COMBAT,
                false, "Needs the effect feed from the game.");
        add(modules, "combat_pearl_timer", "Pearl Cooldown",
                "Shows the ender-pearl cooldown.", PvpCategory.COMBAT,
                false, LOCAL_OVERLAY);
        add(modules, "combat_gapple_timer", "Gapple Timer",
                "Shows the golden-apple effect window.", PvpCategory.COMBAT,
                false, LOCAL_OVERLAY);
        add(modules, "combat_totem_alert", "Totem Alert",
                "Warns when a totem pops.", PvpCategory.COMBAT,
                false, "Needs the effect feed from the game.");

        // --- Movement & Physics (40) --------------------------------------------------------------
        link(modules, "move_auto_sprint", "Auto Sprint",
                "Keeps sprint held without holding the key.", PvpCategory.MOVEMENT,
                "auto_sprint", "Sends the same sprint input a player would.");
        add(modules, "move_friction_handler", "Custom Friction Handler",
                "Shapes the client's movement friction.", PvpCategory.MOVEMENT,
                false, CLIENT_HELPER);
        add(modules, "move_jump_stability", "Jump-Height Stability",
                "Keeps jump height consistent across latency.", PvpCategory.MOVEMENT,
                false, "Jump physics are server-side; a client cannot change them.");
        add(modules, "move_step_assist", "Step Assist",
                "Smooths stepping up a block.", PvpCategory.MOVEMENT,
                false, CLIENT_HELPER);
        add(modules, "move_fluid_bypass", "Fluid Movement Bypass",
                "Reduces the slowdown while in water.", PvpCategory.MOVEMENT,
                false, "Fluid physics are server-side; a client cannot bypass them.");
        add(modules, "move_strafe_maintain", "Strafe Velocity Maintainer",
                "Keeps strafe speed consistent while turning.", PvpCategory.MOVEMENT,
                false, CLIENT_HELPER);
        add(modules, "move_bhop", "Bunny Hop",
                "Keeps momentum on landing.", PvpCategory.MOVEMENT,
                false, "Movement physics are server-side; a client cannot change them.");
        add(modules, "move_speed_bridge", "Speed Bridge",
                "Assists a consistent bridge walk.", PvpCategory.MOVEMENT,
                false, CLIENT_HELPER);
        add(modules, "move_ninja_bridge", "Ninja Bridge",
                "Alternates sneak to place blocks fast.", PvpCategory.MOVEMENT,
                false, "Automating placement is what servers restrict or ban.");
        add(modules, "move_safe_walk", "Safe Walk",
                "Stops you walking off an edge.", PvpCategory.MOVEMENT,
                false, "Automating movement is what servers restrict or ban.");
        add(modules, "move_no_slow", "No Slowdown",
                "Removes the item-use slowdown.", PvpCategory.MOVEMENT,
                false, CLIENT_HELPER);
        add(modules, "move_no_web_slow", "No Web Slowdown",
                "Removes the cobweb slowdown.", PvpCategory.MOVEMENT,
                false, CLIENT_HELPER);
        add(modules, "move_no_soul_sand", "No Soul-Sand Slow",
                "Removes the soul-sand slowdown.", PvpCategory.MOVEMENT,
                false, CLIENT_HELPER);
        add(modules, "move_air_strafe", "Air Strafe",
                "Improves air control on the client.", PvpCategory.MOVEMENT,
                false, CLIENT_HELPER);
        add(modules, "move_elytra_assist", "Elytra Assist",
                "Helps hold a steady elytra course.", PvpCategory.MOVEMENT,
                false, CLIENT_HELPER);
        add(modules, "move_boat_assist", "Boat Assist",
                "Steadies boat steering.", PvpCategory.MOVEMENT,
                false, CLIENT_HELPER);
        add(modules, "move_horse_assist", "Horse Assist",
                "Steadies horse steering.", PvpCategory.MOVEMENT,
                false, CLIENT_HELPER);
        add(modules, "move_sprint_particles", "Sprint Particles Off",
                "Hides your own sprint particles.", PvpCategory.MOVEMENT,
                false, LOCAL_OVERLAY);
        add(modules, "move_speed_display", "Speed Display",
                "Shows your current speed in blocks/s.", PvpCategory.MOVEMENT,
                false, LOCAL_OVERLAY);
        add(modules, "move_position_display", "Position Display",
                "Shows your coordinates.", PvpCategory.MOVEMENT,
                false, LOCAL_OVERLAY);
        add(modules, "move_fall_damage_warn", "Fall Damage Warning",
                "Warns before a fall would hurt.", PvpCategory.MOVEMENT,
                false, CLIENT_HELPER);
        add(modules, "move_scaffold", "Scaffold",
                "Places blocks beneath you while moving.", PvpCategory.MOVEMENT,
                false, "Automating placement is what servers restrict or ban.");
        add(modules, "move_timer", "Timer",
                "Runs client ticks faster than the server.", PvpCategory.MOVEMENT,
                false, "The server enforces its own tick rate; a client cannot exceed it.");
        add(modules, "move_no_fall", "No Fall Damage",
                "Attempts to negate fall damage.", PvpCategory.MOVEMENT,
                false, "Fall damage is server-side; a client cannot negate it.");
        add(modules, "move_ice_speed", "Ice Speed Control",
                "Keeps control on ice.", PvpCategory.MOVEMENT,
                false, CLIENT_HELPER);
        add(modules, "move_ladder_assist", "Ladder Assist",
                "Speeds ladder climbing.", PvpCategory.MOVEMENT,
                false, CLIENT_HELPER);
        add(modules, "move_swim_assist", "Swim Assist",
                "Steadies swimming.", PvpCategory.MOVEMENT,
                false, CLIENT_HELPER);
        add(modules, "move_crouch_speed", "Crouch Speed",
                "Adjusts crouch movement speed.", PvpCategory.MOVEMENT,
                false, CLIENT_HELPER);
        add(modules, "move_jump_cooldown", "Jump Cooldown",
                "Limits repeated jumps.", PvpCategory.MOVEMENT,
                false, CLIENT_HELPER);
        add(modules, "move_auto_jump", "Auto Jump",
                "Jumps when a block is in the way.", PvpCategory.MOVEMENT,
                false, "Automating movement is what servers restrict or ban.");
        add(modules, "move_dolphin_assist", "Dolphin Assist",
                "Times dolphin's-grace boosts.", PvpCategory.MOVEMENT,
                false, CLIENT_HELPER);
        add(modules, "move_riptide_assist", "Riptide Assist",
                "Steadies riptide travel.", PvpCategory.MOVEMENT,
                false, CLIENT_HELPER);
        add(modules, "move_firework_assist", "Firework Assist",
                "Times elytra fireworks.", PvpCategory.MOVEMENT,
                false, CLIENT_HELPER);
        add(modules, "move_no_sprint_particle", "No Sprint Particle",
                "Suppresses the sprint particle on your client.", PvpCategory.MOVEMENT,
                false, LOCAL_OVERLAY);
        add(modules, "move_bob_off", "View Bobbing Off",
                "Disables the view bob.", PvpCategory.MOVEMENT,
                false, LOCAL_OVERLAY);
        add(modules, "move_fov_boost", "FOV Boost While Sprinting",
                "Raises FOV while sprinting.", PvpCategory.MOVEMENT,
                false, LOCAL_OVERLAY);
        add(modules, "move_sprint_swing", "Sprint While Swinging",
                "Keeps sprint during a swing.", PvpCategory.MOVEMENT,
                false, CLIENT_HELPER);
        add(modules, "move_stair_snap", "Stair Snap",
                "Snaps the camera level on stairs.", PvpCategory.MOVEMENT,
                false, LOCAL_OVERLAY);
        add(modules, "move_ground_lock", "Ground Lock",
                "Keeps the view level while moving.", PvpCategory.MOVEMENT,
                false, LOCAL_OVERLAY);
        add(modules, "move_velocity_display", "Velocity Display",
                "Shows your current velocity vector.", PvpCategory.MOVEMENT,
                false, LOCAL_OVERLAY);

        // --- Visual Overlays & HUD (35) -----------------------------------------------------------
        link(modules, "hud_cps_display", "CPS Counter",
                "Shows clicks per second.", PvpCategory.VISUAL, "cps_display", LOCAL_OVERLAY);
        link(modules, "hud_fps_display", "FPS Display",
                "Shows the launcher's own frame counter.", PvpCategory.VISUAL, "fps_display",
                "The launcher's counter, not a measurement of the game's frame rate.");
        add(modules, "hud_keystrokes", "Keystrokes",
                "Shows WASD and mouse presses.", PvpCategory.VISUAL, false, LOCAL_OVERLAY);
        link(modules, "hud_armor_status", "Armor Status",
                "Shows your armour and its durability.", PvpCategory.VISUAL, "armor_hud",
                "Needs the native armour feed; shows an empty state without it.");
        add(modules, "hud_ping_graph", "Ping Graph",
                "Shows a latency history graph.", PvpCategory.VISUAL, false,
                "The launcher has no ping source in this build.");
        add(modules, "hud_coordinates", "Coordinates",
                "Shows your X/Y/Z position.", PvpCategory.VISUAL, false,
                "Needs the local-player feed; shows nothing without it.");
        link(modules, "hud_crosshair_custom", "Custom Crosshair",
                "A customisable crosshair.", PvpCategory.VISUAL, "crosshair", LOCAL_OVERLAY);
        link(modules, "hud_reach_display", "Reach Display",
                "Shows the distance to the target.", PvpCategory.VISUAL, "reach_indicator",
                "Reads the peer feed; vanilla players are invisible to it.");
        add(modules, "hud_combo_display", "Combo Display",
                "Shows the combo count.", PvpCategory.VISUAL, false,
                "Counts your own attacks, not the server's combo value.");
        add(modules, "hud_potion_status", "Potion Status",
                "Shows active potion effects.", PvpCategory.VISUAL, false,
                "Needs the effect feed from the game.");
        add(modules, "hud_clock", "Clock",
                "Shows the in-game time.", PvpCategory.VISUAL, false, LOCAL_OVERLAY);
        add(modules, "hud_compass", "Compass",
                "Shows your facing direction.", PvpCategory.VISUAL, false, LOCAL_OVERLAY);
        add(modules, "hud_direction", "Direction",
                "Shows the cardinal direction you face.", PvpCategory.VISUAL, false, LOCAL_OVERLAY);
        add(modules, "hud_biome", "Biome",
                "Shows the biome you are in.", PvpCategory.VISUAL, false,
                "Needs a world read the launcher does not have.");
        add(modules, "hud_chunk_borders", "Chunk Borders",
                "Draws chunk boundaries.", PvpCategory.VISUAL, false, LOCAL_OVERLAY);
        add(modules, "hud_light_level", "Light Level",
                "Shows the light level at your position.", PvpCategory.VISUAL, false,
                "Needs a world read the launcher does not have.");
        add(modules, "hud_target_info", "Target Info",
                "Shows the target's name and health.", PvpCategory.VISUAL, false,
                "Needs the native entity feed.");
        add(modules, "hud_watermark", "Watermark",
                "Draws a small client watermark.", PvpCategory.VISUAL, false, LOCAL_OVERLAY);
        link(modules, "hud_playtime", "Playtime",
                "Shows session and total playtime.", PvpCategory.VISUAL, "mod_menu",
                "Reads the launcher's own playtime tracker.");
        add(modules, "hud_memory", "Memory Usage",
                "Shows the launcher's memory use.", PvpCategory.VISUAL, false, LOCAL_OVERLAY);
        add(modules, "hud_packet_count", "Packet Count",
                "Shows a network packet counter.", PvpCategory.VISUAL, false,
                "The launcher has no packet-level source in this build.");
        add(modules, "hud_entity_count", "Entity Count",
                "Shows the number of nearby entities.", PvpCategory.VISUAL, false,
                "Needs the native entity feed.");
        add(modules, "hud_item_counter", "Item Counter",
                "Counts a chosen item in your inventory.", PvpCategory.VISUAL, false,
                "Needs an inventory read the launcher does not have.");
        add(modules, "hud_durability_warn", "Durability Warning",
                "Warns when a tool is nearly broken.", PvpCategory.VISUAL, false,
                "Needs an inventory read the launcher does not have.");
        add(modules, "hud_hunger_display", "Hunger Display",
                "Shows the hunger value.", PvpCategory.VISUAL, false, LOCAL_OVERLAY);
        add(modules, "hud_health_display", "Health Display",
                "Shows the health value.", PvpCategory.VISUAL, false, LOCAL_OVERLAY);
        add(modules, "hud_xp_display", "XP Display",
                "Shows the experience level.", PvpCategory.VISUAL, false, LOCAL_OVERLAY);
        add(modules, "hud_ping_bar", "Ping Bar",
                "Shows a latency bar.", PvpCategory.VISUAL, false,
                "The launcher has no ping source in this build.");
        add(modules, "hud_server_ip", "Server Address",
                "Shows the server you are on.", PvpCategory.VISUAL, false,
                "Needs a session read the launcher does not have.");
        add(modules, "hud_tps", "Server TPS",
                "Shows an estimated server tick rate.", PvpCategory.VISUAL, false,
                "The launcher cannot measure the server's tick rate.");
        link(modules, "hud_reach_meter", "Reach Meter",
                "A compact reach readout.", PvpCategory.VISUAL, "reach_indicator",
                "Reads the peer feed; vanilla players are invisible to it.");
        link(modules, "hud_hitbox_toggle", "Hitbox Toggle Indicator",
                "Shows whether hitboxes are on.", PvpCategory.VISUAL, "hitbox", LOCAL_OVERLAY);
        add(modules, "hud_armor_percent", "Armor Percentage",
                "Shows armour as a percentage.", PvpCategory.VISUAL, false,
                "Needs the native armour feed.");
        add(modules, "hud_saturation", "Saturation",
                "Shows the saturation value.", PvpCategory.VISUAL, false, LOCAL_OVERLAY);
        add(modules, "hud_speedometer", "Speedometer",
                "Shows your speed.", PvpCategory.VISUAL, false, LOCAL_OVERLAY);

        // --- Utility & Automation (50) ------------------------------------------------------------
        link(modules, "util_quick_drop", "Quick Drop",
                "Drops the held item with one tap.", PvpCategory.UTILITY, "quick_drop",
                "Sends the same drop input a player would.");
        add(modules, "util_fast_place", "Fast Place",
                "Places blocks without the use delay.", PvpCategory.UTILITY, false,
                "The server enforces its own placement rate.");
        add(modules, "util_auto_tool", "Auto Tool Switch",
                "Switches to the best tool for the block.", PvpCategory.UTILITY, false,
                "Automating inventory actions is what servers restrict or ban.");
        add(modules, "util_inventory_sort", "Inventory Sorter",
                "Sorts the inventory with one action.", PvpCategory.UTILITY, false,
                "Automating inventory actions is what servers restrict or ban.");
        add(modules, "util_chat_macro", "Chat Macro",
                "Sends a saved chat message with one key.", PvpCategory.UTILITY, false,
                "Sends a message you wrote; the server's chat rules still apply.");
        add(modules, "util_quick_disconnect", "Quick Disconnect",
                "Leaves the server immediately.", PvpCategory.UTILITY, false, CLIENT_HELPER);
        add(modules, "util_auto_reconnect", "Auto Reconnect",
                "Rejoins after a disconnect.", PvpCategory.UTILITY, false, CLIENT_HELPER);
        add(modules, "util_auto_respawn", "Auto Respawn",
                "Respawns after death.", PvpCategory.UTILITY, false, CLIENT_HELPER);
        add(modules, "util_auto_gg", "Auto GG",
                "Sends a message after a match.", PvpCategory.UTILITY, false,
                "Sends a message; the server's chat rules still apply.");
        add(modules, "util_auto_tip", "Auto Tip",
                "Sends a tip on a schedule.", PvpCategory.UTILITY, false,
                "Sends a message; the server's chat rules still apply.");
        add(modules, "util_command_bind", "Command Bind",
                "Runs a saved command with a key.", PvpCategory.UTILITY, false,
                "Runs a command you wrote; the server's rules still apply.");
        link(modules, "util_item_scroll", "Item Scroll",
                "Scrolls the hotbar with the mouse wheel.", PvpCategory.UTILITY, "hotbar_slot",
                "Sends the same hotbar input a player would.");
        link(modules, "util_hotbar_slot", "Hotbar Slots",
                "Adds tap targets for each hotbar slot.", PvpCategory.UTILITY, "hotbar_slot", LOCAL_OVERLAY);
        link(modules, "util_more_buttons", "More Buttons",
                "Adds extra on-screen control buttons.", PvpCategory.UTILITY, "more_buttons", LOCAL_OVERLAY);
        link(modules, "util_offhand", "Offhand Helper",
                "Opens the inventory to place an offhand item.", PvpCategory.UTILITY, "offhand",
                "Bedrock has no swap key; the module opens the inventory instead.");
        link(modules, "util_zoom", "Zoom",
                "A configurable zoom.", PvpCategory.UTILITY, "zoom", LOCAL_OVERLAY);
        link(modules, "util_toggle_hud", "Toggle HUD",
                "Hides the vanilla HUD.", PvpCategory.UTILITY, "toggle_hud", LOCAL_OVERLAY);
        link(modules, "util_camera_perspective", "Camera Perspective",
                "Cycles the camera perspective.", PvpCategory.UTILITY, "camera_perspective", LOCAL_OVERLAY);
        link(modules, "util_virtual_cursor", "Virtual Cursor",
                "A touch cursor for menus.", PvpCategory.UTILITY, "virtual_cursor", LOCAL_OVERLAY);
        link(modules, "util_gyro", "Gyro",
                "Drives the look with the device gyroscope.", PvpCategory.UTILITY, "gyro",
                "Sends look deltas through the same path mouse look uses.");
        link(modules, "util_pojav_controls", "Pojav Controls",
                "Edits the on-screen control layout.", PvpCategory.UTILITY, "pojav_controls", LOCAL_OVERLAY);
        link(modules, "util_debug_menu", "Debug Menu",
                "A client-side debug readout.", PvpCategory.UTILITY, "debug_menu",
                "Reads the launcher's own state, not the game's internals.");
        link(modules, "util_list_players", "List Players",
                "Lists nearby players with their distance.", PvpCategory.UTILITY, "list_players",
                "Reads the peer feed; vanilla players are invisible to it.");
        add(modules, "util_third_person_nametag", "Third-Person Nametag",
                "Shows your own nametag in third person.", PvpCategory.UTILITY, false,
                "Needs the native entity feed.");
        add(modules, "util_time_changer", "Time Changer",
                "Changes the world time locally.", PvpCategory.UTILITY, false,
                "Client-side only; the server's time is unchanged.");
        add(modules, "util_fullbright", "Fullbright",
                "Brightens the world by removing the dark overlay.", PvpCategory.UTILITY, false,
                "A brightness option, not a light-source change.");
        add(modules, "util_low_fire", "Low Fire",
                "Lowers the fire overlay.", PvpCategory.UTILITY, false, LOCAL_OVERLAY);
        add(modules, "util_block_outline", "Block Outline",
                "Outlines the targeted block.", PvpCategory.UTILITY, false,
                "Needs the block-target read from the game.");
        add(modules, "util_motion_blur", "Motion Blur",
                "A subtle motion blur when moving.", PvpCategory.UTILITY, false, LOCAL_OVERLAY);
        add(modules, "util_saturation", "Saturation Boost",
                "Raises the frame's colour saturation.", PvpCategory.UTILITY, false, LOCAL_OVERLAY);
        add(modules, "util_armor_bending", "Armor View Bending",
                "Flattens the armour-bar bending.", PvpCategory.UTILITY, false, LOCAL_OVERLAY);
        add(modules, "util_projectile_scaler", "Projectile Scaler",
                "Scales thrown-projectile models up.", PvpCategory.UTILITY, false,
                "Needs the entity feed to find the projectile.");
        add(modules, "util_freecam", "Freecam",
                "A detached free camera.", PvpCategory.UTILITY, false,
                "Needs a camera read the launcher does not have.");
        add(modules, "util_tnt", "TNT Outline",
                "Outlines a spot for TNT placement.", PvpCategory.UTILITY, false,
                "Needs the block-target read from the game.");
        add(modules, "util_auto_eat", "Auto Eat",
                "Eats when hunger is low.", PvpCategory.UTILITY, false,
                "Automating item use is what servers restrict or ban.");
        add(modules, "util_auto_pot", "Auto Pot",
                "Drinks a potion when needed.", PvpCategory.UTILITY, false,
                "Automating item use is what servers restrict or ban.");
        add(modules, "util_auto_armor", "Auto Armor",
                "Equips the best armour.", PvpCategory.UTILITY, false,
                "Automating inventory actions is what servers restrict or ban.");
        add(modules, "util_auto_shield", "Auto Shield",
                "Raises the shield when threatened.", PvpCategory.UTILITY, false,
                "A client that acts for the player is what servers restrict or ban.");
        add(modules, "util_auto_soup", "Auto Soup",
                "Uses soup to heal.", PvpCategory.UTILITY, false,
                "Automating item use is what servers restrict or ban.");
        add(modules, "util_chest_stealer", "Chest Stealer",
                "Takes items from a chest quickly.", PvpCategory.UTILITY, false,
                "Automating inventory actions is what servers restrict or ban.");
        add(modules, "util_inv_manager", "Inventory Manager",
                "Moves items between slots.", PvpCategory.UTILITY, false,
                "Automating inventory actions is what servers restrict or ban.");
        add(modules, "util_drop_all", "Drop All",
                "Drops a chosen item stack.", PvpCategory.UTILITY, false,
                "Sends the same drop input a player would.");
        add(modules, "util_keep_sprint", "Keep Sprint",
                "Keeps sprint after an action.", PvpCategory.UTILITY, false, CLIENT_HELPER);
        link(modules, "util_key_strokes_cfg", "Keybind Config",
                "Rebinds the launcher's own keys.", PvpCategory.UTILITY, "mod_menu", LOCAL_OVERLAY);
        link(modules, "util_theme", "UI Theme",
                "Chooses the launcher's theme.", PvpCategory.UTILITY, "mod_menu", LOCAL_OVERLAY);
        add(modules, "util_screenshot", "Quick Screenshot",
                "Saves a screenshot with one key.", PvpCategory.UTILITY, false, LOCAL_OVERLAY);
        link(modules, "util_recorder", "Clip Recorder",
                "Records a clip of recent play.", PvpCategory.UTILITY, "mod_menu",
                "Records your own screen; needs the platform capture consent.");
        link(modules, "util_voice_chat", "Voice Chat",
                "Talks to nearby launcher users.", PvpCategory.UTILITY, "voice_chat",
                "Works between launcher users over LAN or a relay; not with vanilla players.");
        link(modules, "util_mod_hub", "Mod Hub",
                "Browses and installs community content.", PvpCategory.UTILITY, "mod_menu",
                "Modrinth content is overwhelmingly Java Edition; Bedrock hits are rare.");
        add(modules, "util_auto_totem", "Auto Totem",
                "Keeps a totem in the offhand slot.", PvpCategory.UTILITY, false,
                "Automating inventory actions is what servers restrict or ban.");

        // --- World & Environment (30) -------------------------------------------------------------
        add(modules, "world_fullbright", "Fullbright Multiplier",
                "Raises the brightness multiplier.", PvpCategory.WORLD, false,
                "A brightness option, not a light-source change.");
        add(modules, "world_weather_off", "Weather Suppressor",
                "Hides rain and snow on your client.", PvpCategory.WORLD, false, LOCAL_OVERLAY);
        add(modules, "world_tracer", "Entity Tracer",
                "Draws a line to nearby players.", PvpCategory.WORLD, false,
                "Needs the native entity feed; peer tracers need the voice feed.");
        add(modules, "world_player_esp", "Player ESP",
                "Highlights players through walls.", PvpCategory.WORLD, false,
                "Needs the native entity feed.");
        add(modules, "world_item_esp", "Item ESP",
                "Highlights dropped items.", PvpCategory.WORLD, false,
                "Needs the native entity feed.");
        add(modules, "world_chest_esp", "Chest ESP",
                "Highlights chests.", PvpCategory.WORLD, false,
                "Needs the native entity feed.");
        add(modules, "world_mob_esp", "Mob ESP",
                "Highlights mobs.", PvpCategory.WORLD, false,
                "Needs the native entity feed.");
        add(modules, "world_ore_esp", "Ore ESP",
                "Highlights ore blocks.", PvpCategory.WORLD, false,
                "Needs the native block feed.");
        add(modules, "world_cave_finder", "Cave Finder",
                "Highlights cave openings.", PvpCategory.WORLD, false,
                "Needs the native block feed.");
        add(modules, "world_spawner_finder", "Spawner Finder",
                "Highlights monster spawners.", PvpCategory.WORLD, false,
                "Needs the native block feed.");
        add(modules, "world_dungeon_finder", "Dungeon Finder",
                "Highlights dungeon features.", PvpCategory.WORLD, false,
                "Needs the native block feed.");
        add(modules, "world_seed_cracker", "Seed Cracker",
                "Attempts to recover the world seed.", PvpCategory.WORLD, false,
                "The launcher cannot read the world seed.");
        add(modules, "world_slime_chunk", "Slime Chunk Finder",
                "Marks slime chunks.", PvpCategory.WORLD, false,
                "Needs the world seed and the chunk feed.");
        add(modules, "world_light_overlay", "Light Overlay",
                "Shows where mobs can spawn.", PvpCategory.WORLD, false,
                "Needs the native block feed.");
        add(modules, "world_chunk_esp", "Chunk ESP",
                "Highlights loaded chunks.", PvpCategory.WORLD, false,
                "Needs the chunk feed.");
        add(modules, "world_waypoints", "Waypoints",
                "Saves and shows waypoints.", PvpCategory.WORLD, false,
                "Needs the local-player feed for your position.");
        add(modules, "world_death_waypoint", "Death Waypoint",
                "Marks where you last died.", PvpCategory.WORLD, false,
                "Needs the local-player feed for your position.");
        link(modules, "world_trajectory", "Trajectory Prediction",
                "Predicts a projectile arc from your aim.", PvpCategory.WORLD, "trajectory_prediction",
                "Predicts from your aim; it cannot see a projectile in flight.");
        link(modules, "world_hit_prediction", "Hit Prediction",
                "Draws a ghost box at a peer's predicted position.", PvpCategory.WORLD, "hit_prediction",
                "Reads the peer feed; vanilla players are invisible to it.");
        link(modules, "world_kill_effects", "Custom Kill Effects",
                "Plays a particle burst on a credited kill.", PvpCategory.WORLD, "kill_effects",
                "Credit is inferred, never a server-confirmed kill.");
        add(modules, "world_particle_off", "Particle Suppressor",
                "Reduces particle density on your client.", PvpCategory.WORLD, false,
                LOCAL_OVERLAY);
        add(modules, "world_no_fog", "No Fog",
                "Removes distance fog.", PvpCategory.WORLD, false, LOCAL_OVERLAY);
        add(modules, "world_no_void_fog", "No Void Fog",
                "Removes the void fog.", PvpCategory.WORLD, false, LOCAL_OVERLAY);
        add(modules, "world_no_blindness", "No Blindness",
                "Removes the blindness effect overlay.", PvpCategory.WORLD, false, LOCAL_OVERLAY);
        add(modules, "world_no_nausea", "No Nausea",
                "Removes the nausea warp.", PvpCategory.WORLD, false, LOCAL_OVERLAY);
        add(modules, "world_clear_water", "Clear Water",
                "Reduces water fog.", PvpCategory.WORLD, false, LOCAL_OVERLAY);
        add(modules, "world_clear_lava", "Clear Lava",
                "Reduces lava fog.", PvpCategory.WORLD, false, LOCAL_OVERLAY);
        add(modules, "world_xray", "X-Ray",
                "Shows only valuable blocks.", PvpCategory.WORLD, false,
                "A client that alters world rendering is what servers restrict or ban.");
        add(modules, "world_ambience", "Custom Ambience",
                "Changes sky and ambient colour.", PvpCategory.WORLD, false, LOCAL_OVERLAY);
        add(modules, "world_time_lock", "Time Lock",
                "Pins the client's perceived time of day.", PvpCategory.WORLD, false,
                "Client-side only; the server's time is unchanged.");

        MODULES = Collections.unmodifiableList(modules);
    }

    private PvpModuleCatalog() {
    }

    private static void add(List<Module> modules, String id, String name, String description,
                            PvpCategory category, boolean implemented, String scopeNote) {
        modules.add(new Module(id, name, description, category, implemented, scopeNote, null));
    }

    /**
     * Adds an entry that is a view of a module the launcher already ships, so its toggle drives the
     * real module rather than a second copy.
     */
    private static void link(List<Module> modules, String id, String name, String description,
                             PvpCategory category, String existingModId, String scopeNote) {
        modules.add(new Module(id, name, description, category, true, scopeNote, existingModId));
    }

    /**
     * Every module that is a view of a shipped module, keyed by its catalogue id, for the UI to wire
     * a toggle to the real module.
     */
    public static java.util.Map<String, String> linksToExistingModules() {
        java.util.Map<String, String> out = new java.util.LinkedHashMap<>();
        for (Module m : MODULES) {
            if (m.linksToExisting()) out.put(m.id, m.existingModId);
        }
        return out;
    }

    /** Every module, in declaration order (which is also the category order). */
    public static List<Module> modules() {
        return MODULES;
    }

    /** The modules of one category, in declaration order. */
    public static List<Module> byCategory(PvpCategory category) {
        List<Module> out = new ArrayList<>();
        for (Module m : MODULES) {
            if (m.category == category) out.add(m);
        }
        return out;
    }

    /** The modules this build can actually drive. */
    public static List<Module> implemented() {
        List<Module> out = new ArrayList<>();
        for (Module m : MODULES) {
            if (m.implemented) out.add(m);
        }
        return out;
    }

    /** The module with this id, or null. */
    public static Module find(String id) {
        for (Module m : MODULES) {
            if (m.id.equals(id)) return m;
        }
        return null;
    }

    /** Every module id, for the partition assertion. */
    public static List<String> moduleIds() {
        List<String> out = new ArrayList<>();
        for (Module m : MODULES) out.add(m.id);
        return out;
    }

    /** The total module count. */
    public static int count() {
        return MODULES.size();
    }
}
