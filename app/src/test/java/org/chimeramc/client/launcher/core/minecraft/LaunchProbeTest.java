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
        boolean installedUnreadable;
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
        @Override public boolean installedPackageUnreadable() { maybeThrow("installedPackageUnreadable"); return installedUnreadable; }
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
        assertFalse(facts.installedPackageUnreadable);
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
            @Override public boolean installedPackageUnreadable() { return false; }
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

        FakeSource playStore = new FakeSource();
        // The Play Store case: the instance is the installed package, so gameFilesPresent is true
        // (there is no import file to look for) and the unreadable-package flag is what reports it.
        playStore.installedUnreadable = true;
        assertEquals(LaunchReadiness.State.PLAY_STORE_PACKAGE,
                LaunchReadiness.evaluate(LaunchProbe.gather(playStore)));

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

    @Test
    public void aPlayStoreInstallIsNotReportedAsMissingItsFiles() {
        // The bug: an installed Minecraft has no base.apk.chimera, so the old single "files present"
        // probe was always false for it and the launch was refused as "game files missing" even
        // though the game was installed. The installed case must report its own state.
        LaunchReadiness.Facts facts = new LaunchReadiness.Facts()
                .versionSelected(true)
                .gameFilesPresent(true)
                .installedPackageUnreadable(true);
        assertEquals(LaunchReadiness.State.PLAY_STORE_PACKAGE, LaunchReadiness.evaluate(facts));
        assertTrue(LaunchReadiness.blocksLaunch(LaunchReadiness.State.PLAY_STORE_PACKAGE));
    }

    @Test
    public void anUnreadableInstalledPackageWinsOverTheAbiAndStaleChecks() {
        // The installed package's own facts are not meaningful once the launcher cannot read the
        // library at all, so the accurate state must be reported rather than a downstream symptom.
        LaunchReadiness.Facts facts = new LaunchReadiness.Facts()
                .versionSelected(true)
                .installedPackageUnreadable(true)
                .abiIncompatible(true)
                .extractionStale(true)
                .signatureRulesCoverBuild(false);
        assertEquals(LaunchReadiness.State.PLAY_STORE_PACKAGE, LaunchReadiness.evaluate(facts));
    }

    @Test
    public void aReadableInstalledPackageLaunches() {
        // The regression guard: an installed package the launcher *can* read must not be blocked.
        LaunchReadiness.Facts facts = new LaunchReadiness.Facts()
                .versionSelected(true)
                .gameFilesPresent(true)
                .installedPackageUnreadable(false);
        assertEquals(LaunchReadiness.State.READY, LaunchReadiness.evaluate(facts));
    }
}
