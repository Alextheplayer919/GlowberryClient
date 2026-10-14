package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.chimeramc.client.R;
import org.chimeramc.client.core.cosmetics.CosmeticCatalog;
import org.chimeramc.client.core.cosmetics.CosmeticStore;
import org.chimeramc.client.core.cosmetics.CosmeticSyncModule;
import org.chimeramc.client.core.cosmetics.CosmeticSyncProtocol;
import org.chimeramc.client.core.cosmetics.NativeCosmeticsBridge;
import org.chimeramc.client.core.cosmetics.NativeCosmeticsFeed;
import org.chimeramc.client.core.cosmetics.NativeCosmeticsRuntime;
import org.chimeramc.client.core.mods.inbuilt.manager.InbuiltModManager;
import org.chimeramc.client.ui.animation.DynamicAnim;
import org.chimeramc.client.ui.components.UiTokens;

import java.util.ArrayList;
import java.util.List;

/**
 * The Closet: the in-game cosmetics screen, restyled to the blueberry window.
 *
 * <p>Layout follows the spec: a slim left category rail (Capes, Hats, Pets, Back, Offhand,
 * Effects), a centre grid of cosmetic cards with a name and a state pill, and a right panel holding
 * the live 3D preview with the player's real skin plus an Equip/Unequip button.
 *
 * <p>All colours come from {@link UiTokens}. The catalogue is large (hundreds of entries), so the
 * grid is a {@link ScrollView} of wrapping rows rather than a RecyclerView — the panel is only alive
 * while the menu is open. Selecting a card equips it, publishes to the native registry and
 * re-advertises to peers, exactly as before.
 */
public final class CosmeticsPanel {

    /** One catalogue entry as a card: a stable id, a label, and what picking it does. */
    private static final class Card {
        final String id;
        final String label;
        final Runnable apply;
        final boolean equipped;
        /** The cape to render as a thumbnail, or null for a card that has no visual. */
        final CosmeticCatalog.Cape cape;

        Card(String id, String label, boolean equipped, Runnable apply) {
            this(id, label, equipped, apply, null);
        }

        Card(String id, String label, boolean equipped, Runnable apply, CosmeticCatalog.Cape cape) {
            this.id = id;
            this.label = label;
            this.equipped = equipped;
            this.apply = apply;
            this.cape = cape;
        }
    }

    /** The category rail entries. */
    private enum Category {
        CAPES, HATS, PETS, BACK, OFFHAND, EFFECTS
    }

    private final Activity activity;
    private final CosmeticStore store;
    private final LinearLayout root;
    private final CapePreviewView preview;
    private final boolean compact;

    private final LinearLayout railColumn;
    private final LinearLayout gridContainer;
    private final TextView gridTitle;
    private final TextView equipButton;
    private TextView gameStatus;
    private TextView syncStatus;
    private TextView syncToggle;
    private TextView vfxToggle;
    private LinearLayout peersContainer;
    private LinearLayout manualPeerRow;
    private EditText manualPeerInput;

    private Category category = Category.CAPES;
    private final List<TextView> railButtons = new ArrayList<>();

    public CosmeticsPanel(Activity activity, boolean compact) {
        this.activity = activity;
        this.compact = compact;
        this.store = new CosmeticStore(activity);

        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackground(windowBackground());
        root.setPadding(dp(12), dp(10), dp(12), dp(10));

        // Left rail.
        railColumn = new LinearLayout(activity);
        railColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams railParams = new LinearLayout.LayoutParams(
                dp(UiTokens.RAIL_WIDTH), ViewGroup.LayoutParams.MATCH_PARENT);
        railParams.setMarginEnd(dp(10));
        root.addView(railColumn, railParams);
        buildRail();

        // Centre grid.
        LinearLayout centre = new LinearLayout(activity);
        centre.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams centreParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, compact ? 0.5f : 0.6f);
        centreParams.setMarginEnd(dp(10));
        root.addView(centre, centreParams);
        gridTitle = new TextView(activity);
        gridTitle.setTextSize(UiTokens.TEXT_SECTION);
        gridTitle.setAllCaps(true);
        gridTitle.setTextColor(UiTokens.TEXT_SECONDARY);
        gridTitle.setPadding(0, 0, 0, dp(8));
        centre.addView(gridTitle);
        ScrollView gridScroll = new ScrollView(activity);
        gridContainer = new LinearLayout(activity);
        gridContainer.setOrientation(LinearLayout.VERTICAL);
        gridScroll.addView(gridContainer);
        centre.addView(gridScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // Right preview panel.
        LinearLayout previewColumn = new LinearLayout(activity);
        previewColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, compact ? 0.5f : 0.4f);
        root.addView(previewColumn, previewParams);
        preview = new CapePreviewView(activity);
        preview.setCape(store.getEquippedCapeForDisplay());
        preview.setAccessory(store.getEquippedAccessory());
        preview.setPet(store.getEquippedPet());
        preview.setContentDescription(activity.getString(R.string.cosmetics_preview_description));
        previewColumn.addView(preview, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView skinLine = new TextView(activity);
        skinLine.setTextSize(UiTokens.TEXT_CAPTION);
        skinLine.setTextColor(UiTokens.TEXT_SECONDARY);
        skinLine.setGravity(Gravity.CENTER);
        skinLine.setPadding(0, dp(4), 0, dp(4));
        if (preview.isShowingFallbackSkin()) {
            skinLine.setText(R.string.cosmetics_skin_fallback);
        } else if (preview.isShowingDefaultSteve()) {
            skinLine.setText(R.string.cosmetics_skin_default_steve);
        } else {
            skinLine.setText(activity.getString(R.string.cosmetics_skin_source,
                    preview.getSkinSourceName()));
        }
        previewColumn.addView(skinLine);

        equipButton = primaryButton(R.string.cosmetics_equip, v -> equipSelected());
        previewColumn.addView(equipButton);

        // Below the preview: the in-game status, sync and diagnostics, kept reachable but secondary.
        ScrollView details = new ScrollView(activity);
        LinearLayout detailsColumn = new LinearLayout(activity);
        detailsColumn.setOrientation(LinearLayout.VERTICAL);
        details.addView(detailsColumn);
        previewColumn.addView(details, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.5f));
        detailsColumn.addView(sectionTitle(R.string.cosmetics_in_game));
        gameStatus = bodyLine();
        detailsColumn.addView(gameStatus);
        detailsColumn.addView(secondaryButton(R.string.cosmetics_diagnostics_open,
                v -> CosmeticsDiagnosticsDialog.show(activity)));

        syncStatus = bodyLine();
        detailsColumn.addView(syncStatus);
        syncToggle = secondaryButton(R.string.cosmetics_sync_toggle_on, v -> {
            InbuiltModManager manager = InbuiltModManager.getInstance(activity);
            manager.setCosmeticSyncEnabled(!manager.isCosmeticSyncEnabled());
            refreshSyncStatus();
        });
        detailsColumn.addView(syncToggle);
        vfxToggle = secondaryButton(R.string.cosmetics_vfx_toggle_on, v -> {
            InbuiltModManager manager = InbuiltModManager.getInstance(activity);
            manager.setCosmeticVfxEnabled(!manager.isCosmeticVfxEnabled());
            refreshVfxToggle();
        });
        detailsColumn.addView(vfxToggle);

        detailsColumn.addView(sectionTitle(R.string.cosmetics_peers_title));
        peersContainer = new LinearLayout(activity);
        peersContainer.setOrientation(LinearLayout.VERTICAL);
        detailsColumn.addView(peersContainer);

        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        manualPeerRow = new LinearLayout(activity);
        manualPeerRow.setOrientation(LinearLayout.HORIZONTAL);
        manualPeerInput = new EditText(activity);
        manualPeerInput.setHint(R.string.cosmetics_sync_manual_hint);
        manualPeerInput.setSingleLine(true);
        manualPeerInput.setTextSize(UiTokens.TEXT_CAPTION);
        manualPeerInput.setTextColor(UiTokens.TEXT_PRIMARY);
        manualPeerInput.setText(manager.getCosmeticManualPeer());
        manualPeerRow.addView(manualPeerInput, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        manualPeerRow.addView(secondaryButton(R.string.cosmetics_sync_manual_save, v -> {
            manager.setCosmeticManualPeer(manualPeerInput.getText().toString());
            toast(R.string.cosmetics_sync_manual_saved);
            CosmeticSyncModule.requestAnnounce();
            refreshSyncStatus();
        }));
        detailsColumn.addView(manualPeerRow);

        selectCategory(Category.CAPES);
        refreshGameStatus();
        refreshSyncStatus();
        refreshVfxToggle();
        refreshEquipButton();
    }

    // ---- Rail ------------------------------------------------------------------------------

    private void buildRail() {
        for (Category c : Category.values()) {
            TextView b = new TextView(activity);
            b.setText(categoryLabel(c));
            b.setTextSize(11f);
            b.setGravity(Gravity.CENTER);
            b.setSingleLine(true);
            b.setPadding(dp(6), dp(10), dp(6), dp(10));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(UiTokens.SPACE_2);
            b.setLayoutParams(lp);
            b.setOnClickListener(v -> selectCategory(c));
            DynamicAnim.applyPressScale(b);
            railColumn.addView(b);
            railButtons.add(b);
        }
    }

    private void selectCategory(Category c) {
        this.category = c;
        gridTitle.setText(categoryLabel(c));
        for (int i = 0; i < railButtons.size(); i++) {
            boolean selected = Category.values()[i] == c;
            TextView b = railButtons.get(i);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(dp(UiTokens.RADIUS_CARD));
            bg.setColor(selected ? getAccent() : UiTokens.BG_SURFACE);
            if (!selected) bg.setStroke(dp(1), UiTokens.STROKE_HAIRLINE);
            b.setBackground(bg);
            b.setTextColor(selected ? Color.WHITE : UiTokens.TEXT_SECONDARY);
        }
        rebuildGrid();
    }

    private String categoryLabel(Category c) {
        switch (c) {
            case CAPES: return activity.getString(R.string.cosmetics_capes);
            case HATS: return activity.getString(R.string.cosmetics_accessories);
            case PETS: return activity.getString(R.string.cosmetics_pets);
            case BACK: return activity.getString(R.string.closet_category_back);
            case OFFHAND: return activity.getString(R.string.closet_category_offhand);
            case EFFECTS: return activity.getString(R.string.closet_category_effects);
            default: return "";
        }
    }

    // ---- Grid ------------------------------------------------------------------------------

    private void rebuildGrid() {
        gridContainer.removeAllViews();
        List<Card> cards = cardsFor(category);
        int columns = compact ? 2 : 3;
        LinearLayout row = null;
        for (int i = 0; i < cards.size(); i++) {
            if (i % columns == 0) {
                row = new LinearLayout(activity);
                row.setOrientation(LinearLayout.HORIZONTAL);
                gridContainer.addView(row, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            row.addView(buildCard(cards.get(i)), new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
    }

    private View buildCard(Card card) {
        LinearLayout cardView = new LinearLayout(activity);
        cardView.setOrientation(LinearLayout.VERTICAL);
        cardView.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(3), dp(3), dp(3), dp(3));
        cardView.setLayoutParams(lp);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(12));
        bg.setColor(card.equipped ? UiTokens.ACCENT_900 : UiTokens.BG_SURFACE);
        bg.setStroke(dp(1), card.equipped ? getAccent() : UiTokens.STROKE_HAIRLINE);
        cardView.setBackground(bg);

        // The cape is shown, not named: a thumbnail of the back of the cloth, so a player can see
        // what a cape looks like without equipping it. The name sits underneath, on its own line.
        if (card.cape != null) {
            android.widget.ImageView thumb = new android.widget.ImageView(activity);
            int thumbHeight = dp(64);
            int thumbWidth = Math.max(dp(28), (int) (thumbHeight * org.chimeramc.client.core
                    .cosmetics.CapeThumbnail.ASPECT));
            LinearLayout.LayoutParams thumbParams =
                    new LinearLayout.LayoutParams(thumbWidth, thumbHeight);
            thumbParams.gravity = Gravity.CENTER_HORIZONTAL;
            thumb.setLayoutParams(thumbParams);
            thumb.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            // A rounded frame around the swatch, so each cape reads as its own separate tile and the
            // bitmap's square corners do not poke out of the rounded card.
            GradientDrawable frame = new GradientDrawable();
            frame.setShape(GradientDrawable.RECTANGLE);
            frame.setCornerRadius(dp(8));
            frame.setColor(UiTokens.BG_SURFACE_HOVER);
            frame.setStroke(dp(1), UiTokens.STROKE_HAIRLINE);
            thumb.setBackground(frame);
            thumb.setClipToOutline(true);
            android.graphics.Bitmap bmp = capeThumbnailBitmap(card.cape, thumbWidth, thumbHeight);
            if (bmp != null) {
                thumb.setImageBitmap(bmp);
            }
            cardView.addView(thumb);
        }

        TextView name = new TextView(activity);
        name.setText(card.label);
        name.setTextSize(12f);
        name.setTextColor(UiTokens.TEXT_PRIMARY);
        name.setMaxLines(2);
        name.setGravity(Gravity.CENTER_HORIZONTAL);
        name.setPadding(0, dp(6), 0, 0);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        cardView.addView(name);

        TextView state = new TextView(activity);
        state.setText(card.equipped ? R.string.closet_state_equipped : R.string.closet_state_owned);
        state.setTextSize(UiTokens.TEXT_SECTION);
        state.setAllCaps(true);
        state.setGravity(Gravity.CENTER_HORIZONTAL);
        state.setTextColor(card.equipped ? getAccent() : UiTokens.TEXT_SECONDARY);
        state.setPadding(0, dp(4), 0, 0);
        cardView.addView(state);

        cardView.setOnClickListener(v -> {
            card.apply.run();
            rebuildGrid();
            refreshEquipButton();
        });
        DynamicAnim.applyPressScale(cardView);
        return cardView;
    }

    /**
     * Renders a cape's back into a bitmap for the grid. Returns null on any allocation failure so a
     * card without a thumbnail still renders (its name is the fallback), rather than the whole grid
     * failing.
     */
    private android.graphics.Bitmap capeThumbnailBitmap(CosmeticCatalog.Cape cape, int width,
                                                        int height) {
        if (width <= 0 || height <= 0) return null;
        try {
            int[] pixels = new int[width * height];
            org.chimeramc.client.core.cosmetics.CapeThumbnail.render(
                    cape, width, height, pixels, UiTokens.BG_SURFACE);
            return android.graphics.Bitmap.createBitmap(pixels, width, height,
                    android.graphics.Bitmap.Config.ARGB_8888);
        } catch (Throwable t) {
            return null;
        }
    }

    private List<Card> cardsFor(Category c) {
        List<Card> cards = new ArrayList<>();
        switch (c) {
            case CAPES:
                addCapeCards(cards);
                break;
            case HATS:
                addAccessoryCards(cards, true);
                break;
            case BACK:
                addAccessoryCards(cards, false);
                break;
            case PETS:
                addPetCards(cards);
                break;
            case OFFHAND:
                cards.add(new Card(CosmeticCatalog.NONE,
                        activity.getString(R.string.closet_offhand_note), false, () -> { }));
                break;
            case EFFECTS:
                cards.add(new Card("vfx", activity.getString(R.string.closet_effects_note), false,
                        () -> {
                            InbuiltModManager manager = InbuiltModManager.getInstance(activity);
                            manager.setCosmeticVfxEnabled(!manager.isCosmeticVfxEnabled());
                            refreshVfxToggle();
                        }));
                break;
            default:
                break;
        }
        return cards;
    }

    private void addCapeCards(List<Card> cards) {
        String equipped = store.getEquippedCapeId();
        cards.add(new Card(CosmeticCatalog.NONE, activity.getString(R.string.cosmetics_none),
                CosmeticCatalog.NONE.equals(equipped), () -> {
            store.setEquippedCape(CosmeticCatalog.NONE);
            preview.setCape(null);
            onCosmeticChanged();
        }));
        for (final CosmeticCatalog.Cape cape : CosmeticCatalog.capes()) {
            cards.add(new Card(cape.id, cape.name, cape.id.equals(equipped), () -> {
                store.setEquippedCape(cape.id);
                preview.setCape(cape);
                onCosmeticChanged();
            }, cape));
        }
    }

    private void addAccessoryCards(List<Card> cards, boolean hats) {
        String equipped = store.getEquippedAccessoryId();
        cards.add(new Card(CosmeticCatalog.NONE, activity.getString(R.string.cosmetics_none),
                CosmeticCatalog.NONE.equals(equipped), () -> {
            store.setEquippedAccessory(CosmeticCatalog.NONE);
            preview.setAccessory(null);
            onCosmeticChanged();
        }));
        for (final CosmeticCatalog.Accessory accessory : CosmeticCatalog.accessories()) {
            if (CosmeticCatalog.NONE.equals(accessory.id)) continue;
            // A back-worn piece is a "Back" card; everything else is a "Hat". The split lives in
            // ClosetCategory so it is unit-tested and cannot drop an entry.
            if (hats != org.chimeramc.client.core.cosmetics.ClosetCategory.isHat(accessory)) {
                continue;
            }
            cards.add(new Card(accessory.id, accessory.name, accessory.id.equals(equipped), () -> {
                store.setEquippedAccessory(accessory.id);
                preview.setAccessory(accessory);
                onCosmeticChanged();
            }));
        }
    }

    private void addPetCards(List<Card> cards) {
        String equipped = store.getEquippedPetId();
        cards.add(new Card(CosmeticCatalog.NONE, activity.getString(R.string.cosmetics_none),
                CosmeticCatalog.NONE.equals(equipped), () -> {
            store.setEquippedPet(CosmeticCatalog.NONE);
            preview.setPet(null);
            onCosmeticChanged();
        }));
        for (final CosmeticCatalog.Pet pet : CosmeticCatalog.pets()) {
            cards.add(new Card(pet.id, pet.name, pet.id.equals(equipped), () -> {
                store.setEquippedPet(pet.id);
                preview.setPet(pet);
                onCosmeticChanged();
            }));
        }
    }

    // ---- Equip -----------------------------------------------------------------------------

    private void equipSelected() {
        // The card tap already equips; this button is the explicit affordance the spec asks for, so
        // it re-equips the current selection (idempotent) and republishes.
        onCosmeticChanged();
        toast(R.string.cosmetics_equipped_toast);
    }

    private void refreshEquipButton() {
        if (equipButton == null) return;
        boolean anyEquipped = !CosmeticCatalog.NONE.equals(store.getEquippedCapeId())
                || !CosmeticCatalog.NONE.equals(store.getEquippedAccessoryId())
                || !CosmeticCatalog.NONE.equals(store.getEquippedPetId());
        equipButton.setText(anyEquipped ? R.string.cosmetics_equip
                : R.string.cosmetics_equip);
    }

    private void onCosmeticChanged() {
        try {
            NativeCosmeticsBridge.publishLocal(
                    store.getEquippedCapeForDisplay(),
                    store.getEquippedAccessory(),
                    store.getEquippedPet());
            NativeCosmeticsRuntime.setFrameGait(preview.getPetLocomotion());
        } catch (Throwable ignored) {
            // Fail-closed: the preview and the equipped selection are unaffected.
        }
        CosmeticSyncModule.requestAnnounce();
        refreshGameStatus();
        refreshSyncStatus();
    }

    // ---- Status ----------------------------------------------------------------------------

    private void refreshSyncStatus() {
        if (syncStatus == null) return;
        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        boolean enabled = manager.isCosmeticSyncEnabled();
        if (syncToggle != null) {
            syncToggle.setText(enabled
                    ? R.string.cosmetics_sync_toggle_off
                    : R.string.cosmetics_sync_toggle_on);
        }
        if (manualPeerRow != null) {
            manualPeerRow.setVisibility(manager.isVoiceRelayEnabled()
                    ? View.GONE : View.VISIBLE);
        }
        if (!enabled) {
            syncStatus.setText(R.string.cosmetics_sync_off);
            renderPeers(null);
            return;
        }
        CosmeticSyncModule module = CosmeticSyncModule.peek();
        if (module == null || !module.isRunning()) {
            syncStatus.setText(R.string.cosmetics_sync_with_game);
            renderPeers(null);
            return;
        }
        String route = module.routeLabel();
        syncStatus.setText(activity.getString(R.string.cosmetics_sync_seen,
                module.registry().size(), route));
        renderPeers(module);
    }

    private void refreshVfxToggle() {
        if (vfxToggle == null) return;
        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        vfxToggle.setText(manager.isCosmeticVfxEnabled()
                ? R.string.cosmetics_vfx_toggle_off
                : R.string.cosmetics_vfx_toggle_on);
    }

    private void renderPeers(CosmeticSyncModule module) {
        if (peersContainer == null) return;
        peersContainer.removeAllViews();
        if (module == null) {
            peersContainer.addView(peerLine(activity.getString(R.string.cosmetics_peers_none)));
            return;
        }
        java.util.List<CosmeticSyncProtocol.Advert> peers = module.registry().snapshot();
        if (peers.isEmpty()) {
            peersContainer.addView(peerLine(activity.getString(R.string.cosmetics_peers_none)));
            return;
        }
        for (CosmeticSyncProtocol.Advert peer : peers) {
            peersContainer.addView(buildPeerRow(peer));
        }
    }

    private View buildPeerRow(CosmeticSyncProtocol.Advert peer) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(3), 0, dp(3));
        row.addView(GlowberryBadge.create(activity, compact));
        LinearLayout labels = new LinearLayout(activity);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView name = new TextView(activity);
        name.setText(peer.name.isEmpty() ? peer.peerId : peer.name);
        name.setTextSize(12f);
        name.setTextColor(UiTokens.TEXT_PRIMARY);
        labels.addView(name);
        TextView wearing = new TextView(activity);
        wearing.setText(peerWearingLine(peer));
        wearing.setTextSize(UiTokens.TEXT_CAPTION);
        wearing.setTextColor(UiTokens.TEXT_SECONDARY);
        labels.addView(wearing);
        row.addView(labels, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private String peerWearingLine(CosmeticSyncProtocol.Advert peer) {
        List<String> parts = new ArrayList<>();
        CosmeticCatalog.Cape cape = CosmeticCatalog.equippedCape(peer.capeId);
        CosmeticCatalog.Accessory accessory = CosmeticCatalog.equippedAccessory(peer.accessoryId);
        CosmeticCatalog.Pet pet = CosmeticCatalog.equippedPet(peer.petId);
        if (cape != null) parts.add(cape.name);
        if (accessory != null) parts.add(accessory.name);
        if (pet != null) parts.add(pet.name);
        if (parts.isEmpty()) return activity.getString(R.string.cosmetics_peer_wearing_none);
        return activity.getString(R.string.cosmetics_peer_wearing,
                android.text.TextUtils.join(", ", parts));
    }

    private void refreshGameStatus() {
        if (gameStatus == null) return;
        try {
            switch (NativeCosmeticsFeed.renderState()) {
                case ACTIVE:
                    gameStatus.setText(R.string.cosmetics_native_active);
                    break;
                case HOOK_ONLY:
                    gameStatus.setText(R.string.cosmetics_native_hook_only);
                    break;
                default:
                    gameStatus.setText(R.string.cosmetics_native_inactive);
                    break;
            }
        } catch (Throwable t) {
            gameStatus.setText(R.string.cosmetics_native_inactive);
        }
    }

    // ---- Small builders --------------------------------------------------------------------

    private TextView sectionTitle(int res) {
        TextView title = new TextView(activity);
        title.setText(res);
        title.setTextSize(UiTokens.TEXT_SECTION);
        title.setAllCaps(true);
        title.setTextColor(UiTokens.TEXT_SECONDARY);
        title.setPadding(0, dp(8), 0, dp(4));
        return title;
    }

    private TextView bodyLine() {
        TextView line = new TextView(activity);
        line.setTextSize(UiTokens.TEXT_CAPTION);
        line.setTextColor(UiTokens.TEXT_SECONDARY);
        line.setPadding(0, dp(2), 0, dp(2));
        return line;
    }

    private TextView peerLine(String text) {
        TextView line = bodyLine();
        line.setText(text);
        return line;
    }

    private TextView primaryButton(int labelRes, View.OnClickListener listener) {
        TextView b = baseButton(labelRes, listener);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{getAccent(), UiTokens.ACCENT_400});
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(UiTokens.RADIUS_BUTTON));
        b.setBackground(bg);
        b.setTextColor(Color.WHITE);
        return b;
    }

    private TextView secondaryButton(int labelRes, View.OnClickListener listener) {
        TextView b = baseButton(labelRes, listener);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(UiTokens.RADIUS_CARD));
        bg.setColor(UiTokens.BG_SURFACE);
        bg.setStroke(dp(1), UiTokens.STROKE_HAIRLINE);
        b.setBackground(bg);
        b.setTextColor(UiTokens.TEXT_PRIMARY);
        return b;
    }

    private TextView baseButton(int labelRes, View.OnClickListener listener) {
        TextView b = new TextView(activity);
        b.setText(labelRes);
        b.setTextSize(12f);
        b.setSingleLine(true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(14), dp(9), dp(14), dp(9));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6);
        b.setLayoutParams(lp);
        b.setOnClickListener(listener);
        DynamicAnim.applyPressScale(b);
        return b;
    }

    private GradientDrawable windowBackground() {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(UiTokens.RADIUS_WINDOW));
        bg.setColor(UiTokens.BG_WINDOW);
        bg.setStroke(dp(1), UiTokens.STROKE_HAIRLINE);
        return bg;
    }

    private void toast(int res) {
        Toast.makeText(activity, activity.getString(res), Toast.LENGTH_SHORT).show();
    }

    public View getView() {
        return root;
    }

    /** Stops the preview's animation. Kept for the ModMenuOverlay hide path. */
    public void onHidden() {
        if (preview != null) preview.stopPreview();
    }

    /**
     * Re-resolves the player's skin and restarts the preview. Called when the Closet is shown, so a
     * skin change made since it was last open (an import, an applied pack, or the game's own skin
     * once a provider is installed) appears without reopening the menu.
     */
    public void onShown() {
        if (preview != null) {
            preview.refreshSkin();
            preview.startPreview();
        }
    }

    private int getAccent() {
        return UiTokens.resolveAccent(activity);
    }

    private int dp(float value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
