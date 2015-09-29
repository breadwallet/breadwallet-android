package com.breadwallet.tools.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

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
public class BRSQLiteHelper extends SQLiteOpenHelper {
    public static final String TAG = BRSQLiteHelper.class.getName();

    private static final String DATABASE_NAME = "breadwallet.db";
    private static final int DATABASE_VERSION = 3;

    /**
     * MerkleBlock table
     */
    public static final String MB_TABLE_NAME = "merkleBlockTable";
    public static final String MB_COLUMN_ID = "_id";
    public static final String MB_COLUMN_BLOCK_HASH = "blockHash";
    public static final String MB_COLUMN_FLAGS = "flags";
    public static final String MB_COLUMN_HASHES = "hashes";
    public static final String MB_COLUMN_HEIGHT = "height";
    public static final String MB_COLUMN_MERKLE_ROOT = "merkleRoot";
    public static final String MB_COLUMN_NONCE = "nonce";
    public static final String MB_COLUMN_PREV_BLOCK = "prevBlock";
    public static final String MB_COLUMN_TARGET = "target";
    public static final String MB_COLUMN_TIME_STAMP = "timeStamp";
    public static final String MB_COLUMN_TOTAL_TRANSACTIONS = "totalTransactions";
    public static final String MB_COLUMN_VERSION = "version";

    private static final String MB_DATABASE_CREATE = "create table " + MB_TABLE_NAME + "(" +
            MB_COLUMN_ID + " integer primary key autoincrement, " +
            MB_COLUMN_BLOCK_HASH + " blob, " +
            MB_COLUMN_FLAGS + " blob, " +
            MB_COLUMN_HASHES + " blob, " +
            MB_COLUMN_HEIGHT + " integer, " +
            MB_COLUMN_MERKLE_ROOT + " blob, " +
            MB_COLUMN_NONCE + " integer, " +
            MB_COLUMN_PREV_BLOCK + " blob, " +
            MB_COLUMN_TARGET + " integer, " +
            MB_COLUMN_TIME_STAMP + " integer, " +
            MB_COLUMN_TOTAL_TRANSACTIONS + " integer, " +
            MB_COLUMN_VERSION + " integer );";

    /**
     * Transaction table
     */

    public static final String TX_TABLE_NAME = "transactionTable";
    public static final String TX_COLUMN_ID = "_id";
    public static final String TX_BLOCK_HEIGHT = "blockHeight";
    public static final String TX_LOCK_TIME = "lockTime";
    public static final String TX_TIME_STAMP = "timeStamp";
    public static final String TX_HASH = "txHash";

    private static final String TX_DATABASE_CREATE = "create table " + TX_TABLE_NAME + "(" +
            TX_COLUMN_ID + " integer primary key autoincrement, " +
            TX_BLOCK_HEIGHT + " integer, " +
            TX_LOCK_TIME + " integer, " +
            TX_TIME_STAMP + " integer, " +
            TX_HASH + " blob );";

    /**
     * Peer table
     */

    public static final String PEER_TABLE_NAME = "peerTable";
    public static final String PEER_COLUMN_ID = "_id";
    public static final String PEER_ADDRESS = "address";
    public static final String PEER_MISBEHAVIN = "misbehavin";
    public static final String PEER_PORT = "port";
    public static final String PEER_SERVICES = "services";
    public static final String PEER_TIME_STAMP = "timeStamp";

    private static final String PEER_DATABASE_CREATE = "create table " + PEER_TABLE_NAME + "(" +
            PEER_COLUMN_ID + " integer primary key autoincrement, " +
            PEER_ADDRESS + " integer, " +
            PEER_MISBEHAVIN + " integer, " +
            PEER_PORT + " integer, " +
            PEER_SERVICES + " integer " +
            PEER_TIME_STAMP + " integer );";

    /**
     * Inputs table
     */

    public static final String IN_TABLE_NAME = "inputTable";
    public static final String IN_COLUMN_ID = "_id";
    public static final String IN_TX_HASH = "txHash";
    public static final String IN_INDEX = "index";
    public static final String IN_PREV_OUT_TX_HASH = "prevOutTxHash";
    public static final String IN_PREV_OUT_INDEX = "prevOutIndex";
    public static final String IN_SEQUENCE = "sequence";
    public static final String IN_SIGNATURE = "signature";

    private static final String INPUTS_DATABASE_CREATE = "create table " + IN_TABLE_NAME + "(" +
            IN_COLUMN_ID + " integer primary key autoincrement, " +
            IN_TX_HASH + " blob, " +
            IN_INDEX + " integer, " +
            IN_PREV_OUT_TX_HASH + " blob, " +
            IN_PREV_OUT_INDEX + " integer " +
            IN_SEQUENCE + " integer ," +
            IN_SIGNATURE + " blob );";

    /**
     * Outputs table
     */

    public static final String OUT_TABLE_NAME = "outputTable";
    public static final String OUT_COLUMN_ID = "_id";
    public static final String OUT_TX_HASH = "txHash";
    public static final String OUT_INDEX = "index";
    public static final String OUT_VALUE = "value";

    private static final String OUTPUTS_DATABASE_CREATE = "create table " + OUT_TABLE_NAME + "(" +
            OUT_COLUMN_ID + " integer primary key autoincrement, " +
            OUT_TX_HASH + " blob, " +
            OUT_INDEX + " integer, " +
            OUT_VALUE + " integer );";


    public BRSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(MB_DATABASE_CREATE);
        database.execSQL(TX_DATABASE_CREATE);
        database.execSQL(PEER_DATABASE_CREATE);
        database.execSQL(INPUTS_DATABASE_CREATE);
        database.execSQL(OUTPUTS_DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.e(TAG, "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + MB_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TX_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + PEER_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + IN_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + OUT_TABLE_NAME);
        //recreate the dbs
        onCreate(db);
    }
}
