package org.chimeramc.client.core.cosmetics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A tiny, process-wide sink for faces the preview scene refused to build because their atlas was
 * missing or too small for the declared dimensions.
 *
 * <p>Defence in depth for the "cosmetic equipped crashes the app" bug: an authored texture whose
 * real size disagrees with the size the scene was told to sample with would index out of the pixel
 * array. {@link PreviewScene} now skips such a face instead of drawing it, and records the fact here
 * so the in-app diagnostics report can show <em>why</em> a piece rendered nothing rather than leaving
 * the user with a silently missing hat. Pure and Android-free, so a JVM test pins the wording.
 */
public final class PreviewSceneDiagnostics {

    /** One skipped face: which piece, the reason, and the sizes involved. */
    public static final class Entry {
        public final String tag;
        public final String reason;
        public final int atlasW;
        public final int atlasH;
        public final int atlasLen;

        Entry(String tag, String reason, int atlasW, int atlasH, int atlasLen) {
            this.tag = tag == null ? "" : tag;
            this.reason = reason == null ? "" : reason;
            this.atlasW = atlasW;
            this.atlasH = atlasH;
            this.atlasLen = atlasLen;
        }

        /** A human-readable one-liner for the diagnostics report. */
        public String describe() {
            return (tag.isEmpty() ? "(untagged)" : tag) + ": " + reason
                    + " (atlasW=" + atlasW + ", atlasH=" + atlasH
                    + ", pixels=" + atlasLen + ", need=" + (atlasW * atlasH) + ")";
        }
    }

    private static final int MAX_ENTRIES = 64;
    private static final List<Entry> ENTRIES = Collections.synchronizedList(new ArrayList<>());

    private PreviewSceneDiagnostics() {
    }

    /** Records a skipped face. Bounded so a pathological scene cannot grow the list without limit. */
    public static void record(String tag, String reason, int atlasW, int atlasH, int atlasLen) {
        synchronized (ENTRIES) {
            if (ENTRIES.size() >= MAX_ENTRIES) return;
            ENTRIES.add(new Entry(tag, reason, atlasW, atlasH, atlasLen));
        }
    }

    /** A snapshot copy, safe to iterate while another thread records. */
    public static List<Entry> snapshot() {
        synchronized (ENTRIES) {
            return new ArrayList<>(ENTRIES);
        }
    }

    /** Clears the sink; the preview calls this at the start of a fresh scene build. */
    public static void clear() {
        ENTRIES.clear();
    }
}
