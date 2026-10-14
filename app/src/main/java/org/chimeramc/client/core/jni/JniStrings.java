package org.chimeramc.client.core.jni;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * Binary-safe decoding of native byte buffers into Java strings.
 *
 * <p>The native side hands the JVM text through {@code NewStringUTF}, which aborts the process on a
 * malformed buffer (the {@code illegal start byte} crash). The primary fix is the native
 * {@code pl::jni::NewString} wrapper; this is the second half of the same rule for the cases where a
 * payload is genuinely arbitrary bytes — a file name whose on-disk encoding is not UTF-8, or a blob
 * that is not text at all — and round-tripping it through a C string would either corrupt it or
 * crash.
 *
 * <p>Where the platform's own {@code new String(bytes, UTF_8)} substitutes U+FFFD for a malformed
 * sequence, {@link #isValidUtf8} lets a caller tell the difference between "decoded, with
 * replacements" and "this was never text", so a path can be rejected instead of silently renamed.
 */
public final class JniStrings {

    private JniStrings() {}

    /** True when {@code bytes} is a well-formed UTF-8 sequence (no replacement characters needed). */
    public static boolean isValidUtf8(byte[] bytes) {
        if (bytes == null) return false;
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            decoder.decode(java.nio.ByteBuffer.wrap(bytes));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    /**
     * Decodes a native buffer to a string, substituting U+FFFD for anything malformed.
     *
     * <p>Never throws: this is the decode used for display text, where a replacement character is
     * preferable to losing the whole message.
     */
    public static String decodeUtf8(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Decodes a native buffer that must be a path.
     *
     * @return the decoded path, or null when the bytes are not valid UTF-8 (so the caller can refuse
     *         the path rather than open a renamed file)
     */
    public static String decodePath(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        return isValidUtf8(bytes) ? new String(bytes, StandardCharsets.UTF_8) : null;
    }

    /** True when the buffer holds a NUL byte, which would truncate a C-string round trip. */
    public static boolean containsNul(byte[] bytes) {
        if (bytes == null) return false;
        for (byte b : bytes) {
            if (b == 0) return true;
        }
        return false;
    }
}
