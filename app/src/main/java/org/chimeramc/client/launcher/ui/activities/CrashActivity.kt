package org.chimeramc.client.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import org.chimeramc.client.R
import org.chimeramc.client.core.crash.CrashReporter
import org.chimeramc.client.launcher.core.minecraft.LaunchErrorDiagnosis
import org.chimeramc.client.ui.animation.DynamicAnim
import java.io.File
import java.util.concurrent.Executors

class CrashActivity : BaseActivity() {

    private var executor = Executors.newSingleThreadExecutor()
    private var logPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_crash)

        logPath = intent.getStringExtra("LOG_PATH")
        val summary = intent.getStringExtra("SUMMARY")
            ?: intent.getStringExtra("EMERGENCY")
        val crashType = intent.getStringExtra("CRASH_TYPE")
        CrashReporter.sendUnsentReports(this)

        val tvTitle = findViewById<TextView>(R.id.crash_title)
        val tvDetails = findViewById<TextView>(R.id.crash_details)
        val btnBack = findViewById<Button>(R.id.btn_back_to_main)
        val btnShare = findViewById<Button>(R.id.btn_share_log)
        DynamicAnim.applyPressScale(btnBack)
        DynamicAnim.applyPressScale(btnShare)

        tvTitle.text = getString(R.string.crash_title)

        // Name the cause from the crash's own text, so a native dlopen tombstone reads as
        // "Game library not readable / Google Play version" rather than only a raw stack. The raw
        // text stays underneath for the report.
        val cause = LaunchErrorDiagnosis.classifyText(summary)
        val causeLine = getString(R.string.launch_error_cause_label) + " " +
                LaunchErrorDiagnosis.label(cause) + "\n" +
                getString(LaunchErrorDiagnosis.messageRes(cause)) + "\n\n"

        tvDetails.text = buildString {
            append(causeLine)
            appendHeader(crashType, summary)
            if (!logPath.isNullOrBlank()) {
                append(getString(R.string.crash_log_file_label)).append("\n")
                append(logPath).append("\n\n")
            }
            append(getString(R.string.crash_loading_details))
        }

        btnBack.setOnClickListener {
            navigateToMain()
        }
        
        btnShare.setOnClickListener {
            shareLogFile()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            loadCrashDetails(tvDetails, crashType, summary)
        }, 100)
        Handler(Looper.getMainLooper()).postDelayed({
            CrashReporter.sendUnsentReports(this)
        }, 2000)
    }

    override fun shouldSkipNavBar(): Boolean = true

    private fun loadCrashDetails(tvDetails: TextView, crashType: String?, summary: String?) {
        if (isFinishing || isDestroyed) return

        executor.execute {
            val cause = LaunchErrorDiagnosis.classifyText(summary)
            val causeLine = getString(R.string.launch_error_cause_label) + " " +
                    LaunchErrorDiagnosis.label(cause) + "\n" +
                    getString(LaunchErrorDiagnosis.messageRes(cause)) + "\n\n"
            val detailsText = buildString {
                append(causeLine)
                appendHeader(crashType, summary)

                if (!logPath.isNullOrEmpty()) {
                    append(getString(R.string.crash_log_file_label)).append("\n")
                    append(logPath).append("\n\n")
                    append(getString(R.string.crash_stack_label)).append("\n")
                    try {
                        val f = File(logPath!!)
                        if (f.exists() && f.isFile) {
                            val content = f.readText()
                            val maxLength = 50000
                            if (content.length > maxLength) {
                                append(content.substring(0, maxLength))
                                append("\n\n... [truncated, full log in file] ...")
                            } else {
                                append(content)
                            }
                        } else {
                            append(getString(R.string.crash_log_file_missing)).append("\n")
                        }
                    } catch (e: Exception) {
                        // Name the cause, not the raw exception text: a report that cannot be read
                        // is usually a permission or a deleted file, and "Permission denied" alone
                        // does not tell the user which of the two it is.
                        append(getString(R.string.crash_read_failed, describeReadFailure(e))).append("\n")
                    }
                }
                if (isEmpty()) {
                    append(getString(R.string.crash_no_details))
                }
            }

            if (!isFinishing && !isDestroyed) {
                runOnUiThread {
                    tvDetails.text = detailsText
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun StringBuilder.appendHeader(crashType: String?, summary: String?) {
        if (!crashType.isNullOrBlank()) {
            append(getString(R.string.crash_type_label)).append("\n")
            append(crashType).append("\n\n")
        }
        if (!summary.isNullOrBlank()) {
            append(getString(R.string.crash_summary_label)).append("\n")
            append(summary).append("\n\n")
        }
    }

    private fun navigateToMain() {
        try {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        } catch (ignored: Exception) {
            finish()
        }
    }

    private fun shareLogFile() {
        val path = logPath
        if (path.isNullOrBlank()) {
            // The button used to do nothing at all when the intent carried no log path, which
            // reads as a broken button rather than as a missing report.
            Toast.makeText(this, R.string.crash_share_no_log, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val logFile = File(path)
            if (!logFile.exists() || !logFile.isFile) {
                Toast.makeText(this, R.string.crash_log_file_missing, Toast.LENGTH_SHORT).show()
                return
            }
            val shareFile = copyLogToShareCache(logFile)
            val logFileUri = FileProvider.getUriForFile(
                this,
                packageName + ".fileprovider",
                shareFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, logFileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, shareFile.name))
        } catch (e: Exception) {
            // Swallowing this left a share that silently did nothing - the user tapped, nothing
            // appeared, and there was no way to tell whether the report existed.
            Toast.makeText(
                this,
                getString(R.string.crash_share_failed, describeReadFailure(e)),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /** A short, cause-specific sentence for a report that could not be read or shared. */
    private fun describeReadFailure(error: Throwable): String = when (error) {
        is java.io.FileNotFoundException -> getString(R.string.crash_reason_file_missing)
        is SecurityException -> getString(R.string.crash_reason_no_permission)
        is java.io.IOException -> getString(R.string.crash_reason_io)
        else -> error.javaClass.simpleName
    }

    private fun copyLogToShareCache(logFile: File): File {
        val shareDir = File(cacheDir, "crash_logs_share")
        if (!shareDir.exists()) shareDir.mkdirs()
        val shareFile = File(shareDir, logFile.name)
        logFile.copyTo(shareFile, overwrite = true)
        return shareFile
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            executor.shutdownNow()
        } catch (ignored: Exception) {}
    }
}
