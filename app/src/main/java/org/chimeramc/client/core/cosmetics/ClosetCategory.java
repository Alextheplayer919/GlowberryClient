package org.chimeramc.client.core.cosmetics;

/**
 * The Closet's category split for accessories: head-worn pieces are "Hats", back-worn pieces are
 * "Back". Pure so a test can prove every catalogue accessory lands in exactly one of the two — a
 * split that silently dropped entries would hide cosmetics from the screen with no error.
 */
public final class ClosetCategory {

    private ClosetCategory() {
    }

    /** True when the accessory belongs on the "Hats" card grid (worn on or around the head). */
    public static boolean isHat(CosmeticCatalog.Accessory accessory) {
        return accessory != null
                && !CosmeticCatalog.NONE.equals(accessory.id)
                && accessory.kind.followsHead();
    }

    /** True when the accessory belongs on the "Back" card grid (worn on the back/neck). */
    public static boolean isBack(CosmeticCatalog.Accessory accessory) {
        return accessory != null
                && !CosmeticCatalog.NONE.equals(accessory.id)
                && !accessory.kind.followsHead();
    }
}
