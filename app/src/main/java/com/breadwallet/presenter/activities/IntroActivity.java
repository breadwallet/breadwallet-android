
package com.breadwallet.presenter.activities;

import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.breadwallet.BreadWalletApp;
import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.security.PostAuthenticationProcessor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.wallet.BRWalletManager;
import com.google.firebase.crash.FirebaseCrash;


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

public class IntroActivity extends FragmentActivity {
    private static final String TAG = IntroActivity.class.getName();
    public static IntroActivity app;
    public Button newWalletButton;
    public Button recoverWalletButton;

    //loading the native library
    static {
        System.loadLibrary("core");
    }

    public static final Point screenParametersPoint = new Point();

    @Override
    protected void onRestart() {
        super.onRestart();  // Always call the superclass method first
        app = this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        newWalletButton = (Button) findViewById(R.id.button_new_wallet);
        recoverWalletButton = (Button) findViewById(R.id.button_recover_wallet);
        setListeners();

        app = this;

        if (!BuildConfig.DEBUG && KeyStoreManager.AUTH_DURATION_SEC != 300) {
            Log.e(TAG, "onCreate: KeyStoreManager.AUTH_DURATION_SEC != 300");
            RuntimeException ex = new RuntimeException("AUTH_DURATION_SEC should be 300");
            FirebaseCrash.report(ex);
            throw ex;
        }

        getWindowManager().getDefaultDisplay().getSize(screenParametersPoint);

        byte[] masterPubKey = KeyStoreManager.getMasterPublicKey(this);
        boolean isFirstAddressCorrect = false;
        if (masterPubKey != null && masterPubKey.length != 0) {
            isFirstAddressCorrect = checkFirstAddress(masterPubKey);
        }
//        Log.e(TAG, "isFirstAddressCorrect: " + isFirstAddressCorrect);
        if (!isFirstAddressCorrect) {
            Log.e(TAG, "WARNING: isFirstAddressCorrect - false: CLEARING THE WALLET");

            BRWalletManager.getInstance(this).wipeWalletButKeystore(this);
        }
        setStatusBarColor(android.R.color.transparent);

        PostAuthenticationProcessor.getInstance().onCanaryCheck(this, false);

    }

    private void setListeners() {
        newWalletButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpringAnimator.showAnimation(v);
                Intent intent = new Intent(IntroActivity.this, IntroSetPitActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        });

        recoverWalletButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpringAnimator.showAnimation(v);
                Intent intent = new Intent(IntroActivity.this, IntroRecoverActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        });
    }


    private void setStatusBarColor(int color) {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getColor(color));
    }

    public boolean checkFirstAddress(byte[] mpk) {
        String addressFromPrefs = SharedPreferencesManager.getFirstAddress(this);
        String generatedAddress = BRWalletManager.getFirstAddress(mpk);
        return addressFromPrefs.equals(generatedAddress);
    }

    @Override
    protected void onResume() {
        super.onResume();
        app = this;

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    public void startMainActivity() {
        Intent intent;
        intent = new Intent(this, BreadActivity.class);
        startActivity(intent);
        if (!IntroActivity.this.isDestroyed()) {
            finish();
        }
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

//        if (backNotAllowed) return;
//        if (newRecoverNoneFlag == 1) {
//            leftButton.setVisibility(View.GONE);
//            leftButton.setClickable(false);
//            animateSlide(introNewWalletFragment, introNewRecoverFragment, LEFT);
//            newRecoverNoneFlag = 0;
//        } else if (newRecoverNoneFlag == 2) {
//            showHideFragments(introRecoverWalletFragment, introNewRecoverFragment);
//            leftButton.setVisibility(View.GONE);
//            leftButton.setClickable(false);
//            animateSlide(introRecoverWalletFragment, introNewRecoverFragment, LEFT);
//            newRecoverNoneFlag = 0;
//            introRecoverWalletFragment.showKeyBoard(false);
//        } else {
//            super.onBackPressed();
//        }

    }

    public void startTheWalletIfExists() {
        final BRWalletManager m = BRWalletManager.getInstance(this);
        if (!m.isPasscodeEnabled(this)) {
            //Device passcode/password should be enabled for the app to work
            ((BreadWalletApp) getApplication()).showDeviceNotSecuredWarning(this);
        } else {
            if (!m.noWallet(app)) {
                startMainActivity();
            }

        }
    }
}
