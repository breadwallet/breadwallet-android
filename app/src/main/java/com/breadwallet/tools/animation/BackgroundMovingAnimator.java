package com.breadwallet.tools.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.Log;
import android.widget.ImageView;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 10/9/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class BackgroundMovingAnimator {
    public static final String TAG = BackgroundMovingAnimator.class.getName();
    private static final int RightToLeft = 1;
    private static final int LeftToRight = 2;
    private static int mDirection = RightToLeft;
    private static RectF mDisplayRect = new RectF();
    private static Matrix mMatrix = new Matrix();
    private static float mScaleFactor;
    private static final int DURATION = 30000;
    private static ImageView background;
    private static ValueAnimator mCurrentAnimator;

    private static void animate() {
        if(background == null) {
            stopBackgroundMoving();
            return;
        }
        updateDisplayRect();
        if (mDirection == RightToLeft) {
            animate(mDisplayRect.left, mDisplayRect.left -
                    (mDisplayRect.right - background.getWidth()));
        } else {
            animate(mDisplayRect.left, 0.0f);
        }
    }

    private static void animate(float from, float to) {
        if(background == null) {
            stopBackgroundMoving();
            return;
        }
        mCurrentAnimator = ValueAnimator.ofFloat(from, to);
        mCurrentAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if(background == null) {
                    stopBackgroundMoving();
                    return;
                }
                float value = (Float) animation.getAnimatedValue();

                mMatrix.reset();
                mMatrix.postScale(mScaleFactor, mScaleFactor);
                mMatrix.postTranslate(value, 0);
                background.setImageMatrix(mMatrix);

            }
        });
        mCurrentAnimator.setDuration(DURATION);
        mCurrentAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mDirection == RightToLeft)
                    mDirection = LeftToRight;
                else
                    mDirection = RightToLeft;
                animate();
            }
        });
        mCurrentAnimator.start();
    }

    private static void updateDisplayRect() {
        if(background == null) {
            stopBackgroundMoving();
            return;
        }
        mDisplayRect.set(0, 0, background.getDrawable().getIntrinsicWidth(),
                background.getDrawable().getIntrinsicHeight());
        mMatrix.mapRect(mDisplayRect);
    }

    public static void animateBackgroundMoving(ImageView theBackground) {

        background = theBackground;
        background.post(new Runnable() {
            @Override
            public void run() {
                if(background == null) {
                    stopBackgroundMoving();
                    return;
                }
                mScaleFactor = (float) background.getHeight() /
                        (float) background.getDrawable().getIntrinsicHeight();
                mMatrix.postScale(mScaleFactor, mScaleFactor);
                background.setImageMatrix(mMatrix);
                animate();
            }
        });
    }

    public static void stopBackgroundMoving() {
        if (background != null) {
            background.clearAnimation();
            background = null;
        }
        if (mMatrix != null)
            mMatrix.reset();

        if (mCurrentAnimator != null) {
            mCurrentAnimator.removeAllListeners();
            mCurrentAnimator.cancel();
        }
        mDirection = RightToLeft;
        mScaleFactor = 0;
    }
}
