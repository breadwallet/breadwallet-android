package com.breadwallet;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Point;
import android.hardware.fingerprint.FingerprintManager;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.tools.listeners.SyncReceiver;
import com.breadwallet.tools.util.Utils;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import timber.log.Timber;

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
    public static int DISPLAY_HEIGHT_PX;
    FingerprintManager mFingerprintManager;
    public static String HOST = "api.loafwallet.org";
    private static List<OnAppBackgrounded> listeners;
    private static Timer isBackgroundChecker;
    public static AtomicInteger activityCounter = new AtomicInteger();
    public static long backgroundedTime;

    private static Activity currentActivity;

    @Override
    public void onCreate() {
        super.onCreate();
        boolean enableCrashlytics = true;
        if (Utils.isEmulatorOrDebug(this)) {
//            BRKeyStore.putFailCount(0, this);
            enableCrashlytics = false;
        }

        // setup Timber
        Timber.plant(BuildConfig.DEBUG ? new Timber.DebugTree() : new CrashReportingTree());

        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enableCrashlytics);

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        DISPLAY_HEIGHT_PX = size.y;
        mFingerprintManager = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);
    }


    public static Context getBreadContext() {
        return currentActivity == null ? SyncReceiver.app : currentActivity;
    }

    public static void setBreadContext(Activity app) {
        currentActivity = app;
    }

    public static void fireListeners() {
        if (listeners == null) return;
        for (OnAppBackgrounded lis : listeners) lis.onBackgrounded();
    }

    public static void addOnBackgroundedListener(OnAppBackgrounded listener) {
        if (listeners == null) listeners = new ArrayList<>();
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public static boolean isAppInBackground(final Context context) {
        return context == null || activityCounter.get() <= 0;
    }

    //call onStop on evert activity so
    public static void onStop(final BRActivity app) {
        if (isBackgroundChecker != null) isBackgroundChecker.cancel();
        isBackgroundChecker = new Timer();
        TimerTask backgroundCheck = new TimerTask() {
            @Override
            public void run() {
                if (isAppInBackground(app)) {
                    backgroundedTime = System.currentTimeMillis();
                    Timber.d("App went in background!");
                    // APP in background, do something
                    isBackgroundChecker.cancel();
                    fireListeners();
                }
                // APP in foreground, do something else
            }
        };

        isBackgroundChecker.schedule(backgroundCheck, 500, 500);
    }

    public interface OnAppBackgrounded {
        void onBackgrounded();
    }

    private class CrashReportingTree extends Timber.Tree {
        private static final String CRASHLYTICS_KEY_PRIORITY = "priority";
        private static final String CRASHLYTICS_KEY_TAG = "tag";
        private static final String CRASHLYTICS_KEY_MESSAGE = "message";

        @Override
        protected void log(int priority, String tag, String message, Throwable throwable) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG) {
                return;
            }

            Throwable t = throwable != null ? throwable : new Exception(message);

            // Firebase Crash Reporting
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.setCustomKey(CRASHLYTICS_KEY_PRIORITY, priority);
            crashlytics.setCustomKey(CRASHLYTICS_KEY_TAG, tag);
            crashlytics.setCustomKey(CRASHLYTICS_KEY_MESSAGE, message);

            crashlytics.recordException(t);
        }
    }
}
