package com.breadwallet;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.os.Build;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.breadwallet.app.ApplicationLifecycleObserver;
import com.breadwallet.presenter.activities.DisabledActivity;
import com.breadwallet.protocols.messageexchange.MessageExchangeNetworkHelper;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.crypto.Base32;
import com.breadwallet.tools.crypto.CryptoHelper;
import com.breadwallet.tools.manager.BRApiManager;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.services.BRDFirebaseMessagingService;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.crashlytics.android.Crashlytics;
import com.platform.APIClient;
import com.platform.HTTPServer;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.fabric.sdk.android.Fabric;

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

public class BreadApp extends Application implements ApplicationLifecycleObserver.ApplicationLifecycleListener {
    private static final String TAG = BreadApp.class.getName();
    public static int mDisplayHeightPx;
    public static int mDisplayWidthPx;
    public static final String WALLET_ID_PATTERN = "^[A-Z0-9]*$";
    // host is the server(s) on which the API is hosted
    public static String HOST = "api.breadwallet.com";
    private static Context mContext;
    private static ApplicationLifecycleObserver mObserver;
    private static long mBackgroundedTime;
    private static Lifecycle.Event mLastApplicationEvent;
    private static final int NUMBER_OF_BYTES_FOR_SHA256_NEEDED = 10;
    private static final int LOCK_TIMEOUT = 180000; // 3 minutes in milliseconds

    private Runnable mDisconnectWalletsRunnable = new Runnable() {
        @Override
        public void run() {
            List<BaseWalletManager> list = new ArrayList<>(WalletsMaster.getInstance(BreadApp.this).getAllWallets(BreadApp.this));
            for (final BaseWalletManager walletManager : list) {
                //TODO Temporary new thread until the core lags are fixed
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        walletManager.disconnect(BreadApp.this);
                    }
                });
            }
        }
    };
    private Runnable mConnectWalletsRunnable = new Runnable() {
        @Override
        public void run() {
            List<BaseWalletManager> list = new ArrayList<>(WalletsMaster.getInstance(BreadApp.this).getAllWallets(BreadApp.this));
            for (final BaseWalletManager walletManager : list) {
                //TODO Temporary new thread until the core lags are fixed
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        walletManager.connect(BreadApp.this);
                    }
                });
            }
        }
    };

    private static final String PACKAGE_NAME = BreadApp.getBreadContext() == null ? null : BreadApp.getBreadContext().getApplicationContext().getPackageName();

    static {
        try {
            System.loadLibrary(BRConstants.NATIVE_LIB_NAME);
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
            Log.d(TAG, "Native code library failed to load.\\n\" + " + e);
            Log.d(TAG, "Installer Package Name -> " + (PACKAGE_NAME == null ? "null" : BreadApp.getBreadContext().getPackageManager().getInstallerPackageName(PACKAGE_NAME)));
        }
    }

    public static final boolean IS_ALPHA = false;

    public static final Map<String, String> mHeaders = new HashMap<>();

    private static Activity currentActivity;

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            HOST = "stage2.breadwallet.com";
        }

        final Fabric fabric = new Fabric.Builder(this)
                .kits(new Crashlytics.Builder().disabled(BuildConfig.DEBUG).build())
                .debuggable(BuildConfig.DEBUG)// Enables Crashlytics debugger
                .build();
        Fabric.with(fabric);

        mContext = this;

        if (!Utils.isEmulatorOrDebug(this) && IS_ALPHA)
            throw new RuntimeException("can't be alpha for release");

        boolean isTestVersion = APIClient.getInstance(this).isStaging();
        boolean isTestNet = BuildConfig.BITCOIN_TESTNET;
        String lang = getCurrentLocale(this);

        mHeaders.put(BRApiManager.HEADER_IS_INTERNAL, IS_ALPHA ? "true" : "false");
        mHeaders.put(BRApiManager.HEADER_TESTFLIGHT, isTestVersion ? "true" : "false");
        mHeaders.put(BRApiManager.HEADER_TESTNET, isTestNet ? "true" : "false");
        mHeaders.put(BRApiManager.HEADER_ACCEPT_LANGUAGE, lang);

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mDisplayWidthPx = size.x;
        mDisplayHeightPx = size.y;

        registerReceiver(InternetManager.getInstance(), new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        mObserver = new ApplicationLifecycleObserver();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(mObserver);
        addOnBackgroundedListener(this);
        addOnBackgroundedListener(MessageExchangeNetworkHelper.getInstance());

    }


    /**
     * Clears all app data from disk. This is equivalent to the user choosing to clear the app's data from within the
     * device settings UI. It erases all dynamic data associated with the app -- its private data and data in its
     * private area on external storage -- but does not remove the installed application itself, nor any OBB files.
     * It also revokes all runtime permissions that the app has acquired, clears all notifications and removes all
     * Uri grants related to this application.
     *
     * @throws IllegalStateException if the {@link ActivityManager} fails to wipe the user's data.
     */
    public static void clearApplicationUserData() {
        if (!((ActivityManager) mContext.getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData()) {
            throw new IllegalStateException(TAG + ": Failed to clear user application data.");
        }
    }

    public static void generateWalletIfIfNeeded(final Context context, String address) {

        String walletId = BRSharedPrefs.getWalletRewardId(context);
        if (Utils.isNullOrEmpty(walletId) || !walletId.matches(WALLET_ID_PATTERN)) {
            Log.e(TAG, "generateWalletIfIfNeeded: walletId is empty or faulty: " + walletId + ", generating again.");
            walletId = generateWalletId(context, address);
            if (!Utils.isNullOrEmpty(walletId) && walletId.matches(WALLET_ID_PATTERN)) {
                BRSharedPrefs.putWalletRewardId(context, walletId);
                // TODO: This is a hack.  Decouple FCM logic from rewards id generation logic.
                BRDFirebaseMessagingService.updateFcmRegistrationToken(context);
            } else {
                Log.e(TAG, "generateWalletIfIfNeeded: walletId is empty or faulty after generation");
                BRSharedPrefs.putWalletRewardId(context, "");
                BRReportsManager.reportBug(new IllegalArgumentException("walletId is empty or faulty after generation: " + walletId));
            }
        }

    }

    private static synchronized String generateWalletId(Context app, String address) {
        if (app == null) {
            Log.e(TAG, "generateWalletId: app is null");
            return null;
        }
        try {
            // Remove the first 2 characters
            String cleanAddress = address.substring(2, address.length());

            // Get the shortened address bytes
            byte[] addressBytes = cleanAddress.getBytes("UTF-8");

            // Run sha256 on the shortened address bytes
            byte[] sha256Address = CryptoHelper.sha256(addressBytes);
            if (Utils.isNullOrEmpty(sha256Address)) {
                BRReportsManager.reportBug(new IllegalAccessException("Failed to sha256"));
                return null;
            }

            // Get the first 10 bytes
            byte[] firstTenBytes = Arrays.copyOfRange(sha256Address, 0, NUMBER_OF_BYTES_FOR_SHA256_NEEDED);

            String base32String = new String(Base32.encode(firstTenBytes));
            base32String = base32String.toLowerCase();

            StringBuilder builder = new StringBuilder();

            Matcher matcher = Pattern.compile(".{1,4}").matcher(base32String);
            while (matcher.find()) {
                String piece = base32String.substring(matcher.start(), matcher.end());
                builder.append(piece + " ");
            }
            return builder.toString();

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();

        }
        return null;

    }

    @TargetApi(Build.VERSION_CODES.N)
    public String getCurrentLocale(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return ctx.getResources().getConfiguration().getLocales().get(0).getLanguage();
        } else {
            //noinspection deprecation
            return ctx.getResources().getConfiguration().locale.getLanguage();
        }
    }

    public static Map<String, String> getBreadHeaders() {
        return mHeaders;
    }

    public static Context getBreadContext() {
        Context app = currentActivity;
        if (app == null) {
            app = mContext;
        }
        return app;
    }

    public static void setBreadContext(Activity app) {
        currentActivity = app;
    }

    public static void addOnBackgroundedListener(ApplicationLifecycleObserver.ApplicationLifecycleListener listener) {
        mObserver.addApplicationLifecycleListener(listener);
    }

    public static void removeOnBackgroundListener(ApplicationLifecycleObserver.ApplicationLifecycleListener listener) {
        mObserver.removeApplicationLifecycleListener(listener);
    }

    public static boolean isAppInBackground() {
        return mLastApplicationEvent != null && mLastApplicationEvent.name().equalsIgnoreCase(Lifecycle.Event.ON_STOP.toString());
    }

    @Override
    public void onLifeCycle(Lifecycle.Event event) {
        mLastApplicationEvent = event;
        switch (event) {
            case ON_STOP:
                Log.d(TAG, "onLifeCycle: STOP");
                mBackgroundedTime = System.currentTimeMillis();
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(mDisconnectWalletsRunnable);
                BRExecutor.getInstance().forLightWeightBackgroundTasks().remove(mConnectWalletsRunnable);
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        HTTPServer.stopServer();
                    }
                });
                break;

            case ON_START:
                Log.d(TAG, "onLifeCycle: START");
                mBackgroundedTime = 0;
                BRExecutor.getInstance().forLightWeightBackgroundTasks().remove(mDisconnectWalletsRunnable);
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(mConnectWalletsRunnable);
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (!HTTPServer.isStarted()) {
                            HTTPServer.startServer();
                        }
                    }
                });
                break;

        }
    }

    public static void lockIfNeeded(Activity activity) {
        //lock wallet if 3 minutes passed
        if (mBackgroundedTime != 0
                && (System.currentTimeMillis() - mBackgroundedTime >= LOCK_TIMEOUT)
                && !(activity instanceof DisabledActivity)) {
            if (!BRKeyStore.getPinCode(activity).isEmpty()) {
                UiUtils.startBreadActivity(activity, true);
            }
        }

    }


}
