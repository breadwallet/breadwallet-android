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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class ReplicatedKVStore {
    private static final String TAG = ReplicatedKVStore.class.getName();

    private static final String KEY_REGEX = "^[^_][\\w-]{1,255}$";

    public boolean syncImmediately = true;
    public boolean encrypted = false;
    private boolean syncRunning = false;

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

    private ReplicatedKVStore() {
        dbHelper = null;
    }

    public ReplicatedKVStore(Context context) {
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

    public void set(KVEntity kv) {
        set(new KVEntity[]{kv});
    }

    /**
     * Set the value of a key locally in the database. If syncImmediately is true (the default) then immediately
     * after successfully saving locally, replicate to server. The `localVer` key must be the same as is currently
     * stored in the database. To create a new key, pass `0` as `localVer`
     */
    public void set(KVEntity[] kvEntities) {
//        Log.e(TAG, "set: kvEntities.length: " + kvEntities.length);

        for (KVEntity kv : kvEntities) {
            boolean success = _set(kv);
            if (syncImmediately && success) {
                syncKey(kv.getKey());
                Log.e(TAG, "setKv: key synced: " + kv.getKey());
            }
        }

    }

    public boolean _set(KVEntity kv) {
        boolean success = false;
        long curVer = kv.getVersion();
        long newVer = 0;
        String key = kv.getKey();
        database.beginTransaction();
        try {
            long localVer = _localVersion(key);
            if (curVer != localVer) {
                Log.e(TAG, String.format("set key %s conflict: version %d != current version %d", key, localVer, curVer));
                return false;
            }
            newVer = curVer + 1;
            byte[] encryptionData = encrypted ? encrypt(kv.getValue()) : kv.getValue();

            ContentValues values = new ContentValues();
            values.put(PlatformSqliteHelper.KV_VERSION, newVer);
            values.put(PlatformSqliteHelper.KV_REMOTE_VERSION, kv.getRemoteVersion());
            values.put(PlatformSqliteHelper.KV_KEY, key);
            values.put(PlatformSqliteHelper.KV_VALUE, encryptionData);
            values.put(PlatformSqliteHelper.KV_TIME, kv.getTime());
            values.put(PlatformSqliteHelper.KV_DELETED, kv.getDeleted());
            if (localVer == 0)
                database.insert(PlatformSqliteHelper.KV_STORE_TABLE_NAME, null, values);
            else
                database.update(PlatformSqliteHelper.KV_STORE_TABLE_NAME, values, "key=" + key, null);

            database.setTransactionSuccessful();
        } catch (Exception ex) {
            Log.e(TAG, "Error inserting into SQLite", ex);
            //Error in between database transaction
        } finally {
            database.endTransaction();
        }
        return success;
    }

    /**
     * get kv by key and version (version can be 0)
     */
    public KVEntity getKv(String key, long version) {
        KVEntity kv = null;
        Cursor cursor = null;
        long curVer = 0;

        database.beginTransaction();
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
            database.endTransaction();
            return null;
        }

        cursor = database.query(PlatformSqliteHelper.KV_STORE_TABLE_NAME,
                allColumns, "key = ? AND version = ?", new String[]{key, String.valueOf(version)},
                null, null, "version DESC", "1");
        if (cursor.getCount() != 0) {
            cursor.moveToNext();
            kv = cursorToKv(cursor);
        }
        database.setTransactionSuccessful();
        database.endTransaction();
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

    /**
     * Sync an individual key. Normally this is only called internally and you should call syncAllKeys
     */
    public void syncKey(String key) {
        if (syncRunning) return;
        syncRunning = true;
        try {

        } catch (Exception ex) {
            syncRunning = false;
        }
    }


    /**
     * Sync all keys to and from the remote kv store adaptor
     */
    private boolean syncAllKeys() {
        return false;
    }

    /**
     * Get the remote version for the key for the most recent local version of the key, if stored.
     * If local key doesn't exist, return 0
     * <p>
     * func remoteVersion(key: String) throws -> UInt64 {
     * return 0
     * }
     */
    private long remoteVersion(String key) {
        return 0;
    }

    /**
     * Record the remote version for the object in a new version of the local key
     */
    private boolean setRemoteVersion(String key, long localVer, long remoteVer) {
        return false;
    }

    /**
     * the syncKey kernel - this is provided so syncAllKeys can provide get a bunch of key versions at once
     * and fan out the _syncKey operations
     */
    private void _syncKey(String key, long remoteVersion, long remoteTime) {
        // this is a basic last-write-wins strategy. data loss is possible but in general
        // we will attempt to sync before making any local modifications to the data
        // and concurrency will be so low that we don't really need a fancier solution than this.
        // the strategy is:
        //
        // 1. get the remote version. this is our "lock"
        // 2. along with the remote version will come the last-modified date of the remote object
        // 3. if their last-modified date is newer than ours, overwrite ours
        // 4. if their last-modified date is older than ours, overwrite theirs
        if (!syncRunning) throw new IllegalArgumentException("how did we get here?");

    }

    /**
     * Mark a key as removed locally. If syncImmediately is true (the defualt) then immediately mark the key
     * as removed on the server as well. `localVer` must match the most recent version in the local database.
     */
    public boolean delete(String key, long localVersion) {
        Log.e(TAG, "kv deleted with key: " + key);
        database.delete(PlatformSqliteHelper.KV_STORE_TABLE_NAME, PlatformSqliteHelper.KV_KEY
                + " = " + key, null);
        return false;
    }

    private boolean _delete(String key, long localVersion) {
        return false;
    }

    /**
     * Gets the local version of the provided key, or 0 if it doesn't exist
     */
    private long localVersion(String key) {
        return 0;
    }

    private long _localVersion(String key) {
        return 0;
    }

    /**
     * generate a nonce using microseconds-since-epoch
     */
    private byte[] genNonce() {
        return null;
    }


    /**
     * encrypt some data using self.key
     */
    private byte[] encrypt(byte[] data) {
        return null;
    }

    /**
     * decrypt some data using self.key
     */
    private String decrypt(byte[] data) {
        return null;
    }

    /**
     * validates the key. keys can not start with a _
     */
    public boolean isKeyValid(String key) {
        Pattern pattern = Pattern.compile(KEY_REGEX);
        Matcher matcher = pattern.matcher(key);
        if (matcher.find()) {
            Log.e(TAG, "checkKey: found illegal patterns");
            return true;
        }
        return false;
    }
}