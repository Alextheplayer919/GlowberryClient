package org.chimeramc.client.core.cosmetics;

/**
 * A rigid cape: one 10x16 pixel panel hinged at the shoulders, with a single smoothed swing.
 *
 * <p>The cape used to be a Verlet cloth simulation with gravity, stiffness, an ambient breeze and
 * travelling flutter. On a real device that read as a cape that never stopped moving - it swayed at
 * rest, flapped on every flick and looked like a flag rather than fabric. A Minecraft cape is a flat
 * quad, so the honest model is a flat quad: it hangs straight and only leans back when the player
 * moves.
 *
 * <p>The panel is exposed as the same {@code COLS x ROWS} particle grid the mesh builder walks, so
 * {@link PreviewCapeScene} is unchanged; every particle simply shares one rotation about the top
 * hinge. The swing itself is a first-order lag toward an angle derived from the player's speed, so a
 * flick eases out instead of snapping, and a still player's cape is perfectly still.
 *
 * <p>Everything is preallocated and mutated in place, and the maths is pure float arithmetic with no
 * Android types, so the shape and the smoothing are unit-testable without a device.
 */
public final class CapeSimulator {

    /**
     * Grid resolution of the panel. The cape is a flat quad, so the grid is only the tessellation
     * the mesh builder walks; a modest resolution keeps the face count low while still giving the
     * rasteriser interior vertices to depth-sort against the body.
     */
    public static final int COLS = 9;
    public static final int ROWS = 11;

    /** Cape width in blocks, from Minecraft's 10-px cape texture (10/16). */
    public static final float WIDTH_BLOCKS = 10f / 16f;

    /** Cape height in blocks, from Minecraft's 16-px cape texture (16/16). */
    public static final float HEIGHT_BLOCKS = 1f;

    /**
     * Fixed timestep in seconds. Time is accumulated and consumed in fixed slices so a background
     * frame hitch cannot make the swing jump.
     */
    public static final float FIXED_DT = 1f / 60f;

    /** Cap on slices per call, so a long stall cannot spiral into a catch-up loop. */
    public static final int MAX_SUBSTEPS = 4;

    /** The furthest the panel leans back, in degrees, at a full-speed run. */
    public static final float MAX_SWING_DEG = 22f;

    /** The speed (blocks/s) that maps to a full swing; a walk leans only a little. */
    public static final float FULL_SWING_SPEED = 2.6f;

    /**
     * Swing smoothing time constant, in seconds. The panel eases toward its target angle with this
     * lag, so a flick trails out over a fraction of a second instead of snapping to full lean and
     * snapping back - the "single smoothed swing" the design calls for.
     */
    public static final float SWING_SMOOTHING_SECONDS = 0.18f;

    private final float[] px = new float[COLS * ROWS];
    private final float[] py = new float[COLS * ROWS];
    private final float[] pz = new float[COLS * ROWS];
    private final float[] restX = new float[COLS * ROWS];
    private final float[] restY = new float[COLS * ROWS];

    private float accumulator;
    private boolean initialised;

    /** The current swing angle in degrees; negative leans the hem back. */
    private float swingDeg;

    /**
     * Anchor-space offset per particle: a point on the panel's rest quad relative to the top
     * centre (the shoulders). x spans the cape's width, y hangs negative downward.
     */
    private void computeRestOffsets() {
        for (int r = 0; r < ROWS; r++) {
            float v = r / (float) (ROWS - 1);
            for (int c = 0; c < COLS; c++) {
                float u = c / (float) (COLS - 1) - 0.5f;
                int i = r * COLS + c;
                restX[i] = u * WIDTH_BLOCKS;
                restY[i] = -v * HEIGHT_BLOCKS;
            }
        }
    }

    /** Resets the panel to its rest shape beneath the given anchor, hanging straight. */
    public void reset(float anchorX, float anchorY, float anchorZ) {
        computeRestOffsets();
        swingDeg = 0f;
        accumulator = 0f;
        initialised = true;
        place(anchorX, anchorY, anchorZ);
    }

    /**
     * Advances the swing.
     *
     * @param dtSeconds   real elapsed time; consumed in fixed slices
     * @param anchorX/Y/Z the shoulder point this frame, already moved by the body
     * @param forwardZ    the body's facing direction on the world Z axis (unused by a rigid panel)
     * @param forwardX    the body's facing direction on the world X axis (unused by a rigid panel)
     * @param speedBlocks the body's speed, which drives how far the panel leans back
     */
    public void step(float dtSeconds, float anchorX, float anchorY, float anchorZ,
                     float forwardX, float forwardZ, float speedBlocks) {
        if (!initialised) {
            reset(anchorX, anchorY, anchorZ);
            return;
        }
        // Clamp so a stall cannot bank more work than a few slices.
        accumulator += Math.min(dtSeconds, FIXED_DT * MAX_SUBSTEPS);
        int steps = 0;
        while (accumulator >= FIXED_DT && steps < MAX_SUBSTEPS) {
            integrate(FIXED_DT, speedBlocks);
            accumulator -= FIXED_DT;
            steps++;
        }
        if (steps == MAX_SUBSTEPS) {
            accumulator = 0f;
        }
        place(anchorX, anchorY, anchorZ);
    }

    private void integrate(float dt, float speedBlocks) {
        // A full-speed run leans the panel fully back; slower movement leans proportionally less,
        // and a still player's target is exactly zero, so the cape hangs straight.
        float speed = Float.isFinite(speedBlocks) ? Math.max(0f, speedBlocks) : 0f;
        float fraction = Math.min(1f, speed / FULL_SWING_SPEED);
        float target = -MAX_SWING_DEG * fraction;
        // First-order lag: an exponential approach whose per-step factor derives from the time
        // constant, so the settling time does not change with the timestep.
        float alpha = 1f - (float) Math.exp(-dt / SWING_SMOOTHING_SECONDS);
        swingDeg += (target - swingDeg) * alpha;
    }

    /** Positions every particle as one rigid rotation of the rest quad about the top hinge. */
    private void place(float anchorX, float anchorY, float anchorZ) {
        double radians = Math.toRadians(swingDeg);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        for (int i = 0; i < px.length; i++) {
            float lx = restX[i];
            float ly = restY[i];
            // Rotation about the world X axis through the hinge at the top centre.
            px[i] = anchorX + lx;
            py[i] = anchorY + ly * cos;
            pz[i] = anchorZ + ly * sin;
        }
    }

    public int size() {
        return px.length;
    }

    public float x(int i) { return px[i]; }
    public float y(int i) { return py[i]; }
    public float z(int i) { return pz[i]; }

    /** Row index of a particle, for drawing the mesh in order. */
    public int rowOf(int i) { return i / COLS; }
    public int colOf(int i) { return i % COLS; }
}
