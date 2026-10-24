package org.chimeramc.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.chimeramc.client.preloader.PreloaderRules;
import org.chimeramc.client.preloader.PreloaderSignatureRulesManager;
import org.chimeramc.client.util.Api28Compat;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * The instrumented API-28 smoke test: it runs on a real device/emulator and exercises the exact
 * call that killed the launch.
 *
 * <p>Why this must be an instrumented test and not a JVM one: the JVM suite runs on a desktop JDK
 * where {@code InputStream.readAllBytes()} exists, so it could never have caught the bug. The
 * failure only exists against the device's {@code java.base}, which is what this test uses.
 *
 * <p>CI runs it on an API 28 emulator (the minimum supported level) and on API 35 (the target), so a
 * library call newer than the minimum SDK fails the pipeline instead of a user's launch.
 *
 * <p>Every assertion here reads real state. A {@code NoSuchMethodError} fails the test at the call
 * site, so reaching the assertion is itself the check — but the assertions also pin the parsed
 * result, so a method that silently returned nothing would still fail.
 */
@RunWith(AndroidJUnit4.class)
public class Api28RuntimeSmokeTest {

    private Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    /**
     * A real rules document with one floor rule. Shape copied from the shipped source so the parse
     * exercises the same fields the launcher relies on.
     */
    private static final String RULES_JSON =
            "{\"revision\":1,\"rules\":[{\"min\":\"1.21.132\",\"max\":\"1.26.39.99\","
                    + "\"isShowingMenuVtableIndex\":151,\"playerRenderVtableIndex\":17}]}";

    @Test
    public void theInstalledApkDeclaresTheMinimumSdkTheCompatibilityStatementPromises() throws Exception {
        ApplicationInfo info = context().getPackageManager()
                .getApplicationInfo(context().getPackageName(), 0);
        assertEquals("the installed APK's minSdk must be the 28 the docs promise",
                28, info.minSdkVersion);
    }

    @Test
    public void theRuleParserResolvesACoveredAndAnUncoveredBuild() {
        // Pure and device-independent in logic, but run here so the device's runtime parses it.
        assertTrue("a rule with min 1.21.132 must cover 1.21.132",
                PreloaderRules.coversVersion(RULES_JSON, "1.21.132"));
        assertTrue("the rule's range must cover a build inside it",
                PreloaderRules.coversVersion(RULES_JSON, "1.26.30.5"));
        assertFalse("a build above the rule's max must not be covered",
                PreloaderRules.coversVersion(RULES_JSON, "1.26.60.28"));
        assertFalse("an empty document covers nothing",
                PreloaderRules.coversVersion("", "1.21.132"));
    }

    @Test
    public void readingTheSignatureRulesFileFromDiskDoesNotThrowOnThisDevice() throws Exception {
        // The launch crash was a readAllBytes() in this reader on the "Loading game loader" path.
        // Write a real file into the location the manager reads, then drive the real read path.
        File rules = PreloaderSignatureRulesManager.getRulesFile(context());
        assertNotNull("the rules file path must resolve", rules);
        File parent = rules.getParentFile();
        if (parent != null && !parent.exists()) {
            assertTrue(parent.mkdirs());
        }
        try (FileOutputStream out = new FileOutputStream(rules)) {
            out.write(RULES_JSON.getBytes(StandardCharsets.UTF_8));
        }

        // coversInstalledVersion goes through readRulesFile. On the crashing build this threw
        // NoSuchMethodError (an Error, so the catch(Exception) inside did not contain it).
        assertTrue("a covered build must read back as covered",
                PreloaderSignatureRulesManager.coversInstalledVersion(context(), "1.21.132"));
        assertFalse("an uncovered build must read back as uncovered",
                PreloaderSignatureRulesManager.coversInstalledVersion(context(), "1.26.60.28"));

        // A build with no covering rule yields the diagnostics message, not a throw.
        assertNotNull("an uncovered build must produce a coverage-failure message",
                PreloaderSignatureRulesManager.describeCoverageFailure(context(), "1.26.60.28"));
    }

    @Test
    public void theApi28CompatHelpersBehaveTheSameAsTheLibraryCallsTheyReplace() {
        assertTrue(Api28Compat.isBlank("   "));
        assertTrue(Api28Compat.isBlank("\t\n"));
        assertTrue(Api28Compat.isBlank(""));
        assertFalse(Api28Compat.isBlank("x"));
        assertFalse(Api28Compat.isBlank(" x "));

        List<String> list = Api28Compat.listOf("a", "b");
        assertEquals(2, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));

        Set<String> set = Api28Compat.setOf("a", "b", "a");
        assertEquals(2, set.size());
        assertTrue(set.contains("a") && set.contains("b"));
    }

    @Test
    public void theLauncherEntryActivityIsPresentAndResolves() {
        // A missing or renamed entry class is an instant launch failure on every device.
        PackageManager pm = context().getPackageManager();
        Intent launch = pm.getLaunchIntentForPackage(context().getPackageName());
        assertNotNull("the launcher must have a MAIN/LAUNCHER entry activity", launch);
        assertNotNull("the entry activity must resolve to a component", launch.getComponent());
        assertTrue("the entry activity must be the splash screen",
                launch.getComponent().getClassName().endsWith("SplashActivity"));
        // Resolving it proves the class is present and installable, not just named.
        assertNotNull(pm.resolveActivity(launch, 0));
    }

    @Test
    public void thisTestRanOnADeviceNotAHostJvm() {
        // Guards against the test being silently repointed at a host JVM run, where it would prove
        // nothing about the device runtime.
        assertTrue("an instrumented run reports a real device fingerprint",
                Build.FINGERPRINT != null && !Build.FINGERPRINT.isEmpty());
    }
}
