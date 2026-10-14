package org.chimeramc.client.core.mods.fps;

import android.content.Context;

import org.chimeramc.client.core.mods.inbuilt.manager.InbuiltModManager;
import org.chimeramc.client.core.mods.inbuilt.model.ModIds;
import org.chimeramc.client.core.mods.inbuilt.overlay.InbuiltOverlayManager;

/**
 * Applies {@link FpsPolicy}'s host plan to the launcher's own subsystems.
 *
 * <p>This is the only place the three FPS modules touch the host, and every effect is one the
 * launcher already owns: the overlay tick cadence and the background-request interval. Nothing here
 * reaches Minecraft's renderer, so a module cannot change a frame the player sees. That is the
 * design constraint, not a limitation to fix later.
 *
 * <p>The plan is derived from the module switches and their option values through
 * {@link FpsPolicy}, which is pure and unit-tested, so a switch's effect is a testable function
 * rather than a hidden side effect.
 */
public final class FpsModulesService {

    private static volatile Context sAppContext;
    private static volatile FpsPolicy.HostPlan sPlan =
            new FpsPolicy.HostPlan(FpsPolicy.DEFAULT_OVERLAY_TICK_MS, 0L, false, false, false,
                    "Inactive (no session)");

    private FpsModulesService() {
    }

    public static void init(Context context) {
        if (context != null) sAppContext = context.getApplicationContext();
    }

    /** The most recently applied plan (diagnostics / settings display). */
    public static FpsPolicy.HostPlan currentPlan() {
        return sPlan;
    }

    /**
     * Re-reads the module switches and option values and applies the plan.
     *
     * @param sessionActive whether a game session is running
     * @return the applied plan
     */
    public static FpsPolicy.HostPlan apply(boolean sessionActive) {
        Context context = sAppContext;
        boolean cull = isEnabled(context, ModIds.HYPERSPATIAL_CULL);
        boolean coalesce = isEnabled(context, ModIds.VERTICAL_RENDER_COALESCE);
        boolean tick = isEnabled(context, ModIds.TICK_RATE_PREDICTOR);
        FpsPolicy.HostPlan plan = FpsPolicy.plan(sessionActive, cull, coalesce, tick, reader(context));
        sPlan = plan;
        return plan;
    }

    private static boolean isEnabled(Context context, String modId) {
        if (context == null) return false;
        try {
            InbuiltModManager manager = InbuiltModManager.getInstance(context);
            InbuiltOverlayManager overlayManager = InbuiltOverlayManager.getInstance();
            return overlayManager != null
                    ? overlayManager.isModActive(modId)
                    : manager.resolveInbuiltModEnabled(modId, false);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Reads an option value. The FPS option values are stored with the mod's config store under the
     * module's own key namespace, so a missing value reads the catalogue default rather than 0.
     */
    private static FpsPolicy.ValueSource reader(Context context) {
        return new FpsPolicy.ValueSource() {
            @Override
            public int integer(String key, int fallback) {
                FpsModuleCatalog.Option option = findOption(key);
                int def = option != null ? Integer.parseInt(option.defaultValue) : fallback;
                if (context == null) return def;
                try {
                    return InbuiltModManager.getInstance(context).getModConfigInt(key, def);
                } catch (Throwable t) {
                    return def;
                }
            }

            @Override
            public boolean bool(String key, boolean fallback) {
                FpsModuleCatalog.Option option = findOption(key);
                boolean def = option != null ? Boolean.parseBoolean(option.defaultValue) : fallback;
                if (context == null) return def;
                try {
                    return InbuiltModManager.getInstance(context).getModConfigBool(key, def);
                } catch (Throwable t) {
                    return def;
                }
            }
        };
    }

    private static FpsModuleCatalog.Option findOption(String key) {
        for (FpsModuleCatalog.Option option : FpsModuleCatalog.allOptions()) {
            if (option.key.equals(key)) return option;
        }
        return null;
    }
}
