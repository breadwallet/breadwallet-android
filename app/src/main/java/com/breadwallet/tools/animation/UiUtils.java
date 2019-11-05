package com.breadwallet.tools.animation;

import android.Manifest;
import android.animation.Animator;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;

import com.breadwallet.R;
import com.breadwallet.legacy.presenter.activities.DisabledActivity;
import com.breadwallet.legacy.presenter.activities.camera.ScanQRActivity;
import com.breadwallet.legacy.presenter.customviews.BRDialogView;
import com.breadwallet.legacy.presenter.fragments.FragmentSignal;
import com.breadwallet.legacy.presenter.fragments.FragmentWebModal;
import com.breadwallet.legacy.presenter.interfaces.BROnSignalCompletion;
import com.breadwallet.legacy.wallet.WalletsMaster;
import com.breadwallet.legacy.wallet.abstracts.BaseWalletManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
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

    public static void showBreadSignal(Activity activity, String title, String iconDescription, int drawableId, BROnSignalCompletion completion) {
        FragmentSignal mFragmentSignal = new FragmentSignal();
        Bundle bundle = new Bundle();
        bundle.putString(FragmentSignal.TITLE, title);
        bundle.putString(FragmentSignal.ICON_DESCRIPTION, iconDescription);
        mFragmentSignal.setCompletion(completion);
        bundle.putInt(FragmentSignal.RES_ID, drawableId);
        mFragmentSignal.setArguments(bundle);
        FragmentTransaction transaction = activity.getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.animator.from_bottom, R.animator.to_bottom, R.animator.from_bottom, R.animator.to_bottom);
        transaction.add(android.R.id.content, mFragmentSignal, mFragmentSignal.getClass().getName());
        transaction.addToBackStack(null);
        if (!activity.isDestroyed()) {
            transaction.commit();
        }
    }

    public static void setIsSupportFragmentShown(boolean isSupportFragmentShown) {
        mSupportIsShowing = isSupportFragmentShown;
    }

    public static void showSupportFragment(FragmentActivity fragmentActivity, String articleId, BaseWalletManager walletManager) {
        if (mSupportIsShowing) {
            return;
        }
        try {
            mSupportIsShowing = true;
            if (fragmentActivity == null) {
                Log.e(TAG, "showSupportFragment: app is null");
                return;
            }

            StringBuilder urlBuilder = new StringBuilder().append(HTTPServer.getPlatformUrl(HTTPServer.URL_SUPPORT));
            if (!Utils.isNullOrEmpty(articleId)) {
                urlBuilder.append(ARTICLE_QUERY_STRING);
                urlBuilder.append(articleId);

                // If no wallet is provided, we don't need to pass the current code as a parameter.
                String currencyQuery;
                if (walletManager == null) {
                    currencyQuery = "";
                } else {
                    String currencyCode = walletManager.getCurrencyCode();
                    WalletsMaster walletsMaster = WalletsMaster.getInstance();
                    if (walletsMaster.isCurrencyCodeErc20(fragmentActivity.getApplicationContext(), currencyCode)) {
                        currencyCode = BRConstants.CURRENCY_ERC20;
                    }
                    currencyQuery = CURRENCY_QUERY_STRING + currencyCode;
                }
                urlBuilder.append(currencyQuery.toLowerCase());
            }

            showWebModal(fragmentActivity, urlBuilder.toString());
        } finally {
            mSupportIsShowing = false;
        }

    }

    public static void showWebModal(FragmentActivity fragmentActivity, String url) {
        FragmentWebModal fragmentSupport = (FragmentWebModal) fragmentActivity.getSupportFragmentManager()
                .findFragmentByTag(FragmentWebModal.class.getName());
        if (fragmentSupport != null && fragmentSupport.isAdded()) {
            fragmentActivity.getFragmentManager().popBackStack();
            return;
        }
        fragmentSupport = new FragmentWebModal();
        Bundle bundle = new Bundle();
        bundle.putString(FragmentWebModal.EXTRA_URL, url);

        fragmentSupport.setArguments(bundle);
        /*fragmentActivity.getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(0, 0, 0, R.animator.plain_300)
                .add(android.R.id.content, fragmentSupport, SendSheetController.class.getName())
                .addToBackStack(SendSheetController.class.getName()).commit();*/

    }

    public static void openScanner(Activity app) {
        openScanner(app, 0);
    }

    public static void openScanner(Activity app, int requestCode) {
        try {
            if (app == null) {
                return;
            }

            // Check if the camera permission is granted
            if (ContextCompat.checkSelfPermission(app,
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(app,
                        Manifest.permission.CAMERA)) {
                    BRDialog.showCustomDialog(app, app.getString(R.string.Send_cameraUnavailabeTitle_android),
                            app.getString(R.string.Send_cameraUnavailabeMessage_android), app.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                                @Override
                                public void onClick(BRDialogView brDialogView) {
                                    brDialogView.dismiss();
                                }
                            }, null, null, 0);
                } else {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(app,
                            new String[]{Manifest.permission.CAMERA},
                            BRConstants.CAMERA_REQUEST_ID);
                }
            } else {
                // Permission is granted, open camera
                Intent intent = new Intent(app, ScanQRActivity.class);
                if (requestCode == 0) {
                    app.startActivity(intent);
                } else {
                    app.startActivityForResult(intent, requestCode);
                }
                app.overridePendingTransition(R.anim.fade_up, R.anim.fade_down);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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
