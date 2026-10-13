package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Pins the captured-skin header parse in {@link GameSkinFeed}. The decode itself needs Android, but
 * the header validation is pure and is the part that protects against a malformed native buffer.
 */
public class GameSkinFeedTest {

    /** Builds the native byte[] layout: 16-byte LE header + payload. */
    private static byte[] capture(int width, int height, int format, int payloadBytes) {
        byte[] out = new byte[16 + payloadBytes];
        putLe(out, 0, width);
        putLe(out, 4, height);
        putLe(out, 8, format);
        putLe(out, 12, 1234);
        return out;
    }

    private static void putLe(byte[] out, int offset, int value) {
        out[offset] = (byte) (value & 0xFF);
        out[offset + 1] = (byte) ((value >> 8) & 0xFF);
        out[offset + 2] = (byte) ((value >> 16) & 0xFF);
        out[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }

    @Test
    public void aStandardSkinHeaderParses() {
        int[] header = GameSkinFeed.parseHeader(capture(64, 64, 4, 64 * 64 * 4));
        assertNotNull(header);
        assertArrayEquals(new int[]{64, 64, 4}, header);
        assertNotNull(GameSkinFeed.parseHeader(capture(128, 128, 0, 512)));
        assertNotNull(GameSkinFeed.parseHeader(capture(256, 256, 4, 256 * 256 * 4)));
        assertNotNull(GameSkinFeed.parseHeader(capture(512, 512, 0, 4096)));
    }

    @Test
    public void aNonSquareOrOddSizeIsRejected() {
        assertNull(GameSkinFeed.parseHeader(capture(64, 32, 0, 100)));
        assertNull(GameSkinFeed.parseHeader(capture(48, 48, 0, 100)));
        assertNull(GameSkinFeed.parseHeader(capture(1024, 1024, 0, 100)));
    }

    @Test
    public void anUnknownFormatIsRejected() {
        assertNull(GameSkinFeed.parseHeader(capture(64, 64, 99, 100)));
    }

    @Test
    public void aTruncatedOrEmptyBufferIsRejected() {
        assertNull(GameSkinFeed.parseHeader(null));
        assertNull(GameSkinFeed.parseHeader(new byte[0]));
        assertNull(GameSkinFeed.parseHeader(new byte[16]));   // header only, no payload
    }
}
