package com.breadwallet.tools.animation;

import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringListener;
import com.facebook.rebound.SpringSystem;

/**
 * Created by Mihail on 6/26/15.
 */
public class SpringAnimator {
    private static double TENSION = 600;
    private static double DAMPER = 35;
    public static final String TAG = "SpringAnimation";
    private static MainActivity app;
    public static final int TO_LEFT = -1;
    public static final int TO_RIGHT = 1;

    public static void showExpandCameraGuide(final View view) {
        view.setScaleX(0.1f);
        view.setScaleX(0.1f);
        SpringSystem springSystem = SpringSystem.create();
        app = MainActivity.getApp();

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
     * Shows the springy animation on the burger button
     */
    public static void showAnimation(final View view) {
        SpringSystem springSystem = SpringSystem.create();
        app = MainActivity.getApp();

        // Add a spring to the system.
        final Spring spring = springSystem.createSpring();
        SpringConfig config = new SpringConfig(TENSION, DAMPER);
        spring.setSpringConfig(config);

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

        // Set the spring in motion; moving from 0 to 1
        view.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        spring.setEndValue(1f);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (view == view.getRootView().findViewById(R.id.mainbuttonburger))
                            FragmentAnimator.pressMenuButton(app);
                        spring.setEndValue(0f);
                        return true;
                }
                return false;
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
    public static void showBouncySlide(final View view, final int direction) {
        SpringSystem springSystem = SpringSystem.create();
        app = MainActivity.getApp();

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
        }, 70);

        // Add a listener to observe the motion of the spring.
        spring.addListener(new SpringListener() {

            @Override
            public void onSpringUpdate(Spring spring) {
                // You can observe the updates in the spring
                // state by asking its current value in onSpringUpdate.

                float value = (float) spring.getCurrentValue();
                view.setX(direction * (value * -20));
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

