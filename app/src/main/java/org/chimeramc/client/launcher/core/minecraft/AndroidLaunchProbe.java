package org.chimeramc.client.launcher.core.minecraft;

import android.content.Context;

import org.chimeramc.client.core.minecraft.GamePackageManager;
import org.chimeramc.client.core.minecraft.LaunchTrace;
import org.chimeramc.client.core.versions.GameVersion;
import org.chimeramc.client.preloader.PreloaderSignatureRulesManager;
import org.chimeramc.client.util.PlayStoreValidator;
import org.chimeramc.client.core.auth.MsftAccountStore;

import java.io.File;

/**
 * The launcher's {@link LaunchProbe.Source}: reads each preflight fact from the device.
 *
 * <p>This is the only part of the launch preflight that touches Android, so it is the only part
 * that must run off the UI thread. Everything it reads is a real device fact — a package on disk,
 * the APK's ABI table, the extraction marker, the parsed signature rules — and each read can fail
 * in a way that has nothing to do with the user's launch, so every one of them is contained here
 * rather than in the caller.
 *
 * <p>The one fact that is <em>not</em> contained is {@code gameFilesPresent}: a missing package is
 * the state the user has to act on, so it is reported as missing rather than hidden behind a
 * fallback. {@link LaunchProbe#gather} reads it without a fallback for the same reason.
 */
public final class AndroidLaunchProbe implements LaunchProbe.Source {

    private final Context context;
    private final GameVersion version;
    private final LaunchTrace trace;

    public AndroidLaunchProbe(Context context, GameVersion version, LaunchTrace trace) {
        this.context = context;
        this.version = version;
        this.trace = trace;
    }

    @Override
    public boolean versionSelected() {
        return version != null;
    }

    @Override
    public boolean gameFilesPresent() {
        if (version == null || version.versionDir == null) {
            return false;
        }
        // The instance's package is the base.apk.chimera the import pipeline writes; its absence is
        // what "game files missing" means, not a version that was never selected.
        return new File(version.versionDir, "base.apk.chimera").exists();
    }

    @Override
    public boolean abiIncompatible() {
        if (version == null) {
            return false;
        }
        try {
            return !GamePackageManager.getInstance(context, version, trace, null)
                    .requireLauncherSupportsVersion(version);
        } catch (Throwable ignored) {
            // An unreadable package is reported as missing game files by gameFilesPresent(), not as
            // an ABI problem; leaving the flag false keeps the message honest.
            return false;
        }
    }

    @Override
    public boolean extractionStale() {
        if (version == null) {
            return false;
        }
        try {
            return !GamePackageManager.isExtractionCacheCurrent(context, version);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public boolean signatureRulesCoverBuild() {
        if (version == null || version.versionCode == null || version.versionCode.isEmpty()) {
            // An unknown build cannot be judged; treating it as covered avoids refusing a launch
            // over a missing label rather than a real gap.
            return true;
        }
        try {
            return PreloaderSignatureRulesManager.describeCoverageFailure(context, version.versionCode)
                    == null;
        } catch (Throwable ignored) {
            return true;
        }
    }

    @Override
    public boolean minecraftInstalled() {
        try {
            return PlayStoreValidator.isMinecraftInstalled(context);
        } catch (Throwable ignored) {
            return true;
        }
    }

    @Override
    public boolean signInRequired() {
        try {
            return org.chimeramc.client.settings.FeatureSettings.getInstance()
                    .isLauncherManagedMcLoginEnabled();
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public boolean signedIn() {
        try {
            for (MsftAccountStore.MsftAccount account : MsftAccountStore.list(context)) {
                if (account.active) {
                    return true;
                }
            }
            return false;
        } catch (Throwable ignored) {
            return true;
        }
    }
}
