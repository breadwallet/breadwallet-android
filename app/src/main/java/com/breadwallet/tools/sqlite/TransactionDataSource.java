package com.breadwallet.tools.sqlite;

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
import android.database.sqlite.SQLiteDatabase;
import android.os.NetworkOnMainThreadException;
import android.util.Log;

import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.presenter.entities.BRTransactionEntity;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.util.BRConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TransactionDataSource implements BRDataSourceInterface {
    private static final String TAG = TransactionDataSource.class.getName();

    private AtomicInteger mOpenCounter = new AtomicInteger();

    // Database fields
    private SQLiteDatabase database;
    private final BRSQLiteHelper dbHelper;
    private final String[] allColumns = {
            BRSQLiteHelper.TX_COLUMN_ID,
            BRSQLiteHelper.TX_BUFF,
            BRSQLiteHelper.TX_BLOCK_HEIGHT,
            BRSQLiteHelper.TX_TIME_STAMP
    };

    public interface OnTxAddedListener {
        void onTxAdded();
    }

    List<OnTxAddedListener> listeners = new ArrayList<>();

    public void addTxAddedListener(OnTxAddedListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    public void removeListener(OnTxAddedListener listener) {
        listeners.remove(listener);

    }

    private static TransactionDataSource instance;

    public static TransactionDataSource getInstance(Context context) {
        if (instance == null) {
            instance = new TransactionDataSource(context);
        }
        return instance;
    }


    private TransactionDataSource(Context context) {
        dbHelper = BRSQLiteHelper.getInstance(context);
    }

    public BRTransactionEntity putTransaction(BRTransactionEntity transactionEntity) {
        Cursor cursor = null;
        try {
            database = openDatabase();
            ContentValues values = new ContentValues();
            values.put(BRSQLiteHelper.TX_COLUMN_ID, transactionEntity.getTxHash());
            values.put(BRSQLiteHelper.TX_BUFF, transactionEntity.getBuff());
            values.put(BRSQLiteHelper.TX_BLOCK_HEIGHT, transactionEntity.getBlockheight());
            values.put(BRSQLiteHelper.TX_TIME_STAMP, transactionEntity.getTimestamp());

            database.beginTransaction();
            database.insert(BRSQLiteHelper.TX_TABLE_NAME, null, values);
            cursor = database.query(BRSQLiteHelper.TX_TABLE_NAME,
                    allColumns, null, null, null, null, null);
            cursor.moveToFirst();
            BRTransactionEntity transactionEntity1 = cursorToTransaction(cursor);

            database.setTransactionSuccessful();
            for (OnTxAddedListener listener : listeners) {
                if (listener != null) listener.onTxAdded();
            }
            return transactionEntity1;
        } catch (Exception ex) {
            BRReportsManager.reportBug(ex);
            Log.e(TAG, "Error inserting into SQLite", ex);
            //Error in between database transaction
        } finally {
            database.endTransaction();
            closeDatabase();
            if (cursor != null) cursor.close();
        }
        return null;


    }

    public  void deleteAllTransactions() {
        try {
            database = openDatabase();
            database.delete(BRSQLiteHelper.TX_TABLE_NAME, null, null);
        } finally {
            closeDatabase();
        }
    }

    public  List<BRTransactionEntity> getAllTransactions() {
        List<BRTransactionEntity> transactions = new ArrayList<>();
        Cursor cursor = null;
        try {
            database = openDatabase();

            cursor = database.query(BRSQLiteHelper.TX_TABLE_NAME,
                    allColumns, null, null, null, null, null);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                BRTransactionEntity transactionEntity = cursorToTransaction(cursor);
                transactions.add(transactionEntity);
                cursor.moveToNext();
            }

        } finally {
            closeDatabase();
            if (cursor != null)
                cursor.close();
        }
        return transactions;
    }

    private BRTransactionEntity cursorToTransaction(Cursor cursor) {
        return new BRTransactionEntity(cursor.getBlob(1), cursor.getInt(2), cursor.getLong(3), cursor.getString(0));
    }

    public  void updateTxBlockHeight(String hash, int blockHeight, int timeStamp) {
        try {
            database = openDatabase();
            Log.e(TAG, "transaction updated with id: " + hash);
            String strFilter = "_id=\'" + hash + "\'";
            ContentValues args = new ContentValues();
            args.put(BRSQLiteHelper.TX_BLOCK_HEIGHT, blockHeight);
            args.put(BRSQLiteHelper.TX_TIME_STAMP, timeStamp);

//            Log.e(TAG, "updateTxBlockHeight: size before updating: " + getAllTransactions().size());
            database.update(BRSQLiteHelper.TX_TABLE_NAME, args, strFilter, null);
//            Log.e(TAG, "updateTxBlockHeight: size after updating: " + getAllTransactions().size());
        } finally {
            closeDatabase();
        }

    }

    public  void deleteTxByHash(String hash) {
        try {
            database = openDatabase();
            Log.e(TAG, "transaction deleted with id: " + hash);
            database.delete(BRSQLiteHelper.TX_TABLE_NAME, BRSQLiteHelper.TX_COLUMN_ID
                    + " = \'" + hash + "\'", null);
        } finally {
            closeDatabase();
        }
    }

    @Override
    public  SQLiteDatabase openDatabase() {
        if(ActivityUTILS.isMainThread()) throw new NetworkOnMainThreadException();
//        if (mOpenCounter.incrementAndGet() == 1) {
        // Opening new database
        if (database == null || !database.isOpen())
            database = dbHelper.getWritableDatabase();
        dbHelper.setWriteAheadLoggingEnabled(BRConstants.WAL);
//        }
//        Log.d("Database open counter: ",  String.valueOf(mOpenCounter.get()));
        return database;
    }

    @Override
    public  void closeDatabase() {
//        if (mOpenCounter.decrementAndGet() == 0) {
//            // Closing database
//            database.close();

//        }
//        Log.d("Database open counter: " , String.valueOf(mOpenCounter.get()));
    }
}