package org.chimeramc.client.ui.animation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the Launch tab's reading-order entrance: the block order, the staggered delays and the blur
 * fade curve. Pure JVM — no device.
 */
public class LaunchIntroTest {

    @Test
    public void theOrderIsTopToBottomAndEndsOnTheLaunchAction() {
        assertEquals(LaunchIntro.Block.ACCOUNT, LaunchIntro.ORDER[0]);
        assertEquals(LaunchIntro.Block.LAUNCH, LaunchIntro.ORDER[LaunchIntro.ORDER.length - 1]);
    }

    @Test
    public void theDelaysIncreaseInReadingOrder() {
        long previous = -1;
        for (LaunchIntro.Block block : LaunchIntro.ORDER) {
            long delay = LaunchIntro.delayFor(block);
            assertTrue("delay for " + block + " should be after the previous block",
                    delay > previous);
            previous = delay;
        }
        assertEquals(0L, LaunchIntro.delayFor(LaunchIntro.Block.ACCOUNT));
    }

    @Test
    public void theTotalDurationCoversTheLastBlock() {
        assertEquals(LaunchIntro.delayFor(LaunchIntro.Block.LAUNCH) + LaunchIntro.BLOCK_FADE_MS,
                LaunchIntro.totalDurationMs());
    }

    @Test
    public void theBlurFadesInSmoothly() {
        assertEquals(0f, LaunchIntro.blurAlphaAt(0), 0.001f);
        assertEquals(1f, LaunchIntro.blurAlphaAt(LaunchIntro.BLUR_FADE_MS), 0.001f);
        assertEquals(1f, LaunchIntro.blurAlphaAt(LaunchIntro.BLUR_FADE_MS * 3), 0.001f);
        float mid = LaunchIntro.blurAlphaAt(LaunchIntro.BLUR_FADE_MS / 2);
        assertTrue("mid-fade should be partway", mid > 0f && mid < 1f);
        // Ease-out: past the halfway point it should already be more than half.
        assertTrue("ease-out should be ahead of linear at the midpoint", mid > 0.5f);
    }

    @Test
    public void aBlockIsInvisibleBeforeItsTurnAndFullAfterItsTween() {
        LaunchIntro.Block last = LaunchIntro.Block.LAUNCH;
        assertEquals(0f, LaunchIntro.blockAlphaAt(last, 0), 0.001f);
        assertEquals(1f, LaunchIntro.blockAlphaAt(last,
                LaunchIntro.delayFor(last) + LaunchIntro.BLOCK_FADE_MS), 0.001f);
        assertEquals(0f, LaunchIntro.blockAlphaAt(last, LaunchIntro.delayFor(last)), 0.001f);
    }

    @Test
    public void blocksArriveOneAfterAnotherNotAllAtOnce() {
        // At the hero's delay the hero is still at 0 while the account is already partway in, so the
        // blocks are staggered rather than all appearing at once.
        long atHero = LaunchIntro.delayFor(LaunchIntro.Block.HERO);
        assertEquals(0f, LaunchIntro.blockAlphaAt(LaunchIntro.Block.HERO, atHero), 0.001f);
        assertTrue(LaunchIntro.blockAlphaAt(LaunchIntro.Block.ACCOUNT, atHero) > 0f);
        // A later block is still invisible while the account has fully arrived.
        assertEquals(0f, LaunchIntro.blockAlphaAt(LaunchIntro.Block.LAUNCH, atHero), 0.001f);
    }
}
