package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the cosmetic-sync wire format and registry, with no network or device.
 *
 * <p>The protocol is deliberately separate from voice's, on its own magic bytes, so a cosmetic
 * datagram can never be mistaken for an audio one. That separation is asserted here.
 */
public class CosmeticSyncTest {

    @Test
    public void roundTripPreservesEveryId() {
        byte[] data = CosmeticSyncProtocol.encode("peer-1", "Alex",
                "cape_chimera", "acc_crown", "pet_cat");
        CosmeticSyncProtocol.Advert advert = CosmeticSyncProtocol.decode(data);
        assertNotNull(advert);
        assertEquals("peer-1", advert.peerId);
        assertEquals("Alex", advert.name);
        assertEquals("cape_chimera", advert.capeId);
        assertEquals("acc_crown", advert.accessoryId);
        assertEquals("pet_cat", advert.petId);
    }

    /** Missing cosmetics round-trip as {@link CosmeticCatalog#NONE}, never as garbage. */
    @Test
    public void nullIdsRoundTripAsNone() {
        byte[] data = CosmeticSyncProtocol.encode("p", "n", null, null, null);
        CosmeticSyncProtocol.Advert advert = CosmeticSyncProtocol.decode(data);
        assertNotNull(advert);
        assertEquals(CosmeticCatalog.NONE, advert.capeId);
        assertEquals(CosmeticCatalog.NONE, advert.accessoryId);
        assertEquals(CosmeticCatalog.NONE, advert.petId);
    }

    /** A voice datagram must not decode as a cosmetic one, and vice versa: different magic. */
    @Test
    public void foreignAndMalformedDatagramsAreRejected() {
        assertNull(CosmeticSyncProtocol.decode(null));
        assertNull(CosmeticSyncProtocol.decode(new byte[0]));
        assertNull(CosmeticSyncProtocol.decode("CVhello".getBytes()));
        assertNull(CosmeticSyncProtocol.decode(new byte[]{'C', 'S', 99, 1}));
        // The correct magic but a truncated body must not throw; it reads as "no advert".
        assertNull(CosmeticSyncProtocol.decode(new byte[]{'C', 'S',
                CosmeticSyncProtocol.VERSION, CosmeticSyncProtocol.TYPE_ADVERTISE, 0, 5}));
    }

    /** A request carries no cosmetics and must not be stored as one; it only prompts a reply. */
    @Test
    public void aRequestIsDecodedButNotStored() {
        byte[] data = CosmeticSyncProtocol.encodeRequest("peer-1", "Alex");
        CosmeticSyncProtocol.Advert advert = CosmeticSyncProtocol.decode(data);
        assertNotNull(advert);
        assertTrue(advert.isRequest());
        assertEquals("peer-1", advert.peerId);

        CosmeticSyncRegistry registry = new CosmeticSyncRegistry();
        registry.put(advert);
        assertEquals("a request is not an advert", 0, registry.size());
    }

    @Test
    public void registryKeepsTheLatestAdvertPerPeer() {
        CosmeticSyncRegistry registry = new CosmeticSyncRegistry();
        registry.put(CosmeticSyncProtocol.decode(CosmeticSyncProtocol.encode(
                "p1", "A", "cape_chimera", null, null)));
        registry.put(CosmeticSyncProtocol.decode(CosmeticSyncProtocol.encode(
                "p1", "A", "cape_ember", null, null)));
        registry.put(CosmeticSyncProtocol.decode(CosmeticSyncProtocol.encode(
                "p2", "B", null, null, null)));

        assertEquals(2, registry.size());
        assertEquals("cape_ember", registry.get("p1").capeId);
        registry.remove("p1");
        assertEquals(1, registry.size());
        assertNull(registry.get("p1"));
    }

    /** An advert with no peer id cannot be stored: it could never be addressed or removed. */
    @Test
    public void anAdvertWithoutAPeerIdIsIgnored() {
        CosmeticSyncRegistry registry = new CosmeticSyncRegistry();
        registry.put(CosmeticSyncProtocol.decode(CosmeticSyncProtocol.encode(
                "", "A", "cape_chimera", null, null)));
        assertEquals(0, registry.size());
    }

    /** Resolution is local: an unknown id is no cape, not a crash and not a placeholder. */
    @Test
    public void unknownIdsResolveToNoCosmetic() {
        CosmeticSyncRegistry registry = new CosmeticSyncRegistry();
        registry.put(CosmeticSyncProtocol.decode(CosmeticSyncProtocol.encode(
                "p1", "A", "cape_that_does_not_exist", null, null)));
        CosmeticSyncRegistry reg = registry;
        assertNull(reg.capeFor("p1"));

        CosmeticCatalog.Cape real = CosmeticCatalog.capes().get(1);
        registry.put(CosmeticSyncProtocol.decode(CosmeticSyncProtocol.encode(
                "p2", "B", real.id, null, null)));
        assertSame(real, registry.capeFor("p2"));
    }

    /** A peer not heard within the TTL is pruned and reported so its overrides can be dropped. */
    @Test
    public void aDepartedPeerIsPrunedAfterTheTtl() {
        CosmeticSyncRegistry registry = new CosmeticSyncRegistry();
        registry.put(CosmeticSyncProtocol.decode(CosmeticSyncProtocol.encode(
                "p1", "A", "cape_chimera", null, null)), 1000L);
        registry.put(CosmeticSyncProtocol.decode(CosmeticSyncProtocol.encode(
                "p2", "B", "cape_ember", null, null)), 5000L);

        // At t=6000 with a 30s TTL, neither is gone.
        assertTrue(registry.pruneOlderThan(6000L, 30000L).isEmpty());
        assertEquals(2, registry.peerCount());

        // At t=32000, p1 (heard at 1000) is gone; p2 (heard at 5000) is not (32000-5000=27000).
        java.util.List<String> gone = registry.pruneOlderThan(32000L, 30000L);
        assertEquals(1, gone.size());
        assertEquals("p1", gone.get(0));
        assertNull(registry.get("p1"));
        assertNotNull(registry.get("p2"));
    }

    /** A repeated advertisement refreshes the last-seen time and keeps the peer alive. */
    @Test
    public void aRefreshKeepsAPeerAlivePastTheTtl() {
        CosmeticSyncRegistry registry = new CosmeticSyncRegistry();
        registry.put(CosmeticSyncProtocol.decode(CosmeticSyncProtocol.encode(
                "p1", "A", "cape_chimera", null, null)), 1000L);
        // Heard again at 25000 (within 30s of 1000), so a prune at 40000 (25000+15000) keeps it.
        registry.put(CosmeticSyncProtocol.decode(CosmeticSyncProtocol.encode(
                "p1", "A", "cape_chimera", null, null)), 25000L);
        assertTrue(registry.pruneOlderThan(40000L, 30000L).isEmpty());
        assertEquals(1, registry.peerCount());
    }
}
