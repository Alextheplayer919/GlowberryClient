package org.chimeramc.client.launcher.core.minecraft;

import org.chimeramc.client.R;

/**
 * Maps a {@link LaunchReadiness.State} to the copy the user sees (U14).
 *
 * <p>The mapping is a table rather than a chain of {@code if}s at the call site, because a launch
 * refusal has exactly one title and one explanation per state and there is no reason for the screen
 * to invent either. Keeping it here also means a state can never be added without copy — the
 * presenter's own test walks every state and fails on a missing entry.
 */
public final class LaunchFailurePresenter {

    private LaunchFailurePresenter() {
    }

    /** The dialog title for a state; {@code 0} when the state needs no dialog (it is ready). */
    public static int titleRes(LaunchReadiness.State state) {
        if (state == null) return R.string.dialog_title_launch_failed;
        switch (state) {
            case READY:
                return 0;
            case NO_VERSION_SELECTED:
                return R.string.dialog_title_no_version;
            case GAME_FILES_MISSING:
                return R.string.launch_failure_title_game_files_missing;
            case ABI_INCOMPATIBLE:
                return R.string.launch_failure_title_abi;
            case STALE_NATIVE_LIBS:
                return R.string.launch_failure_title_stale_libs;
            case SIGNATURE_RULES_MISSING:
                return R.string.launch_failure_title_rules;
            case GAME_NOT_INSTALLED:
                return R.string.launch_failure_title_not_installed;
            case SIGN_IN_REQUIRED:
                return R.string.dialog_title_login_required;
            default:
                return R.string.dialog_title_launch_failed;
        }
    }

    /** The dialog message for a state; {@code 0} when the state needs no dialog. */
    public static int messageRes(LaunchReadiness.State state) {
        if (state == null) return R.string.dialog_message_launch_failed;
        switch (state) {
            case READY:
                return 0;
            case NO_VERSION_SELECTED:
                return R.string.dialog_message_no_version;
            case GAME_FILES_MISSING:
                return R.string.launch_failure_message_game_files_missing;
            case ABI_INCOMPATIBLE:
                return R.string.launch_failure_message_abi;
            case STALE_NATIVE_LIBS:
                return R.string.launch_failure_message_stale_libs;
            case SIGNATURE_RULES_MISSING:
                return R.string.launch_failure_message_rules;
            case GAME_NOT_INSTALLED:
                return R.string.launch_failure_message_not_installed;
            case SIGN_IN_REQUIRED:
                return R.string.dialog_message_login_required;
            default:
                return R.string.dialog_message_launch_failed;
        }
    }

    /**
     * Whether a state should be shown as a warning the user can dismiss and continue past.
     *
     * <p>This is the UI half of {@link LaunchReadiness#blocksLaunch}: the two must agree, and the
     * presenter's test asserts they do for every state, because a state that warns in the model but
     * blocks in the UI is how a repairable problem becomes a dead end.
     */
    public static boolean isWarning(LaunchReadiness.State state) {
        return !LaunchReadiness.blocksLaunch(state);
    }
}
