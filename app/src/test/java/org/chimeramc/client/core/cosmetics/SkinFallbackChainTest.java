package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the skin fallback chain: manual import > live capture > cached capture > (opt-in) account
 * lookup > default model > placeholder. The account rung must be unreachable unless the opt-in is
 * on, so no request is ever made without consent.
 */
public class SkinFallbackChainTest {

    @Test
    public void manualImportAlwaysWins() {
        assertEquals(SkinFallbackChain.Rung.IMPORTED,
                SkinFallbackChain.pick(true, true, true, true, true, true));
    }

    @Test
    public void liveCaptureBeatsCache() {
        assertEquals(SkinFallbackChain.Rung.LIVE_CAPTURE,
                SkinFallbackChain.pick(false, true, true, true, true, true));
    }

    @Test
    public void cachedCaptureIsUsedBeforeTheDefault() {
        assertEquals(SkinFallbackChain.Rung.CACHED_CAPTURE,
                SkinFallbackChain.pick(false, false, true, true, true, true));
    }

    @Test
    public void theAccountRungRequiresTheOptIn() {
        // Off: the account rung is skipped even when an account lookup could succeed.
        assertEquals(SkinFallbackChain.Rung.DEFAULT_MODEL,
                SkinFallbackChain.pick(false, false, false, false, true, true));
        // On: it is used.
        assertEquals(SkinFallbackChain.Rung.ACCOUNT_LOOKUP,
                SkinFallbackChain.pick(false, false, false, true, true, true));
    }

    @Test
    public void theDefaultModelIsTheLastResortBeforeThePlaceholder() {
        assertEquals(SkinFallbackChain.Rung.DEFAULT_MODEL,
                SkinFallbackChain.pick(false, false, false, false, false, true));
        assertEquals(SkinFallbackChain.Rung.PLACEHOLDER,
                SkinFallbackChain.pick(false, false, false, false, false, false));
    }

    @Test
    public void everyRungHasALabel() {
        for (SkinFallbackChain.Rung r : SkinFallbackChain.Rung.values()) {
            assertFalse(SkinFallbackChain.label(r).isEmpty());
        }
    }

    @Test
    public void theDefaultModelIsPickedByAStableHash() {
        boolean a = SkinFallbackChain.defaultIsSlim("abc");
        boolean b = SkinFallbackChain.defaultIsSlim("abc");
        assertEquals(a, b);
        assertFalse("no identity must default to wide (Steve)",
                SkinFallbackChain.defaultIsSlim(null));
    }
}
