package org.chimeramc.client.core.antegg;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the SHA-256 pinning helper (item G12). Known digests for known input, cross-checked
 * against an independent implementation so the test never locks in the same bug the helper has.
 */
public class ModHashTest {

    private static final String EMPTY_DIGEST =
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String REAL_HELLO_DIGEST =
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

    @Test
    public void sha256MatchesABlankFile() throws Exception {
        File file = File.createTempFile("hash", ".bin");
        try {
            assertEquals(EMPTY_DIGEST, ModHash.sha256(file));
        } finally {
            file.delete();
        }
    }

    @Test
    public void sha256MatchesKnownInput() throws Exception {
        File file = File.createTempFile("hash", ".bin");
        try {
            Files.write(file.toPath(), "hello".getBytes(StandardCharsets.UTF_8));
            assertEquals(REAL_HELLO_DIGEST, ModHash.sha256(file));
        } finally {
            file.delete();
        }
    }

    @Test
    public void matchesIsCaseInsensitiveAndAcceptsUppercasePins() throws Exception {
        File file = File.createTempFile("hash", ".bin");
        try {
            Files.write(file.toPath(), "hello".getBytes(StandardCharsets.UTF_8));
            assertTrue(ModHash.matches(file, REAL_HELLO_DIGEST.toUpperCase()));
            assertTrue(ModHash.matches(file, REAL_HELLO_DIGEST));
        } finally {
            file.delete();
        }
    }

    @Test
    public void mismatchedBytesDoNotMatch() throws Exception {
        File file = File.createTempFile("hash", ".bin");
        try {
            Files.write(file.toPath(), "world".getBytes(StandardCharsets.UTF_8));
            assertFalse(ModHash.matches(file, REAL_HELLO_DIGEST));
        } finally {
            file.delete();
        }
    }

    @Test
    public void aMissingFileNeverMatchesAndHasNoDigest() {
        File missing = new File("/nonexistent/does-not-exist.bin");
        assertNull(ModHash.sha256(missing));
        assertFalse(ModHash.matches(missing, REAL_HELLO_DIGEST));
    }

    @Test
    public void aBlankPinNeverMatches() throws Exception {
        File file = File.createTempFile("hash", ".bin");
        try {
            Files.write(file.toPath(), "hello".getBytes(StandardCharsets.UTF_8));
            assertFalse(ModHash.matches(file, ""));
            assertFalse(ModHash.matches(file, "   "));
            assertFalse(ModHash.matches(file, null));
        } finally {
            file.delete();
        }
    }

    @Test
    public void sha256OfBytesMatchesFileBytes() throws Exception {
        File file = File.createTempFile("hash", ".bin");
        try {
            byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);
            Files.write(file.toPath(), bytes);
            assertEquals(ModHash.sha256(file), ModHash.sha256(bytes));
        } finally {
            file.delete();
        }
    }

    @Test
    public void sha256OfNullBytesIsNull() {
        assertNull(ModHash.sha256((byte[]) null));
    }

    @Test
    public void isValidDigestAcceptsOnly64Hex() {
        assertTrue(ModHash.isValidDigest(REAL_HELLO_DIGEST));
        assertTrue(ModHash.isValidDigest(REAL_HELLO_DIGEST.toUpperCase()));
        assertFalse(ModHash.isValidDigest("short"));
        assertFalse(ModHash.isValidDigest("g".repeat(64))); // g is not hex
        assertFalse(ModHash.isValidDigest(""));
        assertFalse(ModHash.isValidDigest(null));
        assertFalse(ModHash.isValidDigest(REAL_HELLO_DIGEST + "f"));
    }
}