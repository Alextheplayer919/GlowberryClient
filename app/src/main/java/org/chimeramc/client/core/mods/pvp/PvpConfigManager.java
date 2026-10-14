package org.chimeramc.client.core.mods.pvp;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The central configuration manager for the PvP module set.
 *
 * <p>With 200+ modules, per-module preferences scattered across the codebase become unmaintainable
 * and, worse, unverifiable — two screens can disagree about whether a module is on. This manager is
 * the one place the enabled set and the per-module option values live. It is pure (no Android), so
 * the merge and the defaults are unit-tested, and the Android layer only reads/writes it through a
 * {@link Store}.
 *
 * <p><b>"Server-synced" means: one authoritative snapshot, merged, never last-write-wins-blindly.</b>
 * The launcher cannot receive a config from a Minecraft server — there is no such protocol — so the
 * sync is between the launcher's own surfaces (the Mod Menu, the VIP menu, the settings backup) and
 * an optional shared profile the user can export/import. A remote value is applied only where the
 * local snapshot has no explicit value, so a local choice always wins; this is what stops an import
 * from silently flipping a module the user just set. {@link #mergeRemote} encodes that rule.
 */
public final class PvpConfigManager {

    /** A persistence seam, implemented by the Android SharedPreferences layer. */
    public interface Store {
        boolean getEnabled(String moduleId, boolean fallback);

        void putEnabled(String moduleId, boolean enabled);

        String getValue(String moduleId, String key, String fallback);

        void putValue(String moduleId, String key, String value);
    }

    private final Store store;
    private final Map<String, Boolean> enabled = new HashMap<>();
    private final Map<String, String> values = new HashMap<>();

    public PvpConfigManager(Store store) {
        this.store = store;
    }

    /** The enabled state of a module, defaulting to off for a module this build cannot drive. */
    public boolean isEnabled(String moduleId) {
        Boolean cached = enabled.get(moduleId);
        if (cached != null) return cached;
        PvpModuleCatalog.Module module = PvpModuleCatalog.find(moduleId);
        boolean fallback = module != null && module.implemented && store.getEnabled(moduleId, false);
        enabled.put(moduleId, fallback);
        return fallback;
    }

    /**
     * Sets a module's enabled state. A module this build cannot drive is refused (returns false),
     * so the UI cannot turn on a switch that would do nothing.
     */
    public boolean setEnabled(String moduleId, boolean value) {
        PvpModuleCatalog.Module module = PvpModuleCatalog.find(moduleId);
        if (module == null || !module.implemented) return false;
        enabled.put(moduleId, value);
        store.putEnabled(moduleId, value);
        return true;
    }

    /** A module option value, or the fallback. */
    public String getValue(String moduleId, String key, String fallback) {
        String cacheKey = moduleId + "/" + key;
        String cached = values.get(cacheKey);
        if (cached != null) return cached;
        String stored = store.getValue(moduleId, key, fallback);
        values.put(cacheKey, stored);
        return stored;
    }

    /** Stores a module option value. */
    public void setValue(String moduleId, String key, String value) {
        values.put(moduleId + "/" + key, value);
        store.putValue(moduleId, key, value);
    }

    /**
     * Merges a remote profile into the local snapshot.
     *
     * <p>Precedence is local-first: a module the local snapshot has an explicit value for keeps it,
     * and only an absent local value adopts the remote one. This is deliberately not last-write-wins
     * — an import that silently flipped a module the user just set is the failure this prevents.
     *
     * @return the number of entries adopted
     */
    public int mergeRemote(Map<String, Boolean> remoteEnabled, Set<String> localExplicitKeys) {
        if (remoteEnabled == null) return 0;
        int adopted = 0;
        for (Map.Entry<String, Boolean> e : remoteEnabled.entrySet()) {
            String id = e.getKey();
            if (localExplicitKeys != null && localExplicitKeys.contains(id)) continue;
            PvpModuleCatalog.Module module = PvpModuleCatalog.find(id);
            if (module == null || !module.implemented) continue;
            if (enabled.containsKey(id)) continue;
            enabled.put(id, e.getValue());
            store.putEnabled(id, e.getValue());
            adopted++;
        }
        return adopted;
    }

    /** The enabled set as an immutable snapshot, for export. */
    public Map<String, Boolean> enabledSnapshot() {
        return Collections.unmodifiableMap(new HashMap<>(enabled));
    }

    /** Clears the in-memory cache so the next read comes from the store. */
    public void invalidate() {
        enabled.clear();
        values.clear();
    }
}
