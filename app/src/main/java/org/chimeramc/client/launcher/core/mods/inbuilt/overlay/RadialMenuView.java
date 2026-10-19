package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.PathInterpolator;

import org.chimeramc.client.R;
import org.chimeramc.client.core.cosmetics.PlayerSkinProvider;
import org.chimeramc.client.core.cosmetics.PreviewCharacterScene;
import org.chimeramc.client.core.cosmetics.PreviewRaster;
import org.chimeramc.client.core.cosmetics.PreviewScene;
import org.chimeramc.client.core.cosmetics.SkinModel;
import org.chimeramc.client.core.mods.inbuilt.overlay.RadialWheelLayout.Sector;
import org.chimeramc.client.ui.components.UiChip;
import org.chimeramc.client.ui.components.UiDraw;
import org.chimeramc.client.ui.components.UiIcons;
import org.chimeramc.client.ui.components.UiTokens;

import java.util.ArrayList;
import java.util.List;

/**
 * The radial mod wheel: four rounded annular sectors around the player's skin head, with floating
 * quick-toggle chips.
 *
 * <p>Rewritten to the "blueberry" visual language: a scrim, glass sectors with a rim, outline icons
 * over a caps label, a hub disc with the player's real skin head, and chips in three floating
 * clusters. Motion (open scale/alpha with a per-sector stagger, hover scale, press feedback) and
 * haptics are the reference feel; the geometry and the chip layout are pure
 * ({@link RadialWheelLayout}) and unit-tested.
 */
public class RadialMenuView extends View {

    /** Called when the player releases over a destination sector (or taps outside to close). */
    public interface Listener {
        void onSector(Sector sector);
        void onDismiss();
    }

    /** Called when a quick chip is tapped, with the module id it toggles. */
    public interface ChipToggle {
        void onChipToggled(String moduleId);
        boolean isChipOn(String moduleId);
    }

    /**
     * Called when a profile-driven sector is selected, with the entry that owns it.
     *
     * <p>Separate from {@link Listener} on purpose: {@code Listener} is the fixed four-destination
     * wheel's contract, while this carries an arbitrary profile entry (a module toggle, a screen, or
     * a sub-wheel). Keeping them apart means the default wheel's routing cannot be broken by a
     * profile feature it does not use.
     */
    public interface EntryListener {
        /** The player selected an entry. */
        void onEntry(WheelEntry entry);
        /** The player backed out of the open sub-wheel. */
        void onBack();
    }

    private static final Sector[] SECTORS =
            {Sector.MODS, Sector.CLOSET, Sector.SETTINGS, Sector.HUD};

    /**
     * One row of the enabled-module list.
     *
     * <p>The list shows the modules that are actually on, grouped top-to-bottom on the right, so the
     * wheel tells the player what is active without opening the module grid. Selecting a row toggles
     * its module, which is how the wheel switches things off again.
     */
    private static final class Chip {
        final String moduleId;
        final String label;

        Chip(String moduleId, String label) {
            this.moduleId = moduleId;
            this.label = label == null ? "" : label;
        }
    }

    private final Paint sectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path sectorPath = new Path();
    private final Path headClip = new Path();
    private final Rect srcHead = new Rect();
    private final RectF dstHead = new RectF();

    private final List<Chip> quickChips = new ArrayList<>();
    private final List<Chip> chips = new ArrayList<>();
    private Listener listener;
    private ChipToggle chipToggle;
    private EntryListener entryListener;
    private Sector highlighted = Sector.NONE;
    private int accent;

    // --- Profile mode ---------------------------------------------------------------------------
    //
    // The wheel is either the fixed four-destination wheel (activeProfile == null) or a
    // profile-driven one. The fixed wheel is left exactly as it was: the profile feature is
    // additive, so a regression here cannot break the shipped routing.
    private WheelEntry.Profile activeProfile;
    private final WheelNavigation navigation = new WheelNavigation();
    private WheelProfileStore profileStore;
    /** The profile-entry index the pointer or cursor is over, or -1. */
    private int highlightedEntry = -1;

    /** The controller cursor, non-null only while a pad is driving the wheel. */
    private CursorController cursor;
    /** The chip currently under the controller cursor, or -1. */
    private int cursorChip = -1;
    /** True while the cursor is visible (a touch hides it). */
    private boolean cursorVisible;

    private float density;

    private Bitmap skinHead;

    // The 3D head hub: a small software-rendered turntable of the player's real skin head.
    private final PreviewRaster headRaster = new PreviewRaster();
    private PreviewScene headScene;
    private int[] headAtlas;
    private Bitmap headBitmap;
    private float headYawDeg;
    private float headPitchDeg;
    private long headLastMs;

    // --- Motion state -------------------------------------------------------------------------

    private float openProgress;          // 0 closed .. 1 open
    private final float[] sectorScale = new float[]{1f, 1f, 1f, 1f};
    private ValueAnimator openAnimator;
    private ValueAnimator hoverAnimator;

    private static final PathInterpolator FAST_OUT_SLOW_IN =
            new PathInterpolator(0.4f, 0f, 0.2f, 1f);

    public RadialMenuView(Context context) {
        this(context, null);
    }

    public RadialMenuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        setClickable(true);
        setFocusable(false);
        density = getResources().getDisplayMetrics().density;
        accent = UiTokens.resolveAccent(context);
        buildChips();
        resolveSkinHead();
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, UiTokens.SECTION_WEIGHT, false));
        labelPaint.setLetterSpacing(UiTokens.SECTION_LETTER_SPACING);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setChipToggle(ChipToggle chipToggle) {
        this.chipToggle = chipToggle;
        invalidate();
    }

    public void setEntryListener(EntryListener entryListener) {
        this.entryListener = entryListener;
    }

    /**
     * Switches the wheel to a profile-driven one.
     *
     * <p>Loads the profiles, seeds the navigation at the root and draws the root's entries. Passing
     * null returns the wheel to the fixed four-destination layout, so a caller that does not want
     * profiles is unaffected.
     */
    public void setProfiles(Context context) {
        try {
            profileStore = new WheelProfileStore(context);
            navigation.reset(profileStore.getRootId());
            activeProfile = profileStore.rootProfile();
        } catch (Throwable t) {
            profileStore = null;
            activeProfile = null;
        }
        highlightedEntry = -1;
        invalidate();
    }

    /** True while the wheel is drawing a profile rather than the fixed four destinations. */
    public boolean isProfileMode() {
        return activeProfile != null;
    }

    /** The profile currently open (the top of the navigation stack). */
    WheelEntry.Profile currentProfile() {
        if (profileStore == null) return activeProfile;
        WheelEntry.Profile p = WheelProfileStore.findById(profileStore.load(), navigation.currentId());
        return p == null ? activeProfile : p;
    }

    /**
     * Opens a sub-wheel by profile id.
     *
     * @return true when the profile exists and the stack accepted it
     */
    boolean openSubWheel(String id) {
        if (profileStore == null || id == null) return false;
        if (WheelProfileStore.findById(profileStore.load(), id) == null) return false;
        if (!navigation.push(id)) return false;
        highlightedEntry = -1;
        playOpen();
        return true;
    }

    /**
     * Backs out one level.
     *
     * @return true when a sub-wheel was closed; false at the root, where the caller dismisses
     */
    boolean backOut() {
        if (!navigation.pop()) return false;
        highlightedEntry = -1;
        playOpen();
        return true;
    }

    private void buildChips() {
        // Left column: the quick module options the player can turn on from the wheel. Curated (a
        // short list of the modules that are useful mid-game), so the wheel stays readable.
        quickChips.clear();
        quickChips.add(new Chip("zoom", "Zoom"));
        quickChips.add(new Chip("fullbright", "Fullbright"));
        quickChips.add(new Chip("cps_display", "CPS"));
        quickChips.add(new Chip("fps_display", "FPS"));
        quickChips.add(new Chip("list_players", "Players"));
        quickChips.add(new Chip("debug_menu", "Debug"));
        // Right column: filled live from the enabled modules (see rebuildEnabledList).
        chips.clear();
    }

    /**
     * Rebuilds the enabled-module list from the live provider.
     *
     * <p>Order follows the provider's own list, so the wheel and the Mod Menu show the same order.
     * Modules are shown with their display name; a module that is registered but not enabled is not
     * listed, because the list's job is "what is on right now".
     */
    private void rebuildEnabledList() {
        chips.clear();
        try {
            // InbuiltModuleProvider.load needs an Activity; the wheel is only ever shown over one.
            android.app.Activity activity = null;
            android.content.Context ctx = getContext();
            while (ctx instanceof android.content.ContextWrapper) {
                if (ctx instanceof android.app.Activity) {
                    activity = (android.app.Activity) ctx;
                    break;
                }
                ctx = ((android.content.ContextWrapper) ctx).getBaseContext();
            }
            if (activity == null) return;
            List<org.chimeramc.client.core.mods.inbuilt.UnifiedMod> mods =
                    org.chimeramc.client.core.mods.inbuilt.InbuiltModuleProvider
                            .load(activity);
            for (org.chimeramc.client.core.mods.inbuilt.UnifiedMod mod : mods) {
                if (mod == null || !mod.isEnabled()) continue;
                // Only modules that carry a name are listed; an extension without a label cannot be
                // drawn as a row.
                String name = mod.getName();
                if (name == null || name.trim().isEmpty()) continue;
                chips.add(new Chip(mod.getStableKey(), name));
            }
        } catch (Throwable ignored) {
            chips.clear();
        }
    }

    private void resolveSkinHead() {
        try {
            PlayerSkinProvider.SkinBitmap skin = PlayerSkinProvider.resolve(getContext());
            if (skin != null && skin.bitmap != null) {
                srcHead.set(8, 8, 16, 16);
                skinHead = skin.bitmap;
                if (skin.bitmap.getWidth() == SkinModel.ATLAS_SIZE
                        && skin.bitmap.getHeight() == SkinModel.ATLAS_SIZE) {
                    headAtlas = new int[SkinModel.ATLAS_SIZE * SkinModel.ATLAS_SIZE];
                    skin.bitmap.getPixels(headAtlas, 0, SkinModel.ATLAS_SIZE, 0, 0,
                            SkinModel.ATLAS_SIZE, SkinModel.ATLAS_SIZE);
                    headScene = new PreviewScene();
                    // HEAD ONLY: the hub is the player's head, not a full-body bust. The whole
                    // character belongs in the Closet preview; the wheel's hub is a small disc, so
                    // a body would be a tiny unreadable figure and would also make the "the head
                    // never moves" complaint impossible to judge. "head" is the box id
                    // PreviewCharacterScene keys on, so passing it selects just the head box (and
                    // its overlay) from the same authored skin.
                    PreviewCharacterScene.addCharacter(headScene, headAtlas, SkinModel.ATLAS_SIZE,
                            0f, 0f, "head");
                }
            }
        } catch (Throwable ignored) {
            skinHead = null;
            headScene = null;
        }
    }

    // --- Open/close animation -----------------------------------------------------------------

    /** Runs the open animation (scrim fade + ring scale/alpha with a stagger). */
    public void playOpen() {
        cancelAnimators();
        openAnimator = ValueAnimator.ofFloat(0f, 1f);
        openAnimator.setDuration(UiTokens.MOTION_DEFAULT_MS + 20);
        openAnimator.setInterpolator(FAST_OUT_SLOW_IN);
        openAnimator.addUpdateListener(a -> {
            openProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        openAnimator.start();
    }

    /** Runs the close animation. */
    public void playClose() {
        cancelAnimators();
        openAnimator = ValueAnimator.ofFloat(openProgress, 0f);
        openAnimator.setDuration(120);
        openAnimator.setInterpolator(FAST_OUT_SLOW_IN);
        openAnimator.addUpdateListener(a -> {
            openProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        openAnimator.start();
    }

    private void cancelAnimators() {
        if (openAnimator != null) openAnimator.cancel();
        if (hoverAnimator != null) hoverAnimator.cancel();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Re-resolve the skin so a change made since the wheel was last shown (a new import, an
        // applied pack, or the game's own skin once a provider is installed) appears immediately.
        refreshSkin();
        // Rebuild the enabled-module list so a toggle made while the wheel was closed shows now.
        rebuildEnabledList();
        // The head is deliberately NOT animated on a timer: it turns only when the player moves the
        // cursor, so the hub reads as a still portrait until it is interacted with. Reset the look
        // to forward-facing so a previous session leaves no stare. Nothing schedules a frame here,
        // which also means the view burns no frames while it sits open.
        headYawDeg = 0f;
        headPitchDeg = 0f;
        headLastMs = android.os.SystemClock.uptimeMillis();
        playOpen();
    }

    /**
     * Re-resolves the player's skin and rebuilds the hub head.
     *
     * <p>Called when the wheel is shown and when a skin change is detected, so the head in the
     * centre tracks the player's real skin without recreating the view. It reads through
     * {@link PlayerSkinProvider}, which prefers the game's live skin when a
     * {@link org.chimeramc.client.core.cosmetics.GameSkinSource} provider is installed.
     */
    public void refreshSkin() {
        resolveSkinHead();
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelAnimators();
    }

    // --- Geometry helpers (shared by draw and touch) ----------------------------------------

    private float centerX() {
        return getWidth() / 2f;
    }

    private float centerY() {
        return getHeight() / 2f;
    }

    private float outerRadius() {
        return RadialWheelLayout.outerRadius(getWidth(), getHeight());
    }

    private float ringInnerRadius() {
        return RadialWheelLayout.ringInnerRadius(outerRadius());
    }

    private float hubRadius() {
        return RadialWheelLayout.hubRadius(outerRadius());
    }

    // --- Draw ---------------------------------------------------------------------------------

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() <= 0 || getHeight() <= 0) return;
        float cx = centerX();
        float cy = centerY();
        float outer = outerRadius() * (0.85f + 0.15f * openProgress);
        float inner = RadialWheelLayout.ringInnerRadius(outer);
        float hub = RadialWheelLayout.hubRadius(outer);
        int alpha = (int) (255 * Math.min(1f, openProgress * 1.4f));

        // Scrim. A soft translucent wash so the game behind stays visible but the wheel reads.
        // Deliberately NOT an opaque layer: an opaque white/grey scrim was what left a pale film
        // over the game after the wheel closed. The alpha is tied to openProgress, so at
        // openProgress 0 nothing is painted at all.
        int scrimAlpha = (int) (((UiTokens.WHEEL_SCRIM >>> 24) & 0xFF) * openProgress);
        if (scrimAlpha > 0) {
            canvas.drawColor(UiTokens.withAlpha(UiTokens.WHEEL_SCRIM, scrimAlpha));
        }

        // Sectors.
        if (activeProfile != null) {
            drawProfileSectors(canvas, cx, cy, inner, outer, alpha);
        } else {
            for (int i = 0; i < SECTORS.length; i++) {
                Sector s = SECTORS[i];
                float center = (float) RadialWheelLayout.sectorCenterDegrees(s);
                // Per-sector stagger: each starts 20ms after the previous.
                float local = Math.max(0f, Math.min(1f, (openProgress * 180f - i * 20f) / 120f));
                float scale = sectorScale[i] * (0.9f + 0.1f * local);
                float rOuter = outer * scale;
                float rInner = inner * scale;
                drawSector(canvas, cx, cy, rInner, rOuter, center - 45f, 90f,
                        s == highlighted, alpha, local);
            }
        }

        // Hub (the player's head only).
        drawHub(canvas, cx, cy, hub, alpha);

        // Icons + labels.
        if (activeProfile != null) {
            drawProfileContent(canvas, cx, cy, (inner + outer) / 2f, alpha);
        } else {
            drawSectorContent(canvas, cx, cy, (inner + outer) / 2f, alpha);
        }

        // The enabled-module list (right side) and the controller cursor.
        drawEnabledList(canvas, getWidth(), getHeight(), alpha);
        drawControllerCursor(canvas, alpha);
    }

    /** Draws the controller cursor as a clean pointer arrow with a small shadow. */
    private void drawControllerCursor(Canvas canvas, int alpha) {
        if (!isCursorVisible()) return;
        Paint shadow = iconPaint;
        Paint arrow = chipPaint;
        UiDraw.pointerCursor(canvas, shadow, arrow, cursor.x(), cursor.y(), density);
    }

    private void drawSector(Canvas canvas, float cx, float cy, float rInner, float rOuter,
                            float startDeg, float sweepDeg, boolean hover, int alpha,
                            float localProgress) {
        float gapPx = RadialWheelLayout.GAP_DP * density;
        // Parallel-sided gap: inset the angular span by the half-gap at the mid radius.
        float midR = (rInner + rOuter) / 2f;
        float halfGapDeg = (float) RadialWheelLayout.angularHalfGapDegrees(midR, gapPx);
        float corner = RadialWheelLayout.SECTOR_CORNER_DP * density;
        sectorPath.reset();
        buildRoundedAnnularSector(sectorPath, cx, cy, rInner, rOuter,
                startDeg + halfGapDeg, sweepDeg - 2f * halfGapDeg, corner);

        int fill = hover ? UiTokens.WHEEL_SEGMENT_HOVER : UiTokens.WHEEL_SEGMENT_BASE;
        sectorPaint.setStyle(Paint.Style.FILL);
        sectorPaint.setColor(UiTokens.withAlpha(fill,
                (int) (((fill >>> 24) & 0xFF) * localProgress)));
        canvas.drawPath(sectorPath, sectorPaint);

        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setStrokeWidth(Math.max(1f, density));
        rimPaint.setColor(UiTokens.withAlpha(UiTokens.WHEEL_SEGMENT_RIM,
                (int) (0x24 * localProgress)));
        canvas.drawPath(sectorPath, rimPaint);
    }

    // --- Profile mode drawing -----------------------------------------------------------------

    /**
     * Draws the open profile's sectors.
     *
     * <p>The sector count follows the entry count (one to {@link WheelLayout#MAX_SECTORS}), and each
     * sector takes the entry's own colour when it has one, so a profile with four entries looks like
     * the fixed wheel while an eight-entry one is visibly denser. A single entry is drawn as a full
     * ring, matching {@link WheelLayout#sectorAt}'s "one sector owns the whole ring" rule.
     */
    private void drawProfileSectors(Canvas canvas, float cx, float cy, float inner, float outer,
                                    int alpha) {
        WheelEntry.Profile profile = currentProfile();
        if (profile == null) return;
        List<WheelEntry> entries = profile.safeEntries();
        int count = WheelLayout.clampSectorCount(entries.size());
        if (entries.isEmpty()) return;

        if (count == 1) {
            // A one-entry wheel is a ring: draw the annulus with no seam.
            drawRingSector(canvas, cx, cy, inner, outer, entries.get(0), 0 == highlightedEntry,
                    alpha, 1f);
            return;
        }

        double span = WheelLayout.sectorSpanDegrees(count);
        for (int i = 0; i < entries.size() && i < count; i++) {
            float local = Math.max(0f, Math.min(1f, (openProgress * 180f - i * 20f) / 120f));
            float scale = 0.9f + 0.1f * local;
            float start = (float) (WheelLayout.sectorCenterDegrees(i, count) - span / 2.0);
            drawSectorColored(canvas, cx, cy, inner * scale, outer * scale, start, (float) span,
                    i == highlightedEntry, alpha, local, entries.get(i).effectiveColor(accent));
        }
    }

    /** Draws a profile sector with a per-entry fill colour (the four-destination path's variant). */
    private void drawSectorColored(Canvas canvas, float cx, float cy, float rInner, float rOuter,
                                   float startDeg, float sweepDeg, boolean hover, int alpha,
                                   float localProgress, int entryColor) {
        float gapPx = WheelLayout.GAP_DP * density;
        float midR = (rInner + rOuter) / 2f;
        float halfGapDeg = (float) WheelLayout.angularHalfGapDegrees(midR, gapPx);
        float corner = RadialWheelLayout.SECTOR_CORNER_DP * density;
        sectorPath.reset();
        buildRoundedAnnularSector(sectorPath, cx, cy, rInner, rOuter,
                startDeg + halfGapDeg, sweepDeg - 2f * halfGapDeg, corner);

        int base = hover ? UiTokens.WHEEL_SEGMENT_HOVER : UiTokens.WHEEL_SEGMENT_BASE;
        // Blend the entry's colour into the glass fill so a coloured sector still reads as glass.
        int fill = blend(UiTokens.withAlpha(entryColor, 0x66), base);
        sectorPaint.setStyle(Paint.Style.FILL);
        sectorPaint.setColor(UiTokens.withAlpha(fill,
                (int) (((fill >>> 24) & 0xFF) * localProgress)));
        canvas.drawPath(sectorPath, sectorPaint);

        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setStrokeWidth(Math.max(1f, density));
        rimPaint.setColor(UiTokens.withAlpha(hover ? entryColor : UiTokens.WHEEL_SEGMENT_RIM,
                (int) ((hover ? 0xAA : 0x24) * localProgress)));
        canvas.drawPath(sectorPath, rimPaint);
    }

    /** Draws the single-entry wheel as one full ring. */
    private void drawRingSector(Canvas canvas, float cx, float cy, float rInner, float rOuter,
                                WheelEntry entry, boolean hover, int alpha, float localProgress) {
        float corner = RadialWheelLayout.SECTOR_CORNER_DP * density;
        sectorPath.reset();
        buildRoundedAnnularSector(sectorPath, cx, cy, rInner, rOuter, 0f, 360f, corner);
        int base = hover ? UiTokens.WHEEL_SEGMENT_HOVER : UiTokens.WHEEL_SEGMENT_BASE;
        int fill = blend(UiTokens.withAlpha(entry.effectiveColor(accent), 0x66), base);
        sectorPaint.setStyle(Paint.Style.FILL);
        sectorPaint.setColor(UiTokens.withAlpha(fill,
                (int) (((fill >>> 24) & 0xFF) * localProgress)));
        canvas.drawPath(sectorPath, sectorPaint);
    }

    /** Draws each profile sector's label in its own colour, at the sector's centre. */
    private void drawProfileContent(Canvas canvas, float cx, float cy, float radius, int alpha) {
        WheelEntry.Profile profile = currentProfile();
        if (profile == null) return;
        List<WheelEntry> entries = profile.safeEntries();
        int count = WheelLayout.clampSectorCount(entries.size());
        if (entries.isEmpty()) return;

        float labelSize = UiTokens.TEXT_CAPTION * density;
        labelPaint.setTextSize(labelSize);
        for (int i = 0; i < entries.size() && i < count; i++) {
            double a = Math.toRadians(WheelLayout.sectorCenterDegrees(i, count));
            float px = cx + (float) (Math.cos(a) * radius);
            float py = cy + (float) (Math.sin(a) * radius);
            // A sub-wheel entry gets a chevron instead of an icon so "this opens another wheel" is
            // visible before it is selected.
            UiIcons.Icon icon = entries.get(i).isSubWheel() ? UiIcons.Icon.CHEVRON : iconFor(entries.get(i));
            UiIcons.draw(canvas, iconPaint, icon, px, py - labelSize * 0.6f, 24f * density,
                    UiTokens.withAlpha(entries.get(i).effectiveColor(accent), alpha), density);
            labelPaint.setColor(UiTokens.withAlpha(UiTokens.TEXT_PRIMARY, alpha));
            canvas.drawText(entries.get(i).label, px, py + labelSize * 0.9f, labelPaint);
        }
        // The open profile's name, so a nested wheel says where it is.
        if (profile.name != null && !profile.name.isEmpty() && !navigation.atRoot()) {
            labelPaint.setTextSize(UiTokens.TEXT_CAPTION * density);
            labelPaint.setColor(UiTokens.withAlpha(UiTokens.TEXT_SECONDARY, alpha));
            canvas.drawText(profile.name, cx, cy + RadialWheelLayout.hubRadius(outerRadius()) * 1.5f,
                    labelPaint);
        }
    }

    /** The icon for a profile entry, by action. */
    private static UiIcons.Icon iconFor(WheelEntry entry) {
        if (entry == null) return UiIcons.Icon.CUBE;
        switch (entry.action) {
            case OPEN_MODS: return UiIcons.Icon.CUBE;
            case OPEN_CLOSET: return UiIcons.Icon.HANGER;
            case OPEN_SETTINGS: return UiIcons.Icon.GEAR;
            case OPEN_HUD: return UiIcons.Icon.PENCIL;
            case OPEN_SUBWHEEL: return UiIcons.Icon.CHEVRON;
            default: return UiIcons.Icon.CUBE;
        }
    }

    /** Alpha-blends {@code over} onto {@code under} using the over's alpha. */
    private static int blend(int over, int under) {
        float a = ((over >>> 24) & 0xFF) / 255f;
        int r = (int) (((over >> 16) & 0xFF) * a + ((under >> 16) & 0xFF) * (1 - a));
        int g = (int) (((over >> 8) & 0xFF) * a + ((under >> 8) & 0xFF) * (1 - a));
        int b = (int) ((over & 0xFF) * a + (under & 0xFF) * (1 - a));
        int outA = Math.max((over >>> 24) & 0xFF, (under >>> 24) & 0xFF);
        return (outA << 24) | (r << 16) | (g << 8) | b;
    }

    /** Builds a rounded annular sector as one closed path (outer arc, fillet, inner arc, fillet). */
    private void buildRoundedAnnularSector(Path path, float cx, float cy, float rInner,
                                           float rOuter, float startDeg, float sweepDeg,
                                           float corner) {
        float r = Math.min(corner, Math.min((rOuter - rInner) / 2f, Math.abs(sweepDeg) / 4f));
        float oInset = (float) Math.toDegrees(r / Math.max(1f, rOuter));
        float iInset = (float) Math.toDegrees(r / Math.max(1f, rInner));
        float a0 = startDeg;
        float a1 = startDeg + sweepDeg;

        path.moveTo(polarX(cx, rOuter, a0 + oInset), polarY(cy, rOuter, a0 + oInset));
        arc(path, cx, cy, rOuter, a0 + oInset, a1 - oInset);
        // Corner fillet at the far end (outer -> inner).
        float ix = polarX(cx, rInner, a1 - iInset), iy = polarY(cy, rInner, a1 - iInset);
        path.quadTo(polarX(cx, (rInner + rOuter) / 2f, a1), polarY(cy, (rInner + rOuter) / 2f, a1),
                ix, iy);
        arc(path, cx, cy, rInner, a1 - iInset, a0 + iInset);
        float ox0 = polarX(cx, rOuter, a0 + oInset), oy0 = polarY(cy, rOuter, a0 + oInset);
        path.quadTo(polarX(cx, (rInner + rOuter) / 2f, a0), polarY(cy, (rInner + rOuter) / 2f, a0),
                ox0, oy0);
        path.close();
    }

    private static void arc(Path path, float cx, float cy, float radius, float from, float to) {
        RectF oval = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
        path.arcTo(oval, from, to - from);
    }

    private static float polarX(float cx, float radius, float deg) {
        return cx + (float) (Math.cos(Math.toRadians(deg)) * radius);
    }

    private static float polarY(float cy, float radius, float deg) {
        return cy + (float) (Math.sin(Math.toRadians(deg)) * radius);
    }

    private void drawHub(Canvas canvas, float cx, float cy, float radius, int alpha) {
        hubPaint.setStyle(Paint.Style.FILL);
        hubPaint.setColor(UiTokens.withAlpha(UiTokens.WHEEL_HUB_FILL,
                (int) (((UiTokens.WHEEL_HUB_FILL >>> 24) & 0xFF) * (alpha / 255f))));
        canvas.drawCircle(cx, cy, radius, hubPaint);
        drawHead(canvas, cx, cy, radius);
    }

    private void drawSectorContent(Canvas canvas, float cx, float cy, float radius, int alpha) {
        float iconSize = 24f * density;
        float labelSize = UiTokens.TEXT_CAPTION * density;
        labelPaint.setTextSize(labelSize);
        // Centre the icon+label block vertically in the ring band: the block is iconSize tall plus a
        // small gap plus the label, so its centre is the band radius and both halves sit around it.
        float gap = 4f * density;
        float blockHeight = iconSize + gap + labelSize;
        float blockTop = -blockHeight / 2f;
        float iconCenterY = blockTop + iconSize / 2f;
        float labelBaselineY = blockTop + iconSize + gap + labelSize * 0.8f;
        for (Sector s : SECTORS) {
            double a = Math.toRadians(RadialWheelLayout.sectorCenterDegrees(s));
            float px = cx + (float) (Math.cos(a) * radius);
            float py = cy + (float) (Math.sin(a) * radius);
            UiIcons.draw(canvas, iconPaint, iconFor(s), px, py + iconCenterY, iconSize,
                    UiTokens.withAlpha(UiTokens.TEXT_PRIMARY, alpha), density);
            labelPaint.setColor(UiTokens.withAlpha(UiTokens.TEXT_PRIMARY, alpha));
            canvas.drawText(sectorLabel(s), px, py + labelBaselineY, labelPaint);
        }
    }

    private static UiIcons.Icon iconFor(Sector s) {
        switch (s) {
            case MODS: return UiIcons.Icon.CUBE;
            case HUD: return UiIcons.Icon.PENCIL;
            case CLOSET: return UiIcons.Icon.HANGER;
            case SETTINGS: return UiIcons.Icon.GEAR;
            default: return UiIcons.Icon.CUBE;
        }
    }

    private void drawHead(Canvas canvas, float cx, float cy, float radius) {
        if (headScene != null && headAtlas != null) {
            if (drawHead3D(canvas, cx, cy, radius)) return;
        }
        if (skinHead == null || skinHead.isRecycled()) {
            // No skin available: draw a neutral accent disc rather than a glyph or emoji.
            hubPaint.setColor(UiTokens.ACCENT_900);
            canvas.drawCircle(cx, cy, radius * 0.8f, hubPaint);
            return;
        }
        dstHead.set(cx - radius * 0.72f, cy - radius * 0.72f,
                cx + radius * 0.72f, cy + radius * 0.72f);
        headClip.reset();
        headClip.addCircle(cx, cy, radius * 0.9f, Path.Direction.CW);
        int save = canvas.save();
        canvas.clipPath(headClip);
        headPaint.setFilterBitmap(false);
        canvas.drawBitmap(skinHead, srcHead, dstHead, headPaint);
        canvas.restoreToCount(save);
    }

    /** Renders the 3D head turntable into the hub; false when it cannot allocate a bitmap. */
    private boolean drawHead3D(Canvas canvas, float cx, float cy, float radius) {
        int size = Math.max(24, (int) (radius * 1.7f));
        if (headBitmap == null || headBitmap.getWidth() != size) {
            try {
                headBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            } catch (Throwable t) {
                headBitmap = null;
                return false;
            }
        }
        headRaster.prepare(size, size, 0x00000000);
        // The scene holds ONLY the head box (see resolveSkinHead), so the camera targets the head's
        // own centre (model y = 28 for the 8x8x8 head whose bottom sits at 24) rather than the chest
        // of a bust. A tight margin then fills the disc with the face instead of leaving the head
        // adrift in a large empty circle.
        SkinModel.Camera cam = new SkinModel.Camera(headYawDeg, headPitchDeg,
                SkinModel.CAMERA_DISTANCE, 0f, 28f, 0f);
        PreviewScene.Fit fit = headScene.fit(cam, size, size, 0.06f);
        headScene.render(headRaster, cam, fit);
        headBitmap.setPixels(headRaster.pixels(), 0, size, 0, 0, size, size);
        headClip.reset();
        headClip.addCircle(cx, cy, radius * 0.94f, Path.Direction.CW);
        int save = canvas.save();
        canvas.clipPath(headClip);
        headPaint.setFilterBitmap(true);
        dstHead.set(cx - radius * 0.94f, cy - radius * 0.94f,
                cx + radius * 0.94f, cy + radius * 0.94f);
        canvas.drawBitmap(headBitmap, null, dstHead, headPaint);
        canvas.restoreToCount(save);
        return true;
    }

    /**
     * Draws both module columns.
     *
     * <p>Left: the quick options the player can turn on, drawn left-anchored. Right: the modules that
     * are actually enabled, right-anchored. Both are compact columns (one row pitch apart), so they
     * read as grouped lists rather than the old widely-spaced clusters. A row's status dot shows
     * whether its module is currently on.
     */
    private void drawEnabledList(Canvas canvas, int w, int h, int alpha) {
        chipTextPaint.setTextSize(UiChip.LABEL_SP * density);
        if (!quickChips.isEmpty()) {
            float[][] centers = RadialWheelLayout.moduleListCenters(
                    w, h, quickChips.size(), density, false);
            for (int i = 0; i < quickChips.size() && i < centers.length; i++) {
                Chip chip = quickChips.get(i);
                float labelWidth = chipTextPaint.measureText(chip.label);
                boolean on = chipToggle != null && chipToggle.isChipOn(chip.moduleId);
                UiChip.draw(canvas, chipPaint, chipTextPaint, centers[i][0], centers[i][1],
                        labelWidth, chip.label, UiTokens.withAlpha(accent, alpha), on, density,
                        UiChip.Align.CENTER);
            }
        }
        if (!chips.isEmpty()) {
            float[][] centers = RadialWheelLayout.moduleListCenters(
                    w, h, chips.size(), density, true);
            for (int i = 0; i < chips.size() && i < centers.length; i++) {
                Chip chip = chips.get(i);
                float labelWidth = chipTextPaint.measureText(chip.label);
                boolean on = chipToggle != null && chipToggle.isChipOn(chip.moduleId);
                UiChip.draw(canvas, chipPaint, chipTextPaint, centers[i][0], centers[i][1],
                        labelWidth, chip.label, UiTokens.withAlpha(accent, alpha), on, density,
                        UiChip.Align.RIGHT);
            }
        }
    }

    /**
     * The (list, row) hit under a point, or null.
     *
     * <p>Both columns are hit-tested so a tap selects whichever row is under it; the left column is
     * left-anchored, so its hit box hugs the chip's left edge.
     */
    int[] chipHit(float x, float y, int w, int h) {
        if (chipAtIn(quickChips, x, y, w, h, false) >= 0) {
            return new int[] {0, chipAtIn(quickChips, x, y, w, h, false)};
        }
        int right = chipAtIn(chips, x, y, w, h, true);
        return right >= 0 ? new int[] {1, right} : null;
    }

    private int chipAtIn(List<Chip> list, float x, float y, int w, int h, boolean fromRight) {
        if (list.isEmpty()) return -1;
        float[][] centers = RadialWheelLayout.moduleListCenters(w, h, list.size(), density, fromRight);
        chipTextPaint.setTextSize(UiChip.LABEL_SP * density);
        for (int i = 0; i < list.size() && i < centers.length; i++) {
            float cw = UiChip.width(chipTextPaint.measureText(list.get(i).label), density);
            float ch = UiChip.HEIGHT_DP * density;
            float left = fromRight ? centers[i][0] - cw : centers[i][0] - cw / 2f;
            float right = fromRight ? centers[i][0] : centers[i][0] + cw / 2f;
            if (x >= left && x <= right && Math.abs(y - centers[i][1]) <= ch / 2f) {
                return i;
            }
        }
        return -1;
    }

    /** The right-column index under a point, or -1 (kept for the controller cursor's highlight). */
    int chipAt(float x, float y, int w, int h) {
        return chipAtIn(chips, x, y, w, h, true);
    }

    private static String sectorLabel(Sector s) {
        switch (s) {
            case MODS: return "MODS";
            case CLOSET: return "CLOSET";
            case SETTINGS: return "SETTINGS";
            case HUD: return "HUD";
            default: return "";
        }
    }

    // --- Controller cursor ------------------------------------------------------------------

    /**
     * Enables controller-cursor mode: a cursor appears at the hub and the stick drives it.
     * Called when the wheel is opened by the controller bind.
     *
     * <p>The wheel is enabled from a post on the overlay window, which can run before the view has
     * been measured. Creating the cursor with a 1x1 viewport there clamped it to a single pixel and
     * it could never move — the "controller cursor does not work" report. The viewport is therefore
     * kept in step with the real size in {@link #onSizeChanged}, and the cursor is centred the first
     * time a usable size arrives rather than being pinned at the top-left corner.
     */
    public void enableControllerCursor() {
        cursorVisible = true;
        applyCursorViewport(true);
        invalidate();
    }

    /**
     * Creates or updates the cursor for the current view size.
     *
     * @param centreNow true to recentre the cursor (a fresh open); false to only fix the viewport
     *                  (a rotation/resize must not yank the cursor back to the middle)
     */
    private void applyCursorViewport(boolean centreNow) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return; // Not laid out yet; onSizeChanged will run.
        if (cursor == null) {
            cursor = new CursorController(w, h, density);
        } else {
            cursor.setViewport(w, h);
            if (centreNow) cursor.centre();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // The first real size arrives after the overlay's post; without this the cursor kept the
        // 1x1 viewport it was created with and never moved.
        if (cursor != null) {
            cursor.setViewport(Math.max(1, w), Math.max(1, h));
            if (oldw == 0 || oldh == 0) cursor.centre();
        }
        invalidate();
    }

    /** True when the view is laid out far enough for the cursor to move meaningfully. */
    private boolean cursorViewportUsable() {
        return getWidth() > 1 && getHeight() > 1;
    }

    /** Hides the cursor (a touch, or the wheel closing). */
    public void disableControllerCursor() {
        cursorVisible = false;
        cursorChip = -1;
        invalidate();
    }

    public boolean isCursorVisible() {
        return cursorVisible && cursor != null;
    }

    /**
     * Feeds one frame of controller input.
     *
     * @return true when the wheel consumed it (so the caller must not forward it to the game)
     */
    public boolean handleControllerStick(float rawX, float rawY, float dt, boolean precision,
                                         boolean fast, float deadZone, float curveExponent) {
        // A stick frame can arrive before the overlay's post enabled the cursor (or before layout),
        // so create it here too rather than returning early — the earlier early-return is part of
        // why the cursor never appeared.
        if (cursor == null) {
            applyCursorViewport(true);
            if (cursor == null) return false; // Still not laid out; nothing sensible to move.
        }
        cursorVisible = true;
        float[] shaped = new float[2];
        CursorController.shape(rawX, rawY, deadZone, curveExponent, shaped);
        // Android's stick Y is negative when pushed up, which matches screen Y growing down (an up
        // push must reduce the cursor's y). Pass the shaped axes through unchanged.
        cursor.setMagnetTarget(false, 0f, 0f);
        cursor.update(shaped[0], shaped[1], dt, precision, fast,
                CursorController.DEFAULT_MAX_SPEED_DP);
        // The head looks at the cursor, not at the stick vector: the hub stays a still portrait
        // until the player moves the cursor, then the head turns to face where the cursor now is.
        // The same rule runs for touch (see onTouchEvent), so both inputs turn it consistently.
        updateHeadLook(cursor.x(), cursor.y(), dt);
        // Magnetism: ease toward the sector icon or chip under the cursor.
        applyCursorMagnetism();
        updateCursorHighlight();
        invalidate();
        return true;
    }

    /**
     * Points the hub head at the cursor's current position, eased by one frame.
     *
     * <p>Called from every input path (the right stick and touch), so the head is still until the
     * player moves the cursor, and then turns to face where the cursor is — it never keeps spinning
     * after the cursor stops. Yaw/pitch come from {@link RadialHeadTurn#targetLook} on the cursor's
     * offset from the hub centre, so the exact position (not a velocity) drives the look.
     *
     * @param cursorX   cursor/screen x within the view, pixels
     * @param cursorY   cursor/screen y within the view, pixels
     * @param dtSeconds elapsed time for this input frame (clamped by the caller)
     */
    void updateHeadLook(float cursorX, float cursorY, float dtSeconds) {
        float cx = centerX();
        float cy = centerY();
        float maxH = hubRadius() * 0.9f;
        float maxV = hubRadius() * 0.9f;
        float[] target = RadialHeadTurn.targetLook(cursorX - cx, cursorY - cy, maxH, maxV);
        float[] eased = RadialHeadTurn.stepTowards(
                headYawDeg, headPitchDeg, target[0], target[1], dtSeconds);
        headYawDeg = eased[0];
        headPitchDeg = eased[1];
        invalidate();
    }

    private void applyCursorMagnetism() {
        float cx = centerX();
        float cy = centerY();
        float outer = outerRadius();
        float inner = RadialWheelLayout.ringInnerRadius(outer);
        float radius = (inner + outer) / 2f;
        if (activeProfile != null) {
            int entry = profileEntryAt(cursor.x(), cursor.y());
            if (entry >= 0) {
                int count = WheelLayout.clampSectorCount(currentProfile().safeEntries().size());
                double a = Math.toRadians(WheelLayout.sectorCenterDegrees(entry, count));
                cursor.setMagnetTarget(true, cx + (float) (Math.cos(a) * radius),
                        cy + (float) (Math.sin(a) * radius));
            } else {
                cursor.setMagnetTarget(false, 0f, 0f);
            }
            return;
        }
        Sector s = RadialWheelLayout.sectorAtLinear(
                cursor.x() - cx, cursor.y() - cy, inner, outer, RadialWheelLayout.GAP_DP * density);
        if (RadialWheelLayout.isDestination(s)) {
            double a = Math.toRadians(RadialWheelLayout.sectorCenterDegrees(s));
            float ix = cx + (float) (Math.cos(a) * radius);
            float iy = cy + (float) (Math.sin(a) * radius);
            cursor.setMagnetTarget(true, ix, iy);
        } else {
            cursor.setMagnetTarget(false, 0f, 0f);
        }
    }

    private void updateCursorHighlight() {
        float cx = centerX();
        float cy = centerY();
        float outer = outerRadius();
        float inner = RadialWheelLayout.ringInnerRadius(outer);
        if (activeProfile != null) {
            highlightedEntry = profileEntryAt(cursor.x(), cursor.y());
        } else {
            Sector s = RadialWheelLayout.sectorAtLinear(
                    cursor.x() - cx, cursor.y() - cy, inner, outer,
                    RadialWheelLayout.GAP_DP * density);
            setHighlight(s);
        }
        int[] hit = chipHit(cursor.x(), cursor.y(), getWidth(), getHeight());
        cursorChip = hit != null && hit[0] == 1 ? hit[1] : -1;
    }

    /**
     * The SELECT button: activates the chip under the cursor if there is one, else the sector.
     *
     * @return true when something was activated
     */
    public boolean controllerSelect() {
        if (cursor == null) return false;
        int[] hit = chipHit(cursor.x(), cursor.y(), getWidth(), getHeight());
        if (hit != null) {
            List<Chip> list = hit[0] == 0 ? quickChips : chips;
            int index = hit[1];
            if (index >= 0 && index < list.size()) {
                if (chipToggle != null) chipToggle.onChipToggled(list.get(index).moduleId);
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                // The right column is the enabled set, so a toggle must rebuild it immediately.
                rebuildEnabledList();
                cursorChip = -1;
                invalidate();
                return true;
            }
        }
        if (activeProfile != null) {
            int entry = profileEntryAt(cursor.x(), cursor.y());
            if (entry >= 0) {
                activateProfileEntry(entry);
                return true;
            }
            return false;
        }
        float cx = centerX();
        float cy = centerY();
        float outer = outerRadius();
        float inner = RadialWheelLayout.ringInnerRadius(outer);
        Sector s = RadialWheelLayout.sectorAtLinear(
                cursor.x() - cx, cursor.y() - cy, inner, outer, RadialWheelLayout.GAP_DP * density);
        if (listener != null && RadialWheelLayout.isDestination(s)) {
            listener.onSector(s);
            return true;
        }
        return false;
    }

    /**
     * The BACK button.
     *
     * <p>In profile mode this backs out of a sub-wheel first, and only dismisses at the root — the
     * same "Back leaves the current level" rule the launcher's own screens use.
     */
    public boolean controllerBack() {
        if (activeProfile != null && backOut()) {
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            return true;
        }
        if (entryListener != null && activeProfile != null) {
            entryListener.onBack();
            return true;
        }
        if (listener != null) {
            listener.onDismiss();
            return true;
        }
        return false;
    }

    /** The PIN button: pin/unpin the hovered chip's module. Returns true when a chip was hit. */
    public boolean controllerPinHoveredChip() {
        int[] hit = cursor == null ? null : chipHit(cursor.x(), cursor.y(), getWidth(), getHeight());
        if (hit != null) {
            List<Chip> list = hit[0] == 0 ? quickChips : chips;
            int index = hit[1];
            if (index >= 0 && index < list.size()) {
                if (chipToggle != null) chipToggle.onChipToggled(list.get(index).moduleId);
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                rebuildEnabledList();
                cursorChip = -1;
                invalidate();
                return true;
            }
        }
        return false;
    }

    /** Moves the cursor to a sector's icon (D-pad focus mode). Hides the free cursor. */
    public void focusSector(Sector s) {
        if (!RadialWheelLayout.isDestination(s)) return;
        float cx = centerX();
        float cy = centerY();
        float outer = outerRadius();
        float inner = RadialWheelLayout.ringInnerRadius(outer);
        float radius = (inner + outer) / 2f;
        double a = Math.toRadians(RadialWheelLayout.sectorCenterDegrees(s));
        if (cursor == null) {
            cursor = new CursorController(Math.max(1, getWidth()), Math.max(1, getHeight()), density);
        }
        cursor.setPosition(cx + (float) (Math.cos(a) * radius),
                cy + (float) (Math.sin(a) * radius));
        cursorVisible = false;
        setHighlight(s);
        invalidate();
    }

    /** The D-pad left/right: step through the sectors (or the open profile's entries). */
    public void focusNextSector(int direction) {
        if (activeProfile != null) {
            WheelEntry.Profile profile = currentProfile();
            if (profile == null || profile.safeEntries().isEmpty()) return;
            int count = WheelLayout.clampSectorCount(profile.safeEntries().size());
            int current = highlightedEntry < 0 ? 0 : highlightedEntry;
            highlightedEntry = ((current + direction) % count + count) % count;
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            invalidate();
            return;
        }
        Sector[] order = {Sector.MODS, Sector.CLOSET, Sector.SETTINGS, Sector.HUD};
        Sector current = highlighted;
        int idx = 0;
        for (int i = 0; i < order.length; i++) if (order[i] == current) idx = i;
        int next = ((idx + direction) % order.length + order.length) % order.length;
        focusSector(order[next]);
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
    }

    /** The LB/RB: cycle the chips, selecting the module under the cursor. */
    public void cycleChip(int direction) {
        if (chips.isEmpty()) return;
        int next = cursorChip < 0 ? 0 : (cursorChip + direction + chips.size()) % chips.size();
        cursorChip = next;
        invalidate();
    }

    // --- Touch ------------------------------------------------------------------------------

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // A touch hides the controller cursor: the two input methods never fight.
        if (cursorVisible) {
            disableControllerCursor();
        }
        float cx = centerX();
        float cy = centerY();
        float outer = outerRadius();
        float inner = ringInnerRadius();
        float gapPx = RadialWheelLayout.GAP_DP * density;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (handleChipTap(event.getX(), event.getY(), getWidth(), getHeight())) {
                    return true;
                }
                if (activeProfile != null) {
                    highlightedEntry = profileEntryAt(event.getX(), event.getY());
                } else {
                    setHighlight(RadialWheelLayout.sectorAtLinear(
                            event.getX() - cx, event.getY() - cy, inner, outer, gapPx));
                }
                // Touch turns the head the same way the stick does: look at the pointer.
                updateHeadLook(event.getX(), event.getY(), 1f / 60f);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (activeProfile != null) {
                    highlightedEntry = profileEntryAt(event.getX(), event.getY());
                    invalidate();
                } else {
                    setHighlight(RadialWheelLayout.sectorAtLinear(
                            event.getX() - cx, event.getY() - cy, inner, outer, gapPx));
                }
                updateHeadLook(event.getX(), event.getY(), 1f / 60f);
                return true;
            case MotionEvent.ACTION_UP:
                if (activeProfile != null) {
                    int hit = profileEntryAt(event.getX(), event.getY());
                    highlightedEntry = -1;
                    invalidate();
                    if (hit >= 0) {
                        activateProfileEntry(hit);
                    } else if (listener != null) {
                        listener.onDismiss();
                    }
                    return true;
                }
                Sector s = RadialWheelLayout.sectorAtLinear(
                        event.getX() - cx, event.getY() - cy, inner, outer, gapPx);
                highlighted = Sector.NONE;
                animateHover(null);
                invalidate();
                if (listener != null) {
                    if (RadialWheelLayout.isDestination(s)) {
                        listener.onSector(s);
                    } else {
                        listener.onDismiss();
                    }
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                highlighted = Sector.NONE;
                highlightedEntry = -1;
                invalidate();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    /** The profile-entry index under a point, or -1. */
    private int profileEntryAt(float x, float y) {
        WheelEntry.Profile profile = currentProfile();
        if (profile == null) return -1;
        int count = WheelLayout.clampSectorCount(profile.safeEntries().size());
        if (profile.safeEntries().isEmpty()) return -1;
        float outer = outerRadius();
        float inner = ringInnerRadius();
        return WheelLayout.sectorAt(x - centerX(), y - centerY(), inner, outer,
                WheelLayout.GAP_DP * density, count);
    }

    /**
     * Performs a profile entry's action.
     *
     * <p>A sub-wheel is opened in place (the wheel is navigation, not a destination); anything else
     * is handed to the {@link EntryListener}, which owns the launcher-side routing. A sub-wheel
     * entry whose target no longer resolves is refused rather than silently doing nothing, so the
     * caller can dismiss.
     */
    private void activateProfileEntry(int index) {
        WheelEntry.Profile profile = currentProfile();
        if (profile == null) return;
        List<WheelEntry> entries = profile.safeEntries();
        if (index < 0 || index >= entries.size()) return;
        WheelEntry entry = entries.get(index);
        if (entry.isSubWheel() && openSubWheel(entry.target)) {
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            return;
        }
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        if (entryListener != null) entryListener.onEntry(entry);
    }

    /** Toggles the module row under the pointer, if any; returns true when a row was hit. */
    private boolean handleChipTap(float x, float y, int w, int h) {
        int[] hit = chipHit(x, y, w, h);
        if (hit == null) return false;
        List<Chip> list = hit[0] == 0 ? quickChips : chips;
        int index = hit[1];
        if (index < 0 || index >= list.size()) return false;
        if (chipToggle != null) chipToggle.onChipToggled(list.get(index).moduleId);
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        // The right column reflects what is enabled, so a toggle (either direction) must rebuild it.
        rebuildEnabledList();
        invalidate();
        return true;
    }

    private void setHighlight(Sector sector) {
        if (sector == highlighted) return;
        boolean wasDestination = RadialWheelLayout.isDestination(highlighted);
        highlighted = sector;
        if (RadialWheelLayout.isDestination(sector) && !wasDestination) {
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
        animateHover(sector);
        invalidate();
    }

    /** Scales the hovered sector to 1.04 over 80ms. */
    private void animateHover(Sector sector) {
        int idx = indexOf(sector);
        if (hoverAnimator != null) hoverAnimator.cancel();
        hoverAnimator = ValueAnimator.ofFloat(0f, 1f);
        hoverAnimator.setDuration(UiTokens.MOTION_PRESS_MS);
        hoverAnimator.setInterpolator(FAST_OUT_SLOW_IN);
        final int target = idx;
        hoverAnimator.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            for (int i = 0; i < sectorScale.length; i++) {
                float goal = (i == target) ? 1.04f : 1f;
                sectorScale[i] = sectorScale[i] + (goal - sectorScale[i]) * t;
            }
            invalidate();
        });
        hoverAnimator.start();
    }

    private static int indexOf(Sector sector) {
        for (int i = 0; i < SECTORS.length; i++) if (SECTORS[i] == sector) return i;
        return -1;
    }
}
