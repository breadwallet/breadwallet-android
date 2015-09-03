package com.breadwallet.tools.listeners;

import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.breadwallet.tools.adapter.AmountAdapter;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 9/1/15.
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
public class BackPressCustomKeyboardOnTouchListener implements View.OnTouchListener {
    public static final String TAG = "OnTouchListener";
    private final int LONG_CLICK_DURATION = 500;
    private Handler handler = new Handler();

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    AmountAdapter.resetKeyboard();
                    Log.e(TAG, "AmountAdapter.resetKeyboard()");
                }
            }, LONG_CLICK_DURATION);
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            handler.removeCallbacksAndMessages(null);
            String tmp = ((Button) v).getText().toString();
            AmountAdapter.preConditions(tmp);
        }
        return true;
    }
}
