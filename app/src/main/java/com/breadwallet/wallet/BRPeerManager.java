package com.breadwallet.wallet;

import android.content.Context;
import android.util.Log;

import com.breadwallet.tools.sqlite.SQLiteManager;

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
//    private byte[] peerManager;
    private static Context ctx;

    private BRPeerManager() {
    }

    public static synchronized BRPeerManager getInstance(Context context) {
        ctx = context;
        if (instance == null) {
            instance = new BRPeerManager();
        }
        return instance;
    }

    public native void connect(long earliestKeyTime, long blockCount, long peerCount);

    public native void putPeer(byte[] peerAddress, byte[] peerPort, byte[] peerTimeStamp);

    public native void createPeerArrayWithCount(int count);

    public native void putBlock(byte[] block);

    public native void createBlockArrayWithCount(int count);

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

    public void syncStarted() {
        Log.e(TAG, "syncStarted");
    }

    public void syncSucceded() {
        Log.e(TAG, "syncSucceded");
    }

    public void syncFailed() {
        Log.e(TAG, "syncFailed");
    }

    public void txStatusUpdate() {
        Log.e(TAG, "txStatusUpdate");
    }

    public void saveBlocks(byte[] block) {
        Log.e(TAG, "saveBlocks");
        SQLiteManager.getInstance(ctx).insertMerkleBlock(block);
    }

    public void savePeers(byte[] peerAddress, byte[] peerPort, byte[] peerTimeStamp) {
        Log.e(TAG, "savePeers");
        SQLiteManager.getInstance(ctx).insertPeer(peerAddress, peerPort, peerTimeStamp);
    }

    public void networkIsReachable() {
        Log.e(TAG, "networkIsReachable");
    }

}
