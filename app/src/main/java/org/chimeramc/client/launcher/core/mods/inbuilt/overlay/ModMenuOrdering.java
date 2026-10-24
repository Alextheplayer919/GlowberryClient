package org.chimeramc.client.core.mods.inbuilt.overlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Mod Menu's drag-to-reorder rule (M9).
 *
 * <p>Pure and Android-free so the ordering can be unit-tested without a device: a saved order is a
 * list of module keys, and applying it must reorder each section while keeping every module that
 * the saved list does not mention in its shipped place.
 *
 * <p>The rule that matters is the fallback. A user's arrangement has to survive an update that adds
 * or removes a module, so an unknown key is dropped and an unlisted module keeps its shipped
 * position at the end of its own section rather than being sorted to the front by accident.
 */
public final class ModMenuOrdering {

    private ModMenuOrdering() {
    }

    /**
     * Reorders one section's keys by a saved order.
     *
     * @param shipped the section's keys in their shipped order
     * @param saved   the user's saved order, or null/empty for "never reordered"
     * @return the reordered keys, always containing every shipped key exactly once
     */
    public static List<String> apply(List<String> shipped, List<String> saved) {
        List<String> result = new ArrayList<>();
        if (shipped == null || shipped.isEmpty()) return result;
        if (saved == null || saved.isEmpty()) {
            result.addAll(shipped);
            return result;
        }
        Map<String, Integer> rank = new HashMap<>();
        int next = 0;
        for (String key : saved) {
            if (key == null || rank.containsKey(key)) continue;
            rank.put(key, next++);
        }
        // A stable sort keeps the shipped relative order for everything the saved order does not
        // mention, which is exactly the "keeps its shipped place" rule.
        List<String> ranked = new ArrayList<>();
        List<String> unranked = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String key : shipped) {
            if (key == null || !seen.add(key)) continue;
            if (rank.containsKey(key)) {
                ranked.add(key);
            } else {
                unranked.add(key);
            }
        }
        ranked.sort((a, b) -> Integer.compare(rank.get(a), rank.get(b)));
        result.addAll(ranked);
        result.addAll(unranked);
        return result;
    }

    /**
     * Moves one key within a list, returning the new order.
     *
     * <p>An out-of-range move returns the list unchanged, so a stale drag index cannot silently
     * drop or duplicate an entry.
     */
    public static List<String> move(List<String> keys, int from, int to) {
        List<String> result = new ArrayList<>();
        if (keys != null) result.addAll(keys);
        if (from < 0 || to < 0 || from >= result.size() || to >= result.size()) return result;
        String moved = result.remove(from);
        result.add(to, moved);
        return result;
    }
}
