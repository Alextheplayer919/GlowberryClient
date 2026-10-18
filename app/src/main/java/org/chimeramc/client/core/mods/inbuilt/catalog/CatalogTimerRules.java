package org.chimeramc.client.core.mods.inbuilt.catalog;

/**
 * Which input-driven cooldown readouts each player action arms, and for how long.
 *
 * <p>The readout timers all start from an input the launcher already sees (a swing, a pearl, a
 * gapple, a shield raise). Keeping that mapping here rather than inline in the runtime does two
 * things: the runtime stays a thin caller, and the rule is unit-testable without an Activity — a
 * timer that is wired into the menu but never armed is exactly the "enabled but not working"
 * defect, and it is invisible on a build machine unless a test pins it.
 *
 * <p>Pure and Android-free. The durations are the game's own where they are known (a swing is
 * 0.625&nbsp;s) and a documented nominal window otherwise; every readout is worded "since last use"
 * so none of them is presented as the game's authoritative value.
 */
public final class CatalogTimerRules {

    private CatalogTimerRules() {
    }

    /** One timer to arm: the readout id and its countdown length. */
    public static final class Arm {
        public final String modId;
        public final long durationMs;

        Arm(String modId, long durationMs) {
            this.modId = modId;
            this.durationMs = durationMs;
        }
    }

    /** A swing of the attack button: the crit window, the attack cooldown and the indicator. */
    public static final long SWING_MS = 625L;
    /** A thrown ender pearl. */
    public static final long PEARL_MS = 1000L;
    /** A golden apple's effect window. */
    public static final long GAPPLE_MS = 1600L;
    /** A shield disable window (an axe breaking a shield); a nominal window, not the game's value. */
    public static final long SHIELD_MS = 5000L;
    /** The hit-marker flash on the client's own landed hit. */
    public static final long HIT_MARKER_MS = 250L;

    /** The timers a swing arms. */
    public static Arm[] forAttack() {
        return new Arm[]{
                new Arm("combat_crit_timer", SWING_MS),
                new Arm("combat_attack_cooldown", SWING_MS),
                new Arm("combat_attack_indicator", SWING_MS),
        };
    }

    /**
     * The timers an item use arms.
     *
     * <p>The shield timer is armed here on purpose: in Bedrock the off-hand use key raises a
     * shield, so it is the same input that starts the window. It was previously wired into the
     * readout set but never armed by any action, so it showed the empty state forever.
     */
    public static Arm[] forUse() {
        return new Arm[]{
                new Arm("combat_pearl_timer", PEARL_MS),
                new Arm("combat_gapple_timer", GAPPLE_MS),
                new Arm("combat_shield_timer", SHIELD_MS),
        };
    }
}
