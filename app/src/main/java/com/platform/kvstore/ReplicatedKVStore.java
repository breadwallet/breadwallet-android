package com.platform.kvstore;

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
import android.widget.ListView;

import com.platform.interfaces.KVStoreAdaptor;
import com.platform.sqlite.KVEntity;
import com.platform.sqlite.PlatformSqliteHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.R.attr.data;
import static android.R.attr.key;
import static android.R.attr.version;


public class ReplicatedKVStore {
    private static final String TAG = ReplicatedKVStore.class.getName();

    private static final String KEY_REGEX = "^[^_][\\w-]{1,255}$";

    public boolean syncImmediately = true;
    public boolean encrypted = false;
    private boolean syncRunning = false;
    private KVStoreAdaptor remoteKvStore;
//    private Context context;

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

    public ReplicatedKVStore(Context context, KVStoreAdaptor remoteKvStore) {
//        this.context = context;
        this.remoteKvStore = remoteKvStore;
        dbHelper = new PlatformSqliteHelper(context);
    }

    public void open() throws SQLException {
        if (database == null)
            database = dbHelper.getWritableDatabase();
    }

//    public void close() {
//        dbHelper.close();
//    }

    /**
     * Set the value of a key locally in the database. If syncImmediately is true (the default) then immediately
     * after successfully saving locally, replicate to server. The `localVer` key must be the same as is currently
     * stored in the database. To create a new key, pass `0` as `localVer`
     */
    public CompletionObject set(long version, long remoteVersion, String key, byte[] value, long time, int deleted) {
        KVEntity entity = new KVEntity(version, remoteVersion, key, value, time, deleted);
        return set(entity);
    }

    public void set(List<KVEntity> kvs) {
        for (KVEntity kv : kvs)
            set(kv);
    }

    public CompletionObject set(KVEntity kv) {
        open();
        try {
            if (isKeyValid(kv.getKey())) {
                CompletionObject obj = new CompletionObject(0, 0, CompletionObject.RemoteKVStoreError.unknown);
                database.beginTransaction();
                try {
                    obj = _set(kv);
                    database.setTransactionSuccessful();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    database.endTransaction();
                }
                if (syncImmediately && obj.err == null) {
                    if (!syncRunning) {
                        syncRunning = true;
                        syncKey(kv.getKey(), kv.getRemoteVersion(), kv.getTime());
                        Log.e(TAG, "set: key synced: " + kv.getKey());
                    }
                }
                return obj;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new CompletionObject(0, 0, CompletionObject.RemoteKVStoreError.unknown);
    }

    public void set(KVEntity[] kvEntities) {
        for (KVEntity kv : kvEntities) {
            set(kv);
        }
    }

    private CompletionObject _set(KVEntity kv) throws Exception {
        long curVer = kv.getVersion();
        long newVer = 0;
        String key = kv.getKey();

        long localVer = _localVersion(key);
        if (curVer != localVer) {
            Log.e(TAG, String.format("set key %s conflict: version %d != current version %d", key, localVer, curVer));
            return new CompletionObject(0, 0, CompletionObject.RemoteKVStoreError.conflict);
        }
        newVer = curVer + 1;
        byte[] encryptionData = encrypted ? encrypt(kv.getValue()) : kv.getValue();

        boolean success = insertOrUpdate(new KVEntity(newVer, curVer, key, encryptionData, kv.getTime(), kv.getDeleted()));
        return new CompletionObject(newVer, kv.getTime(), success ? null : CompletionObject.RemoteKVStoreError.unknown);
    }

    private boolean insertOrUpdate(KVEntity kv) {

        try {
            ContentValues values = new ContentValues();
            values.put(PlatformSqliteHelper.KV_VERSION, kv.getVersion());
            values.put(PlatformSqliteHelper.KV_REMOTE_VERSION, kv.getRemoteVersion());
            values.put(PlatformSqliteHelper.KV_KEY, kv.getKey());
            values.put(PlatformSqliteHelper.KV_VALUE, kv.getValue());
            values.put(PlatformSqliteHelper.KV_TIME, kv.getTime());
            values.put(PlatformSqliteHelper.KV_DELETED, kv.getDeleted());

            int id = (int) database.insertWithOnConflict(PlatformSqliteHelper.KV_STORE_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            if (id == -1) {
                id = database.update(PlatformSqliteHelper.KV_STORE_TABLE_NAME, values, "key=?", new String[]{kv.getKey()});
            }
            if (id >= 0)
                return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;

        }
        return false;
    }

    /**
     * get kv by key and version (version can be 0)
     */
    public KVEntity get(String key, long version) {
        KVEntity kv = null;
        Cursor cursor = null;
        long curVer = 0;
        open();
        try {
            database.beginTransaction();
            try {
                //if no version, fine the version
                if (version == 0) {
                    curVer = localVersion(key);
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
                database.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                database.endTransaction();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return kv;
    }

    /**
     * Gets the local version of the provided key, or 0 if it doesn't exist
     */

    public long localVersion(String key) {
        if (isKeyValid(key)) {
            return _localVersion(key);
        } else {
            Log.e(TAG, "Key is invalid: " + key);
        }
        return 0;
    }

    private long _localVersion(String key) {
        long version = 0;
        Cursor cursor = null;
        List<KVEntity> test = getAllKVs();
        open();
        try {
            String selectQuery = "SELECT " + PlatformSqliteHelper.KV_VERSION + ", " + PlatformSqliteHelper.KV_TIME + " FROM " + PlatformSqliteHelper.KV_STORE_TABLE_NAME + " WHERE key = ? ORDER BY version DESC LIMIT 1";
            cursor = database.rawQuery(selectQuery, new String[]{key});
            cursor.moveToNext();
            version = cursor.getLong(0);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return version;
    }

    public void deleteAllKVs() {
        Log.e(TAG, "deleteKVs");
        open();
        try {
            database.delete(PlatformSqliteHelper.KV_STORE_TABLE_NAME, PlatformSqliteHelper.KV_TIME + " <> -1", null);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public List<KVEntity> getAllKVs() {
        List<KVEntity> kvs = new ArrayList<>();
        Cursor cursor = null;
        open();
        try {
            cursor = database.query(PlatformSqliteHelper.KV_STORE_TABLE_NAME,
                    allColumns, null, null, null, null, null);
            Log.e(TAG, "getAllKVs: cursor size: " + cursor.getCount());
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                KVEntity kvEntity = cursorToKv(cursor);
                kvs.add(kvEntity);
                cursor.moveToNext();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        Log.e(TAG, "kvs: " + kvs.size());

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
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            remoteVersion = cursor.getLong(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            key = cursor.getString(2);

        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            value = cursor.getBlob(3);

        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            time = cursor.getLong(4);

        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            deleted = cursor.getInt(5);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new KVEntity(version, remoteVersion, key, value, time, deleted);
    }

    /**
     * Sync an individual key. Normally this is only called internally and you should call syncAllKeys
     */
    public void syncKey(String key, long remoteVersion, long remoteTime) {
        if (syncRunning) return;
        syncRunning = true;
        try {
            if (remoteVersion == 0 || remoteTime == 0) {
                CompletionObject completionObject = remoteKvStore.ver(key);
                Log.e(TAG, String.format("syncKey: completionObject: version: %d, value length: %d, err: %s, time: %d",
                        completionObject.version, completionObject.value.length, completionObject.err, completionObject.time));
                _syncKey(key, completionObject.version, completionObject.time, completionObject.err);
            } else {
                _syncKey(key, remoteVersion, remoteTime, null);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            syncRunning = false;
        }
    }

    /**
     * the syncKey kernel - this is provided so syncAllKeys can provide get a bunch of key versions at once
     * and fan out the _syncKey operations
     */
    private boolean _syncKey(String key, long remoteVersion, long remoteTime, CompletionObject.RemoteKVStoreError err) {
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

        // one optimization is we keep the remote version on the most recent local version, if they match,
        // there is nothing to do

        long recorderRemoteVersion = localVersion(key);
        if (remoteVersion > 0 && recorderRemoteVersion == remoteVersion) {
            Log.e(TAG, "_syncKey: " + String.format("Remote version of key: %s is the same as the one we have", key));
            return false;
        }

        KVEntity localKv = get(key, 0);
        if (localKv == null)
            localKv = new KVEntity(0, 0, key, new byte[0], System.currentTimeMillis(), 0);
        byte[] localValue = localKv.getValue();

        if (encrypted) localValue = encrypt(localValue);
        if (err == null || err == CompletionObject.RemoteKVStoreError.notFound || err == CompletionObject.RemoteKVStoreError.tombstone) {

            if (localKv.getDeleted() > 0 && err == CompletionObject.RemoteKVStoreError.notFound) {
                // was removed on both server and locally
                Log.e(TAG, String.format("Local key %s was deleted, and so was the remote key", key));
                return setRemoteVersion(key, localKv.getVersion(), localKv.getRemoteVersion()).err == null;
            }
            if (localKv.getTime() >= remoteTime) {// local is newer (or a tiebreaker)
                if (localKv.getDeleted() > 0) {
                    Log.e(TAG, String.format("Local key %s was deleted, removing remotely...", key));
                    CompletionObject obj = remoteKvStore.del(key, remoteVersion);
                    if (obj.err == CompletionObject.RemoteKVStoreError.notFound) {
                        Log.e(TAG, String.format("Local key %s was already missing on the server. Ignoring", key));
                        return true;
                    }
                    if (obj.err != null) {
                        Log.e(TAG, String.format("Error deleting remote version for key %s, error: %s", key, err));
                        return false;
                    }

                    boolean success = setRemoteVersion(key, localKv.getVersion(), obj.version).err == null;
                    if (!success) return false;
                } else {
                    Log.e(TAG, String.format("Local key %s is newer, updating remotely...", key));
                    CompletionObject obj = remoteKvStore.put(key, localValue, remoteVersion);
                    if (obj.err != null) {
                        Log.e(TAG, String.format("Error updating remote version for key %s, error: %s", key, err));
                        return false;
                    }

                    boolean success = setRemoteVersion(key, localKv.getVersion(), remoteVersion).err == null;
                    if (!success) return false;
                }
            } else {
                // local is out-of-date
                if (err == CompletionObject.RemoteKVStoreError.tombstone) {
                    // remote is deleted
                    Log.e(TAG, String.format("Remote key %s deleted, removing locally", key));
                    CompletionObject obj = new CompletionObject(0, 0, CompletionObject.RemoteKVStoreError.unknown);
                    database.beginTransaction();
                    try {
                        obj = _delete(key, localKv.getVersion());
                        database.setTransactionSuccessful();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        database.endTransaction();
                    }
                    if (obj.version != 0) {
                        boolean success = setRemoteVersion(key, obj.version, remoteVersion).err == null;
                        if (!success) return false;
                        Log.e(TAG, String.format("Remote key %s was removed locally", key));

                    }
                } else {
                    Log.e(TAG, String.format("Remote key %s is newer, fetching...", key));
                    CompletionObject getObj = remoteKvStore.get(key, remoteVersion);
                    if (getObj.err != null) {
                        Log.e(TAG, String.format("Error fetching the remote value for key %s, error: %s", key, err));
                        return false;
                    }
                    byte[] decryptedValue = encrypted ? decrypt(getObj.value) : getObj.value;
                    CompletionObject setObj = new CompletionObject(0, 0, CompletionObject.RemoteKVStoreError.unknown);
                    database.beginTransaction();
                    try {
                        setObj = _set(new KVEntity(localKv.getVersion(), getObj.version, key, decryptedValue, getObj.time, localKv.getDeleted()));
                        database.setTransactionSuccessful();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        database.endTransaction();
                    }
                    if (setObj.err == null) {
                        boolean success = setRemoteVersion(key, setObj.version, getObj.version).err == null;
                        if (!success) return false;
                    }
                }
            }

        } else {
            Log.e(TAG, String.format("Error fetching remote version for key %s, error: %s", key, err));
            return false;
        }
        return false;
    }


    /**
     * Sync all keys to and from the remote kv store adaptor
     */
    private boolean syncAllKeys() {
        // update all keys locally and on the remote server, replacing missing keys
        //
        // 1. get a list of all keys from the server
        // 2. for keys that we don't have, add em
        // 3. for keys that we do have, sync em
        // 4. for keys that they don't have that we do, upload em
        if (syncRunning) return false;
        syncRunning = true;
        long startTime = System.currentTimeMillis();
        try {
            CompletionObject obj = remoteKvStore.keys();
            if (obj.err != null || obj.keys == null) {
                Log.e(TAG, String.format("Error fetching remote key data: %s", obj.err));
                syncRunning = false;
                return false;
            }
            List<KVEntity> localKvs;
            List<KVEntity> remoteKVs = obj.keys;

            try {
                localKvs = getAllKVs();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, String.format("Error getting local key data: %s", e.getMessage()));
                syncRunning = false;
                return false;
            }
            List<KVEntity> allKvs = localKvs;

            List<String> remoteKeys = getKeysFromKVEntity(remoteKVs);
            for (KVEntity kv : localKvs) {
                if (!remoteKeys.contains(kv.getKey())) // server is missing a key that we have
                    allKvs.add(new KVEntity(0, 0, kv.getKey(), null, System.currentTimeMillis(), 0));
            }
            Log.e(TAG, String.format("Syncing %d keys", allKvs.size()));
            int failures = 0;
            for (KVEntity k : allKvs) {
                boolean success = _syncKey(k.getKey(), k.getRemoteVersion(), k.getTime(), k.getErr());
                if (!success) failures++;
            }
            Log.e(TAG, String.format("Finished syncing in %d)", (System.currentTimeMillis() - startTime)));
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            syncRunning = false;
        }
        return false;
    }

    private List<String> getKeysFromKVEntity(List<KVEntity> entities) {
        List<String> keys = new ArrayList<>();
        for (KVEntity kv : entities)
            keys.add(kv.getKey());
        return keys;
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
        long version = 0;
        Cursor cursor = null;
        if (isKeyValid(key)) {
            try {
                String selectQuery = "SELECT " + PlatformSqliteHelper.KV_REMOTE_VERSION + " FROM " + PlatformSqliteHelper.KV_STORE_TABLE_NAME + " WHERE key = ? ORDER BY version DESC LIMIT 1";
                cursor = database.rawQuery(selectQuery, new String[]{key});
                version = cursor.getLong(0);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        } else {
            Log.e(TAG, "Key is invalid: " + key);
        }
        return version;
    }

    /**
     * Record the remote version for the object in a new version of the local key
     */
    private CompletionObject setRemoteVersion(String key, long localVer, long remoteVer) {
        if (localVer < 1)
            return new CompletionObject(0, 0, CompletionObject.RemoteKVStoreError.conflict);
        if (isKeyValid(key)) {
            Cursor cursor = null;
            open();
            try {
                database.beginTransaction();
                try {
                    long newVer = 0;
                    long time = System.currentTimeMillis();
                    long curVer = localVersion(key);

                    if (curVer != localVer) {
                        Log.e(TAG, String.format("set remote version key %s conflict: version %d != current version %d", key, localVer, curVer));
                        return new CompletionObject(0, 0, CompletionObject.RemoteKVStoreError.conflict);
                    }
                    Log.e(TAG, String.format("SET REMOTE VERSION: %s ver: %d remoteVer: %d", key, localVer, curVer));
                    newVer = curVer + 1;
                    cursor = database.query(PlatformSqliteHelper.KV_STORE_TABLE_NAME,
                            new String[]{PlatformSqliteHelper.KV_KEY, PlatformSqliteHelper.KV_VALUE, PlatformSqliteHelper.KV_DELETED},
                            "key = ? AND version = ?", new String[]{key, String.valueOf(curVer)},
                            null, null, "version DESC", "1");

                    String cursorKey = cursor.getString(0);
                    byte[] cursorVal = cursor.getBlob(1);
                    int cursorDeleted = cursor.getInt(2);

                    boolean success = insertOrUpdate(new KVEntity(newVer, remoteVer, cursorKey, cursorVal, time, cursorDeleted));
                    if (success) {
                        database.setTransactionSuccessful();
                        return new CompletionObject(newVer, time, null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    database.endTransaction();
                }
            } catch (Exception e) {
                e.printStackTrace();

            }
        } else {
            Log.e(TAG, "Key is invalid: " + key);
        }
        return new CompletionObject(0, 0, CompletionObject.RemoteKVStoreError.unknown);
    }

    public boolean delete(String key) {
        return delete(key, 0);
    }

    /**
     * Mark a key as removed locally. If syncImmediately is true (the defualt) then immediately mark the key
     * as removed on the server as well. `localVer` must match the most recent version in the local database.
     */
    public boolean delete(String key, long localVersion) {
        boolean success = false;
        open();
        try {
            Log.e(TAG, "kv deleted with key: " + key);
            if (isKeyValid(key)) {
                CompletionObject obj = new CompletionObject(0, 0, CompletionObject.RemoteKVStoreError.unknown);
                database.beginTransaction();
                try {
                    obj = _delete(key, localVersion);
                    database.setTransactionSuccessful();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    database.endTransaction();
                }
                if (syncImmediately && obj.err == null) {
                    if (!syncRunning) {
                        syncRunning = true;
                        syncKey(key, 0, 0);
                        Log.e(TAG, "set: key synced: " + key);
                    }
                }
                success = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return success;
    }

    private CompletionObject _delete(String key, long localVersion) throws Exception {
        if (localVersion == 0)
            return new CompletionObject(0, 0, CompletionObject.RemoteKVStoreError.notFound);
        long newVer = 0;
        Cursor cursor = null;
        try {
            long curVer = _localVersion(key);
            if (curVer != localVersion) {
                Log.e(TAG, String.format("del key %s conflict: version %d != current version %d", key, localVersion, curVer));
                return new CompletionObject(0, 0, CompletionObject.RemoteKVStoreError.conflict);
            }
            Log.e(TAG, String.format("DEL key: %s ver: %d", key, curVer));
            newVer = curVer + 1;
            cursor = database.query(PlatformSqliteHelper.KV_STORE_TABLE_NAME,
                    new String[]{PlatformSqliteHelper.KV_VALUE}, "key = ? AND version = ?", new String[]{key, String.valueOf(version)},
                    null, null, "version DESC", "1");
            byte[] value = cursor.getBlob(0);
            long time = System.currentTimeMillis();
            ContentValues values = new ContentValues();
            values.put(PlatformSqliteHelper.KV_VERSION, newVer);
            values.put(PlatformSqliteHelper.KV_KEY, key);
            values.put(PlatformSqliteHelper.KV_VALUE, value);
            values.put(PlatformSqliteHelper.KV_TIME, time);
            values.put(PlatformSqliteHelper.KV_DELETED, 1);
            database.insert(PlatformSqliteHelper.KV_STORE_TABLE_NAME, null, values);
            return new CompletionObject(newVer, time, null);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
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
        return data;
    }

    /**
     * decrypt some data using self.key
     */
    private byte[] decrypt(byte[] data) {
        return data;
    }

    /**
     * validates the key. keys can not start with a _
     */
    private boolean isKeyValid(String key) {
        Pattern pattern = Pattern.compile(KEY_REGEX);
        Matcher matcher = pattern.matcher(key);
        if (matcher.find()) {
            Log.e(TAG, "checkKey: found illegal patterns");
            return true;
        }
        return false;
    }
}