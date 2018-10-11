package com.breadwallet.tools.manager;

import android.util.Log;

import com.crashlytics.android.Crashlytics;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 10/17/17.
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
public class BRReportsManager {

    private static final String TAG = BRReportsManager.class.getName();

    private BRReportsManager() {
    }

    public static void reportBug(RuntimeException er, boolean crash) {
        Log.e(TAG, "reportBug: ", er);
        try {
//            Crashlytics.logException(er);
        } catch (Exception e) {
            Log.e(TAG, "reportBug: failed to report to FireBase: ", e);
        }
        if (crash) throw er;
    }

    public static void reportBug(Exception er) {
        Log.e(TAG, "reportBug: ", er);
        try {
//            Crashlytics.logException(er);
        } catch (Exception e) {
            Log.e(TAG, "reportBug: failed to report to FireBase: ", e);
        }
    }
}
