package com.breadwallet.tools.security;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.presenter.fragments.FragmentBreadPin;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.wallet.BRWalletManager;

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

    public boolean checkAuth(CharSequence passcode, Activity context) {
        String pass = KeyStoreManager.getPinCode(context);
        return pass != null && passcode.equals(pass);
    }

    public void setPinCode(String pass, Activity context) {
        KeyStoreManager.putPinCode(pass, context);
        KeyStoreManager.putLastPinUsedTime(System.currentTimeMillis(), context);
        setSpendingLimitIfNotSet(context);
    }

    public int getLimit(Activity activity) {
        return SharedPreferencesManager.getLimit(activity);
    }

    public void setLimit(Activity activity, int limit) {
        SharedPreferencesManager.putLimit(activity, limit);
    }


    private void setSpendingLimitIfNotSet(Activity activity) {
        if (activity == null) return;
        long limit = KeyStoreManager.getSpendLimit(activity);
        if (limit == 0) {
            long totalSpent = BRWalletManager.getInstance().getTotalSent();
            long spendLimit = totalSpent + AuthManager.getInstance().getLimit(activity);
            KeyStoreManager.putSpendLimit(spendLimit, activity);
        }
    }

    public void authPrompt(Context context, String title, String message, boolean forcePin, BRAuthCompletion completion) {
        if (context == null || !(context instanceof Activity)) {
            Log.e(TAG, "authPrompt: context is null or not Activity: " + context);
            return;
        }

        Activity app = (Activity) context;

        FragmentBreadPin breadPin = new FragmentBreadPin();
        breadPin.setmTitle(title);
        breadPin.setmMessage(message);
        breadPin.setCompletion(completion);

        FragmentTransaction transaction = app.getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.animator.from_bottom, R.animator.to_bottom, R.animator.from_bottom, R.animator.to_bottom);
        transaction.add(android.R.id.content, breadPin, breadPin.getClass().getName());
        transaction.addToBackStack(null);
        transaction.commit();

//        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Activity.KEYGUARD_SERVICE);
//
//        boolean useFingerPrint = ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) ==
//                PackageManager.PERMISSION_GRANTED && mFingerprintManager.isHardwareDetected() && mFingerprintManager.hasEnrolledFingerprints();
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
//
//        if (KeyStoreManager.getFailCount(context) != 0) {
//            useFingerPrint = false;
//        }
//        long passTime = KeyStoreManager.getLastPasscodeUsedTime(context);
//        if (passTime + TimeUnit.MILLISECONDS.convert(2, TimeUnit.DAYS) <= System.currentTimeMillis()) {
//            useFingerPrint = false;
//        }
//        if (forcePasscode) useFingerPrint = false;
//
//        if (keyguardManager.isKeyguardSecure()) {
//            if (useFingerPrint) {
//                // This happens when no fingerprints are registered.
//                FingerprintDialogFragment fingerprintDialogFragment = new FingerprintDialogFragment();
//                fingerprintDialogFragment.setMode(mode);
//                fingerprintDialogFragment.setPaymentRequestEntity(requestEntity, paymentRequest);
//                fingerprintDialogFragment.setmMessage(message);
//                fingerprintDialogFragment.setmTitle(message != null ? "" : title);
//                if (!context.isDestroyed())
//                    fingerprintDialogFragment.show(context.getFragmentManager(), FingerprintDialogFragment.class.getName());
//            } else {
//                PasswordDialogFragment passwordDialogFragment = new PasswordDialogFragment();
//                passwordDialogFragment.setMode(mode);
//                passwordDialogFragment.setPaymentRequestEntity(requestEntity, paymentRequest);
//                passwordDialogFragment.setVerifyOnlyTrue();
//                passwordDialogFragment.setmMessage(message);
//                if (!context.isDestroyed())
//                    passwordDialogFragment.show(context.getFragmentManager(), PasswordDialogFragment.class.getName());
//            }
//        } else {
//            showDeviceNotSecuredWarning(context);
//        }

    }
}
