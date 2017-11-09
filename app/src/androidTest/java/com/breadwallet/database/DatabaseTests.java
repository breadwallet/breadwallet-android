package com.breadwallet.database;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.breadwallet.presenter.activities.BreadActivity;
import com.breadwallet.presenter.entities.BRTransactionEntity;
import com.breadwallet.tools.sqlite.TransactionDataSource;
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

    @Rule
    public ActivityTestRule<BreadActivity> mActivityRule = new ActivityTestRule<>(BreadActivity.class);

    @Before
    public void setUp() {
        Log.e(TAG, "setUp: ");
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSetLocal() {
        TransactionDataSource ds = TransactionDataSource.getInstance(mActivityRule.getActivity());
        ds.putTransaction(new BRTransactionEntity(new byte[0], 1234, 4314123, "some hash"));
        List<BRTransactionEntity> txs = ds.getAllTransactions();
        Assert.assertNotNull(txs);
        Assert.assertEquals(txs.size(), 1);
        Assert.assertEquals(txs.get(0).getBuff(), new byte[0]);
        Assert.assertEquals(txs.get(0).getBlockheight(), 1234);
        Assert.assertEquals(txs.get(0).getTimestamp(), 4314123);
        Assert.assertEquals(txs.get(0).getTxHash(), "some hash");
    }


}
