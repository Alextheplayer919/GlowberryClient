package org.chimeramc.client.core.mods.fps;

/**
 * The host-side decisions the three FPS modules actually make.
 *
 * <p>Each FPS module is a set of options ({@link FpsModuleCatalog}); this class is the pure rule that
 * reads those options and returns what the <em>launcher</em> should change about its own work. It is
 * deliberately separated from the options so the effect of a switch is a testable function of the
 * switch, not a hidden side effect at a call site.
 *
 * <p>Nothing here touches Minecraft. The outputs are the launcher's own knobs:
 * <ul>
 *   <li>{@link HostPlan#overlayTickIntervalMs} — how often the launcher advances its overlays;</li>
 *   <li>{@link HostPlan#backgroundRequestIntervalMs} — how often it refreshes background data;</li>
 *   <li>{@link HostPlan#dropDistantAnimation} — whether it stops animating off-screen markers;</li>
 *   <li>{@link HostPlan#coalesceNetworkRequests} — whether it batches its own small requests.</li>
 * </ul>
 *
 * <p>The plan never claims a frame-rate number. A {@link HostPlan} is a description of host work, and
 * the tests pin that no output contains a "fps" figure.
 */
public final class FpsPolicy {

    /** What the launcher should do to its own work this session. */
    public static final class HostPlan {
        /** Milliseconds between the launcher's overlay ticks. */
        public final long overlayTickIntervalMs;
        /** Milliseconds between the launcher's background data refreshes. */
        public final long backgroundRequestIntervalMs;
        /** Whether the launcher stops animating off-screen/distant markers. */
        public final boolean dropDistantAnimation;
        /** Whether the launcher batches its own small network requests. */
        public final boolean coalesceNetworkRequests;
        /** Whether the launcher keeps its cosmetic atlases resident. */
        public final boolean keepAtlasesResident;
        /** A short honest description of the plan. */
        public final String description;

        HostPlan(long overlayTickIntervalMs, long backgroundRequestIntervalMs,
                 boolean dropDistantAnimation, boolean coalesceNetworkRequests,
                 boolean keepAtlasesResident, String description) {
            this.overlayTickIntervalMs = overlayTickIntervalMs;
            this.backgroundRequestIntervalMs = backgroundRequestIntervalMs;
            this.dropDistantAnimation = dropDistantAnimation;
            this.coalesceNetworkRequests = coalesceNetworkRequests;
            this.keepAtlasesResident = keepAtlasesResident;
            this.description = description;
        }
    }

    /** The interval the launcher uses when the modules are off: it ticks overlays every frame. */
    public static final long DEFAULT_OVERLAY_TICK_MS = 0L;

    private FpsPolicy() {
    }

    /**
     * Builds the host plan from the enabled modules and their option values.
     *
     * @param sessionActive whether a game session is running (the modules are no-ops otherwise)
     * @param cullEnabled   the HyperSpatial culling module
     * @param coalesceEnabled the vertical-rendering / draw-call coalescing module
     * @param tickEnabled   the tick-rate / animation predictor module
     * @param values        the option values, keyed by option key; a missing key reads its default
     */
    public static HostPlan plan(boolean sessionActive, boolean cullEnabled, boolean coalesceEnabled,
                                boolean tickEnabled, ValueSource values) {
        if (!sessionActive) {
            return new HostPlan(DEFAULT_OVERLAY_TICK_MS, 0L, false, false, false,
                    "Inactive (no session)");
        }

        long overlayInterval = DEFAULT_OVERLAY_TICK_MS;
        long backgroundInterval = 0L;
        boolean dropDistant = false;
        boolean coalesceNet = false;
        boolean keepAtlases = false;

        // Culling module: stop advancing off-screen animation and keep the atlas resident.
        if (cullEnabled) {
            dropDistant = values.bool("cull_skip_offscreen_anim", false);
        }

        // Coalescing module: keep atlases resident and raise the overlay tick interval to the slice
        // batch, so the launcher advances its overlays in batches rather than every frame.
        if (coalesceEnabled) {
            keepAtlases = values.bool("vc_atlas_lock", false);
            int sliceBatch = values.integer("vc_slice_batch", 8);
            overlayInterval = Math.max(0, sliceBatch) * 2L; // ~2 ms per slice, bounded and honest
        }

        // Tick module: throttle the launcher's own background refresh and coalesce its requests.
        if (tickEnabled) {
            int intervalSeconds = values.integer("tr_net_interval", 60);
            backgroundInterval = Math.max(5, intervalSeconds) * 1000L;
            coalesceNet = values.bool("tr_coalesce_net", false);
            if (values.bool("tr_batch_ticks", false)) {
                int n = Math.max(1, values.integer("tr_batch_n", 4));
                // The overlay interval becomes the batch size in frames; a bigger batch is a longer
                // interval, which is the honest reading of "run this every Nth tick".
                overlayInterval = Math.max(overlayInterval, n * 16L);
            }
        }

        String description = describe(cullEnabled, coalesceEnabled, tickEnabled);
        return new HostPlan(overlayInterval, backgroundInterval, dropDistant, coalesceNet,
                keepAtlases, description);
    }

    private static String describe(boolean cull, boolean coalesce, boolean tick) {
        int active = (cull ? 1 : 0) + (coalesce ? 1 : 0) + (tick ? 1 : 0);
        if (active == 0) {
            return "No FPS modules enabled";
        }
        StringBuilder b = new StringBuilder("Host work reduced: ");
        boolean first = true;
        if (cull) {
            b.append("overlay culling");
            first = false;
        }
        if (coalesce) {
            if (!first) b.append(", ");
            b.append("draw batching");
            first = false;
        }
        if (tick) {
            if (!first) b.append(", ");
            b.append("tick throttling");
        }
        return b.toString();
    }

    /** Reads option values by key, so the policy is testable without a SharedPreferences. */
    public interface ValueSource {
        int integer(String key, int fallback);

        boolean bool(String key, boolean fallback);
    }
}
