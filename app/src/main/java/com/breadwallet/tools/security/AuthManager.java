package com.breadwallet.tools.security;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.support.v13.app.ActivityCompat;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.fragments.FingerprintFragment;
import com.breadwallet.presenter.fragments.FragmentBreadPin;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.animation.BreadDialog;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.wallet.BRWalletManager;

import java.util.concurrent.TimeUnit;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 8/20/15.
 * Copyright (c) 2016 breadwallet LLC
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

public class AuthManager {
    public static final String TAG = AuthManager.class.getName();
    private static AuthManager instance;

    private AuthManager() {

    }

    public static AuthManager getInstance() {
        if (instance == null)
            instance = new AuthManager();
        return instance;
    }

    public boolean checkAuth(CharSequence passcode, Context context) {
        String pass = KeyStoreManager.getPinCode(context);
        return pass != null && passcode.equals(pass);
    }

    public void setPinCode(String pass, Activity context) {
        KeyStoreManager.putPinCode(pass, context);
        KeyStoreManager.putLastPinUsedTime(System.currentTimeMillis(), context);
        setSpendingLimitIfNotSet(context);
    }

    /**
     * Returns the total current limit that cannot be surpass without a pin
     */
    public long getLimit(Activity activity) {
        return KeyStoreManager.getTotalLimit(activity);
    }

    /**
     * Sets the total current limit that cannot be surpass without a pin
     */
    public void setLimit(Activity activity, int limit) {
        KeyStoreManager.putTotalLimit(limit, activity);
    }

    private void setSpendingLimitIfNotSet(final Activity activity) {
        if (activity == null) return;
        long limit = KeyStoreManager.getSpendLimit(activity);
        if (limit == 0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    long totalSpent = BRWalletManager.getInstance().getTotalSent();
                    long spendLimit = totalSpent + AuthManager.getInstance().getLimit(activity);
                    KeyStoreManager.putSpendLimit(spendLimit, activity);
                }
            }).start();

        }
    }

    public void authPrompt(final Context context, String title, String message, boolean forcePin, BRAuthCompletion completion) {
        if (context == null || !(context instanceof Activity)) {
            Log.e(TAG, "authPrompt: context is null or not Activity: " + context);
            return;
        }
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Activity.KEYGUARD_SERVICE);

        boolean useFingerPrint = isFingerPrintAvailable(context);
//        if (mode == BRConstants.AUTH_FOR_PAY) {
//            long limit = KeyStoreManager.getSpendLimit(context);
//            long totalSent = BRWalletManager.getInstance().getTotalSent();
//
//            if (requestEntity != null)
//                if (limit <= totalSent + requestEntity.amount) {
//                    useFingerPrint = false;
//                }
//        }
//
//        if (mode == BRConstants.AUTH_FOR_LIMIT || mode == BRConstants.AUTH_FOR_PHRASE) {
//            useFingerPrint = false;
//        }

        if (KeyStoreManager.getFailCount(context) != 0) {
            useFingerPrint = false;
        }
        long passTime = KeyStoreManager.getLastPasscodeUsedTime(context);
        if (passTime + TimeUnit.MILLISECONDS.convert(2, TimeUnit.DAYS) <= System.currentTimeMillis()) {
            useFingerPrint = false;
        }
        if (forcePin)
            useFingerPrint = false;

        final Activity app = (Activity) context;

        FingerprintFragment fingerprintFragment = (FingerprintFragment) app.getFragmentManager().findFragmentByTag(FingerprintFragment.class.getName());
        FragmentBreadPin breadPin = (FragmentBreadPin) app.getFragmentManager().findFragmentByTag(FragmentBreadPin.class.getName());
        if (fingerprintFragment != null && fingerprintFragment.isAdded() || breadPin != null && breadPin.isAdded()) {
            Log.e(TAG, "authPrompt: auth fragment already added: F:" + fingerprintFragment + ", P:" + breadPin);
            return;
        }

        if (keyguardManager.isKeyguardSecure()) {
            if (useFingerPrint) {

                fingerprintFragment = new FingerprintFragment();
                Bundle args = new Bundle();
                args.putString("title", title);
                args.putString("message", message);
                fingerprintFragment.setArguments(args);
                fingerprintFragment.setCompletion(completion);
                FragmentTransaction transaction = app.getFragmentManager().beginTransaction();
                transaction.setCustomAnimations(0, 0, 0, R.animator.plain_300);
                transaction.add(android.R.id.content, fingerprintFragment, FingerprintFragment.class.getName());
                transaction.addToBackStack(null);
                if (!app.isDestroyed())
                    transaction.commit();
            } else {

                breadPin = new FragmentBreadPin();
                Bundle args = new Bundle();
                args.putString("title", title);
                args.putString("message", message);
                breadPin.setArguments(args);
                breadPin.setCompletion(completion);
                FragmentTransaction transaction = app.getFragmentManager().beginTransaction();
                transaction.setCustomAnimations(0, 0, 0, R.animator.plain_300);
                transaction.add(android.R.id.content, breadPin, breadPin.getClass().getName());
                transaction.addToBackStack(null);
                if (!app.isDestroyed())
                    transaction.commit();
            }
        } else {
            BreadDialog.showCustomDialog(app, "", context.getString(R.string.encryption_needed_for_wallet), "close", null, new BRDialogView.BROnClickListener() {
                @Override
                public void onClick(BRDialogView brDialogView) {
                    app.finish();
                }
            }, null, new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    app.finish();
                }
            }, 0);
        }

    }

    public static boolean isFingerPrintAvailable(Context context) {
        FingerprintManager mFingerprintManager = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) ==
                PackageManager.PERMISSION_GRANTED && mFingerprintManager.isHardwareDetected() && mFingerprintManager.hasEnrolledFingerprints();
    }

}
