package org.chimeramc.client.core.content;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * Hand-rolled RakNet unconnected ping for the server list (G14).
 *
 * <p>The bundled Cloudburst library provides {@code BedrockPong} (parsing) but ships no RakNet
 * transport, so the wire side is implemented here with a plain UDP {@link DatagramSocket}: send
 * {@code 0x01 + 8-byte big-endian timestamp + 16-byte offline-message magic}, read the
 * {@code 0x1c} pong, verify the magic, then hand the MOTD payload to
 * {@link ServerPongInfo#parse}. Request building and response parsing are static and
 * byte-for-byte testable without a device; only {@link #ping} touches the network.
 */
public final class RakNetPing {

    /** RakNet {@code ID_UNCONNECTED_PING}. */
    public static final int REQUEST_ID = 0x01;
    /** RakNet {@code ID_UNCONNECTED_PONG}. */
    public static final int RESPONSE_ID = 0x1c;

    /**
     * The 16-byte offline-message magic every RakNet unconnected packet carries:
     * {@code 00 ff ff 00 fe fe fe fe fd fd fd fd 12 34 56 78}.
     */
    static final byte[] MAGIC = {
            0x00, (byte) 0xff, (byte) 0xff, 0x00,
            (byte) 0xfe, (byte) 0xfe, (byte) 0xfe, (byte) 0xfe,
            (byte) 0xfd, (byte) 0xfd, (byte) 0xfd, (byte) 0xfd,
            0x12, 0x34, 0x56, 0x78
    };

    private static final int MAX_RESPONSE_BYTES = 4096;

    private RakNetPing() {}

    /** Builds the raw request datagram for the given big-endian timestamp. */
    static byte[] buildRequest(long timestamp) {
        byte[] data = new byte[1 + 8 + MAGIC.length];
        data[0] = (byte) REQUEST_ID;
        writeLongBe(data, 1, timestamp);
        System.arraycopy(MAGIC, 0, data, 9, MAGIC.length);
        return data;
    }

    /**
     * Parses an unconnected pong datagram. Returns {@code null} for anything that is not a pong
     * (drop safety: the socket may deliver an unrelated packet, and Java Edition replies are not
     * this shape either).
     *
     * <p>The pong framing is {@code 0x1c + 8-byte time + 8-byte server GUID + 16-byte magic +
     * 2-byte length + UTF-8 MOTD}. The GUID preceding the magic was verified against a live
     * Bedrock server; an implementation that validates the magic directly after the timestamp
     * (the request's shape) rejects every real pong.
     */
    public static ServerPongInfo parseResponse(byte[] response) {
        if (response == null || response.length < 1 + 8 + 8 + MAGIC.length + 2) {
            return null;
        }
        if ((response[0] & 0xFF) != RESPONSE_ID) {
            return null;
        }
        int offset = 1;
        offset += 8; // echo of the request timestamp
        offset += 8; // server GUID
        for (int i = 0; i < MAGIC.length; i++) {
            if (response[offset + i] != MAGIC[i]) {
                return null;
            }
        }
        offset += MAGIC.length;
        if (offset + 2 > response.length) {
            return null;
        }
        int length = ((response[offset] & 0xFF) << 8) | (response[offset + 1] & 0xFF);
        offset += 2;
        if (length < 0 || offset + length > response.length) {
            length = Math.max(0, response.length - offset);
        }
        String pong = new String(response, offset, length, StandardCharsets.UTF_8);
        return ServerPongInfo.parse(pong);
    }

    /**
     * Sends one unconnected ping and waits for the pong.
     *
     * @return the parsed pong, or {@code null} on timeout / I/O error / malformed reply
     */
    public static ServerPongInfo ping(String host, int port, int timeoutMs) {
        if (host == null || host.isEmpty() || port <= 0) {
            return null;
        }
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(timeoutMs);
            InetAddress address = InetAddress.getByName(host);
            socket.connect(address, port);

            byte[] request = buildRequest(System.currentTimeMillis());
            socket.send(new DatagramPacket(request, request.length));

            byte[] buffer = new byte[MAX_RESPONSE_BYTES];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            if (packet.getLength() >= buffer.length) {
                return null;
            }
            byte[] payload = new byte[packet.getLength()];
            System.arraycopy(buffer, 0, payload, 0, packet.getLength());
            return parseResponse(payload);
        } catch (SocketTimeoutException e) {
            return null;
        } catch (IOException | RuntimeException e) {
            return null;
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    private static void writeLongBe(byte[] data, int offset, long value) {
        for (int i = 0; i < 8; i++) {
            data[offset + i] = (byte) (value >>> (56 - 8 * i));
        }
    }
}