package org.chimeramc.client.core.cosmetics;

/**
 * Decides whether a skin captured from the game is actually <em>this</em> player's skin, by comparing
 * it against an independently attributed source (the account lookup).
 *
 * <p>The engine capture is <b>content, not attribution</b>: the image-loader seam carries no player
 * id, so what it holds is the most recent base-skin-shaped upload. In practice that is the local
 * player's skin, but nothing proves it. Showing a stranger's face under "Your Minecraft skin" is the
 * failure this class exists to prevent, so the capture is only promoted to an attributed source when
 * it agrees with a skin whose owner <em>is</em> known.
 *
 * <p>Pure: it takes two 64x64 ARGB atlases and returns a verdict, so every rule is unit-tested
 * without a device.
 */
public final class SkinIdentity {

    /**
     * How much of the atlas may differ and still count as the same skin. A JPEG-free PNG round-trip
     * and a nearest-neighbour downscale are lossless here, so an exact match is the normal case; the
     * tolerance only absorbs the handful of pixels a different but visually identical encoder pass
     * can move. Deliberately small: a genuinely different skin differs across most of the atlas.
     */
    public static final double MAX_DIFFERENCE = 0.06;

    /** The verdict for a capture compared against an attributed skin. */
    public enum Verdict {
        /** The capture matches the attributed skin; it may be shown as this player's own. */
        MATCH,
        /** The capture differs; it belongs to someone or something else and must not be attributed. */
        MISMATCH,
        /** There was nothing to compare against, so the capture stays unattributed. */
        UNVERIFIED,
    }

    private SkinIdentity() {
    }

    /**
     * Compares a captured atlas against an attributed one.
     *
     * <p>Alpha is ignored: a capture whose transparent regions were filled with a different but
     * fully-transparent colour is still the same skin, and an opaque-versus-transparent difference in
     * a region no face samples is not a difference the player would see. Colour is compared per
     * channel with a small allowance for encoder rounding.
     */
    public static Verdict compare(int[] captured, int[] attributed) {
        if (captured == null || attributed == null) return Verdict.UNVERIFIED;
        if (captured.length == 0 || captured.length != attributed.length) return Verdict.UNVERIFIED;

        int differing = 0;
        int compared = 0;
        for (int i = 0; i < captured.length; i++) {
            int a = captured[i];
            int b = attributed[i];
            boolean aClear = ((a >>> 24) & 0xFF) < SkinAlphaFilter.ALPHA_THRESHOLD;
            boolean bClear = ((b >>> 24) & 0xFF) < SkinAlphaFilter.ALPHA_THRESHOLD;
            if (aClear && bClear) continue;      // both invisible: no visible difference
            compared++;
            if (!closeEnough(a, b)) differing++;
        }
        if (compared == 0) return Verdict.UNVERIFIED;
        double fraction = (double) differing / (double) compared;
        return fraction <= MAX_DIFFERENCE ? Verdict.MATCH : Verdict.MISMATCH;
    }

    private static boolean closeEnough(int a, int b) {
        int dr = Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF));
        int dg = Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF));
        int db = Math.abs((a & 0xFF) - (b & 0xFF));
        // A single channel may be off by the encoder's rounding; a real difference moves several.
        return dr <= 8 && dg <= 8 && db <= 8;
    }

    /**
     * The final choice of skin source, as a value so the caller (and the preview's status line) act
     * on one decision.
     */
    public static final class Choice {
        public final SkinResolution.Source source;
        /** True when the shown skin is known to belong to this player. */
        public final boolean attributed;
        public final String label;

        Choice(SkinResolution.Source source, boolean attributed, String label) {
            this.source = source;
            this.attributed = attributed;
            this.label = label;
        }
    }

    /**
     * Resolves which skin to show once every candidate has been gathered.
     *
     * <p>An explicit import wins outright (the player chose it). Then the account skin, because its
     * owner is known. The game capture is used only when it agrees with the account skin; a capture
     * that disagrees, or that has nothing to be checked against, is still shown — it is almost always
     * the local player — but is labelled as unverified rather than claimed as the player's own. Steve
     * is reached only when both attributed sources fail, and the placeholder only when even Steve
     * cannot be produced.
     */
    public static Choice resolve(boolean importedAvailable,
                                 boolean accountAvailable,
                                 boolean capturedAvailable,
                                 Verdict captureVerdict,
                                 boolean appliedPackAvailable,
                                 boolean steveAvailable) {
        if (importedAvailable) {
            return new Choice(SkinResolution.Source.IMPORTED, true, "Imported skin");
        }
        if (accountAvailable) {
            return new Choice(SkinResolution.Source.ACCOUNT, true, "Your Minecraft account skin");
        }
        if (capturedAvailable && captureVerdict == Verdict.MATCH) {
            return new Choice(SkinResolution.Source.GAME, true, "Your Minecraft skin");
        }
        if (capturedAvailable && captureVerdict != Verdict.MISMATCH) {
            // Almost certainly the local player, but nothing proves it: say so.
            return new Choice(SkinResolution.Source.GAME, false,
                    "Your Minecraft skin (unverified — no account skin to compare)");
        }
        if (appliedPackAvailable) {
            return new Choice(SkinResolution.Source.APPLIED_PACK, false, "Applied skin pack");
        }
        if (steveAvailable) {
            return new Choice(SkinResolution.Source.STEVE, false,
                    "Steve (default — no skin found)");
        }
        return new Choice(SkinResolution.Source.PLACEHOLDER, false,
                "Chimera default (no skin could be loaded)");
    }
}
