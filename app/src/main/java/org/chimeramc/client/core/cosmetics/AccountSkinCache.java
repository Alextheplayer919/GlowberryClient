package org.chimeramc.client.core.cosmetics;

import java.io.File;

/**
 * The on-disk cache of the account-looked-up skin, as a plain PNG under {@code files/cosmetics/}.
 *
 * <p>Kept separate from {@link LocalSkinCache} on purpose: that file is written by the game's own
 * skin capture ({@code GameSkinFeed}), and two writers on one file would clobber each other — a
 * capture would erase the account skin, or the reverse. A PNG is also the natural format here
 * because the texture is already a PNG download and it can be inspected by eye on a device.
 *
 * <p>The cache is keyed by the account identifier in a sidecar name, so switching accounts does not
 * leave the previous account's skin on screen.
 */
public final class AccountSkinCache {

    /** The directory, under the app's files dir. */
    public static final String DIR = "cosmetics";
    /** The cached atlas file name. */
    public static final String PNG_NAME = "account_skin.png";

    private AccountSkinCache() {
    }

    /** A cache entry: the atlas source bytes plus the account key it belongs to. */
    public static final class Entry {
        public final byte[] png;
        public final String accountKey;

        public Entry(byte[] png, String accountKey) {
            this.png = png;
            this.accountKey = accountKey == null ? "" : accountKey;
        }
    }

    public static File file(File filesRoot) {
        if (filesRoot == null) return null;
        File dir = new File(filesRoot, DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        return new File(dir, PNG_NAME);
    }

    /** Writes the PNG and its account key. Returns true on success. */
    public static boolean write(File filesRoot, Entry entry) {
        File target = file(filesRoot);
        if (target == null || entry == null || entry.png == null || entry.png.length == 0) {
            return false;
        }
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(target)) {
            out.write(entry.png);
        } catch (java.io.IOException e) {
            return false;
        }
        // The account key lives in a sibling name so the PNG stays a plain, inspectable file.
        try (java.io.FileOutputStream out =
                     new java.io.FileOutputStream(new File(target.getParentFile(), PNG_NAME + ".key"))) {
            out.write(entry.accountKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException ignored) {
        }
        return true;
    }

    /** Reads the cached entry, or null when there is none. */
    public static Entry read(File filesRoot) {
        File target = file(filesRoot);
        if (target == null || !target.exists()) return null;
        try {
            byte[] png = java.nio.file.Files.readAllBytes(target.toPath());
            if (png.length == 0) return null;
            String key = "";
            File keyFile = new File(target.getParentFile(), PNG_NAME + ".key");
            if (keyFile.exists()) {
                key = new String(java.nio.file.Files.readAllBytes(keyFile.toPath()),
                        java.nio.charset.StandardCharsets.UTF_8).trim();
            }
            return new Entry(png, key);
        } catch (java.io.IOException e) {
            return null;
        }
    }

    /** Deletes the cached skin. Returns true when nothing remains. */
    public static boolean clear(File filesRoot) {
        File target = file(filesRoot);
        if (target == null) return true;
        boolean ok = !target.exists() || target.delete();
        File keyFile = new File(target.getParentFile(), PNG_NAME + ".key");
        if (keyFile.exists()) ok &= keyFile.delete();
        return ok;
    }
}
