package org.chimeramc.client.ui.components;

/**
 * The Material 3 Expressive shape scale and its morphing rule (U6).
 *
 * <p>Expressive M3 treats a shape as a property that animates, not a fixed corner: a chip grows
 * into a card, a card becomes a sheet, and the corners travel between the scale's stops while it
 * happens. Two things make that safe to use everywhere:
 *
 * <ul>
 *   <li>the stops are a closed set, so no screen invents a radius; and</li>
 *   <li>the morph is a pure interpolation, so the same pair of shapes always produces the same
 *       in-between — a test can assert it rather than a reviewer squinting at a recording.</li>
 * </ul>
 *
 * <p>The scale is a superset of {@link UiTokens}' four surface radii (the tokens' 8/12/16/28 are
 * this scale's SMALL/MEDIUM/LARGE/EXTRA_LARGE), so the two cannot disagree: a component that morphs
 * ends on a token value.
 */
public final class UiShape {

    private UiShape() {
    }

    // --- The scale (dp) ----------------------------------------------------------------------

    /** Extra-small: an indicator, a hairline tab. */
    public static final int EXTRA_SMALL = 4;
    /** Small: a chip, a tag. Matches {@link UiTokens#RADIUS_SMALL}. */
    public static final int SMALL = UiTokens.RADIUS_SMALL;
    /** Medium: a field, a compact control. Matches {@link UiTokens#RADIUS_MEDIUM}. */
    public static final int MEDIUM = UiTokens.RADIUS_MEDIUM;
    /** Large: a card, a button. Matches {@link UiTokens#RADIUS_LARGE}. */
    public static final int LARGE = UiTokens.RADIUS_LARGE;
    /** Extra-large: a window, a sheet. Matches {@link UiTokens#RADIUS_XLARGE}. */
    public static final int EXTRA_LARGE = UiTokens.RADIUS_XLARGE;
    /** Full: a pill. Clamped by the drawable to half the height. */
    public static final int FULL = UiTokens.RADIUS_TOGGLE;

    /**
     * The shape a control uses at rest, by how much surface it occupies. Named so a caller asks for
     * a role ("a card") rather than picking a number.
     */
    public enum Role {
        INDICATOR(EXTRA_SMALL),
        CHIP(SMALL),
        FIELD(MEDIUM),
        CARD(LARGE),
        WINDOW(EXTRA_LARGE),
        PILL(FULL);

        public final int radiusDp;

        Role(int radiusDp) {
            this.radiusDp = radiusDp;
        }
    }

    /**
     * The corner radius at fraction [t] between two shapes, in dp.
     *
     * <p>A {@link #FULL} end cannot be interpolated numerically (it is a sentinel, not a length), so
     * a morph to or from a pill uses the other end's radius and lets the drawable clamp it: the
     * result is a corner that grows toward a stadium, which is what the morph reads as anyway.
     */
    public static float morph(int fromDp, int toDp, float t) {
        float amount = Math.max(0f, Math.min(1f, t));
        float from = fromDp == FULL ? toDp : fromDp;
        float to = toDp == FULL ? fromDp : toDp;
        return from + (to - from) * amount;
    }

    /** {@link #morph} from a role pair. */
    public static float morph(Role from, Role to, float t) {
        return morph(from.radiusDp, to.radiusDp, t);
    }

    /**
     * The expansion fraction for a shape given the component's size, used to keep a large radius
     * from eating a small box: a 28dp corner on a 32dp chip would leave a lozenge, so the radius is
     * capped at half the shorter side. Pure, so the cap is testable.
     */
    public static float clampToBox(float radiusPx, float widthPx, float heightPx) {
        float limit = Math.min(widthPx, heightPx) / 2f;
        if (limit <= 0f) return 0f;
        return Math.max(0f, Math.min(radiusPx, limit));
    }
}
