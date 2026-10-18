package org.chimeramc.client.ui.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import org.chimeramc.client.core.cosmetics.PlayerSkinProvider;
import org.chimeramc.client.core.cosmetics.PreviewCharacterScene;
import org.chimeramc.client.core.cosmetics.PreviewRaster;
import org.chimeramc.client.core.cosmetics.PreviewScene;
import org.chimeramc.client.core.cosmetics.SkinModel;

/**
 * The launch tab's glassmorphic head pod: a circular frosted-glass containment disc with the player's
 * real skin head rendered inside it as a live 3D turntable.
 *
 * <p>This is the interactive anchor the spec describes — hover brightens the rim and pauses the idle
 * spin, a drag rotates the model 1:1 with the pointer and carries momentum on release, and a click
 * opens the skin customizer. The motion itself lives in {@link HeadPodMotion} and the orbiting
 * accents in {@link PodParticles}, both pure and unit-tested; this view only projects, paints and
 * routes touch.
 *
 * <p>The head is the same software-rasterised skin head the radial wheel's hub uses, so the two
 * cannot show a different skin. It reads through {@link PlayerSkinProvider}, which prefers the game's
 * live skin when a {@code GameSkinSource} provider is installed.
 */
public class HeadPodView extends View {

    /** Called when the pod is tapped (a drag does not count) — the skin-customize entry point. */
    public interface Listener {
        void onPodClicked();
    }

    private final Paint discPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint syncPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path headClip = new Path();
    private final RectF discRect = new RectF();
    private final RectF shadowRect = new RectF();

    private final PreviewRaster raster = new PreviewRaster();
    private PreviewScene headScene;
    private int[] headAtlas;
    private Bitmap headBitmap;

    private final HeadPodMotion motion = new HeadPodMotion();
    private final float[] particleX = new float[PodParticles.COUNT];
    private final float[] particleY = new float[PodParticles.COUNT];

    private Listener listener;
    private int accent;
    private float density;
    private long lastFrameMs;
    private long startMs;
    private float downX;
    private float downY;
    private boolean dragging;
    private boolean movedEnoughToDrag;
    private final int touchSlop;

    private boolean animationsEnabled = true;
    private int syncState = SYNC_UNKNOWN;
    private long entranceStartMs;
    private boolean entranceDone;

    /** The pod's sync indicator states. */
    public static final int SYNC_UNKNOWN = 0;
    public static final int SYNC_SYNCED = 1;
    public static final int SYNC_LOADING = 2;

    private final Runnable frameTick = new Runnable() {
        @Override
        public void run() {
            long now = android.os.SystemClock.uptimeMillis();
            float dt = (now - lastFrameMs) / 1000f;
            lastFrameMs = now;
            motion.tick(dt);
            invalidate();
            // Keep animating only while there is motion or an orbit to draw; the particle ring is
            // always drifting, so the pod is always live while attached.
            if (isAttachedToWindow()) postOnAnimation(this);
        }
    };

    public HeadPodView(Context context) {
        this(context, null);
    }

    public HeadPodView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        setClickable(true);
        setFocusable(false);
        density = getResources().getDisplayMetrics().density;
        accent = UiTokens.resolveAccent(context);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        headPaint.setFilterBitmap(true);
        resolveSkin();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /** Re-resolves the skin and rebuilds the head. Safe to call whenever the skin may have changed. */
    public void refreshSkin() {
        resolveSkin();
        invalidate();
    }

    /** Sets the sync indicator state (green synced / amber loading / none). */
    public void setSyncState(int state) {
        this.syncState = state;
        invalidate();
    }

    /**
     * Gates the idle motion on the personalization "show animations" flag.
     *
     * <p>Under reduced motion the pod paints one static frame: no idle turn, no coast, no particles.
     * It still responds to a direct drag, because that is the player's own input rather than ambient
     * motion.
     */
    public void setAnimationsEnabled(boolean enabled) {
        this.animationsEnabled = enabled;
        if (!enabled) {
            removeCallbacks(frameTick);
        } else if (isAttachedToWindow()) {
            lastFrameMs = android.os.SystemClock.uptimeMillis();
            // Re-run the entrance so a toggle applied while the tab is open still gets the pop.
            entranceStartMs = lastFrameMs;
            entranceDone = false;
            postOnAnimation(frameTick);
        }
        invalidate();
    }

    private void resolveSkin() {
        try {
            PlayerSkinProvider.SkinBitmap skin = PlayerSkinProvider.resolve(getContext());
            if (skin == null || skin.bitmap == null) return;
            headAtlas = new int[SkinModel.ATLAS_SIZE * SkinModel.ATLAS_SIZE];
            skin.bitmap.getPixels(headAtlas, 0, SkinModel.ATLAS_SIZE, 0, 0,
                    SkinModel.ATLAS_SIZE, SkinModel.ATLAS_SIZE);
            headScene = new PreviewScene();
            // Head only: the pod is a bust, so a full body would be a tiny unreadable figure.
            PreviewCharacterScene.addCharacter(headScene, headAtlas, SkinModel.ATLAS_SIZE,
                    0f, 0f, "head");
        } catch (Throwable ignored) {
            headAtlas = null;
            headScene = null;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startMs = android.os.SystemClock.uptimeMillis();
        entranceStartMs = startMs;
        entranceDone = false;
        lastFrameMs = startMs;
        if (animationsEnabled) postOnAnimation(frameTick);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(frameTick);
        releaseHeadBitmap();
    }

    private void releaseHeadBitmap() {
        if (headBitmap != null && !headBitmap.isRecycled()) {
            headBitmap.recycle();
        }
        headBitmap = null;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        releaseHeadBitmap();
    }

    private float podRadius() {
        return Math.min(getWidth(), getHeight()) / 2f * 0.86f;
    }

    // --- Touch: hover, drag, click ------------------------------------------------------------

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                dragging = true;
                movedEnoughToDrag = false;
                motion.beginDrag();
                motion.setHovered(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!dragging) return true;
                float dx = event.getX() - downX;
                downX = event.getX();
                downY = event.getY();
                if (Math.abs(dx) > touchSlop) movedEnoughToDrag = true;
                if (movedEnoughToDrag) {
                    long now = android.os.SystemClock.uptimeMillis();
                    float dt = Math.max(1f, now - lastFrameMs) / 1000f;
                    motion.drag(dx, dt);
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                motion.setHovered(false);
                motion.endDrag();
                if (!movedEnoughToDrag && event.getActionMasked() == MotionEvent.ACTION_UP) {
                    performClick();
                }
                invalidate();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        if (listener != null) listener.onPodClicked();
        return true;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        // Hover: a mouse/controller cursor over the pod pauses the idle spin and brightens the rim.
        if (event.getActionMasked() == MotionEvent.ACTION_HOVER_ENTER
                || event.getActionMasked() == MotionEvent.ACTION_HOVER_MOVE) {
            motion.setHovered(true);
            invalidate();
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_HOVER_EXIT) {
            motion.setHovered(false);
            invalidate();
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    // --- Draw ---------------------------------------------------------------------------------

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;
        long now = android.os.SystemClock.uptimeMillis();
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = podRadius();
        float bob = animationsEnabled ? HeadPodMotion.bobPx(now - startMs) : 0f;

        // Entrance: the whole pod springs up from zero, the bust resolves in behind it. Under
        // reduced motion the pod is simply present at rest.
        long sinceEntrance = now - entranceStartMs;
        float entranceScale = 1f;
        float headAlpha = 1f;
        if (animationsEnabled && !entranceDone) {
            if (PodEntrance.isSettled(sinceEntrance)) {
                entranceDone = true;
            } else {
                entranceScale = PodEntrance.scaleAt(sinceEntrance);
                headAlpha = PodEntrance.headAlphaAt(sinceEntrance);
            }
        }

        int save = canvas.save();
        if (entranceScale != 1f) {
            canvas.scale(entranceScale, entranceScale, cx, cy);
        }
        drawPedestal(canvas, cx, cy, radius);
        drawDisc(canvas, cx, cy, radius, bob);
        drawHead(canvas, cx, cy, radius, bob, headAlpha);
        drawRim(canvas, cx, cy, radius, bob);
        if (animationsEnabled) drawParticles(canvas, cx, cy, radius, now);
        drawSyncIndicator(canvas, cx, cy, radius);
        canvas.restoreToCount(save);
    }

    /** The tinted elliptical drop shadow that grounds the pod. */
    private void drawPedestal(Canvas canvas, float cx, float cy, float radius) {
        float rx = radius * 1.05f;
        float ry = radius * 0.26f;
        float bottom = cy + radius * 0.92f;
        shadowRect.set(cx - rx, bottom - ry, cx + rx, bottom + ry);
        shadowPaint.setStyle(Paint.Style.FILL);
        shadowPaint.setColor(UiTokens.withAlpha(accent, 0x38));
        shadowPaint.setShadowLayer(radius * 0.35f, 0f, radius * 0.06f,
                UiTokens.withAlpha(accent, 0x30));
        canvas.drawOval(shadowRect, shadowPaint);
        shadowPaint.clearShadowLayer();
    }

    /** The frosted-glass disc: a translucent fill, a soft inner glow and a faint outer halo. */
    private void drawDisc(Canvas canvas, float cx, float cy, float radius, float bob) {
        float hover = motion.isHovered() ? 1f : 0f;
        float r = radius;
        discRect.set(cx - r, cy - r + bob, cx + r, cy + r + bob);

        // Outer bloom halo, brighter on hover.
        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setColor(UiTokens.withAlpha(accent, (int) (0x1C + 0x2C * hover)));
        glowPaint.setShadowLayer(r * (0.20f + 0.12f * hover), 0f, 0f,
                UiTokens.withAlpha(accent, (int) (0x55 + 0x55 * hover)));
        canvas.drawCircle(cx, cy + bob, r * 0.99f, glowPaint);
        glowPaint.clearShadowLayer();

        // Glass fill, with a radial inner glow toward the accent at the top-left.
        int fill = UiGlass.panelFill(UiTokens.BG_WINDOW, UiGlass.BLUR_PANEL, false);
        discPaint.setStyle(Paint.Style.FILL);
        discPaint.setShader(new RadialGradient(cx - r * 0.3f, cy + bob - r * 0.3f,
                r * 1.4f,
                UiTokens.withAlpha(accent, (int) (0x22 + 0x22 * hover)),
                fill, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy + bob, r, discPaint);
        discPaint.setShader(null);
    }

    /** The 1dp gradient rim, brightened on hover. */
    private void drawRim(Canvas canvas, float cx, float cy, float radius, float bob) {
        float hover = motion.isHovered() ? 1f : 0f;
        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setStrokeWidth(Math.max(1f, (1f + 0.5f * hover) * density));
        rimPaint.setShader(new android.graphics.SweepGradient(cx, cy + bob,
                new int[]{
                        UiTokens.withAlpha(accent, (int) (0xAA + 0x40 * hover)),
                        UiTokens.withAlpha(UiTokens.ACCENT_400, (int) (0x88 + 0x40 * hover)),
                        UiTokens.withAlpha(accent, (int) (0xAA + 0x40 * hover))},
                new float[]{0f, 0.5f, 1f}));
        canvas.drawCircle(cx, cy + bob, radius, rimPaint);
        rimPaint.setShader(null);
    }

    /** The head itself, clipped to the disc. {@code alpha} is the entrance fade (1 = fully in). */
    private void drawHead(Canvas canvas, float cx, float cy, float radius, float bob, float alpha) {
        if (headScene == null || headAtlas == null) {
            // No skin available: a neutral accent disc rather than a glyph or emoji.
            headPaint.setStyle(Paint.Style.FILL);
            headPaint.setColor(UiTokens.ACCENT_900);
            canvas.drawCircle(cx, cy + bob, radius * 0.7f, headPaint);
            return;
        }
        int size = Math.max(24, (int) (radius * 1.6f));
        if (headBitmap == null || headBitmap.getWidth() != size) {
            releaseHeadBitmap();
            try {
                headBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            } catch (Throwable t) {
                headBitmap = null;
                return;
            }
        }
        raster.prepare(size, size, 0x00000000);
        SkinModel.Camera cam = new SkinModel.Camera(motion.yaw(), 8f, SkinModel.CAMERA_DISTANCE,
                0f, 28f, 0f);
        PreviewScene.Fit fit = headScene.fit(cam, size, size, 0.06f);
        headScene.render(raster, cam, fit);
        headBitmap.setPixels(raster.pixels(), 0, size, 0, 0, size, size);

        headClip.reset();
        headClip.addCircle(cx, cy + bob, radius * 0.94f, Path.Direction.CW);
        int save = canvas.save();
        canvas.clipPath(headClip);
        RectF dst = new RectF(cx - radius * 0.9f, cy - radius * 0.9f + bob,
                cx + radius * 0.9f, cy + radius * 0.9f + bob);
        headPaint.setAlpha(Math.max(0, Math.min(255, (int) (alpha * 255f))));
        canvas.drawBitmap(headBitmap, null, dst, headPaint);
        headPaint.setAlpha(255);
        // Hover rim light: a soft accent spotlight on the upper-left of the head, so the model
        // reads as lit rather than pasted in. Only on hover, per the spec's hover state.
        if (motion.isHovered()) {
            float rr = radius * 0.94f;
            glowPaint.setStyle(Paint.Style.FILL);
            glowPaint.setShader(new RadialGradient(cx - rr * 0.4f, cy + bob - rr * 0.4f, rr * 1.1f,
                    UiTokens.withAlpha(0xFFFFFFFF, 0x40),
                    UiTokens.withAlpha(accent, 0x00), Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy + bob, rr, glowPaint);
            glowPaint.setShader(null);
        }
        canvas.restoreToCount(save);
    }

    /** The sparse orbiting accent particles. */
    private void drawParticles(Canvas canvas, float cx, float cy, float radius, long now) {
        PodParticles.positions(cx, cy + radius * 0f, radius, now - startMs, particleX, particleY);
        particlePaint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < PodParticles.COUNT; i++) {
            float a = PodParticles.alpha(i, now - startMs);
            particlePaint.setColor(UiTokens.withAlpha(accent, (int) (a * 0xCC)));
            particlePaint.setShadowLayer(3f * density, 0f, 0f,
                    UiTokens.withAlpha(accent, (int) (a * 0x88)));
            canvas.drawCircle(particleX[i], particleY[i], 1.6f * density, particlePaint);
        }
        particlePaint.clearShadowLayer();
    }

    /** The small pulsing dot at the pod's bottom edge that reports the skin sync state. */
    private void drawSyncIndicator(Canvas canvas, float cx, float cy, float radius) {
        if (syncState == SYNC_UNKNOWN) return;
        int color = syncState == SYNC_SYNCED ? UiTokens.SUCCESS : UiTokens.WARNING;
        float y = cy + radius * 0.92f;
        syncPaint.setStyle(Paint.Style.FILL);
        syncPaint.setColor(color);
        syncPaint.setShadowLayer(4f * density, 0f, 0f, UiTokens.withAlpha(color, 0xAA));
        canvas.drawCircle(cx, y, 2.5f * density, syncPaint);
        syncPaint.clearShadowLayer();
    }
}
