package com.breadwallet.tools.animation;

import android.animation.Animator;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;

import com.breadwallet.R;
import com.breadwallet.legacy.presenter.activities.DisabledActivity;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.ui.MainActivity;
import com.breadwallet.ui.browser.PlatformBrowserActivity;
import com.breadwallet.ui.wallet.WalletController;
import com.platform.HTTPServer;

import java.util.List;

import static android.content.Context.ACTIVITY_SERVICE;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/13/15.
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

public class UiUtils {
    private static final String TAG = UiUtils.class.getName();
    public static final int CLICK_PERIOD_ALLOWANCE = 300;
    public static final String ARTICLE_QUERY_STRING = "/article?slug=";
    public static final String CURRENCY_QUERY_STRING = "&currency=";
    private static long mLastClickTime = 0;
    private static boolean mSupportIsShowing;

    public static void setIsSupportFragmentShown(boolean isSupportFragmentShown) {
        mSupportIsShowing = isSupportFragmentShown;
    }

    public static LayoutTransition getDefaultTransition() {
        LayoutTransition itemLayoutTransition = new LayoutTransition();
        itemLayoutTransition.setStartDelay(LayoutTransition.APPEARING, 0);
        itemLayoutTransition.setStartDelay(LayoutTransition.DISAPPEARING, 0);
        itemLayoutTransition.setStartDelay(LayoutTransition.CHANGE_APPEARING, 0);
        itemLayoutTransition.setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0);
        itemLayoutTransition.setStartDelay(LayoutTransition.CHANGING, 0);
        itemLayoutTransition.setDuration(100);
        itemLayoutTransition.setInterpolator(LayoutTransition.CHANGING, new OvershootInterpolator(2f));
        Animator scaleUp = ObjectAnimator.ofPropertyValuesHolder((Object) null, PropertyValuesHolder.ofFloat(View.SCALE_X, 1, 1), PropertyValuesHolder.ofFloat(View.SCALE_Y, 0, 1));
        scaleUp.setDuration(50);
        scaleUp.setStartDelay(50);
        Animator scaleDown = ObjectAnimator.ofPropertyValuesHolder((Object) null, PropertyValuesHolder.ofFloat(View.SCALE_X, 1, 1), PropertyValuesHolder.ofFloat(View.SCALE_Y, 1, 0));
        scaleDown.setDuration(2);
        itemLayoutTransition.setAnimator(LayoutTransition.APPEARING, scaleUp);
        itemLayoutTransition.setAnimator(LayoutTransition.DISAPPEARING, null);
        itemLayoutTransition.enableTransitionType(LayoutTransition.CHANGING);
        return itemLayoutTransition;
    }

    public static boolean isClickAllowed() {
        boolean allow = false;
        if (System.currentTimeMillis() - mLastClickTime > CLICK_PERIOD_ALLOWANCE) {
            allow = true;
        }
        mLastClickTime = System.currentTimeMillis();
        return allow;
    }

    public static void killAllFragments(Activity app) {
        if (app != null && !app.isDestroyed()) {
            app.getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    public static void startBreadActivity(Activity from, boolean auth) {
        if (from == null) {
            return;
        }
        Class toStart = auth ? MainActivity.class : WalletController.class;

        // If this is a first launch(new wallet), ensure that we are starting on the Home Screen
        if (toStart.equals(WalletController.class)) {

            if (BRSharedPrefs.isNewWallet(from)) {
                toStart = MainActivity.class;
            }
        }

        Intent intent = new Intent(from, toStart);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        from.startActivity(intent);
    }

    private static void setStatusBarColor(Activity app, int color) {
        if (app == null) return;
        Window window = app.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(app.getColor(color));
    }

    /**
     * Opens WebView with platform formatted rewards url.
     * Reuse this method in Preferences.
     *
     * @param activity The Activity.
     */
    public static void openRewardsWebView(Activity activity) {
        startPlatformBrowser(activity, HTTPServer.getPlatformUrl(HTTPServer.URL_REWARDS), R.anim.enter_from_right,
                R.anim.empty_300, R.anim.fade_up, R.anim.exit_to_right);
    }

    public static void showWalletDisabled(Activity app) {
        Intent intent = new Intent(app, DisabledActivity.class);
        app.startActivity(intent);
        app.overridePendingTransition(R.anim.fade_up, R.anim.fade_down);
        Log.e(TAG, "showWalletDisabled: " + app.getClass().getName());

    }

    public static boolean isLast(Activity app) {
        ActivityManager mngr = (ActivityManager) app.getSystemService(ACTIVITY_SERVICE);

        List<ActivityManager.RunningTaskInfo> taskList = mngr.getRunningTasks(10);

        if (taskList.get(0).numActivities == 1 &&
                taskList.get(0).topActivity.getClassName().equals(app.getClass().getName())) {
            return true;
        }
        return false;
    }

    public static void startPlatformBrowser(Activity activity, String url) {
        startPlatformBrowser(activity, url, R.anim.enter_from_bottom, R.anim.fade_down, 0, 0);
    }

    private static void startPlatformBrowser(Activity activity, String url, int enterAnimation,
                                             int exitAnimation, int returnEnterAnimation, int returnExitAnimation) {
        PlatformBrowserActivity.Companion.start(activity, url, returnEnterAnimation, returnExitAnimation);
        if (enterAnimation != 0 && exitAnimation != 0) {
            activity.overridePendingTransition(enterAnimation, exitAnimation);
        }
    }

    public static boolean isMainThread() {
        boolean isMain = Looper.myLooper() == Looper.getMainLooper();
        if (isMain) {
            Log.e(TAG, "IS MAIN UI THREAD!");
        }
        return isMain;
    }

    public static int getThemeId(Activity activity) {
        try {
            return activity.getPackageManager().getActivityInfo(activity.getComponentName(), 0).getThemeResource();
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error finding theme for this Activity -> " + activity.getLocalClassName());
        }

        return 0;
    }
}
