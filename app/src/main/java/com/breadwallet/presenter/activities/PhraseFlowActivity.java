package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;

import com.breadwallet.R;
import com.breadwallet.presenter.fragments.FragmentPhraseFlow1;
import com.breadwallet.presenter.fragments.FragmentPhraseFlow2;
import com.breadwallet.presenter.fragments.FragmentPhraseFlow3;
import com.breadwallet.presenter.fragments.FragmentRecoveryPhrase;
import com.breadwallet.presenter.fragments.IntroNewRecoverFragment;
import com.breadwallet.presenter.fragments.IntroNewWalletFragment;
import com.breadwallet.presenter.fragments.IntroWelcomeFragment;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.DecelerateOvershootInterpolator;
import com.breadwallet.tools.manager.CurrencyManager;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.security.PostAuthenticationProcessor;
import com.breadwallet.tools.util.BRConstants;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 8/20/16.
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


public class PhraseFlowActivity extends Activity {
    public static final String TAG = PhraseFlowActivity.class.getName();
    public static Point screenParametersPoint = new Point();
    public FragmentPhraseFlow1 fragmentPhraseFlow1;
    public FragmentPhraseFlow2 fragmentPhraseFlow2;
    public FragmentPhraseFlow3 fragmentPhraseFlow3;
    public FragmentRecoveryPhrase fragmentRecoveryPhrase;
    public PhraseFlowActivity phraseFlowActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phrase_flow);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getColor(R.color.status_bar));

        phraseFlowActivity = this;

        fragmentPhraseFlow1 = new FragmentPhraseFlow1();
        fragmentPhraseFlow2 = new FragmentPhraseFlow2();
        fragmentPhraseFlow3 = new FragmentPhraseFlow3();
        fragmentRecoveryPhrase = new FragmentRecoveryPhrase();
        int layoutID = R.id.main_layout;

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        fragmentTransaction.add(layoutID, fragmentPhraseFlow1,
                IntroWelcomeFragment.class.getName());
        fragmentTransaction.add(layoutID, fragmentPhraseFlow2,
                IntroNewRecoverFragment.class.getName());
        fragmentTransaction.add(layoutID, fragmentPhraseFlow3,
                IntroNewWalletFragment.class.getName());
        fragmentTransaction.add(layoutID, fragmentRecoveryPhrase,
                IntroNewWalletFragment.class.getName());

        showHideFragments();
        fragmentTransaction.commitAllowingStateLoss();
        PostAuthenticationProcessor.getInstance().onShowPhraseFlowAuth(phraseFlowActivity, false);

    }

    // direction == 1 -> RIGHT, direction == 2 -> LEFT
    public void animateSlide(final Fragment from, final Fragment to, int direction) {
        int screenWidth = screenParametersPoint.x;
        int screenHeigth = screenParametersPoint.y;

        showHideFragments(from, to);
        TranslateAnimation transFrom = direction == IntroActivity.RIGHT ?
                new TranslateAnimation(0, -screenWidth, 0, 0) : new TranslateAnimation(0, screenWidth, 0, 0);
        transFrom.setDuration(BRAnimator.horizontalSlideDuration);
        transFrom.setInterpolator(new DecelerateOvershootInterpolator(1f, 0.5f));
        View fromView = from.getView();
        if (fromView != null)
            fromView.startAnimation(transFrom);
        TranslateAnimation transTo = direction == IntroActivity.RIGHT ?
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

    public void showHideFragments(Fragment... fragments) {
        if(fragments.length == 0) return;
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.hide(fragmentPhraseFlow1);
        fragmentTransaction.hide(fragmentPhraseFlow2);
        fragmentTransaction.hide(fragmentPhraseFlow3);
        fragmentTransaction.hide(fragmentRecoveryPhrase);
        for (Fragment f : fragments) {
            fragmentTransaction.show(f);
        }
        fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BRConstants.SHOW_PHRASE_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    PostAuthenticationProcessor.getInstance().onShowPhraseFlowAuth(this, true);
                } else {
                    onBackPressed();
                }
                break;

        }
    }

    @Override
    public void onBackPressed() {
        if (fragmentPhraseFlow3 != null && fragmentPhraseFlow3.isVisible()) {
            animateSlide(fragmentPhraseFlow3, fragmentPhraseFlow2, IntroActivity.LEFT);
            fragmentPhraseFlow2.setPhrase(fragmentPhraseFlow3.getPhrase());
        } else {
            if (CurrencyManager.getInstance(this).getBALANCE() > SharedPreferencesManager.getLimit(this)
                    && !SharedPreferencesManager.getPhraseWroteDown(this)) {
                super.onBackPressed();
            } else {
                Intent intent;
                intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                if (!isDestroyed()) {
                    finish();
                }
            }
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releasePhrase();

    }

    private void releasePhrase() {
        fragmentPhraseFlow1.releasePhrase();
        fragmentPhraseFlow3.releasePhrase();
        fragmentRecoveryPhrase.releasePhrase();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
