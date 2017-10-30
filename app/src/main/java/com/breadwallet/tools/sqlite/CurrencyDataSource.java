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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.breadwallet.presenter.entities.CurrencyEntity;
import com.google.firebase.crash.FirebaseCrash;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private Map<String, CurrencyEntity> currencyEntityMap = Collections.synchronizedMap(new HashMap<String, CurrencyEntity>());

    public static CurrencyDataSource getInstance() {
        if (instance == null) {
            instance = new CurrencyDataSource();
            instance.currencyEntityMap = instance.getCurrencyMap();
        }
        return instance;
    }

    private CurrencyDataSource() {
        dbHelper = BRSQLiteHelper.getInstance();
    }

    public void putCurrencies(Collection<CurrencyEntity> currencyEntities) {
        Log.d(TAG, "putCurrencies");
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

        // update currency map
        currencyEntityMap = getCurrencyMap();

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
        Log.d(TAG, "deleteAllCurrencies");
        database = dbHelper.getWritableDatabase();
        database.delete(BRSQLiteHelper.CURRENCY_TABLE_NAME, BRSQLiteHelper.PEER_COLUMN_ID + " <> -1", null);
    }

    private Map<String, CurrencyEntity> getCurrencyMap() {
        Log.d(TAG, "getCurrencyMap");
        database = dbHelper.getReadableDatabase();
        Map<String, CurrencyEntity> currencies = new HashMap<>();

        Cursor cursor = database.query(BRSQLiteHelper.CURRENCY_TABLE_NAME,
                allColumns, null, null, null, null, "\'" + BRSQLiteHelper.CURRENCY_CODE + "\'");

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            CurrencyEntity curEntity = cursorToCurrency(cursor);
            currencies.put(curEntity.code, curEntity);
            cursor.moveToNext();
        }
        // make sure to close the cursor

        cursor.close();
        Log.d(TAG, "getCurrencyMap: " + currencies.size());
        return currencies;
    }

    public List<CurrencyEntity> getAllCurrencies() {
        List<CurrencyEntity> allCurrencies = new ArrayList<>();
        for (CurrencyEntity currencyEntity : currencyEntityMap.values()) {
            allCurrencies.add(currencyEntity);
        }
        return allCurrencies;
    }

    public List<String> getAllISOs() {
        Log.d(TAG, "getAllISOs");
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
        return currencyEntityMap.get(iso);
    }

    private CurrencyEntity cursorToCurrency(Cursor cursor) {
        return new CurrencyEntity(cursor.getString(0), cursor.getString(1), cursor.getFloat(2));
    }
}