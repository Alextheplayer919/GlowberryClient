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
     * Picks the rung. A rung is available only when its flag is true; the placeholder is the
     * terminal fallback and is always selected when nothing else is.
     */
    public static Source pick(boolean gameAvailable, boolean importedAvailable,
                              boolean appliedPackAvailable, boolean steveAvailable) {
        if (gameAvailable) return Source.GAME;
        if (importedAvailable) return Source.IMPORTED;
        if (appliedPackAvailable) return Source.APPLIED_PACK;
        if (steveAvailable) return Source.STEVE;
        return Source.PLACEHOLDER;
    }

    /** A short label for the source, used by the preview's status line. */
    public static String label(Source source) {
        switch (source) {
            case GAME: return "Your Minecraft skin";
            case IMPORTED: return "Imported skin";
            case APPLIED_PACK: return "Applied skin pack";
            case STEVE: return "Steve";
            default: return "Chimera default";
        }
    }
}
