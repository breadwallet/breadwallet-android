package com.breadwallet.wallet;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.breadwallet.BreadWalletApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.BlockEntity;
import com.breadwallet.presenter.entities.PeerEntity;
import com.breadwallet.presenter.fragments.FragmentSettingsAll;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.manager.CurrencyManager;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.sqlite.SQLiteManager;

import java.text.DecimalFormat;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on 12/10/15.
 * Copyright (c) 2016 breadwallet llc <mihail@breadwallet.com>
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
    }

    public static BRPeerManager getInstance(Activity context) {
        ctx = context;
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
        int startHeight = SharedPreferencesManager.getStartHeight(ctx);
        Log.e(TAG, "startHeight: " + startHeight);
        Log.e(TAG, "syncStarted: " + syncProgress(startHeight));
        BRPeerManager.getInstance(ctx).refreshConnection();
    }

    public static void syncSucceeded() {
        Log.e(TAG, "syncSucceeded");
        if (ctx == null) ctx = MainActivity.app;
        stopSyncingProgressThread();
        if (ctx != null) {
            SharedPreferencesManager.putStartHeight(ctx, getCurrentBlockHeight());
            ((MainActivity) ctx).hideAllBubbles();
        }
    }

    public static void syncFailed() {
        stopSyncingProgressThread();
        if (ctx != null && ctx instanceof MainActivity) {
            ((MainActivity) ctx).hideAllBubbles();
        }
    }

    public static void txStatusUpdate() {
        Log.e(TAG, "txStatusUpdate");
        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null)
            FragmentSettingsAll.refreshTransactions(ctx);

    }

    public static void saveBlocks(final BlockEntity[] blockEntities) {
        Log.e(TAG, "saveBlocks: " + blockEntities.length);

        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SQLiteManager.getInstance(ctx).insertMerkleBlocks(blockEntities);
                }
            }).start();
        }

    }

    public static void savePeers(final PeerEntity[] peerEntities) {
        Log.e(TAG, "savePeers");
        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SQLiteManager.getInstance(ctx).insertPeer(peerEntities);
                }
            }).start();
        }

    }

    public static boolean networkIsReachable() {
        Log.e(TAG, "networkIsReachable");
        return ctx != null && ((BreadWalletApp) ctx.getApplication()).isNetworkAvailable(ctx);
    }

    public static void deleteBlocks() {
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

    public static void deletePeers() {
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

    public static void startSyncingProgressThread() {
        try {
            if (syncTask == null) {
                syncTask = new SyncProgressTask();
                syncTask.start();
            }

        } catch (IllegalThreadStateException ex) {
            ex.printStackTrace();
        }
        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null) {
            if (!((BreadWalletApp) ctx.getApplication()).isNetworkAvailable(ctx)) return;
            MiddleViewAdapter.setSyncing(ctx, true);
            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ctx.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        if (BRAnimator.level == 0)
                            ((MainActivity) ctx).showHideSyncProgressViews(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

    }

    public static void stopSyncingProgressThread() {
        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null) {
            MiddleViewAdapter.setSyncing(ctx, false);
            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ctx.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        ((MainActivity) ctx).showHideSyncProgressViews(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            });
        }

        try {
            if (syncTask != null) {
                syncTask.setRunning(false);
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

        public void setRunning(boolean b) {
            Log.e(TAG, "setRunning: " + b);
            running = b;
        }

        @Override
        public void run() {
            final MainActivity app = MainActivity.app;
            progressStatus = 0;
            final DecimalFormat decimalFormat = new DecimalFormat("#.#");
            if (app != null) {

                progressStatus = syncProgress(SharedPreferencesManager.getStartHeight(app));
                app.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (BRAnimator.level == 0)
                            app.showHideSyncProgressViews(true);

                        app.setProgress((int) (progressStatus * 100), String.format("%s%%", decimalFormat.format(progressStatus * 100)));
                    }
                });

                while (running) {
                    progressStatus = syncProgress(SharedPreferencesManager.getStartHeight(app));
                    Log.e(TAG, "progressStatus: " + progressStatus);
                    if (progressStatus == 1) running = false;
                    app.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            app.setProgress((int) (progressStatus * 100), String.format("%s%%", decimalFormat.format(progressStatus * 100)));
                        }
                    });
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        running = false;
                        e.printStackTrace();
                    }
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

    public void refreshConnection() {
        final RelativeLayout networkErrorBar = (RelativeLayout) ctx.findViewById(R.id.main_internet_status_bar);
        if (networkErrorBar == null) return;
        final ConnectivityManager connectivityManager = ((ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE));
        boolean isConnected = connectivityManager.getActiveNetworkInfo() != null &&
                connectivityManager.getActiveNetworkInfo().isConnected() &&
                Settings.System.getInt(ctx.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 1;
        BRPeerManager.getInstance(ctx).connect();
        if (!isConnected) {
            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    networkErrorBar.setVisibility(View.VISIBLE);
                    BRPeerManager.stopSyncingProgressThread();
                }
            });

            Log.e(TAG, "Network Not Available ");

        } else {
            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    networkErrorBar.setVisibility(View.GONE);
                    double progress = BRPeerManager.syncProgress(SharedPreferencesManager.getStartHeight(ctx));
                    if (progress < 1 && progress > 0) {
                        BRPeerManager.startSyncingProgressThread();
                    }
                }
            });
            Log.e(TAG, "Network Available ");
        }
    }

    public native void createAndConnect(int earliestKeyTime, int blockCount, int peerCount);

    public native void connect();

    public native void putPeer(byte[] peerAddress, byte[] peerPort, byte[] peerTimeStamp);

    public native void createPeerArrayWithCount(int count);

    public native void putBlock(byte[] block, int blockHeight);

    public native void createBlockArrayWithCount(int count);

    public native static double syncProgress(int startHeight);

    public native static int getCurrentBlockHeight();

    public native static int getEstimatedBlockHeight();

    public native boolean isCreated();

    public native void peerManagerFreeEverything();

    public native void rescan();
}