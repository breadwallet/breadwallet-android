package com.breadwallet.tools.animation;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.fragments.FragmentDecoder;
import com.breadwallet.presenter.fragments.FragmentScanResult;
import com.breadwallet.presenter.fragments.FragmentSettings;
import com.breadwallet.presenter.fragments.FragmentSettingsAll;
import com.breadwallet.presenter.fragments.MainFragment;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.adapter.CustomPagerAdapter;

import java.util.Stack;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on 7/13/15.
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

public class BRAnimator {
    private static final String TAG = BRAnimator.class.getName();
    public static boolean decoderFragmentOn;
    public static boolean scanResultFragmentOn;
    public static int level = 0;
    public static boolean wipeWalletOpen = false;
    private static Stack<Fragment> previous = new Stack<>();
    private static boolean multiplePressingAvailable = true;
    //    private static int VERTICAL_BOUNCE_DELAY = 60;
    public static int horizontalSlideDuration = 300;
    private static boolean horizontalSlideAvailable = true;
    private static View copy;
//    private static final Object lockObject = new Object();

    public static void animateDecoderFragment() {

        try {
            MainActivity app = MainActivity.app;
            if (app == null) return;

            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(app,
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(app,
                        Manifest.permission.CAMERA)) {
                    Log.e(TAG, "YES explanation!");
                    ((BreadWalletApp) app.getApplication()).showCustomToast(app,
                            app.getString(R.string.allow_camera_access),
                            MainActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG, 0);
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                } else {
                    Log.e(TAG, "NO explanation!");
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(app,
                            new String[]{Manifest.permission.CAMERA},
                            BRConstants.CAMERA_REQUEST_ID);
                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            } else {
                if (BRAnimator.level > 0)
                    BRAnimator.pressMenuButton(app);
                //            Log.e(TAG, "in the animateDecoderFragment");
                //            MainActivity.beenThroughSavedInstanceMethod = false;
                decoderFragmentOn = true;
                app.activityButtonsEnable(false);
                CustomPagerAdapter.adapter.showFragments(false);
                //Disabled inspection: <Expected resource type anim>
                FragmentTransaction fragmentTransaction = app.getFragmentManager().beginTransaction();
                fragmentTransaction.setCustomAnimations(R.animator.from_bottom, R.animator.to_top);
                FragmentDecoder fragmentDecoder = new FragmentDecoder();
                fragmentTransaction.replace(R.id.main_layout, fragmentDecoder, FragmentDecoder.class.getName());
                int temp = fragmentTransaction.commit();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void animateScanResultFragment() {
        try {
            final MainActivity app = MainActivity.app;
            if (app == null) return;
            CustomPagerAdapter.adapter.showFragments(false);
//        Log.e(TAG, "animateScanResultFragment");
//        MainActivity.beenThroughSavedInstanceMethod = false;
            scanResultFragmentOn = true;
            InputMethodManager keyboard = (InputMethodManager) app.
                    getSystemService(Context.INPUT_METHOD_SERVICE);

            try {
                keyboard.hideSoftInputFromWindow(CustomPagerAdapter.adapter.
                        mainFragment.addressEditText.getWindowToken(), 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
            app.setBurgerButtonImage(BRConstants.BACK);
            //Disabled inspection: <Expected resource type anim>
            final FragmentManager fragmentManager = app.getFragmentManager();
            final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

//        fragmentTransaction.setCustomAnimations(R.animator.from_right, R.animator.to_left);
            final FragmentScanResult scanResult = new FragmentScanResult();
            fragmentTransaction.replace(R.id.main_layout, scanResult, FragmentScanResult.class.getName());
            fragmentTransaction.commit();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    TranslateAnimation trans = new TranslateAnimation(app.getResources().getInteger(R.integer.standard_screen_width), 0, 0, 0);
                    trans.setDuration(500);
                    trans.setInterpolator(new DecelerateOvershootInterpolator(3f, 0.5f));
                    View view = scanResult.getView();
                    Log.e(TAG, "startAnimation");
                    if (view != null)
                        view.startAnimation(trans);
                }
            }, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Animate the transition on burgerButton/MenuButton pressed
     */
    public static void pressMenuButton(final MainActivity context) {
        try {
            if (context == null) return;
            ((BreadWalletApp) context.getApplication()).cancelToast();
//        Log.e(TAG, "The level is: " + level);
            final FragmentManager fragmentManager = context.getFragmentManager();
            if (level == 0) {
                level++;
                CustomPagerAdapter.adapter.showFragments(false);
                context.setBurgerButtonImage(BRConstants.CLOSE);
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                //            fragmentTransaction.setCustomAnimations(R.animator.from_bottom, 0);
                FragmentSettingsAll to = (FragmentSettingsAll) fragmentManager.
                        findFragmentByTag(FragmentSettingsAll.class.getName());
                if (to == null) to = new FragmentSettingsAll();
                fragmentTransaction.add(R.id.main_layout, to, FragmentSettingsAll.class.getName());
                fragmentTransaction.commit();
                final FragmentSettingsAll finalTo = to;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        TranslateAnimation trans = new TranslateAnimation(0, 0, 1920, 0);
                        trans.setDuration(500);
                        trans.setInterpolator(new DecelerateOvershootInterpolator(3f, 0.5f));
                        View view = finalTo.getView();
                        Log.e(TAG, "startAnimation");
                        if (view != null)
                            view.startAnimation(trans);
                    }
                }, 1);

                InputMethodManager keyboard = (InputMethodManager) context.
                        getSystemService(Context.INPUT_METHOD_SERVICE);

                if (keyboard != null)
                    keyboard.hideSoftInputFromWindow(CustomPagerAdapter.adapter.
                            mainFragment.addressEditText.getWindowToken(), 0);
            } else if (level == 1) {
                level--;
                context.setBurgerButtonImage(BRConstants.BURGER);
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.setCustomAnimations(R.animator.from_top, R.animator.to_bottom);
                FragmentSettingsAll fragmentSettingsAll = (FragmentSettingsAll) fragmentManager.
                        findFragmentByTag(FragmentSettingsAll.class.getName());
                fragmentTransaction.remove(fragmentSettingsAll);
                fragmentTransaction.commit();
                CustomPagerAdapter.adapter.showFragments(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Animate the transition on wipe wallet fragment
     */
    public static void pressWipeWallet(final MainActivity context, final Fragment to) {
        try {
            if (!wipeWalletOpen) {
                wipeWalletOpen = true;
                FragmentTransaction fragmentTransaction = context.getFragmentManager().beginTransaction();
                //            fragmentTransaction.setCustomAnimations(R.animator.from_bottom, R.animator.to_top);
                fragmentTransaction.replace(R.id.main_layout, to, to.getClass().getName());
                fragmentTransaction.commit();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        TranslateAnimation trans = new TranslateAnimation(0, 0, 1920, 0);
                        trans.setDuration(500);
                        trans.setInterpolator(new DecelerateOvershootInterpolator(3f, 0.5f));
                        View view = to.getView();
                        Log.e(TAG, "startAnimation");
                        if (view != null)
                            view.startAnimation(trans);
                    }
                }, 1);

            } else {
                wipeWalletOpen = false;
                FragmentTransaction fragmentTransaction = context.getFragmentManager().beginTransaction();
                fragmentTransaction.setCustomAnimations(R.animator.from_top, R.animator.to_bottom);
                fragmentTransaction.replace(R.id.main_layout, new FragmentSettings(), FragmentSettings.class.getName());
                fragmentTransaction.commit();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Animates the fragment transition on button_regular_blue "Settings" pressed
     */
    public static void animateSlideToLeft(final MainActivity context, final Fragment to, Fragment previousFragment) {
        try {
            if (!checkTheHorizontalSlideAvailability()) return;
            level++;
            if (level > 1)
                context.setBurgerButtonImage(BRConstants.BACK);
            FragmentTransaction fragmentTransaction = context.getFragmentManager().beginTransaction();
//        fragmentTransaction.setCustomAnimations(R.animator.from_right, R.animator.to_left);
            fragmentTransaction.replace(R.id.main_layout, to, to.getClass().getName());
            if (previousFragment != null)
                previous.add(previousFragment);
            fragmentTransaction.commit();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    TranslateAnimation trans = new TranslateAnimation(context.getResources().getInteger(R.integer.standard_screen_width), 0, 0, 0);
                    trans.setDuration(horizontalSlideDuration);
                    trans.setInterpolator(new DecelerateOvershootInterpolator(1f, 0.5f));
                    View view = to.getView();
                    Log.e(TAG, "startAnimation");
                    if (view != null)
                        view.startAnimation(trans);
                }
            }, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }

//        Log.e(TAG, "The level is: " + level);
    }

    public static void animateSlideToRight(final MainActivity context) {
        try {
            if (!checkTheHorizontalSlideAvailability()) return;
            final Fragment tmp = previous.pop();
            level--;
            if (level < 1)
                context.setBurgerButtonImage(BRConstants.BURGER);
            if (level == 1)
                context.setBurgerButtonImage(BRConstants.CLOSE);
//            Log.e(TAG, "The actual SettingsFragment: " + tmp);
            FragmentTransaction fragmentTransaction = context.getFragmentManager().beginTransaction();
//        fragmentTransaction.setCustomAnimations(R.animator.from_left, R.animator.to_right);
            fragmentTransaction.replace(R.id.main_layout, tmp, tmp.getClass().getName());
            fragmentTransaction.commit();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    TranslateAnimation trans = new TranslateAnimation(-context.getResources().getInteger(R.integer.standard_screen_width), 0, 0, 0);
                    trans.setDuration(horizontalSlideDuration);
                    trans.setInterpolator(new DecelerateOvershootInterpolator(1f, 0.5f));
                    View view = tmp.getView();
                    Log.e(TAG, "startAnimation");
                    if (view != null)
                        view.startAnimation(trans);
                }
            }, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }

//        Log.e(TAG, "The level is: " + level);
    }


    /**
     * Checks if the multipressing is available and if available:
     * - make it unavailable for delay milliseconds and return true,
     * else :
     * - return false.
     */
    public static boolean checkTheMultipressingAvailability() {
        if (multiplePressingAvailable) {
            multiplePressingAvailable = false;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    multiplePressingAvailable = true;
                    Log.w(TAG, "multiplePressingAvailable is back to - true");
                }
            }, 300);
            return true;
        } else {
            return false;
        }
    }

    public static boolean checkTheHorizontalSlideAvailability() {
        if (horizontalSlideAvailable) {
            horizontalSlideAvailable = false;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    horizontalSlideAvailable = true;
                    Log.w(TAG, "multiplePressingAvailable is back to - true");
                }
            }, horizontalSlideDuration);
            return true;
        } else {
            return false;
        }
    }

    public static void hideDecoderFragment() {
//        Log.e(TAG, "hideDecoderFragment");
        try {
            MainActivity app = MainActivity.app;
            if (app == null) return;
            decoderFragmentOn = false;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    BRAnimator.multiplePressingAvailable = true;
                }
            }, 300);
            final FragmentManager fragmentManager = app.getFragmentManager();
            FragmentDecoder fragmentDecoder = (FragmentDecoder) fragmentManager.findFragmentByTag(FragmentDecoder.class.getName());
            if (fragmentDecoder == null) return;
            fragmentManager.beginTransaction().setCustomAnimations(R.animator.from_top, R.animator.to_bottom).
                    remove(fragmentDecoder).commit();
            CustomPagerAdapter.adapter.showFragments(true);
            app.activityButtonsEnable(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void hideScanResultFragment() {
        try {
            MainActivity app = MainActivity.app;
            if (app == null) return;
//        Log.e(TAG, "hideScanResultFragment");
            CustomPagerAdapter.adapter.showFragments(true);
            scanResultFragmentOn = false;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    multiplePressingAvailable = true;
                }
            }, 300);

            FragmentManager fragmentManager = app.getFragmentManager();
            FragmentScanResult fragmentScanResult = (FragmentScanResult)
                    fragmentManager.findFragmentByTag(FragmentScanResult.class.getName());
            if (fragmentScanResult == null) return;
            fragmentManager.beginTransaction().
                    setCustomAnimations(R.animator.from_left, R.animator.to_right).
                    remove(fragmentScanResult).commit();
            app.setBurgerButtonImage(BRConstants.BURGER);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void resetFragmentAnimator() {
        level = 0;
        wipeWalletOpen = false;
        previous.clear();
        multiplePressingAvailable = true;
    }

    public static void goToMainActivity(final Fragment fragment) {
        try {
            final MainActivity app = MainActivity.app;
            if (app == null) return;
            if (fragment != null) {
                app.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        FragmentManager fragmentManager = app.getFragmentManager();
                        fragmentManager.beginTransaction().setCustomAnimations(R.animator.from_top, R.animator.to_bottom).
                                remove(fragment).commit();
                        CustomPagerAdapter.adapter.showFragments(true);
                        app.activityButtonsEnable(true);
                        app.setBurgerButtonImage(0);
                    }
                });

            }
            resetFragmentAnimator();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void showCopyBubble(final Activity context, final View v, final View t) {
        try {
            if (context == null) return;
            if (v != null)
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (copy == null)
                            copy = context.getLayoutInflater().inflate(R.layout.copy, null);
                        if (copy == null) return;
                        final RelativeLayout root = (RelativeLayout) context.findViewById(R.id.main_layout);
                        root.removeView(copy);
                        copy.setClickable(true);
                        copy.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                try {
                                    if (t != null) {
                                        BRClipboardManager.copyToClipboard(context, ((TextView) t).getText().toString());
                                        Log.e(TAG, "clicked copy: " + ((TextView) t).getText().toString());
                                    }
                                    hideCopyBubble(context);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        root.addView(copy);
                        copy.setY(getRelativeTop(v));
                        copy.setX(MainActivity.screenParametersPoint.x / 2 - 40);
                    }
                });
            if (t != null)
                t.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        View parent = (View) t.getParent();
                        if (parent != null)
                            parent.performClick();
                    }
                });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void hideCopyBubble(final Activity context) {
        try {
            if (context == null) return;
            if (copy == null) return;
            final RelativeLayout root = (RelativeLayout) context.findViewById(R.id.main_layout);
            if (copy.getVisibility() == View.VISIBLE) {
                Animation animation = new AlphaAnimation(1f, 0f);
                animation.setDuration(150);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        root.removeView(copy);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                copy.startAnimation(animation);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void fadeScaleBubble(final View... views) {
        if (views == null || views.length == 0) return;
        for (final View v : views) {
            if (v == null || v.getVisibility() != View.VISIBLE) continue;
            Animation animation = new AlphaAnimation(1f, 0f);
            animation.setDuration(150);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    v.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            v.startAnimation(animation);
        }

    }

    public static void scaleView(View v, float startScaleX, float endScaleX, float startScaleY, float endScaleY) {
        Animation anim = new ScaleAnimation(
                startScaleX, endScaleX, // Start and end values for the X axis scaling
                startScaleY, endScaleY, // Start and end values for the Y axis scaling
                Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
        anim.setFillAfter(true); // Needed to keep the result of the animation
        v.startAnimation(anim);
    }

    private static int getRelativeLeft(View myView) {
        if (myView.getParent() == myView.getRootView())
            return myView.getLeft();
        else
            return myView.getLeft() + getRelativeLeft((View) myView.getParent());
    }

    public static int getRelativeTop(View myView) {
        if (myView == null) return 0;
        if (myView.getParent() == myView.getRootView())
            return myView.getTop();
        else
            return myView.getTop() + getRelativeTop((View) myView.getParent());
    }

}