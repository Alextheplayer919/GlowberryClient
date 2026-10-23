package org.chimeramc.client.core.antegg;

import java.util.ArrayList;
import java.util.List;

/**
 * Semantic version range matching for {@code egg.json} dependencies (item G4).
 *
 * <p>The first version of the manifest accepted only plain mod ids. A dependency that names a
 * range lets a package opt into a newer major instead of being pinned forever, and lets the
 * launcher reject a dependency whose version cannot satisfy the constraint — the two directions a
 * flat name check gets wrong. The grammar is deliberately small:
 *
 * <pre>
 *   "1.2.0"        exact
 *   ">=1.2.0"      at or above
 *   ">1.2.0"       strictly above
 *   "<=1.2.0"      at or below
 *   "<1.2.0"       strictly below
 *   "~1.2.0"       compatible with 1.2.0 (>=1.2.0, <1.3.0)
 *   "^1.2.0"       compatible with 1.2.0 (>=1.2.0, <2.0.0)
 *   "1.x" / "1.2.x" any in the given major/minor
 * </pre>
 *
 * Multiple constraints are joined with {@code &&} ({@code ">=1.2.0 && <2.0.0"}).
 *
 * <p>Pure JVM (no Android types), unit-tested by {@code VersionRangeTest}.
 */
public final class VersionRange {

    private final List<Constraint> constraints;

    private VersionRange(List<Constraint> constraints) {
        this.constraints = constraints;
    }

    /** True for an empty/full range that accepts any version. */
    public boolean isAny() {
        return constraints.isEmpty();
    }

    /**
     * Parses [value], returning null when it is not a syntactically valid range. A blank or
     * {@code "*"} value means "any version".
     */
    public static VersionRange parse(String value) {
        if (value == null) return new VersionRange(new ArrayList<>());
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "*".equals(trimmed)) {
            return new VersionRange(new ArrayList<>());
        }

        List<Constraint> out = new ArrayList<>();
        for (String part : trimmed.split("&&")) {
            String clause = part.trim();
            if (clause.isEmpty()) return null;
            Constraint c = parseClause(clause);
            if (c == null) return null;
            out.add(c);
        }
        return new VersionRange(out);
    }

    private static Constraint parseClause(String clause) {
        String operator = null;
        String rest = clause;

        for (String candidate : new String[]{">=", "<=", ">", "<", "~", "^", "=="}) {
            if (clause.startsWith(candidate)) {
                operator = candidate;
                rest = clause.substring(candidate.length()).trim();
                break;
            }
        }

        if (rest.isEmpty()) return null;

        // Wildcard forms: 1.x, 1.2.x, 1, 1.2
        if (rest.contains("x") || rest.contains("X") || rest.contains("*")) {
            if (operator != null) return null; // operators cannot apply to a wildcard
            String[] parts = rest.toLowerCase().split("\\.");
            if (!isWildcardPattern(parts)) return null;
            return WildcardConstraint.wildcard(parts);
        }

        int[] v = parseSemVer(rest);
        if (v == null) return null;

        if (operator == null) {
            return new ExactConstraint(v);
        }
        return switch (operator) {
            case ">=" -> new CompareConstraint(v, 1);
            case "<=" -> new CompareConstraint(v, -1);
            case ">" -> new CompareConstraint(v, 1, false);
            case "<" -> new CompareConstraint(v, -1, false);
            case "==" -> new ExactConstraint(v);
            case "~" -> new TildeConstraint(v);
            case "^" -> new CaretConstraint(v);
            default -> null;
        };
    }

    private static boolean isWildcardPattern(String[] parts) {
        if (parts.length == 0 || parts.length > 3) return false;
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (!p.matches("\\d+") && !p.equals("x")) return false;
            if (p.equals("x") && i != parts.length - 1) return false; // wildcard must be last
        }
        return true;
    }

    private static int[] parseSemVer(String value) {
        int coreEnd = value.length();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '-' || c == '+') {
                coreEnd = i;
                break;
            }
        }
        String[] parts = value.substring(0, coreEnd).split("\\.");
        if (parts.length == 0 || parts.length > 3) return null;
        int[] v = new int[3];
        for (int i = 0; i < 3; i++) {
            if (i < parts.length) {
                if (!parts[i].matches("\\d+")) return null;
                try {
                    v[i] = Integer.parseInt(parts[i]);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return v;
    }

    /** True when [version] (a canonical {@code major.minor.patch} string) satisfies this range. */
    public boolean matches(String version) {
        int[] v = parseSemVer(version);
        if (v == null) return false;
        for (Constraint c : constraints) {
            if (!c.matches(v)) return false;
        }
        return true;
    }

    /** The canonical display form of the range, for UI and error messages. */
    public String describe() {
        if (constraints.isEmpty()) return "*";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < constraints.size(); i++) {
            if (i > 0) sb.append(" && ");
            sb.append(constraints.get(i).describe());
        }
        return sb.toString();
    }

    private interface Constraint {
        boolean matches(int[] version);

        String describe();
    }

    private static final class ExactConstraint implements Constraint {
        private final int[] version;

        ExactConstraint(int[] version) {
            this.version = version;
        }

        @Override
        public boolean matches(int[] v) {
            return compare(v, version) == 0;
        }

        @Override
        public String describe() {
            return format(version);
        }
    }

    /** A {@code >=} / {@code <=} / {@code >} / {@code <} comparison against a fixed version. */
    private static final class CompareConstraint implements Constraint {
        private final int[] version;
        /** 1 = upper-bound semantics are inclusive, -1 = lower-bound semantics are inclusive. */
        private final int sign;
        private final boolean inclusive;

        CompareConstraint(int[] version, int sign) {
            this(version, sign, true);
        }

        CompareConstraint(int[] version, int sign, boolean inclusive) {
            this.version = version;
            this.sign = sign;
            this.inclusive = inclusive;
        }

        @Override
        public boolean matches(int[] v) {
            int cmp = compare(v, version);
            if (sign > 0) {
                if (inclusive) return cmp >= 0;
                return cmp > 0;
            }
            if (inclusive) return cmp <= 0;
            return cmp < 0;
        }

        @Override
        public String describe() {
            if (sign > 0) return (inclusive ? ">=" : ">") + format(version);
            return (inclusive ? "<=" : "<") + format(version);
        }
    }

    /** {@code ~1.2.0}: >=1.2.0 and <1.3.0. Wildcard minor becomes >=1.2 and <1.3 when exact pair. */
    private static final class TildeConstraint implements Constraint {
        private final int[] version;

        TildeConstraint(int[] version) {
            this.version = version;
        }

        @Override
        public boolean matches(int[] v) {
            if (compare(v, version) < 0) return false;
            int[] upper = {version[0], version[1] + 1, 0};
            return compare(v, upper) < 0;
        }

        @Override
        public String describe() {
            return "~" + format(version);
        }
    }

    /** {@code ^1.2.0}: >=1.2.0 and <2.0.0 (same major). For 0.x, <0.(x+1).0. */
    private static final class CaretConstraint implements Constraint {
        private final int[] version;

        CaretConstraint(int[] version) {
            this.version = version;
        }

        @Override
        public boolean matches(int[] v) {
            if (compare(v, version) < 0) return false;
            int[] upper;
            if (version[0] != 0) {
                upper = new int[]{version[0] + 1, 0, 0};
            } else if (version[1] != 0) {
                upper = new int[]{0, version[1] + 1, 0};
            } else {
                upper = new int[]{0, 0, version[2] + 1};
            }
            return compare(v, upper) < 0;
        }

        @Override
        public String describe() {
            return "^" + format(version);
        }
    }

    private static final class WildcardConstraint implements Constraint {
        private final int[] version;
        private final int exactParts;

        WildcardConstraint(int[] version, int exactParts) {
            this.version = version;
            this.exactParts = exactParts;
        }

        static WildcardConstraint wildcard(String[] parts) {
            int[] v = new int[3];
            for (int i = 0; i < parts.length; i++) {
                if (!parts[i].equals("x")) {
                    v[i] = Integer.parseInt(parts[i]);
                }
            }
            return new WildcardConstraint(v, parts.length - 1);
        }

        @Override
        public boolean matches(int[] v) {
            for (int i = 0; i < exactParts; i++) {
                if (v[i] != version[i]) return false;
            }
            return true;
        }

        @Override
        public String describe() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < exactParts; i++) {
                if (i > 0) sb.append('.');
                sb.append(version[i]);
            }
            sb.append(".x");
            return sb.toString();
        }
    }

    private static int compare(int[] left, int[] right) {
        for (int i = 0; i < 3; i++) {
            if (left[i] != right[i]) return Integer.compare(left[i], right[i]);
        }
        return 0;
    }

    private static String format(int[] v) {
        return v[0] + "." + v[1] + "." + v[2];
    }
}