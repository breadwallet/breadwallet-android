package com.breadwallet.tools;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.breadwallet.presenter.fragments.FragmentSettingsAll;

import org.w3c.dom.Text;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on 6/21/16.
 * Copyright (c) 2016 breadwallet llc <mihail@breadwallet.com>
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
public class Utils {
    public static final String TAG = Utils.class.getName();

    public static void overrideFonts(TextView... v) {

        if (v == null) return;

        Typeface FONT_REGULAR = Typeface.create("sans-serif-light", Typeface.NORMAL);

        for (TextView view : v) {
            try {
                if (view != null) {
                    view.setTypeface(FONT_REGULAR);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }


    }
}
