
package com.breadwallet.presenter.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.BreadDialog;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.security.PostAuthenticationProcessor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.wallet.BRWalletManager;
import com.google.firebase.crash.FirebaseCrash;

import java.io.Serializable;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 8/4/15.
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

public class IntroActivity extends FragmentActivity implements Serializable {
    private static final String TAG = IntroActivity.class.getName();
    public Button newWalletButton;
    public Button recoverWalletButton;
    public static IntroActivity introActivity;
    public static boolean appVisible = false;
    private static IntroActivity app;

    public static IntroActivity getApp() {
        return app;
    }

    //loading the native library
    static {
        System.loadLibrary("core");
    }

    public static final Point screenParametersPoint = new Point();

    @Override
    protected void onRestart() {
        super.onRestart();  // Always call the superclass method first
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        newWalletButton = (Button) findViewById(R.id.button_new_wallet);
        recoverWalletButton = (Button) findViewById(R.id.button_recover_wallet);
        setListeners();

        if (!BuildConfig.DEBUG && KeyStoreManager.AUTH_DURATION_SEC != 300) {
            Log.e(TAG, "onCreate: KeyStoreManager.AUTH_DURATION_SEC != 300");
            RuntimeException ex = new RuntimeException("AUTH_DURATION_SEC should be 300");
            FirebaseCrash.report(ex);
            throw ex;
        }
        introActivity = this;

        getWindowManager().getDefaultDisplay().getSize(screenParametersPoint);

        byte[] masterPubKey = KeyStoreManager.getMasterPublicKey(this);
        boolean isFirstAddressCorrect = false;
        if (masterPubKey != null && masterPubKey.length != 0) {
            isFirstAddressCorrect = checkFirstAddress(masterPubKey);
        }
//        Log.e(TAG, "isFirstAddressCorrect: " + isFirstAddressCorrect);
        if (!isFirstAddressCorrect) {
            Log.e(TAG, "WARNING: isFirstAddressCorrect - false: CLEARING THE WALLET");

            BRWalletManager.getInstance().wipeWalletButKeystore(this);
        }

        PostAuthenticationProcessor.getInstance().onCanaryCheck(this, false);

    }


    private void setListeners() {
        newWalletButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                SpringAnimator.springView(v);
                Intent intent = new Intent(IntroActivity.this, IntroSetPitActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);

            }
        });

        recoverWalletButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                SpringAnimator.springView(v);
                Intent intent = new Intent(IntroActivity.this, IntroRecoverActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);

            }
        });
    }

    public boolean checkFirstAddress(byte[] mpk) {
        String addressFromPrefs = SharedPreferencesManager.getFirstAddress(this);
        String generatedAddress = BRWalletManager.getFirstAddress(mpk);
        return addressFromPrefs.equals(generatedAddress);
    }

    @Override
    protected void onResume() {
        super.onResume();
        appVisible = true;
        app = this;
        ActivityUTILS.init(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
//            case BRConstants.PUT_PHRASE_NEW_WALLET_REQUEST_CODE:
//                if (resultCode == RESULT_OK) {
//                    PostAuthenticationProcessor.getInstance().onCreateWalletAuth(this, true);
//                } else {
//                    Log.e(TAG, "WARNING: resultCode != RESULT_OK");
//                    BRWalletManager m = BRWalletManager.getInstance(this);
//                    m.wipeWalletButKeystore(this);
//                    BRAnimator.resetFragmentAnimator();
//                    finish();
//                }
//                break;
//            case BRConstants.PUT_PHRASE_RECOVERY_WALLET_REQUEST_CODE:
//                if (resultCode == RESULT_OK) {
//                    PostAuthenticationProcessor.getInstance().onRecoverWalletAuth(this, true);
//                } else {
//                    finish();
//                }
//                break;
            case BRConstants.CANARY_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    PostAuthenticationProcessor.getInstance().onCanaryCheck(this, true);
                } else {
                    finish();
                }
                break;

        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

    }

    public void startTheWalletIfExists() {
        final BRWalletManager m = BRWalletManager.getInstance();
        if (!m.isPasscodeEnabled(this)) {
            //Device passcode/password should be enabled for the app to work
            BreadDialog.showCustomDialog(this, "Warning", getString(R.string.encryption_needed_for_wallet), "close", null, new BRDialogView.BROnClickListener() {
                @Override
                public void onClick(BRDialogView brDialogView) {
                    finish();
                }
            }, null, new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                }
            }, 0);
        } else {
            if (!m.noWallet(this)) {
                BRAnimator.startBreadActivity(this, true);
            }

        }
    }

}
