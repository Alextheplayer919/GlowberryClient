package org.chimeramc.client.ui.components;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.PathInterpolator;

/**
 * A smooth animated toggle switch.
 *
 * <p>The mod list used a status <em>pill</em> you tapped to flip a module; the tap read as a button
 * press rather than a state change, and the state was carried by text. This is a real switch: a
 * track with a sliding thumb, animated over {@link UiTokens#TOGGLE_SLIDE_MS} with a gentle
 * ease-out, so the state change is visible motion and the control reads as on/off at a glance.
 *
 * <p>The geometry and the colours come from {@link UiToggle} (pure, unit-tested); this view only
 * drives the animation and paints. It is drawn as a sibling of the card's content, so the toggle
 * itself is the click target — a card-wide tap still toggles for convenience, but the switch is what
 * the user aims at.
 *
 * <p>Both constructors exist so XML can inflate it (the same rule the controller illustration and
 * the radial wheel follow).
 */
public class UiSwitchView extends View {

    /** Called when the user flips the switch (not when it is set programmatically). */
    public interface OnCheckedChangeListener {
        void onCheckedChanged(boolean checked);
    }

    private static final PathInterpolator SLIDE =
            new PathInterpolator(0.2f, 0f, 0.2f, 1f);

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;

    private boolean checked;
    private float progress; // 0 = fully off, 1 = fully on
    private boolean interactive = true;
    private int accent = UiTokens.ACCENT_500;
    private OnCheckedChangeListener listener;
    private ValueAnimator animator;

    public UiSwitchView(Context context) {
        this(context, null);
    }

    public UiSwitchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        accent = UiTokens.resolveAccent(context);
        setClickable(true);
        setFocusable(false);
        setDefaultFocusHighlightEnabled(false);
        setContentDescription("Toggle");
    }

    /** Sets the accent used for the on track. */
    public void setAccent(int color) {
        this.accent = color;
        invalidate();
    }

    /** The current state. */
    public boolean isChecked() {
        return checked;
    }

    /**
     * Sets the state without notifying the listener and animates the thumb to it.
     *
     * @param animate false to snap (a recycled view must not animate from the previous item's state)
     */
    public void setChecked(boolean value, boolean animate) {
        if (this.checked == value && ((value ? 1f : 0f) == progress || !animate)) {
            this.checked = value;
            if (!animate) progress = value ? 1f : 0f;
            invalidate();
            return;
        }
        this.checked = value;
        if (!animate || !isAttachedToWindow()) {
            cancelAnimator();
            progress = value ? 1f : 0f;
            invalidate();
            return;
        }
        cancelAnimator();
        animator = ValueAnimator.ofFloat(progress, value ? 1f : 0f);
        animator.setDuration(UiTokens.TOGGLE_SLIDE_MS);
        animator.setInterpolator(SLIDE);
        animator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    /** A module that cannot be driven shows a disabled switch and refuses the tap. */
    public void setInteractive(boolean value) {
        this.interactive = value;
        setAlpha(value ? 1f : 0.45f);
        setClickable(value);
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;
        // The track is the view; the thumb slides inside it (see UiToggle.thumbCenterX).
        UiToggle.draw(canvas, paint, 0f, 0f, w, h, accent, checked, progress, density);
    }

    @Override
    public boolean performClick() {
        if (!interactive) {
            return super.performClick();
        }
        setChecked(!checked, true);
        if (listener != null) listener.onCheckedChanged(checked);
        return true;
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelAnimator();
        super.onDetachedFromWindow();
    }

    private void cancelAnimator() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredW = (int) (UiTokens.TOGGLE_WIDTH * density);
        int desiredH = (int) (UiTokens.TOGGLE_HEIGHT * density);
        setMeasuredDimension(
                resolveSize(desiredW, widthMeasureSpec),
                resolveSize(desiredH, heightMeasureSpec));
    }
}
