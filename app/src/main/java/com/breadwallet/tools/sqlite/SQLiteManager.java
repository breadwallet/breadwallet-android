package com.breadwallet.tools.sqlite;

import android.content.Context;
import android.database.SQLException;
import android.util.Log;

import com.breadwallet.presenter.entities.BRMerkleBlockEntity;
import com.breadwallet.presenter.entities.BRPeerEntity;
import com.breadwallet.presenter.entities.BRTransactionEntity;
import com.breadwallet.presenter.entities.BlockEntity;
import com.breadwallet.presenter.entities.PeerEntity;
import com.google.firebase.crash.FirebaseCrash;

import java.util.ArrayList;
import java.util.List;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 12/21/15.
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

public class SQLiteManager {
    private static final String TAG = SQLiteManager.class.getName();

    private static SQLiteManager instance;
    private Context ctx;

    private SQLiteManager(Context ctx) {
        this.ctx = ctx;
    }

    public static SQLiteManager getInstance(Context context) {
        if (instance == null) {
            instance = new SQLiteManager(context);
        }
        return instance;
    }

    public List<BRTransactionEntity> getTransactions() {

        List<BRTransactionEntity> txValues = new ArrayList<>();
        try {
            txValues = new TransactionDataSource(ctx).getAllTransactions();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return txValues;
    }

    public void deleteTransactions() {
        try {
            new TransactionDataSource(ctx).deleteAllTransactions();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertTransaction(byte[] transaction, int blockheight, long timestamp, String txHash) {
        Log.e(TAG, "SQLiteManager - transaction inserted with timestamp: " + timestamp);
        BRTransactionEntity entity = new BRTransactionEntity(transaction, blockheight, timestamp, txHash);

        try {
            new TransactionDataSource(ctx).createTransaction(entity);
        } catch (SQLException e) {
            FirebaseCrash.report(e);
            e.printStackTrace();
        }
    }

    public List<BRMerkleBlockEntity> getBlocks() {
        List<BRMerkleBlockEntity> BkValues = new ArrayList<>();

        try {
            BkValues = new MerkleBlockDataSource(ctx).getAllMerkleBlocks();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return BkValues;
    }

    public void deleteBlocks() {
        try {
            new MerkleBlockDataSource(ctx).deleteAllBlocks();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertMerkleBlocks(BlockEntity[] blockEntities) {
        try {
            new MerkleBlockDataSource(ctx).putMerkleBlocks(blockEntities);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<BRPeerEntity> getPeers() {
        List<BRPeerEntity> PRValues = new ArrayList<>();

        try {
            PRValues = new PeerDataSource(ctx).getAllPeers();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return PRValues;
    }

    public void deletePeers() {
        try {
            new PeerDataSource(ctx).deleteAllPeers();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertPeer(PeerEntity[] peerEntities) {
        try {
            new PeerDataSource(ctx).putPeers(peerEntities);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateTxByHash(String hash, int blockHeight, int timeStamp) {
        Log.e(TAG, "SQLiteManager - transaction updated");

        try {
            new TransactionDataSource(ctx).updateTxBlockHeight(hash, blockHeight, timeStamp);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void deleteTxByHash(String hash) {
        Log.e(TAG, "SQLiteManager - transaction inserted");

        try {
            new TransactionDataSource(ctx).deleteTxByHash(hash);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
