package org.chimeramc.client.launcher.core.minecraft;

import android.content.Context;

import org.chimeramc.client.core.minecraft.AbiBitness;
import org.chimeramc.client.core.minecraft.GamePackageManager;
import org.chimeramc.client.core.minecraft.LaunchTrace;
import org.chimeramc.client.core.minecraft.MinecraftLauncher;
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
        // An instance can be the device-installed Minecraft package (the Play Store build) rather
        // than an import. That package is read out of the installed app, so it has no
        // base.apk.chimera and requiring one reported a perfectly launchable install as "game files
        // missing" - the bug this method's split into two facts fixes. Its files are "present" by
        // definition; whether the launcher can read the game library out of them is the separate
        // installedPackageUnreadable() question, and LaunchReadiness reports that accurately.
        if (version.isInstalled) {
            return true;
        }
        // The instance's package is the base.apk.chimera the import pipeline writes; its absence is
        // what "game files missing" means, not a version that was never selected.
        return new File(version.versionDir, "base.apk.chimera").exists();
    }

    /**
     * Whether this instance is the installed Minecraft package and the launcher cannot read the
     * game's native library out of it.
     *
     * <p>This is the Play Store case: the package is present and valid, but it is an app the
     * launcher does not own, so its libraries are not laid out the way an imported APK's are. The
     * check asks the package manager for the installed app's own APK paths and looks for the game
     * library this process could load; when nothing readable is found the state is reported, and
     * when the app is not installed at all this is left false so {@code minecraftInstalled()}
     * reports that instead.
     */
    @Override
    public boolean installedPackageUnreadable() {
        if (version == null || !version.isInstalled) {
            return false;
        }
        try {
            return !installedPackageShipsGameLibrary();
        } catch (Throwable ignored) {
            // A read that fails is not proof of a bad install; leave it to the other facts rather
            // than accusing the user's Play Store build on the strength of an exception.
            return false;
        }
    }

    /**
     * Whether the installed Minecraft package ships {@code libminecraftpe.so} for an ABI this
     * process can load.
     *
     * <p>Reads the installed app's own APK and split paths through the package manager rather than
     * this app's {@code ApplicationInfo}: the launcher does not own the game's package, so its own
     * source dir is the launcher APK, not Minecraft's.
     */
    private boolean installedPackageShipsGameLibrary() throws Exception {
        android.content.pm.PackageManager pm = context.getPackageManager();
        android.content.pm.PackageInfo info =
                pm.getPackageInfo(MinecraftLauncher.MC_PACKAGE_NAME, 0);
        java.util.List<File> apks = new java.util.ArrayList<>();
        if (info.applicationInfo != null) {
            if (info.applicationInfo.sourceDir != null) {
                apks.add(new File(info.applicationInfo.sourceDir));
            }
            if (info.applicationInfo.splitSourceDirs != null) {
                for (String path : info.applicationInfo.splitSourceDirs) {
                    if (path != null) apks.add(new File(path));
                }
            }
        }
        if (apks.isEmpty()) {
            return false;
        }
        java.util.List<String> shipped = AbiBitness.INSTANCE.abisInApks(apks);
        boolean processIs64Bit = android.os.Process.is64Bit();
        return AbiBitness.INSTANCE.hasLoadableAbi(shipped, processIs64Bit);
    }

    @Override
    public boolean abiIncompatible() {
        if (version == null) {
            return false;
        }
        try {
            // Ask the ABI question directly rather than through GamePackageManager.getInstance().
            // That factory's init block runs the whole native-library extraction and builds an
            // AssetManager, so using it here meant the preflight performed the expensive preparation
            // just to answer a yes/no question, and the launch then discarded that instance and
            // built another - a large part of the reported lag when Launch is pressed.
            return GamePackageManager.versionAbiIsIncompatible(context, version);
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
