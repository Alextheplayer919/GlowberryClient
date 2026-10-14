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

        // --- Module 1: HyperSpatial Vertex Culling & Occlusion Matrix (25 options) ---------------
        List<Option> cull = new ArrayList<>();
        cull.add(slider("cull_frustum_margin", "Frustum cull aggressiveness", 0, 0, 100,
                "How much margin the launcher's own visibility pre-pass keeps before it skips "
                        + "computing an entity it is tracking. 0 keeps everything (no saving)."));
        cull.add(toggle("cull_subchunk_skip", "Sub-chunk occlusion skip",
                "Skip the launcher's per-sub-chunk bookkeeping for chunks it has already marked "
                        + "occluded. Does not change which chunks the game draws."));
        cull.add(slider("cull_entity_radius", "Entity batching radius", 48, 8, 128,
                "Blocks within which the launcher batches entity updates into one pass."));
        cull.add(slider("cull_particle_throttle", "Particle density throttling", 100, 0, 100,
                "Percentage of the launcher's own particle bookkeeping it keeps. The game's own "
                        + "particles are untouched."));
        cull.add(toggle("cull_mesh_simplify", "Dynamic mesh simplification",
                "Collapse the launcher's preview/overlay geometry to fewer quads while moving."));
        cull.add(toggle("cull_transparent_sort_bypass", "Transparent sort bypass",
                "Skip re-sorting the launcher's overlay draw list when nothing moved."));
        cull.add(toggle("cull_ui_layer_batch", "UI layer draw batching",
                "Merge the launcher's overlay windows into fewer canvas passes."));
        cull.add(toggle("cull_skybox_strip", "Skybox geometry strip",
                "Skip the launcher's background scene (the splash/preview backdrop) while a "
                        + "session runs. The game's sky is not affected."));
        cull.add(toggle("cull_cloud_deactivate", "Cloud mesh deactivation",
                "Pause the launcher's preview cloud/backdrop animation during a session."));
        cull.add(slider("cull_shadow_clamp", "Shadow map resolution clamp", 0, 0, 4096,
                "Upper bound the launcher requests for its own offscreen preview target. 0 leaves "
                        + "it to the device. The game's shadow map is not affected."));
        cull.add(toggle("cull_thread_affinity", "Worker thread affinity lock",
                "Pin the launcher's compute worker to a core away from the render thread."));
        cull.add(toggle("cull_vertex_reuse", "Vertex buffer reuse pool",
                "Reuse the launcher's own vertex buffers instead of reallocating per frame."));
        cull.add(slider("cull_depth_discard", "Depth-stencil discard threshold", 0, 0, 100,
                "How aggressively the launcher discards its own offscreen tiles with no visible "
                        + "content. 0 keeps every tile."));
        cull.add(toggle("cull_backface_prepass", "Back-face pre-pass",
                "Cull the launcher's own geometry back faces before upload."));
        cull.add(toggle("cull_occlusion_cache", "Occlusion result cache",
                "Reuse the previous frame's occlusion result for entities that have not moved."));
        cull.add(slider("cull_cache_ttl", "Occlusion cache lifetime (ms)", 250, 0, 2000,
                "How long an occlusion result stays valid before it is recomputed."));
        cull.add(toggle("cull_skip_offscreen_anim", "Skip off-screen animation",
                "Stop advancing the launcher's preview animation for off-screen items."));
        cull.add(slider("cull_lod_bias", "Distance LOD bias", 0, -50, 50,
                "Bias the launcher applies to its own distance LOD decisions for tracked markers."));
        cull.add(toggle("cull_batch_static", "Static batch merge",
                "Merge the launcher's static overlay elements into one cached layer."));
        cull.add(toggle("cull_frustum_cache", "Frustum plane cache",
                "Cache the frustum planes instead of rebuilding them each frame."));
        cull.add(slider("cull_max_draws", "Per-frame draw budget", 0, 0, 512,
                "Upper bound on the launcher's own overlay draw calls per frame. 0 is unlimited."));
        cull.add(toggle("cull_skip_empty_layers", "Skip empty layers",
                "Do not create a canvas layer for an overlay with nothing to draw."));
        cull.add(toggle("cull_geometry_lod", "Geometry LOD by distance",
                "Use the launcher's coarse preview mesh for distant markers."));
        cull.add(toggle("cull_reduce_overdraw", "Reduce overdraw",
                "Clip the launcher's overlay quads against the ones already painted."));
        cull.add(toggle("cull_async_upload", "Async buffer upload",
                "Hand the launcher's buffer uploads to a worker instead of the frame thread."));
        modules.add(new Module(HYPERSPATIAL_CULL,
                "HyperSpatial Vertex Culling & Occlusion Matrix",
                "Host-side visibility and batching for the launcher's own overlays and previews. "
                        + "It never culls the game's geometry.", cull));

        // --- Module 2: Advanced Vertical Rendering & Draw-Call Coalescing (22 options) -----------
        List<Option> coalesce = new ArrayList<>();
        coalesce.add(slider("vc_slice_batch", "Vertical slice batch size", 8, 1, 32,
                "How many vertical slices the launcher processes in one batch."));
        coalesce.add(slider("vc_merge_threshold", "Draw-call merging threshold", 16, 1, 64,
                "Merge the launcher's adjacent draw calls up to this many into one instanced pass."));
        coalesce.add(toggle("vc_tick_decouple", "CPU tick-rate decoupling for distant entities",
                "Advance the launcher's distant-marker bookkeeping at a lower rate than the frame."));
        coalesce.add(toggle("vc_command_reuse", "Command buffer reuse",
                "Reuse the launcher's recorded command list when the overlay state is unchanged."));
        coalesce.add(slider("vc_gc_interval", "Memory pool GC interval (s)", 30, 1, 300,
                "How often the launcher trims its own pooled allocations."));
        coalesce.add(toggle("vc_atlas_lock", "Texture atlas page locking",
                "Keep the launcher's cosmetic atlases resident instead of re-uploading."));
        coalesce.add(toggle("vc_uniform_cache", "Shader uniform cache",
                "Cache the launcher's overlay shader uniforms instead of setting them per draw."));
        coalesce.add(toggle("vc_attr_compress", "Vertex attribute compression",
                "Pack the launcher's vertex attributes into a narrower format."));
        coalesce.add(slider("vc_thread_priority", "Worker thread priority", 5, 1, 10,
                "Priority the launcher gives its compute worker (1 lowest, 10 highest)."));
        coalesce.add(toggle("vc_cache_pad", "Cache-line padding",
                "Pad the launcher's shared counters to avoid false sharing."));
        coalesce.add(slider("vc_prealloc", "Buffer pre-allocation limit", 64, 0, 512,
                "Number of buffers the launcher pre-allocates. 0 disables pre-allocation."));
        coalesce.add(toggle("vc_index_reuse", "Index buffer reuse",
                "Reuse the launcher's index buffers across frames."));
        coalesce.add(toggle("vc_batch_small", "Small-draw coalescing",
                "Combine the launcher's sub-4-vertex draws into one call."));
        coalesce.add(toggle("vc_defer_upload", "Deferred upload",
                "Queue the launcher's uploads and flush them once per frame."));
        coalesce.add(slider("vc_upload_budget", "Per-frame upload budget (KB)", 256, 0, 8192,
                "Upper bound on bytes the launcher uploads per frame. 0 is unlimited."));
        coalesce.add(toggle("vc_pool_recycle", "Pool recycling",
                "Return the launcher's transient objects to a pool instead of freeing them."));
        coalesce.add(toggle("vc_static_geometry", "Static geometry caching",
                "Cache the launcher's unchanging geometry in one buffer."));
        coalesce.add(toggle("vc_state_sort", "State-change sorting",
                "Sort the launcher's draws to minimise state changes."));
        coalesce.add(toggle("vc_double_buffer", "Double-buffered transfer",
                "Write the launcher's next frame's data while the current one is used."));
        coalesce.add(slider("vc_align", "Buffer alignment (bytes)", 64, 16, 4096,
                "Alignment the launcher uses for its buffers."));
        coalesce.add(toggle("vc_small_cache", "Small-buffer cache",
                "Cache the launcher's small, frequently reused buffers."));
        coalesce.add(toggle("vc_flush_on_idle", "Flush on idle",
                "Flush the launcher's queued work when the session is idle rather than per frame."));
        modules.add(new Module(VERTICAL_RENDER_COALESCE,
                "Advanced Vertical Rendering & Draw-Call Coalescing",
                "Batches the launcher's own vertical-slice overlays and merges its draw calls. It "
                        + "does not change how the game submits geometry.", coalesce));

        // --- Module 3: Intelligent Tick-Rate & Animation Predictor (20 options) ------------------
        List<Option> tick = new ArrayList<>();
        tick.add(slider("tr_anim_lod", "Distant animation LOD scaling", 100, 0, 100,
                "Percentage of the launcher's distant-marker animation it keeps."));
        tick.add(toggle("tr_fluid_skip", "Idle physics tick skip",
                "Skip the launcher's idle environmental bookkeeping when nothing has changed."));
        tick.add(slider("tr_merge_radius", "Item drop merging radius", 2, 0, 16,
                "Radius the launcher uses when it coalesces repeated item-marker updates."));
        tick.add(slider("tr_path_budget", "Pathfinding calculation budget", 0, 0, 64,
                "Upper bound on the launcher's own route computations per tick. 0 disables them."));
        tick.add(slider("tr_packet_window", "Network packet batching window (ms)", 0, 0, 250,
                "How long the launcher holds its own small requests to batch them. 0 sends at once."));
        tick.add(toggle("tr_input_poll", "Input polling optimisation",
                "Coalesce the launcher's own poll reads instead of reading every frame."));
        tick.add(toggle("tr_gc_suppress", "Garbage-collection suppression during combat",
                "Ask the launcher's worker to avoid allocating while a hit is in flight."));
        tick.add(toggle("tr_entity_lookup", "Entity lookup optimisation",
                "Index the launcher's peer list instead of scanning it per marker."));
        tick.add(slider("tr_collision_bounds", "Collision check bounds", 16, 1, 64,
                "Blocks within which the launcher checks its own marker collisions."));
        tick.add(slider("tr_cache_invalidate", "Cache invalidation timer (ms)", 500, 0, 5000,
                "How long the launcher's derived caches stay valid."));
        tick.add(toggle("tr_prediction", "Animation prediction",
                "Extrapolate the launcher's marker motion between ticks so it reads smoothly."));
        tick.add(slider("tr_prediction_ms", "Prediction look-ahead (ms)", 50, 0, 250,
                "How far ahead the launcher predicts marker motion."));
        tick.add(toggle("tr_batch_ticks", "Tick batching",
                "Run the launcher's low-priority work every Nth tick instead of every tick."));
        tick.add(slider("tr_batch_n", "Tick batch size", 4, 1, 16,
                "The N for tick batching."));
        tick.add(toggle("tr_idle_backoff", "Idle backoff",
                "Increase the launcher's own work interval while the player is idle."));
        tick.add(slider("tr_idle_after", "Idle after (s)", 5, 1, 60,
                "Seconds of no input after which the launcher backs off."));
        tick.add(toggle("tr_skip_hidden", "Skip hidden overlays",
                "Do not tick an overlay that is not currently visible."));
        tick.add(toggle("tr_coalesce_net", "Coalesce launcher requests",
                "Merge the launcher's concurrent background requests into one."));
        tick.add(slider("tr_net_interval", "Background request interval (s)", 60, 5, 600,
                "How often the launcher refreshes its background data during a session."));
        tick.add(toggle("tr_combat_priority", "Combat priority",
                "Raise the launcher's input/overlay priority while a fight is detected."));
        modules.add(new Module(TICK_RATE_PREDICTOR,
                "Intelligent Tick-Rate & Animation Predictor",
                "Throttles the launcher's own non-essential per-tick work and predicts marker "
                        + "motion. It does not change player hitboxes, server sync or the game's "
                        + "tick loop.", tick));

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
