
package com.breadwallet.presenter.activities;

import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.fragments.IntroNewRecoverFragment;
import com.breadwallet.presenter.fragments.IntroNewWalletFragment;
import com.breadwallet.presenter.fragments.IntroRecoverWalletFragment;
import com.breadwallet.presenter.fragments.IntroWarningFragment;
import com.breadwallet.presenter.fragments.IntroWelcomeFragment;
import com.breadwallet.tools.animation.BackgroundMovingAnimator;
import com.breadwallet.wallet.BRPeerManager;
import com.breadwallet.wallet.BRWalletManager;

import java.util.List;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 8/4/15.
 * Copyright (c) 2015 Mihail Gutan <mihail@breadwallet.com>
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

        leftButton = (Button) findViewById(R.id.intro_left_button);
        leftButton.setVisibility(View.GONE);
        leftButton.setClickable(false);
        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        getFragmentManager().beginTransaction().add(R.id.intro_layout, new IntroWelcomeFragment(),
                IntroWelcomeFragment.class.getName()).commit();
        startTheWalletIfExists();

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
    protected void onSaveInstanceState(Bundle outState) {

    }

    private void showRecoverNewWalletFragment() {
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

    private void startTheWalletIfExists() {
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
                        showRecoverNewWalletFragment();
                    }
                }, 800);
            } else {
                Log.e(TAG, "should go to the current wallet");
                startMainActivity();
            }

        }
    }



}
