package org.chimeramc.client.launcher.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the release-tag ordering the updater relies on.
 *
 * <p>Every case here is a tag this project actually publishes. The old rule split on {@code "."}
 * and parsed each part as an int, so {@code BETA5} and {@code OV.2} both threw
 * {@code NumberFormatException} and the update check died silently — a regression that is invisible
 * unless the tag vocabulary is exercised.
 */
public class UpdateVersionTest {

    @Test
    public void aNamedTagDoesNotThrow() {
        // The exact input that used to raise NumberFormatException: "BETA5" has no dot to split on,
        // so the single part was handed to Integer.parseInt and failed.
        assertEquals(1, UpdateVersion.compare("BETA5", "OV.2"));
        assertEquals(1, UpdateVersion.compare("OV.2", "OV.1"));
    }

    @Test
    public void namedTagsOrderByTheirNumber() {
        // Publish order on the repository: OV.1, OV.2, BETA3, BETA4, BETA5.
        assertTrue(UpdateVersion.isNewer("BETA5", "BETA3"));
        assertTrue(UpdateVersion.isNewer("BETA3", "OV.2"));
        assertTrue(UpdateVersion.isNewer("OV.2", "OV.1"));
        assertFalse(UpdateVersion.isNewer("OV.1", "OV.2"));
    }

    @Test
    public void dottedNumericTagsStillCompareComponentWise() {
        assertEquals(-1, UpdateVersion.compare("1.2.3", "1.2.4"));
        assertEquals(1, UpdateVersion.compare("1.26.50", "1.26.33"));
        assertEquals(0, UpdateVersion.compare("1.2.3", "1.2.3"));
    }

    @Test
    public void aLeadingVIsIgnored() {
        assertEquals(0, UpdateVersion.compare("v1.2.3", "1.2.3"));
        assertEquals(1, UpdateVersion.compare("v1.2.4", "1.2.3"));
    }

    @Test
    public void aMissingComponentCountsAsZero() {
        assertEquals(0, UpdateVersion.compare("BETA5", "5.0"));
        assertEquals(0, UpdateVersion.compare("1.2", "1.2.0"));
        assertEquals(1, UpdateVersion.compare("1.2.1", "1.2"));
    }

    @Test
    public void aTagWithNoDigitsIsOlderThanOneWithADigits() {
        // A stray name must not be able to hide a real numbered release.
        assertEquals(-1, UpdateVersion.compare("nightly", "BETA5"));
        assertEquals(1, UpdateVersion.compare("BETA5", "nightly"));
        assertEquals(0, UpdateVersion.compare("nightly", "stable"));
    }

    @Test
    public void aHugeComponentSaturatesInsteadOfWrappingNegative() {
        // A wrapped negative part would read as an older release and silently suppress the update.
        assertTrue(UpdateVersion.isNewer("99999999999999999999", "1"));
    }

    @Test
    public void nullAndEmptyInputsAreSafe() {
        assertEquals(0, UpdateVersion.compare(null, null));
        assertEquals(1, UpdateVersion.compare("BETA5", null));
        assertEquals(-1, UpdateVersion.compare(null, "BETA5"));
        assertEquals(0, UpdateVersion.compare("", ""));
    }
}
