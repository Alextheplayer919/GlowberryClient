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

    private static final Sector[] SECTORS =
            {Sector.MODS, Sector.CLOSET, Sector.SETTINGS, Sector.HUD};

    private static final class Chip {
        final String moduleId;
        final int labelRes;
        final int cluster;  // 0 top-left, 1 top-right, 2 right-middle

        Chip(String moduleId, int labelRes, int cluster) {
            this.moduleId = moduleId;
            this.labelRes = labelRes;
            this.cluster = cluster;
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

    private final List<Chip> chips = new ArrayList<>();
    private Listener listener;
    private ChipToggle chipToggle;
    private Sector highlighted = Sector.NONE;
    private int accent;

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
    private long headLastMs;
    private final android.os.Handler headHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable headTick = new Runnable() {
        @Override
        public void run() {
            long now = android.os.SystemClock.uptimeMillis();
            float dt = headLastMs == 0L ? 0f : (now - headLastMs) / 1000f;
            headLastMs = now;
            headYawDeg = (headYawDeg + 22f * Math.min(dt, 0.1f)) % 360f;
            invalidate();
            if (isAttachedToWindow()) headHandler.postDelayed(this, 40L);
        }
    };

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

    private void buildChips() {
        // Two top-left, three top-right, one right-middle — the reference cluster layout.
        chips.add(new Chip("list_players", R.string.inbuilt_mod_list_players, 0));
        chips.add(new Chip("debug_menu", R.string.inbuilt_mod_debug_menu, 0));
        chips.add(new Chip("zoom", R.string.inbuilt_mod_zoom, 1));
        chips.add(new Chip("fullbright", R.string.inbuilt_mod_fullbright, 1));
        chips.add(new Chip("time_changer", R.string.inbuilt_mod_time_changer, 1));
        chips.add(new Chip("third_person_nametag", R.string.inbuilt_mod_third_person_nametag, 2));
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
                    // Head + body = a bust, so the hub shows the character rather than a cropped
                    // head. The fit margin in drawHead3D keeps it inside the disc.
                    PreviewCharacterScene.addCharacter(headScene, headAtlas, SkinModel.ATLAS_SIZE,
                            0f, 0f);
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
        if (headScene != null) {
            headLastMs = 0L;
            headHandler.removeCallbacks(headTick);
            headHandler.post(headTick);
        }
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
        headHandler.removeCallbacks(headTick);
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

        // Scrim.
        canvas.drawColor(UiTokens.withAlpha(UiTokens.WHEEL_SCRIM,
                (int) (((UiTokens.WHEEL_SCRIM >>> 24) & 0xFF) * openProgress)));

        // Sectors.
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

        // Hub.
        drawHub(canvas, cx, cy, hub, alpha);

        // Icons + labels.
        drawSectorContent(canvas, cx, cy, (inner + outer) / 2f, alpha);

        // Chips.
        drawChips(canvas, getWidth(), getHeight(), alpha);
        drawControllerCursor(canvas, alpha);
    }

    /** Draws the controller cursor: a 10dp disc with a 2dp white ring and a soft accent glow. */
    private void drawControllerCursor(Canvas canvas, int alpha) {
        if (!isCursorVisible()) return;
        float cx = cursor.x();
        float cy = cursor.y();
        float r = 5f * density;
        // Soft glow.
        Paint glow = iconPaint;
        glow.reset();
        glow.setAntiAlias(true);
        glow.setColor(UiTokens.withAlpha(accent, (int) (0x55 * (alpha / 255f))));
        glow.setShadowLayer(6f * density, 0f, 0f,
                UiTokens.withAlpha(accent, (int) (0x99 * (alpha / 255f))));
        canvas.drawCircle(cx, cy, r, glow);
        glow.clearShadowLayer();
        // White ring, 2dp.
        glow.setStyle(Paint.Style.STROKE);
        glow.setStrokeWidth(2f * density);
        glow.setColor(UiTokens.withAlpha(0xFFFFFFFF, alpha));
        canvas.drawCircle(cx, cy, r, glow);
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
        // Orbit the bust (head + body) around the chest and fit with a wide margin, so the head is
        // never cropped at the disc edge.
        SkinModel.Camera cam = new SkinModel.Camera(headYawDeg, 8f, SkinModel.CAMERA_DISTANCE,
                0f, 24f, 0f);
        PreviewScene.Fit fit = headScene.fit(cam, size, size, 0.16f);
        headScene.render(headRaster, cam, fit);
        headBitmap.setPixels(headRaster.pixels(), 0, size, 0, 0, size, size);
        headClip.reset();
        headClip.addCircle(cx, cy, radius * 0.9f, Path.Direction.CW);
        int save = canvas.save();
        canvas.clipPath(headClip);
        headPaint.setFilterBitmap(true);
        dstHead.set(cx - radius * 0.86f, cy - radius * 0.86f,
                cx + radius * 0.86f, cy + radius * 0.86f);
        canvas.drawBitmap(headBitmap, null, dstHead, headPaint);
        canvas.restoreToCount(save);
        return true;
    }

    private void drawChips(Canvas canvas, int w, int h, int alpha) {
        int[] counts = chipClusterCounts();
        float[][] centers = RadialWheelLayout.chipCenters(w, h, counts[0], counts[1], counts[2]);
        int i = 0;
        for (Chip chip : chips) {
            float[] c = centers[i++];
            String text = getContext().getString(chip.labelRes);
            chipTextPaint.setTextSize(UiChip.LABEL_SP * density);
            float labelWidth = chipTextPaint.measureText(text);
            boolean on = chipToggle != null && chipToggle.isChipOn(chip.moduleId);
            UiChip.draw(canvas, chipPaint, chipTextPaint, c[0], c[1], labelWidth, text,
                    UiTokens.withAlpha(accent, alpha), on, density);
        }
    }

    private int[] chipClusterCounts() {
        int tl = 0, tr = 0, rm = 0;
        for (Chip chip : chips) {
            if (chip.cluster == 0) tl++;
            else if (chip.cluster == 1) tr++;
            else rm++;
        }
        return new int[]{tl, tr, rm};
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
     */
    public void enableControllerCursor() {
        if (cursor == null) {
            cursor = new CursorController(Math.max(1, getWidth()), Math.max(1, getHeight()), density);
        } else {
            cursor.setViewport(Math.max(1, getWidth()), Math.max(1, getHeight()));
            cursor.centre();
        }
        cursorVisible = true;
        invalidate();
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
        if (cursor == null) return false;
        cursorVisible = true;
        float[] shaped = new float[2];
        CursorController.shape(rawX, rawY, deadZone, curveExponent, shaped);
        // Screen Y grows down; a pad's +Y is up, so negate so pushing up moves the cursor up.
        cursor.setMagnetTarget(false, 0f, 0f);
        cursor.update(shaped[0], -shaped[1], dt, precision, fast,
                CursorController.DEFAULT_MAX_SPEED_DP);
        // Magnetism: ease toward the sector icon or chip under the cursor.
        applyCursorMagnetism();
        updateCursorHighlight();
        invalidate();
        return true;
    }

    private void applyCursorMagnetism() {
        float cx = centerX();
        float cy = centerY();
        float outer = outerRadius();
        float inner = RadialWheelLayout.ringInnerRadius(outer);
        float radius = (inner + outer) / 2f;
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
        Sector s = RadialWheelLayout.sectorAtLinear(
                cursor.x() - cx, cursor.y() - cy, inner, outer, RadialWheelLayout.GAP_DP * density);
        setHighlight(s);
        cursorChip = chipAt(cursor.x(), cursor.y(), getWidth(), getHeight());
    }

    /** The index of the chip under a point, or -1. Pure given the chip layout. */
    int chipAt(float x, float y, int w, int h) {
        int[] counts = chipClusterCounts();
        float[][] centers = RadialWheelLayout.chipCenters(w, h, counts[0], counts[1], counts[2]);
        for (int i = 0; i < chips.size() && i < centers.length; i++) {
            String text = getContext().getString(chips.get(i).labelRes);
            chipTextPaint.setTextSize(UiChip.LABEL_SP * density);
            float cw = UiChip.width(chipTextPaint.measureText(text), density);
            float ch = UiChip.HEIGHT_DP * density;
            if (Math.abs(x - centers[i][0]) <= cw / 2f && Math.abs(y - centers[i][1]) <= ch / 2f) {
                return i;
            }
        }
        return -1;
    }

    /**
     * The SELECT button: activates the chip under the cursor if there is one, else the sector.
     *
     * @return true when something was activated
     */
    public boolean controllerSelect() {
        if (cursor == null) return false;
        if (cursorChip >= 0 && cursorChip < chips.size()) {
            if (chipToggle != null) chipToggle.onChipToggled(chips.get(cursorChip).moduleId);
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            invalidate();
            return true;
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

    /** The BACK button. */
    public boolean controllerBack() {
        if (listener != null) {
            listener.onDismiss();
            return true;
        }
        return false;
    }

    /** The PIN button: pin/unpin the hovered chip's module. Returns true when a chip was hit. */
    public boolean controllerPinHoveredChip() {
        if (cursorChip >= 0 && cursorChip < chips.size()) {
            if (chipToggle != null) chipToggle.onChipToggled(chips.get(cursorChip).moduleId);
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            invalidate();
            return true;
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

    /** The D-pad left/right: step through the sectors. */
    public void focusNextSector(int direction) {
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
                setHighlight(RadialWheelLayout.sectorAtLinear(
                        event.getX() - cx, event.getY() - cy, inner, outer, gapPx));
                return true;
            case MotionEvent.ACTION_MOVE:
                setHighlight(RadialWheelLayout.sectorAtLinear(
                        event.getX() - cx, event.getY() - cy, inner, outer, gapPx));
                return true;
            case MotionEvent.ACTION_UP:
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
                invalidate();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    /** Toggles the chip under the pointer, if any; returns true when a chip was hit. */
    private boolean handleChipTap(float x, float y, int w, int h) {
        int[] counts = chipClusterCounts();
        float[][] centers = RadialWheelLayout.chipCenters(w, h, counts[0], counts[1], counts[2]);
        int i = 0;
        for (Chip chip : chips) {
            float[] c = centers[i++];
            String text = getContext().getString(chip.labelRes);
            chipTextPaint.setTextSize(UiChip.LABEL_SP * density);
            float cw = UiChip.width(chipTextPaint.measureText(text), density);
            float ch = UiChip.HEIGHT_DP * density;
            if (Math.abs(x - c[0]) <= cw / 2f && Math.abs(y - c[1]) <= ch / 2f) {
                if (chipToggle != null) chipToggle.onChipToggled(chip.moduleId);
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                invalidate();
                return true;
            }
        }
        return false;
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
