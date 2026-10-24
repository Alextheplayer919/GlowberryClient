package org.chimeramc.client.core.antegg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The share-sheet / link import rules (F1). Pure, no mocks: these decide whether a pasted string
 * becomes a network fetch, so each refusal is pinned rather than left to the caller.
 */
public class AntEggLinkTest {

    @Test
    public void anHttpsLinkToAPackageIsImportable() {
        assertTrue(AntEggLink.isImportable("https://example.com/mods/cool.antegg"));
        assertTrue(AntEggLink.isImportable("http://example.com/cool.antegg"));
    }

    @Test
    public void theExtensionMatchIsCaseInsensitive() {
        assertTrue(AntEggLink.isImportable("https://example.com/cool.AntEgg"));
        assertEquals("cool.AntEgg", AntEggLink.fileNameOf("https://example.com/cool.AntEgg"));
    }

    /**
     * A link must name the package. Without this the importer becomes "fetch any URL and run
     * whatever comes back", which defeats the archive validation the loader exists to perform.
     */
    @Test
    public void aLinkThatDoesNotNameAPackageIsRefused() {
        assertFalse(AntEggLink.isImportable("https://example.com/"));
        assertFalse(AntEggLink.isImportable("https://example.com/mod.zip"));
        assertFalse(AntEggLink.isImportable("https://example.com/download"));
        assertNull(AntEggLink.fileNameOf("https://example.com/mod.zip"));
    }

    /** Only http(s) is fetched; a file, content or script scheme is never turned into a request. */
    @Test
    public void onlyHttpAndHttpsAreFetchable() {
        assertTrue(AntEggLink.isFetchable("https://example.com/a.antegg"));
        assertTrue(AntEggLink.isFetchable("http://example.com/a.antegg"));
        assertFalse(AntEggLink.isFetchable("file:///sdcard/a.antegg"));
        assertFalse(AntEggLink.isFetchable("content://media/a.antegg"));
        assertFalse(AntEggLink.isFetchable("javascript:alert(1)//a.antegg"));
        assertFalse(AntEggLink.isFetchable("ftp://example.com/a.antegg"));
    }

    /**
     * A non-http scheme is refused even when it names a package, because the downloader speaks
     * HTTP and a file/content URI reaching it is a caller bug.
     */
    @Test
    public void aNonHttpSchemeIsRefusedEvenWithTheRightExtension() {
        assertFalse(AntEggLink.isImportable("file:///sdcard/a.antegg"));
        assertFalse(AntEggLink.isImportable("javascript:alert(1)//a.antegg"));
    }

    /** A query string or fragment must not hide the package name. */
    @Test
    public void aQueryStringOrFragmentDoesNotHideTheName() {
        assertEquals("cool.antegg",
                AntEggLink.fileNameOf("https://example.com/cool.antegg?utm_source=discord"));
        assertEquals("cool.antegg",
                AntEggLink.fileNameOf("https://example.com/cool.antegg#download"));
        assertTrue(AntEggLink.isImportable("https://example.com/cool.antegg?x=1&y=2"));
    }

    /** A share sheet usually hands over a whole message, not a bare link. */
    @Test
    public void theFirstImportableUrlIsScrapedFromSurroundingProse() {
        assertEquals("https://example.com/cool.antegg",
                AntEggLink.firstImportableUrl(
                        "try this mod https://example.com/cool.antegg it's great"));
    }

    /** Prose that mentions a non-package link first still resolves to the importable one. */
    @Test
    public void aNonPackageLinkIsSkippedInFavourOfAPackageLink() {
        assertEquals("https://example.com/cool.antegg",
                AntEggLink.firstImportableUrl(
                        "docs https://example.com/readme.html then https://example.com/cool.antegg"));
    }

    /** Trailing sentence punctuation rides along with a pasted link and must not break the match. */
    @Test
    public void trailingSentencePunctuationIsStripped() {
        assertEquals("https://example.com/cool.antegg",
                AntEggLink.firstImportableUrl("grab it here: https://example.com/cool.antegg."));
        assertEquals("https://example.com/cool.antegg",
                AntEggLink.firstImportableUrl("(https://example.com/cool.antegg)"));
    }

    @Test
    public void textWithNoImportableLinkIsRefused() {
        assertNull(AntEggLink.firstImportableUrl("just a normal message"));
        assertNull(AntEggLink.firstImportableUrl("see https://example.com/readme.html"));
        assertNull(AntEggLink.firstImportableUrl(null));
        assertFalse(AntEggLink.looksLikeImportText("hello"));
    }

    @Test
    public void looksLikeImportTextAgreesWithFirstImportableUrl() {
        assertTrue(AntEggLink.looksLikeImportText("https://example.com/cool.antegg"));
        assertFalse(AntEggLink.looksLikeImportText("https://example.com/cool.zip"));
    }

    /** A pathological paste is bounded rather than walked in full. */
    @Test
    public void anOversizedPasteIsBounded() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < AntEggLink.MAX_TEXT_LENGTH + 500; i++) sb.append('x');
        assertNull(AntEggLink.firstImportableUrl(sb.toString()));
    }

    /** Malformed input is refused rather than throwing. */
    @Test
    public void malformedInputIsRefusedNotThrown() {
        assertNull(AntEggLink.fileNameOf(null));
        assertNull(AntEggLink.fileNameOf(""));
        assertNull(AntEggLink.fileNameOf("::::"));
        assertFalse(AntEggLink.isImportable(null));
        assertEquals("", AntEggLink.schemeOf(null));
        assertEquals("", AntEggLink.schemeOf("no scheme here"));
        // A scheme with a character that cannot appear in one is not a scheme.
        assertEquals("", AntEggLink.schemeOf("ht tp://example.com/a.antegg"));
    }
}
