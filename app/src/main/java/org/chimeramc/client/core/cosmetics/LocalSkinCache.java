package org.chimeramc.client.core.cosmetics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * The on-disk cache of the last captured local skin, so the launcher (which runs before the game)
 * can show the player's real skin after the first game session — no import needed.
 *
 * <p>Two files under {@code files/skins/}: {@code local_skin.bin} (a tiny header + the raw 64x64
 * ARGB atlas) and {@code local_skin.json} (size, arm model, geometry id, capture time and the
 * identity hash the cache is keyed on). The format is written and read here and nowhere else, so it
 * is unit-tested round-trip on the JVM without a device.
 *
 * <p>The identity hash is only ever a cache key; nothing is uploaded.
 */
public final class LocalSkinCache {

    /** The cache directory name, under the app's files dir. */
    public static final String DIR = "skins";
    public static final String BIN_NAME = "local_skin.bin";
    public static final String JSON_NAME = "local_skin.json";

    /** The header magic, "GSK1" little-endian. */
    private static final int MAGIC = 0x314B5347;
    private static final int VERSION = 1;
    private static final int HEADER = 32;  // magic, version, width, height, armModel, time, identity, reserved

    /** A cached skin plus its metadata. */
    public static final class Entry {
        public final int width;
        public final int height;
        public final SkinCaptureValidator.ArmModel armModel;
        public final String geometryId;
        public final long captureTimeMs;
        public final String identityHash;
        public final int[] argb;

        public Entry(int width, int height, SkinCaptureValidator.ArmModel armModel,
                     String geometryId, long captureTimeMs, String identityHash, int[] argb) {
            this.width = width;
            this.height = height;
            this.armModel = armModel;
            this.geometryId = geometryId == null ? "" : geometryId;
            this.captureTimeMs = captureTimeMs;
            this.identityHash = identityHash == null ? "" : identityHash;
            this.argb = argb;
        }
    }

    private LocalSkinCache() {
    }

    /** The cache directory for a files root, created if missing. */
    public static File dir(File filesRoot) {
        File d = new File(filesRoot, DIR);
        if (!d.exists()) d.mkdirs();
        return d;
    }

    /** Writes the entry (atlas + metadata). Returns true on success. */
    public static boolean write(File filesRoot, Entry entry) {
        if (filesRoot == null || entry == null || entry.argb == null) return false;
        File d = dir(filesRoot);
        File bin = new File(d, BIN_NAME);
        File json = new File(d, JSON_NAME);
        try (FileOutputStream out = new FileOutputStream(bin)) {
            byte[] header = new byte[HEADER];
            putInt(header, 0, MAGIC);
            putInt(header, 4, VERSION);
            putInt(header, 8, entry.width);
            putInt(header, 12, entry.height);
            putInt(header, 16, entry.armModel == SkinCaptureValidator.ArmModel.SLIM ? 1 : 0);
            putLong(header, 20, entry.captureTimeMs);
            // Bytes 28..31 reserved.
            out.write(header);
            for (int pixel : entry.argb) {
                putInt(header, 0, pixel);
                out.write(header, 0, 4);
            }
        } catch (IOException e) {
            return false;
        }
        String meta = "{\"width\":" + entry.width + ",\"height\":" + entry.height
                + ",\"arm\":\"" + (entry.armModel == SkinCaptureValidator.ArmModel.SLIM ? "slim" : "wide")
                + "\",\"geometry\":\"" + entry.geometryId.replace("\"", "")
                + "\",\"time\":" + entry.captureTimeMs
                + ",\"identity\":\"" + entry.identityHash.replace("\"", "") + "\"}";
        try (FileOutputStream out = new FileOutputStream(json)) {
            out.write(meta.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /** Reads the cached entry, or null when there is none or it is malformed. */
    public static Entry read(File filesRoot) {
        if (filesRoot == null) return null;
        File bin = new File(dir(filesRoot), BIN_NAME);
        File json = new File(dir(filesRoot), JSON_NAME);
        if (!bin.isFile()) return null;
        try (FileInputStream in = new FileInputStream(bin)) {
            byte[] header = new byte[HEADER];
            if (in.read(header) != HEADER) return null;
            if (getInt(header, 0) != MAGIC) return null;
            if (getInt(header, 4) != VERSION) return null;
            int width = getInt(header, 8);
            int height = getInt(header, 12);
            SkinCaptureValidator.ArmModel arm = getInt(header, 16) == 1
                    ? SkinCaptureValidator.ArmModel.SLIM : SkinCaptureValidator.ArmModel.WIDE;
            long time = getLong(header, 20);
            int count = width * height;
            if (count <= 0 || count > 4096 * 4096) return null;
            int[] argb = new int[count];
            byte[] pixel = new byte[4];
            for (int i = 0; i < count; i++) {
                if (in.read(pixel) != 4) return null;
                argb[i] = getInt(pixel, 0);
            }
            String geometry = "";
            String identity = "";
            if (json.isFile()) {
                byte[] raw = new byte[(int) Math.min(json.length(), 4096)];
                try (FileInputStream jin = new FileInputStream(json)) {
                    int n = jin.read(raw);
                    if (n > 0) {
                        String text = new String(raw, 0, n, StandardCharsets.UTF_8);
                        geometry = jsonString(text, "geometry");
                        identity = jsonString(text, "identity");
                    }
                } catch (IOException ignored) {
                }
            }
            return new Entry(width, height, arm, geometry, time, identity, argb);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** True when a cache is present. */
    public static boolean exists(File filesRoot) {
        return filesRoot != null && new File(dir(filesRoot), BIN_NAME).isFile();
    }

    /** Deletes the cache (a manual import override clears it). */
    public static void clear(File filesRoot) {
        if (filesRoot == null) return;
        new File(dir(filesRoot), BIN_NAME).delete();
        new File(dir(filesRoot), JSON_NAME).delete();
    }

    // --- tiny little-endian helpers ---------------------------------------------------------

    private static void putInt(byte[] b, int off, int v) {
        b[off] = (byte) (v & 0xFF);
        b[off + 1] = (byte) ((v >> 8) & 0xFF);
        b[off + 2] = (byte) ((v >> 16) & 0xFF);
        b[off + 3] = (byte) ((v >> 24) & 0xFF);
    }

    private static int getInt(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8)
                | ((b[off + 2] & 0xFF) << 16) | ((b[off + 3] & 0xFF) << 24);
    }

    private static void putLong(byte[] b, int off, long v) {
        for (int i = 0; i < 8; i++) b[off + i] = (byte) ((v >> (8 * i)) & 0xFF);
    }

    private static long getLong(byte[] b, int off) {
        long v = 0;
        for (int i = 0; i < 8; i++) v |= ((long) (b[off + i] & 0xFF)) << (8 * i);
        return v;
    }

    /** Extracts a simple string field from the tiny metadata JSON. */
    static String jsonString(String json, String key) {
        if (json == null) return "";
        String needle = "\"" + key + "\":\"";
        int i = json.indexOf(needle);
        if (i < 0) return "";
        int start = i + needle.length();
        int end = json.indexOf('"', start);
        return end < 0 ? "" : json.substring(start, end);
    }

    /** A stable, non-reversible identity hash for the cache key (never an upload of the id). */
    public static String identityHash(String identity) {
        if (identity == null || identity.isEmpty()) return "";
        byte[] bytes = identity.getBytes(StandardCharsets.UTF_8);
        long h = 1469598103934665603L;
        for (byte b : bytes) {
            h ^= (b & 0xFF);
            h *= 1099511628211L;
        }
        return Long.toHexString(h);
    }

    /** True when two entries are the same atlas (used to skip a redundant write). */
    static boolean sameAtlas(int[] a, int[] b) {
        return Arrays.equals(a, b);
    }
}
