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

import com.breadwallet.presenter.entities.BRMerkleBlockEntity;
import com.breadwallet.presenter.entities.BlockEntity;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.util.BRConstants;

import java.util.ArrayList;
import java.util.List;

public class MerkleBlockDataSource implements BRDataSourceInterface {
    private static final String TAG = MerkleBlockDataSource.class.getName();

    // Database fields
    private SQLiteDatabase database;
    private final BRSQLiteHelper dbHelper;
    private final String[] allColumns = {
            BRSQLiteHelper.MB_COLUMN_ID,
            BRSQLiteHelper.MB_BUFF,
            BRSQLiteHelper.MB_HEIGHT,
            BRSQLiteHelper.MB_ISO
    };

    private static MerkleBlockDataSource instance;

    public static MerkleBlockDataSource getInstance(Context context) {
        if (instance == null) {
            instance = new MerkleBlockDataSource(context);
        }
        return instance;
    }

    private MerkleBlockDataSource(Context context) {
        dbHelper = BRSQLiteHelper.getInstance(context);
    }

    public void putMerkleBlocks(Context app, String iso, BlockEntity[] blockEntities) {
        try {
            database = openDatabase();
            database.beginTransaction();
            for (BlockEntity b : blockEntities) {
                ContentValues values = new ContentValues();
                values.put(BRSQLiteHelper.MB_BUFF, b.getBlockBytes());
                values.put(BRSQLiteHelper.MB_HEIGHT, b.getBlockHeight());
                values.put(BRSQLiteHelper.MB_ISO, iso.toUpperCase());
                database.insert(BRSQLiteHelper.MB_TABLE_NAME, null, values);
            }
            database.setTransactionSuccessful();
        } catch (Exception ex) {
            BRReportsManager.reportBug(ex);
            Log.e(TAG, "Error inserting into SQLite", ex);
            //Error in between database transaction
        } finally {
            database.endTransaction();
            closeDatabase();
        }
    }

    public void deleteAllBlocks(Context app, String iso) {
        try {
            database = openDatabase();
            database.delete(BRSQLiteHelper.MB_TABLE_NAME, BRSQLiteHelper.MB_ISO + "=?", new String[]{iso.toUpperCase()});
        } finally {
            closeDatabase();
        }
    }

    public void deleteMerkleBlock(Context app, String iso, BRMerkleBlockEntity merkleBlock) {
        try {
            database = openDatabase();
            long id = merkleBlock.getId();
            Log.e(TAG, "MerkleBlock deleted with id: " + id);
            database.delete(BRSQLiteHelper.MB_TABLE_NAME, BRSQLiteHelper.MB_COLUMN_ID
                    + " = ? AND " + BRSQLiteHelper.MB_ISO + " = ?", new String[]{String.valueOf(id), iso.toUpperCase()});
        } finally {
            closeDatabase();
        }
    }

    public List<BRMerkleBlockEntity> getAllMerkleBlocks(Context app, String iso) {
        List<BRMerkleBlockEntity> merkleBlocks = new ArrayList<>();
        Cursor cursor = null;
        try {
            database = openDatabase();

            cursor = database.query(BRSQLiteHelper.MB_TABLE_NAME,
                    allColumns, BRSQLiteHelper.MB_ISO + "=?", new String[]{iso.toUpperCase()},
                    null, null, null);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                BRMerkleBlockEntity merkleBlockEntity = cursorToMerkleBlock(cursor);
                merkleBlocks.add(merkleBlockEntity);
                cursor.moveToNext();
            }
            Log.e(TAG, "merkleBlocks: " + merkleBlocks.size());
            // make sure to close the cursor

        } finally {
            closeDatabase();
            if (cursor != null) cursor.close();
        }
        return merkleBlocks;
    }

    private BRMerkleBlockEntity cursorToMerkleBlock(Cursor cursor) {
        BRMerkleBlockEntity merkleBlockEntity = new BRMerkleBlockEntity(cursor.getBlob(1), cursor.getInt(2));
        merkleBlockEntity.setId(cursor.getInt(0));

        return merkleBlockEntity;
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
//            database.close();
//
//        }
//        Log.d("Database open counter: " , String.valueOf(mOpenCounter.get()));
    }
}