package org.chimeramc.client.core.cosmetics;

/**
 * The resolution rule for which skin the preview shows, as pure logic so a JVM test can pin it.
 *
 * <p>The order is: the game's live skin (via {@link GameSkinSource}) &rarr; a skin imported in the
 * launcher &rarr; the applied skin pack &rarr; Steve &rarr; the grey placeholder. This class only
 * decides <em>which rung</em> a given set of availability flags selects; it does not touch Android.
 */
public final class SkinResolution {

    /** The rungs, in priority order. */
    public enum Source {
        /** The player's live in-game skin, read through an engine hook. */
        GAME,
        /** A skin file the player imported in the launcher. */
        IMPORTED,
        /** The signed-in player's own Bedrock skin, resolved from their XUID. */
        ACCOUNT,
        /** The first texture of the applied skin pack. */
        APPLIED_PACK,
        /** The built-in Steve character. */
        STEVE,
        /** The grey placeholder, reached only when even Steve cannot be produced. */
        PLACEHOLDER,
    }

    private SkinResolution() {
    }

    /**
     * Picks the rung when every candidate is already known to be usable.
     *
     * <p>This is the plain priority order and is kept for callers that do not need the identity check
     * (the game capture is trusted here). The live skin choice for the preview goes through
     * {@link SkinIdentity#resolve}, which additionally refuses to attribute a capture that disagrees
     * with the account skin; prefer that when a capture is involved.
     */
    public static Source pick(boolean gameAvailable, boolean importedAvailable,
                              boolean accountAvailable, boolean appliedPackAvailable,
                              boolean steveAvailable) {
        if (gameAvailable) return Source.GAME;
        if (importedAvailable) return Source.IMPORTED;
        if (accountAvailable) return Source.ACCOUNT;
        if (appliedPackAvailable) return Source.APPLIED_PACK;
        if (steveAvailable) return Source.STEVE;
        return Source.PLACEHOLDER;
    }

    /** A short label for the source, used by the preview's status line. */
    public static String label(Source source) {
        switch (source) {
            case GAME: return "Your Minecraft skin";
            case IMPORTED: return "Imported skin";
            case ACCOUNT: return "Your Minecraft account skin";
            case APPLIED_PACK: return "Applied skin pack";
            case STEVE: return "Steve";
            default: return "Chimera default";
        }
    }
}
