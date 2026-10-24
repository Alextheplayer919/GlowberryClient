package org.chimeramc.client.launcher.core.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;


import org.junit.Test;

/**
 * The launch-failure classifier (G). Pure: it reads message text, never a device.
 *
 * <p>The cases here are the real strings Android and this launcher produce, because the whole point
 * of the class is that a real tombstone maps to the right sentence.
 */
public class LaunchErrorDiagnosisTest {

    @Test
    public void aPlayStoreStyleDlopenNamesTheGameLibraryCause() {
        // The exact shape Android reports when the launcher cannot reach the game's own library.
        assertEquals(LaunchErrorDiagnosis.Cause.MISSING_GAME_LIBRARY,
                LaunchErrorDiagnosis.classifyText(
                        "java.lang.UnsatisfiedLinkError: dlopen failed: library \"libminecraftpe.so\" not found"));
        assertEquals(LaunchErrorDiagnosis.Cause.MISSING_GAME_LIBRARY,
                LaunchErrorDiagnosis.classifyText(
                        "Failed to load libminecraftpe.so: cannot open shared object file"));
    }

    @Test
    public void aBitnessMismatchIsNotReportedAsAMissingFile() {
        // "is 32-bit instead of 64-bit" names no file; classifying it as missing would send the
        // user to re-import the same 32-bit build.
        assertEquals(LaunchErrorDiagnosis.Cause.ABI_MISMATCH,
                LaunchErrorDiagnosis.classifyText(
                        "dlopen failed: \"/data/.../libminecraftpe.so\" is 32-bit instead of 64-bit"));
        assertEquals(LaunchErrorDiagnosis.Cause.ABI_MISMATCH,
                LaunchErrorDiagnosis.classifyText(
                        "32-bit version (armeabi-v7a) requires a 32-bit process, but the current process is 64-bit"));
    }

    @Test
    public void aMissingDependencyIsItsOwnCause() {
        assertEquals(LaunchErrorDiagnosis.Cause.MISSING_DEPENDENCY,
                LaunchErrorDiagnosis.classifyText(
                        "dlopen failed: library \"libc++_shared.so\" not found"));
        assertEquals(LaunchErrorDiagnosis.Cause.MISSING_DEPENDENCY,
                LaunchErrorDiagnosis.classifyText(
                        "Failed to load native libraries:\nlibfmod.so: dlopen failed: library not found"));
    }

    @Test
    public void theGameLibraryWinsOverAGenericLibraryFailure() {
        // libminecraftpe is the library whose absence means "this install cannot launch", so it
        // must not be downgraded to "a dependency is missing".
        assertEquals(LaunchErrorDiagnosis.Cause.MISSING_GAME_LIBRARY,
                LaunchErrorDiagnosis.classifyText(
                        "Failed to load native libraries:\nlibminecraftpe.so: dlopen failed: library not found"));
    }

    @Test
    public void aModFailureIsItsOwnCause() {
        assertEquals(LaunchErrorDiagnosis.Cause.MOD_FAILED,
                LaunchErrorDiagnosis.classifyText(
                        "UnsatisfiedLinkError: No implementation found for mod entry point"));
    }

    @Test
    public void memoryAndMissingGameAreNamed() {
        assertEquals(LaunchErrorDiagnosis.Cause.OUT_OF_MEMORY,
                LaunchErrorDiagnosis.classifyText("java.lang.OutOfMemoryError: Failed to allocate a 4096 byte allocation"));
        assertEquals(LaunchErrorDiagnosis.Cause.GAME_NOT_FOUND,
                LaunchErrorDiagnosis.classifyText("IllegalStateException: Minecraft not found"));
    }

    @Test
    public void anUnrecognisedFailureStaysUnknownRatherThanGuessing() {
        assertEquals(LaunchErrorDiagnosis.Cause.UNKNOWN,
                LaunchErrorDiagnosis.classifyText("java.lang.IllegalStateException: something else"));
        assertEquals(LaunchErrorDiagnosis.Cause.UNKNOWN, LaunchErrorDiagnosis.classifyText(null));
    }

    @Test
    public void aBareUnsatisfiedLinkErrorTakesTheGameLibraryMessage() {
        // A link error with no library named is the game's own library far more often than
        // anything else here; "unknown" would tell the user nothing.
        assertEquals(LaunchErrorDiagnosis.Cause.MISSING_GAME_LIBRARY,
                LaunchErrorDiagnosis.classify(new UnsatisfiedLinkError()));
    }

    @Test
    public void classificationFollowsTheCauseChainToTheRoot() {
        Exception wrapper = new RuntimeException("Launch failed",
                new RuntimeException("preparation failed",
                        new UnsatisfiedLinkError("dlopen failed: library \"libminecraftpe.so\" not found")));
        assertEquals(LaunchErrorDiagnosis.Cause.MISSING_GAME_LIBRARY,
                LaunchErrorDiagnosis.classify(wrapper));
    }

    @Test
    public void anOutOfMemoryErrorIsClassifiedEvenThoughItIsAnErrorNotAnException() {
        assertEquals(LaunchErrorDiagnosis.Cause.OUT_OF_MEMORY,
                LaunchErrorDiagnosis.classify(new OutOfMemoryError("Failed to allocate")));
    }

    @Test
    public void everyCauseHasItsOwnTitleAndMessage() {
        LaunchErrorDiagnosis.Cause[] causes = LaunchErrorDiagnosis.Cause.values();
        for (int i = 0; i < causes.length; i++) {
            assertNotEquals("missing title for " + causes[i],
                    0, LaunchErrorDiagnosis.titleRes(causes[i]));
            assertNotEquals("missing message for " + causes[i],
                    0, LaunchErrorDiagnosis.messageRes(causes[i]));
            for (int j = i + 1; j < causes.length; j++) {
                assertNotEquals("shared message for " + causes[i] + " and " + causes[j],
                        LaunchErrorDiagnosis.messageRes(causes[i]),
                        LaunchErrorDiagnosis.messageRes(causes[j]));
            }
        }
    }

    @Test
    public void aPresentButUnusableLibraryIsStaleNotMissing() {
        // "cannot locate symbol" also contains "dlopen failed", so without its own branch it would
        // be reported as a missing file. The file is present; the cache does not match the build,
        // which is what Repair fixes.
        assertEquals(LaunchErrorDiagnosis.Cause.STALE_NATIVE_LIBS,
                LaunchErrorDiagnosis.classifyText(
                        "dlopen failed: cannot locate symbol \"_ZN6Player4tickEv\" referenced by \"libminecraftpe.so\""));
        assertEquals(LaunchErrorDiagnosis.Cause.STALE_NATIVE_LIBS,
                LaunchErrorDiagnosis.classifyText(
                        "java.lang.UnsatisfiedLinkError: dlopen failed: undefined symbol: _ZN3mce5ImageD1Ev"));
    }

    @Test
    public void everyCauseIsReachableFromSomeRealMessage() {
        // A declared cause with no message that can produce it is a mapping nobody can ever see -
        // the exact shape of a half-finished feature. Pin that each cause has a reachable input.
        assertEquals(LaunchErrorDiagnosis.Cause.MISSING_GAME_LIBRARY,
                LaunchErrorDiagnosis.classifyText("dlopen failed: library \"libminecraftpe.so\" not found"));
        assertEquals(LaunchErrorDiagnosis.Cause.ABI_MISMATCH,
                LaunchErrorDiagnosis.classifyText("is 32-bit instead of 64-bit"));
        assertEquals(LaunchErrorDiagnosis.Cause.MISSING_DEPENDENCY,
                LaunchErrorDiagnosis.classifyText("dlopen failed: library \"libfmod.so\" not found"));
        assertEquals(LaunchErrorDiagnosis.Cause.STALE_NATIVE_LIBS,
                LaunchErrorDiagnosis.classifyText("dlopen failed: cannot locate symbol \"x\""));
        assertEquals(LaunchErrorDiagnosis.Cause.GAME_NOT_FOUND,
                LaunchErrorDiagnosis.classifyText("Minecraft not found"));
        assertEquals(LaunchErrorDiagnosis.Cause.MOD_FAILED,
                LaunchErrorDiagnosis.classifyText("UnsatisfiedLinkError: No implementation found for mod"));
        assertEquals(LaunchErrorDiagnosis.Cause.OUT_OF_MEMORY,
                LaunchErrorDiagnosis.classifyText("OutOfMemoryError"));
        assertEquals(LaunchErrorDiagnosis.Cause.UNKNOWN,
                LaunchErrorDiagnosis.classifyText("something else entirely"));
    }

    @Test
    public void keysAndLabelsAreStableForABugReport() {
        assertEquals("missing_game_library",
                LaunchErrorDiagnosis.key(LaunchErrorDiagnosis.Cause.MISSING_GAME_LIBRARY));
        assertEquals("unknown", LaunchErrorDiagnosis.key(null));
        assertEquals("MISSING_GAME_LIBRARY",
                LaunchErrorDiagnosis.label(LaunchErrorDiagnosis.Cause.MISSING_GAME_LIBRARY));
        assertEquals("UNKNOWN", LaunchErrorDiagnosis.label(null));
    }
}
