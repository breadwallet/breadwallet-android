package com.breadwallet.wallet;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.breadwallet.BreadWalletApp;
import com.breadwallet.presenter.activities.BreadActivity;
import com.breadwallet.presenter.entities.BlockEntity;
import com.breadwallet.presenter.entities.PeerEntity;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.manager.SharedPreferencesManager;
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

    public static List<OnTxStatusUpdate> statusUpdateListeners;

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
        Log.d(TAG, "syncStarted");
//        BRPeerManager.getInstance().refreshConnection();
        Context ctx = BreadWalletApp.getBreadContext();
        int startHeight = SharedPreferencesManager.getStartHeight(ctx);
        int lastHeight = SharedPreferencesManager.getLastBlockHeight(ctx);
        if (startHeight > lastHeight) SharedPreferencesManager.putStartHeight(ctx, lastHeight);
        startSyncingProgressThread();
    }

    public static void syncSucceeded() {
        Log.d(TAG, "syncSucceeded");
        final Activity app = BreadWalletApp.getBreadContext();
        if (app == null) return;
        SharedPreferencesManager.putAllowSpend(app, true);
        stopSyncingProgressThread();
        new Thread(new Runnable() {
            @Override
            public void run() {
                SharedPreferencesManager.putStartHeight(app, getCurrentBlockHeight());
            }
        }).start();

    }

    public static void syncFailed() {
        Log.d(TAG, "syncFailed");
        stopSyncingProgressThread();
        Activity ctx = BreadWalletApp.getBreadContext();
        if (ctx == null) return;
        Log.e(TAG, "Network Not Available, showing not connected bar  ");
//            ((MainActivity) ctx).hideAllBubbles();
//        final RelativeLayout networkErrorBar = (RelativeLayout) ctx.findViewById(R.id.main_internet_status_bar);
//        if (networkErrorBar == null) return;
//
//        ctx.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                networkErrorBar.setVisibility(View.VISIBLE);
//
//            }
//        });
//        BRPeerManager.stopSyncingProgressThread();

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

    public static void saveBlocks(final BlockEntity[] blockEntities) {
        Log.d(TAG, "saveBlocks: " + blockEntities.length);

        final Activity ctx = BreadWalletApp.getBreadContext();
        if (ctx == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                MerkleBlockDataSource.getInstance(ctx).putMerkleBlocks(blockEntities);
            }
        }).start();

    }

    public static void savePeers(final PeerEntity[] peerEntities) {
        Log.d(TAG, "savePeers: " + peerEntities.length);
        final Activity ctx = BreadWalletApp.getBreadContext();
        if (ctx == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                PeerDataSource.getInstance(ctx).putPeers(peerEntities);
            }
        }).start();

    }

    public static boolean networkIsReachable() {
        Log.d(TAG, "networkIsReachable");
        return BRWalletManager.getInstance().isNetworkAvailable(BreadWalletApp.getBreadContext());
    }

    public static void deleteBlocks() {
        Log.d(TAG, "deleteBlocks");
        final Activity ctx = BreadWalletApp.getBreadContext();
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
        final Activity ctx = BreadWalletApp.getBreadContext();
        if (ctx == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                PeerDataSource.getInstance(ctx).deleteAllPeers();
            }
        }).start();

    }

    public static void startSyncingProgressThread() {
        Log.d(TAG, "startSyncingProgressThread");

        try {
            if (syncTask != null) {
                syncTask.interrupt();
                syncTask = null;
            }
            syncTask = new SyncProgressTask();
            syncTask.start();

        } catch (IllegalThreadStateException ex) {
            ex.printStackTrace();
        }

        final BreadActivity ctx = BreadActivity.getApp();
        if (ctx == null) return;
        ctx.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ctx.showSyncing(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void stopSyncingProgressThread() {
        Log.d(TAG, "stopSyncingProgressThread");
        final BreadActivity ctx = BreadActivity.getApp();
        if (ctx == null) return;
        ctx.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ctx.showSyncing(false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        try {
            if (syncTask != null) {
                syncTask.interrupt();
                syncTask = null;
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

        @Override
        public void run() {
            final BreadActivity app = BreadActivity.getApp();
            progressStatus = 0;
            Log.e(TAG, "run: starting: " + progressStatus);
            if (app != null) {
                final long lastBlockTimeStamp = BRPeerManager.getInstance().getLastBlockTimestamp() * 1000;
                app.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (app.syncProgressBar != null)
                            app.syncProgressBar.setProgress((int) (progressStatus * 100));
                        if (app.syncDate != null)
                            app.syncDate.setText(Utils.formatTimeStamp(lastBlockTimeStamp, "MMM. dd, yyyy  ha"));
                    }
                });
            }

            while (running) {
                final BreadActivity tmp = BreadActivity.getApp();
                if (tmp != null) {
                    Context context = BreadWalletApp.getBreadContext();
                    int startHeight = context == null ? 0 : SharedPreferencesManager.getStartHeight(context);
                    progressStatus = syncProgress(startHeight);
//                    Log.e(TAG, "run: progressStatus: " + progressStatus);
                    if (progressStatus == 1) {
                        running = false;
                        continue;
                    }
                    final long lastBlockTimeStamp = BRPeerManager.getInstance().getLastBlockTimestamp() * 1000;

                    tmp.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (tmp.syncProgressBar != null)
                                tmp.syncProgressBar.setProgress((int) (progressStatus * 100));
                            if (tmp.syncDate != null)
                                tmp.syncDate.setText(Utils.formatTimeStamp(lastBlockTimeStamp, "MMM. dd, yyyy  ha"));
                        }
                    });
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "run: Thread.sleep was Interrupted");
                        running = false;
                        tmp.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressStatus = 0;
                                tmp.showSyncing(false);
                            }
                        });
                    }
                }

            }

        }
    }

    public void networkChanged(boolean isOnline) {

//        final RelativeLayout networkErrorBar = (RelativeLayout) ctx.findViewById(R.id.main_internet_status_bar);
//        if (networkErrorBar == null) return;
//
//        final boolean isConnected = ((BreadWalletApp) ctx.getApplication()).hasInternetAccess();
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
//                    final double progress = BRPeerManager.syncProgress(SharedPreferencesManager.getStartHeight(ctx));
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
            Log.e(TAG, "addBalanceChangedListener: statusUpdateListeners is null");
            return;
        }
        if (!statusUpdateListeners.contains(listener))
            statusUpdateListeners.add(listener);
    }

    public void removeListener(OnTxStatusUpdate listener) {
        if (statusUpdateListeners == null) {
            Log.e(TAG, "addBalanceChangedListener: statusUpdateListeners is null");
            return;
        }
        statusUpdateListeners.remove(listener);

    }

    public interface OnTxStatusUpdate {
        void onStatusUpdate();
    }

    public static void updateLastBlockHeight(int blockHeight) {
        final Activity ctx = BreadWalletApp.getBreadContext();
        if (ctx == null) return;
        SharedPreferencesManager.putLastBlockHeight(ctx, blockHeight);
    }

    public native void create(int earliestKeyTime, int blockCount, int peerCount);

    public native void connect();

    public native void putPeer(byte[] peerAddress, byte[] peerPort, byte[] peerTimeStamp);

    public native void createPeerArrayWithCount(int count);

    public native void putBlock(byte[] block, int blockHeight);

    public native void createBlockArrayWithCount(int count);

    public native static double syncProgress(int startHeight);

    public native static int getCurrentBlockHeight();

    public native static int getRelayCount(String hash);

    public native static int getEstimatedBlockHeight();

    public native boolean isCreated();

    public native boolean isConnected();

    public native void peerManagerFreeEverything();

    public native long getLastBlockTimestamp();

    public native void rescan();
}