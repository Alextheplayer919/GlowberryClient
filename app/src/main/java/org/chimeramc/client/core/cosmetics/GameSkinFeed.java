package org.chimeramc.client.core.cosmetics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.chimeramc.client.preloader.PreloaderInput;

/**
 * The engine-backed provider for {@link GameSkinSource}: reads the player's real Minecraft skin from
 * the running game and hands it to the preview.
 *
 * <p>The native side captures the skin at the engine's image-loader seam (see the preloader's
 * {@code mce_image_hook}), where the game hands over the source bytes of every image it builds,
 * including the player's base skin. {@link PreloaderInput#nativeReadCapturedSkin()} returns those
 * bytes with a small header; this class decodes them (a PNG, or raw RGBA8) and normalises to the
 * 64x64 atlas the preview samples.
 *
 * <p>Honest scope: the loader seam carries no player id, so this is <b>content capture, not
 * attribution</b> — the texture is the most recent base-skin-shaped upload, which in practice is the
 * local player's skin, but it is not proven to be. The provider is fail-closed: any decode failure
 * returns null and {@link PlayerSkinProvider} falls back to the imported / applied / Steve skin.
 */
public final class GameSkinFeed implements GameSkinSource.Provider {

    /** The 16-byte header the native capture prepends: width, height, format, stampMs. */
    private static final int HEADER_BYTES = 16;
    /** The native image-loader format value for a PNG payload (auto-detect / stb_image). */
    private static final int FORMAT_PNG = 0;
    /** The native image-loader format value for raw RGBA8. */
    private static final int FORMAT_RGBA8 = 4;

    private static GameSkinFeed instance;

    private GameSkinFeed() {
    }

    /** Installs the feed as the active provider. Idempotent. */
    public static void install() {
        if (instance == null) instance = new GameSkinFeed();
        GameSkinSource.setProvider(instance);
    }

    /** Removes the feed and discards the native capture. */
    public static void uninstall() {
        GameSkinSource.clearProvider();
        try {
            PreloaderInput.nativeClearCapturedSkin();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public PlayerSkinProvider.SkinBitmap read(Context context) {
        try {
            if (!PreloaderInput.nativeHasCapturedSkin()) return null;
            byte[] data = PreloaderInput.nativeReadCapturedSkin();
            return decode(data);
        } catch (Throwable t) {
            // Fail-closed: a native read failure is "no skin", never a crash.
            return null;
        }
    }

    /**
     * Decodes the captured payload. Pure enough to unit-test the header parse: the bitmap decode
     * itself needs Android, so the header validation is split out into {@link #parseHeader}.
     */
    PlayerSkinProvider.SkinBitmap decode(byte[] data) {
        int[] header = parseHeader(data);
        if (header == null) return null;
        int width = header[0];
        int height = header[1];
        int format = header[2];
        byte[] payload = new byte[data.length - HEADER_BYTES];
        System.arraycopy(data, HEADER_BYTES, payload, 0, payload.length);

        Bitmap decoded = decodePayload(payload, width, height, format);
        if (decoded == null) return null;
        return new PlayerSkinProvider.SkinBitmap(PlayerSkinProvider.normalise(decoded), false, false,
                "Your Minecraft skin");
    }

    /**
     * Validates and parses the 16-byte little-endian header, or null when the buffer is too short or
     * the dimensions are not a standard skin size.
     */
    static int[] parseHeader(byte[] data) {
        if (data == null || data.length <= HEADER_BYTES) return null;
        int width = readInt(data, 0);
        int height = readInt(data, 4);
        int format = readInt(data, 8);
        if (width != height || (width != 64 && width != 128 && width != 256 && width != 512)) {
            return null;
        }
        if (format != FORMAT_PNG && format != FORMAT_RGBA8) return null;
        return new int[]{width, height, format};
    }

    private static int readInt(byte[] data, int offset) {
        return (data[offset] & 0xFF)
                | ((data[offset + 1] & 0xFF) << 8)
                | ((data[offset + 2] & 0xFF) << 16)
                | ((data[offset + 3] & 0xFF) << 24);
    }

    /** Decodes a PNG or a raw RGBA8 buffer into a bitmap, or null. */
    private static Bitmap decodePayload(byte[] payload, int width, int height, int format) {
        try {
            if (format == FORMAT_PNG) {
                return BitmapFactory.decodeByteArray(payload, 0, payload.length);
            }
            if (format == FORMAT_RGBA8) {
                if (payload.length < width * height * 4) return null;
                int[] argb = new int[width * height];
                for (int i = 0; i < argb.length; i++) {
                    int r = payload[i * 4] & 0xFF;
                    int g = payload[i * 4 + 1] & 0xFF;
                    int b = payload[i * 4 + 2] & 0xFF;
                    int a = payload[i * 4 + 3] & 0xFF;
                    argb[i] = (a << 24) | (r << 16) | (g << 8) | b;
                }
                return Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
