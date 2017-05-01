package com.breadwallet;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.hardware.fingerprint.FingerprintManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.breadwallet.presenter.activities.BreadActivity;
import com.breadwallet.presenter.activities.ImportActivity;
import com.breadwallet.presenter.activities.IntroActivity;
import com.breadwallet.presenter.activities.IntroPhraseCheckActivity;
import com.breadwallet.presenter.activities.IntroPhraseProveActivity;
import com.breadwallet.presenter.activities.IntroReEnterPinActivity;
import com.breadwallet.presenter.activities.IntroRecoverActivity;
import com.breadwallet.presenter.activities.IntroRecoverWordsActivity;
import com.breadwallet.presenter.activities.IntroSetPitActivity;
import com.breadwallet.presenter.activities.IntroWriteDownActivity;
import com.breadwallet.presenter.activities.PinActivity;
import com.breadwallet.presenter.activities.RestoreActivity;
import com.breadwallet.presenter.activities.ScanQRActivity;
import com.breadwallet.presenter.activities.UpdatePitActivity;
import com.breadwallet.presenter.activities.settings.AboutActivity;
import com.breadwallet.presenter.activities.settings.DefaultCurrencyActivity;
import com.breadwallet.presenter.activities.settings.NotificationActivity;
import com.breadwallet.presenter.activities.settings.SecurityCenterActivity;
import com.breadwallet.presenter.activities.settings.SettingsActivity;
import com.breadwallet.presenter.activities.settings.ShareDataActivity;
import com.breadwallet.presenter.activities.settings.SpendLimitActivity;
import com.breadwallet.presenter.activities.settings.SyncBlockchainActivity;
import com.breadwallet.presenter.activities.settings.WebViewActivity;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/22/15.
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

public class BreadWalletApp extends Application {
    private static final String TAG = BreadWalletApp.class.getName();
    public static int DISPLAY_HEIGHT_PX;
    FingerprintManager mFingerprintManager;

    private static Activity currentActivity;


    @Override
    public void onCreate() {
        super.onCreate();

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int DISPLAY_WIDTH_PX = size.x;
        DISPLAY_HEIGHT_PX = size.y;
        mFingerprintManager = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);

    }

    public static Activity getBreadContext() {
//        Log.e(TAG, "getBreadContext: " + currentActivity.getClass().getName());
        return currentActivity;
    }

    public static void setBreadContext(Activity app) {
//        Log.e(TAG, "setBreadContext: " + app.getClass().getName());
        currentActivity = app;
    }

    public static boolean isAnyActivityOn() {
        boolean on = getBreadContext() != null;
//        if(!on) Log.e(TAG, "isAnyActivityOn: NO ACTIVITY ON");
        return on;
    }
}
