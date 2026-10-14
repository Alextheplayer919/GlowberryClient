package org.chimeramc.client.core.cosmetics;

/**
 * The order the launcher resolves the player's skin in, as pure logic so the chain is unit-tested:
 *
 * <ol>
 *   <li><b>Manual import</b> — an explicit override the player set; it always wins.</li>
 *   <li><b>Live capture</b> — the game's own skin, read this session.</li>
 *   <li><b>Cached capture</b> — the last capture, so the launcher (which runs before the game)
 *       still shows the real skin after the first session.</li>
 *   <li><b>Account lookup</b> — opt-in only (off by default; needs explicit consent and a
 *       third-party service).</li>
 *   <li><b>Steve/Alex</b> — chosen by a stable hash of the identity, then the grey placeholder.</li>
 * </ol>
 *
 * <p>This class decides <em>which rung</em> a set of availability flags selects; it does not touch
 * Android.
 */
public final class SkinFallbackChain {

    /** The rungs, in priority order. */
    public enum Rung {
        IMPORTED, LIVE_CAPTURE, CACHED_CAPTURE, ACCOUNT_LOOKUP, DEFAULT_MODEL, PLACEHOLDER
    }

    private SkinFallbackChain() {
    }

    /**
     * Picks the rung. {@code accountLookupEnabled} is the opt-in flag; when it is off the account
     * rung is skipped entirely, so no request is ever made without consent.
     */
    public static Rung pick(boolean importedAvailable, boolean liveAvailable,
                            boolean cachedAvailable, boolean accountLookupEnabled,
                            boolean accountLookupAvailable, boolean defaultModelAvailable) {
        if (importedAvailable) return Rung.IMPORTED;
        if (liveAvailable) return Rung.LIVE_CAPTURE;
        if (cachedAvailable) return Rung.CACHED_CAPTURE;
        if (accountLookupEnabled && accountLookupAvailable) return Rung.ACCOUNT_LOOKUP;
        if (defaultModelAvailable) return Rung.DEFAULT_MODEL;
        return Rung.PLACEHOLDER;
    }

    /** A short label for the source, for the preview status line. */
    public static String label(Rung rung) {
        switch (rung) {
            case IMPORTED: return "Imported skin";
            case LIVE_CAPTURE: return "Your Minecraft skin";
            case CACHED_CAPTURE: return "Your Minecraft skin (last session)";
            case ACCOUNT_LOOKUP: return "Account skin (lookup)";
            case DEFAULT_MODEL: return "Steve / Alex";
            default: return "Chimera default";
        }
    }

    /**
     * Chooses the default model (Steve vs Alex) from an identity hash, so two identities do not
     * always get the same stand-in. Returns true for the slim (Alex) model.
     */
    public static boolean defaultIsSlim(String identityHash) {
        if (identityHash == null || identityHash.isEmpty()) return false;
        int h = 0;
        for (int i = 0; i < identityHash.length(); i++) h = h * 31 + identityHash.charAt(i);
        return (h & 1) == 1;
    }
}
