package com.breadwallet.presenter.fragments.utils;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.intro.IntroActivity;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 6/14/18.
 * Copyright (c) 2018 breadwallet LLC
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
public class ModalDialogFragment extends Fragment {
    private ViewGroup mBackgroundLayout;
    private ViewGroup mSignalLayout;
    private ViewGroup mRootView;
    public static final int SLIDE_ANIMATION_DURATION = 300;
    public static final float OVERSHOOT_INTERPOLATOR_TENSION = 0.7f;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRootView.setFocusableInTouchMode(true);
        mRootView.requestFocus();
        mRootView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    closeWithAnimation();
                    return true;
                }
                return false;
            }
        });

        final ViewTreeObserver observer = mSignalLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (observer.isAlive()) {
                    observer.removeOnGlobalLayoutListener(this);
                }
                animateBackgroundDim(mBackgroundLayout, false);
                animateSignalSlide(mSignalLayout, false, new OnSlideAnimationEnd() {
                    @Override
                    public void onAnimationEnd() {

                    }
                });
            }
        });
    }

    protected void closeWithAnimation() {
        animateBackgroundDim(mBackgroundLayout, true);
        animateSignalSlide(mSignalLayout, true, new OnSlideAnimationEnd() {
            @Override
            public void onAnimationEnd() {
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            }
        });
    }

    public static void animateSignalSlide(final ViewGroup signalLayout, final boolean reverse, final OnSlideAnimationEnd listener) {
        float translationY = signalLayout.getTranslationY();
        float signalHeight = signalLayout.getHeight();
        signalLayout.setTranslationY(reverse ? translationY : translationY + signalHeight);

        signalLayout.animate().translationY(reverse ? IntroActivity.screenParametersPoint.y : translationY).setDuration(SLIDE_ANIMATION_DURATION)
                .setInterpolator(reverse ? new DecelerateInterpolator() : new OvershootInterpolator(OVERSHOOT_INTERPOLATOR_TENSION))
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        if (listener != null) {
                            listener.onAnimationEnd();
                        }
                    }
                });

    }

    public static void animateBackgroundDim(final ViewGroup backgroundLayout, boolean reverse) {
        int transColor = reverse ? R.color.black_trans : android.R.color.transparent;
        int blackTransColor = reverse ? android.R.color.transparent : R.color.black_trans;

        ValueAnimator anim = new ValueAnimator();
        anim.setIntValues(transColor, blackTransColor);
        anim.setEvaluator(new ArgbEvaluator());
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                backgroundLayout.setBackgroundColor((Integer) valueAnimator.getAnimatedValue());
            }
        });

        anim.setDuration(SLIDE_ANIMATION_DURATION);
        anim.start();
    }


    public interface OnSlideAnimationEnd {
        void onAnimationEnd();
    }


    protected ViewGroup assignBackgroundLayout(ViewGroup backgroundLayout) {
        this.mBackgroundLayout = backgroundLayout;
        return mBackgroundLayout;
    }

    protected ViewGroup assignSignalLayout(ViewGroup signalLayout) {
        this.mSignalLayout = signalLayout;
        return mSignalLayout;
    }

    protected ViewGroup assignRootView(ViewGroup rootView) {
        this.mRootView = rootView;
        return mRootView;
    }
}
