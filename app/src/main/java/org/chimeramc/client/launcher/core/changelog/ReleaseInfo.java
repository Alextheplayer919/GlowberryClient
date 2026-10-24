package org.chimeramc.client.core.changelog;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * The dated metadata of a release (item C2): when it shipped and how long it is supported.
 *
 * <p>Pure and Android-free so the formatting is unit-testable, and so the same rendering is used
 * wherever a release is shown. A release that does not declare the fields renders nothing rather
 * than a fabricated date — an undated release is a gap, not a zero.
 */
public final class ReleaseInfo {

    private static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US).withZone(ZoneOffset.UTC);

    public final String releasedAt;
    public final String supportUntil;

    public ReleaseInfo(String releasedAt, String supportUntil) {
        this.releasedAt = releasedAt == null ? "" : releasedAt.trim();
        this.supportUntil = supportUntil == null ? "" : supportUntil.trim();
    }

    public boolean isEmpty() {
        return releasedAt.isEmpty() && supportUntil.isEmpty();
    }

    /**
     * A one-line summary such as {@code "Released 23 Oct 2026 · supported until 23 Apr 2027"}.
     *
     * <p>Either half is omitted when its field is missing, so a half-declared release still shows
     * the half it has. Returns "" when neither is present, so the caller can hide the view.
     */
    public String summaryLine() {
        String released = formatDay(releasedAt);
        String until = formatDay(supportUntil);
        if (released.isEmpty() && until.isEmpty()) return "";
        if (released.isEmpty()) return "Supported until " + until;
        if (until.isEmpty()) return "Released " + released;
        return "Released " + released + " \u00b7 supported until " + until;
    }

    /** True when {@code nowMillis} is past the declared support end; false when undated. */
    public boolean isSupportExpired(long nowMillis) {
        Long until = parseEpochMillis(supportUntil);
        return until != null && nowMillis > until;
    }

    /** Formats an ISO-8601 UTC instant as a short day; "" when unparseable or blank. */
    public static String formatDay(String iso) {
        Long epoch = parseEpochMillis(iso);
        if (epoch == null) return "";
        return DAY.format(Instant.ofEpochMilli(epoch));
    }

    /** Parses an ISO-8601 timestamp (with or without the trailing {@code Z}); null when invalid. */
    public static Long parseEpochMillis(String iso) {
        if (iso == null) return null;
        String value = iso.trim();
        if (value.isEmpty()) return null;
        try {
            // Accept a trailing Z (the schema requires it) and a bare local form for robustness.
            Instant instant = value.endsWith("Z")
                    ? Instant.parse(value)
                    : Instant.parse(value + "Z");
            return instant.toEpochMilli();
        } catch (RuntimeException e) {
            return null;
        }
    }
}