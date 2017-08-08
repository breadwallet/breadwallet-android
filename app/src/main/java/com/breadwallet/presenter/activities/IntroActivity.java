
package com.breadwallet.presenter.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;

import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.BreadWalletApp;
import com.breadwallet.presenter.fragments.IntroNewRecoverFragment;
import com.breadwallet.presenter.fragments.IntroNewWalletFragment;
import com.breadwallet.presenter.fragments.IntroRecoverWalletFragment;
import com.breadwallet.presenter.fragments.IntroWarningFragment;
import com.breadwallet.presenter.fragments.IntroWelcomeFragment;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.animation.BackgroundMovingAnimator;
import com.breadwallet.tools.animation.DecelerateOvershootInterpolator;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.security.PostAuthenticationProcessor;
import com.breadwallet.tools.util.Utils;
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
    private Button leftButton;
    public static final int RIGHT = 1;
    public static final int LEFT = 2;

    private int newRecoverNoneFlag = 0;//0 - in IntroNewRecoverFragment, 1 - in IntroNewWalletFragment, 2 - IntroRecoverWalletFragment

    //loading the native library
    static {
        System.loadLibrary("core");

    }

    private boolean backNotAllowed = false;
    private IntroWelcomeFragment introWelcomeFragment;
    private IntroNewRecoverFragment introNewRecoverFragment;
    private IntroRecoverWalletFragment introRecoverWalletFragment;
    private IntroNewWalletFragment introNewWalletFragment;
    private IntroWarningFragment introWarningFragment;
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
//        BreadLibs.initNativeLib(this, "libCore.so");
        app = this;

        if (!BuildConfig.DEBUG && KeyStoreManager.AUTH_DURATION_SEC != 300) {
            Log.e(TAG, "onCreate: KeyStoreManager.AUTH_DURATION_SEC != 300");
            RuntimeException ex = new RuntimeException("AUTH_DURATION_SEC should be 300");
            FirebaseCrash.report(ex);
            throw ex;
        }

        getWindowManager().getDefaultDisplay().getSize(screenParametersPoint);
        leftButton = (Button) findViewById(R.id.intro_left_button);
        leftButton.setVisibility(View.GONE);
        leftButton.setClickable(false);
        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        byte[] masterPubKey;
        try {
            masterPubKey = KeyStoreManager.getMasterPublicKey(this);
        } catch (Exception e) {
            return;
        }
        boolean isFirstAddressCorrect = false;
        if (masterPubKey != null && masterPubKey.length != 0) {
            isFirstAddressCorrect = checkFirstAddress(masterPubKey);
        }
//        Log.e(TAG, "isFirstAddressCorrect: " + isFirstAddressCorrect);
        if (!isFirstAddressCorrect) {
            Log.e(TAG, "WARNING: isFirstAddressCorrect - false: CLEARING THE WALLET");

            BRWalletManager.getInstance(this).wipeWalletButKeystore(this);
        }
        createFragments();

        setStatusBarColor(0);

        PostAuthenticationProcessor.getInstance().onCanaryCheck(this, false);

    }

    private void createFragments() {
        introWelcomeFragment = new IntroWelcomeFragment();
        introNewRecoverFragment = new IntroNewRecoverFragment();
        introNewWalletFragment = new IntroNewWalletFragment();
        introRecoverWalletFragment = new IntroRecoverWalletFragment();
        introWarningFragment = new IntroWarningFragment();

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        fragmentTransaction.add(R.id.intro_layout, introWelcomeFragment,
                IntroWelcomeFragment.class.getName());
        fragmentTransaction.add(R.id.intro_layout, introNewRecoverFragment,
                IntroNewRecoverFragment.class.getName());
        fragmentTransaction.add(R.id.intro_layout, introNewWalletFragment,
                IntroNewWalletFragment.class.getName());
        fragmentTransaction.add(R.id.intro_layout, introRecoverWalletFragment,
                IntroRecoverWalletFragment.class.getName());
        fragmentTransaction.add(R.id.intro_layout, introWarningFragment,
                IntroWarningFragment.class.getName());

        showHideFragments(introWelcomeFragment);
        newRecoverNoneFlag = 0;
        fragmentTransaction.commitAllowingStateLoss();
    }

    private void showHideFragments(Fragment... fragments) {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.hide(introWelcomeFragment);
        fragmentTransaction.hide(introNewRecoverFragment);
        fragmentTransaction.hide(introNewWalletFragment);
        fragmentTransaction.hide(introRecoverWalletFragment);
        fragmentTransaction.hide(introWarningFragment);
        for (Fragment f : fragments) {
            fragmentTransaction.show(f);
        }
        if (!isDestroyed())
            fragmentTransaction.commitAllowingStateLoss();
    }

    private void setStatusBarColor(int mode) {
        if (mode == 0) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getColor(R.color.intro_status_bar));
        } else {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getColor(R.color.warning_status_bar));
        }
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
        animateSlide(introWelcomeFragment, introNewRecoverFragment, RIGHT);

    }

    public void showNewWalletFragment() {
        newRecoverNoneFlag = 1;
        leftButton.setVisibility(View.VISIBLE);
        leftButton.setClickable(true);
        animateSlide(introNewRecoverFragment, introNewWalletFragment, RIGHT);
    }

    public void showRecoverWalletFragment() {
        newRecoverNoneFlag = 2;
        leftButton.setVisibility(View.VISIBLE);
        leftButton.setClickable(true);
        animateSlide(introNewRecoverFragment, introRecoverWalletFragment, RIGHT);
        introRecoverWalletFragment.showKeyBoard(true);
    }

    public void showWarningFragment() {
        setStatusBarColor(1);
        newRecoverNoneFlag = 0;
        introNewWalletFragment.introGenerate.setClickable(false);
        leftButton.setVisibility(View.GONE);
        leftButton.setClickable(false);
        backNotAllowed = true;
        showHideFragments(introWarningFragment);
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
                    PostAuthenticationProcessor.getInstance().onCreateWalletAuth(this, true);
                } else {
                    Log.e(TAG, "WARNING: resultCode != RESULT_OK");
                    BRWalletManager m = BRWalletManager.getInstance(this);
                    m.wipeWalletButKeystore(this);
                    BRAnimator.resetFragmentAnimator();
                    finish();
                }
                break;
            case BRConstants.PUT_PHRASE_RECOVERY_WALLET_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    PostAuthenticationProcessor.getInstance().onRecoverWalletAuth(this, true);
                } else {
                    finish();
                }
                break;
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
        if (backNotAllowed) return;
        if (newRecoverNoneFlag == 1) {
            leftButton.setVisibility(View.GONE);
            leftButton.setClickable(false);
            animateSlide(introNewWalletFragment, introNewRecoverFragment, LEFT);
            newRecoverNoneFlag = 0;
        } else if (newRecoverNoneFlag == 2) {
            showHideFragments(introRecoverWalletFragment, introNewRecoverFragment);
            leftButton.setVisibility(View.GONE);
            leftButton.setClickable(false);
            animateSlide(introRecoverWalletFragment, introNewRecoverFragment, LEFT);
            newRecoverNoneFlag = 0;
            introRecoverWalletFragment.showKeyBoard(false);
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
                        showNewRecoverWalletFragment();
                    }
                }, 800);
            } else {
                startMainActivity();
            }

        }
    }

    // direction == 1 -> RIGHT, direction == 2 -> LEFT
    private void animateSlide(final Fragment from, final Fragment to, int direction) {
        if (to instanceof IntroRecoverWalletFragment) {
            if (Utils.isUsingCustomInputMethod(to.getActivity()))
                ((IntroRecoverWalletFragment) to).disableEditText();
        }
        int screenWidth = screenParametersPoint.x;
        int screenHeigth = screenParametersPoint.y;

        showHideFragments(from, to);
        TranslateAnimation transFrom = direction == RIGHT ?
                new TranslateAnimation(0, -screenWidth, 0, 0) : new TranslateAnimation(0, screenWidth, 0, 0);
        transFrom.setDuration(BRAnimator.horizontalSlideDuration);
        transFrom.setInterpolator(new DecelerateOvershootInterpolator(1f, 0.5f));
        View fromView = from.getView();
        if (fromView != null)
            fromView.startAnimation(transFrom);
        TranslateAnimation transTo = direction == RIGHT ?
                new TranslateAnimation(screenWidth, 0, 0, 0) : new TranslateAnimation(-screenWidth, 0, 0, 0);
        transTo.setDuration(BRAnimator.horizontalSlideDuration);
        transTo.setInterpolator(new DecelerateOvershootInterpolator(1f, 0.5f));
        transTo.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                showHideFragments(to);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        View toView = to.getView();
        if (toView != null)
            toView.startAnimation(transTo);
    }
}
