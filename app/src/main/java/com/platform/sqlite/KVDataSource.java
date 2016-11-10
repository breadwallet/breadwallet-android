package com.platform.sqlite;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 9/25/15.
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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

class KVDataSource {
    private static final String TAG = KVDataSource.class.getName();

    // Database fields
    private SQLiteDatabase database;
    private final PlatformSqliteHelper dbHelper;
    private final String[] allColumns = {
            PlatformSqliteHelper.KV_VERSION,
            PlatformSqliteHelper.KV_REMOTE_VERSION,
            PlatformSqliteHelper.KV_KEY,
            PlatformSqliteHelper.KV_VALUE,
            PlatformSqliteHelper.KV_TIME,
            PlatformSqliteHelper.KV_DELETED
    };

    private KVDataSource() {
        dbHelper = null;
    }

    public KVDataSource(Context context) {
        dbHelper = new PlatformSqliteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper != null ? dbHelper.getWritableDatabase() : null;
    }

    public void close() {
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    public void putKV(KVEntity kv) {
        putKV(new KVEntity[]{kv});
    }

    public void putKV(KVEntity[] kvEntities) {
//        Log.e(TAG, "putKV: kvEntities.length: " + kvEntities.length);
        database.beginTransaction();
        try {
            for (KVEntity kv : kvEntities) {
                ContentValues values = new ContentValues();
                values.put(PlatformSqliteHelper.KV_VERSION, kv.getVersion());
                values.put(PlatformSqliteHelper.KV_REMOTE_VERSION, kv.getRemoteVersion());
                values.put(PlatformSqliteHelper.KV_KEY, kv.getKey());
                values.put(PlatformSqliteHelper.KV_VALUE, kv.getValue());
                values.put(PlatformSqliteHelper.KV_TIME, kv.getTime());
                values.put(PlatformSqliteHelper.KV_DELETED, kv.getDeleted());
                database.insert(PlatformSqliteHelper.KV_STORE_TABLE_NAME, null, values);
            }
            database.setTransactionSuccessful();
        } catch (Exception ex) {
            Log.e(TAG, "Error inserting into SQLite", ex);
            //Error in between database transaction
        } finally {
            database.endTransaction();
        }

    }

    public void deleteKv(String key) {
//        Log.e(TAG, "kv deleted with key: " + key);
        database.delete(PlatformSqliteHelper.KV_STORE_TABLE_NAME, PlatformSqliteHelper.KV_KEY
                + " = " + key, null);
    }


    //get kv by key and version (version can be 0)
    public KVEntity getKv(String key, long version) {
        KVEntity kv = null;
        Cursor cursor = null;
        long curVer = 0;

        //if no version, fine the version
        if (version == 0) {
            curVer = getVersionWithKey(key);
        } else {
            //if we have a version, check if it's correct
            cursor = database.query(PlatformSqliteHelper.KV_STORE_TABLE_NAME,
                    allColumns, "key = ? AND version = ?", new String[]{key, String.valueOf(version)},
                    null, null, "version DESC", "1");
            boolean success = cursor.moveToNext();
            if (success)
                curVer = cursor.getLong(0);
            else
                curVer = 0;
        }

        //if still 0 then version is non-existent or wrong.
        if (curVer == 0) {
            if (cursor != null)
                cursor.close();
            return null;
        }

        cursor = database.query(PlatformSqliteHelper.KV_STORE_TABLE_NAME,
                allColumns, "key = ? AND version = ?", new String[]{key, String.valueOf(version)},
                null, null, "version DESC", "1");
        if (cursor.getCount() != 0) {
            cursor.moveToNext();
            kv = cursorToKv(cursor);
        }

        cursor.close();
        return kv;
    }

    public long getVersionWithKey(String key) {
        long version;
        String selectQuery = "SELECT version, thetime FROM " + PlatformSqliteHelper.KV_STORE_TABLE_NAME + " WHERE key = ? ORDER BY version DESC LIMIT 1";
        Cursor cursor = database.rawQuery(selectQuery, new String[]{key});
        version = cursor.getLong(0);

        cursor.close();
        return version;

    }
//    public void deleteKv(KVEntity kv) {
//        Log.e(TAG, "kv deleted with key: " + kv.getKey());
//        database.delete(PlatformSqliteHelper.KV_STORE_TABLE_NAME, PlatformSqliteHelper.KV_KEY
//                + " = " + kv.getKey(), null);
//    }

    public void deleteAllKVs() {
        database.delete(PlatformSqliteHelper.KV_STORE_TABLE_NAME, PlatformSqliteHelper.KV_TIME + " <> -1", null);
    }

    public List<KVEntity> getAllKVs() {
        List<KVEntity> kvs = new ArrayList<>();

        Cursor cursor = database.query(PlatformSqliteHelper.KV_STORE_TABLE_NAME,
                allColumns, null, null, null, null, null);
        Log.e(TAG, "getAllKVs: cursor size: " + cursor.getCount());
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            KVEntity kvEntity = cursorToKv(cursor);
            kvs.add(kvEntity);
            cursor.moveToNext();
        }
        // make sure to close the cursor

        Log.e(TAG, "kvs: " + kvs.size());
        cursor.close();
        return kvs;
    }

    private KVEntity cursorToKv(Cursor cursor) {
        long version = 0;
        long remoteVersion = 0;
        String key = null;
        byte[] value = null;
        long time = 0;
        int deleted = 0;

        try {
            version = cursor.getLong(0);
            remoteVersion = cursor.getLong(1);
            key = cursor.getString(2);
            value = cursor.getBlob(3);
            time = cursor.getLong(4);
            deleted = cursor.getInt(5);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new KVEntity(version, remoteVersion, key, value, time, deleted);
    }
}