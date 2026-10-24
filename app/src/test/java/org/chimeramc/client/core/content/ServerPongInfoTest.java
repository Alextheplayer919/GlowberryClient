package org.chimeramc.client.core.content;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Pins the RakNet unconnected pong parsing (G14): the request byte layout, the pong response
 * framing, the MCPE MOTD field mapping and the compatibility verdict. No network is touched.
 */
public class ServerPongInfoTest {

    private static final byte[] MAGIC = RakNetPing.MAGIC;

    private static final String FULL_MOTD =
            "MCPE;A Crafty Server;766;1.21.60;12;30;1234567890;A sub;Survival;0;19132;https://example.com";

    @Test
    public void buildRequestIsIdTimeMagic() {
        byte[] request = RakNetPing.buildRequest(0x0102030405060708L);
        assertEquals(25, request.length);
        assertEquals((byte) RakNetPing.REQUEST_ID, request[0]);
        assertEquals(0x01, request[1] & 0xFF);
        assertEquals(0x02, request[2] & 0xFF);
        assertEquals(0x03, request[3] & 0xFF);
        assertEquals(0x04, request[4] & 0xFF);
        assertEquals(0x05, request[5] & 0xFF);
        assertEquals(0x06, request[6] & 0xFF);
        assertEquals(0x07, request[7] & 0xFF);
        assertEquals(0x08, request[8] & 0xFF);
        for (int i = 0; i < MAGIC.length; i++) {
            assertEquals(MAGIC[i], request[9 + i]);
        }
    }

    @Test
    public void parseResponseReadsFramedPong() {
        byte[] payload = pongPayload(FULL_MOTD);
        ServerPongInfo info = RakNetPing.parseResponse(payload);
        assertNotNull(info);
        assertFalse(info.parseError);
        assertTrue(info.isBedrock());
        assertEquals("A Crafty Server", info.motd);
        assertEquals(766, info.protocolVersion);
        assertEquals("1.21.60", info.gameVersion);
        assertEquals(12, info.playersOnline);
        assertEquals(30, info.maxPlayers);
        assertEquals(1234567890L, info.serverId);
        assertEquals("Survival", info.gameType);
        assertEquals(19132, info.ipv4Port);
    }

    @Test
    public void parseRejectsNonBedrockAndShortPongs() {
        assertNull(ServerPongInfo.parse("1;Survival;765"));
        assertNull(ServerPongInfo.parse("MCPE;a;b"));
        assertNull(ServerPongInfo.parse(null));
    }

    @Test
    public void parseHandlesMissingTrailingFields() {
        ServerPongInfo info = ServerPongInfo.parse("MCPE;motd;766;1.21.60;5;10");
        assertNotNull(info);
        assertEquals(766, info.protocolVersion);
        assertEquals(5, info.playersOnline);
        assertEquals(10, info.maxPlayers);
        assertEquals("", info.subMotd);
    }

    @Test
    public void parseRejectsNonNumericProtocol() {
        assertNull(ServerPongInfo.parse("MCPE;motd;abc;1.21.60;5;10"));
    }

    @Test
    public void compatibilityMatchesOnMajorMinor() {
        ServerPongInfo info = ServerPongInfo.parse(FULL_MOTD);
        assertNotNull(info);
        assertEquals(ServerPongInfo.Compatibility.COMPATIBLE,
                info.compatibilityWith("1.21.60.01"));
        assertEquals(ServerPongInfo.Compatibility.COMPATIBLE,
                info.compatibilityWith("1.21.60"));
        assertEquals(ServerPongInfo.Compatibility.MISMATCH,
                info.compatibilityWith("1.21.40"));
        assertEquals(ServerPongInfo.Compatibility.MISMATCH,
                info.compatibilityWith("26.51.01"));
        assertEquals(ServerPongInfo.Compatibility.UNKNOWN,
                info.compatibilityWith((String) null));
        assertEquals(ServerPongInfo.Compatibility.UNKNOWN,
                info.compatibilityWith(""));
    }

    @Test
    public void patchAwareCompatibility() {
        ServerPongInfo info = ServerPongInfo.parse("MCPE;motd;766;1.21.60;12;30");
        assertNotNull(info);
        // hotfix patch on the same minor: matching patch reads compatible.
        assertEquals(ServerPongInfo.Compatibility.COMPATIBLE,
                info.compatibilityWith("1.21.60.01"));
        // different patch on the same minor reads as a mismatch (different protocol).
        assertEquals(ServerPongInfo.Compatibility.MISMATCH,
                info.compatibilityWith("1.21.61.01"));
        // server lacks a patch, client has one -> falls back to major+minor.
        assertEquals(ServerPongInfo.Compatibility.COMPATIBLE,
                info.compatibilityWith("1.21.60"));
    }

    @Test
    public void responseWithBadMagicIsRejected() {
        byte[] payload = pongPayload("MCPE;motd;766;1.21.60;5;10");
        payload[1 + 8 + 8] = (byte) 0xFF; // corrupt the first magic byte
        assertNull(RakNetPing.parseResponse(payload));
    }

    @Test
    public void responseWithWrongPacketIdIsRejected() {
        byte[] payload = pongPayload("MCPE;motd;766;1.21.60;5;10");
        payload[0] = (byte) 0x02;
        assertNull(RakNetPing.parseResponse(payload));
    }

    @Test
    public void parsesRealBedrockPongWireBytes() {
        // Capture of a live Bedrock server (play.inpvp.net, 2026) with the timestamp zeroed out:
        // 0x1c + 8 time + 8 guid + 16 magic + 2 length + "MCPE;..." MOTD.
        String hex = "1c000001a16a632180bf976859b618c01c00ffff00fefefefefdfdfdfd12345678008a"
                + "4d4350453bf09d958ae28499f09d9586f09d9586f09d9582f09d959020f09d958af09d94bc"
                + "f09d94b8f09d958af09d9586e2849520e298a0efb88f3b323232353b312e32362e36303b3339"
                + "35383b31303030303b2d343634313132363135363436353535333338303b4d696e657669"
                + "6c6c653b537572766976616c3b313b31393133323b31393133323b";
        byte[] payload = hexBytes(hex);
        ServerPongInfo info = RakNetPing.parseResponse(payload);
        assertNotNull(info);
        assertTrue(info.isBedrock());
        assertEquals(2225, info.protocolVersion);
        assertEquals("1.26.60", info.gameVersion);
        assertTrue("players should parse to a sane non-negative number",
                info.playersOnline >= 0);
        assertEquals(10000, info.maxPlayers);
        assertEquals("Mineville", info.subMotd);
        assertEquals(19132, info.ipv4Port);
        assertEquals(19132, info.ipv6Port);
    }

    @Test
    public void truncatedResponsesAreRejected() {
        assertNull(RakNetPing.parseResponse(null));
        assertNull(RakNetPing.parseResponse(new byte[0]));
        assertNull(RakNetPing.parseResponse(new byte[]{0x1c, 0x00}));
    }

    private static byte[] hexBytes(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return out;
    }

private static byte[] pongPayload(String motd) {
        // Real framing, verified against a live Bedrock server:
        // 0x1c + 8 timestamp + 8 server GUID + 16 magic + 2 length + motd
        byte[] motdBytes = motd.getBytes(StandardCharsets.UTF_8);
        int total = 1 + 8 + 8 + 16 + 2 + motdBytes.length;
        byte[] payload = new byte[total];
        payload[0] = (byte) RakNetPing.RESPONSE_ID;
        // timestamp bytes 1..8 stay zero
        int guidOffset = 1 + 8;
        payload[guidOffset + 2] = 0x01; // arbitrary non-zero so layout shifts are visible
        System.arraycopy(MAGIC, 0, payload, guidOffset + 8, MAGIC.length);
        int lengthOffset = guidOffset + 8 + 16;
        payload[lengthOffset] = (byte) (motdBytes.length >> 8);
        payload[lengthOffset + 1] = (byte) (motdBytes.length & 0xFF);
        System.arraycopy(motdBytes, 0, payload, lengthOffset + 2, motdBytes.length);
        return payload;
    }
}