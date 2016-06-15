
package com.breadwallet.presenter.activities;

import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.fragments.IntroNewRecoverFragment;
import com.breadwallet.presenter.fragments.IntroNewWalletFragment;
import com.breadwallet.presenter.fragments.IntroRecoverWalletFragment;
import com.breadwallet.presenter.fragments.IntroWarningFragment;
import com.breadwallet.presenter.fragments.IntroWelcomeFragment;
import com.breadwallet.tools.BRConstants;
import com.breadwallet.tools.SharedPreferencesManager;
import com.breadwallet.tools.animation.BackgroundMovingAnimator;
import com.breadwallet.tools.animation.FragmentAnimator;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.security.PostAuthenticationProcessor;
import com.breadwallet.wallet.BRWalletManager;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on 8/4/15.
 * Copyright (c) 2016 breadwallet llc <mihail@breadwallet.com>
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
    private Button leftButton;

    //loading the native library
    static {
        System.loadLibrary("core");
    }

    @Override
    protected void onRestart() {
        super.onRestart();  // Always call the superclass method first
        app = this;
        // Activity being restarted from stopped state
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        app = this;


//        testCore();
        leftButton = (Button) findViewById(R.id.intro_left_button);
        leftButton.setVisibility(View.GONE);
        leftButton.setClickable(false);
        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        String canary = KeyStoreManager.getKeyStoreCanary(this, BRConstants.CANARY_REQUEST_CODE);
        if (canary.equals("noauth")) return;
        byte[] masterPubKey = KeyStoreManager.getMasterPublicKey(this);
        boolean isFirstAddressCorrect = false;
        if (masterPubKey != null && masterPubKey.length != 0) {
            isFirstAddressCorrect = checkFirstAddress(masterPubKey);
        }
        Log.e(TAG, "isFirstAddressCorrect: " + isFirstAddressCorrect);
        if (!isFirstAddressCorrect) {
            Log.e(TAG, "CLEARING THE WALLET");
            BRWalletManager.getInstance(this).wipeWalletButKeystore(this);
        }

        if (canary.equals("none")) {
            BRWalletManager m = BRWalletManager.getInstance(this);
            m.wipeWalletButKeystore(this);
            m.wipeKeyStore();

        }
        getFragmentManager().beginTransaction().add(R.id.intro_layout, new IntroWelcomeFragment(),
                IntroWelcomeFragment.class.getName()).commit();
        startTheWalletIfExists();

    }

//    public native void testCore();

    public boolean checkFirstAddress(byte[] mpk) {
        String addressFromPrefs = SharedPreferencesManager.getFirstAddress(this);
        String generatedAddress = BRWalletManager.getFirstAddress(mpk);
        Log.e(TAG, "addressFromPrefs: " + addressFromPrefs);
        Log.e(TAG, "generatedAddress: " + generatedAddress);
        return addressFromPrefs.equals(generatedAddress);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //testSQLiteConnectivity(this);   //do some SQLite testing
        app = this;
        //animates the orange BW background moving.
        ImageView background = (ImageView) findViewById(R.id.intro_bread_wallet_image);
        background.setScaleType(ImageView.ScaleType.MATRIX);
        BackgroundMovingAnimator.animateBackgroundMoving(background);

    }

    @Override
    protected void onPause() {
        super.onPause();
        BackgroundMovingAnimator.stopBackgroundMoving();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

    }

    private void showNewRecoverWalletFragment() {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.animator.from_right, R.animator.to_left);
        IntroWelcomeFragment introWelcomeFragment = (IntroWelcomeFragment) getFragmentManager().
                findFragmentByTag(IntroWelcomeFragment.class.getName());
        fragmentTransaction.replace(introWelcomeFragment.getId(), new IntroNewRecoverFragment(), IntroNewRecoverFragment.class.getName());
        fragmentTransaction.commitAllowingStateLoss();
    }

    public void showNewWalletFragment() {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        leftButton.setVisibility(View.VISIBLE);
        leftButton.setClickable(true);
        fragmentTransaction.setCustomAnimations(R.animator.from_right, R.animator.to_left);
        IntroNewRecoverFragment introNewRecoverFragment = (IntroNewRecoverFragment) getFragmentManager().
                findFragmentByTag(IntroNewRecoverFragment.class.getName());
        fragmentTransaction.replace(introNewRecoverFragment.getId(), new IntroNewWalletFragment(), IntroNewWalletFragment.class.getName()).
                addToBackStack(null);
        fragmentTransaction.commitAllowingStateLoss();
    }

    public void showRecoverWalletFragment() {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        leftButton.setVisibility(View.VISIBLE);
        leftButton.setClickable(true);
        fragmentTransaction.setCustomAnimations(R.animator.from_right, R.animator.to_left);
        IntroNewRecoverFragment introNewRecoverFragment = (IntroNewRecoverFragment) getFragmentManager().
                findFragmentByTag(IntroNewRecoverFragment.class.getName());
        fragmentTransaction.replace(introNewRecoverFragment.getId(), new IntroRecoverWalletFragment(), IntroRecoverWalletFragment.class.getName()).
                addToBackStack(null);
        fragmentTransaction.commitAllowingStateLoss();
    }

    public void showWarningFragment() {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        IntroNewWalletFragment introNewWalletFragment = (IntroNewWalletFragment) getFragmentManager().
                findFragmentByTag(IntroNewWalletFragment.class.getName());
        fragmentTransaction.replace(introNewWalletFragment.getId(), new IntroWarningFragment(), IntroWarningFragment.class.getName());
        introNewWalletFragment.introGenerate.setClickable(false);
        leftButton.setVisibility(View.GONE);
        leftButton.setClickable(false);
        fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    public void startMainActivity() {
        Intent intent;
        intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        if (!IntroActivity.this.isDestroyed()) {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BRConstants.PUT_PHRASE_NEW_WALLET_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    PostAuthenticationProcessor.getInstance().onCreateWalletAuth(this);
                } else {
//                    KeyStoreManager.showAuthenticationScreen(this, requestCode);
                    BRWalletManager m = BRWalletManager.getInstance(this);
                    m.wipeKeyStore();
                    m.wipeWalletButKeystore(this);
                    FragmentAnimator.resetFragmentAnimator();
                    finish();
                }
                break;
            case BRConstants.PUT_PHRASE_RECOVERY_WALLET_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    PostAuthenticationProcessor.getInstance().onRecoverWalletAuth(this);
                } else {
//                    KeyStoreManager.showAuthenticationScreen(this, requestCode);
                    finish();
                }
                break;

            case BRConstants.CANARY_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    PostAuthenticationProcessor.getInstance().onCanaryCheckAuth(this);
                } else {
//                    KeyStoreManager.showAuthenticationScreen(this, requestCode);
                    finish();
                }
                break;

        }

        //when starting another activity that will return a result (ex: auth)
    }

    public void startIntroShowPhrase() {
        Intent intent;
        intent = new Intent(this, IntroShowPhraseActivity.class);
        startActivity(intent);
        if (!IntroActivity.this.isDestroyed()) {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        int backStackEntryCount = getFragmentManager().getBackStackEntryCount();
        if (backStackEntryCount > 0) {
            if (backStackEntryCount == 1) {
                leftButton.setVisibility(View.GONE);
                leftButton.setClickable(false);
            }
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }

    }

    public void startTheWalletIfExists() {
        final BRWalletManager m = BRWalletManager.getInstance(this);
        if (!m.isPasscodeEnabled(this)) {
            //Device passcode/password should be enabled for the app to work
            ((BreadWalletApp) getApplication()).showDeviceNotSecuredWarning(this);
        } else {
            if (m.noWallet(app)) {
                //now check if there is a wallet or should we create/restore one.
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "should create new wallet");
                        showNewRecoverWalletFragment();
                    }
                }, 800);
            } else {
                Log.e(TAG, "should go to the current wallet");
                startMainActivity();
            }

        }
    }
}
