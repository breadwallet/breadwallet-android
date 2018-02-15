package com.breadwallet.database;

import android.app.Activity;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.breadwallet.presenter.activities.intro.IntroActivity;
import com.breadwallet.presenter.entities.BRMerkleBlockEntity;
import com.breadwallet.presenter.entities.BRPeerEntity;
import com.breadwallet.presenter.entities.BRTransactionEntity;
import com.breadwallet.presenter.entities.BlockEntity;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.entities.PeerEntity;
import com.breadwallet.tools.sqlite.BtcBchTransactionDataStore;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.tools.sqlite.MerkleBlockDataSource;
import com.breadwallet.tools.sqlite.PeerDataSource;
import com.breadwallet.tools.threads.BRExecutor;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.breadwallet.wallet.wallets.bitcoincash.WalletBchManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;


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

    // Test Wallets
    BaseWalletManager mBtcWallet;
    BaseWalletManager mBchWallet;

    @Rule
    public ActivityTestRule<IntroActivity> mActivityRule = new ActivityTestRule<>(IntroActivity.class);

    @Before
    public void setUp() {
        Log.e(TAG, "setUp: ");


        mBtcWallet = WalletBitcoinManager.getInstance(mActivityRule.getActivity());
        mBchWallet = WalletBchManager.getInstance(mActivityRule.getActivity());
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSetLocal() {

        // Test BTC transaction insert
        Activity app = mActivityRule.getActivity();
        BtcBchTransactionDataStore tds = BtcBchTransactionDataStore.getInstance(app);
        tds.putTransaction(app, mBtcWallet, new BRTransactionEntity(new byte[0], 1234, 4314123, "some hash", mBtcWallet.getIso(app)));
        List<BRTransactionEntity> txs = tds.getAllTransactions(app, mBtcWallet);
        Assert.assertNotNull(txs);
        Assert.assertEquals(txs.size(), 1);
        Assert.assertArrayEquals(txs.get(0).getBuff(), new byte[0]);
        Assert.assertEquals(txs.get(0).getBlockheight(), 1234);
        Assert.assertEquals(txs.get(0).getTimestamp(), 4314123);
        Assert.assertEquals(txs.get(0).getTxHash(), "some hash");


        // Test BCH Transaction insert
        BtcBchTransactionDataStore bchInsert = BtcBchTransactionDataStore.getInstance(app);
        tds.putTransaction(app, mBchWallet, new BRTransactionEntity(new byte[0], 4321, 5674123, "some hash", mBchWallet.getIso(app)));
        List<BRTransactionEntity> bchTxs = bchInsert.getAllTransactions(app, mBchWallet);
        Assert.assertNotNull(bchTxs);
        Assert.assertEquals(bchTxs.size(), 1);
        Assert.assertArrayEquals(bchTxs.get(0).getBuff(), new byte[0]);
        Assert.assertEquals(bchTxs.get(0).getBlockheight(), 4321);
        Assert.assertEquals(bchTxs.get(0).getTimestamp(), 5674123);
        Assert.assertEquals(bchTxs.get(0).getTxHash(), "some hash");
        Assert.assertEquals(bchTxs.get(0).getTxISO(), "bch");


        MerkleBlockDataSource mds = MerkleBlockDataSource.getInstance(mActivityRule.getActivity());
        mds.putMerkleBlocks(app, mBtcWallet, new BlockEntity[]{new BlockEntity("SOme cool stuff".getBytes(), 123343)});
        List<BRMerkleBlockEntity> ms = mds.getAllMerkleBlocks(app, mBtcWallet);
        Assert.assertNotNull(ms);
        Assert.assertEquals(ms.size(), 1);
        Assert.assertArrayEquals(ms.get(0).getBuff(), "SOme cool stuff".getBytes());
        Assert.assertEquals(ms.get(0).getBlockHeight(), 123343);

        PeerDataSource pds = PeerDataSource.getInstance(mActivityRule.getActivity());
        pds.putPeers(app, mBtcWallet, new PeerEntity[]{new PeerEntity("someAddress".getBytes(), "somePort".getBytes(), "someTimestamp".getBytes())});
        List<BRPeerEntity> ps = pds.getAllPeers(app, mBtcWallet);
        Assert.assertNotNull(ps);
        Assert.assertEquals(ps.size(), 1);
        Assert.assertArrayEquals(ps.get(0).getAddress(), "someAddress".getBytes());
        Assert.assertArrayEquals(ps.get(0).getPort(), "somePort".getBytes());
        Assert.assertArrayEquals(ps.get(0).getTimeStamp(), "someTimestamp".getBytes());


        // Test inserting OMG as a currency
        CurrencyDataSource cds = CurrencyDataSource.getInstance(mActivityRule.getActivity());
        List<CurrencyEntity> toInsert = new ArrayList<>();
        CurrencyEntity ent = new CurrencyEntity();
        ent.code = "OMG";
        ent.name = "OmiseGo";
        ent.rate = 8.43f;
        toInsert.add(ent);
        cds.putCurrencies(app, mBtcWallet, toInsert);
        List<CurrencyEntity> cs = cds.getAllCurrencies(app, mBtcWallet);
        Assert.assertNotNull(cs);
        Assert.assertEquals(cs.size(), 1);
        Assert.assertEquals(cs.get(0).name, "OmiseGo");
        Assert.assertEquals(cs.get(0).code, "OMG");
        Assert.assertEquals(cs.get(0).rate, 8.43f, 0);

        // Test inserting BTC as a currency
        CurrencyDataSource currencyDataSource = CurrencyDataSource.getInstance(mActivityRule.getActivity());
        List<CurrencyEntity> btcToInsert = new ArrayList<>();
        CurrencyEntity btcEntity = new CurrencyEntity();
        ent.code = "BTC";
        ent.name = "Bitcoin";
        ent.rate = 1.0f;
        toInsert.add(ent);
        cds.putCurrencies(app, mBtcWallet, btcToInsert);
        List<CurrencyEntity> currencyEntities = cds.getAllCurrencies(app, mBtcWallet);
        Assert.assertNotNull(cs);
        Assert.assertEquals(cs.size(), 1);
        Assert.assertEquals(cs.get(0).name, "BTC");
        Assert.assertEquals(cs.get(0).code, "Bitcoin");
        Assert.assertEquals(cs.get(0).rate, 1.0f, 0);


    }

    private class InsertionThread extends Thread {


        @Override
        public void run() {
            super.run();

            // Test BCH Transaction insert
            Activity app = mActivityRule.getActivity();
            BtcBchTransactionDataStore tds = BtcBchTransactionDataStore.getInstance(app);
            BtcBchTransactionDataStore bchInsert = BtcBchTransactionDataStore.getInstance(app);
            tds.putTransaction(app, mBchWallet, new BRTransactionEntity(new byte[0], 4321, 5674123, "some hash", mBchWallet.getIso(app)));
            List<BRTransactionEntity> bchTxs = bchInsert.getAllTransactions(app, mBchWallet);
            Assert.assertNotNull(bchTxs);
            Assert.assertEquals(bchTxs.size(), 1);
            Assert.assertArrayEquals(bchTxs.get(0).getBuff(), new byte[0]);
            Assert.assertEquals(bchTxs.get(0).getBlockheight(), 4321);
            Assert.assertEquals(bchTxs.get(0).getTimestamp(), 5674123);
            Assert.assertEquals(bchTxs.get(0).getTxHash(), "some hash");
            Assert.assertEquals(bchTxs.get(0).getTxISO(), "bch");

        }
    }

    private synchronized void done() {
        signal.countDown();
    }

    @Test
    public void testConcurrentTransactionInsertion() {

        final Activity app = mActivityRule.getActivity();


        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                BtcBchTransactionDataStore tds = BtcBchTransactionDataStore.getInstance(app);
                tds.putTransaction(app, mBtcWallet, new BRTransactionEntity(new byte[0], 1989, 00112233, "first", mBchWallet.getIso(app)));
                done();
            }
        });


        BtcBchTransactionDataStore tds2 = BtcBchTransactionDataStore.getInstance(app);
        tds2.putTransaction(app, mBtcWallet, new BRTransactionEntity(new byte[1], 1990, 11223344, "second", mBchWallet.getIso(app)));
        done();

        BtcBchTransactionDataStore tds3 = BtcBchTransactionDataStore.getInstance(app);
        tds3.putTransaction(app, mBtcWallet, new BRTransactionEntity(new byte[2], 1991, 22334455, "third", mBchWallet.getIso(app)));
        done();


    }

    @Test
    public void testAsynchronousInserts() {
        final Activity app = mActivityRule.getActivity();
        for (int i = 0; i < 1000; i++) {
            final int finalI = i;
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {

                    BtcBchTransactionDataStore tds = BtcBchTransactionDataStore.getInstance(app);
                    tds.putTransaction(app, mBtcWallet, new BRTransactionEntity(String.valueOf(finalI).getBytes(), finalI, finalI, String.valueOf(finalI), mBtcWallet.getIso(app)));
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
        BtcBchTransactionDataStore tds = BtcBchTransactionDataStore.getInstance(app);
        List<BRTransactionEntity> txs = tds.getAllTransactions(app, mBtcWallet);
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
