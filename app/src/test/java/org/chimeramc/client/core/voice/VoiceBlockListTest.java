package org.chimeramc.client.core.voice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * Pins the persistent block list: the stable key rule, the reconnect behaviour, and the
 * serialise/restore round trip. No mocks — the class is pure.
 */
public class VoiceBlockListTest {

    private static VoicePeer peer(String id, String deviceId) {
        return new VoicePeer(id, "Player", 0f, 0f, 0f, VoiceChannel.WORLD, "World",
                VoiceProtocol.VISIBILITY_PUBLIC, VoiceProtocol.CAPACITY_NONE, 0f, false,
                0f, 0f, 0L, deviceId);
    }

    @Test
    public void deviceIdIsPreferredSoABlockSurvivesAReconnect() {
        VoiceBlockList list = new VoiceBlockList();
        VoicePeer first = peer("sess1", "device-abc");
        assertTrue(list.block(VoiceBlockList.keyFor(first)));
        assertEquals("device-abc", VoiceBlockList.keyFor(first));

        // Same device, new session id: still blocked.
        VoicePeer reconnect = peer("sess2", "device-abc");
        assertTrue(list.isBlocked(reconnect));
    }

    @Test
    public void aPeerWithNoDeviceIdFallsBackToTheSessionId() {
        VoicePeer legacy = peer("sess9", "");
        assertEquals("sess9", VoiceBlockList.keyFor(legacy));
        assertTrue(VoiceBlockList.canBlock(legacy));

        VoiceBlockList list = new VoiceBlockList();
        assertTrue(list.block(VoiceBlockList.keyFor(legacy)));
        assertTrue(list.isBlocked(legacy));
    }

    @Test
    public void aPeerWithNeitherIdCannotBeBlocked() {
        VoicePeer anonymous = peer("", "");
        assertFalse(VoiceBlockList.canBlock(anonymous));
        assertFalse(VoiceBlockList.canBlock(null));
        assertTrue(VoiceBlockList.keyFor(anonymous).isEmpty());
    }

    @Test
    public void blankKeysAreIgnoredAndUnblockIsIdempotent() {
        VoiceBlockList list = new VoiceBlockList();
        assertFalse(list.block(null));
        assertFalse(list.block("   "));
        assertTrue(list.isEmpty());

        assertTrue(list.block("device-abc"));
        assertFalse(list.block("device-abc")); // already present, nothing changed
        assertEquals(1, list.size());

        assertTrue(list.unblock("device-abc"));
        assertFalse(list.unblock("device-abc"));
        assertTrue(list.isEmpty());
    }

    @Test
    public void theKeyIsTrimmedOnBothWriteAndRead() {
        VoiceBlockList list = new VoiceBlockList();
        list.block("  device-abc  ");
        assertTrue(list.isBlocked("device-abc"));
        assertTrue(list.isBlocked("  device-abc  "));
    }

    @Test
    public void serialiseAndRestoreRoundTripDropsBlanks() {
        VoiceBlockList list = new VoiceBlockList();
        list.restoreFrom(Arrays.asList("device-a", "", "  ", "device-b"));
        assertEquals("device-a,device-b", list.serialize());

        VoiceBlockList restored = new VoiceBlockList();
        restored.restoreFrom(Arrays.asList(list.serialize().split(",")));
        assertEquals(2, restored.size());
        assertTrue(restored.isBlocked("device-a"));
        assertTrue(restored.isBlocked("device-b"));
    }

    @Test
    public void restoreFromNullClearsTheSet() {
        VoiceBlockList list = new VoiceBlockList();
        list.block("device-a");
        list.restoreFrom(null);
        assertTrue(list.isEmpty());
        list.block("device-a");
        list.restoreFrom(Collections.emptyList());
        assertTrue(list.isEmpty());
    }

    @Test
    public void theSnapshotIsAReadOnlyCopy() {
        VoiceBlockList list = new VoiceBlockList();
        list.block("device-a");
        java.util.Set<String> snapshot = list.snapshot();
        try {
            snapshot.add("device-b");
            // A copy must not let a caller mutate the live set through the snapshot.
            assertFalse("snapshot was mutable", true);
        } catch (UnsupportedOperationException expected) {
            // the snapshot is unmodifiable, which is the contract
        }
        assertFalse(list.isBlocked("device-b"));
    }
}