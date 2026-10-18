package org.chimeramc.client.core.mods.fps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.chimeramc.client.core.mods.inbuilt.model.ModIds;
import org.junit.Test;

/**
 * Pins the three FPS-optimization modules (no mocks).
 *
 * <p>Two things matter here. The catalogue must be exactly the three modules with the implemented
 * option counts (1/2/4 = 7 options total — only the ones FpsPolicy actually reads) and unique
 * option keys, so the config schema cannot bind a key to the wrong option. And the policy must be
 * honest: every description says the change is to the launcher's own host work, and no output may
 * contain an "fps" figure the launcher cannot measure.
 */
public class FpsModulesTest {

    @Test
    public void theCatalogueHasTheThreeModulesWithOnlyTheImplementedOptions() {
        List<FpsModuleCatalog.Module> modules = FpsModuleCatalog.modules();
        assertEquals(3, modules.size());
        assertEquals(1, FpsModuleCatalog.find(FpsModuleCatalog.HYPERSPATIAL_CULL).options.size());
        assertEquals(2, FpsModuleCatalog.find(FpsModuleCatalog.VERTICAL_RENDER_COALESCE).options.size());
        assertEquals(4, FpsModuleCatalog.find(FpsModuleCatalog.TICK_RATE_PREDICTOR).options.size());
    }

    @Test
    public void everyOptionKeyIsUniqueAcrossTheThreeModules() {
        Set<String> seen = new HashSet<>();
        for (FpsModuleCatalog.Option option : FpsModuleCatalog.allOptions()) {
            assertTrue("duplicate option key: " + option.key, seen.add(option.key));
        }
        assertEquals(1 + 2 + 4, seen.size());
    }

    @Test
    public void everyOptionIsOneThePolicyReads() {
        // The catalogue and the policy must agree: a displayed option that FpsPolicy never reads is
        // a dead switch. Both sides were trimmed together, so this pins the remaining seven.
        assertNotNull(FpsModuleCatalog.find(ModIds.HYPERSPATIAL_CULL));
        for (FpsModuleCatalog.Option option : FpsModuleCatalog.allOptions()) {
            boolean read = option.key.equals("cull_skip_offscreen_anim")
                    || option.key.equals("vc_slice_batch")
                    || option.key.equals("vc_atlas_lock")
                    || option.key.equals("tr_net_interval")
                    || option.key.equals("tr_coalesce_net")
                    || option.key.equals("tr_batch_ticks")
                    || option.key.equals("tr_batch_n");
            assertTrue(option.key + " is shown but no policy reads it", read);
        }
    }

    @Test
    public void everySliderDefaultIsWithinItsBounds() {
        for (FpsModuleCatalog.Option option : FpsModuleCatalog.allOptions()) {
            if (option.isToggle()) continue;
            int value = Integer.parseInt(option.defaultValue);
            assertTrue(option.key + " default below min", value >= option.min);
            assertTrue(option.key + " default above max", value <= option.max);
            assertTrue(option.key + " min > max", option.min <= option.max);
        }
    }

    @Test
    public void everyOptionCarriesANonEmptyDescription() {
        for (FpsModuleCatalog.Option option : FpsModuleCatalog.allOptions()) {
            assertNotNull(option.description);
            assertFalse(option.key + " has no description", option.description.isEmpty());
        }
    }

    @Test
    public void isFpsModuleRecognisesOnlyTheThree() {
        assertTrue(FpsModuleCatalog.isFpsModule(FpsModuleCatalog.HYPERSPATIAL_CULL));
        assertTrue(FpsModuleCatalog.isFpsModule(FpsModuleCatalog.VERTICAL_RENDER_COALESCE));
        assertTrue(FpsModuleCatalog.isFpsModule(FpsModuleCatalog.TICK_RATE_PREDICTOR));
        assertFalse(FpsModuleCatalog.isFpsModule("aim_settings"));
        assertFalse(FpsModuleCatalog.isFpsModule(null));
        assertNull(FpsModuleCatalog.find("nope"));
    }

    /** A value source with no stored values, so every option reads its default. */
    private static FpsPolicy.ValueSource defaults() {
        return new FpsPolicy.ValueSource() {
            @Override
            public int integer(String key, int fallback) {
                return fallback;
            }

            @Override
            public boolean bool(String key, boolean fallback) {
                return fallback;
            }
        };
    }

    @Test
    public void noSessionMeansNoHostChange() {
        FpsPolicy.HostPlan plan = FpsPolicy.plan(false, true, true, true, defaults());
        assertEquals(FpsPolicy.DEFAULT_OVERLAY_TICK_MS, plan.overlayTickIntervalMs);
        assertEquals(0L, plan.backgroundRequestIntervalMs);
        assertFalse(plan.dropDistantAnimation);
        assertTrue(plan.description.toLowerCase().contains("inactive"));
    }

    @Test
    public void theTickModuleThrottlesBackgroundRefresh() {
        FpsPolicy.ValueSource values = new FpsPolicy.ValueSource() {
            @Override
            public int integer(String key, int fallback) {
                return "tr_net_interval".equals(key) ? 120 : fallback;
            }

            @Override
            public boolean bool(String key, boolean fallback) {
                return "tr_coalesce_net".equals(key) || fallback;
            }
        };
        FpsPolicy.HostPlan plan = FpsPolicy.plan(true, false, false, true, values);
        assertEquals(120_000L, plan.backgroundRequestIntervalMs);
        assertTrue(plan.coalesceNetworkRequests);
    }

    @Test
    public void noOutputEverClaimsAFrameRateNumber() {
        // The launcher cannot measure the game's frame rate, so a digit followed by "fps" anywhere in
        // the plan would be the same dishonesty as promising a ping.
        FpsPolicy.HostPlan plan = FpsPolicy.plan(true, true, true, true, defaults());
        String text = plan.description.toLowerCase();
        assertFalse(text.contains("fps"));
        for (FpsModuleCatalog.Option option : FpsModuleCatalog.allOptions()) {
            assertFalse("option claims fps: " + option.key, option.description.toLowerCase().contains("fps"));
        }
    }

    @Test
    public void everyModuleDescriptionSaysItIsHostSide() {
        for (FpsModuleCatalog.Module module : FpsModuleCatalog.modules()) {
            String d = module.description.toLowerCase();
            boolean honest = d.contains("launcher") || d.contains("host");
            assertTrue(module.id + " description does not scope itself to host work", honest);
        }
    }

    @Test
    public void aDisabledPlanThrottlesNothing() {
        // The modules off (or no session) must leave every per-frame path exactly as it was: the
        // plan's intervals are 0 and the consumers run every frame.
        FpsPolicy.HostPlan plan = FpsPolicy.plan(false, false, false, false, defaults());
        assertEquals(0L, plan.overlayTickIntervalMs);
        assertEquals(0L, plan.backgroundRequestIntervalMs);
    }

    @Test
    public void anEnabledCullingModuleThrottlesTheOverlayTick() {
        // With the coalescing module on, the plan raises the overlay interval, which is what the
        // tick loop reads to batch its work.
        FpsPolicy.HostPlan plan = FpsPolicy.plan(true, false, true, false, defaults());
        assertTrue("an enabled coalescing module must raise the overlay interval",
                plan.overlayTickIntervalMs > 0);
    }

    @Test
    public void anEnabledTickModuleThrottlesBackgroundPolling() {
        FpsPolicy.HostPlan plan = FpsPolicy.plan(true, false, false, true, defaults());
        assertTrue("an enabled tick module must set a background interval",
                plan.backgroundRequestIntervalMs > 0);
    }
}
