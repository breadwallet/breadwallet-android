package com.breadwallet.wallet;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.presenter.activities.BreadActivity;
import com.breadwallet.presenter.entities.BlockEntity;
import com.breadwallet.presenter.entities.PeerEntity;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.PromptManager;
import com.breadwallet.tools.manager.TxManager;
import com.breadwallet.tools.sqlite.MerkleBlockDataSource;
import com.breadwallet.tools.sqlite.PeerDataSource;
import com.breadwallet.tools.util.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 12/10/15.
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

public class BRPeerManager {
    public static final String TAG = BRPeerManager.class.getName();
    private static BRPeerManager instance;
    private static SyncProgressTask syncTask;
    private static List<OnTxStatusUpdate> statusUpdateListeners;
    private static OnSyncSucceeded onSyncFinished;
    public boolean running;
    private final Object lock = new Object();

    private BRPeerManager() {
        statusUpdateListeners = new ArrayList<>();
    }

    public static BRPeerManager getInstance() {
        if (instance == null) {
            instance = new BRPeerManager();
        }
        return instance;
    }

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

    public static void syncStarted() {
        Log.d(TAG, "syncStarted: " + Thread.currentThread().getName());
//        BRPeerManager.getInstance().refreshConnection();
        Context ctx = BreadApp.getBreadContext();
        int startHeight = BRSharedPrefs.getStartHeight(ctx);
        int lastHeight = BRSharedPrefs.getLastBlockHeight(ctx);
        if (startHeight > lastHeight) BRSharedPrefs.putStartHeight(ctx, lastHeight);
        getInstance().startSyncingProgressThread();
    }

    public static void syncSucceeded() {
        Log.d(TAG, "syncSucceeded");
        final Activity app = BreadApp.getBreadContext();
        if (app == null) return;
        BRSharedPrefs.putAllowSpend(app, true);
        getInstance().stopSyncingProgressThread();
        new Thread(new Runnable() {
            @Override
            public void run() {
                BRSharedPrefs.putStartHeight(app, getCurrentBlockHeight());
            }
        }).start();
        if (onSyncFinished != null) onSyncFinished.onFinished();

    }

    public static void syncFailed() {
        Log.d(TAG, "syncFailed");
        getInstance().stopSyncingProgressThread();
        Activity ctx = BreadApp.getBreadContext();
        if (ctx == null) return;
        Log.e(TAG, "Network Not Available, showing not connected bar  ");

        BRPeerManager.getInstance().stopSyncingProgressThread();
        if (onSyncFinished != null) onSyncFinished.onFinished();
    }

    public static void txStatusUpdate() {
        Log.d(TAG, "txStatusUpdate");

        for (OnTxStatusUpdate listener : statusUpdateListeners) {
            if (listener != null) listener.onStatusUpdate();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                updateLastBlockHeight(getCurrentBlockHeight());
            }
        }).start();

    }

    public static void saveBlocks(final BlockEntity[] blockEntities, final boolean replace) {
        Log.d(TAG, "saveBlocks: " + blockEntities.length);

        final Activity ctx = BreadApp.getBreadContext();
        if (ctx == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (replace) MerkleBlockDataSource.getInstance(ctx).deleteAllBlocks();
                MerkleBlockDataSource.getInstance(ctx).putMerkleBlocks(blockEntities);
            }
        }).start();

    }

    public static void savePeers(final PeerEntity[] peerEntities, final boolean replace) {
        Log.d(TAG, "savePeers: " + peerEntities.length);
        final Activity ctx = BreadApp.getBreadContext();
        if (ctx == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (replace) PeerDataSource.getInstance(ctx).deleteAllPeers();
                PeerDataSource.getInstance(ctx).putPeers(peerEntities);
            }
        }).start();

    }

    public static boolean networkIsReachable() {
        Log.d(TAG, "networkIsReachable");
        return BRWalletManager.getInstance().isNetworkAvailable(BreadApp.getBreadContext());
    }

    public static void deleteBlocks() {
        Log.d(TAG, "deleteBlocks");
        final Activity ctx = BreadApp.getBreadContext();
        if (ctx == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                MerkleBlockDataSource.getInstance(ctx).deleteAllBlocks();
            }
        }).start();

    }

    public static void deletePeers() {
        Log.d(TAG, "deletePeers");
        final Activity ctx = BreadApp.getBreadContext();
        if (ctx == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                PeerDataSource.getInstance(ctx).deleteAllPeers();
            }
        }).start();

    }

    public void startSyncingProgressThread() {
        Log.d(TAG, "startSyncingProgressThread:" + Thread.currentThread().getName());

        try {
            if (syncTask != null) {
                if (running) {
                    Log.e(TAG, "startSyncingProgressThread: syncTask.running == true, returning");
                    return;
                }
                syncTask.interrupt();
                syncTask = null;
            }
            syncTask = new SyncProgressTask();
            syncTask.start();

        } catch (IllegalThreadStateException ex) {
            ex.printStackTrace();
        }

    }

    public void stopSyncingProgressThread() {
        Log.d(TAG, "stopSyncingProgressThread");
        final BreadActivity ctx = BreadActivity.getApp();
        if (ctx == null) {
            Log.e(TAG, "stopSyncingProgressThread: ctx is null");
            return;
        }
        try {
            if (syncTask != null) {
                syncTask.interrupt();
                syncTask = null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private class SyncProgressTask extends Thread {
        public double progressStatus = 0;
        private BreadActivity app;

        public SyncProgressTask() {
            progressStatus = 0;
        }

        @Override
        public void run() {
            if (running) return;
            try {
                app = BreadActivity.getApp();
                progressStatus = 0;
                running = true;
                Log.d(TAG, "run: starting: " + progressStatus);

                if (app != null) {
                    final long lastBlockTimeStamp = BRPeerManager.getInstance().getLastBlockTimestamp() * 1000;
                    app.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (TxManager.getInstance().syncingHolder != null)
                                TxManager.getInstance().syncingHolder.progress.setProgress((int) (progressStatus * 100));
                            if (TxManager.getInstance().syncingHolder != null)
                                TxManager.getInstance().syncingHolder.date.setText(Utils.formatTimeStamp(lastBlockTimeStamp, "MMM. dd, yyyy  ha"));
                        }
                    });
                }

                while (running) {
                    if (app != null) {
                        int startHeight = BRSharedPrefs.getStartHeight(app);
                        progressStatus = syncProgress(startHeight);
//                    Log.e(TAG, "run: progressStatus: " + progressStatus);
                        if (progressStatus == 1) {
                            running = false;
                            continue;
                        }
                        final long lastBlockTimeStamp = BRPeerManager.getInstance().getLastBlockTimestamp() * 1000;
//                        Log.e(TAG, "run: changing the progress to: " + progressStatus + ": " + Thread.currentThread().getName());
                        app.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                if (TxManager.getInstance().currentPrompt != PromptManager.PromptItem.SYNCING) {
                                    Log.e(TAG, "run: currentPrompt != SYNCING, showPrompt(SYNCING) ....");
                                    TxManager.getInstance().showPrompt(app, PromptManager.PromptItem.SYNCING);
                                }

                                if (TxManager.getInstance().syncingHolder != null)
                                    TxManager.getInstance().syncingHolder.progress.setProgress((int) (progressStatus * 100));
                                if (TxManager.getInstance().syncingHolder != null)
                                    TxManager.getInstance().syncingHolder.date.setText(Utils.formatTimeStamp(lastBlockTimeStamp, "MMM. dd, yyyy  ha"));
                            }
                        });

                    } else {
//                        Log.e(TAG, "run: app is null");
                        app = BreadActivity.getApp();
                    }

                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "run: Thread.sleep was Interrupted:" + Thread.currentThread().getName(), e);
                    }

                }

                Log.d(TAG, "run: SyncProgress task finished:" + Thread.currentThread().getName());
            } finally {
                if (progressStatus != 1) {
                    throw new RuntimeException("didn't finish");
                }
                running = false;
                progressStatus = 0;
                if (app != null)
                    TxManager.getInstance().hidePrompt(app, PromptManager.PromptItem.SYNCING);
            }

        }
    }

    public void networkChanged(boolean isOnline) {

//        final RelativeLayout networkErrorBar = (RelativeLayout) ctx.findViewById(R.id.main_internet_status_bar);
//        if (networkErrorBar == null) return;
//
//        final boolean isConnected = ((BreadApp) ctx.getApplication()).hasInternetAccess();
        if (isOnline)
            new Thread(new Runnable() {
                @Override
                public void run() {
                    BRPeerManager.getInstance().connect();
                }
            }).start();
//
//        if (!isConnected) {
//            ctx.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    networkErrorBar.setVisibility(View.VISIBLE);
//                }
//            });
//            BRPeerManager.stopSyncingProgressThread();
//            Log.e(TAG, "Network Not Available ");
//
//        } else {
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    final double progress = BRPeerManager.syncProgress(BRSharedPrefs.getStartHeight(ctx));
//                    ctx.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            networkErrorBar.setVisibility(View.GONE);
//
//                        }
//                    });
//
//                    if (progress < 1 && progress > 0) {
//                        BRPeerManager.startSyncingProgressThread();
//                    }
//                    Log.d(TAG, "Network Available ");
//                }
//            }).start();
//
//        }

    }


    public void addStatusUpdateListener(OnTxStatusUpdate listener) {
        if (statusUpdateListeners == null) {
            return;
        }
        if (!statusUpdateListeners.contains(listener))
            statusUpdateListeners.add(listener);
    }

    public void removeListener(OnTxStatusUpdate listener) {
        if (statusUpdateListeners == null) {
            return;
        }
        statusUpdateListeners.remove(listener);

    }

    public static void setOnSyncFinished(OnSyncSucceeded listener) {
        onSyncFinished = listener;
    }

    public interface OnTxStatusUpdate {
        void onStatusUpdate();
    }

    public interface OnSyncSucceeded {
        void onFinished();
    }

    public static void updateLastBlockHeight(int blockHeight) {
        final Activity ctx = BreadApp.getBreadContext();
        if (ctx == null) return;
        BRSharedPrefs.putLastBlockHeight(ctx, blockHeight);
    }

    public native void create(int earliestKeyTime, int blockCount, int peerCount);

    public native void connect();

    public native void putPeer(byte[] peerAddress, byte[] peerPort, byte[] peerTimeStamp);

    public native void createPeerArrayWithCount(int count);

    public native void putBlock(byte[] block, int blockHeight);

    public native void createBlockArrayWithCount(int count);

    public native static double syncProgress(int startHeight);

    public native static int getCurrentBlockHeight();

    public native static int getRelayCount(byte[] hash);

    public native static int getEstimatedBlockHeight();

    public native boolean isCreated();

    public native boolean isConnected();

    public native void peerManagerFreeEverything();

    public native long getLastBlockTimestamp();

    public native void rescan();
}