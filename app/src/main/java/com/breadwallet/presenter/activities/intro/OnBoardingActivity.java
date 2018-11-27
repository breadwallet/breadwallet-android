/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 11/14/18.
 * Copyright (c) 2018 breadwallet LLC
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
package com.breadwallet.presenter.activities.intro;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;

import com.breadwallet.R;

import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.breadwallet.presenter.activities.InputPinActivity;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.tools.animation.OnBoardingAnimationManager;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.security.PostAuth;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.platform.APIClient;
import com.platform.HTTPServer;

/**
 * New on boarding activity
 */

public class OnBoardingActivity extends BRActivity {
    private static final String TAG = OnBoardingActivity.class.getSimpleName();
    public static final float SCALE_TO_ANIMATION = 1.5f;
    private static final int ANIMATION_DURATION = 300;
    private static final int BUTTONS_ANIMATION_DELAY = 500;
    private static final float PRIMARY_TEXT_POSITION = 240f;
    private static final float SECONDARY_TEXT_POSITION = 500f;
    private static final int FIRST_SCENE = 1;
    private static final int SECOND_SCENE = 2;
    public static final int THIRD_SCENE = 3;
    private ImageView mAnimationImageView;
    private BaseTextView mPrimaryText;
    private BaseTextView mSecondaryText;
    private BRButton mButtonBuy;
    private BRButton mButtonBrowse;
    private BRButton mButtonNext;
    private ImageButton mButtonBack;
    private static NEXT_SCREEN mNextScreen;

    private int mCurrentScene;
    private float mBuyButtonY;
    private float mBrowseButtonY;
    private float mPrimaryTextTranslationY;
    private float mImageViewScaleX;
    private float mImageViewScaleY;

    private enum NEXT_SCREEN {
        BUY_SCREEN,
        HOME_SCREEN
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_on_boarding);
        mButtonBuy = findViewById(R.id.button_buy);
        mButtonBrowse = findViewById(R.id.button_browse);
        mButtonNext = findViewById(R.id.button_next);
        mButtonBack = findViewById(R.id.button_back);
        mPrimaryText = findViewById(R.id.primary_text);
        mSecondaryText = findViewById(R.id.secondary_text);
        mAnimationImageView = findViewById(R.id.animation_image_view);
        mBuyButtonY = mButtonBuy.getY();
        mBrowseButtonY = mButtonBrowse.getY();
        mImageViewScaleX = mAnimationImageView.getScaleX();
        mImageViewScaleY = mAnimationImageView.getScaleY();
        mPrimaryTextTranslationY = mPrimaryText.getTranslationY();
        mButtonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAnimation(++mCurrentScene);
            }
        });
        mButtonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        mButtonBuy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BRKeyStore.getPinCode(OnBoardingActivity.this).length() > 0) {
                    showBuyScreen();
                } else {
                    mNextScreen = NEXT_SCREEN.BUY_SCREEN;
                    setupPin();
                }
            }
        });
        mButtonBrowse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BRKeyStore.getPinCode(OnBoardingActivity.this).length() > 0) {
                    UiUtils.startBreadActivity(OnBoardingActivity.this, true);
                } else {
                    mNextScreen = NEXT_SCREEN.HOME_SCREEN;
                    setupPin();
                }
            }
        });
        showAnimation(++mCurrentScene);
        moveButtonsAway();
    }

    private void setupPin() {
        PostAuth.getInstance().onCreateWalletAuth(OnBoardingActivity.this, false, new PostAuth.AuthenticationSuccessListener() {
            @Override
            public void onAuthenticatedSuccess() {
                APIClient.getInstance(OnBoardingActivity.this).updatePlatform(OnBoardingActivity.this);
                Intent intent = new Intent(OnBoardingActivity.this, InputPinActivity.class);
                OnBoardingActivity.this.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                OnBoardingActivity.this.startActivityForResult(intent, InputPinActivity.SET_PIN_REQUEST_CODE);
            }
        });
    }

    private void moveButtonsAway() {
        mButtonBuy.setY(BRSharedPrefs.getScreenHeight(this));
        mButtonBrowse.setY(BRSharedPrefs.getScreenHeight(this));
    }

    private void animateButtonAppearance() {
        mButtonBuy.animate().translationY(mBuyButtonY).setInterpolator(new DecelerateInterpolator()).setDuration(ANIMATION_DURATION);
        mButtonBrowse.animate().translationY(mBrowseButtonY).setInterpolator(new DecelerateInterpolator()).setDuration(ANIMATION_DURATION);
    }

    private void showAnimation(int sceneNumber) {
        switch (sceneNumber) {
            case FIRST_SCENE:
                //Scale to larger animation for the first portion due to the globe being cropped for a thinner device like ios.
                mAnimationImageView.animate().scaleX(SCALE_TO_ANIMATION).scaleY(SCALE_TO_ANIMATION).setDuration(0);
                mPrimaryText.setText(getString(R.string.OnboardingPageTwo_title));
                mPrimaryText.animate().alpha(1).y(PRIMARY_TEXT_POSITION)
                        .setStartDelay(ANIMATION_DURATION).setDuration(ANIMATION_DURATION)
                        .setInterpolator(new DecelerateInterpolator());
                mSecondaryText.setText(getString(R.string.OnboardingPageTwo_subtitle));
                mSecondaryText.animate().alpha(1).y(SECONDARY_TEXT_POSITION)
                        .setDuration(ANIMATION_DURATION).setInterpolator(new DecelerateInterpolator());
                break;
            case SECOND_SCENE:
                mButtonBack.setVisibility(View.GONE);
                mAnimationImageView.animate().scaleX(mImageViewScaleX).scaleY(mImageViewScaleY).setDuration(ANIMATION_DURATION);
                mSecondaryText.animate().alpha(0)
                        .setDuration(ANIMATION_DURATION).setInterpolator(new DecelerateInterpolator());
                mPrimaryText.animate().alpha(0)
                        .setDuration(ANIMATION_DURATION).setInterpolator(new DecelerateInterpolator()).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        mPrimaryText.setText(getString(R.string.OnboardingPageThree_title));
                        mPrimaryText.setTranslationY(mPrimaryTextTranslationY);
                        mPrimaryText.animate().alpha(1).y(PRIMARY_TEXT_POSITION)
                                .setDuration(ANIMATION_DURATION).setInterpolator(new DecelerateInterpolator());
                        mSecondaryText.setText(getString(R.string.OnboardingPageThree_subtitle));
                        mSecondaryText.animate().alpha(1).y(SECONDARY_TEXT_POSITION)
                                .setDuration(ANIMATION_DURATION).setInterpolator(new DecelerateInterpolator());
                    }
                });
                break;
            case THIRD_SCENE:
                mAnimationImageView.animate().y(BRSharedPrefs.getScreenHeight(this));
                mButtonNext.animate().translationY(SECONDARY_TEXT_POSITION).alpha(0);
                mSecondaryText.animate().alpha(0)
                        .setDuration(ANIMATION_DURATION).setInterpolator(new DecelerateInterpolator());
                mPrimaryText.animate().alpha(0)
                        .setDuration(ANIMATION_DURATION).setInterpolator(new DecelerateInterpolator()).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        mPrimaryText.setText(getString(R.string.OnboardingPageFour_title));
                        mPrimaryText.setTranslationY(mPrimaryTextTranslationY);
                        mPrimaryText.animate().alpha(1)
                                .setDuration(ANIMATION_DURATION).setInterpolator(new DecelerateInterpolator());
                    }
                });
                break;

        }
        createAnimationAndStart(sceneNumber);
    }

    private void createAnimationAndStart(int sceneNumber) {
        AnimationDrawable animationDrawable = OnBoardingAnimationManager.getAnimationDrawable(sceneNumber);
        mAnimationImageView.setImageDrawable(animationDrawable);
        animationDrawable.setOneShot(true);
        animationDrawable.start();
        if (sceneNumber == THIRD_SCENE) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    animateButtonAppearance();
                }
            }, BUTTONS_ANIMATION_DELAY);
            OnBoardingAnimationManager.disposeAnimationFrames();
        } else {
            moveButtonsAway();
        }
    }

    private void showBuyScreen() {
        String url = String.format(BRConstants.CURRENCY_PARAMETER_STRING_FORMAT, HTTPServer.URL_BUY,
                WalletBitcoinManager.getInstance(this).getCurrencyCode());
        UiUtils.startWebActivity(this, url);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == InputPinActivity.SET_PIN_REQUEST_CODE) {
            if (data != null) {
                boolean isPinAccepted = data.getBooleanExtra(InputPinActivity.EXTRA_PIN_ACCEPTED, false);
                if (isPinAccepted) {
                    switch (mNextScreen) {
                        case BUY_SCREEN:
                            showBuyScreen();
                            break;
                        case HOME_SCREEN:
                            UiUtils.startBreadActivity(this, false);
                            break;
                    }
                }
            }
        }
    }
}

