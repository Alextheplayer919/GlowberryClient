package org.chimeramc.client.core.antegg;

import java.util.Locale;

/**
 * A single parsed dependency entry from {@code egg.json} (item G4).
 *
 * <p>The written manifest lists plain mod names, and that is still readable. A dependency may also
 * carry an inline version range ({@code "libfoo>=2.0.0"}, {@code "libfoo ~1.2"}, {@code
 * "libfoo^1.0"}), which is the extension this class parses out. The split point is the first
 * range operator, because ids are slug-formed (letters, digits, {@code -}, {@code _}) and never
 * contain {@code =}, {@code >}, {@code <}, {@code ~} or {@code ^}. A plain name parses to {@code
 * range == null}, meaning "any installed version satisfies this dependency".
 *
 * <p>Pure JVM; unit-tested by {@code DependencyRefTest}.
 */
public final class DependencyRef {

    /** The slug id of the required mod, e.g. {@code libfoo}. */
    public final String id;
    /** The required version range, or null when any version satisfies the dependency. */
    public final VersionRange range;

    private DependencyRef(String id, VersionRange range) {
        this.id = id;
        this.range = range;
    }

    /**
     * Parses one dependency entry. A null or blank entry returns null (callers treat null as
     * invalid input). The range is optional: a value without an operator is a plain id and the
     * result's {@link #range} is null.
     */
    public static DependencyRef parse(String entry) {
        if (entry == null) return null;
        String trimmed = entry.trim();
        if (trimmed.isEmpty()) return null;

        int split = indexOfOperator(trimmed);
        if (split < 0) {
            String id = AntEggManifest.slug(trimmed);
            if (id.isEmpty()) return null;
            return new DependencyRef(id, null);
        }

        String idPart = trimmed.substring(0, split).trim();
        String rangePart = trimmed.substring(split).trim();
        if (idPart.isEmpty() || rangePart.isEmpty()) return null;

        String id = AntEggManifest.slug(idPart);
        if (id.isEmpty()) return null;
        VersionRange range = VersionRange.parse(rangePart);
        if (range == null) return null;
        return new DependencyRef(id, range);
    }

    private static int indexOfOperator(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '=' || c == '>' || c == '<' || c == '~' || c == '^') {
                return i;
            }
        }
        return -1;
    }

    /** Human-readable form: {@code id} or {@code id range}. */
    public String describe() {
        if (range == null) return id;
        return id + range.describe();
    }

    @Override
    public String toString() {
        return describe();
    }
}