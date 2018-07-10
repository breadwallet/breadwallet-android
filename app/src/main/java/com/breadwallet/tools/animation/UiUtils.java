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
import com.breadwallet.presenter.activities.DisabledActivity;
import com.breadwallet.presenter.activities.HomeActivity;
import com.breadwallet.presenter.activities.LoginActivity;
import com.breadwallet.presenter.activities.WalletActivity;
import com.breadwallet.presenter.activities.camera.ScanQRActivity;
import com.breadwallet.presenter.activities.settings.WebViewActivity;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.entities.CryptoRequest;
import com.breadwallet.presenter.entities.TxUiHolder;
import com.breadwallet.presenter.fragments.FragmentReceive;
import com.breadwallet.presenter.fragments.FragmentRequestAmount;
import com.breadwallet.presenter.fragments.FragmentSend;
import com.breadwallet.presenter.fragments.FragmentSignal;
import com.breadwallet.presenter.fragments.FragmentSupport;
import com.breadwallet.presenter.fragments.FragmentTxDetails;
import com.breadwallet.presenter.interfaces.BROnSignalCompletion;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.bitcoin.BaseBitcoinWalletManager;

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

    public static void showSendFragment(FragmentActivity app, final CryptoRequest request) {
        if (app == null) {
            Log.e(TAG, "showSendFragment: app is null");
            return;
        }

        FragmentSend fragmentSend = (FragmentSend) app.getSupportFragmentManager().findFragmentByTag(FragmentSend.class.getName());
        if (fragmentSend == null) {
            fragmentSend = new FragmentSend();
        }
        fragmentSend.saveViewModelData(request);
        if (!fragmentSend.isAdded()) {
            app.getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(0, 0, 0, R.animator.plain_300)
                    .add(android.R.id.content, fragmentSend, FragmentSend.class.getName())
                    .addToBackStack(FragmentSend.class.getName()).commit();
        }

    }

    public static void showSupportFragment(FragmentActivity app, String articleId, BaseWalletManager wm) {
        if (mSupportIsShowing) {
            return;
        }
        try {
            mSupportIsShowing = true;
            if (app == null) {
                Log.e(TAG, "showSupportFragment: app is null");
                return;
            }

            FragmentSupport fragmentSupport = (FragmentSupport) app.getSupportFragmentManager().findFragmentByTag(FragmentSupport.class.getName());
            if (fragmentSupport != null && fragmentSupport.isAdded()) {
                app.getFragmentManager().popBackStack();
                return;
            }
            String iso = BaseBitcoinWalletManager.BITCOIN_SYMBOL;
            if (wm != null) wm.getIso();
            fragmentSupport = new FragmentSupport();
            Bundle bundle = new Bundle();
            bundle.putString("walletIso", iso);
            if (!Utils.isNullOrEmpty(articleId))
                bundle.putString("articleId", articleId);

            fragmentSupport.setArguments(bundle);
            app.getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(0, 0, 0, R.animator.plain_300)
                    .add(android.R.id.content, fragmentSupport, FragmentSend.class.getName())
                    .addToBackStack(FragmentSend.class.getName()).commit();
        } finally {
            mSupportIsShowing = false;
        }

    }


    public static void showTransactionDetails(Activity app, TxUiHolder item, int position) {

        FragmentTxDetails txDetails = (FragmentTxDetails) app.getFragmentManager().findFragmentByTag(FragmentTxDetails.class.getName());

        if (txDetails != null && txDetails.isAdded()) {
            Log.e(TAG, "showTransactionDetails: Already showing");

            return;
        }

        txDetails = new FragmentTxDetails();
        txDetails.setTransaction(item);
        txDetails.show(app.getFragmentManager(), "txDetails");

    }

    public static void openScanner(Activity app, int requestID) {
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
                app.startActivityForResult(intent, requestID);
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

    public static void showRequestFragment(FragmentActivity app) {
        if (app == null) {
            Log.e(TAG, "showRequestFragment: app is null");
            return;
        }

        FragmentRequestAmount fragmentRequestAmount = (FragmentRequestAmount) app.getSupportFragmentManager().findFragmentByTag(FragmentRequestAmount.class.getName());
        if (fragmentRequestAmount != null && fragmentRequestAmount.isAdded())
            return;

        fragmentRequestAmount = new FragmentRequestAmount();
        Bundle bundle = new Bundle();
        fragmentRequestAmount.setArguments(bundle);
        app.getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(0, 0, 0, R.animator.plain_300)
                .add(android.R.id.content, fragmentRequestAmount, FragmentRequestAmount.class.getName())
                .addToBackStack(FragmentRequestAmount.class.getName()).commit();

    }

    //isReceive tells the Animator that the Receive fragment is requested, not My Address
    public static void showReceiveFragment(FragmentActivity app, boolean isReceive) {
        if (app == null) {
            Log.e(TAG, "showReceiveFragment: app is null");
            return;
        }
        FragmentReceive fragmentReceive = (FragmentReceive) app.getSupportFragmentManager().findFragmentByTag(FragmentReceive.class.getName());
        if (fragmentReceive != null && fragmentReceive.isAdded())
            return;
        fragmentReceive = new FragmentReceive();
        Bundle args = new Bundle();
        args.putBoolean("receive", isReceive);
        fragmentReceive.setArguments(args);

        app.getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(0, 0, 0, R.animator.plain_300)
                .add(android.R.id.content, fragmentReceive, FragmentReceive.class.getName())
                .addToBackStack(FragmentReceive.class.getName()).commit();

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
        if (app != null && !app.isDestroyed())
            app.getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    public static void startBreadActivity(Activity from, boolean auth) {
        if (from == null) {
            return;
        }
        Class toStart = auth ? LoginActivity.class : WalletActivity.class;

        // If this is a first launch(new wallet), ensure that we are starting on the Home Screen
        if (toStart.equals(WalletActivity.class)) {

            if (BRSharedPrefs.isNewWallet(from)) {
                toStart = HomeActivity.class;
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

    public static void startWebActivity(Activity activity, String url) {
        Intent intent = new Intent(activity, WebViewActivity.class);
        intent.putExtra(BRConstants.EXTRA_URL, url);
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.enter_from_bottom, R.anim.fade_down);
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