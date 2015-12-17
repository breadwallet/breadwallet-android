package com.breadwallet.tools.animation;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.fragments.FragmentDecoder;
import com.breadwallet.presenter.fragments.FragmentScanResult;
import com.breadwallet.presenter.fragments.FragmentSettings;
import com.breadwallet.presenter.fragments.FragmentSettingsAll;
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
    public static final Object lockObject = new Object();

    public static void animateDecoderFragment() {
        MainActivity app = MainActivity.app;
        if (app == null) return;
        if (FragmentAnimator.level > 0)
            FragmentAnimator.pressMenuButton(app, new FragmentSettingsAll());
        Log.e(TAG, "in the animateDecoderFragment");
        MainActivity.beenThroughSavedInstanceMethod = false;
        app.decoderFragmentOn = true;
        app.activityButtonsEnable(false);
        CustomPagerAdapter.adapter.showFragments(false);
        //Disabled inspection: <Expected resource type anim>
        FragmentTransaction fragmentTransaction = MainActivity.app.getFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.animator.from_bottom, R.animator.to_top);
        fragmentTransaction.replace(R.id.main_layout, new FragmentDecoder(), FragmentDecoder.class.getName());
        int temp = fragmentTransaction.commit();
    }

    public static void animateScanResultFragment() {
        CustomPagerAdapter.adapter.showFragments(false);
        Log.e(TAG, "animateScanResultFragment");
        MainActivity.beenThroughSavedInstanceMethod = false;
        MainActivity.app.scanResultFragmentOn = true;
        InputMethodManager keyboard = (InputMethodManager) MainActivity.app.
                getSystemService(Context.INPUT_METHOD_SERVICE);
        keyboard.hideSoftInputFromWindow(CustomPagerAdapter.adapter.
                mainFragment.addressEditText.getWindowToken(), 0);
        MainActivity.app.setBurgerButtonImage(MainActivity.app.BACK);
        //Disabled inspection: <Expected resource type anim>
        final FragmentManager fragmentManager = MainActivity.app.getFragmentManager();
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        fragmentTransaction.setCustomAnimations(R.animator.from_right, R.animator.to_left);
        fragmentTransaction.replace(R.id.main_layout, new FragmentScanResult(), FragmentScanResult.class.getName());
        fragmentTransaction.commit();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                FragmentScanResult fragmentScanResult = (FragmentScanResult) fragmentManager.
                        findFragmentByTag(FragmentScanResult.class.getName());
                SpringAnimator.showBouncySlideHorizontal(fragmentScanResult.getView(),
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
        FragmentManager fragmentManager = context.getFragmentManager();
        if (level == 0) {
            level++;
            CustomPagerAdapter.adapter.showFragments(false);
            MainActivity.app.setBurgerButtonImage(context.CLOSE);
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.setCustomAnimations(R.animator.from_bottom, R.animator.to_top);
            fragmentTransaction.add(R.id.main_layout, to, FragmentSettingsAll.class.getName());
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
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.setCustomAnimations(R.animator.from_top, R.animator.to_bottom);
            FragmentSettingsAll fragmentSettingsAll = (FragmentSettingsAll) fragmentManager.
                    findFragmentByTag(FragmentSettingsAll.class.getName());
            fragmentTransaction.remove(fragmentSettingsAll);
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
            fragmentTransaction.replace(R.id.main_layout, to, to.getClass().getName());
            fragmentTransaction.commit();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    SpringAnimator.showBouncySlideVertical(to.getView(),
                            SpringAnimator.TO_RIGHT);
                }
            }, 200);

        } else {
            wipeWalletOpen = false;
            FragmentTransaction fragmentTransaction = context.getFragmentManager().beginTransaction();
            fragmentTransaction.setCustomAnimations(R.animator.from_top, R.animator.to_bottom);
            fragmentTransaction.replace(R.id.main_layout, new FragmentSettings(), FragmentSettings.class.getName());
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
        fragmentTransaction.replace(R.id.main_layout, to, to.getClass().getName());
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                SpringAnimator.showBouncySlideHorizontal(to.getView(), SpringAnimator.TO_RIGHT, 20);
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
        fragmentTransaction.replace(R.id.main_layout, tmp, tmp.getClass().getName());
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                SpringAnimator.showBouncySlideHorizontal(tmp.getView(), SpringAnimator.TO_LEFT, 20);
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
        Log.e(TAG, "multiplePressingAvailable: " + multiplePressingAvailable);
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
        Log.e(TAG, "hideDecoderFragment");
        MainActivity app = MainActivity.app;
        if(app == null) return;
        app.decoderFragmentOn = false;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                FragmentAnimator.multiplePressingAvailable = true;
            }
        }, 300);
        final FragmentManager fragmentManager = app.getFragmentManager();
        FragmentDecoder fragmentDecoder = (FragmentDecoder) fragmentManager.findFragmentByTag(FragmentDecoder.class.getName());
        if (fragmentDecoder == null || fragmentManager == null) return;
        fragmentManager.beginTransaction().setCustomAnimations(R.animator.from_top, R.animator.to_bottom).
                remove(fragmentDecoder).commit();
        CustomPagerAdapter.adapter.showFragments(true);
        app.activityButtonsEnable(true);
        Log.e(TAG, "got to the end of hideDecoderFragment");
    }

    public static void hideScanResultFragment() {
        MainActivity app = MainActivity.app;
        if (app == null) return;
        Log.e(TAG, "hideScanResultFragment");
        CustomPagerAdapter.adapter.showFragments(true);
        app.scanResultFragmentOn = false;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                FragmentAnimator.multiplePressingAvailable = true;
            }
        }, 300);

        app.getFragmentManager().beginTransaction().
                setCustomAnimations(R.animator.from_left, R.animator.to_right).
                remove(new FragmentScanResult()).commit();
        app.setBurgerButtonImage(app.BURGER);
    }

}