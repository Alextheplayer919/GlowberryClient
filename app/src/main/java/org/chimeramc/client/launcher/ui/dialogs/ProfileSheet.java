package org.chimeramc.client.launcher.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import org.chimeramc.client.R;
import org.chimeramc.client.core.auth.MsftAccountStore;
import org.chimeramc.client.core.mods.inbuilt.overlay.CosmeticsPanel;
import org.chimeramc.client.ui.activities.AccountsActivity;
import org.chimeramc.client.ui.animation.DynamicAnim;
import org.chimeramc.client.ui.components.UiTokens;
import org.chimeramc.client.ui.fragments.SkinsSettingsFragment;

import java.util.List;

/**
 * The Play screen's profile sheet (U2).
 *
 * <p>Cosmetics used to be a top-level destination, which meant a whole tab — and a whole activity —
 * for a surface you only touch when you want to change your look. It is not navigation; it is a
 * property of the player. So it lives here, behind the player's own avatar on the Play screen,
 * together with the account row and the skin-pack manager, which are the other things that describe
 * the player.
 *
 * <p>Two inner tabs, exactly as the old Customize host had: the Closet (the same
 * {@link CosmeticsPanel} the in-game Mod Menu hosts, so there is no second catalogue) and Skins
 * (the same {@link SkinsSettingsFragment} the Customize host used). The header is the one place a
 * player can see who they are signed in as and switch accounts.
 */
public class ProfileSheet extends Dialog {

    private static final String TAG_SKINS = "profile_skins";

    private final Activity activity;
    private CosmeticsPanel cosmeticsPanel;
    private FrameLayout cosmeticsHost;
    private FrameLayout skinsHost;
    private int skinsHostId;
    private TextView closetTab;
    private TextView skinsTab;
    private boolean skinsShown;

    public ProfileSheet(Activity activity) {
        super(activity);
        this.activity = activity;
    }

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(buildContent());
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attrs = window.getAttributes();
            attrs.dimAmount = 0.55f;
            window.setAttributes(attrs);
        }
        setOnDismissListener(d -> {
            if (cosmeticsPanel != null) cosmeticsPanel.onHidden();
        });
        showCloset();
    }

    private View buildContent() {
        int accent = UiTokens.resolveAccent(activity);
        int pad = dp(16);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        GradientDrawable panel = new GradientDrawable();
        panel.setShape(GradientDrawable.RECTANGLE);
        panel.setCornerRadii(new float[]{dp(UiTokens.RADIUS_XLARGE), dp(UiTokens.RADIUS_XLARGE),
                dp(UiTokens.RADIUS_XLARGE), dp(UiTokens.RADIUS_XLARGE), 0f, 0f, 0f, 0f});
        panel.setColor(UiTokens.BG_SURFACE);
        panel.setStroke(Math.max(1, dp(1)), UiTokens.STROKE_HAIRLINE);
        root.setBackground(panel);

        // Account header: who you are, with a tap to switch. A signed-out player is told so rather
        // than shown an empty name.
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(pad / 2, pad / 2, pad / 2, pad / 2);

        MsftAccountStore.MsftAccount active = activeAccount();
        TextView name = new TextView(activity);
        name.setText(active == null
                ? activity.getString(R.string.profile_signed_out)
                : displayName(active));
        name.setTextColor(UiTokens.TEXT_PRIMARY);
        name.setTextSize(17f);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        name.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(name);

        TextView switchAccount = new TextView(activity);
        switchAccount.setText(R.string.profile_switch_account);
        switchAccount.setTextColor(accent);
        switchAccount.setTextSize(12f);
        switchAccount.setPadding(pad, pad / 2, pad, pad / 2);
        switchAccount.setOnClickListener(v -> {
            activity.startActivity(new Intent(activity, AccountsActivity.class));
            dismiss();
        });
        DynamicAnim.applyPressScale(switchAccount);
        header.addView(switchAccount);
        root.addView(header);

        // Inner tabs: Closet / Skins.
        LinearLayout tabs = new LinearLayout(activity);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(0, pad / 2, 0, pad / 2);
        closetTab = tabView(R.string.cosmetics_closet);
        skinsTab = tabView(R.string.customize_tab_skins);
        LinearLayout.LayoutParams skinsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        skinsParams.leftMargin = dp(8);
        skinsTab.setLayoutParams(skinsParams);
        closetTab.setOnClickListener(v -> showCloset());
        skinsTab.setOnClickListener(v -> showSkins());
        DynamicAnim.applyPressScale(closetTab);
        DynamicAnim.applyPressScale(skinsTab);
        tabs.addView(closetTab);
        tabs.addView(skinsTab);
        root.addView(tabs);

        FrameLayout host = new FrameLayout(activity);
        host.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(420)));
        cosmeticsHost = new FrameLayout(activity);
        host.addView(cosmeticsHost, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        skinsHost = new FrameLayout(activity);
        skinsHostId = View.generateViewId();
        skinsHost.setId(skinsHostId);
        skinsHost.setVisibility(View.GONE);
        host.addView(skinsHost, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(host);

        return root;
    }

    private TextView tabView(int textRes) {
        TextView tab = new TextView(activity);
        tab.setText(textRes);
        tab.setTextSize(13f);
        tab.setGravity(Gravity.CENTER);
        tab.setPadding(dp(18), dp(10), dp(18), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(UiTokens.RADIUS_CHIP));
        bg.setColor(UiTokens.BG_SURFACE_HOVER);
        tab.setBackground(bg);
        return tab;
    }

    private MsftAccountStore.MsftAccount activeAccount() {
        List<MsftAccountStore.MsftAccount> accounts = MsftAccountStore.list(activity);
        for (MsftAccountStore.MsftAccount account : accounts) {
            if (account.active) return account;
        }
        return accounts.isEmpty() ? null : accounts.get(0);
    }

    private String displayName(MsftAccountStore.MsftAccount account) {
        if (account.xboxGamertag != null && !account.xboxGamertag.isEmpty()) {
            return account.xboxGamertag;
        }
        if (account.minecraftUsername != null && !account.minecraftUsername.isEmpty()) {
            return account.minecraftUsername;
        }
        return activity.getString(R.string.profile_signed_out);
    }

    private void showCloset() {
        styleTabs(true);
        if (cosmeticsPanel == null) {
            cosmeticsPanel = new CosmeticsPanel(activity, false);
            cosmeticsHost.addView(cosmeticsPanel.getView(), new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        cosmeticsHost.setVisibility(View.VISIBLE);
        if (skinsHost != null) skinsHost.setVisibility(View.GONE);
        cosmeticsPanel.onShown();
    }

    private void showSkins() {
        styleTabs(false);
        if (cosmeticsPanel != null) {
            cosmeticsPanel.onHidden();
            cosmeticsHost.setVisibility(View.GONE);
        }
        if (skinsHost == null) return;
        skinsHost.setVisibility(View.VISIBLE);
        if (skinsShown || !(activity instanceof FragmentActivity)) return;
        skinsShown = true;
        FragmentActivity host = (FragmentActivity) activity;
        host.getSupportFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(skinsHostId, new SkinsSettingsFragment(), TAG_SKINS)
                .commitAllowingStateLoss();
    }

    private void styleTabs(boolean closetActive) {
        int accent = UiTokens.resolveAccent(activity);
        if (closetTab != null) {
            closetTab.setTextColor(closetActive ? accent : UiTokens.TEXT_SECONDARY);
            closetTab.setTypeface(null, closetActive
                    ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
        if (skinsTab != null) {
            skinsTab.setTextColor(!closetActive ? accent : UiTokens.TEXT_SECONDARY);
            skinsTab.setTypeface(null, !closetActive
                    ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
