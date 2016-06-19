package com.breadwallet.tools.animation;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.BounceInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

import com.breadwallet.presenter.fragments.FragmentSettingsAll;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on 6/24/15.
 * Copyright (c) 2016 breadwallet llc <mihail@breadwallet.com>
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

public class SpringAnimator {
    private static final String TAG = SpringAnimator.class.getName();

    public static void showExpandCameraGuide(final View view) {
        if (view != null) {
            view.setVisibility(View.GONE);
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ScaleAnimation trans = new ScaleAnimation(0.0f, 1f, 0.0f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                trans.setDuration(800);
                trans.setInterpolator(new DecelerateOvershootInterpolator(1.3f, 4f));
                Log.e(TAG, "startAnimation");
                if (view != null) {
                    view.setVisibility(View.VISIBLE);
                    view.startAnimation(trans);
                }
            }
        }, 200);

    }

    /**
     * Shows the springy animation on views
     */
    public static void showAnimation(final View view) {
        if (view == null) return;
        ScaleAnimation trans = new ScaleAnimation(0.3f, 1f, 0.3f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        trans.setDuration(500);
        trans.setInterpolator(new DecelerateOvershootInterpolator(1.3f, 4f));
        Log.e(TAG, "startAnimation");
        view.setVisibility(View.VISIBLE);
        view.startAnimation(trans);

    }

    /**
     * Shows the springy bubble animation on views
     */
    public static void showBubbleAnimation(final View view) {
        if (view == null) return;
        ScaleAnimation trans = new ScaleAnimation(0.0f, 1f, 0.0f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        trans.setDuration(500);
        trans.setInterpolator(new DecelerateOvershootInterpolator(1.3f, 1.2f));
        Log.e(TAG, "startAnimation");
        view.setVisibility(View.VISIBLE);
        view.startAnimation(trans);
    }


    /**
     * Uses the Facebook Spring animation to show a bouncy animation on
     * the view that is given as a parameter
     *
     * @param view      a view to apply the bouncy animation
     * @param direction SpringAnimator.TO_LEFT or  SpringAnimator.TO_RIGHT
     * @param tension   an int value to specify the springy tension
     */
    public static void showBouncySlideHorizontal(final View view, final int direction, final int tension) {
//        SpringSystem springSystem = SpringSystem.create();
//        if (view == null) return;
//        // Add a spring to the system.
//        final Spring spring = springSystem.createSpring();
//        SpringConfig config = new SpringConfig(800, 30);
//        spring.setSpringConfig(config);
//        spring.setEndValue(1);
//
//        // Add a listener to observe the motion of the spring.
//        spring.addListener(new SimpleSpringListener() {
//
//            @Override
//            public void onSpringUpdate(Spring spring) {
//                // You can observe the updates in the spring
//                // state by asking its current value in onSpringUpdate.
//
//                float value = (float) spring.getCurrentValue();
//
//                view.setX(direction * (value));
//            }
//
//        });
    }

    /**
     * Uses the Facebook Spring animation to show a bouncy animation on
     * the view that is given as a parameter
     *
     * @param view      a view to apply the bouncy animation
     * @param direction SpringAnimator.TO_LEFT or  SpringAnimator.TO_RIGHT
     */
    public static void showBouncySlideVertical(final View view, final int direction) {
//        SpringSystem springSystem = SpringSystem.create();
//        if (view == null) return;
//
//        // Add a spring to the system.
//        final Spring spring = springSystem.createSpring();
//        SpringConfig config = new SpringConfig(400, 15);
//        spring.setSpringConfig(config);
//        spring.setEndValue(1f);
//
//        // Add a listener to observe the motion of the spring.
//        spring.addListener(new SimpleSpringListener() {
//
//            @Override
//            public void onSpringUpdate(Spring spring) {
//                // You can observe the updates in the spring
//                // state by asking its current value in onSpringUpdate.
//
//                float value = (float) spring.getCurrentValue();
//                view.setY(direction * (value * 20));
////                Log.e(TAG,"direction * (value * -20): "  + (direction * (value * -20)));
//            }
//
//        });
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                spring.setEndValue(0f);
//            }
//        }, 100);

    }

}

