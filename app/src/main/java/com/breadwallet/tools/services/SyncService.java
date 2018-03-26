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

import com.breadwallet.tools.manager.BRSharedPrefs;
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
//    public static final String ACTION_STOP_SYNC_POLLING = "com.breadwallet.tools.services.ACTION_STOP_SYNC_POLLING";
    public static final String ACTION_SYNC_PROGRESS_UPDATE = "com.breadwallet.tools.services.ACTION_SYNC_PROGRESS_UPDATE";
    public static final String EXTRA_WALLET_ISO = "com.breadwallet.tools.services.EXTRA_WALLET_ISO";
    public static final String EXTRA_PROGRESS = "com.breadwallet.tools.services.EXTRA_PROGRESS";

    private static final int DELAY_MILLIS = 500;

    public SyncService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        switch (intent.getAction()) {
            case ACTION_START_SYNC_PROGRESS_POLLING:
                String walletISO = intent.getStringExtra(EXTRA_WALLET_ISO);
                if (walletISO != null) {
                    startSyncPolling(SyncService.this.getApplicationContext(), walletISO);
                }
                break;
//            case ACTION_STOP_SYNC_POLLING:
//                break;
            default:
                Log.i(TAG, "Intent not recognized.");
        }
    }

    private static Intent createIntent(Context context, String action, String walletISO) {
        Intent intent = new Intent(context, SyncService.class);
        intent.setAction(action)
                .putExtra(EXTRA_WALLET_ISO, walletISO);
        return intent;
    }

    private static Intent createIntent(Context context, String action, String walletISO, double progress){
        return createIntent(context, action, walletISO)
                .putExtra(EXTRA_PROGRESS, progress);
    }

    public static void startService(Context context, String action, String walletISO) {
        context.startService(createIntent(context, action, walletISO));
    }

    private void startSyncPolling(Context context, String walletISO) {
        final BaseWalletManager walletManager = WalletsMaster.getInstance(context).getWalletByIso(context, walletISO);
        final double progress = walletManager.getPeerManager()
                .getSyncProgress(BRSharedPrefs.getStartHeight(context,
                        BRSharedPrefs.getCurrentWalletIso(context)));
        Log.e(TAG, "startSyncPolling: Progress:" + progress + " Wallet: " + walletISO);

        if (progress > 0 && progress < 1) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getService(context,
                    0, /* request code not used */
                    createIntent(context, ACTION_START_SYNC_PROGRESS_POLLING, walletISO),
                    PendingIntent.FLAG_UPDATE_CURRENT);

            alarmManager.set(AlarmManager.RTC,
                    System.currentTimeMillis() + DELAY_MILLIS,
                    pendingIntent);
        }

        broadcastSyncProgressUpdate(context, walletManager.getIso(context), progress);
    }

//        private void stopSyncProgressPolling() {
//        sAlarmManager.cancel(sPendingIntent);
//    }

    private static void broadcastSyncProgressUpdate(Context context, String walletIso, double progress) {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
        localBroadcastManager.sendBroadcast(createIntent(context,
                ACTION_SYNC_PROGRESS_UPDATE, walletIso, progress));
    }

    public static void registerSyncNotificationBroadcastReceiver(Context context, BroadcastReceiver broadcastReceiver) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_SYNC_PROGRESS_UPDATE);

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
        localBroadcastManager.registerReceiver(broadcastReceiver, intentFilter);
    }

    public static void unregisterSyncNotificationBroadcastReceiver(Context context, BroadcastReceiver broadcastReceiver) {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
        localBroadcastManager.unregisterReceiver(broadcastReceiver);
    }
}
