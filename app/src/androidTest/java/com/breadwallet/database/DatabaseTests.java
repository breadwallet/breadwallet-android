package com.breadwallet.database;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.breadwallet.presenter.activities.BreadActivity;
import com.breadwallet.presenter.activities.intro.IntroActivity;
import com.breadwallet.presenter.entities.BRMerkleBlockEntity;
import com.breadwallet.presenter.entities.BRPeerEntity;
import com.breadwallet.presenter.entities.BRTransactionEntity;
import com.breadwallet.presenter.entities.BlockEntity;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.entities.PeerEntity;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.tools.sqlite.MerkleBlockDataSource;
import com.breadwallet.tools.sqlite.PeerDataSource;
import com.breadwallet.tools.sqlite.TransactionDataSource;
import com.breadwallet.tools.threads.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.platform.interfaces.KVStoreAdaptor;
import com.platform.kvstore.CompletionObject;
import com.platform.kvstore.ReplicatedKVStore;
import com.platform.sqlite.KVItem;
import com.platform.sqlite.PlatformSqliteHelper;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
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

public class DatabaseTests {
    public static final String TAG = DatabaseTests.class.getName();
    final CountDownLatch signal = new CountDownLatch(1000);

    @Rule
    public ActivityTestRule<IntroActivity> mActivityRule = new ActivityTestRule<>(IntroActivity.class);

    @Before
    public void setUp() {
        Log.e(TAG, "setUp: ");
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSetLocal() {
        TransactionDataSource tds = TransactionDataSource.getInstance(mActivityRule.getActivity());
        tds.putTransaction(new BRTransactionEntity(new byte[0], 1234, 4314123, "some hash"));
        List<BRTransactionEntity> txs = tds.getAllTransactions();
        Assert.assertNotNull(txs);
        Assert.assertEquals(txs.size(), 1);
        Assert.assertArrayEquals(txs.get(0).getBuff(), new byte[0]);
        Assert.assertEquals(txs.get(0).getBlockheight(), 1234);
        Assert.assertEquals(txs.get(0).getTimestamp(), 4314123);
        Assert.assertEquals(txs.get(0).getTxHash(), "some hash");

        MerkleBlockDataSource mds = MerkleBlockDataSource.getInstance(mActivityRule.getActivity());
        mds.putMerkleBlocks(new BlockEntity[]{new BlockEntity("SOme cool stuff".getBytes(), 123343)});
        List<BRMerkleBlockEntity> ms = mds.getAllMerkleBlocks();
        Assert.assertNotNull(ms);
        Assert.assertEquals(ms.size(), 1);
        Assert.assertArrayEquals(ms.get(0).getBuff(), "SOme cool stuff".getBytes());
        Assert.assertEquals(ms.get(0).getBlockHeight(), 123343);

        PeerDataSource pds = PeerDataSource.getInstance(mActivityRule.getActivity());
        pds.putPeers(new PeerEntity[]{new PeerEntity("someAddress".getBytes(), "somePort".getBytes(), "someTimestamp".getBytes())});
        List<BRPeerEntity> ps = pds.getAllPeers();
        Assert.assertNotNull(ps);
        Assert.assertEquals(ps.size(), 1);
        Assert.assertArrayEquals(ps.get(0).getAddress(), "someAddress".getBytes());
        Assert.assertArrayEquals(ps.get(0).getPort(), "somePort".getBytes());
        Assert.assertArrayEquals(ps.get(0).getTimeStamp(), "someTimestamp".getBytes());

        CurrencyDataSource cds = CurrencyDataSource.getInstance(mActivityRule.getActivity());
        List<CurrencyEntity> toInsert = new ArrayList<>();
        CurrencyEntity ent = new CurrencyEntity();
        ent.code = "OMG";
        ent.name = "OmiseGo";
        ent.rate = 8.43f;
        toInsert.add(ent);
        cds.putCurrencies(toInsert);
        List<CurrencyEntity> cs = cds.getAllCurrencies();
        Assert.assertNotNull(cs);
        Assert.assertEquals(cs.size(), 1);
        Assert.assertEquals(cs.get(0).name, "OmiseGo");
        Assert.assertEquals(cs.get(0).code, "OMG");
        Assert.assertEquals(cs.get(0).rate, 8.43f, 0);

    }

    private synchronized void done() {
        signal.countDown();
    }

    @Test
    public void testAsynchronousInserts() {
        for (int i = 0; i < 1000; i++) {
            final int finalI = i;
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    TransactionDataSource tds = TransactionDataSource.getInstance(mActivityRule.getActivity());
                    tds.putTransaction(new BRTransactionEntity(String.valueOf(finalI).getBytes(), finalI, finalI, String.valueOf(finalI)));
                    done();
                }
            });
        }
        try {
            signal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "testAsynchronousInserts: Done waiting!");
        TransactionDataSource tds = TransactionDataSource.getInstance(mActivityRule.getActivity());
        List<BRTransactionEntity> txs = tds.getAllTransactions();
        Assert.assertNotNull(txs);
        Assert.assertEquals(txs.size(), 1000);


//        MerkleBlockDataSource mds = MerkleBlockDataSource.getInstance(mActivityRule.getActivity());
//        mds.putMerkleBlocks(new BlockEntity[]{new BlockEntity("SOme cool stuff".getBytes(), 123343)});
//        List<BRMerkleBlockEntity> ms = mds.getAllMerkleBlocks();
//        Assert.assertNotNull(ms);
//        Assert.assertEquals(ms.size(), 1);
//        Assert.assertArrayEquals(ms.get(0).getBuff(), "SOme cool stuff".getBytes());
//        Assert.assertEquals(ms.get(0).getBlockHeight(), 123343);
//
//        PeerDataSource pds = PeerDataSource.getInstance(mActivityRule.getActivity());
//        pds.putPeers(new PeerEntity[]{new PeerEntity("someAddress".getBytes(), "somePort".getBytes(), "someTimestamp".getBytes())});
//        List<BRPeerEntity> ps = pds.getAllPeers();
//        Assert.assertNotNull(ps);
//        Assert.assertEquals(ps.size(), 1);
//        Assert.assertArrayEquals(ps.get(0).getAddress(), "someAddress".getBytes());
//        Assert.assertArrayEquals(ps.get(0).getPort(), "somePort".getBytes());
//        Assert.assertArrayEquals(ps.get(0).getTimeStamp(), "someTimestamp".getBytes());
//
//        CurrencyDataSource cds = CurrencyDataSource.getInstance(mActivityRule.getActivity());
//        List<CurrencyEntity> toInsert = new ArrayList<>();
//        CurrencyEntity ent = new CurrencyEntity();
//        ent.code = "OMG";
//        ent.name = "OmiseGo";
//        ent.rate = 8.43f;
//        toInsert.add(ent);
//        cds.putCurrencies(toInsert);
//        List<CurrencyEntity> cs = cds.getAllCurrencies();
//        Assert.assertNotNull(cs);
//        Assert.assertEquals(cs.size(), 1);
//        Assert.assertEquals(cs.get(0).name, "OmiseGo");
//        Assert.assertEquals(cs.get(0).code, "OMG");
//        Assert.assertEquals(cs.get(0).rate, 8.43f, 0);

    }

}
