package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the Closet's accessory split: every catalogue accessory is either a Hat or a Back piece, never
 * both and never neither. A category that dropped entries would hide cosmetics with no error.
 */
public class ClosetCategoryTest {

    @Test
    public void everyAccessoryIsEitherAHatOrABackPiece() {
        int hats = 0;
        int back = 0;
        for (CosmeticCatalog.Accessory a : CosmeticCatalog.accessories()) {
            if (CosmeticCatalog.NONE.equals(a.id)) continue;
            boolean hat = ClosetCategory.isHat(a);
            boolean isBack = ClosetCategory.isBack(a);
            assertTrue("accessory " + a.id + " is neither hat nor back", hat || isBack);
            assertFalse("accessory " + a.id + " is both hat and back", hat && isBack);
            if (hat) hats++;
            if (isBack) back++;
        }
        assertTrue("expected some hats", hats > 0);
        assertTrue("expected some back pieces", back > 0);
    }

    @Test
    public void theNoneEntryIsNeitherCategory() {
        CosmeticCatalog.Accessory none = CosmeticCatalog.equippedAccessory(CosmeticCatalog.NONE);
        if (none != null) {
            assertFalse(ClosetCategory.isHat(none));
            assertFalse(ClosetCategory.isBack(none));
        }
    }

    @Test
    public void backWornKindsAreExactlyTheOnesThatDoNotFollowTheHead() {
        // The split is defined by followsHead(), so a kind added to one list cannot silently change
        // category: this asserts the two rules agree on the shipped catalogue.
        for (CosmeticCatalog.Accessory a : CosmeticCatalog.accessories()) {
            if (CosmeticCatalog.NONE.equals(a.id)) continue;
            assertEquals(a.kind + " category disagrees with followsHead",
                    a.kind.followsHead(), ClosetCategory.isHat(a));
        }
    }
}
