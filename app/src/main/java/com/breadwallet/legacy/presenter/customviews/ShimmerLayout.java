/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 1/29/19.
 * Copyright (c) 2019 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.breadwallet.legacy.presenter.customviews;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import com.breadwallet.R;

/**
 * Frame layout with the ability to shimmer over it's children.
 */
public class ShimmerLayout extends FrameLayout {
    private static final String TAG = ShimmerLayout.class.getSimpleName();

    private static final int DEFAULT_ANIMATION_DURATION_MILLISECONDS = 1000;
    // Maximum alpha of the shimmering gradient (out of 255).
    private static final int MAX_ALPHA = 120;
    // Maximum alpha possible.
    private static final int FULL_ALPHA = 255;
    // Number of colors in the gradient (color range).
    private static final int COLOR_COUNT = 10;
    // Maximum progress steps to reach full shimmering effect.
    private static final int MAX_PROGRESS = MAX_ALPHA * COLOR_COUNT / 2;
    // The step in the progress to start fading the shimmering effect.
    private static final int START_PROGRESS_FOR_FADING = MAX_PROGRESS - MAX_PROGRESS / 4;

    private ValueAnimator mProgressAnimation;
    private boolean mIsAnimationStarted;
    // The value tha will be animated by ValueAnimator.
    private int mProgress = 0;
    private GradientDrawable mGradientDrawable;
    // Gradient colors
    private int[] mGradientColors = new int[COLOR_COUNT];
    // The base shimmering color (white).
    private int mShimmerColor;
    private ViewTreeObserver.OnPreDrawListener mStartAnimationPreDrawListener;

    /**
     * @param context The context.
     */
    public ShimmerLayout(Context context) {
        this(context, null);
    }

    /**
     * @param context The context to use for this layout.
     * @param attrs   The AttributeSet.
     */
    public ShimmerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * @param context  The context to use for this layout.
     * @param attrs    The AttributeSet.
     * @param defStyle The default style.
     */
    public ShimmerLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setWillNotDraw(false);
        mShimmerColor = getContext().getColor(R.color.shimmer_color);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        drawShimmer(canvas);
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility != VISIBLE) {
            // No need to show the animation if the visibility is changed to non visible.
            stopShimmerAnimation();
        }
    }

    /**
     * Starts the shimmering animation if not already started.
     */
    public void startShimmerAnimation() {
        if (mIsAnimationStarted) {
            return;
        }
        // View not loaded yet, listen to it.
        if (getWidth() == 0) {
            mStartAnimationPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    getViewTreeObserver().removeOnPreDrawListener(this);
                    animateShimmering();
                    return true;
                }
            };
            getViewTreeObserver().addOnPreDrawListener(mStartAnimationPreDrawListener);
        } else {
            mIsAnimationStarted = true;
            animateShimmering();
        }
    }

    /**
     * Creates the animation and starts showing it to the user.
     */
    private void animateShimmering(){
        Animator animator = createShimmerAnimation();
        animator.start();
    }

    /**
     * Stops the shimmering animation.
     */
    public void stopShimmerAnimation() {
        if (mStartAnimationPreDrawListener != null) {
            getViewTreeObserver().removeOnPreDrawListener(mStartAnimationPreDrawListener);
        }
        resetShimmering();
    }

    /**
     * Draws the shimmer layout
     *
     * @param canvas The canvas to draw on.
     */
    private void drawShimmer(Canvas canvas) {
        if (mIsAnimationStarted) {
            // Lazy loading of the gradient drawable.
            if (mGradientDrawable == null) {
                mGradientDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, mGradientColors);
                mGradientDrawable.setShape(GradientDrawable.RECTANGLE);
                mGradientDrawable.setBounds(new Rect(0, 0, getWidth(), getHeight()));
            }
            updateGradientAlpha();
            updateColors();
            mGradientDrawable.setColors(mGradientColors);
            mGradientDrawable.draw(canvas);
            canvas.save();
            canvas.restore();
        }
    }

    /**
     * Updates the gradient alpha using the mProgress value that is constantly update by our ValueAnimator
     * to create the fading effect starting when
     * the {@link ShimmerLayout#mProgress} passed {@link ShimmerLayout#START_PROGRESS_FOR_FADING}.
     */
    private void updateGradientAlpha() {
        int newAlpha;
        // Use full alpha until mProgress reached START_PROGRESS_FOR_FADING.
        if (mProgress < START_PROGRESS_FOR_FADING) {
            newAlpha = FULL_ALPHA;
        } else {
            // Start fading the gradient using alpha.
            // The full progress from START_PROGRESS_FOR_FADING to MAX_PROGRESS.
            int totalSubProgress = MAX_PROGRESS - START_PROGRESS_FOR_FADING;
            // The actual progress in the above range.
            int subProgress = totalSubProgress - (mProgress - START_PROGRESS_FOR_FADING);
            // New alpha to be applied for fading effect.
            newAlpha = subProgress * FULL_ALPHA / totalSubProgress;
        }
        mGradientDrawable.setAlpha(newAlpha);
    }

    /**
     * Update all the colors alpha for shimmering effect base on {@link ShimmerLayout#mProgress}.
     */
    private void updateColors() {
        for (int i = COLOR_COUNT / 2 - 1; i >= 0; i--) {
            int newAlpha = mProgress >= MAX_ALPHA ? MAX_ALPHA : mProgress;
            // Consume what was used.
            mProgress -= newAlpha;
            int newColor = Color.argb(newAlpha, Color.red(mShimmerColor),
                    Color.green(mShimmerColor), Color.blue(mShimmerColor));
            // Set colors middle -> left
            mGradientColors[i] = newColor;
            // Set colors middle -> right
            mGradientColors[COLOR_COUNT - i - 1] = newColor;
        }
    }

    /**
     * Do the object cleanup here.
     */
    private void resetShimmering() {
        if (mProgressAnimation != null) {
            mProgressAnimation.end();
            mProgressAnimation.removeAllUpdateListeners();
        }
        mProgressAnimation = null;
        mIsAnimationStarted = false;
    }

    /**
     * Create the Value animator tha will modify {@link ShimmerLayout#mProgress}.
     *
     * @return The Animator.
     */
    private Animator createShimmerAnimation() {
        if (mProgressAnimation == null) {
            mProgressAnimation = ValueAnimator.ofInt(0, MAX_PROGRESS);
            mProgressAnimation.setDuration(DEFAULT_ANIMATION_DURATION_MILLISECONDS);
            mProgressAnimation.setInterpolator(new DecelerateInterpolator());
            mProgressAnimation.setRepeatCount(ObjectAnimator.INFINITE);
            mProgressAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mProgress = (int) animation.getAnimatedValue();
                    invalidate();
                }
            });
        }
        return mProgressAnimation;
    }

    /**
     * The animation listener with events like STARTED, STOPPED.
     */
    public interface AnimationListener {
        /**
         * The Animation event enum.
         */
        enum AnimationEvent {
            STARTED,
            STOPPED
        }

        /**
         * Invoked when an event has happened.
         *
         * @param event The event that happened.
         */
        void onAnimationEvent(AnimationEvent event);
    }

}