package org.chimeramc.client.core.mods.inbuilt.catalog;

import java.util.Locale;

/**
 * Counts down the client-visible cooldowns that start from the player's own input.
 *
 * <p>Several combat readouts are timers for something the player just did — a pearl thrown, a gapple
 * eaten, an attack swung, a shield disabled. The game owns the authoritative cooldown, but the
 * <em>event that starts it</em> is the player's own key press, which the launcher already sees in
 * {@code dispatchKeyEvent}. A timer started from that press is honest as long as it is labelled as
 * "since last use" rather than as the game's own value, which is what the readout does.
 *
 * <p>Pure and Android-free: the caller passes the current time, so the countdown rules are
 * unit-testable and no clock is read here. A timer is armed once per press (a re-press re-arms it),
 * reports the remaining time, and expires on its own.
 */
public final class ActionTimers {

    private long armedAtMs = Long.MIN_VALUE;
    private long durationMs;

    /**
     * Arms (or re-arms) the timer with a duration.
     *
     * @param nowMs      the event time, taken from the caller's clock
     * @param durationMs the countdown length; a non-positive value disarms the timer
     */
    public void arm(long nowMs, long durationMs) {
        if (durationMs <= 0) {
            disarm();
            return;
        }
        this.armedAtMs = nowMs;
        this.durationMs = durationMs;
    }

    public void disarm() {
        armedAtMs = Long.MIN_VALUE;
        durationMs = 0;
    }

    /** Whether the timer is currently running. */
    public boolean isActive(long nowMs) {
        return armedAtMs != Long.MIN_VALUE && nowMs < armedAtMs + durationMs;
    }

    /** The remaining milliseconds, or 0 when the timer is not running. */
    public long remainingMs(long nowMs) {
        if (armedAtMs == Long.MIN_VALUE) return 0;
        long remaining = (armedAtMs + durationMs) - nowMs;
        return Math.max(0, remaining);
    }

    /**
     * The remaining time as "1.2s", or null when the timer is not running.
     *
     * <p>Null is the honest empty state: a readout shows "waiting for game data" rather than a
     * fabricated "0.0s" for a timer that was never started.
     */
    public String remainingText(long nowMs) {
        if (!isActive(nowMs)) return null;
        return String.format(Locale.US, "%.1fs", remainingMs(nowMs) / 1000.0);
    }
}
