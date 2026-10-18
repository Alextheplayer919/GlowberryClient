package org.chimeramc.client.ui.animation;

/**
 * The Launch tab's entrance: the order the screen's blocks arrive in and the timing of the blur
 * fade-in that sits behind them.
 *
 * <p>The spec asks the intro to play in "reading order" — the eye should be led from the logo, down
 * through the account row and the hero panel, into the dock — and the background blur to fade in
 * smoothly behind it. The order is a property of the screen, not of the animator, so it lives here as
 * a pure, unit-tested list rather than being hardcoded in the Activity's animation loop (where a
 * reordered layout would silently desync from it).
 *
 * <p>Timings are in milliseconds and are scaled by {@code DynamicAnim}'s global speed multiplier at
 * the call site, so the accessibility "animation speed" setting still applies.
 */
public final class LaunchIntro {

    private LaunchIntro() {
    }

    /** The blocks that arrive, in reading order: brand, account, hero, toggles, dock. */
    public enum Block {
        /** The account row at the top of the header. */
        ACCOUNT,
        /** The hero panel with the live character preview. */
        HERO,
        /** The three quick-toggle cards. */
        TOGGLES,
        /** The content/stats section. */
        CONTENT,
        /** The secondary dock (Mods / Cosmetics / wheel). */
        DOCK,
        /** The launch control itself, last so the eye ends on the primary action. */
        LAUNCH,
    }

    /** The blocks in reading order. */
    public static final Block[] ORDER = {
            Block.ACCOUNT,
            Block.HERO,
            Block.TOGGLES,
            Block.CONTENT,
            Block.DOCK,
            Block.LAUNCH,
    };

    /** The gap between successive blocks. */
    public static final long STEP_MS = 60L;

    /** The blur/backdrop fade, which runs under the whole entrance. */
    public static final long BLUR_FADE_MS = 320L;

    /** The arrival tween for one block. */
    public static final long BLOCK_FADE_MS = 260L;

    /** The delay before a block's arrival, from its position in {@link #ORDER}. */
    public static long delayFor(Block block) {
        for (int i = 0; i < ORDER.length; i++) {
            if (ORDER[i] == block) return i * STEP_MS;
        }
        return 0L;
    }

    /** The total entrance duration: the last block's delay plus its own tween. */
    public static long totalDurationMs() {
        return (ORDER.length - 1) * STEP_MS + BLOCK_FADE_MS;
    }

    /**
     * The alpha of the backdrop blur at a point in the entrance, 0..1.
     *
     * <p>A smooth ease-out so the blur settles rather than arriving linearly; a caller feeds the
     * elapsed time since the screen opened.
     */
    public static float blurAlphaAt(long elapsedMs) {
        if (elapsedMs <= 0) return 0f;
        if (elapsedMs >= BLUR_FADE_MS) return 1f;
        float t = elapsedMs / (float) BLUR_FADE_MS;
        // Ease-out cubic.
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    /**
     * The alpha of a block at a point in the entrance, 0..1.
     *
     * <p>0 before the block's turn, ramping to 1 over {@link #BLOCK_FADE_MS} after it. This is what
     * makes the order a reading order rather than everything appearing at once.
     */
    public static float blockAlphaAt(Block block, long elapsedMs) {
        long start = delayFor(block);
        if (elapsedMs <= start) return 0f;
        if (elapsedMs >= start + BLOCK_FADE_MS) return 1f;
        return (elapsedMs - start) / (float) BLOCK_FADE_MS;
    }
}
