package org.chimeramc.client.core.jni;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

/**
 * Covers the binary-safe decode used for native byte buffers (no mocks).
 *
 * <p>The native {@code pl::jni::NewString} wrapper is the primary defence against the
 * {@code illegal start byte} abort; this pins the Java half so a path payload that is not valid
 * UTF-8 can be detected rather than silently renamed.
 */
public class JniStringsTest {

    @Test
    public void asciiAndUtf8AreValid() {
        assertTrue(JniStrings.isValidUtf8("hello/world.apk".getBytes(StandardCharsets.UTF_8)));
        assertTrue(JniStrings.isValidUtf8("caf\u00e9".getBytes(StandardCharsets.UTF_8)));
        assertTrue(JniStrings.isValidUtf8(new byte[0]));
    }

    @Test
    public void loneContinuationByteIsRejected() {
        // The reported crash shape: an 0x87 byte (a continuation with no lead byte).
        byte[] bad = new byte[] {'p', 'a', 't', 'h', (byte) 0x87, 'n'};
        assertFalse(JniStrings.isValidUtf8(bad));
    }

    @Test
    public void decodeUtf8NeverThrowsAndSubstitutes() {
        byte[] bad = new byte[] {(byte) 0x87};
        assertEquals("\uFFFD", JniStrings.decodeUtf8(bad));
        assertEquals("", JniStrings.decodeUtf8(null));
        assertEquals("", JniStrings.decodeUtf8(new byte[0]));
    }

    @Test
    public void decodePathRefusesMalformedBytes() {
        assertNull(JniStrings.decodePath(new byte[] {(byte) 0x87}));
        assertNull(JniStrings.decodePath(null));
        assertEquals("/sdcard/game.apk",
                JniStrings.decodePath("/sdcard/game.apk".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void nulDetection() {
        assertTrue(JniStrings.containsNul(new byte[] {'a', 0, 'b'}));
        assertFalse(JniStrings.containsNul("abc".getBytes(StandardCharsets.UTF_8)));
        assertFalse(JniStrings.containsNul(null));
    }

    @Test
    public void truncatedSequenceIsRejected() {
        // 0xC3 is a 2-byte lead; without its continuation the buffer is malformed.
        assertFalse(JniStrings.isValidUtf8(new byte[] {(byte) 0xC3}));
    }
}
