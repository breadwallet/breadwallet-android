package com.breadwallet.tools.sqlite;

import android.content.Context;
import android.util.Log;

import com.breadwallet.presenter.entities.BRTransactionEntity;

import java.util.List;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 12/21/15.
 * Copyright (c) 2015 Mihail Gutan <mihail@breadwallet.com>
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
public class SQLiteManager {
    private static final String TAG = SQLiteManager.class.getName();

    private static SQLiteManager instance;
    private static Context ctx;

    private SQLiteManager() {
    }

    public static synchronized SQLiteManager getInstance(Context context) {
        ctx = context;
        if (instance == null) {
            instance = new SQLiteManager();
        }
        return instance;
    }

    public List<BRTransactionEntity> getTransactions() {
        TransactionDataSource TXdataSource = new TransactionDataSource(ctx);
        TXdataSource.open();
        List<BRTransactionEntity> txValues = TXdataSource.getAllTransactions();
//        ByteBuffer transArray[] = new ByteBuffer[txValues.size()];
//        Iterator<BRTransactionEntity> transactionEntityIterator = txValues.iterator();
//        int i = 0;
//        while (transactionEntityIterator.hasNext()) {
//            BRTransactionEntity transactionEntity = transactionEntityIterator.next();
//            ByteBuffer buffer = ByteBuffer.wrap(transactionEntity.getBuff());
//            Log.e(TAG, "The transaction: " + transactionEntity.getId()
//                    + " " + buffer.array().length);
//            Log.e(TAG, "The buffer limit: " + buffer.limit());
//
//            transArray[i++] = buffer;
//
//        }
        TXdataSource.close();
        return txValues;
    }

    public void insertTransaction(byte[] transaction, long blockheight, long timestamp) {
        BRTransactionEntity entity = new BRTransactionEntity(transaction, blockheight, timestamp);
        TransactionDataSource TXdataSource = new TransactionDataSource(ctx);
        TXdataSource.open();
        TXdataSource.createTransaction(entity);
        Log.e(TAG, "SQLiteManager - transaction inserted");
        TXdataSource.close();
    }
}
