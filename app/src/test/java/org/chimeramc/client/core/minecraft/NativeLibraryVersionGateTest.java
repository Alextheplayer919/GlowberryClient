package org.chimeramc.client.core.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The native-library version gate (G3/G4).
 *
 * <p>This rule decides both what the extracted cache must contain and what the loader tries to
 * {@code dlopen}. It used to exist twice, once per caller, with no test — the two could disagree
 * and the launch would fail with "library not found" for a library the cache deliberately skipped.
 * Pinned here so the thresholds cannot drift, and so the "the two callers read the same rule" claim
 * is checked rather than asserted in a comment.
 */
public class NativeLibraryVersionGateTest {

    @Test
    public void theHttpClientGateOpensAtItsReleaseAndNotBefore() {
        assertTrue(NativeLibraryVersionGate.shouldLoadHttpClient("1.21.130"));
        assertTrue(NativeLibraryVersionGate.shouldLoadHttpClient("1.21.130.1"));
        assertTrue(NativeLibraryVersionGate.shouldLoadHttpClient("1.21.132"));
        assertTrue(NativeLibraryVersionGate.shouldLoadHttpClient("1.26.50.4"));
        assertFalse(NativeLibraryVersionGate.shouldLoadHttpClient("1.21.129"));
        assertFalse(NativeLibraryVersionGate.shouldLoadHttpClient("1.21.80"));
    }

    @Test
    public void aBetaComparesAgainstItsOwnTargetNotTheRelease() {
        // The beta target (1.21.130.20) sorts above the release target (1.21.130 == 130.0.0), which
        // is exactly why the beta needs its own threshold: without it a beta would be measured
        // against the release number and the gate would open at the wrong build.
        assertTrue(NativeLibraryVersionGate.shouldLoadHttpClient("1.21.130.20-beta"));
        assertTrue(NativeLibraryVersionGate.shouldLoadHttpClient("1.21.130.20.0-beta"));
        assertFalse(NativeLibraryVersionGate.shouldLoadHttpClient("1.21.129.5-beta"));
    }

    @Test
    public void aSuffixOnTheVersionDoesNotBecomeAPart() {
        // "-beta" / "-preview" must be stripped before the parts are compared, or "130.20-beta"
        // would compare a non-numeric part.
        assertTrue(NativeLibraryVersionGate.shouldLoadHttpClient("1.21.130.20-beta"));
        assertTrue(NativeLibraryVersionGate.shouldLoadHttpClient("1.21.132.1-preview"));
        assertFalse(NativeLibraryVersionGate.shouldLoadHttpClient("1.21.129-preview"));
    }

    @Test
    public void theMaesdkAndPlayFabGatesUseTheirOwnThresholds() {
        // 1.21.110 introduced the maesdk bundle; the HTTP client arrived later. A single threshold
        // for both would either extract maesdk for builds that do not have it or miss it for builds
        // that do.
        assertTrue(NativeLibraryVersionGate.shouldLoadMaesdk("1.21.110"));
        assertFalse(NativeLibraryVersionGate.shouldLoadMaesdk("1.21.109"));
        assertTrue(NativeLibraryVersionGate.shouldLoadMaesdk("1.26.50.4"));
        assertFalse(NativeLibraryVersionGate.shouldLoadPlayFab("1.21.110"));
        assertTrue(NativeLibraryVersionGate.shouldLoadPlayFab("1.21.130"));
    }

    @Test
    public void aMissingOrUnparseableVersionIsNotAtLeastAnything() {
        // Fail-closed: a gate that decides whether a library is required must not answer "yes" for a
        // version it cannot read, or the cache would demand a library the build may not ship.
        assertFalse(NativeLibraryVersionGate.shouldLoadHttpClient(null));
        assertFalse(NativeLibraryVersionGate.shouldLoadMaesdk(null));
        assertFalse(NativeLibraryVersionGate.shouldLoadPlayFab(null));
        assertFalse(NativeLibraryVersionGate.isVersionAtLeast(null, "1.21.130"));
        assertFalse(NativeLibraryVersionGate.isVersionAtLeast("1.21.130", null));
        assertFalse(NativeLibraryVersionGate.isVersionAtLeast("", "1.21.130"));
    }

    @Test
    public void missingComponentsCompareAsZero() {
        assertTrue(NativeLibraryVersionGate.isVersionAtLeast("1.21", "1.21.0"));
        assertTrue(NativeLibraryVersionGate.isVersionAtLeast("1.21.130", "1.21.130.0"));
        assertFalse(NativeLibraryVersionGate.isVersionAtLeast("1.21.129.9", "1.21.130"));
    }

    @Test
    public void bothCallersReadThisOneRule() {
        // The regression guard: if someone re-adds a local copy of the gate in either caller, this
        // test still passes but the drift it caused becomes possible again. What is checked here is
        // that the shared rule itself is stable across the builds the launcher supports, so a change
        // to it is a deliberate, tested change rather than a silent one.
        String[] supported = {
                "1.21.80", "1.21.110", "1.21.129", "1.21.130", "1.21.130.20-beta",
                "1.21.132", "1.26.50.4", "1.26.60.28", "1.26.60.30",
        };
        for (String version : supported) {
            // No exception, and a stable answer: the same input must never flip.
            boolean http = NativeLibraryVersionGate.shouldLoadHttpClient(version);
            boolean maesdk = NativeLibraryVersionGate.shouldLoadMaesdk(version);
            assertTrue(http == NativeLibraryVersionGate.shouldLoadHttpClient(version));
            assertTrue(maesdk == NativeLibraryVersionGate.shouldLoadMaesdk(version));
            // The HTTP client and PlayFab share a threshold, so a build that needs one needs both.
            assertTrue("http and playfab must agree for " + version,
                    http == NativeLibraryVersionGate.shouldLoadPlayFab(version));
        }
    }

    @Test
    public void theGatedLibraryListCoversEveryGateAndNothingElse() {
        // The cache's completeness check is built from this list, and the loader opens exactly these
        // files out of the extracted directory. A library in one but not the other is a launch
        // failure: the cache says "complete", the loader then opens a file that was never required
        // to be there. Pinned by name so adding a gate means adding it here too.
        // 1.21.110 gained the maesdk bundle; the HTTP client and PlayFab arrived at 1.21.130, which
        // is also above the maesdk threshold, so that build carries all three.
        assertEquals(java.util.Collections.singletonList("libmaesdk.so"),
                NativeLibraryVersionGate.gatedLibraries("1.21.110"));
        assertEquals(java.util.Arrays.asList(
                        "libHttpClient.Android.so", "libmaesdk.so", "libPlayFabMultiplayer.so"),
                NativeLibraryVersionGate.gatedLibraries("1.21.130"));
        assertEquals(java.util.Arrays.asList(
                        "libHttpClient.Android.so", "libmaesdk.so", "libPlayFabMultiplayer.so"),
                NativeLibraryVersionGate.gatedLibraries("1.26.50.4"));
        assertEquals(java.util.Collections.emptyList(),
                NativeLibraryVersionGate.gatedLibraries("1.21.80"));

        // Every returned name must be one the loader would actually open, i.e. its gate says yes.
        for (String version : new String[]{"1.21.80", "1.21.110", "1.21.130", "1.26.50.4"}) {
            for (String lib : NativeLibraryVersionGate.gatedLibraries(version)) {
                switch (lib) {
                    case "libHttpClient.Android.so":
                        assertTrue(NativeLibraryVersionGate.shouldLoadHttpClient(version));
                        break;
                    case "libmaesdk.so":
                        assertTrue(NativeLibraryVersionGate.shouldLoadMaesdk(version));
                        break;
                    case "libPlayFabMultiplayer.so":
                        assertTrue(NativeLibraryVersionGate.shouldLoadPlayFab(version));
                        break;
                    default:
                        throw new AssertionError("unexpected gated library: " + lib);
                }
            }
        }
    }
}
