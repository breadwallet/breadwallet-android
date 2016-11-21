package com.breadwallet.platform;

import android.app.Activity;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.breadwallet.presenter.activities.MainActivity;
import com.platform.APIClient;
import com.platform.kvstore.CompletionObject;
import com.platform.kvstore.RemoteKVStore;
import com.platform.kvstore.ReplicatedKVStore;
import com.platform.sqlite.KVEntity;
import com.platform.sqlite.PlatformSqliteHelper;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static android.R.attr.value;

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
public class KVStoreTests {
    public static final String TAG = KVStoreTests.class.getName();

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(MainActivity.class);
    List<KVEntity> kvs;
    RemoteKVStore remote;
    ReplicatedKVStore store;

    private static final String KEY = "S6c56bnXQiBjk9mqSYE7ykVQ7NzrRy";

    @Before
    public void setUp() {
        remote = RemoteKVStore.getInstance(APIClient.getInstance(mActivityRule.getActivity()));
        store = new ReplicatedKVStore(mActivityRule.getActivity(), remote);
        kvs = new LinkedList<>();
        kvs.add(new KVEntity(0, 2, "hello", "hello".getBytes(), System.currentTimeMillis(), 0));
        kvs.add(new KVEntity(0, 2, "removed", "removed".getBytes(), System.currentTimeMillis(), 1));
        for (int i = 0; i < 20; i++) {
            kvs.add(new KVEntity(0, 1, "testkey" + i, ("testkey" + i).getBytes(), System.currentTimeMillis(), 0));
        }
        store.set(kvs);
        Log.e(TAG, "setUp: size: " + store.getAllKVs().size());
        int freshSize = store.getAllKVs().size();
        Assert.assertEquals(22, kvs.size());
        Assert.assertEquals(22, freshSize);
    }

    @After
    public void tearDown() {
        mActivityRule.getActivity().deleteDatabase(PlatformSqliteHelper.DATABASE_NAME);
        Log.e(TAG, "tearDown: " + store.getAllKVs().size());
        kvs.clear();
        store.deleteAllKVs();
        Log.e(TAG, "tearDown: " + store.getAllKVs().size());

    }

    @Test
    public void testDatabasesAreSynced() {
        Map<String, byte[]> remoteKV = new LinkedHashMap<>();

        for (KVEntity kv : kvs) {
            if (kv.getDeleted() == 0)
                remoteKV.put(kv.getKey(), kv.getValue());
        }
        Assert.assertEquals(remoteKV.size(), 21);

        List<KVEntity> allLocalKeys = store.getAllKVs();
        Assert.assertEquals(allLocalKeys.size(), 22);
        Map<String, byte[]> localKV = new LinkedHashMap<>();
        for (KVEntity kv : allLocalKeys) {
            if (kv.getDeleted() == 0) {
                KVEntity tmpKv = store.get(kv.getKey(), kv.getVersion());
                localKV.put(kv.getKey(), tmpKv.getValue());
            }
        }

        Assert.assertEquals(localKV.size(), 21);

        Iterator it = remoteKV.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            byte[] val = (byte[]) pair.getValue();
            byte[] valToAssert = localKV.get((String) pair.getKey());

            Assert.assertArrayEquals(val, valToAssert);
        }

        it = localKV.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            byte[] val = (byte[]) pair.getValue();
            byte[] valToAssert = remoteKV.get((String) pair.getKey());
            Assert.assertArrayEquals(val, valToAssert);
        }

    }

    @Test
    public void testSetLocal() {
        store.set(0, 1, "Key1", "Key1".getBytes(), System.currentTimeMillis(), 0);
        store.set(0, 1, "Key1", "Key1".getBytes(), System.currentTimeMillis(), 0);
        store.set(new KVEntity(0, 1, "Key2", "Key2".getBytes(), System.currentTimeMillis(), 2));
        store.set(new KVEntity[]{
                new KVEntity(0, 4, "Key3", "Key3".getBytes(), System.currentTimeMillis(), 2),
                new KVEntity(0, 2, "Key4", "Key4".getBytes(), System.currentTimeMillis(), 0)});
        store.set(Arrays.asList(new KVEntity[]{
                new KVEntity(0, 4, "Key5", "Key5".getBytes(), System.currentTimeMillis(), 1),
                new KVEntity(0, 5, "Key6", "Key6".getBytes(), System.currentTimeMillis(), 5)}));
        Assert.assertEquals(28, store.getAllKVs().size());
    }

    @Test
    public void testSetLocalIncrementsVersion() {
        store.deleteAllKVs();
        CompletionObject obj = store.set(0, 0, "Key1", "Key1".getBytes(), System.currentTimeMillis(), 0);
        List<KVEntity> test = store.getAllKVs();
        Assert.assertEquals(test.size(), 1);
        Assert.assertNull(obj.err);
        Assert.assertEquals(1, store.localVersion("Key1"));
    }

    @Test
    public void testSetThenGet() {
        store.deleteAllKVs();
        byte[] value = "hello".getBytes();
        CompletionObject setObj = store.set(0, 0, "hello", value, System.currentTimeMillis(), 0);
        Assert.assertNull(setObj.err);
        long v1 = setObj.version;
        long t1 = setObj.time;
        KVEntity kvNoVersion = store.get("hello", 0);
        KVEntity kvWithVersion = store.get("hello", 1);
        Assert.assertArrayEquals(value, kvNoVersion.getValue());
        Assert.assertEquals(v1, kvNoVersion.getVersion());
        Assert.assertEquals(t1, kvNoVersion.getTime(), 0.001);

        Assert.assertArrayEquals(value, kvWithVersion.getValue());
        Assert.assertEquals(v1, kvWithVersion.getVersion());
        Assert.assertEquals(t1, kvWithVersion.getTime(), 0.001);

    }

    @Test
    public void testSetThenSetIncrementsVersion() {
        store.deleteAllKVs();
        byte[] value = "hello".getBytes();
        byte[] value2 = "hello2".getBytes();
        CompletionObject setObj = store.set(0, 0, "hello", value, System.currentTimeMillis(), 0);
        CompletionObject setObj2 = store.set(setObj.version, 0, "hello", value2, System.currentTimeMillis(), 0);
        Assert.assertEquals(setObj2.version, setObj.version + 1);
    }
    @Test
    public void testSetThenDel() {
        store.deleteAllKVs();
        byte[] value = "hello".getBytes();
        CompletionObject setObj = store.set(0, 0, "hello", value, System.currentTimeMillis(), 0);
        CompletionObject delObj = store.delete("hello", setObj.version);
        Assert.assertNull(setObj.err);
        Assert.assertNull(delObj.err);

        Assert.assertEquals(delObj.version, setObj.version + 1);
    }


}
