package org.chimeramc.client.core.content;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsed Bedrock server pong from a RakNet unconnected pong (G14).
 *
 * <p>The pong plays the same role as {@code BedrockPong.fromRakNet} from the Cloudburst library,
 * but with no netty ByteBuf dependency. Given the semicolon-separated MOTD payload that Bedrock
 * servers advertise, it extracts the fields the server list actually shows (motd, protocol,
 * version, players, max players) plus the raw edition so callers can tell a Java server's 1.x
 * pong apart from a Bedrock MCPE pong.
 *
 * <p>{@link #compatibilityWith(String)} compares the server's advertised game version against an
 * installed instance's version code on major+minor parts — the client and server must speak the
 * same protocol, and for Bedrock release builds the protocol tracks the major.minor version, so a
 * matching pair is the honest compatible verdict without a hard-coded protocol-number table that
 * would drift with every new Mojang release.
 */
public final class ServerPongInfo {

    private static final String MCPE_PREFIX = "MCPE;";
    private static final Pattern NUMERIC_VERSION = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");
    private static final int UNKNOWN_PORT = -1;

    public final String edition;
    public final String motd;
    public final int protocolVersion;
    public final String gameVersion;
    public final int playersOnline;
    public final int maxPlayers;
    public final long serverId;
    public final String subMotd;
    public final String gameType;
    public final int ipv4Port;
    public final int ipv6Port;
    public final boolean parseError;

    public ServerPongInfo(String edition, String motd, int protocolVersion, String gameVersion,
                          int playersOnline, int maxPlayers, long serverId, String subMotd,
                          String gameType, int ipv4Port, int ipv6Port) {
        this.edition = edition == null ? "" : edition;
        this.motd = motd == null ? "" : motd;
        this.protocolVersion = protocolVersion;
        this.gameVersion = gameVersion == null ? "" : gameVersion;
        this.playersOnline = playersOnline;
        this.maxPlayers = maxPlayers;
        this.serverId = serverId;
        this.subMotd = subMotd == null ? "" : subMotd;
        this.gameType = gameType == null ? "" : gameType;
        this.ipv4Port = ipv4Port;
        this.ipv6Port = ipv6Port;
        this.parseError = false;
    }

    private ServerPongInfo(boolean parseError) {
        this.edition = "";
        this.motd = "";
        this.protocolVersion = -1;
        this.gameVersion = "";
        this.playersOnline = -1;
        this.maxPlayers = -1;
        this.serverId = -1L;
        this.subMotd = "";
        this.gameType = "";
        this.ipv4Port = UNKNOWN_PORT;
        this.ipv6Port = UNKNOWN_PORT;
        this.parseError = parseError;
    }

    public static ServerPongInfo parseError() {
        return new ServerPongInfo(true);
    }

    /**
     * Parses the MOTD string carried by an unconnected pong. Anything that is not a Bedrock MCPE
     * advertisement (Java Edition's 1.x pongs, garbage, empty) yields {@code null}.
     */
    public static ServerPongInfo parse(String pongString) {
        if (pongString == null || !pongString.startsWith(MCPE_PREFIX)) {
            return null;
        }
        String[] parts = pongString.split(";");
        if (parts.length < 6) {
            return null;
        }
        int protocol = parseInt(parts[2]);
        if (protocol < 0) {
            return null;
        }
        return new ServerPongInfo(
                parts[0],
                parts[1],
                protocol,
                parts[3],
                parseInt(parts[4]),
                parseInt(parts[5]),
                parseLongAt(parts, 6),
                parts.length > 7 ? parts[7] : "",
                parts.length > 8 ? parts[8] : "",
                parseIntAt(parts, 10),
                parseIntAt(parts, 11)
        );
    }

    public boolean isBedrock() {
        return !parseError && MCPE_PREFIX.equals(edition + ";");
    }

    /**
     * The installed instance's version code (e.g. {@code 26.51.01} or {@code 1.21.80.02}) against
     * the server's advertised game version. Returns one of {@link Compatibility}.
     *
     * <p>Comparison is on major, minor and, when both sides carry one, patch. Bedrock servers on
     * the same minor number but different patches (1.21.40 vs 1.21.60) speak different protocols
     * and must not read as compatible; a hotfix patch on both sides normally matches. When either
     * side omits the patch, the verdict falls back to major+minor.
     */
    public Compatibility compatibilityWith(String installedVersionCode) {
        if (!isBedrock() || installedVersionCode == null || installedVersionCode.isEmpty()) {
            return Compatibility.UNKNOWN;
        }
        int[] serverParts = numericParts(gameVersion);
        int[] clientParts = numericParts(installedVersionCode);
        if (serverParts == null || clientParts == null) {
            return Compatibility.UNKNOWN;
        }
        if (serverParts[0] != clientParts[0] || serverParts[1] != clientParts[1]) {
            return Compatibility.MISMATCH;
        }
        int serverPatch = serverParts.length > 2 ? serverParts[2] : -1;
        int clientPatch = clientParts.length > 2 ? clientParts[2] : -1;
        if (serverPatch >= 0 && clientPatch >= 0 && serverPatch != clientPatch) {
            return Compatibility.MISMATCH;
        }
        return Compatibility.COMPATIBLE;
    }

    public enum Compatibility {
        COMPATIBLE,
        MISMATCH,
        UNKNOWN
    }

    private static int[] numericParts(String version) {
        Matcher matcher = NUMERIC_VERSION.matcher(version == null ? "" : version);
        if (!matcher.find()) return null;
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        String patch = matcher.group(3);
        return patch == null
                ? new int[]{major, minor}
                : new int[]{major, minor, Integer.parseInt(patch)};
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static int parseIntAt(String[] parts, int index) {
        return index < parts.length ? parseInt(parts[index]) : UNKNOWN_PORT;
    }

    private static long parseLongAt(String[] parts, int index) {
        if (index >= parts.length) return -1L;
        try {
            return Long.parseLong(parts[index].trim());
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}