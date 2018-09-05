package com.breadwallet.tools.security;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.DisabledActivity;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.customviews.PinLayout;
import com.breadwallet.presenter.fragments.FragmentFingerprint;
import com.breadwallet.presenter.fragments.PinFragment;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.Utils;

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
    private static final int MAX_UNLOCK_ATTEMPTS = 3;

    public static AuthManager getInstance() {
        if (instance == null) {
            instance = new AuthManager();
        }
        return instance;
    }

    public boolean isWalletDisabled(Activity app) {
        long start = System.currentTimeMillis();
        int failCount = BRKeyStore.getFailCount(app);
        return failCount >= MAX_UNLOCK_ATTEMPTS && disabledUntil(app) > BRSharedPrefs.getSecureTime(app);

    }

    public long disabledUntil(Activity app) {
        int failCount = BRKeyStore.getFailCount(app);
        long failTimestamp = BRKeyStore.getFailTimeStamp(app);
        double pow = Math.pow(PinLayout.MAX_PIN_DIGITS, failCount - MAX_UNLOCK_ATTEMPTS) * DateUtils.MINUTE_IN_MILLIS;
        return (long) (failTimestamp + pow);
    }

    public void setWalletDisabled(Activity app) {
        if (!(app instanceof DisabledActivity)) {
            UiUtils.showWalletDisabled(app);
        }
    }

    public void setPinCode(Context context, String pass) {
        BRKeyStore.putFailCount(0, context);
        BRKeyStore.putPinCode(pass, context);
        BRKeyStore.putLastPinUsedTime(System.currentTimeMillis(), context);
    }

    public void authPrompt(Context context, String title, String message, boolean forcePin, boolean forceFingerprint, BRAuthCompletion completion) {
        context = BreadApp.getBreadContext();
        if (context == null || !(context instanceof Activity)) {
            Log.e(TAG, "authPrompt: context is null or not Activity: " + context);
            return;
        }
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Activity.KEYGUARD_SERVICE);

        boolean useFingerPrint = isFingerPrintAvailableAndSetup(context);

        long passTime = BRKeyStore.getLastPinUsedTime(context);
        long twoDays = TimeUnit.MILLISECONDS.convert(2, TimeUnit.DAYS);

        if (BRKeyStore.getFailCount(context) != 0 || (passTime + twoDays <= System.currentTimeMillis())) {
            useFingerPrint = false;
        }

        if (forceFingerprint) {
            useFingerPrint = true;
        }

        if (forcePin) {
            useFingerPrint = false;
        }

        final Activity app = (Activity) context;

        FragmentFingerprint fingerprintFragment;
        PinFragment breadPin;

        if (keyguardManager.isKeyguardSecure()) {
            if (useFingerPrint) {
                fingerprintFragment = new FragmentFingerprint();
                Bundle args = new Bundle();
                args.putString("title", title);
                args.putString("message", message);
                fingerprintFragment.setArguments(args);
                fingerprintFragment.setCompletion(completion);
                FragmentTransaction transaction = app.getFragmentManager().beginTransaction();
                transaction.setCustomAnimations(0, 0, 0, R.animator.plain_300);
                transaction.add(android.R.id.content, fingerprintFragment, FragmentFingerprint.class.getName());
                transaction.addToBackStack(null);
                if (!app.isDestroyed()) {
                    transaction.commitAllowingStateLoss();
                }
            } else {
                breadPin = new PinFragment();
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
                    transaction.commitAllowingStateLoss();
                }
            }
        } else {
            sayNoScreenLock(app);
        }

    }

    private void sayNoScreenLock(final Activity app) {
        BRDialog.showCustomDialog(app,
                "",
                app.getString(R.string.Prompts_NoScreenLock_body_android),
                app.getString(R.string.AccessibilityLabels_close),
                null,
                new BRDialogView.BROnClickListener() {
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

    public static boolean isFingerPrintAvailableAndSetup(Context context) {
        return Utils.isFingerprintAvailable(context) && Utils.isFingerprintEnrolled(context);
    }

}
