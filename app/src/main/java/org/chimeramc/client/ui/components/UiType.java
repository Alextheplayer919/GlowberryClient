package org.chimeramc.client.ui.components;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.chimeramc.client.R;

/**
 * The five type roles as one call (U8).
 *
 * <p>A screen picks a role — display, title, body, label or caption — and this applies the family,
 * the size and the weight together. Setting a size by hand is what lets two screens that show the
 * same kind of text drift apart, so the roles are the only public entry point.
 *
 * <p>The family is a <b>variable</b> font ({@code dmsans}), so a role's weight is a real axis value
 * rather than the coarse {@code NORMAL}/{@code BOLD} pair the platform falls back to: the "title
 * 600" and "label 500" weights the design uses are expressible, which is the Material 3 Expressive
 * type treatment (U6). {@code fontVariationSettings} is API 26+, and the project's minSdk is 28, so
 * it always applies; the bold flag is still set as a belt-and-braces fallback for a device that
 * ignores the axis.
 */
public final class UiType {

    private UiType() {
    }

    /** The one family every role uses. Variable, so a role's weight is an axis value. */
    public static final int FAMILY = R.font.dmsans;

    /** The five roles, in descending prominence. */
    public enum Role {
        DISPLAY(UiTokens.TEXT_DISPLAY, UiTokens.DISPLAY_WEIGHT),
        TITLE(UiTokens.TEXT_TITLE_ROLE, UiTokens.TITLE_WEIGHT),
        BODY(UiTokens.TEXT_BODY, UiTokens.BODY_WEIGHT),
        LABEL(UiTokens.TEXT_LABEL, UiTokens.ROW_TITLE_WEIGHT),
        CAPTION(UiTokens.TEXT_CAPTION, UiTokens.CAPTION_WEIGHT);

        public final float sizeSp;
        public final int weight;

        Role(float sizeSp, int weight) {
            this.sizeSp = sizeSp;
            this.weight = weight;
        }
    }

    /**
     * The {@code fontVariationSettings} string for a weight, e.g. {@code "'wght' 600"}.
     *
     * <p>Pure, so the exact string the platform is handed is unit-tested rather than assumed.
     */
    public static String weightAxis(int weight) {
        int clamped = Math.max(1, Math.min(1000, weight));
        return "'wght' " + clamped;
    }

    /** Applies a role's family, size and weight to a TextView. */
    public static void apply(TextView view, Role role) {
        if (view == null || role == null) return;
        Context context = view.getContext();
        Typeface base = androidx.core.content.res.ResourcesCompat.getFont(context, FAMILY);
        if (base == null) base = Typeface.DEFAULT;
        view.setTypeface(base, role.weight >= 600 ? Typeface.BOLD : Typeface.NORMAL);
        view.setTextSize(role.sizeSp);
        applyWeight(view, role.weight);
    }

    /**
     * Sets the weight axis on a TextView whose family is already a variable font.
     *
     * <p>Kept separate from {@link #apply} so a view that only wants the weight (a title whose size
     * is set in the layout) can have it without the family being reassigned.
     */
    public static void applyWeight(TextView view, int weight) {
        if (view == null) return;
        view.setFontVariationSettings(weightAxis(weight));
    }

    /** The role whose size is closest to [sizeSp]; used by the UI gallery to name a sample. */
    @Nullable
    public static Role closestRole(float sizeSp) {
        Role best = null;
        float bestDelta = Float.MAX_VALUE;
        for (Role role : Role.values()) {
            float delta = Math.abs(role.sizeSp - sizeSp);
            if (delta < bestDelta) {
                bestDelta = delta;
                best = role;
            }
        }
        return best;
    }
}
