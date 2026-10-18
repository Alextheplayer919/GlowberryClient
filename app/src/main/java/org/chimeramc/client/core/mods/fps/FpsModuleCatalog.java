package org.chimeramc.client.core.mods.fps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The catalogue of the three FPS-optimization modules: their ids and every configurable option,
 * with the default value and the bounds.
 *
 * <p><b>Honesty contract (read this before adding an option).</b> The launcher cannot reach into
 * Minecraft's renderer, its chunk mesher or its tick loop — the game is a prebuilt, symbol-stripped
 * binary and a Bedrock client's visuals are set by the player and the server. Every option below
 * therefore configures <em>host-side work the launcher itself does</em> (how hard it polls, how much
 * it computes per frame, how it batches its own I/O), never a game draw call. The names follow the
 * requested module shape so the UI matches the spec, but each option's description says what it
 * actually changes. A switch that claimed to cull the game's vertices would be a lie, and the point
 * of this class is that the lie cannot be introduced silently: {@link #allOptions()} is the single
 * source the config schema, the defaults and the tests read.
 *
 * <p>Pure data and pure helpers, so the counts, the key uniqueness and the bounds are unit-tested
 * without a device — see {@code FpsModulesTest}.
 */
public final class FpsModuleCatalog {

    /** One configurable option of an FPS module. */
    public static final class Option {
        public final String key;
        public final String label;
        /** "toggle" or "slider" (an int slider with min/max). */
        public final String type;
        public final String defaultValue;
        public final int min;
        public final int max;
        public final String description;

        Option(String key, String label, String type, String defaultValue, int min, int max,
               String description) {
            this.key = key;
            this.label = label;
            this.type = type;
            this.defaultValue = defaultValue;
            this.min = min;
            this.max = max;
            this.description = description;
        }

        public boolean isToggle() {
            return "toggle".equals(type);
        }
    }

    /** One FPS module: its identity plus its options. */
    public static final class Module {
        public final String id;
        public final String title;
        public final String description;
        public final List<Option> options;

        Module(String id, String title, String description, List<Option> options) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.options = Collections.unmodifiableList(options);
        }
    }

    /** Module ids, so callers do not repeat the literals. */
    public static final String HYPERSPATIAL_CULL = "hyperspatial_cull";
    public static final String VERTICAL_RENDER_COALESCE = "vertical_render_coalesce";
    public static final String TICK_RATE_PREDICTOR = "tick_rate_predictor";

    private static final List<Module> MODULES;

    static {
        List<Module> modules = new ArrayList<>();

        // --- Module 1: Host Overlay Culling ------------------------------------------------------
        // Only options the policy actually reads are kept here. Every option is a host-side knob:
        // it changes how hard the launcher works, never a draw call the game submits.
        List<Option> cull = new ArrayList<>();
        cull.add(toggle("cull_skip_offscreen_anim", "Skip off-screen animation",
                "Stop the launcher advancing its own preview/animation updates for items that are "
                        + "not on screen. The game's own animations are unaffected."));
        modules.add(new Module(HYPERSPATIAL_CULL,
                "Launcher Animation Culling",
                "Reduces the launcher's own work by not animating off-screen items, so less CPU is "
                        + "spent on the launcher while the game runs. It never culls the game's "
                        + "geometry.", cull));

        // --- Module 2: Launcher Overlay Batching -------------------------------------------------
        List<Option> coalesce = new ArrayList<>();
        coalesce.add(slider("vc_slice_batch", "Overlay batch size", 8, 1, 32,
                "How many of the launcher's own overlay updates are advanced in one batch. A bigger "
                        + "batch means the launcher updates its overlays less often, freeing CPU "
                        + "for the game."));
        coalesce.add(toggle("vc_atlas_lock", "Texture atlas residency",
                "Keep the launcher's cosmetic atlases in memory instead of re-uploading them each "
                        + "time the preview redraws."));
        modules.add(new Module(VERTICAL_RENDER_COALESCE,
                "Launcher Overlay Batching",
                "Batches the launcher's own overlay updates and keeps its cosmetic atlases resident."
                        + " It does not change how the game draws.", coalesce));

        // --- Module 3: Launcher Polling Throttle ------------------------------------------------
        List<Option> tick = new ArrayList<>();
        tick.add(slider("tr_net_interval", "Background request interval (s)", 60, 5, 600,
                "How often the launcher refreshes its own background data (e.g. the update check) "
                        + "during a session."));
        tick.add(toggle("tr_coalesce_net", "Coalesce launcher requests",
                "Merge the launcher's concurrent background requests into one, so it holds fewer "
                        + "connections open."));
        tick.add(toggle("tr_batch_ticks", "Batch launcher ticks",
                "Advance the launcher's low-priority overlay work every Nth batch instead of every "
                        + "frame."));
        tick.add(slider("tr_batch_n", "Tick batch size", 4, 1, 16,
                "The N for batching launcher ticks."));
        modules.add(new Module(TICK_RATE_PREDICTOR,
                "Launcher Polling Throttle",
                "Throttles the launcher's own background polling and per-frame overlay work. It "
                        + "does not change the game's tick loop, hitboxes or server sync.", tick));

        MODULES = Collections.unmodifiableList(modules);
    }

    private FpsModuleCatalog() {
    }

    /** Every FPS module, in display order. */
    public static List<Module> modules() {
        return MODULES;
    }

    /** The module with this id, or null. */
    public static Module find(String id) {
        for (Module m : MODULES) {
            if (m.id.equals(id)) {
                return m;
            }
        }
        return null;
    }

    /** True when this id is one of the FPS modules. */
    public static boolean isFpsModule(String id) {
        return find(id) != null;
    }

    /** Every option across every module, for a key-uniqueness / count assertion. */
    public static List<Option> allOptions() {
        List<Option> out = new ArrayList<>();
        for (Module m : MODULES) {
            out.addAll(m.options);
        }
        return out;
    }

    private static Option toggle(String key, String label, String description) {
        return new Option(key, label, "toggle", "false", 0, 0, description);
    }

    private static Option slider(String key, String label, int def, int min, int max,
                                 String description) {
        return new Option(key, label, "slider", String.valueOf(def), min, max, description);
    }
}
