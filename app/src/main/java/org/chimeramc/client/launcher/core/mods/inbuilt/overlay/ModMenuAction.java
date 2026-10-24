package org.chimeramc.client.core.mods.inbuilt.overlay;

import java.util.Objects;

/**
 * A one-shot entry in the Mod Menu (M3).
 *
 * <p>The counterpart to a module. A module has an enable state and is drawn with a switch; an action
 * fires once and is drawn with a chevron. Modelling the two as separate types rather than as one
 * entry with a flag is what keeps a call site from accidentally rendering an action as a switch —
 * the mistake that made the menu feel wrong in the first place.
 *
 * <p>Pure (resource ids and strings only), so the row kind and the ordering rules are unit-testable.
 */
public final class ModMenuAction {

    private final String id;
    private final int titleRes;
    private final int subtitleRes;
    private final String groupId;

    public ModMenuAction(String id, int titleRes, int subtitleRes, String groupId) {
        this.id = Objects.requireNonNull(id, "id");
        this.titleRes = titleRes;
        this.subtitleRes = subtitleRes;
        this.groupId = groupId;
    }

    public String getId() {
        return id;
    }

    public int getTitleRes() {
        return titleRes;
    }

    /** The action's one-line explanation, or 0 when it has none. */
    public int getSubtitleRes() {
        return subtitleRes;
    }

    /** The section the action belongs to; actions never share a row with a module. */
    public String getGroupId() {
        return groupId;
    }

    /** An action is always an action row: it has no enable state (M3). */
    public ModMenuRowKind kind() {
        return ModMenuRowKind.of(false);
    }

    /** The stable key used for the list's identity and the saved order. */
    public String getStableKey() {
        return "action:" + id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ModMenuAction)) return false;
        return id.equals(((ModMenuAction) other).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
