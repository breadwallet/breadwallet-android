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
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.breadwallet.presenter.entities.BRTransactionEntity;
import com.google.firebase.crash.FirebaseCrash;

import java.util.ArrayList;
import java.util.List;

public class TransactionDataSource {
    private static final String TAG = TransactionDataSource.class.getName();

    // Database fields
    private SQLiteDatabase database;
    private final BRSQLiteHelper dbHelper;
    private final String[] allColumns = {
            BRSQLiteHelper.TX_COLUMN_ID,
            BRSQLiteHelper.TX_BUFF,
            BRSQLiteHelper.TX_BLOCK_HEIGHT,
            BRSQLiteHelper.TX_TIME_STAMP
    };


    public TransactionDataSource(Context context) {
        dbHelper = new BRSQLiteHelper(context);
    }

    public BRTransactionEntity createTransaction(BRTransactionEntity transactionEntity) {
        database = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(BRSQLiteHelper.TX_COLUMN_ID, transactionEntity.getTxHash());
        values.put(BRSQLiteHelper.TX_BUFF, transactionEntity.getBuff());
        values.put(BRSQLiteHelper.TX_BLOCK_HEIGHT, transactionEntity.getBlockheight());
        values.put(BRSQLiteHelper.TX_TIME_STAMP, transactionEntity.getTimestamp());

        database.beginTransaction();
        try {
            database.insert(BRSQLiteHelper.TX_TABLE_NAME, null, values);
            Cursor cursor = database.query(BRSQLiteHelper.TX_TABLE_NAME,
                    allColumns, null, null, null, null, null);
            cursor.moveToFirst();
            BRTransactionEntity transactionEntity1 = cursorToTransaction(cursor);
            cursor.close();
            database.setTransactionSuccessful();
            return transactionEntity1;
        } catch (Exception ex) {
            FirebaseCrash.report(ex);
            Log.e(TAG, "Error inserting into SQLite", ex);
            //Error in between database transaction
        } finally {
            database.endTransaction();
        }
        return null;


    }

    public void deleteTransaction(BRTransactionEntity transaction) {
        database = dbHelper.getWritableDatabase();
        String strHash = transaction.getTxHash();
        Log.e(TAG, "transaction deleted with id: " + strHash);
        database.delete(BRSQLiteHelper.TX_TABLE_NAME, BRSQLiteHelper.TX_COLUMN_ID
                + " = \'" + strHash + "\'", null);
    }

    public void deleteAllTransactions() {
        database = dbHelper.getWritableDatabase();
        database.delete(BRSQLiteHelper.TX_TABLE_NAME, null, null);
    }

    public List<BRTransactionEntity> getAllTransactions() {
        database = dbHelper.getReadableDatabase();
        List<BRTransactionEntity> transactions = new ArrayList<>();

        Cursor cursor = database.query(BRSQLiteHelper.TX_TABLE_NAME,
                allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            BRTransactionEntity transactionEntity = cursorToTransaction(cursor);
            transactions.add(transactionEntity);
            cursor.moveToNext();
        }
        // make sure to close the cursor

        Log.e(TAG, "transactions: " + transactions.size());
        cursor.close();
        return transactions;
    }

    private BRTransactionEntity cursorToTransaction(Cursor cursor) {
        return new BRTransactionEntity(cursor.getBlob(1), cursor.getInt(2), cursor.getLong(3), cursor.getString(0));
    }

    public void updateTxBlockHeight(String hash, int blockHeight, int timeStamp) {
        database = dbHelper.getWritableDatabase();
        Log.e(TAG, "transaction deleted with id: " + hash);
        String strFilter = "_id=\'" + hash + "\'";
        ContentValues args = new ContentValues();
        args.put(BRSQLiteHelper.TX_BLOCK_HEIGHT, blockHeight);
        args.put(BRSQLiteHelper.TX_TIME_STAMP, timeStamp);

        database.update(BRSQLiteHelper.TX_TABLE_NAME, args, strFilter, null);
    }

    public void deleteTxByHash(String hash) {
        database = dbHelper.getWritableDatabase();
        Log.e(TAG, "transaction deleted with id: " + hash);
        database.delete(BRSQLiteHelper.TX_TABLE_NAME, BRSQLiteHelper.TX_COLUMN_ID
                + " = \'" + hash + "\'", null);
    }
}