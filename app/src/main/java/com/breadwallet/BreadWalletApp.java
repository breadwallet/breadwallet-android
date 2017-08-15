package com.breadwallet;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.hardware.fingerprint.FingerprintManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.presenter.activities.IntroActivity;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.PaymentRequestEntity;
import com.breadwallet.presenter.entities.PaymentRequestWrapper;
import com.breadwallet.presenter.fragments.FingerprintDialogFragment;
import com.breadwallet.presenter.fragments.PasswordDialogFragment;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.wallet.BRWalletManager;

import java.util.concurrent.TimeUnit;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/22/15.
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

public class BreadWalletApp extends Application {
    private static final String TAG = BreadWalletApp.class.getName();
    public static boolean unlocked = false;
    private boolean customToastAvailable = true;
    public boolean allowKeyStoreAccess;
    private String oldMessage;
    private Toast toast;
    public static int DISPLAY_HEIGHT_PX;
    FingerprintManager mFingerprintManager;
    public static String HOST = "api.breadwallet.com";

    @Override
    public void onCreate() {
        super.onCreate();

        if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            HOST = "stage.breadwallet.com";
        }

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int DISPLAY_WIDTH_PX = size.x;
        DISPLAY_HEIGHT_PX = size.y;
        mFingerprintManager = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);

    }

    /**
     * Shows a custom toast using the given string as a paramater,
     *
     * @param message the message to be shown in the custom toast
     */

    public void showCustomToast(Activity app, String message, int yOffSet, int duration, int color) {
        if (toast == null) toast = new Toast(getApplicationContext());
        if (MainActivity.appInBackground) return;

        if (customToastAvailable || !oldMessage.equals(message)) {
            oldMessage = message;
            customToastAvailable = false;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    customToastAvailable = true;
                }
            }, 1000);
            LayoutInflater inflater = app.getLayoutInflater();
            View layout = inflater.inflate(R.layout.toast,
                    (ViewGroup) app.findViewById(R.id.toast_layout_root));
            if (color == 1) {
                layout.setBackgroundResource(R.drawable.toast_layout_black);
            }
            TextView text = (TextView) layout.findViewById(R.id.toast_text);
            text.setText(message);
            toast.setGravity(Gravity.BOTTOM, 0, yOffSet);
            toast.setDuration(duration);
            toast.setView(layout);
            toast.show();
        }
    }

    public void cancelToast() {
        if (toast != null) {
            toast.cancel();
        }
    }

    public boolean isToastShown() {
        return toast != null && toast.getView() != null && toast.getView().isShown();
    }

    public void setTopMiddleView(int view, String text) {
        MainActivity app = MainActivity.app;
        if(app == null) return;
        switch (view) {
            case BRConstants.BREAD_WALLET_IMAGE:
                if (app.viewFlipper.getDisplayedChild() == 1) {
                    app.viewFlipper.showPrevious();
                }
                break;
            case BRConstants.BREAD_WALLET_TEXT:
                if (app.viewFlipper.getDisplayedChild() == 0) {
                    app.viewFlipper.showNext();
                }
                ((TextView) app.viewFlipper.getCurrentView()).setText(text);
                ((TextView) app.viewFlipper.getCurrentView()).setTextSize(20);
                break;
        }
    }

    public void setLockerPayButton(int view) {
        MainActivity app = MainActivity.app;
        switch (view) {
            case BRConstants.LOCKER_BUTTON:
                if (app.lockerPayFlipper.getDisplayedChild() == 1) {
                    app.lockerPayFlipper.showPrevious();

                } else if (app.lockerPayFlipper.getDisplayedChild() == 2) {
                    app.lockerPayFlipper.showPrevious();
                    app.lockerPayFlipper.showPrevious();
                }
                app.lockerButton.setVisibility(unlocked ? View.INVISIBLE : View.VISIBLE);
                break;
            case BRConstants.PAY_BUTTON:
                if (app.lockerPayFlipper.getDisplayedChild() == 0)
                    app.lockerPayFlipper.showNext();
                if (app.lockerPayFlipper.getDisplayedChild() == 2)
                    app.lockerPayFlipper.showPrevious();
                break;
            case BRConstants.REQUEST_BUTTON:
                if (app.lockerPayFlipper.getDisplayedChild() == 0) {
                    app.lockerPayFlipper.showNext();
                    app.lockerPayFlipper.showNext();
                } else if (app.lockerPayFlipper.getDisplayedChild() == 1) {
                    app.lockerPayFlipper.showNext();
                }
                break;
        }

    }

    public void promptForAuthentication(Activity context, int mode, PaymentRequestEntity requestEntity, String message, String title, PaymentRequestWrapper paymentRequest, boolean forcePasscode) {
        Log.e(TAG, "promptForAuthentication: " + mode);
        if (context == null) return;
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Activity.KEYGUARD_SERVICE);

        boolean useFingerPrint = ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) ==
                PackageManager.PERMISSION_GRANTED && mFingerprintManager.isHardwareDetected() && mFingerprintManager.hasEnrolledFingerprints();
        if (mode == BRConstants.AUTH_FOR_PAY) {
            long limit = KeyStoreManager.getSpendLimit(context);
            long totalSent = BRWalletManager.getInstance(context).getTotalSent();

            if (requestEntity != null)
                if (limit <= totalSent + requestEntity.amount) {
                    useFingerPrint = false;
                }
        }

        if (mode == BRConstants.AUTH_FOR_LIMIT || mode == BRConstants.AUTH_FOR_PHRASE) {
            useFingerPrint = false;
        }

        if (KeyStoreManager.getFailCount(context) != 0) {
            useFingerPrint = false;
        }
        long passTime = KeyStoreManager.getLastPasscodeUsedTime(context);
        if (passTime + TimeUnit.MILLISECONDS.convert(2, TimeUnit.DAYS) <= System.currentTimeMillis()) {
            useFingerPrint = false;
        }
        if (forcePasscode) useFingerPrint = false;

        if (keyguardManager.isKeyguardSecure()) {
            if (useFingerPrint) {
                // This happens when no fingerprints are registered.
                FingerprintDialogFragment fingerprintDialogFragment = new FingerprintDialogFragment();
                fingerprintDialogFragment.setMode(mode);
                fingerprintDialogFragment.setPaymentRequestEntity(requestEntity, paymentRequest);
                fingerprintDialogFragment.setMessage(message);
                fingerprintDialogFragment.setTitle(message != null ? "" : title);
                if (!context.isDestroyed())
                    fingerprintDialogFragment.show(context.getFragmentManager(), FingerprintDialogFragment.class.getName());
            } else {
                PasswordDialogFragment passwordDialogFragment = new PasswordDialogFragment();
                passwordDialogFragment.setMode(mode);
                passwordDialogFragment.setPaymentRequestEntity(requestEntity, paymentRequest);
                passwordDialogFragment.setVerifyOnlyTrue();
                passwordDialogFragment.setMessage(message);
                if (!context.isDestroyed())
                    passwordDialogFragment.show(context.getFragmentManager(), PasswordDialogFragment.class.getName());
            }
        } else {
            showDeviceNotSecuredWarning(context);
        }

    }

    public void showDeviceNotSecuredWarning(final Activity context) {
        Log.e(TAG, "WARNING device is not secured!");
        new AlertDialog.Builder(context)
                .setMessage(R.string.encryption_needed_for_wallet)
                .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        context.finish();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        context.finish();
                    }
                })
                .show();
    }


    public void showCustomDialog(final String title, final String message, final String buttonText) {
        Activity app = MainActivity.app;
        if (app == null) app = IntroActivity.app;
        if (app == null) {
            Log.e(TAG, "showCustomDialog: FAILED, context is null");
            return;
        }
        final Activity finalApp = app;
        app.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new android.app.AlertDialog.Builder(finalApp)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton(buttonText, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });
    }

    public void setUnlocked(boolean b) {
        unlocked = b;
        MainActivity app = MainActivity.app;
        if (app != null) {
            app.lockerButton.setVisibility(b ? View.GONE : View.VISIBLE);
            app.lockerButton.setClickable(!b);
            MiddleViewAdapter.resetMiddleView(app, null);
        }
    }

    public void allowKeyStoreAccessForSeconds() {
        allowKeyStoreAccess = true;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                allowKeyStoreAccess = false;
            }
        }, 2 * 1000);
    }

    public void hideKeyboard(Activity act) {
        Activity activity = act;
        if (activity == null) activity = MainActivity.app;
        if (activity == null) activity = IntroActivity.app;
        if (activity != null) {
            View view = activity.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                return;
            }
        }
        Log.e(TAG, "hideKeyboard: FAILED");
    }

    public boolean hasInternetAccess() {
        return isNetworkAvailable();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
