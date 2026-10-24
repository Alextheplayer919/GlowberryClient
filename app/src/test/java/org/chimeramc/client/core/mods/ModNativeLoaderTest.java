package org.chimeramc.client.core.mods;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class ModNativeLoaderTest {
    @Test
    public void exactPatternMatchesOnlySameVersion() {
        assertTrue(ModNativeLoader.matchesMinecraftVersionPattern("1.26.20", "1.26.20"));
        assertFalse(ModNativeLoader.matchesMinecraftVersionPattern("1.26.20", "1.26.21"));
    }

    @Test
    public void wildcardPatternMatchesPrefix() {
        assertTrue(ModNativeLoader.matchesMinecraftVersionPattern("1.26.2*", "1.26.20"));
        assertTrue(ModNativeLoader.matchesMinecraftVersionPattern("1.26.2*", "1.26.21"));
        assertTrue(ModNativeLoader.matchesMinecraftVersionPattern("1.26.2*", "1.26.22"));
        assertFalse(ModNativeLoader.matchesMinecraftVersionPattern("1.26.2*", "1.26.30"));
    }

    @Test
    public void anyPatternCanMatch() {
        assertTrue(ModNativeLoader.isCompatibleWithMinecraftVersion(
                Arrays.asList("1.25.*", "1.26.2*", "1.27.0"),
                "1.26.21"));
    }

    @Test
    public void missingOrEmptyPatternsAllowAllVersions() {
        assertTrue(ModNativeLoader.isCompatibleWithMinecraftVersion((java.util.List<String>) null, "1.26.21"));
        assertTrue(ModNativeLoader.isCompatibleWithMinecraftVersion(Collections.emptyList(), "1.26.21"));
        assertTrue(ModNativeLoader.isCompatibleWithMinecraftVersion(Arrays.asList("", "   "), "1.26.21"));
    }

    @Test
    public void incompatiblePatternRejectsVersion() {
        assertFalse(ModNativeLoader.isCompatibleWithMinecraftVersion(
                Arrays.asList("1.25.*", "1.26.20"),
                "1.26.21"));
    }

    @Test
    public void unpinnedModsAlwaysLoad() {
        Mod unpinned = new Mod("m", "libm.so", "libm.so", "M", true, 0);
        assertTrue(ModNativeLoader.mayLoad(unpinned));
    }

    @Test
    public void pinnedVerifiedModLoads() {
        String pin = "ab".repeat(32);
        Mod verified = new Mod("m", "libm.so", "libm.so", "M", java.util.Collections.emptyList(),
                null, null, null, null, null,
                "/mods/m", "/mods/m/config", false, 0, pin, true, true, 0);
        assertTrue(ModNativeLoader.mayLoad(verified));
    }

    @Test
    public void pinnedButUnverifiedModIsRefused() {
        String pin = "ab".repeat(32);
        Mod tampered = new Mod("m", "libm.so", "libm.so", "M", java.util.Collections.emptyList(),
                null, null, null, null, null,
                "/mods/m", "/mods/m/config", false, 0, pin, false, true, 0);
        assertFalse(ModNativeLoader.mayLoad(tampered));
    }
}
