package com.breadwallet.tools.animation;

import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringListener;
import com.facebook.rebound.SpringSystem;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 6/24/15.
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

public class SpringAnimator {
    private static final String TAG = SpringAnimator.class.getName();

    private static final int SPRING_DELAY_MS = 30;
    private static final double TENSION = 600;
    private static final double DAMPER = 35;
    public static final int TO_LEFT = -1;
    public static final int TO_RIGHT = 1;

    public static void showExpandCameraGuide(final View view) {
        view.setScaleX(0.1f);
        view.setScaleX(0.1f);
        SpringSystem springSystem = SpringSystem.create();

        // Add a spring to the system.
        final Spring spring = springSystem.createSpring();
        SpringConfig config = new SpringConfig(200, 10);
        spring.setSpringConfig(config);
        spring.setEndValue(0.8f);

        // Add a listener to observe the motion of the spring.
        spring.addListener(new SpringListener() {

            @Override
            public void onSpringUpdate(Spring spring) {
                // You can observe the updates in the spring
                // state by asking its current value in onSpringUpdate.
                float value = (float) spring.getCurrentValue();
                view.setScaleX(value);
                view.setScaleY(value);
            }

            @Override
            public void onSpringAtRest(Spring spring) {
            }

            @Override
            public void onSpringActivate(Spring spring) {
            }

            @Override
            public void onSpringEndStateChange(Spring spring) {
            }
        });

    }

    /**
     * Shows the springy animation on views
     */
    public static void showAnimation(final View view) {
        if (view != null) {
            SpringSystem springSystem = SpringSystem.create();

            // Add a spring to the system.
            final Spring spring = springSystem.createSpring();
            SpringConfig config = new SpringConfig(TENSION, DAMPER);
            spring.setSpringConfig(config);
            spring.setEndValue(1f);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    spring.setEndValue(0);
                }
            }, 100);

            // Add a listener to observe the motion of the spring.
            spring.addListener(new SpringListener() {

                @Override
                public void onSpringUpdate(Spring spring) {
                    // You can observe the updates in the spring
                    // state by asking its current value in onSpringUpdate.

                    float value = (float) spring.getCurrentValue();
                    float scale = 1f - (value * 0.5f);
                    view.setScaleX(scale);
                    view.setScaleY(scale);
                }

                @Override
                public void onSpringAtRest(Spring spring) {
                }

                @Override
                public void onSpringActivate(Spring spring) {
                }

                @Override
                public void onSpringEndStateChange(Spring spring) {
                }
            });
        } else {
            Log.e(TAG, "The view is null cannot show bouncy animaion!");
        }
        // Set the spring in motion; moving from 0 to 1

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
        SpringSystem springSystem = SpringSystem.create();
        if(view == null) return;
        // Add a spring to the system.
        final Spring spring = springSystem.createSpring();
        SpringConfig config = new SpringConfig(TENSION, DAMPER);
        spring.setSpringConfig(config);
        spring.setEndValue(1f);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                spring.setEndValue(0f);
            }
        }, SPRING_DELAY_MS);

        // Add a listener to observe the motion of the spring.
        spring.addListener(new SpringListener() {

            @Override
            public void onSpringUpdate(Spring spring) {
                // You can observe the updates in the spring
                // state by asking its current value in onSpringUpdate.

                float value = (float) spring.getCurrentValue();
                view.setX(direction * (value * -tension));
            }

            @Override
            public void onSpringAtRest(Spring spring) {
            }

            @Override
            public void onSpringActivate(Spring spring) {
            }

            @Override
            public void onSpringEndStateChange(Spring spring) {
            }
        });
    }

    /**
     * Uses the Facebook Spring animation to show a bouncy animation on
     * the view that is given as a parameter
     *
     * @param view      a view to apply the bouncy animation
     * @param direction SpringAnimator.TO_LEFT or  SpringAnimator.TO_RIGHT
     */
    public static void showBouncySlideVertical(final View view, final int direction) {
        SpringSystem springSystem = SpringSystem.create();

        // Add a spring to the system.
        final Spring spring = springSystem.createSpring();
        SpringConfig config = new SpringConfig(TENSION, DAMPER);
        spring.setSpringConfig(config);
        spring.setEndValue(1f);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                spring.setEndValue(0f);
            }
        }, 30);

        // Add a listener to observe the motion of the spring.
        spring.addListener(new SpringListener() {

            @Override
            public void onSpringUpdate(Spring spring) {
                // You can observe the updates in the spring
                // state by asking its current value in onSpringUpdate.

                float value = (float) spring.getCurrentValue();
                if (view != null)
                    view.setY(direction * (value * -20));
            }

            @Override
            public void onSpringAtRest(Spring spring) {
            }

            @Override
            public void onSpringActivate(Spring spring) {
            }

            @Override
            public void onSpringEndStateChange(Spring spring) {
            }
        });
    }

}

