package com.breadwallet.presenter.customviews;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.BreadActivity;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 5/8/17.
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
public class BRNotificationBar extends android.support.v7.widget.Toolbar {

    private static final String TAG = BRNotificationBar.class.getName();

    private BreadActivity breadActivity;
    private BRText description;
    private BRButton close;

    public boolean[] filterSwitches = new boolean[4];

    public BRNotificationBar(Context context) {
        super(context);
        init(null);
    }

    public BRNotificationBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public BRNotificationBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        inflate(getContext(), R.layout.notification_bar, this);
        breadActivity = (BreadActivity) getContext();
        description = (BRText) findViewById(R.id.description);
        close = (BRButton) findViewById(R.id.cancel_button);

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.BRNotificationBar);
        final int N = a.getIndexCount();
        for (int i = 0; i < N; ++i) {
            int attr = a.getIndex(i);
            switch (attr) {
                case R.styleable.BRNotificationBar_breadText:
                    String text = a.getString(0);
                    description.setText(text);
                    break;
            }
        }
        a.recycle();

        close.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                breadActivity.barFlipper.setDisplayedChild(0);
            }
        });

    }

}