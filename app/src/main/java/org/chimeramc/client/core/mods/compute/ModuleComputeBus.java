package org.chimeramc.client.core.mods.compute;

import java.util.concurrent.atomic.AtomicReference;

/**
 * A single-worker compute bus that keeps heavy module work off the render thread.
 *
 * <p>Modules submit a frame input from the render/tick thread; the worker consumes the <em>latest</em>
 * input off-thread and publishes a result the render thread reads back. This is the in-process
 * equivalent of the "route module communication through a separate daemon thread" pattern: the render
 * thread never blocks, and a slow solve can never stall a frame.
 *
 * <p><b>Bounded by construction.</b> The pending input is a single {@link AtomicReference}, so
 * {@link #submit} overwrites any unconsumed input rather than queueing it — the drop-oldest /
 * keep-latest rule in {@link ComputeSchedule}. A crowded server therefore cannot grow the bus without
 * bound.
 *
 * <p><b>Fail-closed.</b> A throwing solve is caught, recorded as the last error, and the previous
 * result is kept, so one bad module cannot take the worker thread down or publish a half-computed
 * result. {@link #lastError()} surfaces it instead.
 *
 * <p>Threads: {@link #submit} and {@link #latestResult} are safe from any thread. The worker is the
 * only consumer of the pending input.
 */
public final class ModuleComputeBus<I, O> {

    /** A solve from an input to an output. */
    public interface Solver<I, O> {
        O solve(I input) throws Exception;
    }

    private final Solver<I, O> solver;
    private final AtomicReference<I> pending = new AtomicReference<>(null);
    private final AtomicReference<O> result = new AtomicReference<>(null);
    private final AtomicReference<Throwable> lastError = new AtomicReference<>(null);
    private final ComputeSchedule schedule = new ComputeSchedule();

    private Thread worker;
    private volatile boolean running;

    public ModuleComputeBus(Solver<I, O> solver) {
        this.solver = solver;
    }

    /** Starts the worker thread. Idempotent. */
    public synchronized void start(String threadName) {
        if (running) return;
        running = true;
        worker = new Thread(this::loop, threadName == null ? "module-compute" : threadName);
        worker.setDaemon(true);
        worker.start();
    }

    /** Stops the worker thread and clears any pending input. */
    public synchronized void stop() {
        running = false;
        pending.set(null);
        if (worker != null) {
            worker.interrupt();
            worker = null;
        }
    }

    /** Submits an input from the render/tick thread, replacing any unconsumed one. */
    public void submit(I input) {
        pending.set(input);
        schedule.submit(0L);
    }

    /** The most recent published result, or null when none has been produced. */
    public O latestResult() {
        return result.get();
    }

    /** The last solve failure, or null. Never cleared by a later success, so it stays diagnosable. */
    public Throwable lastError() {
        return lastError.get();
    }

    /** True when the worker is running. */
    public boolean isRunning() {
        return running;
    }

    private void loop() {
        while (running) {
            final I input = pending.getAndSet(null);
            if (input == null) {
                try {
                    Thread.sleep(4L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                continue;
            }
            try {
                O output = solver.solve(input);
                if (output != null) result.set(output);
            } catch (Throwable t) {
                // Fail-closed: keep the previous result and record the failure rather than letting it
                // kill the worker thread (which would silently stop every module on the bus).
                lastError.set(t);
            }
        }
    }
}
