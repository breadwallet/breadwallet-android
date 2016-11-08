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
        Log.e(TAG, "putKV: kvEntities.length: " + kvEntities.length);
        database.beginTransaction();
        try {
            for (KVEntity kv : kvEntities) {
//                Log.e(TAG,"sqlite peer saved: " + Arrays.toString(p.getPeerTimeStamp()));
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
        Log.e(TAG, "kv deleted with key: " + key);
        database.delete(PlatformSqliteHelper.KV_STORE_TABLE_NAME, PlatformSqliteHelper.KV_KEY
                + " = " + key, null);
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

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            KVEntity kvEntity = cursorToPeer(cursor);
            kvs.add(kvEntity);
            cursor.moveToNext();
        }
        // make sure to close the cursor

        Log.e(TAG, "kvs: " + kvs.size());
        cursor.close();
        return kvs;
    }


//    func _localVersion(_ key: String) throws -> (UInt64, Date) {
//        var stmt: OpaquePointer? = nil
//        defer {
//            sqlite3_finalize(stmt)
//        }
//        try self.checkErr(sqlite3_prepare_v2(
//                self.db, "SELECT version, thetime FROM kvstore WHERE key = ? ORDER BY version DESC LIMIT 1", -1,
//                &stmt, nil
//        ), s: "get version - prepare")
//        sqlite3_bind_text(stmt, 1, NSString(string: key).utf8String, -1, nil)
//        try self.checkErr(sqlite3_step(stmt), s: "get version - exec", r: SQLITE_ROW)
//        return (
//                UInt64(sqlite3_column_int64(stmt, 0)),
//        Date.withMsTimestamp(UInt64(sqlite3_column_int64(stmt, 1)))
//        )
//    }

    private KVEntity cursorToPeer(Cursor cursor) {
        KVEntity kvEntity = new KVEntity(cursor.getLong(0), cursor.getLong(1), cursor.getString(2), cursor.getBlob(3), cursor.getLong(4), cursor.getInt(5));
//        peerEntity.setId(cursor.getInt(0));
        return kvEntity;
    }
}