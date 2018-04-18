package com.breadwallet;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Point;
import android.hardware.fingerprint.FingerprintManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.breadwallet.presenter.activities.HomeActivity;
import com.breadwallet.presenter.activities.WalletActivity;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.tools.crypto.Base32;
import com.breadwallet.tools.crypto.CryptoHelper;
import com.breadwallet.tools.listeners.SyncReceiver;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.google.firebase.crash.FirebaseCrash;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.platform.APIClient.BREAD_POINT;
import static org.apache.commons.compress.utils.CharsetNames.ISO_8859_1;

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
    private static Timer isBackgroundChecker;
    public static AtomicInteger activityCounter = new AtomicInteger();
    public static long backgroundedTime;
    public static boolean appInBackground;
    private static Context mContext;

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
        if (Utils.isEmulatorOrDebug(this)) {
//            BRKeyStore.putFailCount(0, this);
            HOST = "stage2.breadwallet.com";
            FirebaseCrash.setCrashCollectionEnabled(false);
//            FirebaseCrash.report(new RuntimeException("test with new json file"));
        }
        mContext = this;

        if (!Utils.isEmulatorOrDebug(this) && IS_ALPHA)
            throw new RuntimeException("can't be alpha for release");

        boolean isTestVersion = BREAD_POINT.contains("staging") || BREAD_POINT.contains("stage");
        boolean isTestNet = BuildConfig.BITCOIN_TESTNET;
        String lang = getCurrentLocale(this);

        mHeaders.put("X-Is-Internal", IS_ALPHA ? "true" : "false");
        mHeaders.put("X-Testflight", isTestVersion ? "true" : "false");
        mHeaders.put("X-Bitcoin-Testnet", isTestNet ? "true" : "false");
        mHeaders.put("Accept-Language", lang);


        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int DISPLAY_WIDTH_PX = size.x;
        DISPLAY_HEIGHT_PX = size.y;
        mFingerprintManager = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);

        registerReceiver(InternetManager.getInstance(), new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

//        addOnBackgroundedListener(new OnAppBackgrounded() {
//            @Override
//            public void onBackgrounded() {
//
//            }
//        });

    }

    public static synchronized String generateWalletId() {

        // First, get the ETH wallet address
        BaseWalletManager ethWallet = WalletsMaster.getInstance(mContext).getWalletByIso(mContext, "ETH");
        String ethAddress = ethWallet.getReceiveAddress(mContext).stringify();

        try {
            byte[] ptext = ethAddress.getBytes();

            // Encode the address to UTF-8
            String ethAddressEncoded = URLEncoder.encode(ethAddress, "UTF-8");

            // Remove the first 2 characters
            ethAddressEncoded = ethAddressEncoded.substring(2, ethAddressEncoded.length());

            // Get the shortened address bytes
            byte[] addressBytes = ethAddressEncoded.getBytes();

            // Run sha256 on the shortened address bytes
            byte[] sha256Address = CryptoHelper.sha256(addressBytes);


            // Get the first 10 bytes
            byte[] firstTenBytes = Arrays.copyOfRange(sha256Address, 0, 10);

            Base32 base32 = new Base32();
            String base32String = base32.encodeOriginal(firstTenBytes);
            base32String = base32String.toLowerCase();

            StringBuilder builder = new StringBuilder();

            Matcher matcher = Pattern.compile(".{1,4}").matcher(base32String);
            List<String> result = new ArrayList<>();
            while (matcher.find()) {
                String piece = base32String.substring(matcher.start(), matcher.end());
                result.add(piece);
                builder.append(piece + " ");
            }

            // Add the wallet ID to the request headers if it's not null or empty
            if (builder.toString() != null && !builder.toString().isEmpty()) {
                mHeaders.put("X-Wallet-ID", builder.toString());
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
        if (app == null) app = SyncReceiver.app;
        if (app == null) app = mContext;
        return app;
    }

    public static void setBreadContext(Activity app) {
        currentActivity = app;
    }

    public static synchronized void fireListeners() {
        if (listeners == null) return;
        List<OnAppBackgrounded> copy = listeners;
        for (OnAppBackgrounded lis : copy) if (lis != null) lis.onBackgrounded();
    }

    public static void addOnBackgroundedListener(OnAppBackgrounded listener) {
        if (listeners == null) listeners = new ArrayList<>();
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public static boolean isAppInBackground(final Context context) {
        return context == null || activityCounter.get() <= 0;
    }

    //call onStop on every activity so
    public static void onStop(final BRActivity app) {

        if (app instanceof WalletActivity) {
            BRSharedPrefs.putAppBackgroundedFromHome(mContext, false);

        } else if (app instanceof HomeActivity) {
            BRSharedPrefs.putAppBackgroundedFromHome(mContext, true);

        }
        if (isBackgroundChecker != null) isBackgroundChecker.cancel();
        isBackgroundChecker = new Timer();
        TimerTask backgroundCheck = new TimerTask() {
            @Override
            public void run() {
                if (isAppInBackground(app)) {
                    backgroundedTime = System.currentTimeMillis();
                    Log.e(TAG, "App went in background!");
                    // APP in background, do something
                    fireListeners();
                    isBackgroundChecker.cancel();
                }
                // APP in foreground, do something else
            }
        };

        isBackgroundChecker.schedule(backgroundCheck, 500, 500);
    }

    public interface OnAppBackgrounded {
        void onBackgrounded();
    }
}
