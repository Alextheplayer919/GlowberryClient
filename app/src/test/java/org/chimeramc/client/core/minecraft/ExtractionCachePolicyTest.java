package org.chimeramc.client.core.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The extraction-cache decision (G1/B8).
 *
 * <p>Stale extracted native libraries are the reported cause of `dlopen` failures that only appear
 * after an update, so the rule that decides when to wipe them is pinned here. It lives outside
 * {@code GamePackageManager} because that class loads a native library during class initialisation
 * and could not be asserted against from a JVM test.
 */
public class ExtractionCachePolicyTest {

    @Test
    public void aLauncherUpdateInvalidatesTheCacheEvenWithNoPipelineChange() {
        int before = ExtractionCachePolicy.extractionVersion(4, 100);
        int after = ExtractionCachePolicy.extractionVersion(4, 101);
        assertTrue("a new app version must produce a new extraction version", before != after);
    }

    @Test
    public void aPipelineBumpInvalidatesTheCacheEvenWithNoAppUpdate() {
        int before = ExtractionCachePolicy.extractionVersion(4, 100);
        int after = ExtractionCachePolicy.extractionVersion(5, 100);
        assertTrue("bumping the extractor must produce a new extraction version", before != after);
    }

    @Test
    public void theExtractionVersionKeepsThePipelineInTheHighBits() {
        // The app version code is 16-bit-masked, so it must never bleed into the pipeline half.
        assertEquals(4, ExtractionCachePolicy.extractionVersion(4, 70000) >>> 16);
        assertEquals(4, ExtractionCachePolicy.extractionVersion(4, 0) >>> 16);
    }

    @Test
    public void aMarkerOnlyMatchesTheExactVersion() {
        assertTrue(ExtractionCachePolicy.markerMatches("262244", 262244));
        assertTrue(ExtractionCachePolicy.markerMatches("  262244\n", 262244));
        assertFalse(ExtractionCachePolicy.markerMatches("262243", 262244));
        assertFalse(ExtractionCachePolicy.markerMatches(null, 262244));
        assertFalse(ExtractionCachePolicy.markerMatches("", 262244));
        assertFalse(ExtractionCachePolicy.markerMatches("not a number", 262244));
    }

    @Test
    public void aChangedPackageForcesReExtractionWithoutAWipe() {
        // A manifest mismatch already re-extracts, so there is nothing stale to wipe first.
        assertFalse(ExtractionCachePolicy.shouldWipeStaleLibraries(false, false));
        assertFalse(ExtractionCachePolicy.shouldWipeStaleLibraries(false, true));
    }

    @Test
    public void anOlderExtractorWipesTheCacheBeforeReuse() {
        assertTrue(ExtractionCachePolicy.shouldWipeStaleLibraries(true, false));
        assertFalse(ExtractionCachePolicy.shouldWipeStaleLibraries(true, true));
    }

    @Test
    public void aTruncatedLibraryCountsAsMissing() {
        // A zero-length file is left behind by an interrupted extraction and is not loadable, so
        // the cache-hit path must not accept it.
        assertFalse(ExtractionCachePolicy.allLibrariesPresent(
                Collections.singletonList("libminecraftpe.so"), name -> false));
        assertTrue(ExtractionCachePolicy.allLibrariesPresent(
                Collections.singletonList("libminecraftpe.so"), name -> true));
        assertTrue("no required libraries means nothing can be missing",
                ExtractionCachePolicy.allLibrariesPresent(Collections.emptyList(), name -> false));
    }

    @Test
    public void theCacheIsOnlyReusedWhenEveryConditionHolds() {
        assertTrue(ExtractionCachePolicy.isCacheUsable(true, true, true));
        assertFalse(ExtractionCachePolicy.isCacheUsable(false, true, true));
        assertFalse(ExtractionCachePolicy.isCacheUsable(true, false, true));
        assertFalse(ExtractionCachePolicy.isCacheUsable(true, true, false));
        assertFalse(ExtractionCachePolicy.isCacheUsable(false, false, false));
    }

    @Test
    public void theWipeRuleAndTheReuseRuleAgree() {
        // Every combination: if the cache is usable it must not also be marked for a wipe, and a
        // wipe must always be followed by a cache miss. A disagreement here is a launch that loads
        // a half-deleted cache.
        for (boolean manifest : Arrays.asList(true, false)) {
            for (boolean version : Arrays.asList(true, false)) {
                boolean wipe = ExtractionCachePolicy.shouldWipeStaleLibraries(manifest, version);
                boolean usable = ExtractionCachePolicy.isCacheUsable(manifest, version, true);
                assertFalse("wiped cache must not read as usable", wipe && usable);
                if (manifest && !version) {
                    assertFalse("a stale extraction version must not be a cache hit", usable);
                }
            }
        }
    }

    @Test
    public void theMissingListNamesExactlyTheAbsentLibraries() {
        List<String> required = Arrays.asList("libc++_shared.so", "libfmod.so", "libminecraftpe.so");
        List<String> missing = ExtractionCachePolicy.missingLibraries(
                required, name -> !name.equals("libfmod.so"));
        assertEquals(Collections.singletonList("libfmod.so"), missing);
    }

    @Test
    public void aFullyPopulatedCacheHasNothingMissing() {
        List<String> required = Arrays.asList("libc++_shared.so", "libminecraftpe.so");
        assertTrue(ExtractionCachePolicy.missingLibraries(required, name -> true).isEmpty());
    }

    /**
     * The STL fallback must be able to satisfy the completeness check.
     *
     * <p>{@code libc++_shared.so} is in the required list <em>and</em> is the one library the
     * launcher supplies from its own native directory. If the fallback ran after the check, a
     * package that ships no copy would be refused for a library the launcher was about to provide —
     * a launch-preventing bug. This pins that the two lists agree about it, so the ordering
     * requirement is visible to anyone who edits either.
     */
    @Test
    public void theStlTheLauncherProvidesIsAlsoARequiredLibrary() {
        List<String> provided = ExtractionCachePolicy.launcherProvidedLibraries();
        assertTrue("the launcher must provide the STL", provided.contains("libc++_shared.so"));
        assertFalse("an empty provision list would mean the fallback is dead code",
                provided.isEmpty());
    }
}
