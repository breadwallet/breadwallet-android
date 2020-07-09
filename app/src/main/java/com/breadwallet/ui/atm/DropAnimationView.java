package com.breadwallet.ui.atm;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.breadwallet.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Wayne
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class DropAnimationView extends FrameLayout {

    private final Random mRandom = new Random();

    private boolean mStopAnimation;

    private int mMinSize;

    private int mMaxSize;

    private int mLargeSize;

    private static final int ROTATION_DEGREES = 361;

    private Drawable[] mDrawables;

    private int[] mDrawableFilters;

    private int mAnimationRate;

    private boolean mEnableXAnimation;

    private boolean mEnableYAnimation;

    private boolean mEnableRotationAnimation;

    private static final int DEFAULT_ANIMATION_RATE = 100;

    private final Interpolator mInterpolator = new LinearInterpolator();

    private final List<AnimatorSet> mAnimatorSetList = new ArrayList<>();

    private final Runnable mRunnable = new Runnable() {

        @Override
        public void run() {
            if (mStopAnimation || (mMinSize == 0 && mMaxSize == 0)) {
                return;
            }

            final int width = getWidth();
            final int height = getHeight();
            if(width != 0 && height != 0) {
                // [mMinSize, mMaxSize]
                final int size = mMinSize + mRandom.nextInt(mMaxSize - mMinSize + 1);
                final boolean largeSize = size > mLargeSize;
                final int currentX = mRandom.nextInt(width - size);

                final ImageView imageView = new ImageView(getContext());

                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
                params.leftMargin = currentX;
                imageView.setLayoutParams(params);
                imageView.setImageDrawable(mDrawables[mRandom.nextInt(mDrawables.length)]);

                if (mDrawableFilters != null) {
                    imageView.setColorFilter(mDrawableFilters[mRandom.nextInt(mDrawableFilters.length)]);
                }

                final AnimatorSet animatorSet = new AnimatorSet();
                if (mEnableXAnimation) {
                    final int middle = width / 2;
                    final int end = currentX > middle ? -middle : middle;
                    animatorSet.playTogether(ObjectAnimator.ofFloat(imageView, "translationX", end));
                }

                if (mEnableYAnimation) {
                    animatorSet.playTogether(ObjectAnimator.ofFloat(imageView, "translationY", -size, height));
                }

                if (mEnableRotationAnimation) {
                    animatorSet.playTogether(ObjectAnimator.ofFloat(imageView, "rotation", mRandom.nextInt
                            (ROTATION_DEGREES) * (mRandom.nextInt(3) - 1), mRandom.nextInt(ROTATION_DEGREES) *
                            (mRandom.nextInt(3) - 1)));
                }

                int duration = height / mAnimationRate;
                // random from duration - [1, 3) to duration + [0, 10)
                animatorSet.setDuration(largeSize ? (duration - mRandom.nextInt(2) - 1) * 1000
                        : (duration + mRandom.nextInt(10)) * 1000);

                animatorSet.setInterpolator(mInterpolator);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        removeView(imageView);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        removeView(imageView);
                    }
                });
                addView(imageView);
                animatorSet.start();
                mAnimatorSetList.add(animatorSet);
            }
            beginAnimate(true);
        }
    };

    public DropAnimationView(Context context) {
        this(context, null);
    }

    public DropAnimationView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DropAnimationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.DropAnimationView, defStyleAttr, 0);
        if (array != null) {
            mMinSize = array.getDimensionPixelSize(R.styleable.DropAnimationView_minSize, 0);
            mMaxSize = array.getDimensionPixelSize(R.styleable.DropAnimationView_maxSize, 0);
            mLargeSize = (int) array.getFraction(R.styleable.DropAnimationView_largePercent, mMaxSize, mMaxSize, 0);
            mAnimationRate = array.getInteger(R.styleable.DropAnimationView_rate, DEFAULT_ANIMATION_RATE);
            mEnableXAnimation = array.getBoolean(R.styleable.DropAnimationView_xAnimate, false);
            mEnableYAnimation = array.getBoolean(R.styleable.DropAnimationView_yAnimate, true);
            mEnableRotationAnimation = array.getBoolean(R.styleable.DropAnimationView_rotationAnimate, false);
            array.recycle();
        }

        setClickable(false);
        setVisibility(GONE);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        ViewGroup.LayoutParams params = getLayoutParams();
        if (params.width == ViewGroup.LayoutParams.WRAP_CONTENT ||
                params.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void startAnimation() {
        if(mDrawables != null) {
            mStopAnimation = false;
            setVisibility(VISIBLE);
            beginAnimate(false);
        }
    }

    public void stopAnimation() {
        mStopAnimation = true;
        removeCallbacks(mRunnable);
        setVisibility(GONE);
        for (AnimatorSet as : mAnimatorSetList) {
            as.cancel();
            as.setTarget(null);
        }
        mAnimatorSetList.clear();
    }

    public void setDrawables(int... drawableIds) {
        if (drawableIds != null && drawableIds.length > 0) {
            mDrawables = new Drawable[drawableIds.length];
        }
        for (int i = 0; i < drawableIds.length; i++) {
            mDrawables[i] = getResources().getDrawable(drawableIds[i]);
        }
    }

    public void setDrawableFilters(int... filterColors) {
        mDrawableFilters = filterColors;
    }

    public void setMinSize(int minSize) {
        mMinSize = minSize;
    }

    public void setMaxSize(int maxSize) {
        mMaxSize = maxSize;
    }

    public void setLargeSize(int largeSize) {
        mLargeSize = largeSize;
    }

    public void setEnableXAnimation(boolean enableXAnimation) {
        mEnableXAnimation = enableXAnimation;
    }

    public void setEnableYAnimation(boolean enableYAnimation) {
        mEnableYAnimation = enableYAnimation;
    }

    public void setEnableRotationAnimation(boolean enableRotationAnimation) {
        mEnableRotationAnimation = enableRotationAnimation;
    }

    private void beginAnimate(boolean delayed) {
        postDelayed(mRunnable, delayed ? mRandom.nextInt(4) * 1000 : 0);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }
}
