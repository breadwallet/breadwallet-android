package com.breadwallet.tools.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.breadwallet.BuildConfig;
import com.breadwallet.presenter.entities.BRTransactionEntity;
import com.breadwallet.tools.manager.BRReportsManager;

import java.util.ArrayList;
import java.util.List;

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
    public static final String MB_COLUMN_ID = "_id";
    public static final String MB_BUFF = "merkleBlockBuff";
    public static final String MB_HEIGHT = "merkleBlockHeight";
    public static final String MB_ISO = "merkleBlockIso";

    private static final String MB_DATABASE_CREATE = "create table if not exists " + MB_TABLE_NAME + " (" +
            MB_COLUMN_ID + " integer primary key autoincrement, " +
            MB_BUFF + " blob, " +
            MB_ISO + " text DEFAULT 'BTC' , " +
            MB_HEIGHT + " integer);";

    /**
     * Transaction table
     */

    public static final String TX_TABLE_NAME_OLD = "transactionTable";
    public static final String TX_TABLE_NAME = "transactionTable_v2";
    public static final String TX_COLUMN_ID = "_id";
    public static final String TX_BUFF = "transactionBuff";
    public static final String TX_BLOCK_HEIGHT = "transactionBlockHeight";
    public static final String TX_TIME_STAMP = "transactionTimeStamp";
    public static final String TX_ISO = "transactionISO";

    private static final String TX_DATABASE_CREATE = "create table if not exists " + TX_TABLE_NAME + " (" +
            TX_COLUMN_ID + " text, " +
            TX_BUFF + " blob, " +
            TX_BLOCK_HEIGHT + " integer, " +
            TX_TIME_STAMP + " integer, " +
            TX_ISO + " text DEFAULT 'BTC' );";

    /**
     * Peer table
     */

    public static final String PEER_TABLE_NAME_OLD = "peerTable";
    public static final String PEER_TABLE_NAME = "peerTable_v2";
    public static final String PEER_COLUMN_ID = "_id";
    public static final String PEER_ADDRESS = "peerAddress";
    public static final String PEER_PORT = "peerPort";
    public static final String PEER_TIMESTAMP = "peerTimestamp";
    public static final String PEER_ISO = "peerIso";

    private static final String PEER_DATABASE_CREATE = "create table if not exists " + PEER_TABLE_NAME + " (" +
            PEER_COLUMN_ID + " integer primary key autoincrement, " +
            PEER_ADDRESS + " blob," +
            PEER_PORT + " blob," +
            PEER_TIMESTAMP + " blob," +
            PEER_ISO + "  text default 'BTC');";
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
        //drop peers table due to multiple changes

        Log.e(TAG, "onCreate: " + MB_DATABASE_CREATE);
        Log.e(TAG, "onCreate: " + TX_DATABASE_CREATE);
        Log.e(TAG, "onCreate: " + PEER_DATABASE_CREATE);
        Log.e(TAG, "onCreate: " + CURRENCY_DATABASE_CREATE);
        database.execSQL(MB_DATABASE_CREATE);
        database.execSQL(TX_DATABASE_CREATE);
        database.execSQL(PEER_DATABASE_CREATE);
        database.execSQL(CURRENCY_DATABASE_CREATE);

//        printTableStructures(database, MB_TABLE_NAME);
//        printTableStructures(database, TX_TABLE_NAME);
//        printTableStructures(database, PEER_TABLE_NAME);
//        printTableStructures(database, CURRENCY_TABLE_NAME);

//        database.execSQL("PRAGMA journal_mode=WRITE_AHEAD_LOGGING;");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        if (oldVersion < 13 && (newVersion >= 13)) {
            boolean migrationNeeded = !tableExists(MB_TABLE_NAME, db);
            onCreate(db); //create new db tables

            if (migrationNeeded)
                migrateDatabases(db);
        } else {
            //drop everything maybe?
//            db.execSQL("DROP TABLE IF EXISTS " + MB_TABLE_NAME);
//            db.execSQL("DROP TABLE IF EXISTS " + TX_TABLE_NAME);
//            db.execSQL("DROP TABLE IF EXISTS " + PEER_TABLE_NAME);
//            db.execSQL("DROP TABLE IF EXISTS " + CURRENCY_TABLE_NAME);
//            db.execSQL("PRAGMA journal_mode=WRITE_AHEAD_LOGGING;");
        }
        //recreate if needed

    }

    private void migrateDatabases(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            db.execSQL("INSERT INTO " + MB_TABLE_NAME + " (_id, merkleBlockBuff, merkleBlockHeight) SELECT _id, merkleBlockBuff, merkleBlockHeight FROM " + MB_TABLE_NAME_OLD);
            db.execSQL("INSERT INTO " + TX_TABLE_NAME + " (_id, transactionBuff, transactionBlockHeight, transactionTimeStamp) SELECT _id, transactionBuff, transactionBlockHeight, transactionTimeStamp FROM " + TX_TABLE_NAME_OLD);
            if (tableExists(CURRENCY_TABLE_NAME_OLD, db) && tableExists(CURRENCY_TABLE_NAME, db))
                db.execSQL("INSERT INTO " + CURRENCY_TABLE_NAME + " (code, name, rate) SELECT code, name, rate FROM " + CURRENCY_TABLE_NAME_OLD);

            db.execSQL("DROP TABLE IF EXISTS " + PEER_TABLE_NAME_OLD); //drop this table (fully refactored schema)
            db.execSQL("DROP TABLE IF EXISTS " + MB_TABLE_NAME_OLD);
            db.execSQL("DROP TABLE IF EXISTS " + TX_TABLE_NAME_OLD);
            db.execSQL("DROP TABLE IF EXISTS " + CURRENCY_TABLE_NAME_OLD);

            copyTxsForBch(db);

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

    private void copyTxsForBch(SQLiteDatabase db) {
        List<BRTransactionEntity> transactions = new ArrayList<>();
        Cursor cursorGet = null;
        int bCashForkBlockHeight = BuildConfig.BITCOIN_TESTNET ? 1155876 : 478559;
        int bCashForkTimeStamp = BuildConfig.BITCOIN_TESTNET ? 1501597117 : 1501568580;
        db.beginTransaction();
        try {
            cursorGet = db.query(BRSQLiteHelper.TX_TABLE_NAME,
                    BtcBchTransactionDataStore.allColumns, BRSQLiteHelper.TX_ISO + "=? AND " + BRSQLiteHelper.TX_BLOCK_HEIGHT + " <?", new String[]{"BTC", String.valueOf(bCashForkBlockHeight)}, null, null, null);

            cursorGet.moveToFirst();
            while (!cursorGet.isAfterLast()) {
                BRTransactionEntity transactionEntity = BtcBchTransactionDataStore.cursorToTransaction(null, "BTC", cursorGet);
                transactions.add(transactionEntity);
                cursorGet.moveToNext();
            }

            int count = 0;
            for (BRTransactionEntity tx : transactions) {
                ContentValues values = new ContentValues();
                values.put(BRSQLiteHelper.TX_COLUMN_ID, tx.getTxHash());
                values.put(BRSQLiteHelper.TX_BUFF, tx.getBuff());
                values.put(BRSQLiteHelper.TX_BLOCK_HEIGHT, tx.getBlockheight());
                values.put(BRSQLiteHelper.TX_ISO, "BCH");
                values.put(BRSQLiteHelper.TX_TIME_STAMP, tx.getTimestamp());

                db.insert(BRSQLiteHelper.TX_TABLE_NAME, null, values);
                count++;

            }
            Log.e(TAG, "copyTxsForBch: copied: " + count);
            db.setTransactionSuccessful();

        } finally {
            if (cursorGet != null)
                cursorGet.close();
            db.endTransaction();
        }
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
