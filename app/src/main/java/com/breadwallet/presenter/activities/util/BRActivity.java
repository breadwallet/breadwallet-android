package com.breadwallet.presenter.activities.util;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;

import com.breadwallet.BreadApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.DisabledActivity;
import com.breadwallet.presenter.activities.HomeActivity;
import com.breadwallet.presenter.activities.InputPinActivity;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.ui.recovery.RecoveryKeyActivity;
import com.breadwallet.ui.wallet.WalletActivity;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.manager.BRApiManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.qrcode.QRUtils;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.security.PostAuth;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.wallet.WalletsMaster;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 5/23/17.
 * Copyright (c) 2017 breadwallet LLC
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
public abstract class BRActivity extends FragmentActivity {
    private static final String TAG = BRActivity.class.getName();

    private static final int LOCK_TIMEOUT = 180000; // 3 minutes in milliseconds

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        saveScreenSizesIfNeeded();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //open back to HomeActivity if needed
        if (this instanceof WalletActivity)
            BRSharedPrefs.putAppBackgroundedFromHome(this, false);
        else if (this instanceof HomeActivity)
            BRSharedPrefs.putAppBackgroundedFromHome(this, true);

    }

    @Override
    protected void onResume() {
        super.onResume();
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ((BreadApp) getApplicationContext()).setDelayServerShutdown(false, -1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case BRConstants.CAMERA_REQUEST_ID:
                // Received permission result for camera permission.
                Log.i(TAG, "Received response for CAMERA_REQUEST_ID permission request.");

                // Check if the only required permission has been granted.
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Camera permission has been granted, preview can be displayed.
                    Log.i(TAG, "CAMERA permission has now been granted. Showing preview.");
                    UiUtils.openScanner(this);
                } else {
                    Log.i(TAG, "CAMERA permission was NOT granted.");
                    BRDialog.showSimpleDialog(this, getString(R.string.Send_cameraUnavailabeTitle_android), getString(R.string.Send_cameraUnavailabeMessage_android));
                }
                break;

            case QRUtils.WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_ID:
                // Check if the only required permission has been granted.
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Write storage permission has been granted, preview can be displayed.
                    Log.i(TAG, "WRITE permission has now been granted.");
                    QRUtils.share(this);
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        // 123 is the qrCode result
        switch (requestCode) {
            case BRConstants.PAY_REQUEST_CODE:
                ((BreadApp) getApplicationContext()).setDelayServerShutdown(false, requestCode);
                if (resultCode == RESULT_OK) {
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            PostAuth.getInstance().onPublishTxAuth(BRActivity.this, null, true, null);
                        }
                    });
                }
                break;
            case BRConstants.REQUEST_PHRASE_BITID:
                if (resultCode == RESULT_OK) {
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            PostAuth.getInstance().onBitIDAuth(BRActivity.this, true);
                        }
                    });
                }
                break;

            case BRConstants.PAYMENT_PROTOCOL_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            PostAuth.getInstance().onPaymentProtocolRequest(BRActivity.this, true, null);
                        }
                    });

                }
                break;

            case BRConstants.SHOW_PHRASE_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            PostAuth.getInstance().onPhraseCheckAuth(BRActivity.this, true, null);
                        }
                    });
                }
                break;

            case BRConstants.PROVE_PHRASE_REQUEST:
                if (resultCode == RESULT_OK) {
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            PostAuth.getInstance().onPhraseProveAuth(BRActivity.this, true, null);
                        }
                    });
                }
                break;

            case BRConstants.PUT_PHRASE_NEW_WALLET_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            PostAuth.getInstance().onCreateWalletAuth(BRActivity.this, true, null);
                        }
                    });
                } else {
                    Log.e(TAG, "User failed to authenticate device while creating a wallet. Clearing all user data now.");
                    // TODO: Should this be BreadApp.clearApplicationUserData();?
                    WalletsMaster.getInstance().wipeWalletButKeystore(this);
                    finish();
                }
                break;
        }
    }

    private void init() {
        //show wallet locked if it is and we're not in an illegal activity.
        if (!(this instanceof InputPinActivity || this instanceof RecoveryKeyActivity)) {
            if (AuthManager.getInstance().isWalletDisabled(this)) {
                showWalletDisabled();
            }
        }
        BreadApp.setBreadContext(this);
        lockIfNeeded();
    }

    private void saveScreenSizesIfNeeded() {
        if (BRSharedPrefs.getScreenHeight(this) == 0) {
            Log.d(TAG, "saveScreenSizesIfNeeded: saving screen sizes.");
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            BRSharedPrefs.putScreenHeight(this, size.y);
            BRSharedPrefs.putScreenWidth(this, size.x);
        }
    }

    private void lockIfNeeded() {
        //lock wallet if 3 minutes passed
        BreadApp breadApp = (BreadApp) getApplication();

        long backgroudedTime = breadApp.getBackgroundedTime();
        if (backgroudedTime != 0
                && (System.currentTimeMillis() - backgroudedTime >= LOCK_TIMEOUT)
                && !(this instanceof DisabledActivity)) {
            if (!BRKeyStore.getPinCode(this).isEmpty()) {
                UiUtils.startBreadActivity(this, true);
            }
        }
        breadApp.resetBackgroundedTime();
    }

    /**
     * Start DisabledActivity.
     */
    public void showWalletDisabled() {
        if (!(this instanceof DisabledActivity)) {
            UiUtils.showWalletDisabled(this);
        }
    }

    /**
     * Check if there is an overlay view over the screen, if an overlay view is found the event won't be dispatched and
     * a dialog with a warning will be shown.
     *
     * @param event The touch screen event.
     * @return boolean Return true if this event was consumed or if an overlay view was found.
     */
    protected boolean checkOverlayAndDispatchTouchEvent(MotionEvent event) {
        // Filter obscured touches by consuming them.
        if ((event.getFlags() & MotionEvent.FLAG_WINDOW_IS_OBSCURED) != 0) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                BRDialog.showSimpleDialog(this, getString(R.string.Android_screenAlteringTitle),
                        getString(R.string.Android_screenAlteringMessage));
            }
            return true;
        }
        return super.dispatchTouchEvent(event);
    }
}
