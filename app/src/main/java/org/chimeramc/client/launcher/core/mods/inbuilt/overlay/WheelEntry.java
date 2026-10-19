package org.chimeramc.client.core.mods.inbuilt.overlay;

import java.util.ArrayList;
import java.util.List;

/**
 * A radial-wheel entry and the profiles that group them, as a serializable model.
 *
 * <p>The spec asks for a wheel the player can actually configure: multiple local profiles (one for
 * survival, one for PvP), entries assigned to sectors with their own label, icon and colour, and
 * nested sub-wheels so the action count is not capped at one ring. This is that model, and it is a
 * plain POJO so Gson round-trips it and the rules around it stay unit-testable.
 *
 * <p>Two invariants are load-bearing and are enforced here rather than in the UI, because a profile
 * that violates either draws sectors the player cannot hit:
 *
 * <ul>
 *   <li>an entry always has a label, so a sector is never blank;</li>
 *   <li>a profile never exceeds {@link WheelLayout#MAX_SECTORS} entries, because past that a sector
 *       is too thin to aim at.</li>
 * </ul>
 */
public final class WheelEntry {

    /** What selecting an entry does. */
    public enum Action {
        /** Toggle a mod-menu module by id. */
        TOGGLE_MODULE,
        /** Open the module list. */
        OPEN_MODS,
        /** Open the cosmetics wardrobe. */
        OPEN_CLOSET,
        /** Open the settings screen. */
        OPEN_SETTINGS,
        /** Enter the HUD editor. */
        OPEN_HUD,
        /** Open another wheel profile as a nested sub-wheel. */
        OPEN_SUBWHEEL,
    }

    /** The entry's action. */
    public Action action = Action.TOGGLE_MODULE;
    /** The module id for {@link Action#TOGGLE_MODULE}, or the target profile id for
     * {@link Action#OPEN_SUBWHEEL}. */
    public String target = "";
    /** The label drawn under the sector's icon. */
    public String label = "";
    /** The entry's own colour (ARGB), so a sector is recognisable at a glance; 0 means "use the
     * accent". */
    public int color = 0;

    public WheelEntry() {
    }

    public WheelEntry(Action action, String target, String label) {
        this.action = action == null ? Action.TOGGLE_MODULE : action;
        this.target = target == null ? "" : target;
        this.label = normalizeLabel(label);
    }

    /** A label is never empty, so a sector always has something to draw. */
    public static String normalizeLabel(String label) {
        if (label == null || label.trim().isEmpty()) return "—";
        return label.trim();
    }

    /** The entry's effective colour: its own when set, else the supplied accent. */
    public int effectiveColor(int accent) {
        return color == 0 ? accent : color;
    }

    /** True when this entry opens another wheel rather than performing an action. */
    public boolean isSubWheel() {
        return action == Action.OPEN_SUBWHEEL;
    }

    /**
     * One named wheel: a set of entries plus the id of the profile that opens it.
     *
     * <p>A profile is its own entity so a nested wheel is just another profile the parent points at;
     * there is no separate "sub-wheel" type to keep in step. The root profile is the one the bind
     * opens.
     */
    public static final class Profile {
        /** The stable id the parent's {@code OPEN_SUBWHEEL} entry refers to. */
        public String id = "";
        /** The display name (shown in the manager and on the hub while open). */
        public String name = "";
        /** The entries, in draw order; index 0 is the top sector and they run clockwise. */
        public List<WheelEntry> entries = new ArrayList<>();

        public Profile() {
        }

        public Profile(String id, String name) {
            this.id = id == null ? "" : id;
            this.name = name == null ? "" : name;
        }

        /** The entries, never null and never longer than {@link WheelLayout#MAX_SECTORS}. */
        public List<WheelEntry> safeEntries() {
            if (entries == null) return new ArrayList<>();
            if (entries.size() <= WheelLayout.MAX_SECTORS) return entries;
            return new ArrayList<>(entries.subList(0, WheelLayout.MAX_SECTORS));
        }

        /** A profile with no entries cannot be shown, so it is treated as empty rather than drawn. */
        public boolean isEmpty() {
            return safeEntries().isEmpty();
        }
    }
}
