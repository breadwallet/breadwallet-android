
package com.breadwallet.presenter.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.breadwallet.R;
import com.breadwallet.presenter.fragments.IntroNewRecoverFragment;
import com.breadwallet.presenter.fragments.IntroNewWalletFragment;
import com.breadwallet.presenter.fragments.IntroRecoverWalletFragment;
import com.breadwallet.presenter.fragments.IntroWarningFragment;
import com.breadwallet.presenter.fragments.IntroWelcomeFragment;


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
    public static final String TAG = IntroActivity.class.getName();
    ImageView background;
    RelativeLayout layout;
    boolean noWallet = true;
    private static final int RightToLeft = 1;
    private static final int LeftToRight = 2;
    private static final int DURATION = 30000;

    private ValueAnimator mCurrentAnimator;
    private final Matrix mMatrix = new Matrix();
    private float mScaleFactor;
    private int mDirection = RightToLeft;
    private RectF mDisplayRect = new RectF();
    private IntroWelcomeFragment introWelcomeFragment;
    private IntroNewRecoverFragment introNewRestoreFragment;
    private IntroNewWalletFragment introNewWalletFragment;
    private IntroWarningFragment introWarningFragment;
    private IntroRecoverWalletFragment introRecoverWalletFragment;
    private Button leftButton;
    private boolean backPressAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        introWelcomeFragment = new IntroWelcomeFragment();
        introNewRestoreFragment = new IntroNewRecoverFragment();
        introNewWalletFragment = new IntroNewWalletFragment();
        introWarningFragment = new IntroWarningFragment();
        introRecoverWalletFragment = new IntroRecoverWalletFragment();
        leftButton = (Button) findViewById(R.id.intro_left_button);

        background = (ImageView) findViewById(R.id.intro_bread_wallet_image);
        background.setScaleType(ImageView.ScaleType.MATRIX);
        leftButton.setVisibility(View.GONE);
        leftButton.setClickable(false);

        background.post(new Runnable() {
            @Override
            public void run() {
                mScaleFactor = (float) background.getHeight() /
                        (float) background.getDrawable().getIntrinsicHeight();
                mMatrix.postScale(mScaleFactor, mScaleFactor);
                background.setImageMatrix(mMatrix);
                animate();
            }
        });

        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        getSupportFragmentManager().beginTransaction().add(R.id.intro_layout, introWelcomeFragment,
                "introWelcomeFragment").commit();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (noWallet) {
                    Log.e(TAG, "should create new wallet");
                    showRecoverNewWalletFragment();
                } else {
                    Log.e(TAG, "should go to the current wallet");
                    startMainActivity();
                }

            }
        }, 800);
    }

    void showRecoverNewWalletFragment() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.animator.from_right, R.animator.to_left);
        fragmentTransaction.replace(introWelcomeFragment.getId(), introNewRestoreFragment);
        fragmentTransaction.commit();
    }

    public void showNewWalletFragment() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.addToBackStack(null);
        leftButton.setVisibility(View.VISIBLE);
        leftButton.setClickable(true);
        fragmentTransaction.setCustomAnimations(R.animator.from_right, R.animator.to_left);
        fragmentTransaction.replace(introNewRestoreFragment.getId(), introNewWalletFragment);
        fragmentTransaction.commit();
        backPressAvailable = true;
    }

    public void showRecoverWalletFragment() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.addToBackStack(null);
        leftButton.setVisibility(View.VISIBLE);
        leftButton.setClickable(true);
        fragmentTransaction.setCustomAnimations(R.animator.from_right, R.animator.to_left);
        fragmentTransaction.replace(introNewRestoreFragment.getId(), introRecoverWalletFragment);
        fragmentTransaction.commit();
        backPressAvailable = true;
    }

    public void showWarningFragment() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(introNewWalletFragment.getId(), introWarningFragment);
        introNewWalletFragment.introGenerate.setClickable(false);
        leftButton.setVisibility(View.GONE);
        leftButton.setClickable(false);
        fragmentTransaction.commit();
        backPressAvailable = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void animate() {
        updateDisplayRect();
        if (mDirection == RightToLeft) {
            animate(mDisplayRect.left, mDisplayRect.left -
                    (mDisplayRect.right - background.getWidth()));
        } else {
            animate(mDisplayRect.left, 0.0f);
        }
    }

    private void animate(float from, float to) {
        mCurrentAnimator = ValueAnimator.ofFloat(from, to);
        mCurrentAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (Float) animation.getAnimatedValue();

                mMatrix.reset();
                mMatrix.postScale(mScaleFactor, mScaleFactor);
                mMatrix.postTranslate(value, 0);

                background.setImageMatrix(mMatrix);

            }
        });
        mCurrentAnimator.setDuration(DURATION);
        mCurrentAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mDirection == RightToLeft)
                    mDirection = LeftToRight;
                else
                    mDirection = RightToLeft;

                animate();
            }
        });
        mCurrentAnimator.start();
    }

    private void updateDisplayRect() {
        mDisplayRect.set(0, 0, background.getDrawable().getIntrinsicWidth(),
                background.getDrawable().getIntrinsicHeight());
        mMatrix.mapRect(mDisplayRect);
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
        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            leftButton.setVisibility(View.GONE);
            leftButton.setClickable(false);
        }
        if (backPressAvailable)
            super.onBackPressed();

    }
}
