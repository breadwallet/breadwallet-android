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
import android.util.Log;

import com.breadwallet.presenter.entities.BRPeerEntity;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.entities.PeerEntity;
import com.google.firebase.crash.FirebaseCrash;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class CurrencyDataSource {
    private static final String TAG = CurrencyDataSource.class.getName();

    // Database fields
    private SQLiteDatabase database;
    private final BRSQLiteHelper dbHelper;
    private final String[] allColumns = {
            BRSQLiteHelper.CURRENCY_CODE,
            BRSQLiteHelper.CURRENCY_NAME,
            BRSQLiteHelper.CURRENCY_RATE
    };

    private static CurrencyDataSource instance;

    public static CurrencyDataSource getInstance(Context context) {
        if (instance == null) {
            instance = new CurrencyDataSource(context);
        }
        return instance;
    }

    public CurrencyDataSource(Context context) {
        dbHelper = BRSQLiteHelper.getInstance(context);
    }

    public void putCurrencies(Collection<CurrencyEntity> currencyEntities) {
        if (currencyEntities == null) return;
        database = dbHelper.getWritableDatabase();
        database.beginTransaction();
        try {
            for (CurrencyEntity c : currencyEntities) {
                ContentValues values = new ContentValues();
                values.put(BRSQLiteHelper.CURRENCY_CODE, c.code);
                values.put(BRSQLiteHelper.CURRENCY_NAME, c.name);
                values.put(BRSQLiteHelper.CURRENCY_RATE, c.rate);
                database.insertWithOnConflict(BRSQLiteHelper.CURRENCY_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);

            }

            database.setTransactionSuccessful();
        } catch (Exception ex) {
            FirebaseCrash.report(ex);
            Log.e(TAG, "Error inserting into SQLite", ex);
            //Error in between database transaction
        } finally {
            database.endTransaction();
        }

    }
//    public void putCurrencies(Set<CurrencyEntity> currencyEntities) {
//        if(currencyEntities == null) return;
//        database = dbHelper.getWritableDatabase();
//        database.beginTransaction();
//        try {
//            for (CurrencyEntity c : currencyEntities) {
//                Log.e(TAG,"sqlite peer saved: " + Arrays.toString(p.getPeerTimeStamp()));
//                ContentValues values = new ContentValues();
//                values.put(BRSQLiteHelper.CURRENCY_CODE, c.code);
//                values.put(BRSQLiteHelper.CURRENCY_NAME, c.name);
//                values.put(BRSQLiteHelper.CURRENCY_RATE, c.rate);
//                database.insert(BRSQLiteHelper.CURRENCY_TABLE_NAME, null, values);
//            }
//
//            database.setTransactionSuccessful();
//        } catch (Exception ex) {
//            FirebaseCrash.report(ex);
//            Log.e(TAG, "Error inserting into SQLite", ex);
//            //Error in between database transaction
//        } finally {
//            database.endTransaction();
//        }
//
//    }

//    public void deleteCurrency(BRPeerEntity peerEntity) {
//        database = dbHelper.getWritableDatabase();
//        long id = peerEntity.getId();
//        Log.e(TAG, "Peer deleted with id: " + id);
//        database.delete(BRSQLiteHelper.PEER_TABLE_NAME, BRSQLiteHelper.PEER_COLUMN_ID
//                + " = " + id, null);
//    }

    public void deleteAllCurrencies() {
        database = dbHelper.getWritableDatabase();
        database.delete(BRSQLiteHelper.CURRENCY_TABLE_NAME, BRSQLiteHelper.PEER_COLUMN_ID + " <> -1", null);
    }

    public List<CurrencyEntity> getAllCurrencies() {
        database = dbHelper.getReadableDatabase();
        List<CurrencyEntity> currencies = new ArrayList<>();

        Cursor cursor = database.query(BRSQLiteHelper.CURRENCY_TABLE_NAME,
                allColumns, null, null, null, null, "\'" + BRSQLiteHelper.CURRENCY_CODE + "\'");

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            CurrencyEntity curEntity = cursorToCurrency(cursor);
            currencies.add(curEntity);
            cursor.moveToNext();
        }
        // make sure to close the cursor

        cursor.close();
        Log.e(TAG, "getAllCurrencies: " + currencies.size());
        return currencies;
    }

    public List<String> getAllISOs() {
        database = dbHelper.getReadableDatabase();
        List<String> ISOs = new ArrayList<>();

        Cursor cursor = database.query(BRSQLiteHelper.CURRENCY_TABLE_NAME,
                allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            CurrencyEntity curEntity = cursorToCurrency(cursor);
            ISOs.add(curEntity.code);
            cursor.moveToNext();
        }
        // make sure to close the cursor

        cursor.close();
        return ISOs;
    }

    public CurrencyEntity getCurrencyByIso(String iso) {
        database = dbHelper.getReadableDatabase();

        Cursor cursor = database.query(BRSQLiteHelper.CURRENCY_TABLE_NAME,
                allColumns, BRSQLiteHelper.CURRENCY_CODE + "=\'" + iso + "\'", null, null, null, null);

        try {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                return cursorToCurrency(cursor);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return null;
    }

    private CurrencyEntity cursorToCurrency(Cursor cursor) {
        return new CurrencyEntity(cursor.getString(0), cursor.getString(1), cursor.getFloat(2));
    }
}