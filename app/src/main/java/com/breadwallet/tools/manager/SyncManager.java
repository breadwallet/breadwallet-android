package com.breadwallet.tools.manager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.ProgressBar;

import com.breadwallet.tools.listeners.SyncReceiver;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

import java.util.concurrent.TimeUnit;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 9/19/17.
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
public class SyncManager {

    private static final String TAG = SyncManager.class.getName();
    private static SyncManager instance;
    private static final long SYNC_PERIOD = TimeUnit.HOURS.toMillis(24);

    //todo refactor this task to have a list of unique tasks (multiple UI syncing)
    private static SyncProgressTask syncTask;
    public boolean running;
    private String mCurrentThreadName;
    private BaseWalletManager mWallet;
    //    private final Object lock = new Object();
    private ProgressBar mProgressBar;

    public static SyncManager getInstance() {
        if (instance == null) instance = new SyncManager();
        return instance;
    }

    private SyncManager() {
    }

    private void createAlarm(Context app, long time) {
        AlarmManager alarmManager = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(app, SyncReceiver.class);
        intent.setAction(SyncReceiver.SYNC_RECEIVER);//my custom string action name
        PendingIntent pendingIntent = PendingIntent.getService(app, 1001, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, time, time + TimeUnit.MINUTES.toMillis(1), pendingIntent);//first start will start asap
    }

    public synchronized void updateAlarms(Context app) {
        createAlarm(app, System.currentTimeMillis() + SYNC_PERIOD);
    }

    public synchronized void stopSyncing() {
        if (syncTask != null) {
            syncTask.interrupt();
        }
    }

    public synchronized void startSyncing(final Context app, BaseWalletManager walletManager, OnProgressUpdate listener) {
        mWallet = walletManager;

        if (syncTask != null) syncTask.interrupt();

        syncTask = new SyncProgressTask(app, walletManager, listener);
        syncTask.start();

    }

    class SyncProgressTask extends Thread {

        private BaseWalletManager mCurrentWallet;
        private OnProgressUpdate mListener;
        private static final int DELAY_MILLIS = 500;
        private Context mApp;

        public SyncProgressTask(Context app, BaseWalletManager currentWallet, OnProgressUpdate listener) {
            mCurrentWallet = currentWallet;
            mListener = listener;
            mApp = app;
        }

        @Override
        public void run() {
//            Log.e(TAG, "SyncProgressTask: started: " + Thread.currentThread());
            try {
                mCurrentThreadName = getName();
                while (!isInterrupted() && mCurrentThreadName.equalsIgnoreCase(getName())) {
                    final double syncProgress = mCurrentWallet.getSyncProgress(BRSharedPrefs.getStartHeight(mApp, mCurrentWallet.getIso(mApp)));
                    if (!mCurrentThreadName.equalsIgnoreCase(getName()))
                        Log.e(TAG, "run: WARNING: " + getName());
                    BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            if (mListener != null)
                                if (!mListener.onProgressUpdated(syncProgress)) interrupt();
                        }
                    });
                    Thread.sleep(DELAY_MILLIS);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "run: " + getName(), e);
                final double syncProgress = mCurrentWallet.getSyncProgress(BRSharedPrefs.getStartHeight(mApp, mCurrentWallet.getIso(mApp)));
                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (mListener != null)
                            if (!mListener.onProgressUpdated(syncProgress)) interrupt();
                    }
                });
            }

        }
    }

    public interface OnProgressUpdate {
        //get the progressUpdate and return true if you want to continue or false if it's done syncing
        boolean onProgressUpdated(double progress);
    }
}


