package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists the radial wheel's profiles and the active one.
 *
 * <p>Stored in SharedPreferences as JSON, the same shape {@code ControllerProfileManager} uses, so
 * there is one persistence idiom for a user-authored list. A profile is only ever referenced by id
 * (a parent's {@code OPEN_SUBWHEEL} entry names it), so the store owns the id -> profile lookup and
 * a dangling reference degrades to "not found" rather than a crash.
 *
 * <p>The store never throws: a corrupt blob reads as the built-in default profile set, because a
 * wheel that cannot load must still be usable.
 */
public final class WheelProfileStore {

    private static final String PREFS_NAME = "radial_wheel_profiles";
    private static final String KEY_PROFILES = "profiles";
    private static final String KEY_ROOT = "root_profile";
    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE =
            new TypeToken<List<WheelEntry.Profile>>() { }.getType();

    /** The id of the built-in default profile. */
    public static final String DEFAULT_ID = "default";

    private final SharedPreferences prefs;

    public WheelProfileStore(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * The built-in default: the four destinations the wheel has always had, plus a nested PvP
     * sub-wheel. This is what a fresh install shows, so the wheel is useful before it is edited.
     */
    public static List<WheelEntry.Profile> defaultProfiles() {
        List<WheelEntry.Profile> profiles = new ArrayList<>();
        WheelEntry.Profile root = new WheelEntry.Profile(DEFAULT_ID, "Main");
        root.entries.add(new WheelEntry(WheelEntry.Action.OPEN_MODS, "", "Mods"));
        root.entries.add(new WheelEntry(WheelEntry.Action.OPEN_CLOSET, "", "Closet"));
        root.entries.add(new WheelEntry(WheelEntry.Action.OPEN_HUD, "", "HUD"));
        root.entries.add(new WheelEntry(WheelEntry.Action.OPEN_SUBWHEEL, "pvp", "PvP"));
        profiles.add(root);

        WheelEntry.Profile pvp = new WheelEntry.Profile("pvp", "PvP");
        pvp.entries.add(new WheelEntry(WheelEntry.Action.TOGGLE_MODULE, "cps_display", "CPS"));
        pvp.entries.add(new WheelEntry(WheelEntry.Action.TOGGLE_MODULE, "hit_registration", "HitReg"));
        pvp.entries.add(new WheelEntry(WheelEntry.Action.TOGGLE_MODULE, "snaplook", "Snaplook"));
        pvp.entries.add(new WheelEntry(WheelEntry.Action.OPEN_SUBWHEEL, DEFAULT_ID, "Back"));
        profiles.add(pvp);
        return profiles;
    }

    /** The stored profiles, or the built-in default when none are stored or the blob is unreadable. */
    public List<WheelEntry.Profile> load() {
        String json = prefs.getString(KEY_PROFILES, null);
        if (json == null || json.isEmpty()) return defaultProfiles();
        try {
            List<WheelEntry.Profile> parsed = GSON.fromJson(json, LIST_TYPE);
            if (parsed == null || parsed.isEmpty()) return defaultProfiles();
            return sanitize(parsed);
        } catch (RuntimeException e) {
            return defaultProfiles();
        }
    }

    /** Persists the profiles, dropping anything that cannot be shown. */
    public void save(List<WheelEntry.Profile> profiles) {
        List<WheelEntry.Profile> safe = sanitize(profiles);
        if (safe.isEmpty()) safe = defaultProfiles();
        prefs.edit().putString(KEY_PROFILES, GSON.toJson(safe)).apply();
    }

    /** The id of the profile the bind opens. Falls back to the first profile. */
    public String getRootId() {
        List<WheelEntry.Profile> profiles = load();
        String stored = prefs.getString(KEY_ROOT, null);
        if (stored != null && findById(profiles, stored) != null) return stored;
        return profiles.isEmpty() ? DEFAULT_ID : profiles.get(0).id;
    }

    /** Sets the profile the bind opens, ignoring an id that does not exist. */
    public void setRootId(String id) {
        if (id == null || findById(load(), id) == null) return;
        prefs.edit().putString(KEY_ROOT, id).apply();
    }

    /** The profile with an id, or null. */
    public static WheelEntry.Profile findById(List<WheelEntry.Profile> profiles, String id) {
        if (profiles == null || id == null) return null;
        for (WheelEntry.Profile p : profiles) {
            if (p != null && id.equals(p.id)) return p;
        }
        return null;
    }

    /**
     * The root profile, or the first profile, or a fresh default.
     *
     * <p>Never null: a wheel with no profile cannot be drawn, and the caller would have to invent
     * one at the draw site.
     */
    public WheelEntry.Profile rootProfile() {
        List<WheelEntry.Profile> profiles = load();
        WheelEntry.Profile root = findById(profiles, getRootId());
        if (root != null) return root;
        if (!profiles.isEmpty()) return profiles.get(0);
        return defaultProfiles().get(0);
    }

    /**
     * Drops entries a profile cannot draw and gives every profile a usable id and label.
     *
     * <p>A profile with no id cannot be referenced by a sub-wheel entry, and a duplicate id makes
     * the reference ambiguous, so both are repaired here rather than at the lookup.
     */
    public static List<WheelEntry.Profile> sanitize(List<WheelEntry.Profile> profiles) {
        List<WheelEntry.Profile> out = new ArrayList<>();
        if (profiles == null) return out;
        List<String> seenIds = new ArrayList<>();
        for (WheelEntry.Profile p : profiles) {
            if (p == null) continue;
            if (p.id == null || p.id.trim().isEmpty()) continue;
            if (seenIds.contains(p.id)) continue;
            seenIds.add(p.id);
            if (p.name == null || p.name.trim().isEmpty()) p.name = p.id;
            List<WheelEntry> safe = new ArrayList<>();
            for (WheelEntry e : p.safeEntries()) {
                if (e == null) continue;
                e.label = WheelEntry.normalizeLabel(e.label);
                safe.add(e);
            }
            p.entries = safe;
            out.add(p);
        }
        return out;
    }
}
