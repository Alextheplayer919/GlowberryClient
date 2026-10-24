package org.chimeramc.client.launcher.core.minecraft;

/**
 * The facts a launch preflight gathers, and the seam that gathers them.
 *
 * <p>Gathering them is I/O: stat the instance's package, read the APK's ABI table, hash the
 * extraction marker, parse the signature rules. On the UI thread that is the reported "huge lag when
 * Launch is pressed" — the button dims and then nothing happens while the disk is read.
 *
 * <p>The split is deliberate: {@link Source} is the I/O and runs on a background thread,
 * {@link LaunchReadiness} is the pure decision and runs on either. Keeping the probe a separate
 * type from the decision is what lets the UI call the I/O off-thread without duplicating the
 * precedence rules, and lets a test drive every branch with no device.
 *
 * <p>Every read is defensive. A probe is a preflight, not a validator: a source that throws must
 * leave the fact at its safe default rather than turning a diagnostic problem into a launch failure
 * the user cannot act on. The one fact that is not swallowed is "the game files are gone", because
 * that is the state the user has to fix.
 */
public final class LaunchProbe {

    /** The I/O the preflight needs. Implemented by the launcher; faked in tests. */
    public interface Source {
        /** Whether a version is selected and usable. */
        boolean versionSelected();

        /** Whether the instance's package is present on disk. */
        boolean gameFilesPresent();

        /**
         * Whether the instance is the installed Minecraft package and the launcher cannot read the
         * game's native library out of it (see {@link LaunchReadiness.State#PLAY_STORE_PACKAGE}).
         */
        boolean installedPackageUnreadable();

        /** Whether the APK ships no ABI this process can load. */
        boolean abiIncompatible();

        /** Whether the extracted native libraries are older than the current extractor. */
        boolean extractionStale();

        /** Whether the signature rules cover the installed build. */
        boolean signatureRulesCoverBuild();

        /** Whether the Minecraft package is installed on the device. */
        boolean minecraftInstalled();

        /** Whether a launcher-managed sign-in is required. */
        boolean signInRequired();

        /** Whether an account is signed in. */
        boolean signedIn();
    }

    /**
     * Reads every fact from [source], containing each failure at its safe default.
     *
     * <p>{@code versionSelected} is read first and, when false, the remaining reads are skipped: on
     * a fresh install every other probe would be asking about a version that does not exist, and a
     * missing version is the only state that matters.
     */
    public static LaunchReadiness.Facts gather(Source source) {
        LaunchReadiness.Facts facts = new LaunchReadiness.Facts();
        if (source == null) {
            return facts.versionSelected(false);
        }
        boolean selected = read(source::versionSelected, false);
        facts.versionSelected(selected);
        if (!selected) {
            return facts;
        }
        // A throwing read keeps the fact at its safe default rather than failing the launch:
        // "game files missing" is the only probe whose answer the user must act on, so it is read
        // without a fallback that would hide it.
        facts.gameFilesPresent(read(source::gameFilesPresent, false));
        facts.installedPackageUnreadable(read(source::installedPackageUnreadable, false));
        facts.abiIncompatible(read(source::abiIncompatible, false));
        facts.extractionStale(read(source::extractionStale, false));
        facts.signatureRulesCoverBuild(read(source::signatureRulesCoverBuild, true));
        facts.minecraftInstalled(read(source::minecraftInstalled, true));
        facts.signInRequired(read(source::signInRequired, false));
        facts.signedIn(read(source::signedIn, true));
        return facts;
    }

    private static boolean read(Probe probe, boolean fallback) {
        try {
            return probe.read();
        } catch (Throwable t) {
            // Includes LinkageError: a device-side method that does not exist must degrade a
            // diagnostic, not kill the launch. See Api28Compat for the class of bug this guards.
            return fallback;
        }
    }

    private interface Probe {
        boolean read();
    }

    private LaunchProbe() {
    }
}
