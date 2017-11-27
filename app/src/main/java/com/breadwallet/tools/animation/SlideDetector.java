package com.breadwallet.tools.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 5/9/17.
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
public class SlideDetector implements View.OnTouchListener {

    private static final String TAG = SlideDetector.class.getName();

    private Context context;
    private View _root;
    float origY;
    float dY;

    public SlideDetector(Context context, final View view) {
        this.context = context;
        _root = view;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                origY = _root.getY();
                dY = _root.getY() - event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (event.getRawY() + dY > origY)
                    _root.animate()
                            .y(event.getRawY() + dY)
                            .setDuration(0)
                            .start();
                break;
            case MotionEvent.ACTION_UP:
                if (_root.getY() > origY + _root.getHeight() / 5) {
                    _root.animate()
                            .y(_root.getHeight() * 2)
                            .setDuration(200)
                            .setInterpolator(new OvershootInterpolator(0.5f))
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    removeCurrentView();
                                }
                            })
                            .start();
                } else {
                    _root.animate()
                            .y(origY)
                            .setDuration(100)
                            .setInterpolator(new OvershootInterpolator(0.5f))
                            .start();
                }

                break;
            default:
                return false;
        }
        return true;
    }

    private void removeCurrentView() {
        ((Activity) context).getFragmentManager().popBackStack();
    }
}
