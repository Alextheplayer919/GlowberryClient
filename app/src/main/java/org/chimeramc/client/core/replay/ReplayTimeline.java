package org.chimeramc.client.core.replay;

/**
 * The replay transport: play, pause, speed (including reverse) and scrub.
 *
 * <p>The old player exposed only a play/pause button and a 1x/1.5x/2x cycle, so a clip could not be
 * rewound, slowed down or scrubbed to an exact moment. This is the pure state machine behind the
 * transport controls, so the rules are unit-tested instead of living inside click handlers: a
 * reversed speed really runs the position backwards, the position clamps at both ends, and a
 * boundary stop reports that it happened so the UI can drop the playing state.
 *
 * <p>It never reads a clock; the caller feeds elapsed milliseconds, which is what makes it testable
 * and lets the same object serve the video transport and a camera path.
 */
public final class ReplayTimeline {

    /** The speeds the cycle steps through, slowest reverse to fastest forward. */
    public static final float[] SPEEDS = {-2.0f, -1.0f, -0.5f, 0.5f, 1.0f, 1.5f, 2.0f};

    /** Where a fresh timeline starts in {@link #SPEEDS}. */
    private static final int DEFAULT_SPEED_INDEX = 4;

    private long durationMs;
    private long positionMs;
    private int speedIndex = DEFAULT_SPEED_INDEX;
    private boolean playing;

    public ReplayTimeline(long durationMs) {
        this.durationMs = Math.max(0L, durationMs);
    }

    public long durationMs() {
        return durationMs;
    }

    /** Replaces the duration, clamping the position if it now sits past the end. */
    public void setDurationMs(long durationMs) {
        this.durationMs = Math.max(0L, durationMs);
        if (positionMs > this.durationMs) positionMs = this.durationMs;
    }

    public long positionMs() {
        return positionMs;
    }

    public float speed() {
        return SPEEDS[speedIndex];
    }

    public int speedIndex() {
        return speedIndex;
    }

    public boolean isPlaying() {
        return playing;
    }

    public boolean isReversed() {
        return SPEEDS[speedIndex] < 0f;
    }

    public float fraction() {
        if (durationMs <= 0L) return 0f;
        return (float) positionMs / (float) durationMs;
    }

    public boolean atStart() {
        return positionMs <= 0L;
    }

    public boolean atEnd() {
        return durationMs > 0L && positionMs >= durationMs;
    }

    public void play() {
        // Starting a reversed transport from the end, or a forward one from the start, has nowhere
        // to go; leaving it playing would spin a zero-length step every tick.
        if (isReversed() && atStart()) return;
        if (!isReversed() && atEnd()) return;
        playing = true;
    }

    public void pause() {
        playing = false;
    }

    public void toggle() {
        if (playing) {
            pause();
        } else {
            play();
        }
    }

    /** Jumps to an absolute position, clamped to the clip. */
    public void seekMs(long ms) {
        positionMs = clamp(ms, 0L, durationMs);
    }

    /** Jumps to a fraction of the clip; a non-finite fraction is ignored. */
    public void seekFraction(float fraction) {
        if (Float.isNaN(fraction) || Float.isInfinite(fraction)) return;
        seekMs((long) (clamp(fraction, 0f, 1f) * durationMs));
    }

    /** Steps one speed slot forward in the cycle. */
    public void cycleSpeed() {
        setSpeedIndex(speedIndex + 1);
    }

    /** Steps one speed slot backward in the cycle. */
    public void cycleSpeedBack() {
        setSpeedIndex(speedIndex - 1);
    }

    /** Selects a speed slot, wrapping, and stops a transport that can no longer move. */
    public void setSpeedIndex(int index) {
        int count = SPEEDS.length;
        speedIndex = ((index % count) + count) % count;
        if (playing && ((isReversed() && atStart()) || (!isReversed() && atEnd()))) {
            playing = false;
        }
    }

    /** Selects the speed slot nearest {@code speed}, so a preset can ask for a value, not an index. */
    public void setSpeed(float speed) {
        int best = DEFAULT_SPEED_INDEX;
        float bestDelta = Float.MAX_VALUE;
        for (int i = 0; i < SPEEDS.length; i++) {
            float delta = Math.abs(SPEEDS[i] - speed);
            if (delta < bestDelta) {
                bestDelta = delta;
                best = i;
            }
        }
        setSpeedIndex(best);
    }

    /**
     * Advances the position by {@code elapsedMs} of wall time scaled by the current speed.
     *
     * @return true when the transport reached a boundary and stopped; the caller should drop its
     *         playing state and re-read {@link #isPlaying()}.
     */
    public boolean advance(long elapsedMs) {
        if (!playing || elapsedMs <= 0L || durationMs <= 0L) return false;
        long delta = (long) (elapsedMs * speed());
        long next = positionMs + delta;
        if (next <= 0L) {
            positionMs = 0L;
            playing = false;
            return true;
        }
        if (next >= durationMs) {
            positionMs = durationMs;
            playing = false;
            return true;
        }
        positionMs = next;
        return false;
    }

    /** A stable label for the current speed, e.g. {@code 1x} or {@code -0.5x}. */
    public String speedLabel() {
        float s = speed();
        if (s == (long) s) return ((long) s) + "x";
        return s + "x";
    }

    private static long clamp(long value, long lo, long hi) {
        return value < lo ? lo : (value > hi ? hi : value);
    }

    private static float clamp(float value, float lo, float hi) {
        return value < lo ? lo : (value > hi ? hi : value);
    }
}
