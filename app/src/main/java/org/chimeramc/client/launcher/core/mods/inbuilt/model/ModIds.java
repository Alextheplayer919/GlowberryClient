package org.chimeramc.client.core.mods.inbuilt.model;

public final class ModIds {
    /** Group id for combat/PvP modules; the Mod Menu exposes it as the PvP tab. */
    public static final String GROUP_PVP = "pvp";

    /** Group id for social modules (proximity voice), so the menu can section them apart. */
    public static final String GROUP_VOICE = "voice";

    public static final String QUICK_DROP = "quick_drop";
    public static final String CAMERA_PERSPECTIVE = "camera_perspective";
    public static final String TOGGLE_HUD = "toggle_hud";
    public static final String AUTO_SPRINT = "auto_sprint";
    public static final String CHICK_PET = "chick_pet";
    public static final String ZOOM = "zoom";
    public static final String FPS_DISPLAY = "fps_display";
    public static final String CPS_DISPLAY = "cps_display";
    public static final String SNAPLOOK = "snaplook";
    public static final String VIRTUAL_CURSOR = "virtual_cursor";
    public static final String GYRO = "gyro";
    public static final String POJAV_CONTROLS = "pojav_controls";
    public static final String MORE_BUTTONS = "more_buttons";
    public static final String HOTBAR_SLOT = "hotbar_slot";
    public static final String OFFHAND = "offhand";
    public static final String AIM_SETTINGS = "aim_settings";
    public static final String MOD_MENU = "mod_menu";
    public static final String ARMOR_HUD = "armor_hud";
    public static final String CRYSTAL_OPTIMIZER = "crystal_optimizer";
    public static final String HIT_REGISTRATION = "hit_registration";
    public static final String HIT_TIMING = "hit_timing";
    public static final String HITBOX = "hitbox";
    public static final String VOICE_CHAT = "voice_chat";
    /** PvP Suite (V1.1): overlay-only visual aids, filed under the General tab. */
    public static final String REACH_INDICATOR = "reach_indicator";
    public static final String TRAJECTORY_PREDICTION = "trajectory_prediction";
    public static final String HIT_PREDICTION = "hit_prediction";
    public static final String KILL_EFFECTS = "kill_effects";

    // --- FPS optimization modules (host-side performance, filed under the General tab) --------
    /** HyperSpatial Vertex Culling & Occlusion Matrix (host-side visibility/batching). */
    public static final String HYPERSPATIAL_CULL = "hyperspatial_cull";
    /** Advanced Vertical Rendering & Draw-Call Coalescing (host-side batching). */
    public static final String VERTICAL_RENDER_COALESCE = "vertical_render_coalesce";
    /** Intelligent Tick-Rate & Animation Predictor (host-side tick throttling). */
    public static final String TICK_RATE_PREDICTOR = "tick_rate_predictor";

    // --- Visual / HUD / Misc modules (the mod-panel list) ------------------------------------

    /** Brightens the world by removing the dark overlay, a full-bright visual. */
    public static final String FULLBRIGHT = "fullbright";
    /** Cycles the world time locally (client-side, no server command). */
    public static final String TIME_CHANGER = "time_changer";
    /** Lowers the fire overlay so it does not block the view. */
    public static final String LOW_FIRE = "low_fire";
    /** Draws the block the crosshair is on with an outline. */
    public static final String BLOCK_OUTLINE = "block_outline";
    /** A subtle screen motion blur when moving. */
    public static final String MOTION_BLUR = "motion_blur";
    /** Raises colour saturation of the rendered frame. */
    public static final String SATURATION = "saturation";
    /** A customisable crosshair, drawn by the client. */
    public static final String CROSSHAIR = "crosshair";
    /** Softens the armor-bar bending so the HUD reads flatter. */
    public static final String ARMOR_VIEW_BENDING = "armor_view_bending";
    /** Scales thrown-projectile models up so they are easier to see. */
    public static final String PROJECTILE_SCALER = "projectile_scaler";
    /** A detached free camera, separate from the player's view. */
    public static final String FREECAM = "freecam";
    /** A thin-outline block selection for TNT placement, a build aid. */
    public static final String TNT = "tnt";

    // --- Utility / information modules --------------------------------------------------------

    /** Lists players in the world with their distance, a client-side overlay. */
    public static final String LIST_PLAYERS = "list_players";
    /** A client-side debug readout (position, chunk, fps, entities). */
    public static final String DEBUG_MENU = "debug_menu";
    /** Draws a nametag on the player in third person, which vanilla omits. */
    public static final String THIRD_PERSON_NAMETAG = "third_person_nametag";

    /** The module id for the in-game Mod Menu itself. */
    public static final String MOD_MENU_ID = MOD_MENU;

    /**
     * Combat-oriented modules the Mod Menu files under its PvP tab. Kept here rather than
     * derived from the group string at the call site so the filter and the provider cannot
     * drift apart.
     */
    private static final java.util.Set<String> PVP_MODULES = java.util.Set.of(
            AIM_SETTINGS, CPS_DISPLAY, SNAPLOOK, CRYSTAL_OPTIMIZER, HIT_REGISTRATION,
            HIT_TIMING, HITBOX);

    public static boolean isPvpModule(String modId) {
        if (modId == null) return false;
        if (PVP_MODULES.contains(modId)) return true;
        // A PvP catalogue entry is part of the PvP section too, so the tab filter and the group
        // header agree about it. The catalogue entries are appended last and are all in GROUP_PVP,
        // so they stay one contiguous run.
        return org.chimeramc.client.core.mods.pvp.PvpModuleCatalog.find(modId) != null;
    }

    /**
     * Social modules the Mod Menu files under its Voice section. Same reasoning as
     * {@link #isPvpModule}: the section predicate and the provider's grouping must agree.
     */
    private static final java.util.Set<String> VOICE_MODULES = java.util.Set.of(VOICE_CHAT);

    public static boolean isVoiceModule(String modId) {
        return modId != null && VOICE_MODULES.contains(modId);
    }

    /**
     * True for modules that are <em>entirely</em> dependent on a per-frame data feed from the
     * game process and have no other route to a result: durability/target reads (Armor HUD),
     * placement geometry (Crystal Optimizer), the held-item read the trajectory arc needs
     * (Trajectory Prediction) and the opponent-death feed kill credit needs (Custom Kill
     * Effects). With no native provider installed none of them can produce anything, so the Mod
     * Menu badges them and refuses the toggle.
     *
     * <p>The Hitboxes module is deliberately <em>not</em> here. It draws other players from the
     * proximity-voice feed, which needs no native hook at all, so it works today; only some of its
     * entity kinds need the native feed. Those are listed by {@link #outputRequiresGameData} and
     * are disabled per-option rather than taking the whole module down. The Reach Indicator and
     * Hit Prediction share that peer route and stay usable for the same reason.
     */
    public static boolean requiresGameData(String modId) {
        // A PvP catalogue roadmap entry (one this build cannot drive) is badged by the same rule:
        // it is listed for discoverability but must not present a switch that does nothing.
        if (org.chimeramc.client.core.mods.pvp.PvpModuleCatalog.find(modId) != null
                && !org.chimeramc.client.core.mods.pvp.PvpModuleCatalog.find(modId).implemented) {
            return true;
        }
        return ARMOR_HUD.equals(modId) || CRYSTAL_OPTIMIZER.equals(modId)
                || TRAJECTORY_PREDICTION.equals(modId) || KILL_EFFECTS.equals(modId)
                // These visual modules act on the game's rendered frame or its entity list, which
                // this build exposes to no mod yet (the prebuilt libinbuiltmods.so supports only
                // AutoSprint/Fps/Gyro/HotbarSlot/MoreButtons/PojavControls/Snaplook/Zoom), so with
                // no native provider they cannot produce anything. Listing them as if they worked
                // would be the defect. Not listed here, because they reach a result through a route
                // this build already has: Fullbright (a brightness option), Crosshair (an overlay),
                // and List Players / Debug Menu (the local-player feed and the proximity-voice peer
                // list).
                || TIME_CHANGER.equals(modId) || LOW_FIRE.equals(modId)
                || BLOCK_OUTLINE.equals(modId) || MOTION_BLUR.equals(modId)
                || SATURATION.equals(modId) || ARMOR_VIEW_BENDING.equals(modId)
                || PROJECTILE_SCALER.equals(modId) || FREECAM.equals(modId)
                || TNT.equals(modId)
                // Third-person nametags need the drawn actor's world position, which needs the
                // native entity feed.
                || THIRD_PERSON_NAMETAG.equals(modId);
    }

    /**
     * Hitbox outputs that can only be produced by the native entity feed.
     *
     * <p>A hitbox for a mob, a dropped item, a thrown item or a projectile needs the game's
     * entity list, which this build cannot read (see the native-feed notes). The player box, the
     * aim line and the chest-height target box are <em>not</em> here: peers advertise their own
     * position and view over the voice protocol, so those three work without the game process.
     * Keeping the distinction in one place is what stops the config dialog from offering a switch
     * that can never draw anything.
     */
    private static final java.util.Set<String> HITBOX_NATIVE_OUTPUTS = java.util.Set.of(
            "hitbox_show_mobs", "hitbox_show_items", "hitbox_show_projectiles",
            "hitbox_show_thrown_items", "hitbox_show_crit_line", "hitbox_show_combo_box");

    public static boolean outputRequiresGameData(String modId, String configKey) {
        return HITBOX.equals(modId) && configKey != null && HITBOX_NATIVE_OUTPUTS.contains(configKey);
    }

    private ModIds() {}
}
