package com.breadwallet.wallet;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.breadwallet.BreadWalletApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.BlockEntity;
import com.breadwallet.presenter.entities.PeerEntity;
import com.breadwallet.presenter.fragments.FragmentSettingsAll;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.sqlite.SQLiteManager;
import com.breadwallet.tools.util.TrustedNode;
import com.breadwallet.tools.util.Utils;

import java.text.DecimalFormat;


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
        Log.d(TAG, "syncStarted");
        int startHeight = SharedPreferencesManager.getStartHeight(ctx);
        int lastHeight = SharedPreferencesManager.getLastBlockHeight(ctx);
        if (startHeight > lastHeight) SharedPreferencesManager.putStartHeight(ctx, lastHeight);

        BRPeerManager.getInstance(ctx).refreshConnection();
    }

    public static void syncSucceeded() {
        Log.d(TAG, "syncSucceeded");
        if (ctx == null) ctx = MainActivity.app;
        SharedPreferencesManager.putAllowSpend(ctx, true);
        stopSyncingProgressThread();
        if (ctx != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SharedPreferencesManager.putStartHeight(ctx, getCurrentBlockHeight());
                }
            }).start();

            ((MainActivity) ctx).hideAllBubbles();
        }
    }

    public static void syncFailed() {
        Log.d(TAG, "syncFailed");
        stopSyncingProgressThread();
        if (ctx != null && ctx instanceof MainActivity) {
            Log.e(TAG, "Network Not Available, showing not connected bar  ");
            ((MainActivity) ctx).hideAllBubbles();
            final RelativeLayout networkErrorBar = (RelativeLayout) ctx.findViewById(R.id.main_internet_status_bar);
            if (networkErrorBar == null) return;

            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    networkErrorBar.setVisibility(View.VISIBLE);

                }
            });
            BRPeerManager.stopSyncingProgressThread();
        }

    }

    public static void txStatusUpdate() {
        Log.d(TAG, "txStatusUpdate");
        if (ctx == null) ctx = MainActivity.app;
        if (ctx == null) return;

        FragmentSettingsAll.refreshTransactions(ctx);
        new Thread(new Runnable() {
            @Override
            public void run() {
                updateLastBlockHeight(getCurrentBlockHeight());
            }
        }).start();

    }

    public static void saveBlocks(final BlockEntity[] blockEntities, final boolean replace) {
        Log.d(TAG, "saveBlocks: " + blockEntities.length);

        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (replace) SQLiteManager.getInstance(ctx).deleteBlocks();
                    SQLiteManager.getInstance(ctx).insertMerkleBlocks(blockEntities);
                }
            }).start();
        }

    }

    public static void savePeers(final PeerEntity[] peerEntities, final boolean replace) {
        Log.d(TAG, "savePeers: " + peerEntities.length);
        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (replace) SQLiteManager.getInstance(ctx).deletePeers();
                    SQLiteManager.getInstance(ctx).insertPeer(peerEntities);
                }
            }).start();
        }

    }

    public static boolean networkIsReachable() {
        Log.d(TAG, "networkIsReachable");
        return ctx != null && ((BreadWalletApp) ctx.getApplication()).hasInternetAccess();
    }

    public static void deleteBlocks() {
        Log.d(TAG, "deleteBlocks");
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
        Log.d(TAG, "deletePeers");
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
        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null) {
            MiddleViewAdapter.setSyncing(ctx, true);
            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
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
        Log.d(TAG, "stopSyncingProgressThread");
        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null) {
            MiddleViewAdapter.setSyncing(ctx, false);
            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ((MainActivity) ctx).showHideSyncProgressViews(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
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

    private static class SyncProgressTask extends Thread {
        public boolean running = true;
        public double progressStatus = 0;

        public SyncProgressTask() {
            progressStatus = 0;
            running = true;
        }

        @Override
        public void run() {
            final MainActivity app = MainActivity.app;
            progressStatus = 0;
            final DecimalFormat decimalFormat = new DecimalFormat("#.#");
            if (app != null) {
                app.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        app.setProgress((int) (progressStatus * 100), String.format("%s%%", decimalFormat.format(progressStatus * 100)));
                    }
                });
                progressStatus = syncProgress(SharedPreferencesManager.getStartHeight(app));
                app.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (BRAnimator.level == 0)
                            app.showHideSyncProgressViews(true);

                        app.setProgress((int) (progressStatus * 100), String.format("%s%%", decimalFormat.format(progressStatus * 100)));
                    }
                });
                int startHeight = SharedPreferencesManager.getStartHeight(app);
                while (running) {
                    progressStatus = syncProgress(startHeight);
//                    Log.e(TAG, "run: progressStatus: " + progressStatus);
                    if (progressStatus == 1) {
                        running = false;
                        continue;
                    }
                    app.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            app.setProgress((int) (progressStatus * 100), String.format("%s%%", decimalFormat.format(progressStatus * 100)));
                        }
                    });
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "run: Thread.sleep was Interrupted");
                        running = false;
                        app.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressStatus = 0;
                                app.showHideSyncProgressViews(false);
                            }
                        });
                    }
                }

            }

        }
    }

    public void refreshConnection() {
        final RelativeLayout networkErrorBar = (RelativeLayout) ctx.findViewById(R.id.main_internet_status_bar);
        if (networkErrorBar == null) return;

        final boolean isConnected = ((BreadWalletApp) ctx.getApplication()).hasInternetAccess();
        new Thread(new Runnable() {
            @Override
            public void run() {
                BRPeerManager.getInstance(ctx).connect();
            }
        }).start();

        if (!isConnected) {
            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    networkErrorBar.setVisibility(View.VISIBLE);
                }
            });
            BRPeerManager.stopSyncingProgressThread();
            Log.e(TAG, "Network Not Available ");

        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final double progress = BRPeerManager.syncProgress(SharedPreferencesManager.getStartHeight(ctx));
                    ctx.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            networkErrorBar.setVisibility(View.GONE);

                        }
                    });

                    if (progress < 1 && progress > 0) {
                        BRPeerManager.startSyncingProgressThread();
                    }
                    Log.d(TAG, "Network Available ");
                }
            }).start();

        }

    }

    public void updateFixedPeer() {
        String node = SharedPreferencesManager.getTrustNode(ctx);
        String host = TrustedNode.getNodeHost(node);
        int port = TrustedNode.getNodePort(node);
//        Log.e(TAG, "trust onClick: host:" + host);
//        Log.e(TAG, "trust onClick: port:" + port);
        boolean success = setFixedPeer(host, port);
        if (!success) Log.e(TAG, "updateFixedPeer: Failed to updateFixedPeer with input: " + node);
        BRPeerManager.getInstance(ctx).connect();
    }

    public static void updateLastBlockHeight(int blockHeight) {
        if (ctx == null) ctx = MainActivity.app;
        if (ctx == null) return;
        SharedPreferencesManager.putLastBlockHeight(ctx, blockHeight);
    }

    public native void create(int earliestKeyTime, int blockCount, int peerCount);

    public native void connect();

    public native void putPeer(byte[] peerAddress, byte[] peerPort, byte[] peerTimeStamp);

    public native void createPeerArrayWithCount(int count);

    public native boolean setFixedPeer(String node, int port);

    public native void putBlock(byte[] block, int blockHeight);

    public native void createBlockArrayWithCount(int count);

    public native static double syncProgress(int startHeight);

    public native static int getCurrentBlockHeight();

    public native static int getEstimatedBlockHeight();

    public native boolean isCreated();

    public native boolean isConnected();

    public native void peerManagerFreeEverything();

    public native String getCurrentPeerName();

    public native void rescan();
}