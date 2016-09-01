package com.breadwallet.tools.animation;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.BounceInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import com.breadwallet.R;
import com.breadwallet.presenter.fragments.FragmentSettingsAll;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/24/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class SpringAnimator {
    private static final String TAG = SpringAnimator.class.getName();

    public static void showExpandCameraGuide(final View view) {
        if (view != null) {
            view.setVisibility(View.GONE);
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ScaleAnimation trans = new ScaleAnimation(0.0f, 1f, 0.0f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                trans.setDuration(800);
                trans.setInterpolator(new DecelerateOvershootInterpolator(1.5f, 2.5f));
                if (view != null) {
                    view.setVisibility(View.VISIBLE);
                    view.startAnimation(trans);
                }
            }
        }, 200);

    }

    /**
     * Shows the springy animation on views
     */
    public static void showAnimation(final View view) {
        if (view == null) return;
        ScaleAnimation trans = new ScaleAnimation(0.6f, 1f, 0.6f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        trans.setDuration(500);
        trans.setInterpolator(new DecelerateOvershootInterpolator(1.3f, 3f));
        view.setVisibility(View.VISIBLE);
        view.startAnimation(trans);

    }

    /**
     * Shows the springy bubble animation on views
     */
    public static void showBubbleAnimation(final View view) {
        if (view == null) return;
        ScaleAnimation trans = new ScaleAnimation(0.75f, 1f, 0.75f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        trans.setDuration(300);
        trans.setInterpolator(new DecelerateOvershootInterpolator(1.0f, 1.85f));
        view.setVisibility(View.VISIBLE);
        view.startAnimation(trans);
    }

    public static void failShakeAnimation(Activity context, View view) {
        if (view == null) return;
        Animation shake = AnimationUtils.loadAnimation(context, R.anim.shake);
        view.setVisibility(View.VISIBLE);
        view.startAnimation(shake);
    }

}