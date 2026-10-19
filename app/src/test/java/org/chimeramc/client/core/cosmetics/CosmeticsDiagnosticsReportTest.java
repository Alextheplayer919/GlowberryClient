package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the cosmetics diagnostics report's formatting and — more importantly — its honesty.
 *
 * <p>The report is what a player reads (and sends) to describe the native render path when there is
 * no adb, so a wrong wording is a wrong diagnosis. These tests check that each field is rendered
 * truthfully and that the status line never claims a draw that did not happen.
 */
public class CosmeticsDiagnosticsReportTest {

    private static CosmeticsDiagnosticsReport.Inputs fullInputs() {
        CosmeticsDiagnosticsReport.Inputs in = new CosmeticsDiagnosticsReport.Inputs();
        in.appVersion = "v1.2.3";
        in.minecraftVersion = "1.26.33.1";
        in.nativeLoaded = true;
        in.renderSlot = 17;
        in.resolvedBy = 1;
        in.glBackend = 1;
        in.eglContextCurrent = true;
        in.hookInstalled = true;
        in.framesSeen = 120;
        in.capeEngineRouteLive = true;
        in.imagePipelineLive = true;
        in.lastError = 0;
        in.geometryBytes = 2048;
        in.pieceAtlasMask = 0b011;
        in.capeOverrides = 1;
        in.textureOverrides = 2;
        in.timestampMs = 0;
        in.localPlayerLive = true;
        in.localPlayerPosition = new float[] {10.5f, 64.0f, -3.25f, 90.0f, 12.5f};
        in.rulesSource = "bundled";
        in.rulesRevision = 6;
        in.rulesMergeConflicts = 1;
        in.rulesHaveRenderIndex = true;
        in.rulesLoadAttempted = true;
        in.preloaderModuleMappings = 1;
        in.hookInstallState = 2;
        in.hookInstallFrameTicks = 3;
        in.cameraAnchorSource = 1;
        in.rulesSummary = "pauseMenuDtorSig,playerRenderVtableIndex=17";
        return in;
    }

    @Test
    public void theSkinSeamLineNamesTheStageAndTheCounters() {
        // Item 3: a "skin not visible" report must say which stage the skin path reached, and the
        // observed stage must state that the payload itself is still unproven.
        CosmeticsDiagnosticsReport.Inputs in = fullInputs();
        in.skinSeamStage = 2;
        in.skinJoinCalls = 4;
        in.skinTextureCalls = 12;
        in.skinSeamNote = "joins observed; the payload's skin fields are not readable";
        String report = CosmeticsDiagnosticsReport.format(in);
        assertTrue(report.contains("Skin path seam: joins observed"));
        assertTrue(report.contains("joins=4"));
        assertTrue(report.contains("textures=12"));
    }

    @Test
    public void anAbsentNativeLibraryNeverClaimsTheSeamIsMissing() {
        // -1 means "no reading", which must not be rendered as a confident "hook not installed".
        CosmeticsDiagnosticsReport.Inputs in = fullInputs();
        in.skinSeamStage = -1;
        String report = CosmeticsDiagnosticsReport.format(in);
        assertTrue(report.contains("Skin path seam: unknown"));
        assertFalse(report.contains("Skin path seam: the join-packet hook is not installed"));
    }

    @Test
    public void theReportNamesTheFirstFailingStage() {
        // Phase 0.3: the report must point at one broken link, not just the end result.
        String report = CosmeticsDiagnosticsReport.format(fullInputs());
        assertTrue(report.contains("FIRST FAILING STAGE: none — the cape reaches the game"));

        CosmeticsDiagnosticsReport.Inputs noLib = fullInputs();
        noLib.nativeLoaded = false;
        assertTrue(CosmeticsDiagnosticsReport.firstFailingStage(noLib).startsWith("0. native library"));

        CosmeticsDiagnosticsReport.Inputs noRules = fullInputs();
        noRules.rulesRevision = 0;
        assertTrue(CosmeticsDiagnosticsReport.firstFailingStage(noRules).startsWith("1. rules file"));

        CosmeticsDiagnosticsReport.Inputs notLoaded = fullInputs();
        notLoaded.rulesLoadAttempted = false;
        assertTrue(CosmeticsDiagnosticsReport.firstFailingStage(notLoaded).startsWith("2. rules load"));

        CosmeticsDiagnosticsReport.Inputs noSlot = fullInputs();
        noSlot.rulesHaveRenderIndex = false;
        assertTrue(CosmeticsDiagnosticsReport.firstFailingStage(noSlot).startsWith("3. rule match"));

        CosmeticsDiagnosticsReport.Inputs notArmed = fullInputs();
        notArmed.hookInstallState = 0;
        assertTrue(CosmeticsDiagnosticsReport.firstFailingStage(notArmed).startsWith("4. install armed"));

        CosmeticsDiagnosticsReport.Inputs waiting = fullInputs();
        waiting.hookInstallState = 1;
        assertTrue(CosmeticsDiagnosticsReport.firstFailingStage(waiting).contains("waiting for the game runtime"));

        CosmeticsDiagnosticsReport.Inputs noHook = fullInputs();
        noHook.hookInstalled = false;
        assertTrue(CosmeticsDiagnosticsReport.firstFailingStage(noHook).startsWith("5. render slot"));
    }

    @Test
    public void theReportSaysWhetherItWasGeneratedInsideAWorld() {
        CosmeticsDiagnosticsReport.Inputs in = fullInputs();
        in.inWorld = true;
        assertTrue(CosmeticsDiagnosticsReport.format(in).contains("Generated inside a world: yes"));
        in.inWorld = false;
        assertTrue(CosmeticsDiagnosticsReport.format(in).contains("Generated inside a world: no"));
    }

    @Test
    public void theReportCarriesEveryRequestedField() {
        String report = CosmeticsDiagnosticsReport.format(fullInputs());
        assertTrue(report.contains("App version: v1.2.3"));
        assertTrue(report.contains("Minecraft version: 1.26.33.1"));
        assertTrue(report.contains("Player-render slot: 17"));
        assertTrue(report.contains("resolved by render-diagnostic string scan"));
        assertTrue(report.contains("Hook installed: yes"));
        assertTrue(report.contains("Frames seen: 120"));
        assertTrue(report.contains("Cape engine texture route: yes"));
        assertTrue(report.contains("Engine image pipeline live: yes"));
        assertTrue(report.contains("GL backend: GLES"));
        assertTrue(report.contains("EGL context current: yes"));
        assertTrue(report.contains("Published geometry bytes: 2048"));
        assertTrue(report.contains("Piece atlases: cape hat"));
        assertTrue(report.contains("published under key: \"local\""));
        assertTrue(report.contains("Mod-menu build failures (containment guard)"));
        assertTrue(report.contains("Last crash report: none"));
    }

    @Test
    public void theReportCarriesTheStageOneDiagnostics() {
        String report = CosmeticsDiagnosticsReport.format(fullInputs());
        assertTrue(report.contains("Rules source: bundled (revision 6)"));
        assertTrue(report.contains("Rules delivered to native: yes"));
        assertTrue(report.contains("Effective rule: pauseMenuDtorSig,playerRenderVtableIndex=17"));
        assertTrue(report.contains("Rule has render slot: yes"));
        assertTrue(report.contains("Hook install state: resolved after 3 frame tick(s)"));
        assertTrue(report.contains("libpreloader.so copies mapped: 1"));
        assertTrue(report.contains("Local player feed: live x=10.50 y=64.00 z=-3.25"));
        assertTrue(report.contains("Camera anchor source: engine view-projection"));
    }

    @Test
    public void aSplitPreloaderMappingIsCalledOut() {
        CosmeticsDiagnosticsReport.Inputs in = fullInputs();
        in.preloaderModuleMappings = 2;
        assertTrue(CosmeticsDiagnosticsReport.format(in).contains("SPLIT"));
    }

    @Test
    public void theFirstPersonFallbackIsNotReportedAsTheEngineCamera() {
        CosmeticsDiagnosticsReport.Inputs in = fullInputs();
        in.cameraAnchorSource = 0;
        String report = CosmeticsDiagnosticsReport.format(in);
        assertTrue(report.contains("first-person fallback"));
        assertFalse(report.contains("Camera anchor source: engine view-projection"));
    }

    @Test
    public void aSkipDrawDecisionIsNamed() {
        // The GLES rasteriser is retired; the draw-decision field is gone from the report.
        String report = CosmeticsDiagnosticsReport.format(fullInputs());
        assertFalse(report.contains("Last draw decision"));
    }

    @Test
    public void theLastCrashReportIsIncludedWhenPresent() {
        CosmeticsDiagnosticsReport.Inputs in = fullInputs();
        in.lastCrashReport = "java.lang.IllegalStateException: already has a parent\n\tat A.b";
        String report = CosmeticsDiagnosticsReport.format(in);
        assertTrue(report.contains("Last crash report:"));
        assertTrue(report.contains("already has a parent"));
    }

    @Test
    public void menuGuardFailuresAreIncludedWhenPresent() {
        CosmeticsDiagnosticsReport.Inputs in = fullInputs();
        in.menuGuardFailures = "  buildControllerSection: java.lang.NullPointerException: boom\n";
        String report = CosmeticsDiagnosticsReport.format(in);
        assertTrue(report.contains("buildControllerSection"));
        assertTrue(report.contains("NullPointerException"));
    }

    @Test
    public void theReportAlwaysStatesTheDeviceVerificationCaveat() {
        String report = CosmeticsDiagnosticsReport.format(fullInputs());
        assertTrue(report.contains("unverified on device"));
    }

    @Test
    public void unknownReadingsAreNeverRenderedAsARealValue() {
        // No native library: every native-derived field must read as unknown, not as 0/yes.
        CosmeticsDiagnosticsReport.Inputs in = new CosmeticsDiagnosticsReport.Inputs();
        in.timestampMs = 0;
        String report = CosmeticsDiagnosticsReport.format(in);
        assertTrue(report.contains("Native library loaded: no"));
        assertTrue(report.contains("Player-render slot: unknown"));
        assertTrue(report.contains("Frames seen: unknown"));
        assertTrue(report.contains("Published geometry bytes: unknown"));
        assertTrue(report.contains("Piece atlases: unknown"));
    }

    @Test
    public void theStatusLineRequiresAnActualDrawToClaimActive() {
        // The cape engine route is the cape's live route; with it live the status says so.
        CosmeticsDiagnosticsReport.Inputs in = fullInputs();
        assertTrue(CosmeticsDiagnosticsReport.statusText(in).contains("cape active through the engine texture loader"));

        // Hook live but the cape route not yet observed: not "active".
        CosmeticsDiagnosticsReport.Inputs noCape = fullInputs();
        noCape.capeEngineRouteLive = false;
        assertTrue(CosmeticsDiagnosticsReport.statusText(noCape).contains("has not observed a loaded cape texture"));
    }

    @Test
    public void theStatusLineIsInactiveWhenTheLibraryOrHookIsMissing() {
        CosmeticsDiagnosticsReport.Inputs noLib = new CosmeticsDiagnosticsReport.Inputs();
        assertTrue(CosmeticsDiagnosticsReport.statusText(noLib).contains("native library not loaded"));

        CosmeticsDiagnosticsReport.Inputs noHookNoCape = fullInputs();
        noHookNoCape.hookInstalled = false;
        noHookNoCape.capeEngineRouteLive = false;
        assertTrue(CosmeticsDiagnosticsReport.statusText(noHookNoCape).contains("preview-only"));
    }

    @Test
    public void aNullInputIsSafe() {
        assertTrue(CosmeticsDiagnosticsReport.format(null).contains("Cosmetics diagnostics"));
    }

    @Test
    public void theHookLineDistinguishesWaitingFromFailed() {
        CosmeticsDiagnosticsReport.Inputs pending = fullInputs();
        pending.hookInstalled = false;
        pending.hookInstallPending = true;
        assertTrue(CosmeticsDiagnosticsReport.format(pending)
                .contains("Hook installed: waiting for the render context"));

        CosmeticsDiagnosticsReport.Inputs failed = fullInputs();
        failed.hookInstalled = false;
        failed.hookInstallPending = false;
        assertTrue(CosmeticsDiagnosticsReport.format(failed).contains("Hook installed: no"));

        assertTrue(CosmeticsDiagnosticsReport.format(fullInputs()).contains("Hook installed: yes"));
    }

    @Test
    public void theLocalKeyIsStableAndNamed() {
        assertEquals("local", NativeCosmeticsBridge.LOCAL_PLAYER_ID);
        assertEquals(NativeCosmeticsBridge.keyFor("local"), NativeCosmeticsBridge.localKey());
        assertTrue(NativeCosmeticsBridge.localKey() != 0L);
    }

    @Test
    public void clearingTheLocalSetDoesNotThrowWithoutTheNativeLibrary() {
        // A JVM test has no libpreloader, so this must be a safe no-op rather than a crash.
        NativeCosmeticsBridge.clearLocal();
    }

    @Test
    public void theFramePacingLineIsHonestAboutAMissingReading() {
        CosmeticsDiagnosticsReport.Inputs none = fullInputs();
        none.framePacingAvailable = false;
        assertTrue(CosmeticsDiagnosticsReport.format(none).contains("Frame pacing: no reading yet"));
    }

    @Test
    public void theFramePacingLineReportsLockAndHitch() {
        CosmeticsDiagnosticsReport.Inputs locked = fullInputs();
        locked.framePacingAvailable = true;
        locked.framePacingAverageUs = 16667;
        locked.framePacingWorstUs = 17000;
        locked.framePacingVsyncLocked = true;
        String report = CosmeticsDiagnosticsReport.format(locked);
        assertTrue(report.contains("Frame pacing: 16.7 ms average, 17.0 ms worst — vsync-locked"));

        CosmeticsDiagnosticsReport.Inputs hitching = fullInputs();
        hitching.framePacingAvailable = true;
        hitching.framePacingAverageUs = 16667;
        hitching.framePacingWorstUs = 60000;
        hitching.framePacingJittering = true;
        assertTrue(CosmeticsDiagnosticsReport.format(hitching).contains("a recent frame was late"));
    }
}
