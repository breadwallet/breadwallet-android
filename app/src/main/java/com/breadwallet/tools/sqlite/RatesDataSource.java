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

import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RatesDataSource implements BRDataSourceInterface {
    private static final String TAG = RatesDataSource.class.getName();

    List<OnDataChanged> onDataChangedListeners = new ArrayList<>();

    // Database fields
    private SQLiteDatabase database;
    private final BRSQLiteHelper dbHelper;
    private final String[] allColumns = {
            BRSQLiteHelper.CURRENCY_CODE,
            BRSQLiteHelper.CURRENCY_NAME,
            BRSQLiteHelper.CURRENCY_RATE,
            BRSQLiteHelper.CURRENCY_ISO,
    };

    private static RatesDataSource instance;

    public static RatesDataSource getInstance(Context context) {
        if (instance == null) {
            instance = new RatesDataSource(context);
        }
        return instance;
    }

    public RatesDataSource(Context context) {
        dbHelper = BRSQLiteHelper.getInstance(context);
    }

    public void putCurrencies(Context app, Collection<CurrencyEntity> currencyEntities) {
        if (currencyEntities == null || currencyEntities.size() <= 0) {
            Log.e(TAG, "putCurrencies: failed: " + currencyEntities);
            return;
        }

        try {
            database = openDatabase();
            database.beginTransaction();
            int failed = 0;
            for (CurrencyEntity c : currencyEntities) {
                ContentValues values = new ContentValues();
                if (Utils.isNullOrEmpty(c.code) || Utils.isNullOrEmpty(c.name) || c.rate <= 0) {
                    failed++;
                    continue;
                }
                values.put(BRSQLiteHelper.CURRENCY_CODE, c.code);
                values.put(BRSQLiteHelper.CURRENCY_NAME, c.name);
                values.put(BRSQLiteHelper.CURRENCY_RATE, c.rate);
                values.put(BRSQLiteHelper.CURRENCY_ISO, c.iso);
                long length = database.insertWithOnConflict(BRSQLiteHelper.CURRENCY_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                Log.i(TAG, "length:"+length);
            }
            if (failed != 0) Log.e(TAG, "putCurrencies: failed:" + failed);
            database.setTransactionSuccessful();
            for (OnDataChanged list : onDataChangedListeners) if (list != null) list.onChanged();
        } catch (Exception ex) {
            Log.e(TAG, "putCurrencies: failed: ", ex);
            BRReportsManager.reportBug(ex);

            //Error in between database transaction
        } finally {
            database.endTransaction();
            closeDatabase();
        }
    }

    public void deleteAllCurrencies(Context app, String iso) {
        try {
            database = openDatabase();
            database.delete(BRSQLiteHelper.CURRENCY_TABLE_NAME, BRSQLiteHelper.CURRENCY_ISO + " = ?", new String[]{iso.toUpperCase()});
            for (OnDataChanged list : onDataChangedListeners) if (list != null) list.onChanged();
        } finally {
            closeDatabase();
        }
    }

    public List<CurrencyEntity> getAllCurrencies(Context app, String iso) {

        List<CurrencyEntity> currencies = new ArrayList<>();
        Cursor cursor = null;
        try {
            database = openDatabase();

            cursor = database.query(BRSQLiteHelper.CURRENCY_TABLE_NAME, allColumns, BRSQLiteHelper.CURRENCY_ISO + " = ? COLLATE NOCASE",
                    new String[]{iso.toUpperCase()}, null, null, "\'" + BRSQLiteHelper.CURRENCY_CODE + "\'");

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                CurrencyEntity curEntity = cursorToCurrency(cursor);
                if (!WalletsMaster.getInstance(app).isIsoCrypto(app, curEntity.code))
                    currencies.add(curEntity);
                cursor.moveToNext();
            }
            // make sure to close the cursor
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }
        Log.e(TAG, "getAllCurrencies: size:" + currencies.size());
        return currencies;
    }

    public List<String> getAllCurrencyCodes(Context app, String iso) {
        List<String> ISOs = new ArrayList<>();
        Cursor cursor = null;
        try {
            database = openDatabase();

            cursor = database.query(BRSQLiteHelper.CURRENCY_TABLE_NAME,
                    allColumns, BRSQLiteHelper.CURRENCY_ISO + " = ? COLLATE NOCASE", new String[]{iso.toUpperCase()},
                    null, null, null);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                CurrencyEntity curEntity = cursorToCurrency(cursor);
                ISOs.add(curEntity.code);
                cursor.moveToNext();
            }
            // make sure to close the cursor
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }

        return ISOs;
    }

    public synchronized CurrencyEntity getCurrencyByCode(Context app, String iso, String code) {
        Cursor cursor = null;
        CurrencyEntity result = null;
        try {
            database = openDatabase();
//            printTest();
//            Log.e(TAG, "getCurrencyByCode: code: " + code + ", iso: " + walletManager.getIso(app));
            cursor = database.query(BRSQLiteHelper.CURRENCY_TABLE_NAME,
                    allColumns, BRSQLiteHelper.CURRENCY_CODE + " = ? AND " + BRSQLiteHelper.CURRENCY_ISO + " = ? COLLATE NOCASE",
                    new String[]{code, iso.toUpperCase()}, null, null, null);

            cursor.moveToNext();
            if (!cursor.isAfterLast()) {
                result = cursorToCurrency(cursor);
            }

            return result;
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }
    }

    private void printTest() {
        Cursor cursor = null;
        try {
            database = openDatabase();
            StringBuilder builder = new StringBuilder();

            cursor = database.query(BRSQLiteHelper.CURRENCY_TABLE_NAME,
                    allColumns, null, null, null, null, null);
            builder.append("Total: " + cursor.getCount() + "\n");
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                CurrencyEntity ent = cursorToCurrency(cursor);
                builder.append("Name: " + ent.name + ", code: " + ent.code + ", rate: " + ent.rate + ", iso: " + ent.iso + "\n");
                cursor.moveToNext();
            }
            Log.e(TAG, "printTest: " + builder.toString());
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }
    }

    private CurrencyEntity cursorToCurrency(Cursor cursor) {
        return new CurrencyEntity(cursor.getString(0), cursor.getString(1), cursor.getFloat(2), cursor.getString(3));
    }

    @Override
    public SQLiteDatabase openDatabase() {
//        if (mOpenCounter.incrementAndGet() == 1) {
        // Opening new database
        if (database == null || !database.isOpen())
            database = dbHelper.getWritableDatabase();
        dbHelper.setWriteAheadLoggingEnabled(BRConstants.WRITE_AHEAD_LOGGING);
//        }
//        Log.d("Database open counter: ",  String.valueOf(mOpenCounter.get()));
        return database;
    }

    @Override
    public void closeDatabase() {
//        if (mOpenCounter.decrementAndGet() == 0) {
//            // Closing database
//        database.close();
//
//        }
//        Log.d("Database open counter: " , String.valueOf(mOpenCounter.get()));
    }

    public void addOnDataChangedListener(OnDataChanged list) {
        if (!onDataChangedListeners.contains(list)) {
            onDataChangedListeners.add(list);
        }
    }

    public interface OnDataChanged {
        void onChanged();
    }
}