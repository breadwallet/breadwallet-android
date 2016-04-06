package com.breadwallet.wallet;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;

import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.BlockEntity;
import com.breadwallet.presenter.entities.PeerEntity;
import com.breadwallet.presenter.fragments.FragmentSettingsAll;
import com.breadwallet.tools.CurrencyManager;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.sqlite.SQLiteManager;

import java.text.DecimalFormat;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 12/10/15.
 * Copyright (c) 2015 Mihail Gutan <mihail@breadwallet.com>
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
public class BRPeerManager {
    public static final String TAG = BRPeerManager.class.getName();
    private static BRPeerManager instance;
    private static SyncProgressTask syncTask;
    private static Activity ctx;

    private BRPeerManager() {
        syncTask = new SyncProgressTask();
    }

    public static synchronized BRPeerManager getInstance(Activity context) {
        ctx = context;
        if (instance == null) {
            instance = new BRPeerManager();
        }
        return instance;
    }

    public native void createAndConnect(long earliestKeyTime, int blockCount, int peerCount);

    public native void connect();

    public native void putPeer(byte[] peerAddress, byte[] peerPort, byte[] peerTimeStamp);

    public native void createPeerArrayWithCount(int count);

    public native void putBlock(byte[] block, int blockHeight);

    public native void createBlockArrayWithCount(int count);

    public native static double syncProgress();

    public native static int getCurrentBlockHeight();

    public native static int getEstimatedBlockHeight();

    public native boolean isCreated();

    /**
     * void BRPeerManagerSetCallbacks(BRPeerManager *manager, void *info,
     * void (*syncStarted)(void *info),
     * void (*syncSucceeded)(void *info),
     * void (*syncFailed)(void *info, BRPeerManagerError error),
     * void (*txStatusUpdate)(void *info),
     * void (*saveBlocks)(void *info, const BRMerkleBlock blocks[], size_t count),
     * void (*savePeers)(void *info, const BRPeer peers[], size_t count),
     * int (*networkIsReachable)(void *info))
     */

    public static synchronized void syncStarted() {
        Log.e(TAG, "syncStarted");
        startSyncingProgressThread();
    }

    public static synchronized void syncSucceeded() {
        Log.e(TAG, "syncSucceeded");
        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null)
//            if (KeyStoreManager.getWalletCreationTime(ctx) == 0) {
//                Log.e(TAG, "getWalletCreationTime() is 0 ! setting the new walletCreationTime in the keystore!");
//                KeyStoreManager.putWalletCreationTime((int) (System.currentTimeMillis() / 1000), ctx);
//            }
            stopSyncingProgressThread();
    }

    public static synchronized void syncFailed() {
        stopSyncingProgressThread();
    }

    public static synchronized void txStatusUpdate() {
        Log.e(TAG, "txStatusUpdate");
        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null)
            FragmentSettingsAll.refreshTransactions(ctx);
//        if (syncProgress() >= 1) {
//            if (ctx == null) ctx = MainActivity.app;
//            if (ctx != null) MiddleViewAdapter.setSyncing((Activity) ctx, false);
//        }

    }

    public static synchronized void txRejected(int rescanRecommended) {
        Log.e(TAG, "txStatusUpdate");
    }

    public static synchronized void saveBlocks(final BlockEntity[] blockEntities) {
        Log.e(TAG, "saveBlocks: " + blockEntities.length);

        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < blockEntities.length; i++) {
                        SQLiteManager.getInstance(ctx).insertMerkleBlock(blockEntities[i].getBlockBytes(), blockEntities[i].getBlockHeight());
                    }
                }
            }).start();
        }

    }

    public static synchronized void savePeers(final PeerEntity[] peerEntities) {
        Log.e(TAG, "savePeers");
        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < peerEntities.length; i++) {
                        SQLiteManager.getInstance(ctx).insertPeer(peerEntities[i].getPeerAddress(), peerEntities[i].getPeerPort(), peerEntities[i].getPeerTimeStamp());
                    }
                }
            }).start();
        }

    }

    public static synchronized boolean networkIsReachable() {
        Log.e(TAG, "networkIsReachable");
        return ctx != null && CurrencyManager.getInstance(ctx).isNetworkAvailable(ctx);
    }

    public static synchronized void deleteBlocks() {
        Log.e(TAG, "deleteBlocks");
        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SQLiteManager.getInstance(ctx).deleteBlocks();
                }
            }).start();

        }
    }

    public static synchronized void deletePeers() {
        Log.e(TAG, "deletePeers");
        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SQLiteManager.getInstance(ctx).deletePeers();
                }
            }).start();

        }
    }

//    public static void saveLastBlockHeight() {
//        Log.e(TAG, "saveLastBlockHeight");
//        MainActivity app = MainActivity.app;
//        if (app != null) {
//            int blockHeight = getCurrentBlockHeight();
//            Log.e(TAG, "saveLastBlockHeight: blockHeight: " + blockHeight);
//            if (blockHeight <= 0) return;
//            SharedPreferences prefs = app.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
//            int blockHeightFromPrefs = prefs.getInt(BRConstants.BLOCK_HEIGHT, 0);
//            Log.e(TAG, "saveLastBlockHeight: blockHeightFromPrefs: " + blockHeightFromPrefs);
//            if (blockHeight > blockHeightFromPrefs && blockHeight < Integer.MAX_VALUE) {
//                SharedPreferences prefs2 = app.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
//                SharedPreferences.Editor editor = prefs2.edit();
//                editor.putInt(BRConstants.BLOCK_HEIGHT, blockHeight);
//                editor.apply();
//                if (ctx == null) ctx = MainActivity.app;
//                ((Activity) ctx).runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        FragmentSettingsAll.refreshTransactions(ctx);
//                    }
//                });
//
//            }
//        }
//    }
//
//    public static int getLastBlockFromPrefs() {
//        Log.e(TAG, "getLastBlockFromPrefs");
//        MainActivity app = MainActivity.app;
//        int blockHeightFromPrefs = 0;
//        int blockHeight = getCurrentBlockHeight();
//        if (app != null) {
//            SharedPreferences prefs = app.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
//            blockHeightFromPrefs = prefs.getInt(BRConstants.BLOCK_HEIGHT, 0);
//
//        }
//        Log.e(TAG, "getLastBlockFromPrefs: blockHeight: " + blockHeight);
//        return blockHeight < Integer.MAX_VALUE && blockHeight > blockHeightFromPrefs ? blockHeight : blockHeightFromPrefs;
//    }

    public static void startSyncingProgressThread() {
        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null) {
            MiddleViewAdapter.setSyncing(ctx, true);
            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((MainActivity) ctx).showHideSyncProgressViews(true);
                }
            });
        }

        try {
            if (syncTask != null) {
                syncTask.interrupt();
            }
            syncTask = new SyncProgressTask();
            syncTask.start();
        } catch (IllegalThreadStateException ex) {
            ex.printStackTrace();
        }
    }

    public static void stopSyncingProgressThread() {
        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null) {
            MiddleViewAdapter.setSyncing(ctx, false);
            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((MainActivity) ctx).showHideSyncProgressViews(false);
                }
            });
        }

        try {
            if (syncTask != null) {
                syncTask.setRunning(false);
                syncTask.interrupt();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static class SyncProgressTask extends Thread {

        public boolean running = true;
        public double progressStatus = 0;

        public SyncProgressTask() {
            progressStatus = 0;
            running = true;
        }

        public void setRunning(boolean b) {
            running = b;
        }

        @Override
        public void run() {
            final MainActivity app = MainActivity.app;
            progressStatus = 0;
            if (app != null) {
                app.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressStatus = syncProgress();
                        app.showHideSyncProgressViews(true);
                        app.syncProgressBar.setProgress((int) (progressStatus * 100));
                        app.syncProgressText.setText(String.format("%s%%", new DecimalFormat("#.##").format(progressStatus * 100)));
                    }
                });

                while (running) {
                    app.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressStatus = syncProgress();
                            app.syncProgressBar.setProgress((int) (progressStatus * 100));
                            app.syncProgressText.setText(String.format("%s%%", new DecimalFormat("#.##").format(progressStatus * 100)));

                        }
                    });
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
//                    if (progressStatus >= 1) running = false;
                }

                app.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressStatus = 0;
                        app.showHideSyncProgressViews(false);
                        MiddleViewAdapter.setSyncing(app, false);
                    }
                });

            }

        }
    }
}