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

import com.breadwallet.BreadApp;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.threads.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.jniwrappers.BRKey;
import com.platform.interfaces.KVStoreAdaptor;
import com.platform.sqlite.KVItem;
import com.platform.sqlite.PlatformSqliteHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

import static com.platform.sqlite.PlatformSqliteHelper.KV_STORE_TABLE_NAME;

public class ReplicatedKVStore {

    private SQLiteDatabase mDatabase;

    private static final String KEY_REGEX = "^[^_][\\w-]{1,255}$";

    public final boolean encrypted = true;
    public final boolean encryptedReplication = true;
    //    private Lock dbLock = new ReentrantLock();
    public boolean syncImmediately = false;
    private boolean syncRunning = false;
    private KVStoreAdaptor remoteKvStore;
    private static Context mContext;
    private static byte[] tempAuthKey;
    // Database fields
//    private SQLiteDatabase readDb;
//    private SQLiteDatabase writeDb;
    private final PlatformSqliteHelper dbHelper;
    private final String[] allColumns = {
            PlatformSqliteHelper.KV_VERSION,
            PlatformSqliteHelper.KV_REMOTE_VERSION,
            PlatformSqliteHelper.KV_KEY,
            PlatformSqliteHelper.KV_VALUE,
            PlatformSqliteHelper.KV_TIME,
            PlatformSqliteHelper.KV_DELETED
    };

    private static ReplicatedKVStore instance;

    public static ReplicatedKVStore getInstance(Context context, KVStoreAdaptor remoteKvStore) {
        if (instance == null) {
            instance = new ReplicatedKVStore(context, remoteKvStore);
        }
        return instance;
    }

    private ReplicatedKVStore(Context context, KVStoreAdaptor remoteKvStore) {
        mContext = context;
        this.remoteKvStore = remoteKvStore;
        dbHelper = PlatformSqliteHelper.getInstance(context);
    }

    public SQLiteDatabase getWritable() {
//        if (mOpenCounter.incrementAndGet() == 1) {
        // Opening new database
//        if (ActivityUTILS.isMainThread()) throw new NetworkOnMainThreadException();
        if (mDatabase == null || !mDatabase.isOpen())
            mDatabase = dbHelper.getWritableDatabase();
        dbHelper.setWriteAheadLoggingEnabled(BRConstants.WAL);
//        }
        return mDatabase;
    }

    public SQLiteDatabase getReadable() {
        return getWritable();
    }

    public void closeDB() {
//        mDatabase.close();
//        if (mOpenCounter.decrementAndGet() == 0) {
        // Closing database
//            mDatabase.close();

//        }
//        Log.d(TAG, "closeDB open counter: " + String.valueOf(mOpenCounter.get()));
    }

    /**
     * Set the value of a key locally in the database. If syncImmediately is true (the default) then immediately
     * after successfully saving locally, replicate to server. The `localVer` key must be the same as is currently
     * stored in the database. To create a new key, pass `0` as `localVer`
     */
    public CompletionObject set(long version, long remoteVersion, String key, byte[] value, long time, int deleted) {
        KVItem entity = new KVItem(version, remoteVersion, key, value, time, deleted);
        return set(entity);
    }

    public void set(List<KVItem> kvs) {
        for (KVItem kv : kvs)
            set(kv);
    }

    public CompletionObject set(KVItem kv) {
        try {
            if (isKeyValid(kv.key)) {
                CompletionObject obj = new CompletionObject(CompletionObject.RemoteKVStoreError.unknown);

                try {
                    obj = _set(kv);
                } catch (Exception e) {
                    Timber.e(e);
                }
                if (syncImmediately && obj.err == null) {
                    if (!syncRunning) {
                        syncKey(kv.key, 0, 0, null);
                        Timber.d("set: key synced: " + kv.key);
                    }
                }
                return obj;
            }
        } catch (SQLException e) {
            Timber.e(e);
        }
        return new CompletionObject(CompletionObject.RemoteKVStoreError.unknown);
    }

    public void set(KVItem[] kvEntities) {
        for (KVItem kv : kvEntities) {
            set(kv);
        }
    }

    private synchronized CompletionObject _set(KVItem kv) throws Exception {
        Timber.d("_set: " + kv.key);
        long localVer = kv.version;
        long newVer = 0;
        long time = System.currentTimeMillis();
        String key = kv.key;

        long curVer = _localVersion(key).version;
        if (curVer != localVer) {
            Timber.d("set key %s conflict: version %d != current version %d", key, localVer, curVer);
            return new CompletionObject(CompletionObject.RemoteKVStoreError.conflict);
        }
        newVer = curVer + 1;
        byte[] encryptionData = encrypted ? encrypt(kv.value, mContext) : kv.value;
        SQLiteDatabase db = getWritable();
        try {
            db.beginTransaction();
            boolean success = insert(new KVItem(newVer, -1, key, encryptionData, time, kv.deleted));
            if (!success) return new CompletionObject(CompletionObject.RemoteKVStoreError.unknown);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Timber.e(e, "_set: ");
        } finally {
            db.endTransaction();
            closeDB();
        }
        return new CompletionObject(newVer, time, null);
    }

    private boolean insert(KVItem kv) {
        try {
            SQLiteDatabase db = getWritable();
            ContentValues values = new ContentValues();
            if (kv.version != -1)
                values.put(PlatformSqliteHelper.KV_VERSION, kv.version);
            if (kv.remoteVersion != -1)
                values.put(PlatformSqliteHelper.KV_REMOTE_VERSION, kv.remoteVersion);
            values.put(PlatformSqliteHelper.KV_KEY, kv.key);
            values.put(PlatformSqliteHelper.KV_VALUE, kv.value);
            values.put(PlatformSqliteHelper.KV_TIME, kv.time);
            values.put(PlatformSqliteHelper.KV_DELETED, kv.deleted);
            long n = db.insertWithOnConflict(KV_STORE_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            if (n == -1) {
                //try updating if inserting failed
                n = db.updateWithOnConflict(KV_STORE_TABLE_NAME, values, "key=?", new String[]{kv.key}, SQLiteDatabase.CONFLICT_REPLACE);
            }
            return n != -1;
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
    }

    /**
     * get kv by key and version (version can be 0)
     */
    public CompletionObject get(String key, long version) {
        KVItem kv = null;
        Cursor cursor = null;
        long curVer = 0;

        try {
            //if no version, fine the version
            SQLiteDatabase db = getReadable();
            if (version == 0) {
                curVer = _localVersion(key).version;
            } else {
                //if we have a version, check if it's correct
                cursor = db.query(KV_STORE_TABLE_NAME,
                        allColumns, "key = ? AND version = ?", new String[]{key, String.valueOf(version)},
                        null, null, "version DESC", "1");
                if (cursor.moveToNext())
                    curVer = cursor.getLong(0);
                else
                    curVer = 0;
            }

            //if still 0 then version is non-existent or wrong.
            if (curVer == 0) {
                return new CompletionObject(CompletionObject.RemoteKVStoreError.notFound);
            }
            if (cursor != null) cursor.close();
            cursor = db.query(KV_STORE_TABLE_NAME,
                    allColumns, "key = ? AND version = ?", new String[]{key, String.valueOf(curVer)},
                    null, null, "version DESC", "1");
            if (cursor.moveToNext()) {
                kv = cursorToKv(cursor);
            }
            if (kv != null) {
                byte[] val = kv.value;
                kv.value = encrypted ? decrypt(val, mContext) : val;
                if (val != null && Utils.isNullOrEmpty(kv.value)) {
                    //decrypting failed
                    Timber.d("get: Decrypting failed for key: " + key + ", deleting the kv");
                    delete(key, curVer);
                }
            }
        } catch (Exception e) {
            Timber.e(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return kv == null ? new CompletionObject(CompletionObject.RemoteKVStoreError.notFound) : new CompletionObject(kv, null);
    }

    /**
     * Gets the local version of the provided key, or 0 if it doesn't exist
     */

    public CompletionObject localVersion(String key) {
        if (isKeyValid(key)) {
            return _localVersion(key);
        } else {
            Timber.d("Key is invalid: %s", key);
        }
        return new CompletionObject(CompletionObject.RemoteKVStoreError.notFound);
    }

    private synchronized CompletionObject _localVersion(String key) {
        long version = 0;
        long time = System.currentTimeMillis();
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getReadable();
            String selectQuery = "SELECT " + PlatformSqliteHelper.KV_VERSION + ", " + PlatformSqliteHelper.KV_TIME + " FROM " + KV_STORE_TABLE_NAME + " WHERE key = ? ORDER BY version DESC LIMIT 1";
            cursor = db.rawQuery(selectQuery, new String[]{key});
            if (cursor.moveToNext()) {
                version = cursor.getLong(0);
                time = cursor.getLong(1);
            }
        } catch (Exception e) {
            Timber.e(e);
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return new CompletionObject(version, time, null);
    }

    public synchronized void deleteAllKVs() {
        try {
            SQLiteDatabase db = getWritable();
            db.delete(KV_STORE_TABLE_NAME, null, null);
        } catch (SQLException e) {
            Timber.e(e);
        } finally {
            closeDB();
        }
    }

    public List<KVItem> getRawKVs() {
        List<KVItem> kvs = new ArrayList<>();
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getReadable();
            String selectQuery = "SELECT kvs.version, kvs.remote_version, kvs.key, kvs.value, kvs.thetime, kvs.deleted FROM " + PlatformSqliteHelper.KV_STORE_TABLE_NAME + " kvs " +
                    "INNER JOIN ( " +
                    "   SELECT MAX(version) AS latest_version, key " +
                    "   FROM " + PlatformSqliteHelper.KV_STORE_TABLE_NAME +
                    "   GROUP BY " + PlatformSqliteHelper.KV_KEY +
                    " ) vermax " +
                    "ON kvs.version = vermax.latest_version " +
                    "AND kvs.key = vermax.key";

            cursor = db.rawQuery(selectQuery, null);

            while (cursor.moveToNext()) {
                KVItem kvItem = cursorToKv(cursor);
                if (kvItem != null)
                    kvs.add(kvItem);
            }
        } catch (Exception e) {
            Timber.e(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return kvs;
    }

    public List<KVItem> getAllTxMdKv() {
        List<KVItem> kvs = new ArrayList<>();
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getReadable();
            String selectQuery = "SELECT kvs.version, kvs.remote_version, kvs.key, kvs.value, kvs.thetime, kvs.deleted FROM kvStoreTable kvs " +
                    "INNER JOIN ( SELECT MAX(version) AS latest_version, key FROM kvStoreTable where key like 'txn2-%' GROUP BY key ) vermax ON kvs.version = vermax.latest_version AND kvs.key = vermax.key";

            cursor = db.rawQuery(selectQuery, null);
            while (cursor.moveToNext()) {
                KVItem kvItem = cursorToKv(cursor);
                if (kvItem != null) {
                    byte[] val = kvItem.value;
                    kvItem.value = encrypted ? decrypt(val, mContext) : val;
                    kvs.add(kvItem);
                }
            }
        } catch (Exception e) {
            Timber.e(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return kvs;
    }

    private KVItem cursorToKv(Cursor cursor) {
        long version = 0;
        long remoteVersion = 0;
        String key = null;
        byte[] value = null;
        long time = 0;
        int deleted = 0;
        try {
            version = cursor.getLong(0);
        } catch (Exception e) {
            Timber.e(e);
        }
        try {
            remoteVersion = cursor.getLong(1);
        } catch (Exception e) {
            Timber.e(e);
        }
        try {
            key = cursor.getString(2);
        } catch (Exception e) {
            Timber.e(e);
        }
        if (Utils.isNullOrEmpty(key)) return null;
        try {
            value = cursor.getBlob(3);

        } catch (Exception e) {
            Timber.e(e);
        }
        try {
            time = cursor.getLong(4);

        } catch (Exception e) {
            Timber.e(e);
        }
        try {
            deleted = cursor.getInt(5);
        } catch (Exception e) {
            Timber.e(e);
        }
        return new KVItem(version, remoteVersion, key, value, time, deleted);
    }

    /**
     * Sync an individual key. Normally this is only called internally and you should call syncAllKeys
     */
    public void syncKey(final String key, final long remoteVersion, final long remoteTime, final CompletionObject.RemoteKVStoreError err) {
        if (syncRunning) return;
        syncRunning = true;

        try {
            if (remoteVersion == 0 || remoteTime == 0) {
                final CompletionObject completionObject = remoteKvStore.ver(key);
                Timber.d("syncKey: completionObject: version: %d, value: %s, err: %s, time: %d",
                        completionObject.version, Arrays.toString(completionObject.value), completionObject.err, completionObject.time);
                _syncKey(key, completionObject.version, completionObject.time, completionObject.err);
            } else {
                _syncKey(key, remoteVersion, remoteTime, err);
            }
        } catch (Exception ex) {
            Timber.e(ex);
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

        long recorderRemoteVersion = remoteVersion(key);
        if (err != CompletionObject.RemoteKVStoreError.notFound && remoteVersion > 0 && recorderRemoteVersion == remoteVersion) {
            return true;
        }

        CompletionObject completionObject = get(key, 0);
        KVItem localKv = completionObject.kv;
        byte[] locVal = new byte[0];
        if (completionObject.err == CompletionObject.RemoteKVStoreError.notFound) {
            localKv = new KVItem(0, 0, key, locVal, 0, 0);
        }

        if (completionObject.err == null) {
            locVal = localKv.value;
            localKv.value = encryptedReplication ? encrypt(locVal, mContext) : locVal;
        }

        if (err == null || err == CompletionObject.RemoteKVStoreError.notFound || err == CompletionObject.RemoteKVStoreError.tombstone) {

            if (localKv.deleted > 0 && err == CompletionObject.RemoteKVStoreError.tombstone) {
                // was removed on both server and locally
                Timber.i("Local key %s was deleted, and so was the remote key", key);
                return setRemoteVersion(key, localKv.version, localKv.remoteVersion).err == null;
            }
            if (localKv.time >= remoteTime) {// local is newer (or a tiebreaker)
                if (localKv.deleted > 0) {
                    Timber.i("Local key %s was deleted, removing remotely...", key);
                    CompletionObject obj = remoteKvStore.del(key, remoteVersion);
                    if (obj.err == CompletionObject.RemoteKVStoreError.notFound) {
                        Timber.i("Local key %s was already missing on the server. Ignoring", key);
                        return true;
                    }
                    if (obj.err != null) {
                        Timber.d("Error deleting remote version for key %s, error: %s", key, err);
                        return false;
                    }

                    boolean success = setRemoteVersion(key, localKv.version, obj.version).err == null;
                    if (!success) return false;
                } else {
                    Timber.i("Local key %s is newer, updating remotely...", key);
                    // if the remote version is zero it means it doesnt yet exist on the server. set the remote version
                    // to "1" to create the key on the server
                    long useRemoteVer = (remoteVersion == 0 || remoteVersion < recorderRemoteVersion) ? 1 : remoteVersion;
                    byte[] val = localKv.value;
                    if (Utils.isNullOrEmpty(val)) {
                        Timber.d("_syncKey: encrypting value before sending to remote failed");
                        return false;
                    }
                    CompletionObject obj = remoteKvStore.put(key, val, useRemoteVer);

                    if (obj.err != null) {
                        Timber.d("Error updating remote version for key %s, error: %s", key, err);
                        return false;
                    }

                    boolean success = setRemoteVersion(key, localKv.version, obj.version).err == null;
                    Timber.i("Local key %s updated on server", key);
                    if (!success) return false;
                }
            } else {
                // local is out-of-date
                if (err == CompletionObject.RemoteKVStoreError.tombstone) {
                    // remote is deleted
                    Timber.i("Remote key %s deleted, removing locally", key);
                    CompletionObject obj = new CompletionObject(CompletionObject.RemoteKVStoreError.unknown);
                    try {
                        obj = _delete(key, localKv.version);
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                    if (obj.version != 0) {
                        boolean success = setRemoteVersion(key, obj.version, remoteVersion).err == null;
                        if (!success) return false;
                        Timber.i("Remote key %s was removed locally", key);
                    }
                } else {
                    Timber.i("Remote key %s is newer, fetching...", key);
                    CompletionObject remoteGet = remoteKvStore.get(key, remoteVersion);

                    if (remoteGet.err != null) {
                        Timber.d("Error fetching the remote value for key %s, error: %s", key, err);
                        return false;
                    }

                    byte[] val = remoteGet.value;
                    if (Utils.isNullOrEmpty(val)) {
                        Timber.d("_syncKey: key: " + key + " ,from the remote, is empty");
                        return false;
                    }
                    byte[] decryptedValue = encryptedReplication ? decrypt(val, mContext) : val;
                    if (Utils.isNullOrEmpty(decryptedValue)) {
                        Timber.d("_syncKey: failed to decrypt the value from remote for key: %s", key);
                        return false;
                    }

                    CompletionObject setObj = new CompletionObject(CompletionObject.RemoteKVStoreError.unknown);
                    try {
                        setObj = _set(new KVItem(localKv.version, remoteGet.version, key, decryptedValue, remoteGet.time, localKv.deleted));
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                    if (setObj.err == null) {
                        boolean success = setRemoteVersion(key, setObj.version, remoteGet.version).err == null;
                        if (!success) return false;
                    }
                }
            }
        } else {
            Timber.d("Error fetching remote version for key %s, error: %s", key, err);
            return false;
        }
        return true;
    }

    /**
     * Sync all kvs to and from the remote kv store adaptor
     */
    public boolean syncAllKeys() {
        // update all kvs locally and on the remote server, replacing missing kvs
        //
        // 1. get a list of all kvs from the server
        // 2. for kvs that we don't have, add em
        // 3. for kvs that we do have, sync em
        // 4. for kvs that they don't have that we do, upload em
        if (syncRunning) {
            Timber.d("syncAllKeys: already syncing");
            return false;
        }
        syncRunning = true;
        long startTime = System.currentTimeMillis();

        try {
            CompletionObject obj = remoteKvStore.keys();
            if (obj.err != null) {
                Timber.i("Error fetching remote key data: %s", obj.err);
                syncRunning = false;
                return false;
            }
            List<KVItem> localKvs = getRawKVs();
            List<KVItem> remoteKVs = obj.kvs;

            List<String> remoteKeys = getKeysFromKVEntity(remoteKVs);
            List<KVItem> allKvs = new ArrayList<>();
            allKvs.addAll(remoteKVs);

            for (KVItem kv : localKvs) {
                if (!remoteKeys.contains(kv.key)) // server is missing a key that we have
                    allKvs.add(new KVItem(0, 0, kv.key, null, 0, 0));
            }

            Timber.d("Syncing %d kvs", allKvs.size());
            int failures = 0;
            for (KVItem k : allKvs) {
                boolean success = _syncKey(k.key, k.remoteVersion == -1 ? k.version : k.remoteVersion, k.time, k.err);
                if (!success) failures++;
            }
            Timber.d("Finished syncing in %d, with failures: %d", System.currentTimeMillis() - startTime, failures);
            return true;
        } catch (Exception ex) {
            Timber.e(ex);
        } finally {
            syncRunning = false;
        }
        return false;
    }

    private List<String> getKeysFromKVEntity(List<KVItem> entities) {
        List<String> keys = new ArrayList<>();
        for (KVItem kv : entities)
            keys.add(kv.key);
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
    public long remoteVersion(String key) {
        long version = 0;
        Cursor cursor = null;
        try {
            if (isKeyValid(key)) {

                try {
                    SQLiteDatabase db = getReadable();
                    String selectQuery = "SELECT " + PlatformSqliteHelper.KV_REMOTE_VERSION + " FROM " + KV_STORE_TABLE_NAME + " WHERE key = ? ORDER BY version DESC LIMIT 1";
                    cursor = db.rawQuery(selectQuery, new String[]{key});
                    if (cursor.moveToNext())
                        version = cursor.getLong(0);
                    else
                        Timber.i("remoteVersion: cursor is null for: %s", selectQuery);
                } catch (Exception e) {
                    Timber.e(e);
                } finally {
                    if (cursor != null)
                        cursor.close();
                    closeDB();
                }
            } else {
                Timber.i("Key is invalid: %s", key);
            }
        } finally {
            if (cursor != null) cursor.close();
        }

        return version;
    }

    /**
     * Record the remote version for the object in a new version of the local key
     */
    public synchronized CompletionObject setRemoteVersion(String key, long localVer, long remoteVer) {
        if (localVer < 1)
            return new CompletionObject(CompletionObject.RemoteKVStoreError.conflict); // setRemoteVersion can't be used for creates
        if (isKeyValid(key)) {

            Cursor c = null;
            SQLiteDatabase db = getWritable();
            try {
                db.beginTransaction();
                long newVer = 0;
                long time = System.currentTimeMillis();

                long curVer = _localVersion(key).version;

                if (curVer != localVer) {
                    Timber.d("set remote version key %s conflict: version %d != current version %d", key, localVer, curVer);
                    return new CompletionObject(CompletionObject.RemoteKVStoreError.conflict);
                }

                newVer = curVer + 1;
                c = db.query(true, KV_STORE_TABLE_NAME, allColumns, PlatformSqliteHelper.KV_KEY + "=? and " + PlatformSqliteHelper.KV_VERSION + "=?", new String[]{key, String.valueOf(curVer)}, null, null, "version DESC", "1");
                KVItem tmp = null;
                if (c.moveToNext()) {
                    tmp = cursorToKv(c);
                }
                if (tmp == null)
                    return new CompletionObject(CompletionObject.RemoteKVStoreError.notFound);
                c.close();

                ContentValues values = new ContentValues();
                values.put(PlatformSqliteHelper.KV_VERSION, newVer);
                values.put(PlatformSqliteHelper.KV_REMOTE_VERSION, remoteVer);
                values.put(PlatformSqliteHelper.KV_KEY, key);
                values.put(PlatformSqliteHelper.KV_VALUE, tmp.value);
                values.put(PlatformSqliteHelper.KV_TIME, time);
                values.put(PlatformSqliteHelper.KV_DELETED, tmp.deleted);
                long id = db.insert(KV_STORE_TABLE_NAME, null, values);
                if (id == -1)
                    return new CompletionObject(CompletionObject.RemoteKVStoreError.unknown);

                db.setTransactionSuccessful();
                return new CompletionObject(newVer, time, null);
            } catch (Exception e) {
                Timber.e(e);
            } finally {
                db.endTransaction();
                if (c != null) c.close();
                closeDB();
            }
        } else {
            Timber.i("Key is invalid: %s", key);
        }
        return new CompletionObject(CompletionObject.RemoteKVStoreError.unknown);
    }

    /**
     * Mark a key as removed locally. If syncImmediately is true (the defualt) then immediately mark the key
     * as removed on the server as well. `localVer` must match the most recent version in the local database.
     */
    public CompletionObject delete(String key, long localVersion) {
        try {
            Timber.d("kv deleted with key: %s", key);
            if (isKeyValid(key)) {
                CompletionObject obj = new CompletionObject(CompletionObject.RemoteKVStoreError.unknown);
                try {
                    obj = _delete(key, localVersion);
                } catch (Exception e) {
                    Timber.e(e);
                }
                if (syncImmediately && obj.err == null) {
                    if (!syncRunning) {
                        syncKey(key, 0, 0, null);
                        Timber.d("set: key synced: %s", key);
                    }
                }
                return obj;
            }
        } catch (SQLException e) {
            Timber.e(e);
        }
        return new CompletionObject(CompletionObject.RemoteKVStoreError.unknown);
    }

    private synchronized CompletionObject _delete(String key, long localVersion) throws Exception {
        if (localVersion == 0)
            return new CompletionObject(CompletionObject.RemoteKVStoreError.notFound);
        long newVer = 0;
        long time = System.currentTimeMillis();
        Cursor cursor = null;
        try {
            long curVer = _localVersion(key).version;
            if (curVer != localVersion) {
                Timber.d("del key %s conflict: version %d != current version %d", key, localVersion, curVer);
                return new CompletionObject(CompletionObject.RemoteKVStoreError.conflict);
            }
            SQLiteDatabase db = getWritable();
            try {
                db.beginTransaction();
                Timber.d("DEL key: %s ver: %d", key, curVer);
                newVer = curVer + 1;
                cursor = db.query(KV_STORE_TABLE_NAME,
                        new String[]{PlatformSqliteHelper.KV_VALUE}, "key = ? AND version = ?", new String[]{key, String.valueOf(localVersion)},
                        null, null, "version DESC", "1");
                byte[] value = null;
                if (cursor.moveToNext()) {
                    value = cursor.getBlob(0);
                }

                if (Utils.isNullOrEmpty(value)) throw new NullPointerException("cannot be empty");
                KVItem kvToInsert = new KVItem(newVer, -1, key, value, time, 1);
                insert(kvToInsert);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
                closeDB();
            }
            return new CompletionObject(newVer, time, null);
        } catch (Exception e) {
            Timber.e(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return new CompletionObject(CompletionObject.RemoteKVStoreError.unknown);
    }

    /**
     * generate a nonce using microseconds-since-epoch
     */
    public static byte[] getNonce() {
        byte[] nonce = new byte[12];
        ByteBuffer buffer = ByteBuffer.allocate(8);
        long t = System.nanoTime() / 1000;
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(t);
        byte[] byteTime = buffer.array();
        System.arraycopy(byteTime, 0, nonce, 4, byteTime.length);
        return nonce;
    }

    /**
     * encrypt some data using self.key
     */
    public static byte[] encrypt(byte[] data, Context app) {
        if (data == null) {
            Timber.i("encrypt: data is null");
            return null;
        }
        if (app == null) app = BreadApp.getBreadContext();
        if (app == null) {
            Timber.i("encrypt: app is null");
            return null;
        }
        if (tempAuthKey == null) retrieveAuthKey(app);
        if (Utils.isNullOrEmpty(tempAuthKey)) {
            Timber.i("encrypt: authKey is empty: %s", tempAuthKey == null ? null : tempAuthKey.length);
            return null;
        }
        BRKey key = new BRKey(tempAuthKey);
        byte[] nonce = getNonce();
        if (Utils.isNullOrEmpty(nonce) || nonce.length != 12) {
            Timber.i("encrypt: nonce is invalid: %s", nonce == null ? null : nonce.length);
            return null;
        }
        byte[] encryptedData = key.encryptNative(data, nonce);
        if (Utils.isNullOrEmpty(encryptedData)) {
            Timber.i("encrypt: encryptNative failed: %s", encryptedData == null ? null : encryptedData.length);
            return null;
        }
        //result is nonce + encryptedData
        byte[] result = new byte[nonce.length + encryptedData.length];
        System.arraycopy(nonce, 0, result, 0, nonce.length);
        System.arraycopy(encryptedData, 0, result, nonce.length, encryptedData.length);
        return result;
    }

    /**
     * decrypt some data using key
     */
    public static byte[] decrypt(byte[] data, Context app) {
        if (data == null || data.length <= 12) {
            Timber.i("decrypt: failed to decrypt: %s", data == null ? null : data.length);
            return null;
        }
        if (app == null) app = BreadApp.getBreadContext();
        if (app == null) return null;
        if (tempAuthKey == null)
            retrieveAuthKey(app);
        BRKey key = new BRKey(tempAuthKey);
        //12 bytes is the nonce
        return key.decryptNative(Arrays.copyOfRange(data, 12, data.length), Arrays.copyOfRange(data, 0, 12));
    }

    //store the authKey for 10 seconds (expensive operation)
    private static void retrieveAuthKey(Context context) {
        if (Utils.isNullOrEmpty(tempAuthKey)) {
            tempAuthKey = BRKeyStore.getAuthKey(context);
            if (tempAuthKey == null) Timber.d("retrieveAuthKey: FAILED, still null!");
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        Timber.e(e);
                    }
                    if (tempAuthKey != null)
                        Arrays.fill(tempAuthKey, (byte) 0);
                    tempAuthKey = null;
                }
            });
        }
    }

    /**
     * validates the key. kvs can not start with a _
     */
    private boolean isKeyValid(String key) {
        Pattern pattern = Pattern.compile(KEY_REGEX);
        Matcher matcher = pattern.matcher(key);
        if (matcher.find()) {
            return true;
        }
        Timber.d("checkKey: found illegal patterns, key: %s", key);
        return false;
    }

}