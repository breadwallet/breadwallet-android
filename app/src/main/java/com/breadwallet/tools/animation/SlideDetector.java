package com.breadwallet.tools.animation;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;


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
    private View view;
    private boolean moving;

    public SlideDetector(Context context, View view) {
        this.context = context;
        this.view = view;
        moving = false;
    }

    private GestureDetector gestureDetector = new GestureDetector(context,
            new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onScroll(MotionEvent start, MotionEvent event, float distanceX, float distanceY) {
                    if (moving) return true;
                    view.setTranslationY(event.getY() - start.getY());
                    if (event.getY() - start.getY() > 20) {
                        moving = true;
                        removeCurrentView();
                    }
                    return true;
                }
            });

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        gestureDetector.onTouchEvent(event);  // here we pass events to detector above
        if (event.getActionMasked() == MotionEvent.ACTION_UP && !moving) {
            view.setTranslationY(0);
        }
        return false;
    }

    private void removeCurrentView() {
        Log.e(TAG, "removeCurrentView: ");
        ((Activity) context).getFragmentManager().popBackStack();
    }
}
