package org.chimeramc.client.core.cosmetics;

import org.chimeramc.client.BuildConfig;
import org.chimeramc.client.core.crash.CrashReportStore;
import org.chimeramc.client.core.versions.GameVersion;
import org.chimeramc.client.core.versions.VersionManager;
import org.chimeramc.client.util.LauncherStorage;
import org.chimeramc.client.preloader.PreloaderInput;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * The in-app cosmetics diagnostics report.
 *
 * <p>The dev environment has no device and no adb, so the player has to be able to read — and share —
 * the state of the native render path from inside the app. Logcat is not available to the app without
 * {@code READ_LOGS}, so this class reads the native feed directly through {@link PreloaderInput} and
 * formats a plain-text report. Every field is either a real native reading or explicitly "unknown":
 * the report never invents a value.
 *
 * <p>The formatting half is pure and Android-free ({@link #format}) so it is unit-tested; only
 * {@link #readLive} touches Android.
 */
public final class CosmeticsDiagnosticsReport {

    private CosmeticsDiagnosticsReport() {
    }

    /** The raw inputs the report is formatted from. Plain fields so a test can set them directly. */
    public static final class Inputs {
        public String appVersion = "unknown";
        public String minecraftVersion = "unknown";
        public boolean nativeLoaded;
        /** The render slot the hook resolved to, or -1 when unknown. */
        public int renderSlot = -1;
        /** 1 = resolved by the render-diagnostic string scan, 0 = by configured index, -1 unknown. */
        public int resolvedBy = -1;
        /** {@code GlBackend}: 0 unknown, 1 GLES, 2 Vulkan. */
        public int glBackend = 0;
        public boolean eglContextCurrent;
        public boolean hookInstalled;
        /** The exact gate that refused the render hook (item 18): 0 live, 1 version gate,
         * 2 slot unresolved, 3 not executable, 4 hook() failed; -1 unknown/absent. */
        public int renderHookFailureReason = -1;
        /**
         * True while the deferred hook install is still waiting for the game runtime to come up
         * (armed but not yet run). Distinguishes "waiting" from "failed" in the report.
         */
        public boolean hookInstallPending;
        /** Render ticks seen this session, or -1 when the renderer has not run. */
        public int framesSeen = -1;
        /** Whether the cape's engine texture route is live this session (the cape's live route). */
        public boolean capeEngineRouteLive;
        /** Whether the engine image-loader pipeline observed a texture upload this session. */
        public boolean imagePipelineLive;
        public int lastError;
        /** Bytes of published render geometry, or -1 when none. */
        public int geometryBytes = -1;
        /** bit0 cape, bit1 hat, bit2 pet; -1 when the native library is absent. */
        public int pieceAtlasMask = -1;
        public int capeOverrides;
        public int textureOverrides;
        public String localKeyId = NativeCosmeticsBridge.LOCAL_PLAYER_ID;
        /** Whether the local-player position feed produced a reading this frame. */
        public boolean localPlayerLive;
        /** The effective hook rule's resolved fields (which slots the running build used). */
        public String rulesSummary = "";
        /** The rules file's declared revision, or -1 when unknown. */
        public int rulesRevision = -1;
        /** Extra rules that matched this version (>0 is where first-match selection lost fields). */
        public int rulesMergeConflicts = -1;
        /** Whether the effective rule carries a playerRenderVtableIndex. */
        public boolean rulesHaveRenderIndex;
        /** Whether the rules were delivered to the native library at all this session. */
        public boolean rulesLoadAttempted;
        /** How many copies of libpreloader.so are mapped; >1 means a split, empty registry. */
        public int preloaderModuleMappings = -1;
        /** The deferred hook-install state: 0 not armed, 1 waiting, 2 resolved. */
        public int hookInstallState;
        /** Frame ticks the install gate observed before deciding. */
        public int hookInstallFrameTicks;
        /** Camera anchor source: 1 engine view-projection, 0 first-person fallback, -1 unknown. */
        public int cameraAnchorSource = -1;
        /** The rules source actually in use ("bundled" / "remote"), for the report. */
        public String rulesSource = "bundled";
        /** Local player feed: {x, y, z, yaw, pitch} when live, else null. */
        public float[] localPlayerPosition;
        /** True when the engine captured a base-skin texture this session (the real-skin route). */
        public boolean skinCaptured;
        /** True when a game-skin provider is installed (the engine-hook seam is active). */
        public boolean skinFeedInstalled;
        /** Per-cosmetic asset audit lines (PNG size, geometry size, cube count), or null. */
        public java.util.List<CosmeticAssetAudit.Entry> cosmeticAssets;
        /** Faces the scene refused to build (a size mismatch), as one-line descriptions. */
        public java.util.List<String> skippedFaces;
        /** The most recent crash report text (from the crash_logs dir), or "" when none. */
        public String lastCrashReport = "";
        /** True when the crash report was truncated for display. */
        public boolean lastCrashTruncated;
        /** True when the report was generated while a world was loaded (not in a menu). */
        public boolean inWorld;
        /** Item 11: the render thread's smoothed frame interval in microseconds, or -1 unknown. */
        public int framePacingAverageUs = -1;
        /** Item 11: the worst frame interval in the recent window, or -1 unknown. */
        public int framePacingWorstUs = -1;
        /** Item 11: a recent frame was late (a hitch). */
        public boolean framePacingJittering;
        /** Item 11: the frame interval matched the display interval. */
        public boolean framePacingVsyncLocked;
        /** Item 11: whether a frame-pacing reading exists at all this session. */
        public boolean framePacingAvailable;
        /** True when a game session is running (the launcher's own session flag). */
        public boolean sessionActive;
        /** The HUD hook's "a HUD screen is open" reading, or false when the hook has not fired. */
        public boolean hudScreenOpen;
        /** Mod-menu build-step failures captured by the containment guard (formatted). */
        public String menuGuardFailures = "  none\n";
        public long timestampMs;
    }

    /** A one-shot reading of the live state, formatted into a plain-text report. */
    public static String readLive(Context context) {
        return format(readInputs(context));
    }

    /** Gathers the live inputs. Every native read is guarded so an absent library reads as unknown. */
    public static Inputs readInputs(Context context) {
        Inputs in = new Inputs();
        in.timestampMs = System.currentTimeMillis();
        in.appVersion = safeVersionName();
        in.minecraftVersion = selectedMinecraftVersion(context);

        int[] diag = PreloaderInput.readRenderDiagnostics();
        in.nativeLoaded = diag != null;
        if (diag != null) {
            in.renderSlot = diag[0];
            in.resolvedBy = diag[1];
            in.glBackend = diag[2];
            in.eglContextCurrent = diag[3] == 1;
            in.hookInstalled = diag[4] == 1;
            in.renderHookFailureReason = PreloaderInput.renderHookFailureReason();
            in.framesSeen = diag[5];
            in.lastError = diag[6];
            in.geometryBytes = diag[7];
            in.pieceAtlasMask = diag[8];
            if (diag.length >= 17) {
                in.localPlayerLive = diag[10] == 1;
                in.rulesRevision = diag[11];
                in.rulesMergeConflicts = diag[12];
                in.rulesHaveRenderIndex = diag[13] == 1;
                in.rulesLoadAttempted = diag[14] == 1;
                in.preloaderModuleMappings = diag[15];
            }
        }
        in.cameraAnchorSource = PreloaderInput.cameraAnchorSource();

        // Item 11: the render-thread frame-pacing reading, when the renderer has produced frames.
        try {
            int[] pacing = PreloaderInput.readFramePacing();
            if (pacing != null) {
                in.framePacingAvailable = true;
                in.framePacingAverageUs = pacing[1];
                in.framePacingWorstUs = pacing[2];
                in.framePacingJittering = (pacing[3] & 1) != 0;
                in.framePacingVsyncLocked = (pacing[3] & 2) != 0;
            }
        } catch (Throwable ignored) {
        }

        // The local-player feed, decoded from its raw float bits. Only present when live.
        int[] feed = PreloaderInput.readLocalPlayerFeed();
        if (feed != null && feed.length >= 6 && feed[0] == 1) {
            in.localPlayerPosition = new float[] {
                    Float.intBitsToFloat(feed[1]), Float.intBitsToFloat(feed[2]),
                    Float.intBitsToFloat(feed[3]), Float.intBitsToFloat(feed[4]),
                    Float.intBitsToFloat(feed[5])};
        }

        try {
            in.hookInstallState = org.chimeramc.client.core.mods.ModManager.gameHookInstallState();
            in.hookInstallFrameTicks =
                    org.chimeramc.client.core.mods.ModManager.gameHookInstallFrameTicks();
            in.rulesSummary = org.chimeramc.client.core.mods.ModManager.gameHookRulesSummary();
        } catch (Throwable ignored) {
        }
        try {
            in.rulesSource = org.chimeramc.client.preloader.PreloaderSignatureRulesManager
                    .getRulesSource(context);
        } catch (Throwable ignored) {
        }
        in.capeEngineRouteLive = NativeCosmeticsFeed.isCapeEngineRouteLive();
        in.imagePipelineLive = NativeCosmeticsBridge.isImagePipelineLive();
        try {
            in.hookInstallPending =
                    org.chimeramc.client.core.mods.ModManager.isGameHookInstallPending();
        } catch (Throwable ignored) {
        }
        try {
            in.sessionActive =
                    org.chimeramc.client.settings.LowLatencyNetworkManager.isGameSessionActive();
        } catch (Throwable ignored) {
        }
        try {
            in.hudScreenOpen = PreloaderInput.isHudScreenOpen();
        } catch (Throwable ignored) {
        }
        // "In a world" is the HUD hook having reported a world at least once and no HUD screen being
        // open right now. Before the hook fires there is no reading at all, so the report says so
        // rather than claiming a menu.
        in.inWorld = in.sessionActive && !in.hudScreenOpen;
        try {
            in.skinCaptured = PreloaderInput.nativeHasCapturedSkin();
        } catch (Throwable ignored) {
        }
        in.skinFeedInstalled = GameSkinSource.hasProvider();

        // The most recent crash report, so a device crash trace can be copied/shared from inside the
        // app (logcat is not app-readable without READ_LOGS).
        try {
            CrashReportStore.Report report =
                    CrashReportStore.readDir(LauncherStorage.getCrashLogsDir(context));
            if (report.isPresent()) {
                in.lastCrashReport = report.text;
                in.lastCrashTruncated = report.truncated;
            }
        } catch (Throwable ignored) {
        }
        // Mod-menu build failures the containment guard caught (item 1 of the v3 update).
        in.menuGuardFailures = org.chimeramc.client.core.mods.inbuilt.overlay.ModMenuGuard.format();

        // Per-cosmetic asset audit: the real PNG size, the geometry's declared size and the cube
        // count, so a size mismatch (which used to crash the preview) is visible without a device.
        try {
            if (context != null) {
                in.cosmeticAssets = CosmeticAssetAudit.scan(
                        path -> context.getAssets().open(path));
            }
        } catch (Throwable ignored) {
        }
        // Faces the scene refused to build, with the reason.
        java.util.List<PreviewSceneDiagnostics.Entry> skipped = PreviewSceneDiagnostics.snapshot();
        if (!skipped.isEmpty()) {
            in.skippedFaces = new java.util.ArrayList<>();
            for (PreviewSceneDiagnostics.Entry e : skipped) in.skippedFaces.add(e.describe());
        }

        int[] stats = NativeCosmeticsBridge.stats();
        if (stats != null && stats.length >= 4) {
            in.capeOverrides = stats[2];
            in.textureOverrides = stats[3];
        }
        return in;
    }

    private static String safeVersionName() {
        try {
            String name = BuildConfig.VERSION_NAME;
            return name == null || name.isEmpty() ? "unknown" : name;
        } catch (Throwable t) {
            return "unknown";
        }
    }

    private static String selectedMinecraftVersion(Context context) {
        try {
            if (context == null) return "none selected";
            VersionManager manager = VersionManager.get(context);
            if (manager == null) return "none selected";
            GameVersion version = manager.getSelectedVersion();
            if (version == null || version.versionCode == null || version.versionCode.isEmpty()) {
                return "none selected";
            }
            return version.versionCode;
        } catch (Throwable t) {
            return "none selected";
        }
    }

    /**
     * Formats the report. Pure: no Android types, so a JVM test pins the exact lines and their
     * truthful wording.
     */
    public static String format(Inputs in) {
        Inputs v = in == null ? new Inputs() : in;
        StringBuilder b = new StringBuilder(1024);
        b.append("GlowberryClient — Cosmetics diagnostics\n");
        b.append("Generated: ").append(timestamp(v.timestampMs)).append('\n');
        b.append("Generated inside a world: ").append(yesNo(v.inWorld)).append('\n');
        b.append("App version: ").append(v.appVersion).append('\n');
        b.append("Minecraft version: ").append(v.minecraftVersion).append('\n');
        b.append('\n');

        b.append("FIRST FAILING STAGE: ").append(firstFailingStage(v)).append('\n');
        b.append('\n');

        b.append("Native library loaded: ").append(yesNo(v.nativeLoaded)).append('\n');
        b.append("Rules source: ").append(v.rulesSource == null ? "bundled" : v.rulesSource)
                .append(" (revision ").append(countText(v.rulesRevision)).append(")\n");
        b.append("Rules delivered to native: ").append(yesNo(v.rulesLoadAttempted)).append('\n');
        b.append("Effective rule: ").append(ruleSummaryText(v)).append('\n');
        b.append("Rules matching this version (extra): ").append(countText(v.rulesMergeConflicts))
                .append('\n');
        b.append("Rule has render slot: ").append(yesNo(v.rulesHaveRenderIndex)).append('\n');
        b.append("Hook install state: ").append(installStateText(v)).append('\n');
        b.append("libpreloader.so copies mapped: ").append(mappingText(v.preloaderModuleMappings))
                .append('\n');
        b.append("Player-render slot: ").append(slotText(v)).append('\n');
        b.append("Hook installed: ").append(hookText(v)).append('\n');
        b.append("Hook refusal reason: ").append(hookRefusalText(v)).append('\n');
        b.append("Frames seen: ").append(countText(v.framesSeen)).append('\n');
        b.append("Local player feed: ").append(localPlayerText(v)).append('\n');
        b.append("Camera anchor source: ").append(cameraText(v)).append('\n');
        b.append("Frame pacing: ").append(framePacingText(v)).append('\n');
        b.append("Cape engine texture route: ").append(yesNo(v.capeEngineRouteLive)).append('\n');
        b.append("Engine image pipeline live: ").append(yesNo(v.imagePipelineLive)).append('\n');
        b.append("GL backend: ").append(backendText(v.glBackend)).append('\n');
        b.append("EGL context current: ").append(yesNo(v.eglContextCurrent)).append('\n');
        b.append("Last GL error: ").append(errorText(v.lastError)).append('\n');
        b.append('\n');

        b.append("Published geometry bytes: ").append(countText(v.geometryBytes)).append('\n');
        b.append("Piece atlases: ").append(pieceMaskText(v.pieceAtlasMask)).append('\n');
        b.append("Cape overrides: ").append(v.capeOverrides).append('\n');
        b.append("Texture overrides: ").append(v.textureOverrides).append('\n');
        b.append("Local cosmetics published under key: \"").append(v.localKeyId).append("\"\n");
        b.append("Game-skin feed installed: ").append(yesNo(v.skinFeedInstalled)).append('\n');
        b.append("Base skin captured from game: ").append(yesNo(v.skinCaptured)).append('\n');
        b.append('\n');

        b.append("Cosmetic assets (PNG size / geometry size / cubes):\n");
        b.append(CosmeticAssetAudit.format(v.cosmeticAssets));
        b.append('\n');

        b.append("Faces skipped (atlas size mismatch): ");
        if (v.skippedFaces == null || v.skippedFaces.isEmpty()) {
            b.append("none\n");
        } else {
            b.append(v.skippedFaces.size()).append('\n');
            for (String s : v.skippedFaces) b.append("  ").append(s).append('\n');
        }
        b.append('\n');

        b.append("Status: ").append(statusText(v)).append('\n');
        b.append('\n');

        b.append("Mod-menu build failures (containment guard):\n");
        b.append(v.menuGuardFailures == null ? "  none\n" : v.menuGuardFailures);
        b.append('\n');

        b.append("Last crash report: ");
        if (v.lastCrashReport == null || v.lastCrashReport.isEmpty()) {
            b.append("none\n");
        } else {
            b.append(v.lastCrashTruncated ? "(truncated)\n" : "\n");
            b.append(v.lastCrashReport);
            if (!v.lastCrashReport.endsWith("\n")) b.append('\n');
        }
        b.append('\n');
        b.append("Note: whether cosmetics actually appear in game is unverified on device. This "
                + "report only describes the state of the native render path.\n");
        return b.toString();
    }

    private static String timestamp(long ms) {
        long value = ms > 0 ? ms : System.currentTimeMillis();
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date(value));
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    /**
     * The hook-install line. "waiting for the render context" is distinct from a plain "no": the
     * install is deferred to the game's frame tick, so an armed-but-not-yet-run state is expected
     * briefly and must not read as a failure.
     */
    private static String hookText(Inputs v) {
        if (v.hookInstalled) return "yes";
        return v.hookInstallPending ? "waiting for the render context" : "no";
    }

    /** Maps the render hook's refusal reason (item 18) to the exact gate that refused it. */
    private static String hookRefusalText(Inputs v) {
        if (v.hookInstalled) return "n/a (hook live)";
        switch (v.renderHookFailureReason) {
            case 0:
                return "not refused (still pending or not yet resolved)";
            case 1:
                return "build outside the verified version range";
            case 2:
                return "LivePlayerRenderer render slot unresolved";
            case 3:
                return "resolved slot is not executable code";
            case 4:
                return "hook() install call failed";
            case 5:
                return "the matching rule's probe for the render seam did not resolve";
            default:
                return "unknown";
        }
    }

    private static String countText(int value) {
        return value < 0 ? "unknown" : Integer.toString(value);
    }

    private static String slotText(Inputs v) {
        if (v.renderSlot < 0) return "unknown (hook not installed)";
        String how;
        if (v.resolvedBy == 1) {
            how = "resolved by render-diagnostic string scan";
        } else if (v.resolvedBy == 0) {
            how = "resolved by configured/default index";
        } else {
            how = "resolution method unknown";
        }
        return v.renderSlot + " (" + how + ")";
    }

    private static String backendText(int backend) {
        return RenderBackend.label(RenderBackend.fromNativeCode(backend));
    }

    private static String errorText(int error) {
        if (error < 0) return "unknown";
        return error == 0 ? "none" : "0x" + Integer.toHexString(error);
    }

    private static String pieceMaskText(int mask) {
        if (mask < 0) return "unknown";
        StringBuilder b = new StringBuilder();
        if ((mask & 1) != 0) b.append("cape ");
        if ((mask & 2) != 0) b.append("hat ");
        if ((mask & 4) != 0) b.append("pet ");
        return b.length() == 0 ? "none published" : b.toString().trim();
    }

    private static String ruleSummaryText(Inputs v) {
        if (v.rulesSummary == null || v.rulesSummary.isEmpty()) return "unknown";
        return v.rulesSummary;
    }

    private static String installStateText(Inputs v) {
        switch (v.hookInstallState) {
            case 0: return "not armed";
            case 1: return "armed, waiting for the render context (" + v.hookInstallFrameTicks
                    + " frame tick(s))";
            case 2: return "resolved after " + v.hookInstallFrameTicks + " frame tick(s)";
            default: return "unknown";
        }
    }

    private static String mappingText(int count) {
        if (count < 0) return "unknown";
        if (count > 1) return count + " (SPLIT: more than one copy has an empty registry)";
        return Integer.toString(count);
    }

    private static String localPlayerText(Inputs v) {
        if (!v.localPlayerLive) return "no live local player";
        if (v.localPlayerPosition == null || v.localPlayerPosition.length < 5) return "live (values unavailable)";
        return String.format(Locale.US, "live x=%.2f y=%.2f z=%.2f yaw=%.2f pitch=%.2f",
                v.localPlayerPosition[0], v.localPlayerPosition[1], v.localPlayerPosition[2],
                v.localPlayerPosition[3], v.localPlayerPosition[4]);
    }

    private static String cameraText(Inputs v) {
        if (v.cameraAnchorSource < 0) return "unknown";
        return v.cameraAnchorSource == 1
                ? "engine view-projection"
                : "first-person fallback (engine camera did not resolve)";
    }

    /**
     * The render-thread frame-pacing line (item 11). "no reading yet" is distinct from a bad
     * interval: the renderer has to produce two frames before any interval exists. The verdict
     * never claims a lock without a reading, and an unknown display rate is never called locked.
     */
    private static String framePacingText(Inputs v) {
        if (!v.framePacingAvailable || v.framePacingAverageUs < 0) return "no reading yet";
        StringBuilder s = new StringBuilder();
        s.append(formatMicros(v.framePacingAverageUs)).append(" average");
        if (v.framePacingWorstUs >= 0) {
            s.append(", ").append(formatMicros(v.framePacingWorstUs)).append(" worst");
        }
        if (v.framePacingJittering) {
            s.append(" — a recent frame was late (hitching)");
        } else if (v.framePacingVsyncLocked) {
            s.append(" — vsync-locked");
        } else {
            s.append(" — steady, not vsync-locked");
        }
        return s.toString();
    }

    /** Microseconds to a short "16.7 ms" form; a sub-millisecond value is shown in microseconds. */
    private static String formatMicros(int micros) {
        if (micros < 1000) return micros + " us";
        return String.format(Locale.US, "%.1f ms", micros / 1000.0);
    }

    /** The honest status line, matching the rule the Cosmetics panel uses. */
    static String statusText(Inputs v) {
        if (!v.nativeLoaded) {
            return "native library not loaded — cosmetics are preview-only.";
        }
        if (v.capeEngineRouteLive) {
            return "cape active through the engine texture loader"
                    + (v.imagePipelineLive ? " (image pipeline live)" : "")
                    + (v.hookInstalled ? "; accessories and pets reach the game through the native "
                            + "player-render hook." : "; accessories and pets need the native "
                            + "player-render hook, which is not live this session.");
        }
        if (v.hookInstalled) {
            return "native player-render hook live; the cape's engine texture route has not "
                    + "observed a loaded cape texture yet this session.";
        }
        return "render hook not installed and no cape texture observed — cosmetics are preview-only.";
    }

    /**
     * Names the first stage that did not complete, in the order the native path is built.
     *
     * <p>A bare "Status" line says the end result but not which link broke; with the install
     * deferred to the game's frame tick, "not installed" could mean the library never loaded, the
     * rules never arrived, no rule matched the version, or the runtime never came up. This walks the
     * same order the preloader does and stops at the first failure, so a device report points at one
     * step instead of leaving the reader to guess. Every stage is a real reading; nothing is
     * inferred.
     */
    static String firstFailingStage(Inputs v) {
        if (!v.nativeLoaded) {
            return "0. native library — libpreloader.so is not loaded, so nothing below ran.";
        }
        if (v.rulesRevision <= 0) {
            return "1. rules file — the delivered rules declared revision 0 (or none was delivered);"
                    + " the file did not reach the native side or is unreadable.";
        }
        if (!v.rulesLoadAttempted) {
            return "2. rules load — the hook install never ran, so the rules file was never read"
                    + " (the install is deferred to the game's frame tick; see the install state).";
        }
        if (!v.rulesHaveRenderIndex) {
            return "3. rule match — no matching rule for Minecraft " + v.minecraftVersion
                    + " carried a playerRenderVtableIndex, so the render slot could not resolve.";
        }
        if (v.hookInstallState == 0) {
            return "4. install armed — the deferred install was never armed; the game activity did"
                    + " not reach the pump.";
        }
        if (v.hookInstallState == 1) {
            return "4. install armed — waiting for the game runtime (" + v.hookInstallFrameTicks
                    + " frame tick(s)); if this persists the runtime never came up.";
        }
        if (!v.hookInstalled) {
            return "5. render slot — the install ran but the render hook is not live; the slot did"
                    + " not resolve to an executable address on this build.";
        }
        if (v.capeEngineRouteLive) {
            return "none — the cape reaches the game through the engine texture loader this session.";
        }
        return "none blocking — the render hook is live; the cape's engine texture route fires once"
                + " the engine has loaded your cape texture this session.";
    }
}
