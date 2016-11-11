package com.platform.sqlite;

import android.content.Context;
import android.database.SQLException;
import android.util.Log;

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

        ReplicatedKVStore txDataSource = null;
        List<KVEntity> kvValues = new ArrayList<>();
        try {
            txDataSource = new ReplicatedKVStore(ctx);
            txDataSource.open();
            kvValues = txDataSource.getAllKVs();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (txDataSource != null)
                txDataSource.close();
        }
//        Log.e(TAG, "getKVs: kvValues.size: " + kvValues.size());
        return kvValues;
    }

    public void deleteKVs() {
        ReplicatedKVStore kvDataSource = null;
        try {
            kvDataSource = new ReplicatedKVStore(ctx);
            kvDataSource.open();
            kvDataSource.deleteAllKVs();
//            Log.e(TAG, "deleteKVs");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (kvDataSource != null)
                kvDataSource.close();
        }
    }

    public void setKv(long version, long remoteVersion, String key, byte[] value, long time, int deleted) {
        KVEntity entity = new KVEntity(version, remoteVersion, key, value, time, deleted);
        ReplicatedKVStore kvDataSource = null;
        try {
            kvDataSource = new ReplicatedKVStore(ctx);
            kvDataSource.open();
            if (kvDataSource.isKeyValid(entity.getKey())) {
                kvDataSource.set(entity);
            }


        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (kvDataSource != null)
                kvDataSource.close();
        }
    }

    public void setKv(KVEntity[] kvs) {
        for (KVEntity kv : kvs)
            setKv(kv);
    }

    public void setKv(List<KVEntity> kvs) {
        for (KVEntity kv : kvs)
            setKv(kv);
    }

    public void setKv(KVEntity kv) {
        setKv(kv.getVersion(), kv.getRemoteVersion(), kv.getKey(), kv.getValue(), kv.getTime(), kv.getDeleted());
    }

    public void deleteKvByKey(String key) {
        ReplicatedKVStore kvDataSource = null;

        try {
            kvDataSource = new ReplicatedKVStore(ctx);
            kvDataSource.open();
            kvDataSource.delete(key, 0);
//            Log.e(TAG, "SQLiteManager - kv deleted with key: " + key);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (kvDataSource != null)
                kvDataSource.close();
        }
    }

    public KVEntity getKv(String key, long version) {
        ReplicatedKVStore txDataSource = null;
        KVEntity kv = null;
        try {
            txDataSource = new ReplicatedKVStore(ctx);
            txDataSource.open();
            kv = txDataSource.getKv(key, version);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (txDataSource != null)
                txDataSource.close();
        }
//        Log.e(TAG, "getKVs: kvValues.size: " + kvValues.size());
        return kv;
    }
}
