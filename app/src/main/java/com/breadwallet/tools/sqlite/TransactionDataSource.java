package com.breadwallet.tools.sqlite;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 9/25/15.
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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.breadwallet.presenter.entities.BRTransactionEntity;

import java.util.ArrayList;
import java.util.List;

public class TransactionDataSource {
    public static final String TAG = TransactionDataSource.class.getName();

    // Database fields
    private SQLiteDatabase database;
    private BRSQLiteHelper dbHelper;
    private String[] allColumns = {
            BRSQLiteHelper.TX_COLUMN_ID,
            BRSQLiteHelper.TX_BUFF
//            BRSQLiteHelper.TX_BLOCK_HEIGHT, BRSQLiteHelper.TX_LOCK_TIME,
//            BRSQLiteHelper.TX_TIME_STAMP, BRSQLiteHelper.TX_HASH
    };

    public TransactionDataSource(Context context) {
        dbHelper = new BRSQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public BRTransactionEntity createTransaction(BRTransactionEntity transactionEntity) {
        ContentValues values = new ContentValues();
//        values.put(BRSQLiteHelper.TX_COLUMN_ID, transactionEntity.getId());
        values.put(BRSQLiteHelper.TX_BUFF, transactionEntity.getBuff());
//        values.put(BRSQLiteHelper.TX_BLOCK_HEIGHT, transactionEntity.getBlockHeight());
//        values.put(BRSQLiteHelper.TX_LOCK_TIME, transactionEntity.getLockTime());
//        values.put(BRSQLiteHelper.TX_TIME_STAMP, transactionEntity.getTimeStamp());
//        values.put(BRSQLiteHelper.TX_HASH, transactionEntity.getTxHash());

//        for (BRTxInputEntity input : transactionEntity.getInputs()) {
//            ContentValues inputValues = new ContentValues();
//            inputValues.put(BRSQLiteHelper.IN_INDEX, input.getIndex());
//            inputValues.put(BRSQLiteHelper.IN_PREV_OUT_INDEX, input.getPrevOutIndex());
//            inputValues.put(BRSQLiteHelper.IN_PREV_OUT_TX_HASH, input.getPrevOutTxHash());
//            inputValues.put(BRSQLiteHelper.IN_SEQUENCE, input.getSequence());
//            inputValues.put(BRSQLiteHelper.IN_SIGNATURE, input.getSignatures());

//            database.insert(BRSQLiteHelper.IN_TABLE_NAME, null, values);
//        }
//
//        for (BRTxOutputEntity output : transactionEntity.getOutputs()) {
//            ContentValues outputValues = new ContentValues();
//            outputValues.put(BRSQLiteHelper.OUT_INDEX, output.getIndex());
//            outputValues.put(BRSQLiteHelper.OUT_TX_HASH, output.getTxHash());
//            outputValues.put(BRSQLiteHelper.OUT_VALUE, output.getValue());
//
//            database.insert(BRSQLiteHelper.OUT_TABLE_NAME, null, values);
//        }

        database.beginTransaction();
        try {
            long insertId = database.insert(BRSQLiteHelper.TX_TABLE_NAME, null, values);
            Cursor cursor = database.query(BRSQLiteHelper.TX_TABLE_NAME,
                    allColumns, BRSQLiteHelper.TX_COLUMN_ID + " = " + insertId, null,
                    null, null, null);
            cursor.moveToFirst();
            BRTransactionEntity transactionEntity1 = cursorToTransaction(cursor);
            cursor.close();
            database.setTransactionSuccessful();
            return transactionEntity1;
        } catch (Exception ex) {
            Log.e(TAG, "Error inserting into SQLite", ex);
            //Error in between database transaction
        } finally {
            database.endTransaction();
        }
        return null;


    }

    public void deleteTransaction(BRTransactionEntity transaction) {
        long id = transaction.getId();
        Log.e(TAG, "transaction deleted with id: " + id);
        database.delete(BRSQLiteHelper.TX_TABLE_NAME, BRSQLiteHelper.TX_COLUMN_ID
                + " = " + id, null);
    }

    public void deleteAllTransactions(){
        database.delete(BRSQLiteHelper.TX_TABLE_NAME, BRSQLiteHelper.TX_COLUMN_ID + " <> -1", null);
    }

    public List<BRTransactionEntity> getAllTransactions() {
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
        cursor.close();
        return transactions;
    }

    private BRTransactionEntity cursorToTransaction(Cursor cursor) {
        BRTransactionEntity transactionEntity = new BRTransactionEntity(cursor.getBlob(1));
        transactionEntity.setId(cursor.getInt(0));
//        transactionEntity.setBlockHeight(cursor.getInt(1));
//        transactionEntity.setLockTime(cursor.getInt(2));
//        transactionEntity.setTimeStamp(cursor.getInt(3));
//        transactionEntity.setTxHash(cursor.getBlob(4));
        return transactionEntity;
    }

}