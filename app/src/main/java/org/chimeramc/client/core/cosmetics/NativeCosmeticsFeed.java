package org.chimeramc.client.core.cosmetics;

import org.chimeramc.client.preloader.PreloaderInput;

/**
 * Reports whether the native player-render path is available this session.
 *
 * <p>The cosmetics architecture is native-only: there is no resource pack. A cosmetic reaches the
 * game only through a hook on the game's own player renderer, and that hook has several failure
 * modes (an unresolved render slot, a build whose renderer never ran, a Vulkan-only surface), so the
 * UI has to say which state is actually in effect rather than implying the path always works. This
 * class is that one probe: it reads the preloader's render feed and classifies the result.
 *
 * <p><b>Fail-closed.</b> A missing native library, an unresolved render slot, or a build whose
 * renderer never ran all read as {@link Mode#UNAVAILABLE}. The launcher never claims a native path it
 * does not have; when it is unavailable cosmetics show in the preview only.
 *
 * <p>Android-free apart from the {@link PreloaderInput} call itself, which is safe to call from a
 * plain JVM test because it catches {@link UnsatisfiedLinkError} and returns "unavailable".
 */
public final class NativeCosmeticsFeed {

    /** Which cosmetics route is live right now. */
    public enum Mode {
        /**
         * The render hook is installed and the game's player renderer has run, so native cape/pet
         * motion can be driven from a real render tick.
         */
        NATIVE_RENDER,
        /**
         * No live render hook this session (an unresolved slot, an absent library, or a renderer
         * that has not run), so nothing reaches the game.
         */
        UNAVAILABLE,
    }

    /** A one-shot reading of the native feed. */
    public static final class Status {
        public final Mode mode;
        /** Render ticks seen this session, or 0 when the feed is unavailable. */
        public final int renderTick;
        /** Player models drawn in the most recent frame; &gt; 1 means non-local players too. */
        public final int callsThisFrame;
        /** Milliseconds since the last render call, or -1 when unavailable. */
        public final int msSinceLastRender;

        Status(Mode mode, int renderTick, int callsThisFrame, int msSinceLastRender) {
            this.mode = mode;
            this.renderTick = renderTick;
            this.callsThisFrame = callsThisFrame;
            this.msSinceLastRender = msSinceLastRender;
        }

        public boolean isNative() {
            return mode == Mode.NATIVE_RENDER;
        }

        /**
         * Whether the hook has been seen covering more than the local player.
         *
         * <p>The renderer runs once per rendered player, so a frame with two or more calls means
         * the hook fires for other players' models as well.
         */
        public boolean coversOtherPlayers() {
            return callsThisFrame > 1;
        }
    }

    private NativeCosmeticsFeed() {
    }

    /**
     * Reads the native render feed and classifies it.
     *
     * @return a status that is never null; an unavailable feed reads as {@link Mode#UNAVAILABLE}.
     */
    public static Status read() {
        int[] stats = PreloaderInput.readPlayerRenderStats();
        if (stats == null) {
            return new Status(Mode.UNAVAILABLE, 0, 0, -1);
        }
        Mode mode = PreloaderInput.isPlayerRenderHookLive() ? Mode.NATIVE_RENDER : Mode.UNAVAILABLE;
        return new Status(mode, stats[0], stats[1], stats[3]);
    }

    /** Convenience: true when the native render path is live. */
    public static boolean isNativeRenderLive() {
        return read().isNative();
    }

    /**
     * Whether the cape's engine texture route is live this session.
     *
     * <p>The cape reaches the game as a texture swap at the engine's image loader, which does not
     * need a GL renderer and so survives a Vulkan-only build. The route is live once the engine has
     * loaded the local player's cape once (so there is a capture to key on) and the launcher has
     * registered a substitution for it. Both halves are required: a capture with no substitution
     * changes nothing, and a substitution with no capture has nothing to match.
     *
     * <p>Fail-closed: no native library reads as false.
     */
    public static boolean isCapeEngineRouteLive() {
        try {
            return PreloaderInput.hasCapturedCape() && PreloaderInput.capeSubstitutionCount() > 0;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * A short human-readable line for the cosmetics status screen.
     *
     * <p>Never contains a number that reads as a performance claim; the render tick is a count,
     * not a frame rate, and the launcher cannot measure the game's frame rate.
     */
    public static String describe(Status status) {
        if (status == null || !status.isNative()) {
            return "Native renderer: not active this session — cosmetics show in this preview only.";
        }
        if (status.coversOtherPlayers()) {
            return "Native renderer: active (" + status.renderTick + " ticks, "
                    + status.callsThisFrame + " models/frame) — capes can render on other players "
                    + "too.";
        }
        return "Native renderer: active (" + status.renderTick + " ticks) — cape and pet motion "
                + "is driven by the game's own render tick.";
    }

    /**
     * A one-line description of the native *image pipeline* route (the engine-owned
     * texture-substitution seam).
     *
     * <p>This is the seam that replaces a loaded cape texture with the engine's own loader, so a
     * substitution is live only when the init probe verified the loader address AND the pipeline
     * hook has run this session. "Not active" is information, not a failure. Never fabricates a
     * claim: without the verified loader, the report says the pipeline is not installed.
     */
    public static String describeImagePipeline() {
        if (!NativeCosmeticsBridge.isImagePathVerified()) {
            return "Native image pipeline: not installed this session — the engine image loader "
                    + "was not verified (or the library is absent), so cape textures load vanilla.";
        }
        int[] stats = NativeCosmeticsBridge.imagePipelineStats();
        if (!NativeCosmeticsBridge.isImagePipelineLive()) {
            return "Native image pipeline: loader verified but no texture upload observed yet — "
                    + "substitutions will fire on the next skin/cape bind.";
        }
        long substitutions = stats != null && stats.length >= 2 ? stats[1] : 0L;
        long overrides = stats != null && stats.length >= 3 ? stats[2] : 0L;
        return "Native image pipeline: active (" + substitutions + " substitution(s), "
                + overrides + " rule(s) registered) — loaded cape textures are replaced by the "
                + "engine's own loader.";
    }
}
