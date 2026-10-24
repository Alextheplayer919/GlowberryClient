package org.chimeramc.client.core.mods;

import java.util.Locale;

/**
 * The derived presentation of one mod row (U17).
 *
 * <p>A row has to answer four questions at a glance — what is it, what kind of mod is it, is it on,
 * and if it is not working, why — and the answer must be the same in the Mods tab and anywhere else
 * a mod is listed. Deriving it here, purely from the {@link Mod} and an optional
 * {@link ModLoadDiagnostics.Record}, is what keeps the row from being rebuilt slightly differently
 * on each screen, and lets the rules be unit-tested without inflating a layout.
 *
 * <p>Two rules are worth stating because they are the ones a naive row gets wrong:
 *
 * <ul>
 *   <li><b>A failure is stated in one line and never guessed.</b> The reason comes from the recorded
 *       diagnostic; when there is no record, the row says nothing rather than "unknown error".</li>
 *   <li><b>The kind is read from the entry point, not the file name.</b> An {@code .AntEgg} archive
 *       can hold either runtime, so the extension of the entry path is the authority.</li>
 * </ul>
 */
public final class ModRowPresentation {

    private ModRowPresentation() {
    }

    /** What kind of runtime a mod uses. */
    public enum Kind {
        NATIVE("native"),
        SCRIPT("script"),
        UNKNOWN("unknown");

        public final String label;

        Kind(String label) {
            this.label = label;
        }
    }

    /** The row's state, in the order the UI cares about it. */
    public enum State {
        /** Enabled and no recorded failure. */
        ENABLED,
        /** Disabled by the user. */
        DISABLED,
        /** A load failure was recorded for it; the row must say why. */
        FAILED,
    }

    /**
     * The runtime kind of a mod, from its entry point.
     *
     * <p>{@code .so} is the native runtime, {@code .lua} the script runtime — the two the
     * {@code .AntEgg} manifest declares. A mod with no entry point (a plain library dropped in)
     * reads as {@link Kind#UNKNOWN} rather than being assumed native.
     */
    public static Kind kindOf(Mod mod) {
        if (mod == null) return Kind.UNKNOWN;
        String entry = mod.getEntryPath();
        if (entry == null) return Kind.UNKNOWN;
        String lower = entry.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".so")) return Kind.NATIVE;
        if (lower.endsWith(".lua")) return Kind.SCRIPT;
        return Kind.UNKNOWN;
    }

    /**
     * The row's state.
     *
     * <p>A recorded failure outranks the enabled flag: a mod the user switched on and that then
     * failed is the case the row exists to explain, and showing it as merely "enabled" is how a
     * broken mod looks healthy.
     */
    public static State stateOf(Mod mod, ModLoadDiagnostics.Record failure) {
        if (mod == null) return State.DISABLED;
        if (failure != null) return State.FAILED;
        return mod.isEnabled() ? State.ENABLED : State.DISABLED;
    }

    /**
     * The single-line failure reason for a row, or {@code null} when there is nothing to say.
     *
     * <p>The reason is prefixed with the diagnostic kind so the one line carries both the category
     * ("hash mismatch") and the detail, which is what a bug report needs. An empty or blank reason
     * still yields the kind alone rather than an empty badge.
     */
    public static String failureLine(ModLoadDiagnostics.Record failure) {
        if (failure == null) return null;
        String kind = failure.kind == null ? ModLoadDiagnostics.KIND_UNKNOWN : failure.kind;
        String reason = failure.reason == null ? "" : failure.reason.trim();
        if (reason.isEmpty()) return kind;
        return kind + ": " + reason;
    }

    /**
     * Whether the row's toggle should be interactive.
     *
     * <p>A failed mod stays toggleable so the user can switch it off — disabling every red row
     * would trap them with a broken mod they cannot turn off. Only a missing model is inert.
     */
    public static boolean toggleEnabled(Mod mod) {
        return mod != null;
    }
}
