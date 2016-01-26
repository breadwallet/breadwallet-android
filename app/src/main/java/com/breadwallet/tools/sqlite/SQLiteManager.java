package com.breadwallet.tools.sqlite;

import android.content.Context;
import android.util.Log;

import com.breadwallet.presenter.entities.BRMerkleBlockEntity;
import com.breadwallet.presenter.entities.BRPeerEntity;
import com.breadwallet.presenter.entities.BRTransactionEntity;

import java.util.List;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 12/21/15.
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
public class SQLiteManager {
    private static final String TAG = SQLiteManager.class.getName();

    private static SQLiteManager instance;
    private static Context ctx;

    private SQLiteManager() {
    }

    public static synchronized SQLiteManager getInstance(Context context) {
        ctx = context;
        if (instance == null) {
            instance = new SQLiteManager();
        }
        return instance;
    }

    public List<BRTransactionEntity> getTransactions() {
        TransactionDataSource TXdataSource = new TransactionDataSource(ctx);
        TXdataSource.open();
        List<BRTransactionEntity> txValues = TXdataSource.getAllTransactions();
        TXdataSource.close();
        return txValues;
    }

    public void insertTransaction(byte[] transaction, long blockheight, long timestamp) {
        BRTransactionEntity entity = new BRTransactionEntity(transaction, blockheight, timestamp);
        TransactionDataSource TXdataSource = new TransactionDataSource(ctx);
        TXdataSource.open();
        TXdataSource.createTransaction(entity);
        Log.e(TAG, "SQLiteManager - transaction inserted");
        TXdataSource.close();
    }

    public List<BRMerkleBlockEntity> getBlocks() {
        MerkleBlockDataSource BKdataSource = new MerkleBlockDataSource(ctx);
        BKdataSource.open();
        List<BRMerkleBlockEntity> BkValues = BKdataSource.getAllMerkleBlocks();
        BKdataSource.close();
        return BkValues;
    }

    public void insertMerkleBlock(byte[] merkleBlock) {
        BRMerkleBlockEntity entity = new BRMerkleBlockEntity(merkleBlock);
        MerkleBlockDataSource BKdataSource = new MerkleBlockDataSource(ctx);
        BKdataSource.open();
        BKdataSource.createMerkleBlock(entity);
        Log.e(TAG, "SQLiteManager - merkleBlock inserted");
        BKdataSource.close();
    }

    public List<BRPeerEntity> getPeers() {
        PeerDataSource PRdataSource = new PeerDataSource(ctx);
        PRdataSource.open();
        List<BRPeerEntity> PRValues = PRdataSource.getAllPeers();
        PRdataSource.close();
        return PRValues;
    }

    public void insertPeer(byte[] peerAddress, byte[] peerPort, byte[] peerTimeStamp) {
        BRPeerEntity entity = new BRPeerEntity(peerAddress, peerPort, peerTimeStamp);
        PeerDataSource PRdataSource = new PeerDataSource(ctx);
        PRdataSource.open();
        PRdataSource.createPeer(entity);
        Log.e(TAG, "SQLiteManager - peer inserted");
        PRdataSource.close();
    }

}
