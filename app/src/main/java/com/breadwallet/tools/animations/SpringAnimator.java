package com.breadwallet.tools.animations;

import android.os.Handler;
import android.util.Log;
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
                            app.animateSettingsFragment();
                        spring.setEndValue(0f);
                        return true;
                }
                return false;
            }

        });
    }

    public static void showBouncySlide(final View view) {
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
        }, 100);

        // Add a listener to observe the motion of the spring.
        spring.addListener(new SpringListener() {

            @Override
            public void onSpringUpdate(Spring spring) {
                // You can observe the updates in the spring
                // state by asking its current value in onSpringUpdate.

                float value = (float) spring.getCurrentValue();
                view.setX(value * -10);
                Log.d(TAG, "Update X: " + value);
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
