package org.chimeramc.client.launcher.core.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the launch preflight's fact gathering, including how it contains a failing read.
 *
 * <p>No mocks: {@link LaunchProbe.Source} is a small interface, so the fake is a plain class whose
 * behaviour a test sets directly. The point of the class is that a probe is I/O that must not be
 * able to fail a launch, so the interesting cases are the throwing ones.
 */
public class LaunchProbeTest {

    /** A source whose every answer is settable, and which can be told to throw on any read. */
    private static final class FakeSource implements LaunchProbe.Source {
        boolean selected = true;
        boolean filesPresent = true;
        boolean abiIncompatible;
        boolean stale;
        boolean rulesCover = true;
        boolean installed = true;
        boolean signInRequired;
        boolean signedIn = true;
        String throwOn;

        private void maybeThrow(String name) {
            if (name.equals(throwOn)) {
                throw new RuntimeException("probe failed: " + name);
            }
        }

        @Override public boolean versionSelected() { maybeThrow("versionSelected"); return selected; }
        @Override public boolean gameFilesPresent() { maybeThrow("gameFilesPresent"); return filesPresent; }
        @Override public boolean abiIncompatible() { maybeThrow("abiIncompatible"); return abiIncompatible; }
        @Override public boolean extractionStale() { maybeThrow("extractionStale"); return stale; }
        @Override public boolean signatureRulesCoverBuild() { maybeThrow("signatureRulesCoverBuild"); return rulesCover; }
        @Override public boolean minecraftInstalled() { maybeThrow("minecraftInstalled"); return installed; }
        @Override public boolean signInRequired() { maybeThrow("signInRequired"); return signInRequired; }
        @Override public boolean signedIn() { maybeThrow("signedIn"); return signedIn; }
    }

    @Test
    public void aHealthyInstallGathersEveryFact() {
        FakeSource source = new FakeSource();
        LaunchReadiness.Facts facts = LaunchProbe.gather(source);
        assertTrue(facts.versionSelected);
        assertTrue(facts.gameFilesPresent);
        assertFalse(facts.abiIncompatible);
        assertFalse(facts.extractionStale);
        assertTrue(facts.signatureRulesCoverBuild);
        assertTrue(facts.minecraftInstalled);
        assertFalse(facts.signInRequired);
        assertTrue(facts.signedIn);
        assertEquals(LaunchReadiness.State.READY, LaunchReadiness.evaluate(facts));
    }

    @Test
    public void noVersionShortCircuitsTheOtherReads() {
        // On a fresh install the other probes would be asking about a version that does not exist.
        FakeSource source = new FakeSource();
        source.selected = false;
        // Every other read is a failure, but they must never be reached.
        source.throwOn = "gameFilesPresent";
        LaunchReadiness.Facts facts = LaunchProbe.gather(source);
        assertFalse(facts.versionSelected);
        assertEquals(LaunchReadiness.State.NO_VERSION_SELECTED, LaunchReadiness.evaluate(facts));
    }

    @Test
    public void aNullSourceReadsAsNoVersionRatherThanCrashing() {
        LaunchReadiness.Facts facts = LaunchProbe.gather(null);
        assertFalse(facts.versionSelected);
        assertEquals(LaunchReadiness.State.NO_VERSION_SELECTED, LaunchReadiness.evaluate(facts));
    }

    @Test
    public void aThrowingDiagnosticReadDoesNotFailTheLaunch() {
        // A probe that cannot answer is a diagnostic problem, not a reason to refuse a launch.
        for (String read : new String[] {"abiIncompatible", "extractionStale",
                "signatureRulesCoverBuild", "minecraftInstalled", "signInRequired", "signedIn"}) {
            FakeSource source = new FakeSource();
            source.throwOn = read;
            LaunchReadiness.Facts facts = LaunchProbe.gather(source);
            assertEquals("a throwing " + read + " must still leave a launchable state",
                    LaunchReadiness.State.READY, LaunchReadiness.evaluate(facts));
        }
    }

    @Test
    public void aThrowingGameFilesReadReadsAsMissingRatherThanReady() {
        // The one fact the user must act on. A read that cannot prove the files are there must not
        // report them present: launching into a missing package is the failure this prevents.
        FakeSource source = new FakeSource();
        source.throwOn = "gameFilesPresent";
        LaunchReadiness.Facts facts = LaunchProbe.gather(source);
        assertFalse(facts.gameFilesPresent);
        assertEquals(LaunchReadiness.State.GAME_FILES_MISSING, LaunchReadiness.evaluate(facts));
    }

    @Test
    public void aLinkageErrorIsContainedLikeAnyOtherFailure() {
        // The class of bug that killed the launch: NoSuchMethodError is an Error, not an Exception,
        // so a catch (Exception) would not contain it. The probe catches Throwable deliberately.
        LaunchProbe.Source source = new LaunchProbe.Source() {
            @Override public boolean versionSelected() { return true; }
            @Override public boolean gameFilesPresent() { return true; }
            @Override public boolean abiIncompatible() { throw new NoSuchMethodError("readAllBytes"); }
            @Override public boolean extractionStale() { return false; }
            @Override public boolean signatureRulesCoverBuild() { return true; }
            @Override public boolean minecraftInstalled() { return true; }
            @Override public boolean signInRequired() { return false; }
            @Override public boolean signedIn() { return true; }
        };
        LaunchReadiness.Facts facts = LaunchProbe.gather(source);
        assertFalse(facts.abiIncompatible);
        assertEquals(LaunchReadiness.State.READY, LaunchReadiness.evaluate(facts));
    }

    @Test
    public void realProblemsStillBlockTheLaunch() {
        FakeSource missingFiles = new FakeSource();
        missingFiles.filesPresent = false;
        assertEquals(LaunchReadiness.State.GAME_FILES_MISSING,
                LaunchReadiness.evaluate(LaunchProbe.gather(missingFiles)));

        FakeSource badAbi = new FakeSource();
        badAbi.abiIncompatible = true;
        assertEquals(LaunchReadiness.State.ABI_INCOMPATIBLE,
                LaunchReadiness.evaluate(LaunchProbe.gather(badAbi)));

        FakeSource notInstalled = new FakeSource();
        notInstalled.installed = false;
        assertEquals(LaunchReadiness.State.GAME_NOT_INSTALLED,
                LaunchReadiness.evaluate(LaunchProbe.gather(notInstalled)));

        FakeSource needsSignIn = new FakeSource();
        needsSignIn.signInRequired = true;
        needsSignIn.signedIn = false;
        assertEquals(LaunchReadiness.State.SIGN_IN_REQUIRED,
                LaunchReadiness.evaluate(LaunchProbe.gather(needsSignIn)));
    }
}
