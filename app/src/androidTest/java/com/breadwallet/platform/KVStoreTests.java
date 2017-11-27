package com.breadwallet.platform;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.breadwallet.presenter.activities.intro.WriteDownActivity;
import com.breadwallet.tools.threads.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.platform.entities.TxMetaData;
import com.platform.interfaces.KVStoreAdaptor;
import com.platform.kvstore.CompletionObject;
import com.platform.kvstore.ReplicatedKVStore;
import com.platform.sqlite.KVItem;
import com.platform.sqlite.PlatformSqliteHelper;
import com.platform.tools.KVStoreManager;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 9/30/16.
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

@RunWith(AndroidJUnit4.class)
@LargeTest
public class KVStoreTests {
    public static final String TAG = KVStoreTests.class.getName();

    @ClassRule
    public static ActivityTestRule<WriteDownActivity> mActivityRule = new ActivityTestRule<>(WriteDownActivity.class);
    private static KVStoreAdaptor remote = new MockUpAdapter();
    private static ReplicatedKVStore store;

    public static class MockUpAdapter implements KVStoreAdaptor {
        public Map<String, KVItem> remoteKVs = new HashMap<>();

        @Override
        public CompletionObject ver(String key) {
            KVItem result = remoteKVs.get(key);
            return result == null ? new CompletionObject(CompletionObject.RemoteKVStoreError.notFound) : new CompletionObject(result.version, result.time, result.deleted == 0 ? null : CompletionObject.RemoteKVStoreError.tombstone);
        }

        @Override
        public CompletionObject put(String key, byte[] value, long version) {
            KVItem result = remoteKVs.get(key);
            if (result == null) {
                if (version != 1)
                    return new CompletionObject(CompletionObject.RemoteKVStoreError.notFound);
                KVItem newObj = new KVItem(1, -1, key, value, System.currentTimeMillis(), 0);
                remoteKVs.put(key, newObj);
                return new CompletionObject(1, newObj.time, null);
            }
            if (version != result.version)
                return new CompletionObject(CompletionObject.RemoteKVStoreError.conflict);
            KVItem newObj = new KVItem(result.version + 1, -1, key, value, System.currentTimeMillis(), 0);
            remoteKVs.put(newObj.key, newObj);
            return new CompletionObject(newObj.version, newObj.time, null);
        }

        @Override
        public CompletionObject del(String key, long version) {
            KVItem result = remoteKVs.get(key);
            if (result == null)
                return new CompletionObject(CompletionObject.RemoteKVStoreError.notFound);
            if (result.version != version)
                return new CompletionObject(CompletionObject.RemoteKVStoreError.conflict);
            KVItem newObj = new KVItem(result.version + 1, -1, result.key, result.value, result.time, 1);
            remoteKVs.put(newObj.key, newObj);
            return new CompletionObject(newObj.version, newObj.time, null);
        }

        @Override
        public CompletionObject get(String key, long version) {
            KVItem result = remoteKVs.get(key);
            if (result == null)
                return new CompletionObject(CompletionObject.RemoteKVStoreError.notFound);
            if (version != result.version)
                return new CompletionObject(0, System.currentTimeMillis(), CompletionObject.RemoteKVStoreError.conflict);
            return new CompletionObject(result.version, result.time, result.value, result.deleted == 0 ? null : CompletionObject.RemoteKVStoreError.tombstone);
        }

        @Override
        public CompletionObject keys() {
            List<KVItem> result = new ArrayList<>();
            for (KVItem kv : remoteKVs.values()) {
                if (kv.deleted != 0) kv.err = CompletionObject.RemoteKVStoreError.tombstone;
                result.add(kv);
            }
            return new CompletionObject(result);
        }

        public void putKv(KVItem kv) {
            remoteKVs.put(kv.key, kv);
        }

    }

    @Before
    public void setUp() {
        BRConstants.PLATFORM_ON = false;
        ((MockUpAdapter) remote).remoteKVs.clear();
        ((MockUpAdapter) remote).putKv(new KVItem(1, 1, "hello", ReplicatedKVStore.encrypt("hello".getBytes(), mActivityRule.getActivity()), System.currentTimeMillis(), 0));
        ((MockUpAdapter) remote).putKv(new KVItem(1, 1, "removed", ReplicatedKVStore.encrypt("removed".getBytes(), mActivityRule.getActivity()), System.currentTimeMillis(), 1));
        for (int i = 0; i < 20; i++) {
            ((MockUpAdapter) remote).putKv(new KVItem(1, 1, "testkey" + i, ReplicatedKVStore.encrypt(("testkey" + i).getBytes(), mActivityRule.getActivity()), System.currentTimeMillis(), 0));
        }

        store = ReplicatedKVStore.getInstance(mActivityRule.getActivity(), remote);
        store.deleteAllKVs();
        Assert.assertEquals(22, ((MockUpAdapter) remote).remoteKVs.size());
    }

    @After
    public void tearDown() {
        store.deleteAllKVs();
        mActivityRule.getActivity().deleteDatabase(PlatformSqliteHelper.DATABASE_NAME);
        ((MockUpAdapter) remote).remoteKVs.clear();
    }

    public void assertDatabasesAreSynced() {
        Map<String, byte[]> remoteKV = new LinkedHashMap<>();
        List<KVItem> kvs = new ArrayList<>(((MockUpAdapter) remote).remoteKVs.values());
        for (KVItem kv : kvs) {
            if (kv.deleted == 0)
                remoteKV.put(kv.key, kv.value);
        }

        List<KVItem> allLocalKeys = store.getRawKVs();
        Map<String, byte[]> localKV = new LinkedHashMap<>();

        for (KVItem kv : allLocalKeys) {
            if (kv.deleted == 0) {
                CompletionObject object = store.get(kv.key, kv.version);
//                KVItem tmpKv = object.kv;

                localKV.put(kv.key, object.kv.value);
            }
        }

        Assert.assertEquals(remoteKV.size(), localKV.size());

        Iterator it = remoteKV.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            byte[] val = ReplicatedKVStore.decrypt((byte[]) pair.getValue(), mActivityRule.getActivity());
            byte[] valToAssert = localKV.get((String) pair.getKey());
            String valStr = new String(val);
            String valToAssertStr = new String(valToAssert);
            Assert.assertArrayEquals(val, valToAssert);
        }

        it = localKV.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            byte[] val = (byte[]) pair.getValue();
            byte[] valToAssert = ReplicatedKVStore.decrypt(remoteKV.get((String) pair.getKey()), mActivityRule.getActivity());
            Assert.assertArrayEquals(val, valToAssert);
        }

    }


    @Test
    public void testSetLocal() {
        CompletionObject obj = store.set(0, 1, "Key1", "Key1".getBytes(), System.currentTimeMillis(), 0);
        Assert.assertNull(obj.err);
        store.set(0, 1, "Key1", "Key1".getBytes(), System.currentTimeMillis(), 0);
        store.set(new KVItem(0, 1, "Key2", "Key2".getBytes(), System.currentTimeMillis(), 2));
        store.set(new KVItem[]{
                new KVItem(0, 4, "Key3", "Key3".getBytes(), System.currentTimeMillis(), 2),
                new KVItem(0, 2, "Key4", "Key4".getBytes(), System.currentTimeMillis(), 0)});
        store.set(Arrays.asList(new KVItem[]{
                new KVItem(0, 4, "Key5", "Key5".getBytes(), System.currentTimeMillis(), 1),
                new KVItem(0, 5, "Key6", "Key6".getBytes(), System.currentTimeMillis(), 5)}));
        Assert.assertEquals(6, store.getRawKVs().size());
    }

    @Test
    public void testDeleteAll() {
        CompletionObject obj = store.set(0, 1, "Key1", "Key1".getBytes(), System.currentTimeMillis(), 0);
        store.set(0, 1, "Key1", "Key1".getBytes(), System.currentTimeMillis(), 0);
        store.set(new KVItem(0, 1, "Key2", "Key2".getBytes(), System.currentTimeMillis(), 2));
        store.set(new KVItem[]{
                new KVItem(0, 4, "Key3", "Key3".getBytes(), System.currentTimeMillis(), 2),
                new KVItem(0, 2, "Key4", "Key4".getBytes(), System.currentTimeMillis(), 0)});
        store.set(Arrays.asList(new KVItem[]{
                new KVItem(0, 4, "Key5", "Key5".getBytes(), System.currentTimeMillis(), 1),
                new KVItem(0, 5, "Key6", "Key6".getBytes(), System.currentTimeMillis(), 5)}));
        List<KVItem> kvs = store.getRawKVs();
        Assert.assertEquals(6, kvs.size());
        store.deleteAllKVs();
        kvs = store.getRawKVs();
        Assert.assertEquals(0, kvs.size());

    }

    @Test
    public void testSetLocalIncrementsVersion() {
        store.deleteAllKVs();
        CompletionObject obj = store.set(0, 0, "Key1", "Key1".getBytes(), System.currentTimeMillis(), 0);
        Assert.assertNull(obj.err);
        List<KVItem> test = store.getRawKVs();
        Assert.assertEquals(1, test.size());
        Assert.assertEquals(1, store.localVersion("Key1").version);
    }

    @Test
    public void testMultithreadedInserts() {
        final AtomicInteger count = new AtomicInteger();
        for (int i = 0; i < 1000; i++) {
            final int finalI = i;
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    CompletionObject obj = store.set(0, 0, "Key" + finalI, "Key1".getBytes(), System.currentTimeMillis(), 0);
                    Assert.assertNull(obj.err);
                    Assert.assertEquals(obj.version, 1);
                    count.incrementAndGet();
                    CompletionObject remObj = store.delete("Key" + finalI, store.localVersion("Key" + finalI).version);
                    Assert.assertNull(remObj.err);
                    count.decrementAndGet();
                }
            });
        }
        try {
            Thread.sleep(10000);
            Assert.assertEquals(count.get(), 0);
            List<KVItem> items = store.getRawKVs();
            Assert.assertEquals(items.size(), 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testSetThenGet() {
        byte[] value = "hello".getBytes();
        CompletionObject setObj = store.set(0, 0, "hello", value, System.currentTimeMillis(), 0);
        Assert.assertNull(setObj.err);
        long v1 = setObj.version;
        long t1 = setObj.time;
        CompletionObject obj = store.get("hello", 0);
        KVItem kvNoVersion = obj.kv;
        obj = store.get("hello", 1);
        KVItem kvWithVersion = obj.kv;
        Assert.assertArrayEquals(value, kvNoVersion.value);
        Assert.assertEquals(v1, kvNoVersion.version);
        Assert.assertEquals(t1, kvNoVersion.time, 0.001);

        Assert.assertNotNull(kvWithVersion);
        Assert.assertArrayEquals(value, kvWithVersion.value);
        Assert.assertEquals(v1, kvWithVersion.version);
        Assert.assertEquals(t1, kvWithVersion.time, 0.001);

    }

    @Test
    public void testSetThenSetIncrementsVersion() {
        byte[] value = "hello".getBytes();
        byte[] value2 = "hello2".getBytes();
        CompletionObject setObj = store.set(0, 0, "hello", value, System.currentTimeMillis(), 0);
        CompletionObject setObj2 = store.set(setObj.version, 0, "hello", value2, System.currentTimeMillis(), 0);
        Assert.assertEquals(setObj2.version, setObj.version + 1);
    }

    @Test
    public void testSetThenDel() {
        byte[] value = "hello".getBytes();
        CompletionObject setObj = store.set(0, 0, "hello", value, System.currentTimeMillis(), 0);
        CompletionObject delObj = store.delete("hello", setObj.version);
        Assert.assertNull(setObj.err);
        Assert.assertNull(delObj.err);

        Assert.assertEquals(delObj.version, setObj.version + 1);
    }

    @Test
    public void testSetThenDelThenGet() {
        byte[] value = "hello".getBytes();
        CompletionObject setObj = store.set(0, 0, "hello", value, System.currentTimeMillis(), 0);
        CompletionObject delObj = store.delete("hello", setObj.version);
        Assert.assertNull(setObj.err);
        Assert.assertNull(delObj.err);

        CompletionObject object = store.get("hello", 0);
        KVItem getKv = object.kv;

        Assert.assertEquals(delObj.version, setObj.version + 1);
        Assert.assertEquals(getKv.version, setObj.version + 1);
    }

    @Test
    public void testSetWithIncorrectFirstVersionFails() {
        byte[] value = "hello".getBytes();
        CompletionObject setObj = store.set(1, 0, "hello", value, System.currentTimeMillis(), 0);
        Assert.assertNotNull(setObj.err);
    }

    @Test
    public void testSetWithStaleVersionFails() {
        byte[] value = "hello".getBytes();
        CompletionObject setObj = store.set(0, 0, "hello", value, System.currentTimeMillis(), 0);
        CompletionObject setStaleObj = store.set(0, 0, "hello", value, System.currentTimeMillis(), 0);
        Assert.assertNull(setObj.err);
        Assert.assertEquals(CompletionObject.RemoteKVStoreError.conflict, setStaleObj.err);
    }

    @Test
    public void testGetNonExistentKeyFails() {
        CompletionObject object = store.get("hello", 0);
        KVItem getKv = object.kv;
        Assert.assertNull(getKv);
    }

    @Test
    public void testGetNonExistentKeyVersionFails() {
        CompletionObject object = store.get("hello", 1);
        KVItem getKv = object.kv;

        Assert.assertNull(getKv);
    }

    @Test
    public void testGetAllKeys() {
        byte[] value = "hello".getBytes();
        long time = System.currentTimeMillis();
        CompletionObject setObj = store.set(0, 0, "hello", value, time, 0);
        List<KVItem> list = store.getRawKVs();

        Assert.assertNotNull(list);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals("hello", list.get(0).key);
        Assert.assertEquals(setObj.version, list.get(0).version);
        Assert.assertEquals(setObj.time, list.get(0).time, 0.001);
        Assert.assertEquals(0, list.get(0).remoteVersion);
        Assert.assertEquals(0, list.get(0).deleted);
    }

    @Test
    public void testSetRemoteVersion() {
        byte[] value = "hello".getBytes();
        long time = System.currentTimeMillis();
        CompletionObject setObj = store.set(0, 0, "hello", value, time, 0);
        CompletionObject setRemoteVersionObj = store.setRemoteVersion("hello", setObj.version, 1);
        Assert.assertEquals(setRemoteVersionObj.version, setObj.version + 1);
        long remoteVer = store.remoteVersion("hello");
        Assert.assertEquals(1, remoteVer);

    }

    @Test
    public void testBasicSyncGetAllObjects() {
        boolean success = store.syncAllKeys();
        Assert.assertEquals(true, success);

        List<KVItem> localKeys = store.getRawKVs();
        Assert.assertEquals(((MockUpAdapter) remote).remoteKVs.size() - 1, localKeys.size());
        assertDatabasesAreSynced();
    }

    @Test
    public void testSyncTenTimes() {
        int n = 10;
        while (n > 0) {
            boolean success = store.syncAllKeys();
            Assert.assertTrue(success);
            n--;
        }
        assertDatabasesAreSynced();
    }

    @Test
    public void testSyncAddsLocalKeysToRemote() {
        CompletionObject setObj = store.set(new KVItem(0, -1, "derp", "derp".getBytes(), System.currentTimeMillis(), 0));
        Assert.assertNull(setObj.err);
        boolean success = store.syncAllKeys();
        Assert.assertTrue(success);
        CompletionObject obj = remote.get("derp", 1);
        Assert.assertArrayEquals(ReplicatedKVStore.decrypt(obj.value, mActivityRule.getActivity()), "derp".getBytes());
    }

    @Test
    public void testSyncSavesRemoteVersion() {
        boolean success = store.syncAllKeys();
        Assert.assertTrue(success);
        long ver = store.remoteVersion("hello");
        Assert.assertEquals(((MockUpAdapter) remote).remoteKVs.get("hello").version, 1);
        Assert.assertEquals(((MockUpAdapter) remote).remoteKVs.get("hello").version, ver);
        assertDatabasesAreSynced();
    }

    @Test
    public void testSyncPreventsAnotherConcurrentSync() {
//        boolean success = store.syncAllKeys();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                boolean success = store.syncAllKeys();
//                Assert.assertTrue(success);
//                Assert.assertFalse(success);
//            }
//        }).start();
//        Assert.assertTrue(success);
    }

    @Test
    public void testLocalDeleteReplicates() {
        CompletionObject setObj = store.set(new KVItem(0, 0, "goodbye_cruel_world", "goodbye_cruel_world".getBytes(), System.currentTimeMillis(), 0));
        Assert.assertNull(setObj.err);
        boolean success = store.syncAllKeys();
        Assert.assertTrue(success);
        assertDatabasesAreSynced();
        CompletionObject delObj = store.delete("goodbye_cruel_world", store.localVersion("goodbye_cruel_world").version);
        Assert.assertNull(delObj.err);
        success = store.syncAllKeys();
        Assert.assertTrue(success);
        assertDatabasesAreSynced();
        KVItem kv = ((MockUpAdapter) remote).remoteKVs.get("goodbye_cruel_world");
        Assert.assertTrue(kv.deleted > 0);

    }

    @Test
    public void testLocalUpdateReplicates() {
        CompletionObject setObj = store.set(new KVItem(0, -1, "goodbye_cruel_world", "goodbye_cruel_world".getBytes(), System.currentTimeMillis(), 0));
        boolean success = store.syncAllKeys();
        Assert.assertTrue(success);
        assertDatabasesAreSynced();
        setObj = store.set(new KVItem(store.localVersion("goodbye_cruel_world").version, -1, "goodbye_cruel_world", "goodbye_cruel_world with some new info".getBytes(), System.currentTimeMillis(), 0));
        success = store.syncAllKeys();
        Assert.assertTrue(success);
        assertDatabasesAreSynced();
        Assert.assertArrayEquals("goodbye_cruel_world with some new info".getBytes(), ReplicatedKVStore.decrypt(remote.get("goodbye_cruel_world", store.remoteVersion("goodbye_cruel_world")).value, mActivityRule.getActivity()));

    }

    @Test
    public void testRemoteDeleteReplicates() {
        boolean success = store.syncAllKeys();
        Assert.assertTrue(success);
        assertDatabasesAreSynced();
        KVItem kv = ((MockUpAdapter) remote).remoteKVs.get("hello");
        ((MockUpAdapter) remote).remoteKVs.put("hello", new KVItem(kv.version + 1, -1, kv.key, kv.value, System.currentTimeMillis(), 1));
        success = store.syncAllKeys();
        Assert.assertTrue(success);
        assertDatabasesAreSynced();
        CompletionObject getObj = store.get("hello", 0);
        Assert.assertNull(getObj.err);
        Assert.assertTrue(getObj.kv.deleted > 0);
        success = store.syncAllKeys();
        Assert.assertTrue(success);
        assertDatabasesAreSynced();

    }

    @Test
    public void testRemoteUpdateReplicates() {
        boolean success = store.syncAllKeys();
        Assert.assertTrue(success);
        assertDatabasesAreSynced();

        KVItem kv = ((MockUpAdapter) remote).remoteKVs.get("hello");
        ((MockUpAdapter) remote).remoteKVs.put("hello", new KVItem(kv.version + 1, -1, kv.key, ReplicatedKVStore.encrypt("newVal".getBytes(), mActivityRule.getActivity()), System.currentTimeMillis(), 0));
        success = store.syncAllKeys();
        Assert.assertTrue(success);
        assertDatabasesAreSynced();

        CompletionObject getObj = store.get("hello", 0);
        Assert.assertNull(getObj.err);
        Assert.assertArrayEquals(getObj.kv.value, "newVal".getBytes());
        success = store.syncAllKeys();
        Assert.assertTrue(success);
        assertDatabasesAreSynced();

    }

    @Test
    public void testEnableEncryptedReplication() {
        ((MockUpAdapter) remote).remoteKVs.clear();
        CompletionObject setObj = store.set(new KVItem(0, 0, "derp", "derp".getBytes(), System.currentTimeMillis(), 0));
        Assert.assertNull(setObj.err);
        boolean success = store.syncAllKeys();
        Assert.assertTrue(success);
        CompletionObject obj = remote.get("derp", 1);
        Assert.assertArrayEquals(ReplicatedKVStore.decrypt(obj.value, mActivityRule.getActivity()), "derp".getBytes());

    }
    @Test
    public void testGetAllMds() {
        store.set(0, 1, "Key1", "Key1".getBytes(), System.currentTimeMillis(), 0);
        store.set(new KVItem(0, 1, "Key2", "Key2".getBytes(), System.currentTimeMillis(), 2));
        store.set(new KVItem[]{
                new KVItem(0, 4, "fdf-gsd34534", "second".getBytes(), System.currentTimeMillis(), 2),
                new KVItem(0, 2, "fsdtxn2-fdslkjf34", "ignore".getBytes(), System.currentTimeMillis(), 0)});
        store.set(Arrays.asList(new KVItem[]{
                new KVItem(0, 4, "Key5", "Key5".getBytes(), System.currentTimeMillis(), 1),
                new KVItem(0, 5, "Key6", "Key6".getBytes(), System.currentTimeMillis(), 5)}));
        List<KVItem> kvs = store.getRawKVs();
        Assert.assertEquals(kvs.size(), 6);

        TxMetaData tx = new TxMetaData();
        byte[] theHash = new byte[]{3, 5, 64, 2, 4, 5, 63, 7, 0, 56, 34};
        tx.blockHeight = 123;
        tx.classVersion = 3;
        tx.comment = "hehey !";
        tx.creationTime = 21324;
        tx.deviceId = "someDevice2324";
        tx.fee = 234;
        tx.txSize = 23423;
        tx.exchangeCurrency = "curr";
        tx.exchangeRate = 23.4343;
        KVStoreManager.getInstance().putTxMetaData(mActivityRule.getActivity(), tx, theHash);
        List<KVItem> items = store.getRawKVs();
        Assert.assertEquals(7, items.size());

        Map<String, TxMetaData> mds = KVStoreManager.getInstance().getAllTxMD(mActivityRule.getActivity());
        Assert.assertEquals(mds.size(), 1);

//        Assert.assertEquals(mds.(0).blockHeight, 123);
//        Assert.assertEquals(mds.get(0).classVersion, 3);
//        Assert.assertEquals(mds.get(0).comment, "hehey !");
//        Assert.assertEquals(mds.get(0).creationTime, 21324);
//        Assert.assertEquals(mds.get(0).deviceId, "someDevice2324");
//        Assert.assertEquals(mds.get(0).fee, 234);
//        Assert.assertEquals(mds.get(0).txSize, 23423);
//        Assert.assertEquals(mds.get(0).exchangeCurrency, "curr");
//        Assert.assertEquals(mds.get(0).exchangeRate, 23.4343, 0);

    }

    @Test
    public void testEncryptDecrypt() {
        String data = "Ladies and Gentlemen of the class of '99: If I could offer you only one tip for the future, " +
                "sunscreen would be it.";
        byte[] encryptedData = ReplicatedKVStore.encrypt(data.getBytes(), mActivityRule.getActivity());

        Assert.assertTrue(encryptedData != null && encryptedData.length > 0);

        byte[] decryptedData = ReplicatedKVStore.decrypt(encryptedData, mActivityRule.getActivity());

        Assert.assertNotEquals(encryptedData, decryptedData);

        Assert.assertArrayEquals(decryptedData, data.getBytes());
        Assert.assertEquals(new String(decryptedData), data);

    }

    @Test
    public void testKVManager() {
        TxMetaData tx = new TxMetaData();
        byte[] theHash = new byte[]{3, 5, 64, 2, 4, 5, 63, 7, 0, 56, 34};
        tx.blockHeight = 123;
        tx.classVersion = 3;
        tx.comment = "hehey !";
        tx.creationTime = 21324;
        tx.deviceId = "someDevice2324";
        tx.fee = 234;
        tx.txSize = 23423;
        tx.exchangeCurrency = "curr";
        tx.exchangeRate = 23.4343;
        KVStoreManager.getInstance().putTxMetaData(mActivityRule.getActivity(), tx, theHash);
        List<KVItem> items = store.getRawKVs();
        Assert.assertEquals(1, items.size());

        TxMetaData newTx = KVStoreManager.getInstance().getTxMetaData(mActivityRule.getActivity(), theHash);
        Assert.assertEquals(newTx.blockHeight, 123);
        Assert.assertEquals(newTx.classVersion, 3);
        Assert.assertEquals(newTx.comment, "hehey !");
        Assert.assertEquals(newTx.creationTime, 21324);
        Assert.assertEquals(newTx.deviceId, "someDevice2324");
        Assert.assertEquals(newTx.fee, 234);
        Assert.assertEquals(newTx.txSize, 23423);
        Assert.assertEquals(newTx.exchangeCurrency, "curr");
        Assert.assertEquals(newTx.exchangeRate, 23.4343, 0);

    }
    //((MockUpAdapter) remote).remoteKVs.size()

}
