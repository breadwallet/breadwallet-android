package com.breadwallet;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Point;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Handler;
import android.view.Display;
import android.view.WindowManager;

import com.breadwallet.tools.sqlite.BRSQLiteHelper;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.tools.sqlite.MerkleBlockDataSource;
import com.breadwallet.tools.sqlite.PeerDataSource;
import com.breadwallet.tools.sqlite.TransactionDataSource;
import com.platform.sqlite.PlatformSqliteHelper;

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

public class BreadApp extends Application {
    private static final String TAG = BreadApp.class.getName();
    public static int DISPLAY_HEIGHT_PX;
    FingerprintManager mFingerprintManager;
    // host is the server(s) on which the API is hosted
    public static String HOST = "api.breadwallet.com";
    private static List<OnAppBackgrounded> listeners;

    private static Activity currentActivity;
    private static volatile BreadApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // initialize DB stuff
        PlatformSqliteHelper.getInstance();
        BRSQLiteHelper.getInstance();
        CurrencyDataSource.getInstance();
        MerkleBlockDataSource.getInstance();
        PeerDataSource.getInstance();
        TransactionDataSource.getInstance();

        if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            HOST = "stage.breadwallet.com";
        }

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int DISPLAY_WIDTH_PX = size.x;
        DISPLAY_HEIGHT_PX = size.y;
        mFingerprintManager = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);
    }


    public static Activity getCurrentActivity() {
//        Log.e(TAG, "getCurrentActivity: " + currentActivity.getClass().getName());
        return currentActivity;
    }

    public static BreadApp getInstance() {
        return instance;
    }

    public static void setBreadContext(Activity app) {
        currentActivity = app;
        if (app == null) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isApplicationSentToBackground(currentActivity)) fireListeners();
                }
            }, 500);
        }
    }

    public static void fireListeners() {
        for (OnAppBackgrounded lis : listeners) lis.onBackgrounded();
    }

    public static void addOnBackgroundedListener(OnAppBackgrounded listener) {
        if (listeners == null) listeners = new ArrayList<>();
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public static boolean isApplicationSentToBackground(final Context context) {
        if (context == null) return true;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (!tasks.isEmpty()) {
            ComponentName topActivity = tasks.get(0).topActivity;
            if (!topActivity.getPackageName().equals(context.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAnyActivityOn() {
        //        if(!on) Log.e(TAG, "isAnyActivityOn: NO ACTIVITY ON");
        return getCurrentActivity() != null;
    }

    public interface OnAppBackgrounded {
        void onBackgrounded();
    }
}
