package org.chimeramc.client.core.antegg;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 digesting for mod hash pinning (item G12).
 *
 * <p>A mod that declares {@code sha256} in its manifest is asserting a specific byte-for-byte
 * identity for its entry file (a native mod's entry {@code .so}, or an .AntEgg package's entry
 * {@code .so}/{@code .lua}). This class computes the digest a loader needs to check that claim
 * and compares it hex-insensitively, so a manifest with an uppercase hash matches a lowercase
 * computed digest and vice versa. Android-free, so the rules are JVM-testable.
 */
public final class ModHash {

    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private static final int BUFFER_SIZE = 65536;

    private ModHash() {
    }

    /**
     * Computes the lowercase hex SHA-256 of a file.
     *
     * @return the 64-character hex digest, or null if the file is unreadable
     */
    public static String sha256(File file) {
        if (file == null || !file.isFile()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = new FileInputStream(file)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available on this JVM", e);
        } catch (IOException e) {
            return null;
        }
    }

    /** Lowercase hex SHA-256 of an in-memory byte array (the entry bytes of a package). */
    public static String sha256(byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available on this JVM", e);
        }
    }

    /** Whether a declared hex digest matches the file's computed digest. A null/blank declared hash never matches. */
    public static boolean matches(File file, String declaredHex) {
        String declared = normalize(declaredHex);
        if (declared == null) {
            return false;
        }
        String actual = sha256(file);
        return actual != null && actual.equals(declared);
    }

    /** True when the string is a 64-character lowercase or uppercase hex SHA-256. */
    public static boolean isValidDigest(String hex) {
        String value = normalize(hex);
        if (value == null) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean hexDigit = (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F');
            if (!hexDigit) {
                return false;
            }
        }
        return true;
    }

    /** Lowercases a hex digest and validates its length; null when blank or not 64 chars. */
    private static String normalize(String hex) {
        if (hex == null) {
            return null;
        }
        String trimmed = hex.trim();
        if (trimmed.isEmpty() || trimmed.length() != 64) {
            return null;
        }
        return trimmed.toLowerCase(java.util.Locale.ROOT);
    }

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xFF;
            out[i * 2] = HEX[value >>> 4];
            out[i * 2 + 1] = HEX[value & 0x0F];
        }
        return new String(out);
    }
}