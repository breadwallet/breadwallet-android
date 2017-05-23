package com.breadwallet.tools.security;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.fragments.FingerprintFragment;
import com.breadwallet.presenter.fragments.FragmentPin;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.animation.BreadDialog;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.util.Utils;
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
    private String previousTry;

    private AuthManager() {
        previousTry = "";
    }

    public static AuthManager getInstance() {
        if (instance == null)
            instance = new AuthManager();
        return instance;
    }

    public boolean checkAuth(CharSequence passSequence, Context context) {
        Log.e(TAG, "checkAuth: ");
        String tempPass = passSequence.toString();
        if (!previousTry.equals(tempPass)) {
            int failCount = KeyStoreManager.getFailCount(context);
            KeyStoreManager.putFailCount(failCount + 1, context);
        }
        previousTry = tempPass;

        String pass = KeyStoreManager.getPinCode(context);
        boolean match = pass != null && tempPass.equals(pass);
        if (!match) {
            if (KeyStoreManager.getFailCount(context) >= 3) {
                setWalletDisabled((Activity) context);
            }
        }

        return match;
    }

    public void authSuccess(final Context app) {
        //put the new total limit in 3 seconds, leave some time for the core to register any new tx
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                AuthManager.getInstance().setTotalLimit(app, BRWalletManager.getInstance().getTotalSent()
                        + KeyStoreManager.getSpendLimit(app));
            }
        }).start();

        KeyStoreManager.putFailCount(0, app);
        KeyStoreManager.putLastPinUsedTime(System.currentTimeMillis(), app);
    }

    public void authFail(Context app) {

    }

    public boolean isWalletDisabled(Activity app) {
        int failCount = KeyStoreManager.getFailCount(app);
        long secureTime = SharedPreferencesManager.getSecureTime(app);
        long failTimestamp = KeyStoreManager.getFailTimeStamp(app);
        return failCount >= 3 && secureTime < failTimestamp + Math.pow(6, failCount - 3) * 60.0;

    }

    public void setWalletDisabled(Activity app) {
        int failCount = KeyStoreManager.getFailCount(app);
        long now = System.currentTimeMillis() / 1000;
        long secureTime = SharedPreferencesManager.getSecureTime(app);
        long failTimestamp = KeyStoreManager.getFailTimeStamp(app);
        double waitTimeMinutes = (failTimestamp + Math.pow(6, failCount - 3) * 60.0 - secureTime) / 60.0;

        ActivityUTILS.showWalletDisabled(app, waitTimeMinutes);
    }

    public void setPinCode(String pass, Activity context) {
        KeyStoreManager.putFailCount(0, context);
        KeyStoreManager.putPinCode(pass, context);
        KeyStoreManager.putLastPinUsedTime(System.currentTimeMillis(), context);
        setSpendingLimitIfNotSet(context);
    }

    /**
     * Returns the total current limit that cannot be surpass without a pin
     */
    public long getTotalLimit(Context activity) {
        return KeyStoreManager.getTotalLimit(activity);
    }

    /**
     * Sets the total current limit that cannot be surpass without a pin
     */
    public void setTotalLimit(Context activity, long limit) {
        KeyStoreManager.putTotalLimit(limit, activity);
    }

    private void setSpendingLimitIfNotSet(final Activity activity) {
        if (activity == null) return;
        long limit = AuthManager.getInstance().getTotalLimit(activity);
        if (limit == 0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    long totalSpent = BRWalletManager.getInstance().getTotalSent();
                    long totalLimit = totalSpent + KeyStoreManager.getSpendLimit(activity);
                    setTotalLimit(activity, totalLimit);
                }
            }).start();

        }
    }

    public void updateDots(Context context, int pinLimit, String pin, View dot1, View dot2, View dot3, View dot4, View dot5, View dot6, int emptyPinRes, final OnPinSuccess onPinSuccess) {
        if (dot1 == null) return;
        int selectedDots = pin.length();

        if (pinLimit == 6) {
            dot6.setVisibility(View.VISIBLE);
            dot1.setVisibility(View.VISIBLE);
            dot1.setBackground(context.getDrawable(selectedDots <= 0 ? emptyPinRes : R.drawable.ic_pin_dot_black));
            selectedDots--;
        } else {
            dot6.setVisibility(View.GONE);
            dot1.setVisibility(View.GONE);
        }

        dot2.setBackground(context.getDrawable(selectedDots <= 0 ? emptyPinRes : R.drawable.ic_pin_dot_black));
        selectedDots--;
        dot3.setBackground(context.getDrawable(selectedDots <= 0 ? emptyPinRes : R.drawable.ic_pin_dot_black));
        selectedDots--;
        dot4.setBackground(context.getDrawable(selectedDots <= 0 ? emptyPinRes : R.drawable.ic_pin_dot_black));
        selectedDots--;
        dot5.setBackground(context.getDrawable(selectedDots <= 0 ? emptyPinRes : R.drawable.ic_pin_dot_black));
        if (pinLimit == 6) {
            selectedDots--;
            dot6.setBackground(context.getDrawable(selectedDots <= 0 ? emptyPinRes : R.drawable.ic_pin_dot_black));
        }

        if (pin.length() == pinLimit) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    onPinSuccess.onSuccess();

                }
            }, 100);

        }
    }

    public void authPrompt(final Context context, String title, String message, boolean forcePin, BRAuthCompletion completion) {
        if (context == null || !(context instanceof Activity)) {
            Log.e(TAG, "authPrompt: context is null or not Activity: " + context);
            return;
        }
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Activity.KEYGUARD_SERVICE);

        boolean useFingerPrint = isFingerPrintAvailableAndSetup(context);

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
        FragmentPin breadPin = (FragmentPin) app.getFragmentManager().findFragmentByTag(FragmentPin.class.getName());

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
                breadPin = new FragmentPin();
                Bundle args = new Bundle();
                args.putString("title", title);
                args.putString("message", message);
                breadPin.setArguments(args);
                breadPin.setCompletion(completion);
                FragmentTransaction transaction = app.getFragmentManager().beginTransaction();
                transaction.setCustomAnimations(0, 0, 0, R.animator.plain_300);
                transaction.add(android.R.id.content, breadPin, breadPin.getClass().getName());
                transaction.addToBackStack(null);
                if (!app.isDestroyed()) {
                    transaction.commit();
                }
            }
        } else {
            BreadDialog.showCustomDialog(app, "", context.getString(R.string.IntroScreen_encryption_needed_Android), "close", null, new BRDialogView.BROnClickListener() {
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

    public static boolean isFingerPrintAvailableAndSetup(Context context) {
        return Utils.isFingerprintAvailable(context) && Utils.isFingerprintEnrolled(context);
    }

    public interface OnPinSuccess {
        void onSuccess();
    }
}
