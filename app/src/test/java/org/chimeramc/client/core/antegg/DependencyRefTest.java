package org.chimeramc.client.core.antegg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Pins dependency-entry parsing with inline ranges (item G4). */
public class DependencyRefTest {

    @Test
    public void plainNameParsesWithoutRange() {
        DependencyRef ref = DependencyRef.parse("libfoo");
        assertNotNull(ref);
        assertEquals("libfoo", ref.id);
        assertNull(ref.range);
    }

    @Test
    public void slugNormalisesTheId() {
        DependencyRef ref = DependencyRef.parse("My Cool Mod");
        assertNotNull(ref);
        assertEquals("my-cool-mod", ref.id);
    }

    @Test
    public void rangeSuffixParses() {
        DependencyRef ref = DependencyRef.parse("libfoo>=2.0.0");
        assertNotNull(ref);
        assertEquals("libfoo", ref.id);
        assertNotNull(ref.range);
        assertTrue(ref.range.matches("2.1.0"));
        assertTrue(!ref.range.matches("1.9.0"));
    }

    @Test
    public void tildeAndCaretSuffixesParse() {
        DependencyRef ref1 = DependencyRef.parse("libfoo ~1.2");
        assertNotNull(ref1);
        assertNotNull(ref1.range);
        assertTrue(ref1.range.matches("1.2.7"));

        DependencyRef ref2 = DependencyRef.parse("libfoo^2.0.0");
        assertNotNull(ref2);
        assertTrue(ref2.range.matches("2.9.0"));
    }

    @Test
    public void malformedEntriesAreRejected() {
        assertNull(DependencyRef.parse(null));
        assertNull(DependencyRef.parse(""));
        assertNull(DependencyRef.parse("   "));
        assertNull(DependencyRef.parse("libfoo>="));
        assertNull(DependencyRef.parse(">=2.0.0"));
    }
}