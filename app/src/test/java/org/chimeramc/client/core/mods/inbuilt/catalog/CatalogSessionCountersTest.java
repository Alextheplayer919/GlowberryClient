package org.chimeramc.client.core.mods.inbuilt.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The session counters behind the client's own combat readouts.
 *
 * <p>Pure, so the counting rules are pinned here. The important ones are the reset-on-death rule
 * (a death is the only thing that ends a streak, so it is the single reset point) and the
 * divide-by-zero guard on K/D, which would otherwise render "NaN" or crash the readout.
 */
public class CatalogSessionCountersTest {

    @Test
    public void comboCountsUpAndEndsOnDeath() {
        CatalogSessionCounters counters = new CatalogSessionCounters();
        assertEquals(1, counters.onLandedHit());
        assertEquals(2, counters.onLandedHit());
        counters.onDeath();
        assertEquals(0, counters.combo());
    }

    @Test
    public void aDeathEndsTheStreakButKeepsTheKillTally() {
        CatalogSessionCounters counters = new CatalogSessionCounters();
        counters.onCreditedKill();
        counters.onCreditedKill();
        assertEquals(2, counters.streak());
        assertEquals(2, counters.kills());

        counters.onDeath();
        assertEquals(0, counters.streak());
        assertEquals(2, counters.kills());
        assertEquals(1, counters.deaths());
    }

    @Test
    public void theBestStreakSurvivesLaterDeaths() {
        CatalogSessionCounters counters = new CatalogSessionCounters();
        for (int i = 0; i < 5; i++) counters.onCreditedKill();
        counters.onDeath();
        counters.onCreditedKill();
        assertEquals(1, counters.streak());
        assertEquals(5, counters.bestStreak());
    }

    @Test
    public void kdrIsGuardedAgainstNoDeaths() {
        CatalogSessionCounters counters = new CatalogSessionCounters();
        assertEquals("0.00", counters.kdr());
        counters.onCreditedKill();
        assertEquals("1.00", counters.kdr());
        counters.onDeath();
        assertEquals("1.00", counters.kdr());
    }

    @Test
    public void resetClearsEverything() {
        CatalogSessionCounters counters = new CatalogSessionCounters();
        counters.onLandedHit();
        counters.onCreditedKill();
        counters.onDeath();
        counters.reset();
        assertEquals(0, counters.combo());
        assertEquals(0, counters.kills());
        assertEquals(0, counters.deaths());
        assertEquals(0, counters.streak());
        assertEquals(0, counters.bestStreak());
    }

    @Test
    public void killsAreNotStreakResets() {
        // A kill must never lower the tally; a regression that reset kills on a kill would show
        // "1 kill" forever and is silent on a build machine.
        CatalogSessionCounters counters = new CatalogSessionCounters();
        counters.onCreditedKill();
        counters.onCreditedKill();
        counters.onCreditedKill();
        assertTrue(counters.kills() >= counters.streak());
        assertFalse(counters.kills() < 3);
    }
}
