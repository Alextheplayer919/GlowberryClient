package org.chimeramc.client.core.voice;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the voice-chat safety gate: voice cannot start without recorded consent, and the consent
 * version rule re-asks after a text change. No mocks — the class is pure.
 */
public class VoiceConsentTest {

    @Test
    public void voiceCannotStartWithoutConsent() {
        // The master switch alone is not enough: that is what makes the default age-appropriate
        // rather than merely off-by-default.
        assertFalse(VoiceConsent.canStart(true, false));
    }

    @Test
    public void voiceCannotStartWithoutTheMasterSwitch() {
        assertFalse(VoiceConsent.canStart(false, true));
    }

    @Test
    public void voiceStartsOnlyWithBoth() {
        assertTrue(VoiceConsent.canStart(true, true));
    }

    @Test
    public void aFreshInstallHasNoConsent() {
        assertFalse(VoiceConsent.isCurrentVersion(null));
        assertFalse(VoiceConsent.isCurrentVersion(""));
    }

    @Test
    public void aStaleConsentVersionIsRefused() {
        // Bumping the text must re-ask, exactly like the LLM consent rule.
        assertFalse(VoiceConsent.isCurrentVersion("0"));
        assertFalse(VoiceConsent.isCurrentVersion(VoiceConsent.CONSENT_VERSION + "x"));
        assertTrue(VoiceConsent.isCurrentVersion(VoiceConsent.CONSENT_VERSION));
    }

    @Test
    public void theConsentTextNamesTheRelayAndTheLackOfEndToEndEncryption() {
        // The scope note must not over-promise privacy: a relay carries the audio.
        String text = VoiceConsent.CONSENT_TEXT.toLowerCase();
        assertTrue(text.contains("relay"));
        assertTrue(text.contains("not end-to-end encrypted"));
        assertTrue(text.contains("block"));
    }

    @Test
    public void reportBodyCarriesTheReporterIdAndTheDetail() {
        String body = VoiceConsent.reportBody("abcd1234", "Lynx", "Griefer",
                "world", "spamming");
        assertTrue(body.contains("abcd1234"));
        assertTrue(body.contains("Lynx"));
        assertTrue(body.contains("Griefer"));
        assertTrue(body.contains("world"));
        assertTrue(body.contains("spamming"));
    }

    @Test
    public void reportBodyFillsInForBlankFieldsRatherThanEmittingAnEmptyLine() {
        String body = VoiceConsent.reportBody(null, "   ", null, "", null);
        assertTrue(body.contains("Reporter peer id: unknown"));
        assertTrue(body.contains("Reporter name: unknown"));
        assertTrue(body.contains("Reported player: not specified"));
        assertTrue(body.contains("Channel: world"));
        assertTrue(body.contains("(no description given)"));
    }
}