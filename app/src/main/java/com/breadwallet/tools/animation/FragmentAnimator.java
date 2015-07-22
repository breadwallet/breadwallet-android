package com.breadwallet.tools.animation;

import android.content.Context;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.adapter.CustomPagerAdapter;

import java.util.Stack;

/**
 * Created by Mihail on 7/13/15.
 */
public class FragmentAnimator {
    public static final String TAG = "FragmentAnimator";
    public static final MainActivity APP = MainActivity.getApp();
    public static boolean settingsAllON;
    public static boolean settingsAvailable = true;
    private static boolean animationAvailable = true;
    public static int level = 0;
    public static Stack<Fragment> previous = new Stack<>();

    public static void animateDecoderFragment() {
        APP.setDecoderFragmentOn(true);
        //Disabled inspection: <Expected resource type anim>
        FragmentTransaction fragmentTransaction = APP.getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.animator.from_bottom, R.animator.to_top);
        fragmentTransaction.replace(R.id.mainlayout, APP.getMainFragmentDecoder());
        int temp = fragmentTransaction.commit();
        Log.e(TAG, String.valueOf(temp));
    }

    /**
     * Animate the transition on burgerButton/MenuButton pressed
     */
    public static void pressMenuButton(MainActivity context) {
        if (settingsAvailable) {
            Log.d(TAG, "Inside the pressMenuButton!");
            settingsAvailable = false;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    settingsAvailable = true;
                }
            }, 300);
            if (level == 0) {
                level++;
                FragmentTransaction fragmentTransaction = context.getSupportFragmentManager().beginTransaction();
                fragmentTransaction.setCustomAnimations(R.animator.from_bottom, R.animator.from_bottom);
                fragmentTransaction.replace(R.id.mainlayout, context.getMainFragmentSettingsAll());
                fragmentTransaction.commit();
                CustomPagerAdapter.getAdapter().showFragments(false);
                context.getPageIndicator().setVisibility(View.GONE);
                InputMethodManager keyboard = (InputMethodManager) APP.
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.hideSoftInputFromWindow(CustomPagerAdapter.getAdapter().
                        getMainFragment().getAddressEditText().getWindowToken(), 0);

            } else if (level == 1) {
                level--;
                settingsAllON = false;
                FragmentTransaction fragmentTransaction = context.getSupportFragmentManager().beginTransaction();
                fragmentTransaction.setCustomAnimations(R.animator.to_bottom, R.animator.to_bottom);
                fragmentTransaction.remove(context.getMainFragmentSettingsAll());
                fragmentTransaction.commit();
                CustomPagerAdapter.getAdapter().showFragments(true);
                context.getPageIndicator().setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Animates the fragment transition on button "Settings" pressed
     */
    public static void animateSlideToLeft(MainActivity context, final Fragment to, Fragment previousFragment) {
        if (animationAvailable) {
            animationAvailable = false;
            makeAnimationAvailable(300);
            level++;
            FragmentTransaction fragmentTransaction = context.getSupportFragmentManager().beginTransaction();
            fragmentTransaction.setCustomAnimations(R.animator.from_right, R.animator.to_left);
            fragmentTransaction.replace(R.id.mainlayout, to);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    SpringAnimator.showBouncySlide(to.getView(), SpringAnimator.TO_RIGHT);
                }
            }, 200);
            previous.add(previousFragment);
            fragmentTransaction.commit();
            Log.e(TAG, "The level is: " + level);
        }
    }

    public static void animateSlideToRight(MainActivity context) {
        if (animationAvailable) {
            animationAvailable = false;
            makeAnimationAvailable(300);
            final Fragment tmp = previous.pop();
            level--;
            Log.e(TAG, "The actual SettingsFragment: " + tmp);
            FragmentTransaction fragmentTransaction = context.getSupportFragmentManager().beginTransaction();
            fragmentTransaction.setCustomAnimations(R.animator.from_left, R.animator.to_right);
            fragmentTransaction.replace(R.id.mainlayout, tmp);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    SpringAnimator.showBouncySlide(tmp.getView(), SpringAnimator.TO_LEFT);
                }
            }, 200);
            fragmentTransaction.commit();
            Log.e(TAG, "The level is: " + level);
        }
    }

    public static void makeAnimationAvailable(int delay) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                animationAvailable = true;
            }
        }, delay);
    }
}