package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

/**
 * Pins the game-skin seam: with no provider the read is null (fail-closed), a throwing provider is
 * swallowed, and the resolution rule picks the right rung. Pure JVM — no device.
 */
public class GameSkinSourceTest {

    @After
    public void tearDown() {
        GameSkinSource.clearProvider();
    }

    @Test
    public void withNoProviderTheReadIsNull() {
        GameSkinSource.clearProvider();
        assertFalse(GameSkinSource.hasProvider());
        assertNull(GameSkinSource.read(null));
    }

    @Test
    public void anInstalledProviderIsUsed() {
        PlayerSkinProvider.SkinBitmap skin = new PlayerSkinProvider.SkinBitmap(
                null, false, false, "from game");
        GameSkinSource.setProvider(context -> skin);
        assertTrue(GameSkinSource.hasProvider());
        assertEquals(skin, GameSkinSource.read(null));
    }

    @Test
    public void aThrowingProviderReadsAsNoSkin() {
        GameSkinSource.setProvider(context -> {
            throw new RuntimeException("native read failed");
        });
        // Fail-closed: a bad read is "no skin", never a crash and never a fabricated one.
        assertNull(GameSkinSource.read(null));
    }

    @Test
    public void aNullReturningProviderReadsAsNoSkin() {
        GameSkinSource.setProvider(context -> null);
        assertNull(GameSkinSource.read(null));
    }

    @Test
    public void theResolutionOrderPrefersTheGameSkin() {
        assertEquals(SkinResolution.Source.GAME,
                SkinResolution.pick(true, true, true, true, true));
        assertEquals(SkinResolution.Source.IMPORTED,
                SkinResolution.pick(false, true, true, true, true));
        assertEquals(SkinResolution.Source.ACCOUNT,
                SkinResolution.pick(false, false, true, true, true));
        assertEquals(SkinResolution.Source.APPLIED_PACK,
                SkinResolution.pick(false, false, false, true, true));
        assertEquals(SkinResolution.Source.STEVE,
                SkinResolution.pick(false, false, false, false, true));
        assertEquals(SkinResolution.Source.PLACEHOLDER,
                SkinResolution.pick(false, false, false, false, false));
    }

    @Test
    public void everySourceHasALabel() {
        for (SkinResolution.Source s : SkinResolution.Source.values()) {
            assertFalse(SkinResolution.label(s).isEmpty());
        }
    }
}
