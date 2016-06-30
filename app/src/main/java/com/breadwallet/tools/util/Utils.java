package com.breadwallet.tools.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Typeface;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.breadwallet.presenter.activities.MainActivity;

import java.util.List;

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

    public static boolean isUsingCustomInputMethod(Activity context) {
        if(context == null) return false;
        InputMethodManager imm = (InputMethodManager) context.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        List<InputMethodInfo> mInputMethodProperties = imm.getEnabledInputMethodList();
        final int N = mInputMethodProperties.size();
        for (int i = 0; i < N; i++) {
            InputMethodInfo imi = mInputMethodProperties.get(i);
            if (imi.getId().equals(
                    Settings.Secure.getString(context.getContentResolver(),
                            Settings.Secure.DEFAULT_INPUT_METHOD))) {
                if ((imi.getServiceInfo().applicationInfo.flags &
                        ApplicationInfo.FLAG_SYSTEM) == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    public static void printPhoneSpecs() {
        String specsTag = "PHONE SPECS";
        Log.e(specsTag, "");
        Log.e(specsTag, "***************************PHONE SPECS***************************");
        Log.e(specsTag, "* screen X: " + MainActivity.screenParametersPoint.x + " , screen Y: " + MainActivity.screenParametersPoint.y);
        Log.e(specsTag, "* Build.CPU_ABI: " + Build.CPU_ABI);
        Runtime rt = Runtime.getRuntime();
        long maxMemory = rt.maxMemory();
        Log.e(specsTag, "* maxMemory:" + Long.toString(maxMemory));
        Log.e(specsTag, "----------------------------PHONE SPECS----------------------------");
        Log.e(specsTag, "");
    }

    public static boolean isEmulatorOrDebug() {
        String fing = Build.FINGERPRINT;
        boolean isEmulator = false;
        if (fing != null) {
            isEmulator = fing.contains("vbox") || fing.contains("generic");
        }
        return isEmulator;
    }
}
