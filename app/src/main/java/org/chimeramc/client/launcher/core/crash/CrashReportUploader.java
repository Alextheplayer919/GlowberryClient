package org.chimeramc.client.core.crash;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.chimeramc.client.BuildConfig;
import org.chimeramc.client.settings.FeatureSettings;
import org.chimeramc.client.util.LauncherStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Sends captured crash reports to the configured collector.
 *
 * <p>Before this class the Settings switch "Crash report upload" persisted a flag that nothing
 * read: turning it off changed a stored value and left the switch reading "on" forever, and turning
 * it on promised an upload that never happened. That is the same class of defect as the Icon Size
 * slider — a control whose only effect is on itself.
 *
 * <p>The endpoint is a build-time property ({@code crash.upload.endpoint} in {@code local.properties}
 * → {@link BuildConfig#CRASH_UPLOAD_ENDPOINT}), the same shape the voice relay address uses. It is
 * empty in the shipped source, and the source holds no collector address, so:
 *
 * <ul>
 *   <li>{@link #isConfigured()} is false and {@link #isAvailable(Context)} is false, which the
 *       Settings screen turns into a disabled switch with a reason rather than a switch that lies;
 *   <li>when an operator does set the property, the upload runs for real.
 * </ul>
 *
 * <p>Nothing is ever sent unless the user's own switch is on <em>and</em> the endpoint is
 * configured. A crash report contains a stack trace and the app version; it is not sent implicitly,
 * which is why the consent check is the first statement of {@link #uploadPending} rather than a
 * condition somewhere inside the request.
 */
public final class CrashReportUploader {

    private static final String TAG = "CrashUploader";
    private static final String PREFS = "crash_upload";
    private static final String KEY_SENT = "sent_ids";
    private static final int MAX_REPORTS_PER_RUN = 3;

    private static final MediaType TEXT = MediaType.parse("text/plain; charset=utf-8");

    private CrashReportUploader() {
    }

    /** True when this build has a collector configured. */
    public static boolean isConfigured() {
        String endpoint = BuildConfig.CRASH_UPLOAD_ENDPOINT;
        return endpoint != null && !endpoint.trim().isEmpty();
    }

    /**
     * True when an upload could actually happen now: the user enabled it and a collector exists.
     *
     * <p>The Settings screen reads this, so the switch is only interactive when it can do
     * something.
     */
    public static boolean isAvailable(Context context) {
        return isConfigured() && FeatureSettings.getInstance().isCrashUploadEnabled();
    }

    /**
     * Uploads every report that has not been sent yet.
     *
     * <p>Call from a background thread. Never throws: a failure to report a crash must not become a
     * crash, so every error is logged and the run ends.
     */
    public static void uploadPending(Context context) {
        if (context == null) return;
        if (!isAvailable(context)) return;
        try {
            upload(context.getApplicationContext());
        } catch (Throwable error) {
            Log.w(TAG, "Crash report upload aborted: " + describe(error));
        }
    }

    private static void upload(Context context) {
        File dir = LauncherStorage.getCrashLogsDir(context);
        List<File> candidates = new ArrayList<>();
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) candidates.add(child);
        }

        Set<String> alreadySent = readSent(context);
        Set<File> pending = CrashUploadPolicy.pending(candidates, alreadySent);
        if (pending.isEmpty()) {
            Log.i(TAG, "No new crash report to upload");
            return;
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Set<String> sentNow = new LinkedHashSet<>(alreadySent);
        int attempts = 0;
        for (File report : pending) {
            if (attempts++ >= MAX_REPORTS_PER_RUN) break;
            if (send(client, context, report)) {
                sentNow.add(CrashUploadPolicy.identity(report));
            }
        }
        writeSent(context, sentNow);
    }

    /** Sends one report. Returns true only on a 2xx response, so a failure is retried next run. */
    private static boolean send(OkHttpClient client, Context context, File report) {
        try {
            CrashReportStore.Report read = CrashReportStore.read(report);
            if (!read.isPresent()) return false;

            Request request = new Request.Builder()
                    .url(BuildConfig.CRASH_UPLOAD_ENDPOINT.trim())
                    .header("X-Crash-App-Version", BuildConfig.VERSION_NAME)
                    .header("X-Crash-Report-Name", report.getName())
                    .header("X-Crash-Report-Truncated", String.valueOf(read.truncated))
                    .post(RequestBody.create(read.text, TEXT))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "Uploaded crash report " + report.getName());
                    return true;
                }
                Log.w(TAG, "Crash report upload refused: HTTP " + response.code());
                return false;
            }
        } catch (Throwable error) {
            Log.w(TAG, "Crash report upload failed: " + describe(error));
            return false;
        }
    }

    /**
     * The identities of reports already uploaded.
     *
     * <p>Bounded: the newest {@code 64} identities are kept, which is far more than the number of
     * reports that accumulate between launches, so the set cannot grow without limit.
     */
    private static Set<String> readSent(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> raw = prefs.getStringSet(KEY_SENT, null);
        return raw == null ? new LinkedHashSet<>() : new LinkedHashSet<>(raw);
    }

    private static void writeSent(Context context, Set<String> identities) {
        List<String> ordered = new ArrayList<>(identities);
        while (ordered.size() > 64) ordered.remove(0);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_SENT, new LinkedHashSet<>(ordered))
                .apply();
    }

    /** Forgets what was uploaded, so the next run sends the current reports again. */
    public static void resetSentHistory(Context context) {
        if (context == null) return;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_SENT).apply();
    }

    private static String describe(Throwable error) {
        if (error == null) return "unknown error";
        String message = error.getMessage();
        return message == null || message.trim().isEmpty()
                ? error.getClass().getSimpleName()
                : error.getClass().getSimpleName() + ": " + message;
    }
}
