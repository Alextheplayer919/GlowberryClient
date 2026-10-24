package org.chimeramc.client.core.mods;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Pins the crash-attribution counting rule (item G8).
 *
 * <p>The full store touches {@code SharedPreferences}, which does not exist in a JVM test; the
 * counting decision itself is pure ({@link ModCrashAttribution#advanceCount}), so the same-set
 * advancement, the changed-set restart, and the auto-disable threshold are verified here without
 * a device or mocks.
 */
public class ModCrashAttributionTest {

    private static Set<String> set(String... ids) {
        return new LinkedHashSet<>(Arrays.asList(ids));
    }

    @Test
    public void firstCrashStartsAtOne() {
        assertEquals(1, ModCrashAttribution.advanceCount(0, Collections.<String>emptySet(), set("a")));
    }

    @Test
    public void sameSuspectsAdvanceTheCounter() {
        Set<String> suspects = set("a", "b");
        // Launch 1 crashed (count becomes 1), launch 2 crashed again with the same set.
        assertEquals(1, ModCrashAttribution.advanceCount(0, Collections.<String>emptySet(), suspects));
        assertEquals(2, ModCrashAttribution.advanceCount(1, suspects, suspects));
    }

    @Test
    public void aChangedSuspectSetRestartsTheCounter() {
        // Launch 1 crashed with {a,b}; launch 2 crashed with {a,c} — different mod took it down
        // (or the set changed), so the count must restart rather than compound across sets.
        Set<String> first = set("a", "b");
        Set<String> second = set("a", "c");
        assertEquals(1, ModCrashAttribution.advanceCount(0, Collections.<String>emptySet(), first));
        assertEquals(1, ModCrashAttribution.advanceCount(1, first, second));
    }

    @Test
    public void anEmptySuspectSetDoesNotCompound() {
        Set<String> suspects = set("a");
        assertEquals(1, ModCrashAttribution.advanceCount(2, suspects, Collections.<String>emptySet()));
        assertEquals(1, ModCrashAttribution.advanceCount(2, suspects, null));
    }

    @Test
    public void thresholdIsTwoSoTwoSameSetCrashesAutoDisable() {
        assertEquals(2, ModCrashAttribution.AUTO_DISABLE_THRESHOLD);
    }

    @Test
    public void nullPreviousSetIsTreatedAsFirstCrash() {
        assertEquals(1, ModCrashAttribution.advanceCount(0, null, set("m")));
        assertEquals(2, ModCrashAttribution.advanceCount(1, set("m"), set("m")));
    }
}