package org.chimeramc.client.core.mods;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Per-mod crash attribution and auto-disable (item G8).
 *
 * <p>On top of {@link ModSafeMode}'s single-shot crash loop guard, this store counts how often
 * the <em>same set</em> of suspect mods crashed the previous launch. A mod that crashed twice
 * in a row is auto-disabled on the third launch instead of being offered for the user to disable
 * manually — the crash-loop experience becomes "the launcher fixed it and told you why", which is
 * the difference between a support ticket and a self-explaining screen ({@code ModLoadDiagnostics}
 * already carries the per-mod reason).
 *
 * <p>Counting rules:
 * <ul>
 *   <li>The counter advances only when {@link ModSafeMode#hasCrashLoop} is true (the previous
 *       launch started loading mods and never reported the runtime up).</li>
 *   <li>A launch that reaches the runtime, or the user manually toggling mods afterwards, resets
 *       the counter via {@link #clear}.</li>
 *   <li>If the suspect set changes between crashes the counter restarts: same mods crashed twice
 *       is what makes the attribution trustworthy.</li>
 * </ul>
 *
 * <p>The decision logic is pure ({@link #evaluate}) so the unit test pins the counting without a
 * device; the class only persists state.
 */
public final class ModCrashAttribution {

    private static final String PREFS_NAME = "mod_crash_attribution";
    private static final String KEY_CRASH_COUNT = "consecutive_crash_count";
    private static final String KEY_SUSPECTS = "suspects";
    private static final String KEY_AUTO_DISABLED = "auto_disabled_mods";
    private static final char SEPARATOR = '\n';

    /** How many consecutive crashes of the same suspect set trigger auto-disable. */
    public static final int AUTO_DISABLE_THRESHOLD = 2;

    private ModCrashAttribution() {
    }

    /** A mod id that should now be auto-disabled because it crashed enough times. */
    public static final class AutoDisableCandidate {
        public final String modId;
        public final int crashCount;

        AutoDisableCandidate(String modId, int crashCount) {
            this.modId = modId;
            this.crashCount = crashCount;
        }
    }

    /**
     * The outcome of a launch start. {@code autoDisable} lists mods that passed the threshold and
     * should be disabled now; a mod that already was auto-disabled in a previous run is not listed
     * again (re-enabling it is an explicit user action, and {@link #clear} then resets the count).
     */
    public static final class Evaluation {
        public final int crashCount;
        /** Suspects that have now crashed {@code AUTO_DISABLE_THRESHOLD}+ times in a row. */
        public final List<String> autoDisable;

        Evaluation(int crashCount, List<String> autoDisable) {
            this.crashCount = crashCount;
            this.autoDisable = autoDisable != null ? autoDisable : Collections.<String>emptyList();
        }
    }

    /**
     * Advances the crash record when the previous launch crashed with mods loaded. Call exactly
     * once per process start, before loading any mod.
     *
     * @param previousLaunchCrashed whether {@link ModSafeMode#hasCrashLoop} was true
     * @param suspectIds the mods {@link ModSafeMode#suspectModIds} reported
     */
    public static Evaluation onLaunchStart(Context context, boolean previousLaunchCrashed, List<String> suspectIds) {
        if (context == null) {
            return new Evaluation(0, Collections.<String>emptyList());
        }
        SharedPreferences prefs = prefs(context);
        if (!previousLaunchCrashed) {
            // The last launch reached the runtime; any crash counter was about that launch.
            clear(context);
            return new Evaluation(0, Collections.<String>emptyList());
        }

        Set<String> suspects = sanitize(suspectIds);
        Set<String> previousSuspects = readSet(prefs, KEY_SUSPECTS);

        int newCount = advanceCount(prefs.getInt(KEY_CRASH_COUNT, 0), previousSuspects, suspects);
        List<String> overThreshold = new ArrayList<>();
        if (newCount >= AUTO_DISABLE_THRESHOLD) {
            overThreshold.addAll(suspects);
        }
        prefs.edit()
                .putInt(KEY_CRASH_COUNT, newCount)
                .putString(KEY_SUSPECTS, join(suspects))
                .putString(KEY_AUTO_DISABLED, join(new LinkedHashSet<>(overThreshold)))
                .apply();
        return new Evaluation(newCount, overThreshold);
    }

    /**
     * Pure counting rule, exposed package-private so the JVM unit test (no Context,
     * no mocks) can pin it. The counter only advances while the suspect set stays identical;
     * a changed set restarts it, because "the same mods crashed twice" is what makes the
     * attribution trustworthy.
     *
     * @param previousCount the stored consecutive-crash count
     * @param previousSuspects the suspects recorded for the previous crash (empty set on the
     *     first crash, normalized ids)
     * @param suspects the suspects of the crash this evaluation is for
     * @return the count to store: {@code previousCount + 1} when the sets match, else 1
     */
    static int advanceCount(int previousCount, java.util.Set<String> previousSuspects, java.util.Set<String> suspects) {
        if (suspects == null || suspects.isEmpty()) {
            return 1;
        }
        boolean sameSuspects = previousSuspects != null && previousSuspects.equals(suspects);
        return sameSuspects ? previousCount + 1 : 1;
    }

    /** Mods that have been auto-disabled by the crash attribution (never re-listed after enable). */
    public static List<String> getAutoDisabledMods(Context context) {
        if (context == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(readSet(prefs(context), KEY_AUTO_DISABLED));
    }

    /** Whether {@code modId} was auto-disabled by the attribution store (persisted). */
    public static boolean wasAutoDisabled(Context context, String modId) {
        if (context == null || modId == null || modId.isEmpty()) {
            return false;
        }
        return readSet(prefs(context), KEY_AUTO_DISABLED).contains(modId);
    }

    /** The consecutive crash count for the current launch (0 when the last launch ran fine). */
    public static int getCrashCount(Context context) {
        if (context == null) {
            return 0;
        }
        return prefs(context).getInt(KEY_CRASH_COUNT, 0);
    }

    /** Clears the crash counter and auto-disable list; call when the user re-enables a mod. */
    public static void clear(Context context) {
        if (context == null) {
            return;
        }
        prefs(context).edit()
                .remove(KEY_CRASH_COUNT)
                .remove(KEY_SUSPECTS)
                .remove(KEY_AUTO_DISABLED)
                .apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static Set<String> readSet(SharedPreferences prefs, String key) {
        String raw = prefs.getString(key, "");
        if (raw == null || raw.isEmpty()) {
            return Collections.emptySet();
        }
        return parseSet(raw);
    }

    private static Set<String> parseSet(String raw) {
        Set<String> out = new LinkedHashSet<>();
        if (raw == null) {
            return out;
        }
        for (String part : raw.split(String.valueOf(SEPARATOR))) {
            if (!part.isEmpty()) {
                out.add(part);
            }
        }
        return out;
    }

    private static Set<String> sanitize(List<String> modIds) {
        Set<String> out = new LinkedHashSet<>();
        if (modIds == null) {
            return out;
        }
        for (String id : modIds) {
            if (id != null && !id.isEmpty()) {
                out.add(id);
            }
        }
        return out;
    }

    private static String join(Set<String> mods) {
        StringBuilder builder = new StringBuilder();
        for (String mod : mods) {
            if (builder.length() > 0) {
                builder.append(SEPARATOR);
            }
            builder.append(mod);
        }
        return builder.toString();
    }
}