package com.breadwallet.tools.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.widget.ImageView;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 10/9/15.
 * Copyright (c) 2015 Mihail Gutan <mihail@breadwallet.com>
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
public class BackgroundMovingAnimator {
    public static final String TAG = BackgroundMovingAnimator.class.getName();
    private static final int RightToLeft = 1;
    private static final int LeftToRight = 2;
    private static int mDirection = RightToLeft;
    private static final RectF mDisplayRect = new RectF();
    private static final Matrix mMatrix = new Matrix();
    private static float mScaleFactor;
    private static final int DURATION = 30000;
    private static ImageView background;

    private static void animate() {
        updateDisplayRect();
        if (mDirection == RightToLeft) {
            animate(mDisplayRect.left, mDisplayRect.left -
                    (mDisplayRect.right - background.getWidth()));
        } else {
            animate(mDisplayRect.left, 0.0f);
        }
    }

    private static void animate(float from, float to) {
        ValueAnimator mCurrentAnimator = ValueAnimator.ofFloat(from, to);
        mCurrentAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
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
//                if (mDirection == RightToLeft)
                mDirection = LeftToRight;
//                else
//                    mDirection = RightToLeft;

                animate();
            }
        });
        mCurrentAnimator.start();
    }

    private static void updateDisplayRect() {
        mDisplayRect.set(0, 0, background.getDrawable().getIntrinsicWidth(),
                background.getDrawable().getIntrinsicHeight());
        mMatrix.mapRect(mDisplayRect);
    }

    public static void animateBackgroundMoving(ImageView theBackground) {
        if (background != null)
            background.clearAnimation();
        background = theBackground;
        background.clearAnimation();
        background.post(new Runnable() {
            @Override
            public void run() {
                mScaleFactor = (float) background.getHeight() /
                        (float) background.getDrawable().getIntrinsicHeight();
                mMatrix.postScale(mScaleFactor, mScaleFactor);
                background.setImageMatrix(mMatrix);
                animate();
            }
        });
    }
}
