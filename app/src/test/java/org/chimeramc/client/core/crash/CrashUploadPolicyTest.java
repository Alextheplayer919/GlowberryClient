package org.chimeramc.client.core.crash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Pins {@link CrashUploadPolicy}: which reports are sent, in what order, and which are never sent
 * twice.
 *
 * <p>A wrong rule here is not cosmetic. A de-duplication mistake re-uploads the same tombstone on
 * every launch, which burns the user's mobile data and buries the real report under duplicates; a
 * missing size cap sends a multi-megabyte tombstone over a connection the user did not agree to use
 * for it. Both are asserted below without a device.
 */
public class CrashUploadPolicyTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private File write(String name, String content, long lastModified) throws IOException {
        File file = new File(temp.getRoot(), name);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        }
        file.setLastModified(lastModified);
        return file;
    }

    @Test
    public void aReportThatWasAlreadySentIsNotSentAgain() throws IOException {
        File report = write("crash.log", "boom", 1_000L);
        Set<File> pending = CrashUploadPolicy.pending(
                Collections.singletonList(report),
                Collections.singletonList(CrashUploadPolicy.identity(report)));
        assertTrue("an already-sent report must not be pending", pending.isEmpty());
    }

    @Test
    public void aNewReportIsPending() throws IOException {
        File report = write("crash.log", "boom", 1_000L);
        Set<File> pending = CrashUploadPolicy.pending(
                Collections.singletonList(report), Collections.emptyList());
        assertEquals(1, pending.size());
        assertTrue(pending.contains(report));
    }

    /**
     * The identity must include the length, or a second crash that reuses the file name would be
     * treated as already sent and never reported.
     */
    @Test
    public void theSameNameWithADifferentLengthIsANewReport() throws IOException {
        File first = write("crash.log", "boom", 1_000L);
        String firstIdentity = CrashUploadPolicy.identity(first);

        File second = write("crash.log", "boom, and a much longer second crash trace", 2_000L);
        assertFalse("a reused name with new content must read as a new report",
                firstIdentity.equals(CrashUploadPolicy.identity(second)));

        Set<File> pending = CrashUploadPolicy.pending(
                Collections.singletonList(second), Collections.singletonList(firstIdentity));
        assertEquals("the new report must still be pending", 1, pending.size());
    }

    @Test
    public void theNewestReportComesFirst() throws IOException {
        File older = write("older.log", "older", 1_000L);
        File newer = write("newer.log", "newer", 9_000L);
        Set<File> pending = CrashUploadPolicy.pending(
                Arrays.asList(older, newer), Collections.emptyList());
        List<File> ordered = new ArrayList<>(pending);
        assertEquals("the most recent crash is the one the user is asking about",
                newer, ordered.get(0));
    }

    @Test
    public void anEmptyFileIsNotUploadable() throws IOException {
        File empty = write("empty.log", "", 1_000L);
        assertFalse(CrashUploadPolicy.isUploadable(empty));
        assertTrue(CrashUploadPolicy.pending(
                Collections.singletonList(empty), Collections.emptyList()).isEmpty());
    }

    @Test
    public void aMissingFileIsNotUploadable() {
        assertFalse(CrashUploadPolicy.isUploadable(new File(temp.getRoot(), "absent.log")));
        assertFalse(CrashUploadPolicy.isUploadable(null));
    }

    /**
     * A tombstone carrying a heap dump is skipped rather than sent: the report the developer needs
     * is the stack trace, and the bytes are the user's to spend.
     */
    @Test
    public void anOversizedReportIsNotUploadable() throws IOException {
        File huge = new File(temp.getRoot(), "huge.log");
        try (FileOutputStream out = new FileOutputStream(huge)) {
            byte[] block = new byte[64 * 1024];
            long written = 0;
            while (written <= CrashUploadPolicy.MAX_UPLOAD_BYTES) {
                out.write(block);
                written += block.length;
            }
        }
        assertFalse("a report past the cap must be skipped", CrashUploadPolicy.isUploadable(huge));
        assertTrue(CrashUploadPolicy.pending(
                Collections.singletonList(huge), Collections.emptyList()).isEmpty());
    }

    @Test
    public void aReportExactlyAtTheCapIsUploadable() throws IOException {
        File atCap = new File(temp.getRoot(), "at-cap.log");
        try (FileOutputStream out = new FileOutputStream(atCap)) {
            byte[] block = new byte[64 * 1024];
            long written = 0;
            while (written < CrashUploadPolicy.MAX_UPLOAD_BYTES) {
                int remaining = (int) Math.min(block.length,
                        CrashUploadPolicy.MAX_UPLOAD_BYTES - written);
                out.write(block, 0, remaining);
                written += remaining;
            }
        }
        assertEquals(CrashUploadPolicy.MAX_UPLOAD_BYTES, atCap.length());
        assertTrue(CrashUploadPolicy.isUploadable(atCap));
    }

    @Test
    public void aNullCandidateListYieldsNothingRatherThanThrowing() {
        assertTrue(CrashUploadPolicy.pending(null, null).isEmpty());
        assertTrue(CrashUploadPolicy.pending(Arrays.asList(null, null), null).isEmpty());
    }
}
