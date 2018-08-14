package com.breadwallet.tools.services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

/**
 * BreadWallet
 * <p/>
 * Created by Shivangi Gandhi on <shivangi.gandhi@breadwallet.com> 3/24/18.
 * Copyright (c) 2018 breadwallet LLC
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
public class SyncService extends IntentService {
    private static final String TAG = SyncService.class.getSimpleName();

    public static final String ACTION_START_SYNC_PROGRESS_POLLING = "com.breadwallet.tools.services.ACTION_START_SYNC_PROGRESS_POLLING";
    public static final String ACTION_SYNC_PROGRESS_UPDATE = "com.breadwallet.tools.services.ACTION_SYNC_PROGRESS_UPDATE";
    public static final String EXTRA_WALLET_ISO = "com.breadwallet.tools.services.EXTRA_WALLET_ISO";
    public static final String EXTRA_PROGRESS = "com.breadwallet.tools.services.EXTRA_PROGRESS";

    private static final int POLLING_INTERVAL = 500; // in milliseconds

    /**
     * Progress is identified as a double value between 0 and 1.
     */
    public static final int PROGRESS_NOT_DEFINED = -1;
    public static final int PROGRESS_START = 0;
    public static final int PROGRESS_FINISH = 1;

    private static final String PACKAGE_NAME = BreadApp.getBreadContext() == null ? null
            : BreadApp.getBreadContext().getApplicationContext().getPackageName();

    static {
        try {
            System.loadLibrary(BRConstants.NATIVE_LIB_NAME);
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
            Log.d(TAG, "Native code library failed to load.\\n\" + " + e);
            Log.d(TAG, "Installer Package Name -> " + (PACKAGE_NAME == null ? "null"
                    : BreadApp.getBreadContext().getPackageManager().getInstallerPackageName(PACKAGE_NAME)));
        }
    }

    /**
     * The {@link SyncService} is responsible for polling the native layer for wallet sync updates and
     * posting updates to registered listeners.  The actual data sync is done natively and not in Java.
     */
    public SyncService() {
        super(TAG);
    }

    /**
     * Handles intents passed to the {@link SyncService} by creating a new worker thread to complete the work required.
     *
     * @param intent The intent specifying the work that needs to be completed.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            switch (intent.getAction()) {
                case ACTION_START_SYNC_PROGRESS_POLLING:
                    String walletIso = intent.getStringExtra(EXTRA_WALLET_ISO);
                    if (walletIso != null) {
                        startSyncPolling(SyncService.this.getApplicationContext(), walletIso);
                    }
                    break;
                default:
                    Log.i(TAG, "Intent not recognized.");
            }
        }
    }

    /**
     * Creates an intent with the specified parameters.
     *
     * @param context   The context in which we are operating.
     * @param action    The action of the intent.
     * @param walletIso The wallet ISO used to identify which wallet is going to be acted upon.
     * @return An intent with the specified parameters.
     */
    private static Intent createIntent(Context context, String action, String walletIso) {
        Intent intent = new Intent(context, SyncService.class);
        intent.setAction(action)
                .putExtra(EXTRA_WALLET_ISO, walletIso);
        return intent;
    }

    /**
     * Creates an intent with the specified parameters.
     *
     * @param context   The context in which we are operating.
     * @param action    The action of the intent.
     * @param walletIso The wallet ISO used to identify which wallet is going to be acted upon.
     * @param progress  The current sync progress of the specified wallet.
     * @return An intent with the specified parameters.
     */
    private static Intent createIntent(Context context, String action, String walletIso, double progress) {
        return createIntent(context, action, walletIso)
                .putExtra(EXTRA_PROGRESS, progress);
    }

    /**
     * Starts the sync polling service with the specified parameters.
     *
     * @param context   The context in which we are operating.
     * @param action    The action of the intent.
     * @param walletIso The wallet ISO used to identify which wallet is going to be acted upon.
     */
    public static void startService(Context context, String action, String walletIso) {
        context.startService(createIntent(context, action, walletIso));
    }

    /**
     * Starts polling the native layer for sync progress on the specified wallet.
     *
     * @param context   The context in which we are operating.
     * @param walletIso The wallet ISO used to identify which wallet needs to be polled for sync progress.
     */
    private void startSyncPolling(Context context, String walletIso) {
        final BaseWalletManager walletManager = WalletsMaster.getInstance(context).getWalletByIso(context, walletIso);
        final double progress = walletManager.getSyncProgress(BRSharedPrefs.getStartHeight(context,
                BRSharedPrefs.getCurrentWalletIso(context)));
        Log.e(TAG, "startSyncPolling: Progress:" + progress + " Wallet: " + walletIso);

        if (progress > PROGRESS_START && progress < PROGRESS_FINISH) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getService(context,
                    0, /* request code not used */
                    createIntent(context, ACTION_START_SYNC_PROGRESS_POLLING, walletIso),
                    PendingIntent.FLAG_UPDATE_CURRENT);

            alarmManager.set(AlarmManager.RTC,
                    System.currentTimeMillis() + POLLING_INTERVAL,
                    pendingIntent);
        }

        broadcastSyncProgressUpdate(context, walletManager.getIso(), progress);
    }

    /**
     * Broadcasts the sync progress update to registered listeners.
     *
     * @param context   The context in which we are operating.
     * @param walletIso The wallet ISO used to identify which wallet is going to be acted upon.
     * @param progress  The current sync progress of the specified wallet.
     */
    private static void broadcastSyncProgressUpdate(Context context, String walletIso, double progress) {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
        localBroadcastManager.sendBroadcast(createIntent(context,
                ACTION_SYNC_PROGRESS_UPDATE, walletIso, progress));
    }

    /**
     * Registers the specified listener for sync progress updates.
     *
     * @param context           The context in which we are operating.
     * @param broadcastReceiver The specified listener.
     */
    public static void registerSyncNotificationBroadcastReceiver(Context context, BroadcastReceiver broadcastReceiver) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_SYNC_PROGRESS_UPDATE);

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
        localBroadcastManager.registerReceiver(broadcastReceiver, intentFilter);
    }

    /**
     * Unregisters the specified listener from receiving sync progress updates.
     *
     * @param context           The context in which we are operating.
     * @param broadcastReceiver The specified listener.
     */
    public static void unregisterSyncNotificationBroadcastReceiver(Context context, BroadcastReceiver broadcastReceiver) {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
        localBroadcastManager.unregisterReceiver(broadcastReceiver);
    }
}
