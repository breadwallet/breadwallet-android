package com.breadwallet.tools.animation;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.adapter.CustomPagerAdapter;

import java.util.Stack;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 7/13/15.
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

public class FragmentAnimator {
    public static final String TAG = "FragmentAnimator";
    public static int level = 0;
    public static boolean wipeWalletOpen = false;
    public static Stack<Fragment> previous = new Stack<>();
    private static boolean multiplePressingAvailable = true;
    public static Object lockObject = new Object();

    public static void animateDecoderFragment() {
        MainActivity.beenThroughSavedInstanceMethod = false;
        MainActivity.app.decoderFragmentOn = true;
        InputMethodManager keyboard = (InputMethodManager) MainActivity.app.
                getSystemService(Context.INPUT_METHOD_SERVICE);
        keyboard.hideSoftInputFromWindow(CustomPagerAdapter.adapter.
                mainFragment.addressEditText.getWindowToken(), 0);
        MainActivity.app.activityButtonsEnable(false);
        CustomPagerAdapter.adapter.showFragments(false);
        //Disabled inspection: <Expected resource type anim>
        Log.e(TAG, "MainActivity.app: " + MainActivity.app);
        FragmentTransaction fragmentTransaction = MainActivity.app.getFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.animator.from_bottom, R.animator.to_top);
        fragmentTransaction.replace(R.id.main_layout, MainActivity.app.getFragmentDecoder());
        int temp = fragmentTransaction.commit();
    }

    public static void animateScanResultFragment() {
        Log.e(TAG, "animateScanResultFragment");
        MainActivity.beenThroughSavedInstanceMethod = false;
        MainActivity.app.scanResultFragmentOn = true;
        InputMethodManager keyboard = (InputMethodManager) MainActivity.app.
                getSystemService(Context.INPUT_METHOD_SERVICE);
        keyboard.hideSoftInputFromWindow(CustomPagerAdapter.adapter.
                mainFragment.addressEditText.getWindowToken(), 0);
        MainActivity.app.setBurgerButtonImage(MainActivity.app.BACK);
        //Disabled inspection: <Expected resource type anim>
        FragmentTransaction fragmentTransaction = MainActivity.app.getFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.animator.from_right, R.animator.to_left);
        fragmentTransaction.replace(R.id.main_layout, MainActivity.app.getFragmentScanResult());
        fragmentTransaction.commit();
        CustomPagerAdapter.adapter.showFragments(false);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                SpringAnimator.showBouncySlide(MainActivity.app.fragmentScanResult.getView(),
                        SpringAnimator.TO_RIGHT, 70);
            }
        }, 200);
    }

    /**
     * Animate the transition on burgerButton/MenuButton pressed
     */
    public static void pressMenuButton(final MainActivity context, final Fragment to) {
        ((BreadWalletApp) MainActivity.app.getApplication()).cancelToast();
        Log.e(TAG, "The level is: " + level);
        if (level == 0) {
            level++;
            CustomPagerAdapter.adapter.showFragments(false);
            MainActivity.app.setBurgerButtonImage(context.CLOSE);
            FragmentTransaction fragmentTransaction = context.getFragmentManager().beginTransaction();
            fragmentTransaction.setCustomAnimations(R.animator.from_top, R.animator.from_top);
            fragmentTransaction.replace(R.id.main_layout, context.fragmentSettingsAll);
            fragmentTransaction.commit();

            context.pageIndicator.setVisibility(View.GONE);
            InputMethodManager keyboard = (InputMethodManager) MainActivity.app.
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.hideSoftInputFromWindow(CustomPagerAdapter.adapter.
                    mainFragment.addressEditText.getWindowToken(), 0);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    SpringAnimator.showBouncySlideVertical(to.getView(), SpringAnimator.TO_LEFT);
                }
            }, 200);

        } else if (level == 1) {
            level--;
            MainActivity.app.setBurgerButtonImage(context.BURGER);
            FragmentTransaction fragmentTransaction = context.getFragmentManager().beginTransaction();
            fragmentTransaction.setCustomAnimations(R.animator.to_top, R.animator.to_top);
            fragmentTransaction.remove(context.fragmentSettingsAll);
            fragmentTransaction.commit();
            CustomPagerAdapter.adapter.showFragments(true);
            context.pageIndicator.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Animate the transition on wipe wallet fragment
     */
    public static void pressWipeWallet(final MainActivity context, final Fragment to) {
        if (!wipeWalletOpen) {
            wipeWalletOpen = true;
            FragmentTransaction fragmentTransaction = context.getFragmentManager().beginTransaction();
            fragmentTransaction.setCustomAnimations(R.animator.from_bottom, R.animator.to_top);
            fragmentTransaction.replace(R.id.main_layout, to);
            fragmentTransaction.commit();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    SpringAnimator.showBouncySlideVertical(MainActivity.app.fragmentWipeWallet.getView(),
                            SpringAnimator.TO_RIGHT);
                }
            }, 200);

        } else {
            wipeWalletOpen = false;
            FragmentTransaction fragmentTransaction = context.getFragmentManager().beginTransaction();
            fragmentTransaction.setCustomAnimations(R.animator.from_top, R.animator.to_bottom);
            fragmentTransaction.replace(R.id.main_layout, context.fragmentSettings);
            fragmentTransaction.commit();
        }
    }

    /**
     * Animates the fragment transition on button_regular_blue "Settings" pressed
     */
    public static void animateSlideToLeft(MainActivity context, final Fragment to, Fragment previousFragment) {
        level++;
        if (level > 1)
            context.setBurgerButtonImage(context.BACK);
        FragmentTransaction fragmentTransaction = context.getFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.animator.from_right, R.animator.to_left);
        fragmentTransaction.replace(R.id.main_layout, to);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                SpringAnimator.showBouncySlide(to.getView(), SpringAnimator.TO_RIGHT, 20);
            }
        }, 200);
        previous.add(previousFragment);
        fragmentTransaction.commit();
        Log.e(TAG, "The level is: " + level);
    }

    public static void animateSlideToRight(MainActivity context) {
        final Fragment tmp = previous.pop();
        level--;
        if (level < 1)
            context.setBurgerButtonImage(context.BURGER);
        if (level == 1)
            context.setBurgerButtonImage(context.CLOSE);
//            Log.e(TAG, "The actual SettingsFragment: " + tmp);
        FragmentTransaction fragmentTransaction = context.getFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.animator.from_left, R.animator.to_right);
        fragmentTransaction.replace(R.id.main_layout, tmp);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                SpringAnimator.showBouncySlide(tmp.getView(), SpringAnimator.TO_LEFT, 20);
            }
        }, 200);
        fragmentTransaction.commit();
        Log.e(TAG, "The level is: " + level);
    }


    /**
     * Checks if the multipressing is available and if available:
     * - make it unavailable for delay milliseconds and return true,
     * else :
     * - return false.
     *
     * @param delay in milliseconds
     */
    public static boolean checkTheMultipressingAvailability(int delay) {
        synchronized (lockObject) {
            if (multiplePressingAvailable) {
                multiplePressingAvailable = false;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        multiplePressingAvailable = true;
                        Log.w(TAG, "multiplePressingAvailable is back to - true");
                    }
                }, delay);
                return true;
            } else {
                return false;
            }
        }
    }

    public static void hideDecoderFragment() {
        MainActivity.app.decoderFragmentOn = false;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                FragmentAnimator.multiplePressingAvailable = true;
            }
        }, 300);
        MainActivity.app.getFragmentManager().beginTransaction().
                setCustomAnimations(R.animator.from_top, R.animator.to_bottom).
                remove(MainActivity.app.mainFragmentDecoder).commit();
        CustomPagerAdapter.adapter.showFragments(true);
        MainActivity.app.activityButtonsEnable(true);
        MainActivity.app.mainFragmentDecoder = null;
    }

    public static void hideScanResultFragment() {
        Log.e(TAG, "hideScanResultFragment");
        CustomPagerAdapter.adapter.showFragments(true);
        MainActivity.app.scanResultFragmentOn = false;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                FragmentAnimator.multiplePressingAvailable = true;
            }
        }, 300);

        MainActivity.app.getFragmentManager().beginTransaction().
                setCustomAnimations(R.animator.from_left, R.animator.to_right).
                remove(MainActivity.app.fragmentScanResult).commit();
        MainActivity.app.setBurgerButtonImage(MainActivity.app.BURGER);
    }

}