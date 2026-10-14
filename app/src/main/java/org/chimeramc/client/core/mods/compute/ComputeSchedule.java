package org.chimeramc.client.core.mods.compute;

/**
 * The coalescing rule for the off-thread module compute bus.
 *
 * <p>Modules that need real work per frame (a geometry evaluation, a peer-velocity solve, a
 * prediction) must not run it on the render thread. The bus hands the work to a worker thread, but a
 * plain queue would then grow without bound the moment the worker falls behind the frame rate —
 * on a crowded server the worker can never catch up and the queue becomes the memory leak that
 * eventually OOMs the session.
 *
 * <p>The rule is <b>drop-oldest, keep-latest</b>: when a new input arrives while the previous one has
 * not been consumed, the new one replaces it. A consumer only ever needs the most recent frame state,
 * so discarding the superseded input loses nothing and bounds the queue to a single pending item. A
 * minimum interval additionally throttles a worker that is keeping up, so a 240 Hz render loop does
 * not drive 240 solves a second for work that only needs 60.
 *
 * <p>Pure and time-injected, so the coalescing and the throttle are pinned without threads — see
 * {@code ComputeScheduleTest}.
 */
public final class ComputeSchedule {

    /** The default minimum interval between runs: at most one solve per 16 ms (~60 Hz). */
    public static final long DEFAULT_MIN_INTERVAL_MS = 16L;

    private long lastRunMs = Long.MIN_VALUE;
    private boolean pending;
    private long pendingRevision;
    private long consumedRevision;

    /** Submits an input, replacing any unconsumed one. Returns the new pending revision. */
    public long submit(long nowMs) {
        pendingRevision += 1;
        pending = true;
        return pendingRevision;
    }

    /**
     * Whether the worker should run now.
     *
     * @param nowMs monotonic milliseconds
     * @param minIntervalMs the throttle; 0 disables it
     * @return true when there is a pending input and the interval has elapsed
     */
    public boolean shouldRun(long nowMs, long minIntervalMs) {
        if (!pending) return false;
        if (pendingRevision == consumedRevision) return false;
        if (lastRunMs != Long.MIN_VALUE && minIntervalMs > 0 && nowMs - lastRunMs < minIntervalMs) {
            return false;
        }
        return true;
    }

    /** Marks the pending input consumed and records the run time. */
    public void markConsumed(long nowMs) {
        consumedRevision = pendingRevision;
        pending = false;
        lastRunMs = nowMs;
    }

    /** True when an input is waiting to be consumed. */
    public boolean hasPending() {
        return pending;
    }

    /** The number of inputs submitted since construction (diagnostics). */
    public long submittedCount() {
        return pendingRevision;
    }
}
