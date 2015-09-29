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

import com.breadwallet.tools.sqlite.entities.BRMerkleBlockEntity;

import java.util.ArrayList;
import java.util.List;

public class MerkleBlockDataSource {
    public static final String TAG = MerkleBlockDataSource.class.getName();

    // Database fields
    private SQLiteDatabase database;
    private BRSQLiteHelper dbHelper;
    private String[] allColumns = {BRSQLiteHelper.MB_COLUMN_ID,
            BRSQLiteHelper.MB_COLUMN_BLOCK_HASH, BRSQLiteHelper.MB_COLUMN_FLAGS,
            BRSQLiteHelper.MB_COLUMN_HASHES, BRSQLiteHelper.MB_COLUMN_HEIGHT,
            BRSQLiteHelper.MB_COLUMN_MERKLE_ROOT, BRSQLiteHelper.MB_COLUMN_NONCE,
            BRSQLiteHelper.MB_COLUMN_PREV_BLOCK, BRSQLiteHelper.MB_COLUMN_TARGET,
            BRSQLiteHelper.MB_COLUMN_TIME_STAMP, BRSQLiteHelper.MB_COLUMN_TOTAL_TRANSACTIONS,
            BRSQLiteHelper.MB_COLUMN_VERSION};

    public MerkleBlockDataSource(Context context) {
        dbHelper = new BRSQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public BRMerkleBlockEntity createMerkleBlock(BRMerkleBlockEntity merkleBlock) {
        ContentValues values = new ContentValues();
//        values.put(BRSQLiteHelper.MB_COLUMN_ID, merkleBlock.getId());
        values.put(BRSQLiteHelper.MB_COLUMN_BLOCK_HASH, merkleBlock.getBlockHash());
        values.put(BRSQLiteHelper.MB_COLUMN_FLAGS, merkleBlock.getFlags());
        values.put(BRSQLiteHelper.MB_COLUMN_HASHES, merkleBlock.getHashes());
        values.put(BRSQLiteHelper.MB_COLUMN_HEIGHT, merkleBlock.getHeight());
        values.put(BRSQLiteHelper.MB_COLUMN_MERKLE_ROOT, merkleBlock.getMerkleRoot());
        values.put(BRSQLiteHelper.MB_COLUMN_NONCE, merkleBlock.getNonce());
        values.put(BRSQLiteHelper.MB_COLUMN_PREV_BLOCK, merkleBlock.getPrevBlock());
        values.put(BRSQLiteHelper.MB_COLUMN_TARGET, merkleBlock.getTarget());
        values.put(BRSQLiteHelper.MB_COLUMN_TIME_STAMP, merkleBlock.getTimeStamp());
        values.put(BRSQLiteHelper.MB_COLUMN_TOTAL_TRANSACTIONS, merkleBlock.getTotalTransactions());
        values.put(BRSQLiteHelper.MB_COLUMN_VERSION, merkleBlock.getVersion());

        long insertId = database.insert(BRSQLiteHelper.MB_TABLE_NAME, null, values);
        Cursor cursor = database.query(BRSQLiteHelper.MB_TABLE_NAME,
                allColumns, BRSQLiteHelper.MB_COLUMN_ID + " = " + insertId, null,
                null, null, null);
        cursor.moveToFirst();
        BRMerkleBlockEntity newMerkleBlock = cursorToMerkleBlock(cursor);
        cursor.close();
        return newMerkleBlock;
    }

    public void deleteMerkleBlock(BRMerkleBlockEntity merkleBlock) {
        long id = merkleBlock.getId();
        Log.e(TAG, "MerkleBlock deleted with id: " + id);
        database.delete(BRSQLiteHelper.MB_TABLE_NAME, BRSQLiteHelper.MB_COLUMN_ID
                + " = " + id, null);
    }

    public List<BRMerkleBlockEntity> getAllMerkleBlocks() {
        List<BRMerkleBlockEntity> merkleBlocks = new ArrayList<>();

        Cursor cursor = database.query(BRSQLiteHelper.MB_TABLE_NAME,
                allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            BRMerkleBlockEntity merkleBlockEntity = cursorToMerkleBlock(cursor);
            merkleBlocks.add(merkleBlockEntity);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return merkleBlocks;
    }

    private BRMerkleBlockEntity cursorToMerkleBlock(Cursor cursor) {
        BRMerkleBlockEntity merkleBlockEntity = new BRMerkleBlockEntity();
        merkleBlockEntity.setId(cursor.getInt(0));
        merkleBlockEntity.setBlockHash(cursor.getBlob(1));
        merkleBlockEntity.setFlags(cursor.getBlob(2));
        merkleBlockEntity.setHashes(cursor.getBlob(3));
        merkleBlockEntity.setHeight(cursor.getInt(4));
        merkleBlockEntity.setMerkleRoot(cursor.getBlob(5));
        merkleBlockEntity.setNonce(cursor.getInt(6));
        merkleBlockEntity.setPrevBlock(cursor.getBlob(7));
        merkleBlockEntity.setTarget(cursor.getInt(8));
        merkleBlockEntity.setTimeStamp(cursor.getLong(9));
        merkleBlockEntity.setTotalTransactions(cursor.getInt(10));
        merkleBlockEntity.setVersion(cursor.getInt(11));
        return merkleBlockEntity;
    }
}