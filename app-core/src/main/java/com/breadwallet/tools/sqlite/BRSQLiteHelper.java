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
package com.breadwallet.tools.sqlite;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class BRSQLiteHelper extends SQLiteOpenHelper {
    private static final String TAG = BRSQLiteHelper.class.getName();
    private static BRSQLiteHelper instance;

    private BRSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static BRSQLiteHelper getInstance(Context context) {
        if (instance == null) instance = new BRSQLiteHelper(context);
        return instance;
    }

    public static final String DATABASE_NAME = "breadwallet.db";
    private static final int DATABASE_VERSION = 15;

    /**
     * MerkleBlock table
     */
    public static final String MB_TABLE_NAME_OLD = "merkleBlockTable";
    public static final String MB_TABLE_NAME = "merkleBlockTable_v2";

    /**
     * Transaction table
     */

    public static final String TX_TABLE_NAME_OLD = "transactionTable";
    public static final String TX_TABLE_NAME = "transactionTable_v2";
    public static final String TX_BUFF = "transactionBuff";
    public static final String TX_BLOCK_HEIGHT = "transactionBlockHeight";

    /**
     * Peer table
     */

    public static final String PEER_TABLE_NAME_OLD = "peerTable";
    public static final String PEER_ADDRESS = "peerAddress";

    /**
     * Currency table
     */

    public static final String CURRENCY_TABLE_NAME_OLD = "currencyTable";
    public static final String CURRENCY_TABLE_NAME = "currencyTable_v2";
    public static final String CURRENCY_CODE = "code";
    public static final String CURRENCY_NAME = "name";
    public static final String CURRENCY_RATE = "rate";
    public static final String CURRENCY_ISO = "iso";//iso for the currency of exchange (BTC, BCH, ETH)

    private static final String CURRENCY_DATABASE_CREATE = "create table if not exists " + CURRENCY_TABLE_NAME + " (" +
            CURRENCY_CODE + " text," +
            CURRENCY_NAME + " text," +
            CURRENCY_RATE + " integer," +
            CURRENCY_ISO + " text DEFAULT 'BTC', " +
            "PRIMARY KEY (" + CURRENCY_CODE + ", " + CURRENCY_ISO + ")" +
            ");";


    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(CURRENCY_DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 13 && (newVersion >= 13)) {
            boolean migrationNeeded = !tableExists(MB_TABLE_NAME, db);
            onCreate(db); //create new db tables

            if (migrationNeeded)
                migrateDatabases(db);
        }
    }

    private void migrateDatabases(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            if (tableExists(CURRENCY_TABLE_NAME_OLD, db) && tableExists(CURRENCY_TABLE_NAME, db))
                db.execSQL("INSERT INTO " + CURRENCY_TABLE_NAME + " (code, name, rate) SELECT code, name, rate FROM " + CURRENCY_TABLE_NAME_OLD);

            db.execSQL("DROP TABLE IF EXISTS " + PEER_TABLE_NAME_OLD); //drop this table (fully refactored schema)
            db.execSQL("DROP TABLE IF EXISTS " + MB_TABLE_NAME_OLD);
            db.execSQL("DROP TABLE IF EXISTS " + TX_TABLE_NAME_OLD);
            db.execSQL("DROP TABLE IF EXISTS " + CURRENCY_TABLE_NAME_OLD);

            db.setTransactionSuccessful();
            Log.e(TAG, "migrateDatabases: SUCCESS");
        } catch (SQLiteException ex) {

        } finally {
            Log.e(TAG, "migrateDatabases: ENDED");
            db.endTransaction();
        }
    }

    public boolean tableExists(String tableName, SQLiteDatabase db) {

        Cursor cursor = db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '" + tableName + "'", null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.close();
                return true;
            }
            cursor.close();
        }
        return false;
    }

    public void printTableStructures(SQLiteDatabase db, String tableName) {
        Log.e(TAG, "printTableStructures: " + tableName);
        String tableString = String.format("Table %s:\n", tableName);
        Cursor allRows = db.rawQuery("SELECT * FROM " + tableName, null);
        if (allRows.moveToFirst()) {
            String[] columnNames = allRows.getColumnNames();
            do {
                for (String name : columnNames) {
                    tableString += String.format("%s: %s\n", name,
                            allRows.getString(allRows.getColumnIndex(name)));
                }
                tableString += "\n";

            } while (allRows.moveToNext());
        }

        Log.e(TAG, "SQL:" + tableString);
    }

}
