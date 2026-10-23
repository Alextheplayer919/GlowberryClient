package org.chimeramc.client.launcher.ui.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.chimeramc.client.BuildConfig;
import org.chimeramc.client.R;
import org.chimeramc.client.core.minecraft.GamePackageManager;
import org.chimeramc.client.core.versions.GameVersion;
import org.chimeramc.client.core.versions.VersionManager;
import org.chimeramc.client.core.mods.ModLoadDiagnostics;
import org.chimeramc.client.preloader.PreloaderInput;
import org.chimeramc.client.preloader.PreloaderSignatureRulesManager;
import org.chimeramc.client.ui.activities.BaseActivity;
import org.chimeramc.client.ui.animation.DynamicAnim;
import org.chimeramc.client.ui.dialogs.CustomAlertDialog;
import org.chimeramc.client.util.PersonalizationManager;

import java.util.Locale;

/**
 * In-app diagnostics (U16) plus the "Repair installation" action (G2).
 *
 * <p>The launch pipeline is hard to debug from logcat (no adb on a consumer device), so this screen
 * reads the same live state the pipeline uses — app/preloader versions, extraction version, signature
 * rule source+revision, native render hook, and the persisted mod-load failure store — and renders it
 * as shareable plain text. Every unknown reading renders as "unknown", never as a made-up number.
 */
public class DiagnosticsActivity extends BaseActivity {

    private VersionManager versionManager;
    private PersonalizationManager personalizationManager;
    private TextView repairStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnostics);
        personalizationManager = new PersonalizationManager(this);
        versionManager = VersionManager.get(this);

        setActiveNavTab(R.id.nav_tab_settings);
        DynamicAnim.applyPressScaleRecursively(findViewById(android.R.id.content));

        setupRepairButton();
        setupActionButtons();

        refresh();
    }

    private void setupRepairButton() {
        Button repair = findViewById(R.id.diagnostics_repair_button);
        if (repair == null) return;
        repair.setOnClickListener(v -> confirmRepair());
        DynamicAnim.applyPressScale(repair);
    }

    private void setupActionButtons() {
        Button copy = findViewById(R.id.diagnostics_copy_button);
        if (copy != null) {
            copy.setOnClickListener(v -> copyReport());
            DynamicAnim.applyPressScale(copy);
        }
        Button share = findViewById(R.id.diagnostics_share_button);
        if (share != null) {
            share.setOnClickListener(v -> shareReport());
            DynamicAnim.applyPressScale(share);
        }
        Button refresh = findViewById(R.id.diagnostics_refresh_button);
        if (refresh != null) {
            refresh.setOnClickListener(v -> refresh());
            DynamicAnim.applyPressScale(refresh);
        }
    }

    private void refresh() {
        renderAppRows();
        renderLibRows();
        renderRulesRows();
        renderNativeRows();
        renderModRows();
    }

    private GameVersion selectedVersion() {
        try {
            return versionManager != null ? versionManager.getSelectedVersion() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------------------------------------------------------- sections

    private void renderAppRows() {
        LinearLayout rows = findViewById(R.id.diagnostics_app_rows);
        if (rows == null) return;
        rows.removeAllViews();

        addRow(rows, R.string.diagnostics_field_app_version, BuildConfig.VERSION_NAME
                + " (" + BuildConfig.VERSION_CODE + ")");
        addRow(rows, R.string.diagnostics_field_android_version, Build.VERSION.RELEASE
                + " (API " + Build.VERSION.SDK_INT + ")");
        addRow(rows, R.string.diagnostics_field_device, Build.MANUFACTURER + " " + Build.MODEL);
        addRow(rows, R.string.diagnostics_field_sdk_build, String.valueOf(Build.VERSION_CODES.TIRAMISU <= Build.VERSION.SDK_INT ? "35" : "28"));
        addRow(rows, R.string.diagnostics_field_abi, String.join(", ", Build.SUPPORTED_ABIS));

        GameVersion version = selectedVersion();
        if (version != null) {
            addRow(rows, R.string.diagnostics_field_selected_version, version.directoryName
                    + " (" + (version.versionCode != null ? version.versionCode : "?") + ")");
        }
    }

    private void renderLibRows() {
        LinearLayout rows = findViewById(R.id.diagnostics_libs_rows);
        if (rows == null) return;
        rows.removeAllViews();

        GameVersion version = selectedVersion();
        if (version == null) {
            addRow(rows, R.string.diagnostics_field_extraction_version, getString(R.string.diagnostics_no_version));
            return;
        }
        boolean current = GamePackageManager.isExtractionCacheCurrent(this, version);
        int expected = GamePackageManager.calculateNativeExtractionVersion();
        addRow(rows, R.string.diagnostics_field_extraction_version,
                current
                        ? getString(R.string.diagnostics_extraction_ok) + " (v" + expected + ")"
                        : getString(R.string.diagnostics_extraction_stale) + " (v" + expected + ")");
        addRow(rows, "Extractor", String.valueOf(GamePackageManager.extractorVersion()));
    }

    private void renderRulesRows() {
        LinearLayout rows = findViewById(R.id.diagnostics_rules_rows);
        if (rows == null) return;
        rows.removeAllViews();

        String source = PreloaderSignatureRulesManager.getRulesSource(this);
        String sourceLabel;
        if (source == null) {
            sourceLabel = getString(R.string.diagnostics_rules_unknown);
        } else if ("remote".equals(source)) {
            sourceLabel = getString(R.string.diagnostics_rules_remote);
        } else {
            sourceLabel = getString(R.string.diagnostics_rules_bundled);
        }
        addRow(rows, R.string.diagnostics_field_signature_rules_source, sourceLabel);

        int revision = PreloaderSignatureRulesManager.getRulesRevision(this);
        addRow(rows, R.string.diagnostics_field_signature_rules_revision,
                revision < 0 ? "?" : String.valueOf(revision));
    }

    private void renderNativeRows() {
        LinearLayout rows = findViewById(R.id.diagnostics_native_rows);
        if (rows == null) return;
        rows.removeAllViews();

        boolean nativeLoaded = isNativeLoaded();
        addRow(rows, R.string.diagnostics_field_cosmetics_hook,
                nativeLoaded && PreloaderInput.nativeIsPlayerRenderHookLive()
                        ? getString(R.string.diagnostics_hook_live)
                        : getString(R.string.diagnostics_hook_inactive));

        boolean hasSkinSeam = false;
        try {
            hasSkinSeam = nativeLoaded && PreloaderInput.nativeHasCapturedSkin();
        } catch (Throwable ignored) {}
        addRow(rows, R.string.diagnostics_field_skin_seam,
                hasSkinSeam ? getString(R.string.diagnostics_hook_live)
                            : getString(R.string.diagnostics_hook_inactive));
    }

    private void renderModRows() {
        LinearLayout rows = findViewById(R.id.diagnostics_mods_rows);
        if (rows == null) return;
        rows.removeAllViews();

        java.util.List<ModLoadDiagnostics.Record> records = ModLoadDiagnostics.getAll(this);
        if (records.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.diagnostics_no_mod_errors);
            empty.setTextColor(getColor(R.color.text_secondary));
            empty.setTextSize(12);
            empty.setPadding(0, 8, 0, 0);
            rows.addView(empty);
            return;
        }
        for (ModLoadDiagnostics.Record record : records) {
            addRow(rows, record.modName != null ? record.modName : (record.modId != null ? record.modId : "?"),
                    record.reason != null ? record.reason : "?");
        }
    }

    private boolean isNativeLoaded() {
        try {
            return PreloaderInput.readRenderDiagnostics() != null;
        } catch (Throwable t) {
            return false;
        }
    }

    // ---------------------------------------------------------------- rows

    private void addRow(LinearLayout parent, int labelRes, String value) {
        addRow(parent, getString(labelRes), value);
    }

    private void addRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(getColor(R.color.text_secondary));
        labelView.setTextSize(12);
        labelView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView valueView = new TextView(this);
        valueView.setText(value != null ? value : "?");
        valueView.setTextColor(getColor(R.color.on_surface));
        valueView.setTextSize(12);
        valueView.setGravity(android.view.Gravity.END);
        valueView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        row.addView(labelView);
        row.addView(valueView);
        parent.addView(row);
    }

    // ---------------------------------------------------------------- actions

    private void confirmRepair() {
        GameVersion version = selectedVersion();
        if (version == null) {
            Toast.makeText(this, R.string.diagnostics_no_version, Toast.LENGTH_SHORT).show();
            return;
        }
        new CustomAlertDialog(this)
                .setTitleText(getString(R.string.diagnostics_repair_confirm_title))
                .setMessage(getString(R.string.diagnostics_repair_confirm_message))
                .setPositiveButton(getString(R.string.repair), v -> runRepair(version))
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void runRepair(GameVersion version) {
        Toast.makeText(this, R.string.diagnostics_repair_started, Toast.LENGTH_SHORT).show();
        // Invalidate the extraction marker + manifest and let the normal launch re-extract.
        GamePackageManager.invalidateExtractionCache(this, version);
        VersionManager.attemptRepairLibs(this, version);
    }

    private void copyReport() {
        String report = buildReport();
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("GlowberryClient diagnostics", report));
            Toast.makeText(this, R.string.diagnostics_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void shareReport() {
        String report = buildReport();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, report);
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.diagnostics_share_title));
        startActivity(Intent.createChooser(intent, getString(R.string.diagnostics_share_title)));
    }

    private String buildReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("GlowberryClient Diagnostics\n");
        sb.append("===========================\n\n");
        sb.append("App version: ").append(BuildConfig.VERSION_NAME).append(" (").append(BuildConfig.VERSION_CODE).append(")\n");
        sb.append("Android: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
        sb.append("ABIs: ").append(String.join(", ", Build.SUPPORTED_ABIS)).append("\n");

        GameVersion version = selectedVersion();
        if (version != null) {
            sb.append("Selected version: ").append(version.directoryName)
                    .append(" (").append(version.versionCode).append(")\n");
        }

        boolean extractionCurrent = version != null && GamePackageManager.isExtractionCacheCurrent(this, version);
        sb.append("Extraction version: ")
                .append(extractionCurrent ? "up to date" : "stale")
                .append(" (expected ").append(GamePackageManager.calculateNativeExtractionVersion()).append(")\n");

        String source = PreloaderSignatureRulesManager.getRulesSource(this);
        sb.append("Signature rules source: ").append(source != null ? source : "unknown").append("\n");
        sb.append("Signature rules revision: ").append(PreloaderSignatureRulesManager.getRulesRevision(this)).append("\n");

        boolean nativeLoaded = isNativeLoaded();
        sb.append("Native library loaded: ").append(nativeLoaded ? "yes" : "no").append("\n");
        if (nativeLoaded) {
            sb.append("Cosmetics render hook: ")
                    .append(PreloaderInput.nativeIsPlayerRenderHookLive() ? "live" : "inactive").append("\n");
        }

        sb.append("\nMod load errors:\n");
        java.util.List<ModLoadDiagnostics.Record> records = ModLoadDiagnostics.getAll(this);
        if (records.isEmpty()) {
            sb.append("  (none)\n");
        } else {
            for (ModLoadDiagnostics.Record r : records) {
                String name = r.modName != null ? r.modName : (r.modId != null ? r.modId : "?");
                sb.append("  - ").append(name).append(": ").append(r.reason != null ? r.reason : "?").append("\n");
            }
        }

        sb.append("\n");
        sb.append(Locale.getDefault().toString()).append("\n");
        return sb.toString();
    }
}