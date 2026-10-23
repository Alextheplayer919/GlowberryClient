package org.chimeramc.client.core.antegg;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Pins the dependency version-range grammar (item G4). */
public class VersionRangeTest {

    private static boolean matches(String range, String version) {
        VersionRange parsed = VersionRange.parse(range);
        return parsed != null && parsed.matches(version);
    }

    @Test
    public void anyRangeMatchesEverything() {
        assertTrue(VersionRange.parse(null).isAny());
        assertTrue(VersionRange.parse("").isAny());
        assertTrue(VersionRange.parse("*").isAny());
        assertTrue(matches("", "0.0.1"));
        assertTrue(matches("*", "99.9.9"));
    }

    @Test
    public void exactVersionMatchesExactly() {
        assertTrue(matches("1.2.0", "1.2.0"));
        assertFalse(matches("1.2.0", "1.2.1"));
        assertFalse(matches("1.2.0", "1.3.0"));
        assertFalse(matches("1.2.0", "0.9.0"));
    }

    @Test
    public void comparisonOperators() {
        assertTrue(matches(">=1.2.0", "1.2.0"));
        assertTrue(matches(">=1.2.0", "2.0.0"));
        assertFalse(matches(">=1.2.0", "1.1.9"));
        assertTrue(matches(">1.2.0", "1.2.1"));
        assertFalse(matches(">1.2.0", "1.2.0"));
        assertTrue(matches("<=1.2.0", "1.2.0"));
        assertTrue(matches("<=1.2.0", "0.5.0"));
        assertFalse(matches("<=1.2.0", "1.3.0"));
        assertTrue(matches("<1.2.0", "1.1.9"));
        assertFalse(matches("<1.2.0", "1.2.0"));
        assertTrue(matches("==1.2.0", "1.2.0"));
        assertFalse(matches("==1.2.0", "1.2.1"));
    }

    @Test
    public void tildeIsMinorCompatible() {
        assertTrue(matches("~1.2.0", "1.2.0"));
        assertTrue(matches("~1.2.0", "1.2.9"));
        assertFalse(matches("~1.2.0", "1.3.0"));
        assertFalse(matches("~1.2.0", "1.1.0"));
    }

    @Test
    public void caretIsMajorCompatible() {
        assertTrue(matches("^1.2.0", "1.2.0"));
        assertTrue(matches("^1.2.0", "1.9.9"));
        assertFalse(matches("^1.2.0", "2.0.0"));
        assertFalse(matches("^1.2.0", "1.1.0"));

        // 0.x caret semantics
        assertTrue(matches("^0.2.3", "0.2.9"));
        assertFalse(matches("^0.2.3", "0.3.0"));
        assertTrue(matches("^0.0.3", "0.0.3"));
        assertFalse(matches("^0.0.3", "0.0.4"));
    }

    @Test
    public void wildcardVersions() {
        assertTrue(matches("1.x", "1.0.0"));
        assertTrue(matches("1.x", "1.9.9"));
        assertFalse(matches("1.x", "2.0.0"));
        assertTrue(matches("1.2.x", "1.2.0"));
        assertFalse(matches("1.2.x", "1.3.0"));
    }

    @Test
    public void combinedConstraints() {
        assertTrue(matches(">=1.0.0 && <2.0.0", "1.5.0"));
        assertFalse(matches(">=1.0.0 && <2.0.0", "2.0.0"));
        assertFalse(matches(">=1.0.0 && <2.0.0", "0.9.0"));
    }

    @Test
    public void malformedRangesAreRejected() {
        assertNull(VersionRange.parse("banana"));
        assertNull(VersionRange.parse(">=banana"));
        assertNull(VersionRange.parse("~"));
        assertNull(VersionRange.parse("1.x.x.y"));
        assertNull(VersionRange.parse(">=1.x"));
    }
}