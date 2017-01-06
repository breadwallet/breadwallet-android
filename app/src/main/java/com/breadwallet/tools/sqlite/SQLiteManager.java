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
    private static Context ctx;

    private SQLiteManager() {
    }

    public static SQLiteManager getInstance(Context context) {
        ctx = context;
        if (instance == null) {
            instance = new SQLiteManager();
        }
        return instance;
    }

    public List<BRTransactionEntity> getTransactions() {

        TransactionDataSource TXdataSource = null;
        List<BRTransactionEntity> txValues = new ArrayList<>();
        try {
            TXdataSource = new TransactionDataSource(ctx);
            TXdataSource.open();
            txValues = TXdataSource.getAllTransactions();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (TXdataSource != null)
                TXdataSource.close();
        }
        return txValues;
    }

    public void deleteTransactions() {
        TransactionDataSource TXdataSource = null;
        try {
            TXdataSource = new TransactionDataSource(ctx);
            TXdataSource.open();
            TXdataSource.deleteAllTransactions();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (TXdataSource != null)
                TXdataSource.close();
        }
    }

    public void insertTransaction(byte[] transaction, int blockheight, long timestamp, String txHash) {
        BRTransactionEntity entity = new BRTransactionEntity(transaction, blockheight, timestamp, txHash);
        TransactionDataSource TXdataSource = null;

        Log.e(TAG, "SQLiteManager - transaction inserted with timestamp: " + timestamp);

        try {
            TXdataSource = new TransactionDataSource(ctx);
            TXdataSource.open();
            TXdataSource.createTransaction(entity);
        } catch (SQLException e) {
            FirebaseCrash.report(e);
            e.printStackTrace();
        } finally {
            if (TXdataSource != null)
                TXdataSource.close();
        }
    }

    public List<BRMerkleBlockEntity> getBlocks() {
        MerkleBlockDataSource BKdataSource = null;
        List<BRMerkleBlockEntity> BkValues = new ArrayList<>();

        try {
            BKdataSource = new MerkleBlockDataSource(ctx);
            BKdataSource.open();
            BkValues = BKdataSource.getAllMerkleBlocks();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (BKdataSource != null)
                BKdataSource.close();
        }

        return BkValues;
    }

    public void deleteBlocks() {
        MerkleBlockDataSource BKdataSource = null;

        try {
            BKdataSource = new MerkleBlockDataSource(ctx);
            BKdataSource.open();
            BKdataSource.deleteAllBlocks();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (BKdataSource != null)
                BKdataSource.close();
        }
    }

    public void insertMerkleBlocks(BlockEntity[] blockEntities) {
        MerkleBlockDataSource BKdataSource = null;

//        Log.e(TAG, "SQLiteManager - merkleBlock inserted");

        try {
            BKdataSource = new MerkleBlockDataSource(ctx);
            BKdataSource.open();
            BKdataSource.putMerkleBlocks(blockEntities);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (BKdataSource != null)
                BKdataSource.close();
        }
    }

    public List<BRPeerEntity> getPeers() {
        PeerDataSource PRdataSource = null;
        List<BRPeerEntity> PRValues = new ArrayList<>();

        try {
            PRdataSource = new PeerDataSource(ctx);
            PRdataSource.open();
            PRValues = PRdataSource.getAllPeers();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (PRdataSource != null)
                PRdataSource.close();
        }
        return PRValues;
    }

    public void deletePeers() {
        PeerDataSource PRdataSource = null;

        try {
            PRdataSource = new PeerDataSource(ctx);
            PRdataSource.open();
            PRdataSource.deleteAllPeers();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (PRdataSource != null)
                PRdataSource.close();
        }
    }

    public void insertPeer(PeerEntity[] peerEntities) {


        PeerDataSource PRdataSource = null;

//        Log.e(TAG, "SQLiteManager - peer inserted");

        try {
            PRdataSource = new PeerDataSource(ctx);
            PRdataSource.open();
            PRdataSource.putPeers(peerEntities);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (PRdataSource != null)
                PRdataSource.close();
        }
    }

    public void updateTxByHash(String hash, int blockHeight, int timeStamp) {
        TransactionDataSource TXdataSource = null;

        Log.e(TAG, "SQLiteManager - transaction updated");

        try {
            TXdataSource = new TransactionDataSource(ctx);
            TXdataSource.open();
            TXdataSource.updateTxBlockHeight(hash, blockHeight, timeStamp);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (TXdataSource != null)
                TXdataSource.close();
        }
    }


    public void deleteTxByHash(String hash) {
        TransactionDataSource TXdataSource = null;

        Log.e(TAG, "SQLiteManager - transaction inserted");

        try {
            TXdataSource = new TransactionDataSource(ctx);
            TXdataSource.open();
            TXdataSource.deleteTxByHash(hash);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (TXdataSource != null)
                TXdataSource.close();
        }
    }
}
