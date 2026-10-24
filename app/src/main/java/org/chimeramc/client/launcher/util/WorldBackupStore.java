package org.chimeramc.client.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Writes scheduled world backups into a user-picked SAF document tree (G13).
 *
 * <p>Using a persisted {@link android.content.Intent#ACTION_OPEN_DOCUMENT_TREE} URI means backups
 * live in a location Android cannot revoke behind the app (the internal backups dir is wiped on
 * uninstall / data clear). The tree root is treated as the backup folder directly — the user picks
 * whichever folder they want, and the world files land at its top level.
 *
 * <p>All interaction goes through {@link DocumentsContract} rather than the {@code DocumentFile}
 * compat library to avoid a dependency. Operations fail closed: a create/delete that throws
 * records an error and the run still reports what it managed to do.
 */
public final class WorldBackupStore {

    private static final String TAG = "WorldBackupStore";
    public static final String MIME_MCWORLD = "application/octet-stream";
    private static final int BUFFER_SIZE = 131072;

    public static final class RunResult {
        public int backedUp;
        public int deleted;
        public final List<String> createdNames = new ArrayList<>();
        public final List<String> deletedNames = new ArrayList<>();
        public final List<String> errors = new ArrayList<>();

        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }

    private WorldBackupStore() {}

    /** Lists the {@code .mcworld} names present directly inside the tree root. */
    public static List<String> listBackupNames(Context context, Uri treeUri) {
        List<String> names = new ArrayList<>();
        if (treeUri == null) return names;

        String treeId = DocumentsContract.getTreeDocumentId(treeUri);
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeId);
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(childrenUri,
                    new String[]{
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                            DocumentsContract.Document.COLUMN_MIME_TYPE
                    },
                    null, null, null);
            if (cursor == null) return names;
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME));
                String mime = cursor.getString(cursor.getColumnIndexOrThrow(
                        DocumentsContract.Document.COLUMN_MIME_TYPE));
                if (name != null && name.endsWith(WorldBackupNaming.EXTENSION)
                        && !DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                    names.add(name);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "listBackupNames failed", e);
        } finally {
            closeQuietly(cursor);
        }
        return names;
    }

    /**
     * Runs one backup of {@code worlds} into the tree root, then deletes the named stale files.
     *
     * @param timestampMs the run's wall-clock timestamp; burned into each file name
     */
    public static RunResult run(Context context, Uri treeUri, List<WorldBackupPlan.WorldEntry> worlds,
                                List<String> deleteNames, long timestampMs) {
        RunResult result = new RunResult();
        if (treeUri == null || worlds == null) return result;
        String treeId = DocumentsContract.getTreeDocumentId(treeUri);
        Uri treeRoot = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeId);
        ContentResolver resolver = context.getContentResolver();

        for (WorldBackupPlan.WorldEntry world : worlds) {
            if (world.directory == null || !world.directory.isDirectory()) continue;
            String displayName = WorldBackupNaming.forWorld(world.name, timestampMs);
            try {
                Uri created = DocumentsContract.createDocument(
                        resolver, treeRoot, MIME_MCWORLD, displayName);
                if (created == null) {
                    result.errors.add(displayName);
                    continue;
                }
                try (OutputStream out = resolver.openOutputStream(created)) {
                    if (out == null) throw new IOException("openOutputStream returned null");
                    writeWorldZip(world.directory, out);
                }
                result.createdNames.add(displayName);
                result.backedUp++;
            } catch (Exception e) {
                Log.w(TAG, "backup failed for " + world.name, e);
                result.errors.add(displayName);
            }
        }

        if (deleteNames != null) {
            for (String name : deleteNames) {
                try {
                    deleteByName(resolver, treeUri, name);
                    result.deletedNames.add(name);
                    result.deleted++;
                } catch (Exception e) {
                    Log.w(TAG, "delete failed for " + name, e);
                    result.errors.add(name);
                }
            }
        }
        return result;
    }

    /**
     * Reads the human-readable name of the tree root document, falling back to the last path
     * segment of the tree id when the provider cannot answer (permission dropped, provider quirk).
     * Never returns null — the status line should always be able to name the chosen folder.
     */
    public static String displayNameFromTreeUri(Context context, Uri treeUri) {
        if (treeUri == null) return "";
        String treeId = DocumentsContract.getTreeDocumentId(treeUri);
        if (context == null) return treeId;
        try {
            Uri rootUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeId);
            Cursor cursor = context.getContentResolver().query(rootUri,
                    new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},
                    null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        String name = cursor.getString(0);
                        if (name != null && !name.isEmpty()) return name;
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "displayNameFromTreeUri failed", e);
        }
        return treeId;
    }

    private static void deleteByName(ContentResolver resolver, Uri treeUri, String name)
            throws Exception {
        String treeId = DocumentsContract.getTreeDocumentId(treeUri);
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeId);
        Cursor cursor = null;
        String documentId = null;
        try {
            cursor = resolver.query(childrenUri,
                    new String[]{
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME
                    },
                    null, null, null);
            if (cursor == null) return;
            while (cursor.moveToNext()) {
                String display = cursor.getString(cursor.getColumnIndexOrThrow(
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME));
                if (name.equals(display)) {
                    documentId = cursor.getString(cursor.getColumnIndexOrThrow(
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID));
                    break;
                }
            }
        } finally {
            closeQuietly(cursor);
        }
        if (documentId != null) {
            Uri docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId);
            DocumentsContract.deleteDocument(resolver, docUri);
        }
    }

    /** Mirrors {@link org.chimeramc.client.launcher.core.content.WorldManager#zipDirectory}. */
    private static void writeWorldZip(File worldDir, OutputStream out) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            zipDirectory(worldDir, "", zos);
        }
    }

    private static void zipDirectory(File dir, String basePath, ZipOutputStream zos) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;
        Arrays.sort(files, (a, b) -> a.getName().compareTo(b.getName()));
        for (File file : files) {
            String entryPath = basePath.isEmpty() ? file.getName() : basePath + "/" + file.getName();
            if (file.isDirectory()) {
                zipDirectory(file, entryPath, zos);
            } else {
                ZipEntry entry = new ZipEntry(entryPath);
                zos.putNextEntry(entry);
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            }
        }
    }

    /** Resolves a {@link WorldItem}'s directory for the plan. Called by the service, not here. */

    private static void closeQuietly(Cursor cursor) {
        if (cursor != null) {
            try {
                cursor.close();
            } catch (Exception ignored) {
            }
        }
    }
}