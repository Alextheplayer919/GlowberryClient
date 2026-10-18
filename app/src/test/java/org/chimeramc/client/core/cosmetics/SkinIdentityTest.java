package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the Phase 1 skin-identity rules: a capture that disagrees with the attributed account skin is
 * never presented as the player's own, and a capture with nothing to compare against is shown but
 * labelled unverified. Pure JVM — no device, no bitmap.
 */
public class SkinIdentityTest {

    private static int[] atlas(int fill) {
        int[] a = new int[64 * 64];
        java.util.Arrays.fill(a, fill);
        return a;
    }

    @Test
    public void anIdenticalAtlasMatches() {
        assertEquals(SkinIdentity.Verdict.MATCH,
                SkinIdentity.compare(atlas(0xFF3A7BD5), atlas(0xFF3A7BD5)));
    }

    @Test
    public void encoderRoundingStillMatches() {
        // A few channels off by the encoder's rounding must not read as a different skin.
        int[] a = atlas(0xFF3A7BD5);
        int[] b = atlas(0xFF3A7BD5);
        for (int i = 0; i < 64; i++) {
            b[i] = 0xFF3A7BD8;
        }
        assertEquals(SkinIdentity.Verdict.MATCH, SkinIdentity.compare(a, b));
    }

    @Test
    public void aGenuinelyDifferentSkinIsAMismatch() {
        // Half the atlas a clearly different colour is far past the tolerance.
        int[] a = atlas(0xFF3A7BD5);
        int[] b = atlas(0xFF3A7BD5);
        for (int i = 0; i < b.length / 2; i++) {
            b[i] = 0xFFE02020;
        }
        assertEquals(SkinIdentity.Verdict.MISMATCH, SkinIdentity.compare(a, b));
    }

    @Test
    public void transparentRegionsAreIgnored() {
        // Both invisible: a fully transparent pixel differs in colour but not visibly.
        int[] a = atlas(0x00000000);
        int[] b = atlas(0x00FFFFFF);
        // Nothing visible to compare at all.
        assertEquals(SkinIdentity.Verdict.UNVERIFIED, SkinIdentity.compare(a, b));
    }

    @Test
    public void aMissingSideIsUnverified() {
        assertEquals(SkinIdentity.Verdict.UNVERIFIED, SkinIdentity.compare(atlas(0xFF112233), null));
        assertEquals(SkinIdentity.Verdict.UNVERIFIED, SkinIdentity.compare(null, atlas(0xFF112233)));
    }

    @Test
    public void anImportAlwaysWinsAndIsAttributed() {
        SkinIdentity.Choice c = SkinIdentity.resolve(
                true, true, true, SkinIdentity.Verdict.MATCH, true, true);
        assertEquals(SkinResolution.Source.IMPORTED, c.source);
        assertTrue(c.attributed);
    }

    @Test
    public void theAccountSkinBeatsAnUnverifiedCapture() {
        SkinIdentity.Choice c = SkinIdentity.resolve(
                false, true, true, SkinIdentity.Verdict.UNVERIFIED, true, true);
        assertEquals(SkinResolution.Source.ACCOUNT, c.source);
        assertTrue(c.attributed);
    }

    @Test
    public void aMatchingCaptureIsPresentedAsThePlayersOwn() {
        SkinIdentity.Choice c = SkinIdentity.resolve(
                false, false, true, SkinIdentity.Verdict.MATCH, true, true);
        assertEquals(SkinResolution.Source.GAME, c.source);
        assertTrue(c.attributed);
        assertFalse(c.label.contains("unverified"));
    }

    @Test
    public void anUnverifiedCaptureIsShownButLabelledUnverified() {
        SkinIdentity.Choice c = SkinIdentity.resolve(
                false, false, true, SkinIdentity.Verdict.UNVERIFIED, true, true);
        assertEquals(SkinResolution.Source.GAME, c.source);
        assertFalse("an unverified capture is not attributed", c.attributed);
        assertTrue(c.label.contains("unverified"));
    }

    @Test
    public void aMismatchedCaptureIsNotShownAsThePlayersOwn() {
        // A capture that disagrees with the account skin must fall to the attributed account skin.
        SkinIdentity.Choice c = SkinIdentity.resolve(
                false, true, true, SkinIdentity.Verdict.MISMATCH, true, true);
        assertEquals(SkinResolution.Source.ACCOUNT, c.source);
        assertTrue(c.attributed);
    }

    @Test
    public void aMismatchWithNoAccountSkinFallsToTheAppliedPack() {
        SkinIdentity.Choice c = SkinIdentity.resolve(
                false, false, true, SkinIdentity.Verdict.MISMATCH, true, true);
        assertEquals(SkinResolution.Source.APPLIED_PACK, c.source);
        assertFalse(c.attributed);
    }

    @Test
    public void steveIsTheDefaultOnlyWhenEveryRealSourceFails() {
        SkinIdentity.Choice c = SkinIdentity.resolve(
                false, false, false, SkinIdentity.Verdict.UNVERIFIED, false, true);
        assertEquals(SkinResolution.Source.STEVE, c.source);
        assertFalse(c.attributed);
        assertTrue(c.label.contains("default"));
    }

    @Test
    public void thePlaceholderIsTheLastResort() {
        SkinIdentity.Choice c = SkinIdentity.resolve(
                false, false, false, SkinIdentity.Verdict.UNVERIFIED, false, false);
        assertEquals(SkinResolution.Source.PLACEHOLDER, c.source);
    }
}
