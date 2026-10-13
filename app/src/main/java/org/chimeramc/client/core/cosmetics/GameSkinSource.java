package org.chimeramc.client.core.cosmetics;

import android.content.Context;

/**
 * The seam through which the launcher reads the player's <b>actual</b> Minecraft skin from the
 * running game, rather than an imported file or the built-in Steve.
 *
 * <p>The intended native route is an engine hook on the local player: the client already latches the
 * live {@code LocalPlayer} (see the preloader's {@code GameLocalPlayer}); the skin would be read from
 * the player's {@code SerializedSkinRef} base-skin {@code mce::Image} at the recovered {@code +0x78}
 * offset. That is the same {@code mce::Image} the cape work documents: the struct's internal pixel
 * pointer is <em>not</em> recoverable from the stripped binary, so a pixel read is not reachable from
 * Java today. This class is therefore the honest seam: a provider can be installed (by a future
 * native feed), and until one is, {@link #read} returns null and {@link PlayerSkinProvider} falls
 * back to the imported skin / applied pack / Steve.
 *
 * <p>It is deliberately fail-closed and allocation-light: a throwing or null provider reads as "no
 * game skin", never as a fabricated one, so the preview and the radial hub can never show a
 * stranger's face because of a bad read.
 */
public final class GameSkinSource {

    /** Supplies the game's live skin. Returns null when it cannot read one. */
    public interface Provider {
        PlayerSkinProvider.SkinBitmap read(Context context);
    }

    private static volatile Provider provider;

    private GameSkinSource() {
    }

    /** Installs the provider (a native feed installs one at session start). */
    public static void setProvider(Provider p) {
        provider = p;
    }

    /** Clears the provider; {@link #read} then returns null. */
    public static void clearProvider() {
        provider = null;
    }

    /** True when a provider is installed (whether or not it can currently read a skin). */
    public static boolean hasProvider() {
        return provider != null;
    }

    /**
     * Reads the game's live skin, or null when there is no provider or the read failed. Never
     * throws: a provider that throws is treated as "no skin".
     */
    public static PlayerSkinProvider.SkinBitmap read(Context context) {
        Provider p = provider;
        if (p == null) return null;
        try {
            return p.read(context);
        } catch (Throwable t) {
            return null;
        }
    }
}
