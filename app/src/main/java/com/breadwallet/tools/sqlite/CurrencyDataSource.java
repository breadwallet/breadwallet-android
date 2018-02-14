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
import com.breadwallet.wallet.abstracts.BaseWalletManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CurrencyDataSource implements BRDataSourceInterface {
    private static final String TAG = CurrencyDataSource.class.getName();

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

    public void putCurrencies(Context app, BaseWalletManager walletManager, Collection<CurrencyEntity> currencyEntities) {
        if (currencyEntities == null || currencyEntities.size() <= 0) {
            Log.e(TAG, "putCurrencies: failed: " + currencyEntities);
            return;
        }

        try {
            database = openDatabase();
            database.beginTransaction();
            String iso = walletManager.getIso(app);
            for (CurrencyEntity c : currencyEntities) {
                ContentValues values = new ContentValues();
                values.put(BRSQLiteHelper.CURRENCY_CODE, c.code);
                values.put(BRSQLiteHelper.CURRENCY_NAME, c.name);
                values.put(BRSQLiteHelper.CURRENCY_RATE, c.rate);
                values.put(BRSQLiteHelper.CURRENCY_ISO, iso);
                database.insertWithOnConflict(BRSQLiteHelper.CURRENCY_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);

            }
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

    public void deleteAllCurrencies(Context app, BaseWalletManager walletManager) {
        try {
            database = openDatabase();
            database.delete(BRSQLiteHelper.CURRENCY_TABLE_NAME, BRSQLiteHelper.CURRENCY_ISO + " = ?", new String[]{walletManager.getIso(app)});
            for (OnDataChanged list : onDataChangedListeners) if (list != null) list.onChanged();
        } finally {
            closeDatabase();
        }
    }

    public List<CurrencyEntity> getAllCurrencies(Context app, BaseWalletManager walletManager) {

        List<CurrencyEntity> currencies = new ArrayList<>();
        Cursor cursor = null;
        try {
            database = openDatabase();

            cursor = database.query(BRSQLiteHelper.CURRENCY_TABLE_NAME,
                    allColumns, BRSQLiteHelper.CURRENCY_ISO + " = ?", new String[]{walletManager.getIso(app)}, null, null, "\'" + BRSQLiteHelper.CURRENCY_CODE + "\'");

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                CurrencyEntity curEntity = cursorToCurrency(cursor);
                currencies.add(curEntity);
                cursor.moveToNext();
            }
            // make sure to close the cursor
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }

        return currencies;
    }

    public List<String> getAllCurrencyCodes(Context app, BaseWalletManager walletManager) {
        List<String> ISOs = new ArrayList<>();
        Cursor cursor = null;
        try {
            database = openDatabase();

            cursor = database.query(BRSQLiteHelper.CURRENCY_TABLE_NAME,
                    allColumns, BRSQLiteHelper.CURRENCY_ISO + " = ?", new String[]{walletManager.getIso(app)},
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

    public CurrencyEntity getCurrencyByCode(Context app, BaseWalletManager walletManager, String code) {
        Cursor cursor = null;
        try {
            database = openDatabase();

            cursor = database.query(BRSQLiteHelper.CURRENCY_TABLE_NAME,
                    allColumns, BRSQLiteHelper.CURRENCY_CODE + " = ? AND " + BRSQLiteHelper.CURRENCY_ISO + " = ?",
                    new String[]{code, walletManager.getIso(app)}, null, null, null);

            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                return cursorToCurrency(cursor);
            }
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }

        return null;
    }

    private CurrencyEntity cursorToCurrency(Cursor cursor) {
        return new CurrencyEntity(cursor.getString(0), cursor.getString(1), cursor.getFloat(2));
    }

    @Override
    public SQLiteDatabase openDatabase() {
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