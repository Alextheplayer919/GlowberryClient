package org.chimeramc.client.core.mods.inbuilt.catalog;

/**
 * The per-session counters behind the client's own combat readouts (combo, kills, deaths, K/D,
 * streak).
 *
 * <p>Every one of these counts something the <em>client</em> can observe from its own input and
 * the local-player feed: a landed attack from the same signal Select Hit uses, a death from the
 * health feed, a credited kill from the existing credit heuristic. None of them is a server value,
 * and the scope notes say so — this class only holds the tally.
 *
 * <p>Pure and Android-free so the counting rules are unit-testable: the monotonic combo, the
 * reset-on-death behaviour and the K/D guard against a divide-by-zero are all pinned without a
 * device.
 */
public final class CatalogSessionCounters {

    private int combo;
    private int kills;
    private int deaths;
    private int streak;
    private int bestStreak;

    /** Records a landed attack and returns the new combo length. */
    public int onLandedHit() {
        combo++;
        return combo;
    }

    /**
     * Records a death: the combo ends, the death tally rises, and the streak resets.
     *
     * <p>A death is the only thing that ends a streak, so this is the single reset point rather
     * than a scatter of clears at each call site.
     */
    public int onDeath() {
        combo = 0;
        streak = 0;
        deaths++;
        return deaths;
    }

    /** Records a credited kill and returns the new streak length. */
    public int onCreditedKill() {
        kills++;
        streak++;
        if (streak > bestStreak) bestStreak = streak;
        return streak;
    }

    public int combo() {
        return combo;
    }

    public int kills() {
        return kills;
    }

    public int deaths() {
        return deaths;
    }

    public int streak() {
        return streak;
    }

    public int bestStreak() {
        return bestStreak;
    }

    /** The session K/D as a display string, guarded against a divide-by-zero. */
    public String kdr() {
        if (deaths == 0) return kills + ".00";
        return String.format(java.util.Locale.US, "%.2f", kills / (double) deaths);
    }

    /** Clears every counter; called when a game session starts. */
    public void reset() {
        combo = 0;
        kills = 0;
        deaths = 0;
        streak = 0;
        bestStreak = 0;
    }
}
