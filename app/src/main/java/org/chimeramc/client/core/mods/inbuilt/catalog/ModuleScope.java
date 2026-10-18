package org.chimeramc.client.core.mods.inbuilt.catalog;

/**
 * The data route a catalogue module needs to produce a result, and whether this build has it.
 *
 * <p>The catalogue lists 200+ modules. Every one of them was previously either a link to a shipped
 * module or a roadmap entry that the menu greyed out with a single shared "no game data" reason.
 * That was honest but coarse: it told the player a module did not work without saying <em>why</em>,
 * and it hid the large group that can work through a route this build already owns (device input,
 * the local-player feed, the peer feed, the launcher host, the game options file).
 *
 * <p>This enum is the classification. {@link #isReachable()} is the single predicate that decides
 * whether a module gets a real toggle; the reason text is what the menu shows when it does not. The
 * classification is a pure value, so it is unit-testable and the badge, the grey-out and the
 * documented limitation all read the same field.
 *
 * <p>It is deliberately not the same axis as {@code PvpCategory}: the category is navigation
 * (Combat / Movement / …), the scope is capability. A module keeps its category and gains a scope.
 */
public enum ModuleScope {

    /**
     * Acts only on the device's own input or the launcher's own state: overlays, key injection,
     * chat/command injection, the client's own click counter. No game data is read at all, so it
     * works on every build the launcher runs on.
     */
    DEVICE_INPUT(true),

    /**
     * Reads or writes the instance's {@code minecraftpe/options.txt}. The game applies the value on
     * its next load, so the module is a settings writer, not a live hook.
     */
    GAME_OPTIONS(true),

    /**
     * Reads the local player's position and view through the already-installed native local-player
     * feed (the {@code ClientInstance} local-player slot). The offsets are verified on the shipped
     * builds, so this route is live; it degrades to an honest empty state when the feed is absent.
     */
    LOCAL_FEED(true),

    /**
     * Reads other players from the proximity-voice peer feed, which needs no game hook at all.
     * Vanilla players are invisible to it, and the module says so.
     */
    PEER_FEED(true),

    /**
     * Acts on the game's rendered frame or its entity/block list. This build exposes no such feed
     * to a mod (game classes export no symbols), so the module is badged and its toggle is refused
     * rather than pretending to work.
     */
    ENGINE_HOOK(false),

    /**
     * Describes an outcome the server decides (reach, knockback, crits, TPS) or that automating the
     * client would be a ban risk. Not shipped on purpose; the scope note says which.
     */
    SERVER_DECIDED(false);

    private final boolean reachable;

    ModuleScope(boolean reachable) {
        this.reachable = reachable;
    }

    /** True when this build has the route the module needs, so it may get a real toggle. */
    public boolean isReachable() {
        return reachable;
    }
}
