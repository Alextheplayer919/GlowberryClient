package org.chimeramc.client.core.voice;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The listener's persistent block list for voice chat (B15).
 *
 * <p>Distinct from {@link VoiceMutes}: a mute is "not right now" and is keyed by the per-session
 * {@link VoicePeer#id}, so a reconnecting peer is audible again. A block is "never again" and is
 * keyed by the peer's <b>stable device id</b> when the transport carries one (the relay and any
 * v4+ sender do), falling back to the peer id on the v3 multicast path that has no device id.
 *
 * <p>Blocking is enforced locally and is stronger than muting: a blocked peer is dropped before
 * their advertisement is registered, so they do not appear in the member list or the in-world
 * nametag icons, and their audio is never mixed. Nothing about the block is transmitted — the
 * blocked party is never told, exactly as with a mute.
 *
 * <p>Pure and Android-free so the keying rule ("a peer that reconnects under a new session id but
 * the same device id stays blocked") is a unit test.
 */
public final class VoiceBlockList {

    private final Set<String> blocked = new LinkedHashSet<>();

    /**
     * The stable key a peer is blocked under.
     *
     * <p>The device id is preferred because it survives a reconnect; the per-session peer id is
     * the fallback for a transport that does not carry one. A peer with neither is unblockable by
     * key and the caller must treat it as such rather than inventing a key.
     */
    public static String keyFor(VoicePeer peer) {
        if (peer == null) return "";
        if (peer.deviceId != null && !peer.deviceId.trim().isEmpty()) return peer.deviceId.trim();
        return peer.id == null ? "" : peer.id.trim();
    }

    /** Whether a non-blank key can be derived for this peer. */
    public static boolean canBlock(VoicePeer peer) {
        return !keyFor(peer).isEmpty();
    }

    /** Adds a key to the block list; blank/null is ignored. Returns whether it changed anything. */
    public synchronized boolean block(String key) {
        if (key == null) return false;
        String trimmed = key.trim();
        if (trimmed.isEmpty()) return false;
        return blocked.add(trimmed);
    }

    /** Removes a key from the block list. */
    public synchronized boolean unblock(String key) {
        return key != null && blocked.remove(key.trim());
    }

    /** Whether this key is blocked. */
    public synchronized boolean isBlocked(String key) {
        return key != null && blocked.contains(key.trim());
    }

    /** Whether this peer is blocked, resolved through {@link #keyFor}. */
    public synchronized boolean isBlocked(VoicePeer peer) {
        return isBlocked(keyFor(peer));
    }

    public synchronized boolean isEmpty() {
        return blocked.isEmpty();
    }

    public synchronized int size() {
        return blocked.size();
    }

    /** An immutable snapshot of the blocked keys, in insertion order. */
    public synchronized Set<String> snapshot() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(blocked));
    }

    public synchronized void clear() {
        blocked.clear();
    }

    /** Replaces the set from a persisted list, dropping blanks and duplicates. */
    public synchronized void restoreFrom(List<String> keys) {
        blocked.clear();
        if (keys == null) return;
        for (String key : keys) {
            if (key == null) continue;
            String trimmed = key.trim();
            if (!trimmed.isEmpty()) blocked.add(trimmed);
        }
    }

    /** Serialises the set as a comma-separated list for persistence. */
    public synchronized String serialize() {
        StringBuilder builder = new StringBuilder();
        for (String key : blocked) {
            if (builder.length() > 0) builder.append(',');
            builder.append(key);
        }
        return builder.toString();
    }
}