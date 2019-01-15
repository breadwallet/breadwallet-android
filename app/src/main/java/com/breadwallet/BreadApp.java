package com.breadwallet;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.breadwallet.app.ApplicationLifecycleObserver;
import com.breadwallet.presenter.activities.DisabledActivity;
import com.breadwallet.protocols.messageexchange.InboxPollingAppLifecycleObserver;
import com.breadwallet.protocols.messageexchange.InboxPollingWorker;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.crypto.Base32;
import com.breadwallet.tools.crypto.CryptoHelper;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.services.BRDFirebaseMessagingService;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.EventUtils;
import com.breadwallet.tools.util.TokenUtil;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.ethereum.WalletEthManager;
import com.crashlytics.android.Crashlytics;
import com.platform.APIClient;
import com.platform.HTTPServer;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    // The server(s) on which the API is hosted
    private static final String HOST = BuildConfig.DEBUG ? "stage2.breadwallet.com" : "api.breadwallet.com";
    private static final int LOCK_TIMEOUT = 180000; // 3 minutes in milliseconds
    private static final String WALLET_ID_PATTERN = "^[a-z0-9 ]*$"; // The wallet ID is in the form "xxxx xxxx xxxx xxxx" where x is a lowercase letter or a number.
    private static final String WALLET_ID_SEPARATOR = " ";
    private static final int NUMBER_OF_BYTES_FOR_SHA256_NEEDED = 10;

    private static BreadApp mInstance;
    public static int mDisplayHeightPx;
    public static int mDisplayWidthPx;
    private static long mBackgroundedTime;
    private static Activity mCurrentActivity;

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

    @Override
    public void onCreate() {
        super.onCreate();

        mInstance = this;

        final Fabric fabric = new Fabric.Builder(this)
                .kits(new Crashlytics.Builder().disabled(BuildConfig.DEBUG).build())
                .debuggable(BuildConfig.DEBUG)// Enables Crashlytics debugger
                .build();
        Fabric.with(fabric);

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mDisplayWidthPx = size.x;
        mDisplayHeightPx = size.y;

        registerReceiver(InternetManager.getInstance(), new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        initialize(true);

        // Start our local server as soon as the application instance is created, since we need to
        // display support WebViews during onboarding.
        HTTPServer.getInstance().startServer();

    }

    /**
     * Initializes the application.  Only put things in here that need to happen after the user has created or
     * recovered the BRD wallet.
     *
     * @param isApplicationOnCreate True if initialize is called from application onCreate.
     */
    public static void initialize(boolean isApplicationOnCreate) {
        if (bRDWalletExists()) {
            // Initialize the wallet id (also called rewards id).
            initializeWalletId();

            // Initialize application lifecycle observer and register this application for events.
            ProcessLifecycleOwner.get().getLifecycle().addObserver(new ApplicationLifecycleObserver());
            ApplicationLifecycleObserver.addApplicationLifecycleListener(mInstance);

            // Initialize message exchange inbox polling.
            ApplicationLifecycleObserver.addApplicationLifecycleListener(new InboxPollingAppLifecycleObserver());
            if (!isApplicationOnCreate) {
                InboxPollingWorker.initialize();
            }

            // Initialize the Firebase Messaging Service.
            BRDFirebaseMessagingService.initialize(mInstance);

            // Initialize TokenUtil to load our tokens.json file from res/raw
            TokenUtil.initialize(mInstance);
        }
    }

    /**
     * Returns whether the BRD wallet exists.  i.e. has the BRD wallet been created or recovered.
     *
     * @return True if the BRD wallet exists; false, otherwise.
     */
    private static boolean bRDWalletExists() {
        return BRKeyStore.getMasterPublicKey(mInstance) != null;
    }

    /**
     * Initialize the wallet id (rewards id), and save it in the SharedPreferences.
     */
    private static void initializeWalletId() {
        String walletId = generateWalletId();
        if (!Utils.isNullOrEmpty(walletId) && walletId.matches(WALLET_ID_PATTERN)) {
            BRSharedPrefs.putWalletRewardId(mInstance, walletId);
        } else {
            Log.e(TAG, "initializeWalletId: walletId is empty or faulty after generation");
            BRSharedPrefs.putWalletRewardId(mInstance, "");
            BRReportsManager.reportBug(new IllegalArgumentException("walletId is empty or faulty after generation: " + walletId));
        }
    }

    /**
     * Generates the wallet id (rewards id) based on the Ethereum address. The format of the id is
     * "xxxx xxxx xxxx xxxx", where x is a lowercase letter or a number.
     *
     * @return The wallet id.
     */
    private static synchronized String generateWalletId() {
        try {
            // Retrieve the ETH address since the wallet id is based on this.
            String address = WalletEthManager.getInstance(mInstance).getAddress(mInstance);

            // Remove the first 2 characters i.e. 0x
            String rawAddress = address.substring(2, address.length());

            // Get the address bytes.
            byte[] addressBytes = rawAddress.getBytes("UTF-8");

            // Run SHA256 on the address bytes.
            byte[] sha256Address = CryptoHelper.sha256(addressBytes);
            if (Utils.isNullOrEmpty(sha256Address)) {
                BRReportsManager.reportBug(new IllegalAccessException("Failed to generate SHA256 hash."));
                return null;
            }

            // Get the first 10 bytes of the SHA256 hash.
            byte[] firstTenBytes = Arrays.copyOfRange(sha256Address, 0, NUMBER_OF_BYTES_FOR_SHA256_NEEDED);

            // Convert the first 10 bytes to a lower case string.
            String base32String = new String(Base32.encode(firstTenBytes));
            base32String = base32String.toLowerCase();

            // Insert a space every 4 chars to match the specified format.
            StringBuilder builder = new StringBuilder();
            Matcher matcher = Pattern.compile(".{1,4}").matcher(base32String);
            String separator = "";
            while (matcher.find()) {
                String piece = base32String.substring(matcher.start(), matcher.end());
                builder.append(separator + piece);
                separator = WALLET_ID_SEPARATOR;
            }
            return builder.toString();

        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unable to get address bytes.", e);
            return null;
        }
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
        if (!((ActivityManager) mInstance.getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData()) {
            throw new IllegalStateException(TAG + ": Failed to clear user application data.");
        }
    }

    /**
     * Returns true if the application is in the background; false, otherwise.
     *
     * @return True if the application is in the background; false, otherwise.
     */
    public static boolean isInBackground() {
        return mBackgroundedTime > 0;
    }

    // TODO: Refactor so this does not store the current activity like this.
    public static Context getBreadContext() {
        Context app = mCurrentActivity;
        if (app == null) {
            app = mInstance;
        }
        return app;
    }

    // TODO: Refactor so this does not store the current activity like this.
    public static void setBreadContext(Activity app) {
        mCurrentActivity = app;
    }

    @Override
    public void onLifeCycle(Lifecycle.Event event) {
        switch (event) {
            case ON_START:
                Log.d(TAG, "onLifeCycle: START");
                mBackgroundedTime = 0;
                BRExecutor.getInstance().forLightWeightBackgroundTasks().remove(mDisconnectWalletsRunnable);
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(mConnectWalletsRunnable);

                HTTPServer.getInstance().startServer();

                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        TokenUtil.fetchTokensFromServer(mInstance);
                    }
                });
                APIClient.getInstance(this).updatePlatform(this);
                break;
            case ON_STOP:
                Log.d(TAG, "onLifeCycle: STOP");
                mBackgroundedTime = System.currentTimeMillis();
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(mDisconnectWalletsRunnable);
                BRExecutor.getInstance().forLightWeightBackgroundTasks().remove(mConnectWalletsRunnable);
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        EventUtils.saveEvents(BreadApp.this);
                        EventUtils.pushToServer(BreadApp.this);
                    }
                });
                HTTPServer.getInstance().stopServer();

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

    /**
     * @return host or debug host if build is DEBUG
     */
    public static String getHost() {
        if (BuildConfig.DEBUG) {
            String host = BRSharedPrefs.getDebugHost(mInstance);
            if (!Utils.isNullOrEmpty(host)) {
                return host;
            }
        }
        return HOST;
    }

    /**
     * Sets the debug host into the shared preferences, only do that if the build is DEBUG.
     *
     * @param host
     */
    public static void setDebugHost(String host) {
        if (BuildConfig.DEBUG) {
            BRSharedPrefs.putDebugHost(mCurrentActivity, host);
        }
    }

}
