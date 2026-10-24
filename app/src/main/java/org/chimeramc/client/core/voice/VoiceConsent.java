package org.chimeramc.client.core.voice;

/**
 * The safety gate for proximity voice chat (B15).
 *
 * <p>Voice chat is <b>off until the user records consent</b>. This is the age-appropriate default:
 * an app whose privacy policy is not intended for children under 13 must not switch a live
 * microphone and a peer-to-peer audio link on by itself, and it must not do so as a side effect of
 * another feature (a game session, a cosmetic sync, a module preset). Turning the master switch on
 * asks for consent once; bumping {@link #CONSENT_VERSION} invalidates every previously recorded
 * decision so a change to what the feature does re-asks.
 *
 * <p>This mirrors {@code core.javabridge.LlmConsent} on purpose: the rule for "may this feature
 * touch the network / a microphone at all?" lives in one pure, tested class rather than being
 * re-derived at each call site.
 *
 * <p>The class also owns the two other safety rules that must not drift:
 * <ul>
 *   <li>{@link #canStart} — the single predicate the start path consults, so a future caller cannot
 *       start the link while consent is missing.</li>
 *   <li>{@link #reportBody} — the text of the human report path, which includes the reporter's own
 *       peer id so a report can be correlated without asking the user to identify themselves.</li>
 * </ul>
 */
public final class VoiceConsent {

    /** Bumping this invalidates previously recorded consent and re-asks the user. */
    public static final String CONSENT_VERSION = "1";

    public static final String CONSENT_TEXT =
            "Voice chat opens your microphone and connects you to other players over the local "
            + "network or, if you set one, a relay server. Audio is not end-to-end encrypted: on a "
            + "relay the server carries it. You can mute or block any player locally at any time, "
            + "and blocking is never shared with them. Only turn this on if you are old enough to "
            + "use it and agree to talk to the players you meet. Continue?";

    private VoiceConsent() {}

    /**
     * Whether the voice link may be started.
     *
     * <p>Both must hold: the master switch is on (the user asked for it) and consent for the
     * current text version was recorded. The master switch alone is not enough, which is what
     * makes the default age-appropriate rather than merely off-by-default.
     */
    public static boolean canStart(boolean masterEnabled, boolean consentRecorded) {
        return masterEnabled && consentRecorded;
    }

    /** True when the recorded consent version matches the current one. */
    public static boolean isCurrentVersion(String recordedVersion) {
        return CONSENT_VERSION.equals(recordedVersion);
    }

    /**
     * The body of a report, including the reporter's peer id.
     *
     * <p>The reporter is never asked for a name or an account: the id is per-session and is the
     * only handle the client itself has, so a report can be correlated with the session it came
     * from without collecting anything that identifies the person.
     *
     * @param reporterPeerId the reporting client's own peer id
     * @param reporterName   the display name the reporter is currently advertising
     * @param subjectName    the display name of the player being reported, or null/blank if none
     * @param channel        the channel the report was made on
     * @param detail         the user's own description, which may be blank
     */
    public static String reportBody(String reporterPeerId, String reporterName,
                                    String subjectName, String channel, String detail) {
        String id = blankTo(reporterPeerId, "unknown");
        String who = blankTo(reporterName, "unknown");
        String subject = blankTo(subjectName, "not specified");
        String where = blankTo(channel, VoiceChannel.WORLD);
        String body = blankTo(detail, "(no description given)");
        return "Voice chat report\n"
                + "Reporter peer id: " + id + "\n"
                + "Reporter name: " + who + "\n"
                + "Reported player: " + subject + "\n"
                + "Channel: " + where + "\n"
                + "What happened:\n" + body + "\n";
    }

    private static String blankTo(String value, String fallback) {
        if (value == null) return fallback;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
}