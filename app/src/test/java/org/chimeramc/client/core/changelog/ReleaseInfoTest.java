package org.chimeramc.client.core.changelog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the release-dating rules (item C2): a declared release renders its date and support window,
 * a half-declared one renders the half it has, and an undated one renders nothing rather than a
 * fabricated date. No mocks — the class is pure.
 */
public class ReleaseInfoTest {

    @Test
    public void aFullyDatedReleaseRendersBothHalves() {
        ReleaseInfo info = new ReleaseInfo("2026-10-23T00:00:00Z", "2027-04-23T00:00:00Z");
        assertEquals("Released 23 Oct 2026 \u00b7 supported until 23 Apr 2027", info.summaryLine());
    }

    @Test
    public void aHalfDatedReleaseRendersOnlyWhatItHas() {
        assertEquals("Released 23 Oct 2026",
                new ReleaseInfo("2026-10-23T00:00:00Z", "").summaryLine());
        assertEquals("Supported until 23 Apr 2027",
                new ReleaseInfo("", "2027-04-23T00:00:00Z").summaryLine());
    }

    @Test
    public void anUndatedReleaseRendersNothing() {
        assertEquals("", new ReleaseInfo(null, null).summaryLine());
        assertEquals("", new ReleaseInfo("", "   ").summaryLine());
        assertTrue(new ReleaseInfo(null, null).isEmpty());
        assertFalse(new ReleaseInfo("2026-10-23T00:00:00Z", null).isEmpty());
    }

    @Test
    public void anUnparseableTimestampIsOmittedRatherThanPrinted() {
        // A malformed date must not leak into the UI as raw text.
        assertEquals("Supported until 23 Apr 2027",
                new ReleaseInfo("not a date", "2027-04-23T00:00:00Z").summaryLine());
        assertNull(ReleaseInfo.parseEpochMillis("not a date"));
        assertEquals("", ReleaseInfo.formatDay("2026-13-45T00:00:00Z"));
    }

    @Test
    public void aBareTimestampWithoutTheZStillParses() {
        // The schema requires Z, but the reader should not fall over on a bare form.
        assertEquals(ReleaseInfo.parseEpochMillis("2026-10-23T00:00:00Z"),
                ReleaseInfo.parseEpochMillis("2026-10-23T00:00:00"));
    }

    @Test
    public void supportExpiryIsMeasuredAgainstTheDeclaredEnd() {
        ReleaseInfo info = new ReleaseInfo("2026-10-23T00:00:00Z", "2027-04-23T00:00:00Z");
        long before = ReleaseInfo.parseEpochMillis("2027-01-01T00:00:00Z");
        long after = ReleaseInfo.parseEpochMillis("2027-06-01T00:00:00Z");
        assertFalse(info.isSupportExpired(before));
        assertTrue(info.isSupportExpired(after));
        // An undated release is never reported as expired — that would be a fabricated claim.
        assertFalse(new ReleaseInfo(null, null).isSupportExpired(after));
    }
}