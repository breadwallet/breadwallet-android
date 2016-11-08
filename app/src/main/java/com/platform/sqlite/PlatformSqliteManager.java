package com.platform.sqlite;

import android.content.Context;
import android.database.SQLException;
import android.util.Log;

import com.breadwallet.presenter.entities.BRTransactionEntity;
import com.breadwallet.tools.sqlite.TransactionDataSource;

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

public class PlatformSqliteManager {
    private static final String TAG = PlatformSqliteManager.class.getName();

    private static PlatformSqliteManager instance;
    private Context ctx;

    private PlatformSqliteManager(Context ctx) {
        this.ctx = ctx;
    }

    public static PlatformSqliteManager getInstance(Context context) {

        if (instance == null) {
            instance = new PlatformSqliteManager(context);
        }
        return instance;
    }

    public List<KVEntity> getKVs() {

        KVDataSource txDataSource = null;
        List<KVEntity> kvValues = new ArrayList<>();
        try {
            txDataSource = new KVDataSource(ctx);
            txDataSource.open();
            kvValues = txDataSource.getAllKVs();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (txDataSource != null)
                txDataSource.close();
        }
        Log.e(TAG, "getKVs: kvValues.size: " + kvValues.size());
        return kvValues;
    }

    public void deleteKVs() {
        KVDataSource kvDataSource = null;
        try {
            kvDataSource = new KVDataSource(ctx);
            kvDataSource.open();
            kvDataSource.deleteAllKVs();
            Log.e(TAG, "deleteKVs");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (kvDataSource != null)
                kvDataSource.close();
        }
    }

    public void insertKv(long version, long remoteVersion, String key, byte[] value, long time, int deleted) {
        Log.e(TAG, "SQLiteManager - kv inserted with key: " + key);
        KVEntity entity = new KVEntity(version, remoteVersion, key, value, time, deleted);
        KVDataSource kvDataSource = null;
        try {
            kvDataSource = new KVDataSource(ctx);
            kvDataSource.open();
            kvDataSource.putKV(entity);

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (kvDataSource != null)
                kvDataSource.close();
        }
    }

    public void deleteKvByKey(String key) {
        KVDataSource kvDataSource = null;

        try {
            kvDataSource = new KVDataSource(ctx);
            kvDataSource.open();
            kvDataSource.deleteKv(key);
            Log.e(TAG, "SQLiteManager - kv deleted with key: " + key);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (kvDataSource != null)
                kvDataSource.close();
        }
    }
//
//    public List<BRMerkleBlockEntity> getBlocks() {
//        MerkleBlockDataSource BKdataSource = null;
//        List<BRMerkleBlockEntity> BkValues = new ArrayList<>();
//
//        try {
//            BKdataSource = new MerkleBlockDataSource(ctx);
//            BKdataSource.open();
//            BkValues = BKdataSource.getAllMerkleBlocks();
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//        } finally {
//            if (BKdataSource != null)
//                BKdataSource.close();
//        }
//
//        return BkValues;
//    }
//
//    public void deleteBlocks() {
//        MerkleBlockDataSource BKdataSource = null;
//
//        try {
//            BKdataSource = new MerkleBlockDataSource(ctx);
//            BKdataSource.open();
//            BKdataSource.deleteAllBlocks();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        } finally {
//            if (BKdataSource != null)
//                BKdataSource.close();
//        }
//    }
//
//    public void insertMerkleBlocks(BlockEntity[] blockEntities) {
//        MerkleBlockDataSource BKdataSource = null;
//
////        Log.e(TAG, "SQLiteManager - merkleBlock inserted");
//
//        try {
//            BKdataSource = new MerkleBlockDataSource(ctx);
//            BKdataSource.open();
//            BKdataSource.putMerkleBlocks(blockEntities);
//        } catch (SQLException e) {
//            e.printStackTrace();
//        } finally {
//            if (BKdataSource != null)
//                BKdataSource.close();
//        }
//    }
//
//    public List<BRPeerEntity> getPeers() {
//        PeerDataSource PRdataSource = null;
//        List<BRPeerEntity> PRValues = new ArrayList<>();
//
//        try {
//            PRdataSource = new PeerDataSource(ctx);
//            PRdataSource.open();
//            PRValues = PRdataSource.getAllPeers();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        } finally {
//            if (PRdataSource != null)
//                PRdataSource.close();
//        }
//        return PRValues;
//    }
//
//    public void deletePeers() {
//        PeerDataSource PRdataSource = null;
//
//        try {
//            PRdataSource = new PeerDataSource(ctx);
//            PRdataSource.open();
//            PRdataSource.deleteAllPeers();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        } finally {
//            if (PRdataSource != null)
//                PRdataSource.close();
//        }
//    }
//
//    public void insertPeer(PeerEntity[] peerEntities) {
//
//
//        PeerDataSource PRdataSource = null;
//
////        Log.e(TAG, "SQLiteManager - peer inserted");
//
//        try {
//            PRdataSource = new PeerDataSource(ctx);
//            PRdataSource.open();
//            PRdataSource.putPeers(peerEntities);
//        } catch (SQLException e) {
//            e.printStackTrace();
//        } finally {
//            if (PRdataSource != null)
//                PRdataSource.close();
//        }
//    }
//
//    public void updateTxByHash(String hash, int blockHeight, int timeStamp) {
//        TransactionDataSource TXdataSource = null;
//
//        Log.e(TAG, "SQLiteManager - transaction updated");
//
//        try {
//            TXdataSource = new TransactionDataSource(ctx);
//            TXdataSource.open();
//            TXdataSource.updateTxBlockHeight(hash, blockHeight, timeStamp);
//        } catch (SQLException e) {
//            e.printStackTrace();
//        } finally {
//            if (TXdataSource != null)
//                TXdataSource.close();
//        }
//    }
//
//
//    public void deleteTxByHash(String hash) {
//        TransactionDataSource TXdataSource = null;
//
//        Log.e(TAG, "SQLiteManager - transaction inserted");
//
//        try {
//            TXdataSource = new TransactionDataSource(ctx);
//            TXdataSource.open();
//            TXdataSource.deleteTxByHash(hash);
//        } catch (SQLException e) {
//            e.printStackTrace();
//        } finally {
//            if (TXdataSource != null)
//                TXdataSource.close();
//        }
//    }
}
