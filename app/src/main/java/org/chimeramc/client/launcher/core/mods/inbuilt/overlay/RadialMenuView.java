package org.chimeramc.client.core.mods.inbuilt.overlay;

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

import org.chimeramc.client.R;
import org.chimeramc.client.core.cosmetics.PlayerSkinProvider;
import org.chimeramc.client.core.mods.inbuilt.overlay.RadialWheelLayout.Sector;

import java.util.ArrayList;
import java.util.List;

/**
 * The radial mod wheel: four curved sectors around the player's skin head, with floating
 * quick-toggle chips.
 *
 * <p>Drawn in code rather than XML because the sectors are arcs whose geometry depends on the view
 * size, and the chips are data-driven. The hit-testing is delegated to {@link RadialWheelLayout}, so
 * which sector a touch lands on is unit-tested and this view only paints and dispatches.
 *
 * <p>The centre hub shows the player's real skin head (the front face of the skin atlas), so the
 * wheel is anchored to the player rather than to a generic icon.
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

    private static final class Chip {
        final String moduleId;
        final int labelRes;
        // The chip's centre as a fraction of the view, so it follows any screen size.
        final float fx;
        final float fy;

        Chip(String moduleId, int labelRes, float fx, float fy) {
            this.moduleId = moduleId;
            this.labelRes = labelRes;
            this.fx = fx;
            this.fy = fy;
        }
    }

    private final Paint arcFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chipDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF();
    private final Rect srcHead = new Rect();
    private final RectF dstHead = new RectF();
    private final Path headClip = new Path();

    private final List<Chip> chips = new ArrayList<>();
    private Listener listener;
    private ChipToggle chipToggle;
    private Sector highlighted = Sector.NONE;

    private Bitmap skinHead;

    // Palette (matches the app's violet/magenta brand).
    private static final int ACCENT = 0xFF6236E8;
    private static final int ACCENT_LIGHT = 0xFFA88CFF;
    private static final int SECTOR_BASE = 0xCC2A2338;
    private static final int SECTOR_HIGH = 0xE67C5CFF;
    private static final int CHIP_BG = 0xE61A1A24;
    private static final int CHIP_ON = 0xFF39C46A;
    private static final int CHIP_OFF = 0xFF5A5A66;
    private static final int TEXT = 0xFFF1F4F6;

    /** The half-width, in degrees, of the dead gap on each sector boundary. */
    private static final float GAP_DEG = 6f;
    /** The stroke width of a sector band, as a fraction of the ring's outer radius. */
    private static final float BAND_FRACTION = 0.34f;
    /** The wheel's outer radius, as a fraction of the shorter view dimension. */
    private static final float OUTER_FRACTION = 0.30f;

    public RadialMenuView(Context context) {
        this(context, null);
    }

    public RadialMenuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        setClickable(true);
        buildChips();
        resolveSkinHead();
        labelPaint.setColor(TEXT);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(Typeface.DEFAULT_BOLD);
        iconPaint.setColor(TEXT);
        iconPaint.setTextAlign(Paint.Align.CENTER);
        chipTextPaint.setColor(TEXT);
        chipTextPaint.setTextAlign(Paint.Align.LEFT);
        hubPaint.setColor(0xFF201A2C);
        arcFill.setStyle(Paint.Style.STROKE);
        arcFill.setStrokeCap(Paint.Cap.BUTT);
        arcStroke.setStyle(Paint.Style.STROKE);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setChipToggle(ChipToggle chipToggle) {
        this.chipToggle = chipToggle;
        invalidate();
    }

    private void buildChips() {
        // Layout follows the reference: two top-left, three top-right, one bottom-right.
        chips.add(new Chip("list_players", R.string.inbuilt_mod_list_players, 0.15f, 0.14f));
        chips.add(new Chip("debug_menu", R.string.inbuilt_mod_debug_menu, 0.15f, 0.22f));
        chips.add(new Chip("zoom", R.string.inbuilt_mod_zoom, 0.85f, 0.14f));
        chips.add(new Chip("fullbright", R.string.inbuilt_mod_fullbright, 0.85f, 0.22f));
        chips.add(new Chip("time_changer", R.string.inbuilt_mod_time_changer, 0.85f, 0.30f));
        chips.add(new Chip("third_person_nametag", R.string.inbuilt_mod_third_person_nametag,
                0.85f, 0.70f));
    }

    private void resolveSkinHead() {
        try {
            PlayerSkinProvider.SkinBitmap skin = PlayerSkinProvider.resolve(getContext());
            if (skin != null && skin.bitmap != null) {
                // The head's front face is the standard 8x8 region at (8,8) in a 64x64 atlas.
                srcHead.set(8, 8, 16, 16);
                skinHead = skin.bitmap;
            }
        } catch (Throwable ignored) {
            skinHead = null;
        }
    }

    // --- Geometry helpers (shared by draw and touch) ----------------------------------------

    private float centerX() {
        return getWidth() / 2f;
    }

    private float centerY() {
        return getHeight() / 2f;
    }

    private float outerRadius() {
        return Math.min(getWidth(), getHeight()) * OUTER_FRACTION;
    }

    private float innerRadius() {
        return outerRadius() * (1f - BAND_FRACTION);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() <= 0 || getHeight() <= 0) return;
        float cx = centerX();
        float cy = centerY();
        float outer = outerRadius();
        float inner = innerRadius();
        float band = outer - inner;
        float radius = (inner + outer) / 2f;

        arcFill.setStrokeWidth(band);
        float sweep = 90f - 2f * GAP_DEG;
        for (Sector s : new Sector[]{Sector.MODS, Sector.CLOSET, Sector.SETTINGS, Sector.HUD}) {
            float center = (float) RadialWheelLayout.sectorCenterDegrees(s);
            arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius);
            arcFill.setColor(s == highlighted ? SECTOR_HIGH : SECTOR_BASE);
            canvas.drawArc(arcRect, center - sweep / 2f, sweep, false, arcFill);
            // A thin lighter outline on the outer edge of the band.
            arcStroke.setStrokeWidth(Math.max(1.5f, band * 0.05f));
            arcStroke.setColor(s == highlighted ? ACCENT_LIGHT : ACCENT);
            float or = outer - arcStroke.getStrokeWidth() / 2f;
            arcRect.set(cx - or, cy - or, cx + or, cy + or);
            canvas.drawArc(arcRect, center - sweep / 2f, sweep, false, arcStroke);
        }

        // The hub with the player's skin head.
        canvas.drawCircle(cx, cy, inner, hubPaint);
        drawHead(canvas, cx, cy, inner);

        // Labels on the band at each sector's centre angle.
        labelPaint.setTextSize(Math.max(11f, band * 0.34f));
        for (Sector s : new Sector[]{Sector.MODS, Sector.CLOSET, Sector.SETTINGS, Sector.HUD}) {
            double a = Math.toRadians(RadialWheelLayout.sectorCenterDegrees(s));
            float lx = cx + (float) (Math.cos(a) * radius);
            float ly = cy + (float) (Math.sin(a) * radius);
            canvas.drawText(sectorLabel(s), lx, ly + labelPaint.getTextSize() * 0.35f, labelPaint);
        }

        drawChips(canvas, getWidth(), getHeight());
    }

    private void drawHead(Canvas canvas, float cx, float cy, float radius) {
        if (skinHead == null || skinHead.isRecycled()) {
            // No skin available: a neutral head glyph rather than a blank hub.
            iconPaint.setTextSize(radius * 0.9f);
            canvas.drawText("\uD83D\uDC64", cx, cy + radius * 0.32f, iconPaint);
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
        // A thin ring around the head so it reads as seated in the hub.
        arcStroke.setColor(ACCENT_LIGHT);
        arcStroke.setStrokeWidth(Math.max(2f, radius * 0.06f));
        canvas.drawCircle(cx, cy, radius * 0.9f, arcStroke);
    }

    private void drawChips(Canvas canvas, int w, int h) {
        for (Chip chip : chips) {
            RectF box = chipRect(chip, w, h);
            chipPaint.setColor(CHIP_BG);
            canvas.drawRoundRect(box, box.height() / 2f, box.height() / 2f, chipPaint);
            // A status dot: green when the module is on, grey when off.
            boolean on = chipToggle != null && chipToggle.isChipOn(chip.moduleId);
            chipDotPaint.setColor(on ? CHIP_ON : CHIP_OFF);
            float dotR = box.height() * 0.22f;
            canvas.drawCircle(box.left + box.height() * 0.5f, box.centerY(), dotR, chipDotPaint);
            String text = getContext().getString(chip.labelRes);
            Paint.FontMetrics fm = chipTextPaint.getFontMetrics();
            float ty = box.centerY() - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(text, box.left + box.height() * 0.95f, ty, chipTextPaint);
        }
    }

    private RectF chipRect(Chip chip, int w, int h) {
        float min = Math.min(w, h);
        float chipH = Math.max(24f, min * 0.055f);
        chipTextPaint.setTextSize(Math.max(10f, min * 0.028f));
        String text = getContext().getString(chip.labelRes);
        float chipW = chipTextPaint.measureText(text) + chipH * 1.5f;
        float cx = w * chip.fx;
        float cy = h * chip.fy;
        return new RectF(cx - chipW / 2f, cy - chipH / 2f, cx + chipW / 2f, cy + chipH / 2f);
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

    // --- Touch ------------------------------------------------------------------------------

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float cx = centerX();
        float cy = centerY();
        float outer = outerRadius();
        float inner = innerRadius();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (handleChipTap(event.getX(), event.getY(), getWidth(), getHeight())) {
                    return true;
                }
                setHighlight(RadialWheelLayout.sectorAt(
                        event.getX() - cx, event.getY() - cy, inner, outer, GAP_DEG));
                return true;
            case MotionEvent.ACTION_MOVE:
                setHighlight(RadialWheelLayout.sectorAt(
                        event.getX() - cx, event.getY() - cy, inner, outer, GAP_DEG));
                return true;
            case MotionEvent.ACTION_UP:
                Sector s = RadialWheelLayout.sectorAt(
                        event.getX() - cx, event.getY() - cy, inner, outer, GAP_DEG);
                highlighted = Sector.NONE;
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
        for (Chip chip : chips) {
            if (chipRect(chip, w, h).contains(x, y)) {
                if (chipToggle != null) chipToggle.onChipToggled(chip.moduleId);
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
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
        invalidate();
    }
}
